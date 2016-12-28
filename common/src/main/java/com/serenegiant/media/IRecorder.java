package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
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

import java.io.IOException;

import android.view.Surface;

public interface IRecorder {

	public interface RecorderCallback {
		public void onPrepared(IRecorder recorder);
		public void onStarted(IRecorder recorder);
		public void onStopped(IRecorder recorder);
		public void onError(Exception e);
	}

	/**
	 * キャプチャしていない
	 */
	public static final int STATE_UNINITIALIZED = 0;
	/**
	 * キャプチャ初期化済(Muxerセット済)
	 */
	public static final int STATE_INITIALIZED = 1;
	/**
	 * キャプチャ準備完了(prepare済)
	 */
	public static final int STATE_PREPARED = 2;
	/**
	 * キャプチャ開始中
	 */
	public static final int STATE_STARTING = 3;
	/**
	 * キャプチャ中
	 */
	public static final int STATE_STARTED = 4;
	/**
	 * キャプチャ停止要求中
	 */
	public static final int STATE_STOPPING = 5;
	/**
	 * キャプチャ終了
	 */
//	public static final int STATE_STOPPED = 6;

	public abstract void setMuxer(IMuxer muxer);

	/**
	 * Encoderの準備
	 * 割り当てられているMediaEncoderの下位クラスのインスタンスの#prepareを呼び出す
	 * @throws IOException
	 */
	public abstract void prepare();

	/**
	 * キャプチャ開始要求
	 * 割り当てられているEncoderの下位クラスのインスタンスの#startRecordingを呼び出す
	 */
	public abstract void startRecording() throws IllegalStateException;

	/**
	 * キャプチャ終了要求
	 * 割り当てられているEncoderの下位クラスの#stopRecordingを呼び出す
	 */
	public abstract void stopRecording();

	public abstract Surface getInputSurface();

	public abstract com.serenegiant.video.Encoder getVideoEncoder();

	public abstract com.serenegiant.video.Encoder getAudioEncoder();

	/**
	 * Muxerが出力開始しているかどうかを返す
	 * @return
	 */
	public abstract boolean isStarted();

	/**
	 * エンコーダーの初期化が終わって書き込み可能になったかどうかを返す
	 * @return
	 */
	public abstract boolean isReady();

	/**
	 * 終了処理中かどうかを返す
	 * @return
	 */
	public abstract boolean isStopping();

	/**
	 * 終了したかどうかを返す
	 * @return
	 */
	public abstract boolean isStopped();

	public abstract int getState();

	public abstract IMuxer getMuxer();

	public abstract String getOutputPath();

	public abstract void frameAvailableSoon();

	/**
	 * 関連するリソースを開放する
	 */
	public abstract void release();

}