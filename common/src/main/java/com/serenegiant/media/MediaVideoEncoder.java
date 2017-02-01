package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.common.BuildConfig;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaVideoEncoder extends MediaEncoder {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "MediaEncoder";

	private static final String MIME_TYPE = "video/avc";
	private static final int FRAME_RATE = 25;
	private static final float BPP = 0.25f;
	private Surface mSurface;
	private int mVideoWidth = 1280;
	private int mVideoHeight = 720;

	public MediaVideoEncoder(final MediaMovieRecorder muxer, final IMediaCodecCallback listener) {
		this(1280, 720, muxer, listener);
	}

	public MediaVideoEncoder(final int width, final int height, final MediaMovieRecorder muxer, final IMediaCodecCallback listener) {
		super(false, muxer, listener);
		mVideoWidth = width;
		mVideoHeight = height;
	}

	@Override
	public void prepare() throws IOException {
		if (DEBUG) Log.i(TAG, "prepare: ");
		mTrackIndex = -1;
		mMuxerStarted = mIsEOS = false;

		final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
		if (videoCodecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			return;
		}
		if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

		final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoWidth, mVideoHeight);
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
		format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
		format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
		if (DEBUG) Log.i(TAG, "format: " + format);

		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		// get Surface for encoder input
		// this method only can call between #configure and #start
		mSurface = mMediaCodec.createInputSurface();	// API >= 18
		mMediaCodec.start();
		mIsPrepared = true;
		if (DEBUG) Log.i(TAG, "prepare finishing");
		callOnPrepared();
	}

	@Override
	public void release() {
		if (DEBUG) Log.i(TAG, "release:");
		if (mSurface != null) {
			mSurface.release();
			mSurface = null;
		}
		super.release();
	}

	public void setVideoSize(final int width, final int height) throws IllegalArgumentException, IllegalStateException {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException(String.format(Locale.US, "size(%d,%d)", width, height));
		if (isRunning())
			throw new IllegalStateException("already start capturing");
		mVideoWidth = width;
		mVideoHeight = height;
	}

	public Surface getInputSurface() throws IllegalStateException {
		if (mSurface == null)
			throw new IllegalStateException("not prepared yet");
		return mSurface;
	}

	public int getWidth() {
		return mVideoWidth;
	}

	public int getHeight() {
		return mVideoHeight;
	}

	private int calcBitRate() {
		final int bitrate = (int)(BPP * FRAME_RATE * mVideoWidth * mVideoHeight);
		Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
		return bitrate;
	}

	/**
	 * select the first codec that match a specific MIME type
	 * @param mimeType
	 * @return null if no codec matched
	 */
	protected static final MediaCodecInfo selectVideoCodec(String mimeType) {
		if (DEBUG) Log.v(TAG, "selectVideoCodec:");

		// get the list of available codecs
		final int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

			if (!codecInfo.isEncoder()) {	// skipp decoder
				continue;
			}
			// select first codec that match a specific MIME type and color format
			final String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
					int format = selectColorFormat(codecInfo, mimeType);
					if (format > 0) {
						return codecInfo;
					}
				}
			}
		}
		return null;
	}

	/**
	 * select color format available on specific codec and we can use.
	 * @return 0 if no colorFormat is matched
	 */
	protected static final int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
		if (DEBUG) Log.i(TAG, "selectColorFormat: ");
		int result = 0;
		final MediaCodecInfo.CodecCapabilities caps;
		try {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			caps = codecInfo.getCapabilitiesForType(mimeType);
		} finally {
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		}
		int colorFormat;
		for (int i = 0; i < caps.colorFormats.length; i++) {
			colorFormat = caps.colorFormats[i];
			if (isRecognizedViewFormat(colorFormat)) {
				result = colorFormat;
				break;
			}
		}
		if (result == 0)
			Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
		return result;
	}

	/**
	 * color formats that we can use in this class
	 */
	protected static int[] recognizedFormats;
	static {
		recognizedFormats = new int[] {
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
			MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
		};
	}

	private static final boolean isRecognizedViewFormat(int colorFormat) {
		if (DEBUG) Log.i(TAG, "isRecognizedViewFormat:colorFormat=" + colorFormat);
		final int n = recognizedFormats != null ? recognizedFormats.length : 0;
		for (int i = 0; i < n; i++) {
			if (recognizedFormats[i] == colorFormat) {
				return true;
			}
		}
		return false;
	}

}
