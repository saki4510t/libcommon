package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2016-2025 saki t_saki@serenegiant.com
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

import android.content.Context;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * MP4自動分割録画用IRecorder実装
 * MediaAVSplitRecorderはAPI29/Android10以降の対象範囲別ストレージだと使えないので新たに作成
 */
public class MediaAVSplitRecorderV2 extends Recorder {
	private static final boolean DEBUG = false; // FIXME set false on production
	private static final String TAG = MediaAVSplitRecorderV2.class.getSimpleName();

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param outputDir 出力先ディレクトリを示すDocumentFile
	 * 					API>=29の場合はSAFのツリードキュメントかnullでないとだめ
	 * 					nullを指定した場合はFileUtils.getCaptureDir(API>=29の場合は対象範囲別ストレージを使ってEnvironment.DIRECTORY_MOVIES)の下に
	 * 					MediaAVSplitRecorderV2生成時刻文字列をフォルダ名として追加した上で各セグメント毎の録画ファイルが生成される
	 * @param splitSize
	 * @throws IOException
	 */
	public MediaAVSplitRecorderV2(
		@NonNull final Context context,
		final RecorderCallback callback,
		@Nullable final DocumentFile outputDir,
		final long splitSize) throws IOException {

		this(context, callback, null, null, null, outputDir, splitSize);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param config
	 * @param factory
	 * @param outputDir 出力先ディレクトリを示すDocumentFile
	 * 					API>=29の場合はSAFのツリードキュメントかnullでないとだめ
	 * 					nullを指定した場合はFileUtils.getCaptureDir(API>=29の場合は対象範囲別ストレージを使ってEnvironment.DIRECTORY_MOVIES)の下に
	 * 					MediaAVSplitRecorderV2生成時刻文字列をフォルダ名として追加した上で各セグメント毎の録画ファイルが生成される
	 * @param queue
	 * @param splitSize
	 * @throws IOException
	 */
	public MediaAVSplitRecorderV2(
		@NonNull final Context context,
		final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		@Nullable final IMediaQueue<RecycleMediaData> queue,
		@Nullable final DocumentFile outputDir,
		final long splitSize) throws IOException {

		super(context, callback, config, factory);
		setMuxer(new MediaSplitMuxerV2(context,
			outputDir, getConfig(), getMuxerFactory(),
			queue, splitSize));
	}

	@Override
	public boolean check() {
		return false;
	}

	@Nullable
	@Override
	public DocumentFile getOutputFile() {
		return null;
	}

}
