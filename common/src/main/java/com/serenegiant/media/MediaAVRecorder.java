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

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.serenegiant.system.StorageUtils;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.system.StorageInfo;
import com.serenegiant.utils.UriHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * Encoderを使って録音録画するためのヘルパー用Recorder実装
 */
@SuppressLint("NewApi")
public class MediaAVRecorder extends Recorder {
//	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = MediaAVRecorder.class.getSimpleName();

	protected final int mSaveTreeId;	// 0以外: SFAを使った出力を行うかどうか

	protected String mOutputPath;
	protected DocumentFile mOutputFile;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param output
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final DocumentFile output) throws IOException {

		this(context, callback, null, null, output);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param output
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
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
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		@NonNull final DocumentFile output) throws IOException {

		super(context, callback, config, factory);
		mSaveTreeId = 0;
		mOutputFile = output;
		mOutputPath = UriHelper.getPath(context, output.getUri());
		setupMuxer(context, output);
	}

//--------------------------------------------------------------------------------
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
			info = mOutputFile != null
				? StorageUtils.getStorageInfo(context, mOutputFile) : null;
			if ((info != null) && (info.totalBytes != 0)) {
				return ((info.freeBytes/ (float)info.totalBytes) < FileUtils.FREE_RATIO)
					|| (info.freeBytes < FileUtils.FREE_SIZE);
			}
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
		// チェックできないときは常にfalseを返す
		return false;
	}

	protected void setupMuxer(
		@NonNull final Context context,
		@NonNull final DocumentFile output) throws IOException {

		setMuxer(getMuxerFactory().createMuxer(context, getConfig().useMediaMuxer(), output));
	}

}
