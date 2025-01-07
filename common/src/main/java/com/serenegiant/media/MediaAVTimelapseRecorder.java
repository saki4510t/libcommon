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
import android.content.Context;
import android.media.MediaCodec;
import android.util.Log;

import com.serenegiant.system.StorageInfo;
import com.serenegiant.system.StorageUtils;
import com.serenegiant.utils.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * Encoderを使ってタイムラプス録画するためのRecorder実装
 * 実際には入力映像のフレームレートにかかわらず固定のフレームレートになるRecorder実装。
 * 音声には対応しない(#addEncoderでUnsupportedOperationExceptionを投げる)
 * XXX 入力映像のフレームレートを動画ファイルのフレームレートよりも遅くすることでタイムラプス動画になる
 *     例えば動画ファイルのフレームレートを30fps(デフォルト)で、入力映像のフレームレートを5fpsにすると
 *     30÷5=6倍速になる
 */
@SuppressLint("NewApi")
public class MediaAVTimelapseRecorder extends Recorder {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = MediaAVTimelapseRecorder.class.getSimpleName();

	/**
	 * 録画フレームレートを指定しない場合のデフォルトのフレームインターバル, 30fps相当
	 */
	private static final long DEFAULT_FRAME_INTERVALS_US = 1000000L / 30;

	@NonNull
	private final DocumentFile mOutputFile;
	/**
	 * フレームインターバル(30fps)
	 */
	private long mFrameIntervalsUs = DEFAULT_FRAME_INTERVALS_US;
	/**
	 * 受け取ったフレーム数
	 * 音声用エンコーダーには対応していないので映像フレーム数のみを保持する
	 */
	private long mFrameCounts;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param output
	 * @throws IOException
	 */
	public MediaAVTimelapseRecorder(
		@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final DocumentFile output) throws IOException {

		this(context, callback, null, null, output);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param factory
	 * @param output
	 * @throws IOException
	 */
	public MediaAVTimelapseRecorder(
		@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final IMuxer.IMuxerFactory factory,
		@NonNull final DocumentFile output) throws IOException {

		this(context, callback, null, factory, output);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param config
	 * @param factory
	 * @param output
	 * @throws IOException
	 */
	public MediaAVTimelapseRecorder(
		@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		@NonNull final DocumentFile output) throws IOException {

		super(context, callback, config, factory);
		mOutputFile = output;
		@NonNull
		final VideoConfig _config = getConfig();
		if (_config.getCaptureFps().asDouble() >= 0) {
			mFrameIntervalsUs = Math.round(1000000L / _config.getCaptureFps().asDouble());
		}
		if (mFrameIntervalsUs < 0) {
			mFrameIntervalsUs = DEFAULT_FRAME_INTERVALS_US;
		}
		setMuxer(getMuxerFactory().createMuxer(context, getConfig().useMediaMuxer(), output));
	}

	/**
	 * Encoderを登録する
	 * Encoderの下位クラスのコンストラクタから呼び出される
	 * MediaAVTimelapseRecorderでは音声用エンコーダー(IAudioEncoder)をサポートしていない
	 * @param encoder
	 * @throws UnsupportedOperationException
	 */
	@Override
	public synchronized void addEncoder(final Encoder encoder) throws UnsupportedOperationException {
		if (encoder instanceof IAudioEncoder) {
			throw new UnsupportedOperationException("MediaAVTimelapseRecorder only support video encoder!");
		}
		super.addEncoder(encoder);
	}

	@Nullable
	@Override
	public DocumentFile getOutputFile() {
		return mOutputFile;
	}

	/**
	 * ディスクの空き容量をチェックして足りなければtrueを返す
	 * @return true: 空き容量が足りない
	 */
	@Override
	protected boolean check() {
		final Context context = requireContext();
		final StorageInfo info;
		try {
			info = StorageUtils.getStorageInfo(context, mOutputFile);
			if (info.totalBytes != 0) {
				return ((info.freeBytes/ (float)info.totalBytes) < FileUtils.FREE_RATIO)
					|| (info.freeBytes < FileUtils.FREE_SIZE);
			}
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
		// チェックできないときは常にfalseにする
		return false;
	}

	@Override
	public void writeSampleData(
		final int trackIndex,
		@NonNull final ByteBuffer byteBuf,
		@NonNull final MediaCodec.BufferInfo bufferInfo) {
		// 元のptsに関わらず受け取ったフレーム数に応じたptsで上書きする
		bufferInfo.presentationTimeUs = getInputPTSUs(trackIndex);
		super.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}

	/**
	 * リアルタイム録画時間の代わりに受け取ったフレーム数に応じたPTSを取得する
	 * @return
	 */
	private long getInputPTSUs(final int trackIndex) {
		if (DEBUG && ((mFrameCounts % 100) == 0))
			Log.v(TAG, "getInputPTSUs:" + mFrameCounts);
		if (mFrameIntervalsUs <= 0) {
			mFrameIntervalsUs = DEFAULT_FRAME_INTERVALS_US;
		}
		// リアルタイム録画時間の代わりに受け取ったフレーム数に応じてPTSをセットする
		return (mFrameCounts++) * mFrameIntervalsUs;
	}

}
