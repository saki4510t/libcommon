package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class VideoDecoder extends AbstractDecoder {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = VideoDecoder.class.getSimpleName();

	/**
	 * インスタンス生成用のヘルパーメソッド
	 * @param outputSurface
	 * @param listener
	 * @return
	 */
	@SuppressLint("NewApi")
	@NonNull
	public static VideoDecoder createDecoder(
		@NonNull final Surface outputSurface,
		@NonNull final DecoderListener listener) {
		if (BuildCheck.isAPI21()) {
			return new VideoDecoderAPI21(outputSurface, listener);
		} else {
			return new VideoDecoderAPI16(outputSurface, listener);
		}
	}

//--------------------------------------------------------------------------------
	private final Surface mOutputSurface;
	private int mVideoWidth, mVideoHeight;
	private long mDuration;
	private int mBitrate;
	private float mFrameRate;
	private int mRotation;

	/**
	 * コンストラクタ
	 * 直接のインスタンス生成を防止するためにprivateにする
	 * @param outputSurface
	 * @param listener
	 */
	private VideoDecoder(
		@NonNull final Surface outputSurface,
		@NonNull final DecoderListener listener) {
		super("video/", listener);
		mOutputSurface = outputSurface;
	}

	public final int getWidth() {
		return mVideoWidth;
	}

	public final int getHeight() {
		return mVideoHeight;
	}

	public final int getBitRate() {
		return mBitrate;
	}

	public final float getFramerate() {
		return mFrameRate;
	}

	/**
	 * @return 0, 90, 180, 270
	 */
	public final int getRotation() {
		return mRotation;
	}

	/**
	 * get duration time as micro seconds
	 *
	 * @return
	 */
	public final long getDurationUs() {
		return mDuration;
	}

	@Override
	protected void updateInfo(@NonNull final MediaMetadataRetriever metaData) {
		mVideoWidth = mVideoHeight = mRotation = mBitrate = 0;
		mDuration = 0;
		mFrameRate = 0;
		String value = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
		if (!TextUtils.isEmpty(value)) {
			mVideoWidth = Integer.parseInt(value);
		}
		value = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
		if (!TextUtils.isEmpty(value)) {
			mVideoHeight = Integer.parseInt(value);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			value = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);	// API>=17
			if (!TextUtils.isEmpty(value)) {
				mRotation = Integer.parseInt(value);
			}
		}
		value = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
		if (!TextUtils.isEmpty(value)) {
			mBitrate = Integer.parseInt(value);
		}
		value = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if (!TextUtils.isEmpty(value)) {
			mDuration = Long.parseLong(value) * 1000;
		}
	}

	@Override
	protected void internalPrepare(final int trackIndex, @NonNull final MediaFormat format) {
		mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
		mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
		mDuration = format.getLong(MediaFormat.KEY_DURATION);

		if (DEBUG) Log.v(TAG, String.format("format:size(%d,%d),duration=%d,bps=%d,framerate=%f,rotation=%d",
			mVideoWidth, mVideoHeight, mDuration, mBitrate, mFrameRate, mRotation));
	}

	@Override
	protected MediaCodec createDecoder(final int trackIndex, @NonNull final MediaFormat format) throws IOException {
		MediaCodec codec = null;
		if (trackIndex >= 0) {
	        final String mime = format.getString(MediaFormat.KEY_MIME);
			try {
				codec = MediaCodec.createDecoderByType(mime);
				codec.configure(format, mOutputSurface, null, 0);
		        codec.start();
				if (DEBUG) Log.v(TAG, "createDecoder:codec started");
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		return codec;
	}

	private static final long VSYNC2 = 33330000;		// 33.33ミリ秒, approx. 2 frames @ 60fps
	/*
	 * API21以降で使用可能なMediaCodec#releaseOutputBuffer(int,long)は再生したいシステム時刻から
	 * vsync x 2早く(通常の60fpsディスプレーであれば約33ミリ秒早く)#releaseOutputBufferを呼び出すと
	 * 最適なパフォーマンスと品質が得られるらしいのでptsを調整する。
	 * #releaseOutputBufferを呼ぶときのptsがシステム時刻と大きく離れている時は無視されて、一番早く表示可能な
	 * タイミング表示される、この場合にはフレームがドロップすることはないらしい。
	 * でも#releaseOutputBuffer(int,long)へ調整したptsを渡すだけではだめで自前でウエイトを入れないとだめっぽい
	 */
	private long mOffsetPtsNs = -1L;
	private long mOffsetSysTimeNs = -1L;
	/**
	 * 最初のフレームのpresentationTimeUsとシステム時間を保存しておいて
	 * 現在のフレームのpresentationTimeUsと現在時刻から描画予定時刻を計算して、
	 * そのフレームの描画予定時刻-VSYNC2まで待機する
	 * @param presentationTimeUs
	 * @return
	 */
	protected long adjustPresentationTime(final long presentationTimeUs) {
		final long presentationTimeNs = presentationTimeUs * 1000L;
		if (mOffsetSysTimeNs <= 0) {
			// 初回
			mOffsetSysTimeNs = System.nanoTime();
			mOffsetPtsNs = mOffsetSysTimeNs - presentationTimeNs;
		} else {
			// 2回目以降
			// 現在のptsから最初のptsを引いたのが再生位置、そこから最初のシステム時間と現在システム時間の差を引いたのが待ち時間
			final long base = (mOffsetPtsNs + presentationTimeNs) - VSYNC2;
			for (long t = base - System.nanoTime();
				isRunning() && (t > 0); t = base - System.nanoTime()) {

				if (t > 20000000) t = 20000000;	// 20ミリ以上なら10ミリ秒にする
				synchronized (mSync) {
					try {
						mSync.wait(t / 1000000, (int)(t % 1000000));
					} catch (final InterruptedException e) {
						// ignore
					}
				}
			}
		}
		return System.nanoTime() + VSYNC2;
	}

//--------------------------------------------------------------------------------
	private static class VideoDecoderAPI16 extends VideoDecoder {
		private static final String TAG = VideoDecoderAPI16.class.getSimpleName();
		@Nullable
		private ByteBuffer[] mInputBuffers;

		/**
		 * コンストラクタ
		 * 直接のインスタンス生成を防止するためにprivateにする
		 * @param outputSurface
		 * @param listener
		 */
		private VideoDecoderAPI16(
			@NonNull final Surface outputSurface,
			@NonNull final DecoderListener listener) {
			super(outputSurface, listener);
		}

		@Override
		protected MediaCodec createDecoder(
			final int trackIndex,
			@NonNull final MediaFormat format) throws IOException {

			final MediaCodec codec = super.createDecoder(trackIndex, format);
			mInputBuffers = codec.getInputBuffers();	// API>=16, deprecated API21
			// XXX 映像の場合はSurfaceへ出力するようにしているので出力バッファの取得は不要
			return codec;
		}

		@Override
		protected DecodeTask createOutputTask() {
			return new DecodeTask() {

				@Override
				protected void handleInput(
					@NonNull final MediaExtractor extractor, final int targetTrackIndex,
					@NonNull final MediaCodec decoder) {

					handleInputAPI16(extractor, targetTrackIndex, decoder, mInputBuffers);
				}

				@Override
				protected void handleOutput(@NonNull final MediaCodec decoder) {
					while (isRunning() && !mOutputDone) {
						final int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
						if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
							return;
						} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
							// XXX 映像の場合はSurfaceへ出力するようにしているので出力バッファの取得は不要
							if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
						} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
							if (DEBUG) Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED:" + decoder.getOutputFormat());
						} else if (decoderStatus < 0) {
							throw new RuntimeException(
								"unexpected result from video decoder.dequeueOutputBuffer: " + decoderStatus);
						} else { // decoderStatus >= 0
							boolean doRender = (mBufferInfo.size > 0);
							if (doRender) {
								if (!onFrameAvailable(mBufferInfo.presentationTimeUs)) {
									adjustPresentationTime(mBufferInfo.presentationTimeUs);
								}
							}
							decoder.releaseOutputBuffer(decoderStatus, doRender);
							if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
								if (DEBUG) Log.d(TAG, "video:output EOS");
								synchronized (mSync) {
									mOutputDone = true;
									mSync.notifyAll();
								}
							}
						}
					}	// end of while
				}
			};
		}

	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private static class VideoDecoderAPI21 extends VideoDecoder {
		private static final String TAG = VideoDecoderAPI21.class.getSimpleName();
		/**
		 * コンストラクタ
		 * 直接のインスタンス生成を防止するためにprivateにする
		 * @param outputSurface
		 * @param listener
		 */
		private VideoDecoderAPI21(
			@NonNull final Surface outputSurface,
			@NonNull final DecoderListener listener) {
			super(outputSurface, listener);
		}

		@Override
		protected DecodeTask createOutputTask() {
			return new DecodeTask() {
				@Override
				protected void handleInput(
					@NonNull final MediaExtractor extractor, final int targetTrackIndex,
					@NonNull final MediaCodec decoder) {

					handleInputAPI21(extractor, targetTrackIndex, decoder);
				}

				@Override
				protected void handleOutput(@NonNull final MediaCodec decoder) {
					while (isRunning() && !mOutputDone) {
						final int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
						if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
							return;
						} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
							// API>=21ではdeprecated
							if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
						} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
							if (DEBUG) Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED:" + decoder.getOutputFormat());
						} else if (decoderStatus < 0) {
							throw new RuntimeException(
								"unexpected result from video decoder.dequeueOutputBuffer: " + decoderStatus);
						} else { // decoderStatus >= 0
							decoder.releaseOutputBuffer(decoderStatus,						// API>=21
								adjustPresentationTime(mBufferInfo.presentationTimeUs));
							if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
								if (DEBUG) Log.d(TAG, "video:output EOS");
								synchronized (mSync) {
									mOutputDone = true;
									mSync.notifyAll();
								}
							}
						}
					}	// end of while
				}
			};
		}
	}
}
