package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.Time;

import java.nio.ByteBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MediaCodecを使ったエンコーダーの基本クラス
 */
public abstract class AbstractEncoder implements Encoder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractEncoder.class.getSimpleName();

    public static final int TIMEOUT_USEC = 10000;	// 10ミリ秒

	/**
	 * フラグの排他制御用
	 */
	@NonNull
	protected final Object mSync = new Object();
	/**
	 * エンコーダーイベントコールバックリスナー
	 */
	@NonNull
	private final EncoderListener2 mListener;
	/**
	 * MIME
	 */
	@NonNull
	protected final String MIME_TYPE;
    /**
     * エンコード実行中フラグ
     */
    protected volatile boolean mIsEncoding;
    /**
     * 終了要求フラグ(新規エンコード禁止フラグ)
     */
    protected volatile boolean mRequestStop;
    /**
     * トラックインデックス
     */
    protected int mTrackIndex;
    /**
     * エンコーダーの本体MediaCodecインスタンス
     */
    @Nullable
    protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
	@Nullable
	protected MediaReaper mReaper;

    /**
     * Recorderオブジェクトへの参照
     */
    @Nullable
    private IRecorder mRecorder;

//********************************************************************************
	@SuppressWarnings("deprecation")
	@Deprecated
    public AbstractEncoder(@NonNull final String mimeType,
    	@NonNull final IRecorder recorder,
    	@NonNull final EncoderListener listener) {

		this(mimeType, recorder, EncoderListener2.wrap(listener));
    }

    public AbstractEncoder(@NonNull final String mimeType,
    	@NonNull final IRecorder recorder,
    	@NonNull final EncoderListener2 listener) {

    	if (listener == null) throw new NullPointerException("EncodeListener is null");
    	if (recorder == null) throw new NullPointerException("recorder is null");
    	MIME_TYPE = mimeType;
    	mRecorder = recorder;
    	mListener = listener;
		recorder.addEncoder(this);
    }

    /**
     * 出力用のMuxerWrapperを返す
     * @return
     */
    @NonNull
	public IRecorder getRecorder() {
    	return mRecorder;
    }

	@NonNull
	public VideoConfig getConfig() {
		return mRecorder.getConfig();
	}

	public int getCaptureFormat() {
		return -1;
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

	@Override
	public final void prepare() throws Exception {
		final boolean mayFail = internalPrepare(mReaperListener);
		final Surface surface = (this instanceof ISurfaceEncoder) ?
			((ISurfaceEncoder)this).getInputSurface() : null;
		try {
			mListener.onStartEncode(this, surface, mayFail);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * MediaCodecのエンコーダーとMediaReaperを初期化する
	 * @param listener
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean internalPrepare(
		@NonNull final MediaReaper.ReaperListener listener) throws Exception;

	/**
	 * エラー発生時に呼び出す
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
	public  void start() {
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
	public  void stop() {
    	if (DEBUG) Log.v(TAG, "stop");
    	synchronized (mSync) {
            if (mRequestStop) {
                return;
            }
			if (mReaper != null) {
				mReaper.frameAvailableSoon();
			}
	        // 終了要求
            mRequestStop = true;	// 新規のフレームを受けないようにする
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
//    	if (DEBUG) Log.v(TAG, "AbstractEncoder#frameAvailableSoon");
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

//--------------------------------------------------------------------------------
	private final MediaReaper.ReaperListener mReaperListener
		= new MediaReaper.ReaperListener() {
		@Override
		public void writeSampleData(
			@NonNull final MediaReaper reaper,
			@NonNull final ByteBuffer byteBuf,
			@NonNull final MediaCodec.BufferInfo bufferInfo) {

//			if (DEBUG) Log.v(TAG, "writeSampleData:");
			if (mIsEncoding && !mRequestStop && (mRecorder != null)) {
				mRecorder.writeSampleData(mTrackIndex, byteBuf, bufferInfo);
			}
		}

		@Override
		public void onOutputFormatChanged(
			@NonNull final MediaReaper reaper,
			@NonNull final MediaFormat format) {

			if (DEBUG) Log.v(TAG, "onOutputFormatChanged:" + format);
			if (mIsEncoding && !mRequestStop && (mRecorder != null)) {
				startRecorder(mRecorder, format);
			}
		}

		@Override
		public void onStop(@NonNull final MediaReaper reaper) {
			if (DEBUG) Log.v(TAG, "onStop:");
			// FIXME エンコーダー破棄したほうがいい？
			mRequestStop = true;
		}

		@Override
		public void onError(@NonNull final MediaReaper reaper, final Throwable t) {
			if (DEBUG) Log.w(TAG, t);
		}
	};

	/**
	 * 子クラスでOverrideした時でもEncoder#releaseを呼び出すこと
	 */
	@CallSuper
	@Override
	public  void release() {
		if (DEBUG) Log.d(TAG, "release:");
		mRecorder = null;
		if (mIsEncoding) {
			try {
				mListener.onStopEncode(this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, "release: failed onStopped", e);
			}
		}
		mIsEncoding = false;
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
		if (mRecorder != null) {
			try {
   				if (DEBUG) Log.v(TAG, "release: call Recorder#stop");
				mRecorder.stop(this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, "release: failed stopping Recorder", e);
			}
		}
		try {
			mListener.onDestroy(this);
		} catch (final Exception e) {
			if (DEBUG) Log.e(TAG, "release: onDestroy failed", e);
		}
		mRecorder = null;
	}

	/**
	 * ストリーミング終了指示を送る
	 */
	@Override
	public  void signalEndOfInputStream() {
//		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
        // MediaCodec#signalEndOfInputStreamはBUFFER_FLAG_END_OF_STREAMフラグを付けて
        // 空のバッファをセットするのと等価である
    	// ・・・らしいので空バッファを送る。encode内でBUFFER_FLAG_END_OF_STREAMを付けてセットする
        encode(null, getInputPTSUs());
	}

	@Override
	public boolean isEncoding() {
        synchronized (mSync) {
            return mIsEncoding;
        }
	}

    /**
     * バイト配列をエンコードする場合
     * @param buffer
     * @param presentationTimeUs [マイクロ秒]
     */
	@Override
	public  void encode(final ByteBuffer buffer, final long presentationTimeUs) {
		synchronized (mSync) {
			if (!mIsEncoding || mRequestStop) return;
			if (mMediaCodec == null) return;
		}

		final int length = buffer != null ? buffer.remaining() : 0;
		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsEncoding) {
	        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
	        if (inputBufferIndex >= 0) {
	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            if ((buffer != null) && (length > 0)) {
	            	buffer.clear();
	            	buffer.position(length);
	            	buffer.flip();
	            	inputBuffer.put(buffer);
	            }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
	            if (length <= 0) {
	            	// エンコード要求サイズが0の時はEOSを送信
//	            	mIsEOS = true;
//	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
	            } else {
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
	            		presentationTimeUs, 0);
	            }
	            break;
	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//	        	// 送れるようになるまでループする
//	        	// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
	        	frameAvailableSoon();	// drainが詰まってると予想されるのでdrain要求をする
	        }
        }
    }

	/**
	 * コーデックからの出力フォーマットを取得してnative側へ引き渡してRecorderをスタートさせる
	 */
	protected boolean startRecorder(@NonNull final IRecorder recorder, @NonNull final MediaFormat outFormat) {
		if (DEBUG) Log.i(TAG, "startRecorder:outFormat=" + outFormat + ",state=" + recorder.getState());
		if (recorder.getState() != IRecorder.STATE_STARTING) {
			for (int i = 0; i < 10; i++) {
				if (recorder.getState() == IRecorder.STATE_STARTING) {
					break;
				}
				if (DEBUG) Log.v(TAG, "startRecorder:wait");
				synchronized (mSync) {
					try {
						mSync.wait(10);
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
		}
		if (recorder.getState() == IRecorder.STATE_STARTING) {
			mTrackIndex = recorder.addTrack(this, outFormat);
			if (mTrackIndex >= 0) {
				if (!recorder.start(this)) {
					// startは全てのエンコーダーがstartを呼ぶまで返ってこない
					// falseを返した時はmuxerをスタート出来てない。何らかの異常
		       		Log.w(TAG, "startRecorder: failed to start recorder mTrackIndex=" + mTrackIndex);
				}
			} else {
				if (DEBUG) Log.w(TAG, "startRecorder: failed to addTrack: mTrackIndex=" + mTrackIndex);
				recorder.removeEncoder(this);
			}
		}
       	return recorder.isStarted();
	}

	protected void stopRecorder(final IRecorder recorder) {
   		mIsEncoding = false;
	}

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
    @SuppressLint("NewApi")
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
/*		if (result <= prevOutputPTSUs) {
			Log.w(TAG, "input pts smaller than previous output PTS");
			result = (prevOutputPTSUs - result) + result;
		} */
		if (result <= prevInputPTSUs) {
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
    }

}
