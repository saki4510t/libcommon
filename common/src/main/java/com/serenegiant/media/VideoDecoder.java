package com.serenegiant.media;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.common.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder extends Decoder {
	private static final boolean DEBUG = BuildConfig.DEBUG;

	private Surface mSurface;
	private int mVideoWidth, mVideoHeight;
	private int mRotation;

	public int getVideoWidth() {
		return mVideoWidth;
	}

	public int getVideoHeight() {
		return mVideoHeight;
	}

	public int getRotation() {
		return mRotation;
	}

	@Override
	protected int handlePrepare(final MediaExtractor media_extractor) {
		int track_index = selectTrack(media_extractor, "video/");
		if (track_index >= 0) {
			final MediaFormat format = media_extractor.getTrackFormat(track_index);
			mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
			mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

			if (DEBUG) Log.v(TAG, String.format("format:size(%d,%d),duration=%d,bps=%d,rotation=%d",
					mVideoWidth, mVideoHeight, getDuration(), getBitRate(), mRotation));
		}
		return track_index;
	}

	@Override
	protected MediaCodec createCodec(final MediaExtractor media_extractor, final int track_index, final MediaFormat format)
		throws IOException {

		if (Build.VERSION.SDK_INT > 18) {
			format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);
		}
		return super.createCodec(media_extractor, track_index, format);
	}

	public void setSurface(Surface surface) {
		mSurface = surface;
	}

	@Override
	protected Surface getOutputSurface() {
		if (mSurface == null) {
			IllegalArgumentException e = new IllegalArgumentException("need to call setSurface before prepare");
			if (!callErrorHandler(e)) {
				throw e;
			}
		}
		return mSurface;
	}

	@Override
	protected boolean handleOutput(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
		return false;
	}

	@Override
	protected void updateMovieInfo(final MediaMetadataRetriever metadata) {
		super.updateMovieInfo(metadata);

		mVideoWidth = mVideoHeight = mRotation = 0;
		String value = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
		if (!TextUtils.isEmpty(value)) {
			mVideoWidth = Integer.parseInt(value);
		}
		value = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
		if (!TextUtils.isEmpty(value)) {
			mVideoHeight = Integer.parseInt(value);
		}
		value = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
		if (!TextUtils.isEmpty(value)) {
			mRotation = Integer.parseInt(value);
		}
	}

}
