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
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.Time;

import java.nio.ByteBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * MediaCodecとMediaReaperの処理をまとめたエンコード用の基本クラス
 */
public abstract class MediaEncoder implements Encoder {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = MediaEncoder.class.getSimpleName();

	public static final int TIMEOUT_USEC = 10000;    // 10ミリ秒

	public static class Encoder {
		@NonNull
		final MediaCodec mediaCodec;
		@NonNull
		final MediaReaper reaper;
		final boolean mayFail;

		public Encoder(
			@NonNull final MediaCodec mediaCodec,
			@NonNull final MediaReaper reaper,
			final boolean mayFail) {

			this.mediaCodec = mediaCodec;
			this.reaper = reaper;
			this.mayFail = mayFail;
		}
	}

	/**
	 * フラグの排他制御用
	 */
	@NonNull
	protected final Object mSync = new Object();
	@NonNull
	private final EncoderListener2 mListener;
	/**
	 * MIME
	 */
	@NonNull
	private final String MIME_TYPE;
	/**
	 * エンコード中かどうか
	 */
	private volatile boolean mIsEncoding;
	/**
	 * 終了要求フラグ(新規エンコード禁止フラグ)
	 */
	private volatile boolean mRequestStop;
	/**
	 * エンコーダーの本体MediaCodecインスタンス
	 */
	@Nullable
	private MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
	/**
	 * エンコーダーからの出力データハンドリング用MediaReaper
	 */
	@Nullable
	private MediaReaper mReaper;

	/**
	 * コンストラクタ
	 *
	 * @param mime_type
	 * @param encoderListener
	 */
	public MediaEncoder(
		@NonNull final String mime_type,
		@NonNull final EncoderListener2 encoderListener) {

		MIME_TYPE = mime_type;
		mListener = encoderListener;
	}

	@Override
	protected void finalize() throws Throwable {
//    	if (DEBUG) Log.v(TAG, "finalize:");
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 子クラスでOverrideした時でもEncoder#releaseを呼び出すこと
	 */
	@CallSuper
	@Override
	public void release() {
		if (DEBUG) Log.d(TAG, "release:");
		final boolean shouldCallOnDestroy = (mMediaCodec != null) || (mReaper != null);
		final boolean isEncoding = mIsEncoding;
		mIsEncoding = false;
		if (isEncoding && shouldCallOnDestroy) {
			try {
				mListener.onStopEncode(this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, "release: failed onStopped", e);
			}
		}
		if (mMediaCodec != null) {
			try {
				if (DEBUG) Log.v(TAG, "release: call MediaCodec#stop");
				mMediaCodec.stop();
				mMediaCodec.release();
				mMediaCodec = null;
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, "release: failed releasing MediaCodec", e);
			}
		}
		if (mReaper != null) {
			mReaper.release();
			mReaper = null;
		}
		if (shouldCallOnDestroy) {
			try {
				mListener.onDestroy(this);
			} catch (final Exception e) {
				if (DEBUG) Log.e(TAG, "release: onDestroy failed", e);
			}
		}
	}

