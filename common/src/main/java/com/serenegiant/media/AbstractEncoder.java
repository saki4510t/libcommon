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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

/**
 * MediaCodecを使ったエンコーダ
 * MediaEncoderへIRecorder関係の処理を追加
 */
public abstract class AbstractEncoder extends MediaEncoder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractEncoder.class.getSimpleName();

    /**
     * トラックインデックス
     */
    protected int mTrackIndex;
    /**
     * Recorderオブジェクトへの参照
     */
    @NonNull
    private final IRecorder mRecorder;

//********************************************************************************

	/**
	 * コンストラクタ
	 * @param mimeType
	 * @param recorder
	 * @param listener
	 */
    public AbstractEncoder(@NonNull final String mimeType,
    	@NonNull final IRecorder recorder,
    	@NonNull final EncoderListener2 listener) {

		super(mimeType, listener);
    	mRecorder = recorder;
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

	@NonNull
	@Override
	protected MediaReaper.ReaperListener getReaperListener() {
		return mReaperListener;
	}

//--------------------------------------------------------------------------------
	/**
	 * エンコード済みデータを書き込む
	 * @param byteBuf
	 * @param bufferInfo
	 */
	protected void writeSampleData(
		@NonNull final ByteBuffer byteBuf,
		@NonNull final MediaCodec.BufferInfo bufferInfo) {

//		if (DEBUG) Log.v(TAG, "writeSampleData:");
		if (isReady() && (mRecorder != null)) {
			mRecorder.writeSampleData(mTrackIndex, byteBuf, bufferInfo);
		}
	}

	/**
	 * エンコーダーのMediaFormatが変更された時
	 * @param format
	 */
	protected void onOutputFormatChanged(
		@NonNull final MediaFormat format) {

		if (DEBUG) Log.v(TAG, "onOutputFormatChanged:" + format);
		if (isReady() && (mRecorder != null)) {
			startRecorder(mRecorder, format);
		}
	}

	private final MediaReaper.ReaperListener mReaperListener
		= new MediaReaper.ReaperListener() {
		@Override
		public void writeSampleData(
			@NonNull final MediaReaper reaper,
			@NonNull final ByteBuffer byteBuf,
			@NonNull final MediaCodec.BufferInfo bufferInfo) {

			AbstractEncoder.this.writeSampleData(byteBuf, bufferInfo);
		}

		@Override
		public void onOutputFormatChanged(
			@NonNull final MediaReaper reaper,
			@NonNull final MediaFormat format) {

			AbstractEncoder.this.onOutputFormatChanged(format);
		}

		@Override
		public void onStop(@NonNull final MediaReaper reaper) {
			if (DEBUG) Log.v(TAG, "onStop:");
			// FIXME エンコーダー破棄したほうがいい？
			requestStop();
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
		requestStop();
		super.release();
		if (!mRecorder.isStopped()) {
			try {
   				if (DEBUG) Log.v(TAG, "release: call Recorder#stop");
				mRecorder.stop(this);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, "release: failed stopping Recorder", e);
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
		requestStop();
	}

}
