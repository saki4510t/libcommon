package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * 動画のデコード用MediaCodecのラッパークラス
 */
public abstract class VideoDecoder extends AbstractDecoder {
	private static final boolean DEBUG = false;	// set false on production
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

	@Override
	public void prepare(final int trackIndex, @NonNull final MediaFormat format) {
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
		public void decode(@NonNull final MediaExtractor extractor) {
			if (mDecoder != null) {
				decodeAPI16(extractor, mDecoder, mInputBuffers);
			}
		}

		@Override
		protected OutputTask createOutputTask(final int trackIndex) {
			return new OutputTask(trackIndex) {
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
		public void decode(@NonNull final MediaExtractor extractor) {
			if (mDecoder != null) {
				decodeAPI21(extractor, mDecoder);
			}
		}

		@Override
		protected OutputTask createOutputTask(final int trackIndex) {
			return new OutputTask(trackIndex) {
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