	@Override
	public final void prepare() throws Exception {
		mIsEncoding = true;
		@NonNull final Encoder encoder = internalPrepare(getReaperListener());
		mMediaCodec = encoder.mediaCodec;
		mReaper = encoder.reaper;
		final boolean mayFail = encoder.mayFail;
		final Surface surface = (this instanceof ISurfaceEncoder) ?
			((ISurfaceEncoder) this).getInputSurface() : null;
		try {
			mListener.onStartEncode(this, surface, mayFail);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	@NonNull
	protected abstract MediaReaper.ReaperListener getReaperListener();

	/**
	 * MediaCodecのエンコーダーとMediaReaperを初期化する
	 *
	 * @param listener
	 * @return
	 * @throws Exception
	 */
	protected abstract Encoder internalPrepare(
		@NonNull final MediaReaper.ReaperListener listener) throws Exception;

	/**
	 * エラー発生時に呼び出す
	 *
	 * @param e
	 */
	protected void callOnError(final Exception e) {
		try {
			mListener.onError(e);
		} catch (final Exception e2) {
			Log.w(TAG, e2);
		}
	}
//********************************************************************************

	/**
	 * エンコード開始要求(Recorderから呼び出される)
	 */
	@Override
	public void start() {
//    	if (DEBUG) Log.v(TAG, "start");
		synchronized (mSync) {
			mIsEncoding = true;
			mRequestStop = false;
		}
	}

	/**
	 * エンコーダ終了要求(Recorderから呼び出される)
	 */
	@Override
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop");
		synchronized (mSync) {
			if (mRequestStop) {
				return;
			}
			if (mReaper != null) {
				mReaper.frameAvailableSoon();
			}
			// 終了要求
			mRequestStop = true;    // 新規のフレームを受けないようにする
			mSync.notifyAll();
		}
		// 本当のところいつ終了するのかはわからないので、呼び出し元スレッドを遅延させないために終了待ちせずに直ぐに返る
	}

	/**
	 * フレームデータの読込み準備要求
	 * native側からも呼び出されるので名前を変えちゃダメ
	 */
	@Override
	public void frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon:");
		synchronized (mSync) {
			if (!mIsEncoding || mRequestStop) {
				return;
			}
			if (mReaper != null) {
				mReaper.frameAvailableSoon();
			}
			mSync.notifyAll();
		}
	}

	@Override
	public boolean isEncoding() {
		return mIsEncoding;
	}

	protected boolean isRequestStop() {
		return mRequestStop;
	}

	protected void requestStop() {
		mRequestStop = true;
	}

	protected boolean isReady() {
		return mIsEncoding && !mRequestStop;
	}

	@NonNull
	public String getMimeType() {
		return MIME_TYPE;
	}

	/**
	 * ストリーミング終了指示を送る
	 */
	@Override
	public void signalEndOfInputStream() {
//		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
		// MediaCodec#signalEndOfInputStreamはBUFFER_FLAG_END_OF_STREAMフラグを付けて
		// 空のバッファをセットするのと等価である
		// ・・・らしいので空バッファを送る。encode内でBUFFER_FLAG_END_OF_STREAMを付けてセットする
		if (BuildCheck.isAPI18()) {
			if (mMediaCodec != null) {
				mMediaCodec.signalEndOfInputStream();	// API >= 18
			}
		} else {
			encode(null, getInputPTSUs());	// API>=16
		}
	}

	/**
	 * バイト配列をエンコードする場合
	 * @param buffer
	 * @param presentationTimeUs [マイクロ秒]
	 */
	@Override
	public void encode(final ByteBuffer buffer, final long presentationTimeUs) {
		final MediaCodec encoder;
		synchronized (mSync) {
			if (!mIsEncoding || mRequestStop) return;
			if (mMediaCodec == null) return;
			encoder = mMediaCodec;
		}
		if (BuildCheck.isAPI21()) {
			encodeApi21(encoder, buffer, presentationTimeUs);
		} else {
			encode(encoder, buffer, presentationTimeUs);
		}
	}

