package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

@SuppressLint("NewApi")
public class MediaAVRecorder extends AbstractMediaAVRecorder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaAVRecorder.class.getSimpleName();

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param ext 出力ファイルの拡張子
	 * @param saveTreeId
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String ext, final int saveTreeId)
			throws IOException {

		super(context, callback, null, ext, saveTreeId);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param prefix
	 * @param _ext
	 * @param saveTreeId
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String prefix, final String _ext, final int saveTreeId)
			throws IOException {

		super(context, callback, prefix, _ext, saveTreeId);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param saveTreeId
	 * @param dirs savedTreeIdが示すディレクトリからの相対ディレクトリパス, nullならsavedTreeIdが示すディレクトリ
	 * @param fileName
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final int saveTreeId, @Nullable final String dirs, @NonNull final String fileName)
			throws IOException {
		
		super(context, callback, saveTreeId, dirs, fileName);
	}
	
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
		
		super(context, callback, output);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param outputPath
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final String outputPath)
			throws IOException {

		super(context, callback, outputPath);
	}
	
	@Override
	protected void setupMuxer(final int fd) throws IOException {
		setMuxer(createMuxer(fd));
	}
	
	@Override
	protected void setupMuxer(@NonNull final String output) throws IOException {
		setMuxer(createMuxer(output));
	}
	
	@Override
	protected void setupMuxer(
		@NonNull final Context context,
		@NonNull final DocumentFile output) throws IOException {

		setMuxer(createMuxer(context, output));
	}
	
}
