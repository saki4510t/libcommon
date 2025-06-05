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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 音声デコード用のMediaCodecラッパー
 */
public abstract class AudioDecoder extends AbstractDecoder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AudioDecoder.class.getSimpleName();

	/**
	 * インスタンス生成用のヘルパーメソッド
	 * @param listener
	 * @return
	 */
	@NonNull
	public static AudioDecoder createDecoder(
		@NonNull final DecoderListener listener) {
		if (BuildCheck.isAPI21()) {
			return new AudioDecoderAPI21(listener);
		} else {
			return new AudioDecoderAPI16(listener);
		}
	}

//--------------------------------------------------------------------------------
	private boolean mHasAudio;
	@Nullable
	protected AudioTrack mAudioTrack;
	protected int mAudioInputBufSize;

	/**
	 * コンストラクタ
	 * 直接のインスタンス生成を防止するためにprivateにする
	 * @param listener
	 */
	private AudioDecoder(
		@NonNull final DecoderListener listener) {
		super("audio/", listener);
	}

	@Override
	public void prepare(final int trackIndex, @NonNull final MediaFormat format) {
		final int audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
		final int audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		final int min_buf_size = AudioTrack.getMinBufferSize(audioSampleRate,
			(audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
			AudioFormat.ENCODING_PCM_16BIT);
		final int max_input_size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
		mAudioInputBufSize =  min_buf_size > 0 ? min_buf_size * 4 : max_input_size;
		if (mAudioInputBufSize > max_input_size) mAudioInputBufSize = max_input_size;
		final int frameSizeInBytes = audioChannels * 2;
		mAudioInputBufSize = (mAudioInputBufSize / frameSizeInBytes) * frameSizeInBytes;
		if (DEBUG) Log.v(TAG, String.format("getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d",
			min_buf_size, max_input_size, mAudioInputBufSize));
		//
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
			audioSampleRate,
			(audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
			AudioFormat.ENCODING_PCM_16BIT,
			mAudioInputBufSize,
			AudioTrack.MODE_STREAM);
		try {
			mAudioTrack.play();
		} catch (final Exception e) {
			Log.e(TAG, "failed to start audio track playing", e);
			mAudioTrack.release();
			mAudioTrack = null;
		}
	}

	@Override
	protected MediaCodec createDecoder(final int trackIndex, @NonNull final MediaFormat format) throws IOException {
		MediaCodec codec = null;
		if (trackIndex >= 0) {
	        final String mime = format.getString(MediaFormat.KEY_MIME);
			try {
				codec = MediaCodec.createDecoderByType(mime);
				codec.configure(format, null, null, 0);
		        codec.start();
				if (DEBUG) Log.v(TAG, "createDecoder:codec started");
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		return codec;
	}

//--------------------------------------------------------------------------------
	private static class AudioDecoderAPI16 extends AudioDecoder {
		private static final String TAG = AudioDecoderAPI16.class.getSimpleName();
		@Nullable
		private ByteBuffer[] mInputBuffers;
		@Nullable
		private ByteBuffer[] mOutputBuffers;
		protected byte[] mAudioOutTempBuf;

		/**
		 * コンストラクタ
		 * 直接のインスタンス生成を防止するためにprivateにする
		 * @param listener
		 */
		private AudioDecoderAPI16(
			@NonNull final DecoderListener listener) {
			super(listener);
		}

		@Override
		protected MediaCodec createDecoder(final int trackIndex, @NonNull final MediaFormat format) throws IOException {
			final MediaCodec codec = super.createDecoder(trackIndex, format);
			mInputBuffers = codec.getInputBuffers();	// API>=16, deprecated API21
			mOutputBuffers = codec.getOutputBuffers();	// API>=16, deprecated API21
			int sz = mOutputBuffers[0].capacity();
			if (sz <= 0) {
				sz = mAudioInputBufSize;
			}
			if (DEBUG) Log.v(TAG, "AudioOutputBufSize:" + sz);
			mAudioOutTempBuf = new byte[sz];
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
							mOutputBuffers = decoder.getOutputBuffers();
							if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
						} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
							if (DEBUG) {
								final MediaFormat newFormat = decoder.getOutputFormat();
								Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED:" + newFormat);
							}
						} else if (decoderStatus < 0) {
							throw new RuntimeException(
								"unexpected result from audio decoder.dequeueOutputBuffer: " + decoderStatus);
						} else { // decoderStatus >= 0
							final int size = mBufferInfo.size;
							if (size > 0) {
								final ByteBuffer buffer = mOutputBuffers[decoderStatus];
								if ((mAudioOutTempBuf == null) || (mAudioOutTempBuf.length < size)) {
									mAudioOutTempBuf = new byte[size * 3 / 2];    // 1.5倍確保する
								}
								buffer.position(0);
								buffer.get(mAudioOutTempBuf, 0, size);
								buffer.clear();
								if (mAudioTrack != null) {
									mAudioTrack.write(mAudioOutTempBuf, 0, size);
								}
								if (!onFrameAvailable(mBufferInfo.presentationTimeUs)) {
									adjustPresentationTime(mBufferInfo.presentationTimeUs);
								}
							}
							decoder.releaseOutputBuffer(decoderStatus, false);
							if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
								if (DEBUG) Log.d(TAG, "audio:output EOS");
								synchronized (mSync) {
									mOutputDone = true;
									mSync.notifyAll();
								}
							}
						}
					} // end of while
				}
			};
		}

	}

	@androidx.annotation.RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private static class AudioDecoderAPI21 extends AudioDecoder {
		private static final String TAG = AudioDecoderAPI21.class.getSimpleName();
		/**
		 * コンストラクタ
		 * 直接のインスタンス生成を防止するためにprivateにする
		 * @param listener
		 */
		private AudioDecoderAPI21(
			@NonNull final DecoderListener listener) {
			super(listener);
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
							if (DEBUG) {
								final MediaFormat newFormat = decoder.getOutputFormat();
								Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED:" + newFormat);
							}
						} else if (decoderStatus < 0) {
							throw new RuntimeException(
								"unexpected result from audio decoder.dequeueOutputBuffer: " + decoderStatus);
						} else { // decoderStatus >= 0
							final int size = mBufferInfo.size;
							if (size > 0) {
								final ByteBuffer out = decoder.getOutputBuffer(decoderStatus);	// API>=21
								out.clear();
								if (mAudioTrack != null) {
									mAudioTrack.write(out, size, AudioTrack.WRITE_BLOCKING);	// API>=21
								}
								if (!onFrameAvailable(mBufferInfo.presentationTimeUs)) {
									adjustPresentationTime(mBufferInfo.presentationTimeUs);
								}
							}
							decoder.releaseOutputBuffer(decoderStatus, false);
							if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
								if (DEBUG) Log.d(TAG, "audio:output EOS");
								synchronized (mSync) {
									mOutputDone = true;
									mSync.notifyAll();
								}
							}
						}
					}
				}
			};
		}
	}
}
