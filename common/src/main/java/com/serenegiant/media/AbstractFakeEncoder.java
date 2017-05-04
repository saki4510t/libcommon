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
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractFakeEncoder implements Encoder {
	private static final boolean DEBUG = true;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AbstractFakeEncoder.class.getSimpleName();

	private static final class FrameData {
		private final WeakReference<AbstractFakeEncoder> mWeakParent;
		public ByteBuffer data;
		public long presentationTimeUs;
		public int offset;
		public int size;
		public int flags;
		
		FrameData(final AbstractFakeEncoder parent, final int frameSize) {
			mWeakParent = new WeakReference<AbstractFakeEncoder>(parent);
			resize(frameSize);
		}
		
		void resize(final int newSize) {
			final int sz = newSize > 0 ? newSize : DEFAULT_FRAME_SZ;
			if ((data == null) || (data.capacity() < sz)) {
				data = ByteBuffer.allocateDirect(sz)
					.order(ByteOrder.nativeOrder());
			}
			data.clear();
		}
		
		void set(@Nullable final ByteBuffer buffer, final int offset, final int size,
			final long presentationTimeUs, final int flags) {
			
			resize(size);
			if (buffer != null) {
				buffer.position(offset + size);
				buffer.flip();
				buffer.position(offset);
				data.put(buffer);
			}
			data.flip();
			this.presentationTimeUs = presentationTimeUs;
			this.offset = offset;
			this.size = size;
			this.flags = flags;
		}
	}
	
	/**
	 * フレームプールの最大数
	 */
	private static final int MAX_POOL_SZ = 6;
	/**
	 * フレームキューの最大数
	 */
	private static final int MAX_QUEUE_SZ = 4;
	/**
	 * デフォルトのフレームサイズ
	 */
	private static final int DEFAULT_FRAME_SZ = 1024;
	
	private static final long MAX_WAIT_FRAME_MS = 20;
	
	/**
	 * エンコード実行中フラグ
	 */
	protected volatile boolean mIsCapturing;
	/**
	 * 終了要求フラグ(新規エンコード禁止フラグ)
	 */
	protected volatile boolean mRequestStop;
	/**
	 * ファイルへの出力中フラグ
	 */
	protected volatile boolean mRecorderStarted;
	/**
	 * 終了フラグ
	 */
	protected boolean mIsEOS;
	/**
	 * トラックインデックス
	 */
	protected int mTrackIndex;
	/**
	 * Recorderオブジェクトへの参照
	 */
	private IRecorder mRecorder;
	/**
	 * フラグの排他制御用
	 */
	protected final Object mSync = new Object();
	/**
	 * エンコーダーイベントコールバックリスナー
	 */
	private final EncoderListener mListener;
	/**
	 * MIME
	 */
	protected final String MIME_TYPE;
	/**
	 * フレームプール
	 */
	private final List<FrameData> mPool = new ArrayList<FrameData>();
	/**
	 * フレームキュー
	 */
	private final LinkedBlockingQueue<FrameData> mFrameQueue = new LinkedBlockingQueue<FrameData>(MAX_QUEUE_SZ);
	/**
	 * デフォルトのフレームサイズ
	 */
	private final int mDefaultFrameSz;
	/**
	 * フレーム情報(ワーク用)
	 */
	private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

	public AbstractFakeEncoder(final String mime_type, final IRecorder recorder,
		final EncoderListener listener) {
		
		this(mime_type, recorder, listener, DEFAULT_FRAME_SZ);
	}
	
	public AbstractFakeEncoder(final String mime_type, final IRecorder recorder,
		final EncoderListener listener, final int defaultFrameSz) {
		
		if (listener == null) throw new NullPointerException("EncodeListener is null");
		if (recorder == null) throw new NullPointerException("IMuxer is null");
		MIME_TYPE = mime_type;
		mRecorder = recorder;
		mListener = listener;
		mDefaultFrameSz = defaultFrameSz;
		recorder.addEncoder(this);
	}

	/**
	 * 出力用のMuxerWrapperを返す
	 * @return
	 */
	public IRecorder getRecorder() {
		return mRecorder;
	}

	/**
	 * 出力ファイルのパスを返す
	 * @return
	 */
	@Override
	public String getOutputPath() {
		return mRecorder != null ? mRecorder.getOutputPath() : null;
	}

	@Override
	protected void finalize() throws Throwable {
//		if (DEBUG) Log.v(TAG, "finalize:");
		mRecorder = null;
		release();
		super.finalize();
	}

	/**
	 * 子クラスでOverrideした時でもEncoder#releaseを呼び出すこと
	 */
	@Override
	public  void release() {
//		if (DEBUG) Log.d(TAG, "release:");
		if (mIsCapturing) {
			try {
				mListener.onStopEncode(this);
			} catch (final Exception e) {
//				Log.e(TAG, "failed onStopped", e);
			}
		}
		mIsCapturing = false;
        if (mRecorderStarted) {
        	mRecorderStarted = false;
        	if (mRecorder != null) {
       			try {
//    				if (DEBUG) Log.v(TAG, "call MuxerWrapper#stop");
       				mRecorder.stop(this);
    			} catch (final Exception e) {
//    				Log.e(TAG, "failed stopping muxer", e);
    			}
//				mRecorder = null;
        	}
        }
		try {
			mListener.onDestroy(this);
		} catch (final Exception e) {
//			Log.e(TAG, "destroy:", e);
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
    	final FrameData frame = obtain(0);
    	frame.set(null, 0, 0, getInputPTSUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    	offer(frame);
	}

	/**
	 * このクラスではサポートしていない
	 * 代わりに#queueFrameへエンコード済みのフレームを渡すこと
	 * encodeはnative側からアクセスするので変更時は注意
	 * @param buffer
	 */
	@Override
	public void encode(final ByteBuffer buffer) {
		throw new UnsupportedOperationException("can not call encode");
	}
	
	/**
	 * このクラスではサポートしていない
	 * 代わりに#queueFrameへエンコード済みのフレームを渡すこと
	 * @param buffer
	 * @param length
	 * @param presentationTimeUs
	 */
	@Override
	public void encode(final ByteBuffer buffer, final int length,
		final long presentationTimeUs) {
		throw new UnsupportedOperationException("can not call encode");
	}
	
	/**
	 * フレームデータをキューに追加する
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 * @param flags
	 * @return true: 正常にキューに追加できた
	 * @throws IllegalStateException
	 */
	public boolean queueFrame(@Nullable final ByteBuffer buffer, final int offset, final int size,
		final long presentationTimeUs, final int flags) throws IllegalStateException {
		
		synchronized (mSync) {
			if (!mIsCapturing) {
				throw new IllegalStateException();
			}
		}
		final FrameData frame = obtain(size);
		frame.set(buffer, offset, size, presentationTimeUs, flags);
		return offer(frame);
	}
	
	@Override
	public boolean isCapturing() {
        synchronized (mSync) {
            return mIsCapturing;
        }
	}

	/**
	 * エンコード開始要求(Recorderから呼び出される)
	 */
	@Override
	public void start() {
//		if (DEBUG) Log.v(TAG, "start");
		synchronized (mSync) {
			if (!mIsCapturing) {
				initPool();
				mIsCapturing = true;
				mRequestStop = false;
				// エンコーダースレッドを生成
				new Thread(mDrainTask, getClass().getSimpleName()).start();
				try {
					mSync.wait();	// エンコーダースレッド起床待ち
				} catch (final InterruptedException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * エンコーダ終了要求(Recorderから呼び出される)
	 */
	@Override
	public void stop() {
//		if (DEBUG) Log.v(TAG, "stop");
		synchronized (mSync) {
			if (/*!mIsCapturing ||*/ mRequestStop) {
				return;
			}
			// 終了要求
			signalEndOfInputStream();
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
//		if (DEBUG) Log.v(TAG, "AbstractEncoder#frameAvailableSoon");
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) {
				return;
			}
			mSync.notifyAll();
		}
	}

//================================================================================
	/**
	 * prepareの最後に呼び出すこと
	 * @param source
	 * @param captureFormat
	 */
	protected void callOnStartEncode(final Surface source,
		final int captureFormat, final boolean mayFail) {
		
//		if (DEBUG) Log.v(TAG, "callOnStartEncode:mListener=" + mListener);
		try {
			mListener.onStartEncode(this, source, captureFormat, mayFail);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

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

//================================================================================
	/**
	 * フレームプールを初期化する
	 */
	protected void initPool() {
		mFrameQueue.clear();
		synchronized (mPool) {
			mPool.clear();
			for (int i = 0; i < MAX_POOL_SZ; i++) {
				mPool.add(new FrameData(this, mDefaultFrameSz));
			}
		}
	}
	
	private int cnt = 0;
	
	/**
	 * フレームプールからフレームを取得する
	 * @param newSize
	 * @return
	 */
	protected FrameData obtain(final int newSize) {
		FrameData result;
		synchronized (mPool) {
			if (mPool.isEmpty()) {
				cnt++;
				if (DEBUG) Log.v(TAG, "obtain:create new FrameData, total=" + cnt);
				result = new FrameData(this, mDefaultFrameSz);
			} else {
				result = mPool.remove(mPool.size() - 1);
				result.resize(newSize);
			}
		}
		return result;
	}
	
	/**
	 * フレームキューにフレームデータを追加する
	 * @param frame
	 */
	protected boolean offer(@NonNull final FrameData frame) {
		boolean result = mFrameQueue.offer(frame);
		if (!result) {
			if (DEBUG) Log.w(TAG, "offer:先頭を破棄する");
		    final FrameData head = mFrameQueue.poll();
			result = mFrameQueue.offer(frame);
			if (!result) {
				if (DEBUG) Log.w(TAG, "offer:frame dropped");
			}
			if (head != null) {
				recycle(head);
			}
		}
		return result;
	}
	
	/**
	 * フレームキューからフレームデータを取り出す
	 * フレームキューが空ならブロックする
	 * @param waitTimeMs 最大待ち時間[ミリ秒]
	 * @return
	 */
	protected FrameData waitFrame(final long waitTimeMs) {
		FrameData result = null;
		try {
			result = mFrameQueue.poll(waitTimeMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
		return result;
	}
	
	/**
	 * フレームプールにフレームを戻す
	 * @param frame
	 */
	protected void recycle(@NonNull final FrameData frame) {
		synchronized (mPool) {
			if (mPool.size() < MAX_POOL_SZ) {
				mPool.add(frame);
			} else {
				cnt--;
			}
		}
	}
//================================================================================

	private final Runnable mDrainTask = new Runnable() {
		@Override
		public void run() {
			drainLoop();
		}
	};
	
	private void drainLoop() {
		synchronized (mSync) {
			mRequestStop = false;
			mSync.notify();
		}
		for ( ; mIsCapturing ; ) {
			final FrameData frame = waitFrame(MAX_WAIT_FRAME_MS);
			if (frame != null) {
				try {
					handleFrame(frame);
				} finally {
					recycle(frame);
				}
			} else if (mRequestStop) {
				break;
			}
		} // end of while
		synchronized (mSync) {
			mRequestStop = true;
			mIsCapturing = false;
		}
	}
	
	protected void handleFrame(final FrameData frame) {
		final IRecorder recorder = mRecorder;
		if (recorder == null) {
//			throw new NullPointerException("muxer is unexpectedly null");
//			Log.w(TAG, "muxer is unexpectedly null");
			return;
		}
		mBufferInfo.set(0, frame.size, frame.presentationTimeUs, frame.flags);
		if (!mRecorderStarted && ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
//			if (DEBUG) Log.d(TAG, "handleFrame:BUFFER_FLAG_CODEC_CONFIG");
			// csd-0とcsd-1が同時に来ているはずなので分離してセットする
			final byte[] tmp = new byte[mBufferInfo.size];
			frame.data.position(0);
			frame.data.get(tmp, 0, mBufferInfo.size);
			frame.data.position(0);
			final int ix0 = byteComp(tmp, 0, START_MARK, START_MARK.length);
			final int ix1 = byteComp(tmp, ix0+1, START_MARK, START_MARK.length);
//			if (DEBUG) Log.i(TAG, "ix0=" + ix0 + ",ix1=" + ix1);
			final MediaFormat outFormat = createOutputFormat(tmp, mBufferInfo.size, ix0, ix1);
			if (!startRecorder(recorder, outFormat)) {
				return;
			}
			mBufferInfo.size = 0;
		}
		
		if (mRecorderStarted && (mBufferInfo.size != 0)) {
			// エンコード済みバッファにデータが入っている時・・・待機カウンタをクリア
			if (mRecorderStarted) {
				// ファイルに出力(presentationTimeUsを調整)
				try {
					mBufferInfo.presentationTimeUs = getNextOutputPTSUs(mBufferInfo.presentationTimeUs);
					recorder.writeSampleData(mTrackIndex, frame.data, mBufferInfo);
//					prevOutputPTSUs = mBufferInfo.presentationTimeUs;
               	 } catch (final TimeoutException e) {
//					if (DEBUG) Log.v(TAG, "最大録画時間を超えた", e);
					recorder.stopRecording();
				} catch (final Exception e) {
//					if (DEBUG) Log.w(TAG, e);
					recorder.stopRecording();
				}
			}
		}
		if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			// ストリーム終了指示が来た時
			stopRecorder(recorder);
		}
//		if (DEBUG) Log.v(TAG, "handleFrame:finished");
	}

	protected abstract MediaFormat createOutputFormat(byte[] csd, int size, int ix0, int ix1);

	/**
	 * コーデックからの出力フォーマットを取得してnative側へ引き渡してRecorderをスタートさせる
	 */
	public boolean startRecorder(final IRecorder recorder, final MediaFormat outFormat) {
//		if (DEBUG) Log.i(TAG, "startMuxer:outFormat=" + outFormat);
		mTrackIndex = recorder.addTrack(this, outFormat);
		if (mTrackIndex >= 0) {
			mRecorderStarted = true;
			if (!recorder.start(this)) {
				// startは全てのエンコーダーがstartを呼ぶまで返ってこない
				// falseを返した時はmuxerをスタート出来てない。何らかの異常
//				Log.e(TAG, "failed to start muxer mTrackIndex=" + mTrackIndex);
			}
    	} else {
//			Log.e(TAG, "failed to addTrack: mTrackIndex=" + mTrackIndex);
			recorder.removeEncoder(this);
		}
		return recorder.isStarted();
	}

	public void stopRecorder(final IRecorder recorder) {
		mRecorderStarted = mIsCapturing = false;
	}

	/**
	 * 前回Recorderに書き込んだ際のpresentationTimeUs
	 */
	private long prevOutputPTSUs = -1;
//	private long firstOutputPTSUs = -1;

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * System.nanoTime()を1000で割ってマイクロ秒にしただけ(切り捨て)
	 * @return
	*/
	protected long getInputPTSUs() {
		long result = System.nanoTime() / 1000L;
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

	/**
	 * Muxerの今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
	protected long getNextOutputPTSUs(long presentationTimeUs) {
//		long result = System.nanoTime() / 1000L;
//		 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
/*		if (result < prevOutputPTSUs)
			result = (prevOutputPTSUs - result) + result; */
		if (presentationTimeUs <= prevOutputPTSUs) {
			presentationTimeUs = prevOutputPTSUs + 9643;
		}
		prevOutputPTSUs = presentationTimeUs;
		return presentationTimeUs;
	}

	/**
	 * codec specific dataの先頭マーカー
	 */
	protected static final byte[] START_MARK = { 0, 0, 0, 1, };
	/**
	 * byte[]を検索して一致する先頭インデックスを返す
	 * @param array 検索されるbyte[]
	 * @param search 検索するbyte[]
	 * @param len 検索するバイト数
	 * @return 一致した先頭位置、一致しなければ-1
	 */
	protected final int byteComp(@NonNull final byte[] array, final int offset, @NonNull final byte[] search, final int len) {
		int index = -1;
		final int n0 = array.length;
		final int ns = search.length;
		if ((n0 >= offset + len) && (ns >= len)) {
			for (int i = offset; i < n0 - len; i++) {
				int j = len - 1;
				while (j >= 0) {
					if (array[i + j] != search[j]) break;
					j--;
				}
				if (j < 0) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

}
