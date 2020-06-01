package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.SAFUtils;
import com.serenegiant.system.StorageUtils;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.system.StorageInfo;
import com.serenegiant.utils.UriHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

@SuppressLint("NewApi")
public class MediaAVRecorder extends Recorder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaAVRecorder.class.getSimpleName();

	protected final int mSaveTreeId;	// 0以外: SFAを使った出力を行うかどうか

	protected String mOutputPath;
	protected DocumentFile mOutputFile;

//--------------------------------------------------------------------------------
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param ext 出力ファイルの拡張子
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String ext, final int saveTreeId) throws IOException {

		this(context, callback, null, null,null, ext, saveTreeId);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param ext 出力ファイルの拡張子
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final IMuxer.IMuxerFactory factory,
		final String ext, final int saveTreeId) throws IOException {

		this(context, callback, null, null, ext, saveTreeId);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param prefix
	 * @param _ext
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String prefix, final String _ext, final int saveTreeId)
			throws IOException {

		this(context, callback, null, null, prefix, _ext, saveTreeId);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param prefix
	 * @param _ext
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final IMuxer.IMuxerFactory factory,
		final String prefix, final String _ext, final int saveTreeId) throws IOException {

		this(context, callback, null, factory, prefix, _ext, saveTreeId);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param dirs savedTreeIdが示すディレクトリからの相対ディレクトリパス, nullならsavedTreeIdが示すディレクトリ
	 * @param fileName
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final int saveTreeId, @Nullable final String dirs, @NonNull final String fileName)
			throws IOException {
		
		this(context, callback, null, null, saveTreeId, dirs, fileName);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param dirs savedTreeIdが示すディレクトリからの相対ディレクトリパス, nullならsavedTreeIdが示すディレクトリ
	 * @param fileName
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final IMuxer.IMuxerFactory factory,
		final int saveTreeId, @Nullable final String dirs, @NonNull final String fileName) throws IOException {

		this(context, callback, null, factory, saveTreeId, dirs, fileName);
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
	 * @param outputPath
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final String outputPath) throws IOException {

		this(context, callback, null, null, outputPath);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param factory
	 * @param outputPath
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final IMuxer.IMuxerFactory factory,
		@NonNull final String outputPath) throws IOException {

		this(context, callback, null, factory, outputPath);
	}

//--------------------------------------------------------------------------------
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param prefix
	 * @param _ext
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		final String prefix, final String _ext, final int saveTreeId) throws IOException {

		super(context, callback, config, factory);
		mSaveTreeId = saveTreeId;
		String ext = _ext;
		if (TextUtils.isEmpty(ext)) {
			ext = ".mp4";
		}
		if ((saveTreeId != 0) && SAFUtils.hasPermission(context, saveTreeId)) {
			mOutputPath = FileUtils.getCaptureFile(context,
				Environment.DIRECTORY_MOVIES, prefix, ext, saveTreeId).toString();
			final String file_name = (TextUtils.isEmpty(prefix)
				? FileUtils.getDateTimeString()
				: prefix + FileUtils.getDateTimeString()) + ext;
//			final int fd = SAFUtils.createStorageFileFD(context, saveTreeId, "*/*", file_name);
			final int fd = SAFUtils.getFd(context, saveTreeId, null, "*/*", file_name).getFd();
			setupMuxer(fd);
		} else {
			try {
				mOutputPath = FileUtils.getCaptureFile(context,
					Environment.DIRECTORY_MOVIES, prefix, ext, 0).toString();
			} catch (final Exception e) {
				throw new IOException("This app has no permission of writing external storage");
			}
			setupMuxer(mOutputPath);
		}
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param config
	 * @param saveTreeId 0: SAFを使わない, それ以外: SAFのツリーIDとみなして処理を試みる
	 * @param dirs savedTreeIdが示すディレクトリからの相対ディレクトリパス, nullならsavedTreeIdが示すディレクトリ
	 * @param fileName
	 * @param factory
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		final int saveTreeId, @Nullable final String dirs, @NonNull final String fileName) throws IOException {

		super(context, callback, config, factory);
		mSaveTreeId = saveTreeId;
		if ((saveTreeId > 0) && SAFUtils.hasPermission(context, saveTreeId)) {
			DocumentFile tree = SAFUtils.getFile(context,
				saveTreeId, dirs, "*/*", fileName);
			if (tree != null) {
				mOutputPath = UriHelper.getPath(context, tree.getUri());
				final ParcelFileDescriptor pfd
					= context.getContentResolver().openFileDescriptor(
						tree.getUri(), "rw");
				try {
					if (pfd != null) {
						setupMuxer(pfd.getFd());
						return;
					} else {
						// ここには来ないはずだけど
						throw new IOException("could not create ParcelFileDescriptor");
					}
				} catch (final Exception e) {
					if (pfd != null) {
						pfd.close();
					}
					throw e;
				}
			}
		}
		// フォールバックはしない
		throw new IOException("path not found/can't write");
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

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param config
	 * @param factory
	 * @param outputPath
	 * @throws IOException
	 */
	public MediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@Nullable final VideoConfig config,
		@Nullable final IMuxer.IMuxerFactory factory,
		@NonNull final String outputPath) throws IOException {

		super(context, callback, config, factory);
		mSaveTreeId = 0;
		mOutputPath = outputPath;
		if (TextUtils.isEmpty(outputPath)) {
			try {
				mOutputPath = FileUtils.getCaptureFile(context,
					Environment.DIRECTORY_MOVIES, null, ".mp4", 0).toString();
			} catch (final Exception e) {
				throw new IOException("This app has no permission of writing external storage");
			}
		}
		setupMuxer(mOutputPath);
	}

//--------------------------------------------------------------------------------
	@Nullable
	@Override
	public String getOutputPath() {
		return mOutputPath;
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
			info = mOutputFile != null
				? StorageUtils.getStorageInfo(context, mOutputFile) : null;
			if ((info != null) && (info.totalBytes != 0)) {
				return ((info.freeBytes/ (float)info.totalBytes) < FileUtils.FREE_RATIO)
					|| (info.freeBytes < FileUtils.FREE_SIZE);
			}
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
		return (context == null)
			|| ((mOutputFile == null)
				&& !FileUtils.checkFreeSpace(context,
					getConfig().maxDuration(), mStartTime, mSaveTreeId));
	}

	protected void setupMuxer(final int fd) throws IOException {
		setMuxer(getMuxerFactory().createMuxer(getConfig().useMediaMuxer(), fd));
	}

	protected void setupMuxer(@NonNull final String output) throws IOException {
		setMuxer(getMuxerFactory().createMuxer(getConfig().useMediaMuxer(), output));
	}

	protected void setupMuxer(
		@NonNull final Context context,
		@NonNull final DocumentFile output) throws IOException {

		setMuxer(getMuxerFactory().createMuxer(context, getConfig().useMediaMuxer(), output));
	}

}