	/**
	 * バイト配列エンコードする場合, API<21
	 * @param encoder
	 * @param buffer
	 * @param presentationTimeUs
	 */
	protected void encode(
		@NonNull final MediaCodec encoder,
		@Nullable final ByteBuffer buffer,
		final long presentationTimeUs) {

		final int length = buffer != null ? buffer.remaining() : 0;
		final ByteBuffer[] inputBuffers = encoder.getInputBuffers();
		while (mIsEncoding) {
			final int inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufferIndex >= 0) {
				final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				if (buffer != null) {
					inputBuffer.put(buffer);
				}
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
				if (length <= 0) {
					// エンコード要求サイズが0の時はEOSを送信
//	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
					encoder.queueInputBuffer(inputBufferIndex, 0, 0,
						presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				} else {
					encoder.queueInputBuffer(inputBufferIndex, 0, length,
						presentationTimeUs, 0);
				}
				break;
			} else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//	        	// 送れるようになるまでループする
//	        	// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
				frameAvailableSoon();    // drainが詰まってると予想されるのでdrain要求をする
			}
		}
	}

	/**
	 * バイト配列エンコードする場合, API>=21
	 * @param encoder
	 * @param buffer
	 * @param presentationTimeUs
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	protected void encodeApi21(
		@NonNull final MediaCodec encoder,
		@Nullable final ByteBuffer buffer,
		final long presentationTimeUs) {

		final int length = buffer != null ? buffer.remaining() : 0;
		while (mIsEncoding) {
			final int inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufferIndex >= 0) {
				final ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
				inputBuffer.clear();
				if (buffer != null) {
					inputBuffer.put(buffer);
				}
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
				if (length <= 0) {
					// エンコード要求サイズが0の時はEOSを送信
//	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
					encoder.queueInputBuffer(inputBufferIndex, 0, 0,
						presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				} else {
					encoder.queueInputBuffer(inputBufferIndex, 0, length,
						presentationTimeUs, 0);
				}
				break;
			} else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//	        	// 送れるようになるまでループする
//	        	// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
				frameAvailableSoon();    // drainが詰まってると予想されるのでdrain要求をする
			}
		}
	}

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 *
	 * @return
	 */
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
		if (result <= prevInputPTSUs) {
//			Log.w(TAG, "input pts smaller than previous output PTS");
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
	}

	public static boolean supportsAdaptiveStreaming = BuildCheck.isKitKat();

	public void adjustBitrate(final int targetBitrate) {
		if (supportsAdaptiveStreaming && mMediaCodec != null) {
			final Bundle bitrate = new Bundle();
			bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, targetBitrate);
			mMediaCodec.setParameters(bitrate);
		} else if (!supportsAdaptiveStreaming) {
			Log.w(TAG, "adjustBitrate: Ignoring adjustVideoBitrate call. This functionality is only available on Android API 19+");
		}
	}

	/**
	 * 音声エンコード用MediaEncoder実装
	 */
	public static abstract class MediaAudioEncoder extends MediaEncoder implements IAudioEncoder {
		private final int mChannelCount;
		private final int mSampleRate;
		private final int mBitRate;

		/**
		 * コンストラクタ
		 *
		 * @param mime_type
		 * @param encoderListener
		 */
		public MediaAudioEncoder(
			@NonNull final String mime_type,
			@NonNull final EncoderListener2 encoderListener,
			final int audioChannels, final int sampleRate, final int bitRate) {

			super(mime_type, encoderListener);
			mChannelCount = audioChannels;
			mSampleRate = sampleRate;
			mBitRate = bitRate;
		}

		/**
		 * MediaEncoderの抽象メソッドの実装
		 *
		 * @param listener
		 * @return
		 * @throws Exception
		 */
		@Override
		protected Encoder internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
			final String mimeType = getMimeType();
			if (DEBUG) Log.v(TAG, "internalPrepare:" + mimeType);
			// 音声を取り込んでAACにエンコードするためのMediaCodecの準備
			final MediaCodecInfo codecInfo = MediaCodecUtils.selectAudioEncoder(mimeType);
			if (codecInfo == null) {
				throw new IllegalArgumentException("Unable to find an appropriate codec for " + mimeType);
			}
			if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());

			final MediaFormat audioFormat = MediaFormat.createAudioFormat(mimeType, mSampleRate, mChannelCount);
			audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK,
				mChannelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
			audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
			audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
			if (DEBUG) Log.i(TAG, "format: " + audioFormat);

			final MediaCodec mediaCodec = MediaCodec.createEncoderByType(mimeType);
			mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mediaCodec.start();
			final MediaReaper reaper = new MediaReaper.AudioReaper(mediaCodec, listener, mSampleRate, mChannelCount);
			return new Encoder(mediaCodec, reaper, false);
		}

	}

	/**
	 * 映像エンコード用MediaEncoder実装
	 * API>=18ならSurfaceからの映像入力、API<18なら#encodeメソッドによる映像入力となる
	 */
	public static abstract class MediaVideoEncoder extends MediaEncoder implements ISurfaceEncoder {
		@NonNull
		private final VideoConfig mVideoConfig;
		private int mWidth, mHeight;
		private int mBitRate = -1;
		private int mFramerate = -1;
		private int mIFrameIntervals = -1;
		@Nullable
		private Surface mInputSurface;

		/**
		 * コンストラクタ
		 *
		 * @param mimeType
		 * @param encoderListener
		 * @param config
		 */
		public MediaVideoEncoder(
			@NonNull final String mimeType,
			@NonNull final EncoderListener2 encoderListener,
			@Nullable final VideoConfig config) {

			super(mimeType, encoderListener);
			mVideoConfig = config != null ? config : new VideoConfig();
		}

		/**
		 * MediaEncoderの抽象メソッドの実装
		 *
		 * @param listener
		 * @return
		 * @throws Exception
		 */
		@SuppressLint("NewApi")
		@Override
		protected Encoder internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
			final String mimeType = getMimeType();
			if (DEBUG) Log.v(TAG, "internalPrepare:" + mimeType);
			final MediaCodecInfo codecInfo = MediaCodecUtils.selectVideoEncoder(mimeType);
			if (codecInfo == null) {
				throw new IllegalArgumentException("Unable to find an appropriate codec for " + mimeType);
			}
			if ((mWidth < MIN_WIDTH) || (mHeight < MIN_HEIGHT)) {
				throw new IllegalArgumentException("Wrong video size(" + mWidth + "x" + mHeight + ")");
			}
			if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());
			final boolean mayFail = ((mWidth >= 1000) || (mHeight >= 1000));
			final MediaFormat format = MediaFormat.createVideoFormat(mimeType, mWidth, mHeight);

			// MediaCodecに適用するパラメータを設定する。誤った設定をするとMediaCodec#configureが
			// 復帰不可能な例外を生成する
			if (BuildCheck.isAPI18()) {
				format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
					MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
			}
			format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate > 0
				? mBitRate : getConfig().getBitrate(mWidth, mHeight));
			format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate > 0
				? mFramerate : getConfig().captureFps());
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameIntervals > 0
				? mIFrameIntervals : getConfig().calcIFrameIntervals());
			if (DEBUG) Log.d(TAG, "format: " + format);
			// 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
			final MediaCodec mediaCodec = MediaCodec.createEncoderByType(mimeType);
			mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			if (BuildCheck.isAPI18()) {
				// エンコーダーへの入力に使うSurfaceを取得する
				mInputSurface = mediaCodec.createInputSurface();    // API >= 18
			}
			mediaCodec.start();
			final MediaReaper reaper = new MediaReaper.VideoReaper(mediaCodec, listener, mWidth, mHeight);
			return new Encoder(mediaCodec, reaper, mayFail);
		}

		/**
		 * IVideoEncoderの実装
		 * 動画サイズをセット
		 * ビットレートもサイズとVideoConfigの設定値に合わせて変更される
		 *
		 * @param width
		 * @param height
		 */
		@Override
		public void setVideoSize(final int width, final int height)
			throws IllegalArgumentException, IllegalStateException {
			if (DEBUG) Log.v(TAG, String.format("setVideoSize(%d,%d)", width, height));
			mWidth = width;
			mHeight = height;
			mBitRate = getConfig().getBitrate(width, height);
		}

		/**
		 * IVideoEncoderの実装
		 *
		 * @param bitRate
		 * @param frameRate
		 * @param iFrameIntervals
		 */
		@Override
		public void setVideoConfig(final int bitRate, final int frameRate, final int iFrameIntervals) {
			mBitRate = bitRate;
			mFramerate = frameRate;
			mIFrameIntervals = iFrameIntervals;
		}

		/**
		 * IVideoEncoderの実装
		 *
		 * @return
		 */
		@Override
		public int getWidth() {
			return mWidth;
		}

		/**
		 * IVideoEncoderの実装
		 *
		 * @return
		 */
		@Override
		public int getHeight() {
			return mHeight;
		}

		/**
		 * ISurfaceEncoderの実装
		 *
		 * @return
		 */
		@Nullable
		@Override
		public Surface getInputSurface() {
			return mInputSurface;
		}

		@NonNull
		public VideoConfig getConfig() {
			return mVideoConfig;
		}

	}
}
