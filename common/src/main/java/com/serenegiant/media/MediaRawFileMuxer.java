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
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * Rawファイル形式でエンコードデータをファイルに書き出すためのIMuxer実装
 * 実際のファイルへの出力はMediaRawFilerWriterで行う。
 * 実際のmp4ファイルへの出力は別途PostMuxBuilderで行う。
 * 映像エンコード処理と音声エンコード処理とmux処理を同時に実行すると
 * 映像and/or音声が正常に記録されない端末がいくつかあるので、
 * 一度一時ファイルへ書き出しておいてエンコード終了後にmux処理を
 * 行うPostMux関係のクラス
 */
public class MediaRawFileMuxer implements IPostMuxer {
	private static final boolean DEBUG = false; // FIXME set false on production
	private static final String TAG = MediaRawFileMuxer.class.getSimpleName();

	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final VideoConfig mVideoConfig;
	/**
	 * MediaCodecの動画エンコーダーの設定
	 * 最終のmp4ファイル出力時に必要なため保持しておく
	 */
	private final MediaFormat mConfigFormatVideo;
	/**
	 * MediaCodecの音声エンコーダーの設定
	 * 最終のmp4ファイル出力時に必要なため保持しておく
	 */
	private final MediaFormat mConfigFormatAudio;
	/**
	 * mp4ファイルの出力先ファイル(DocumentFile)
	 */
	@NonNull
	private final DocumentFile mOutputDoc;
	/**
	 * 一時ファイルを保存するディレクトリ名
	 */
	@NonNull
	private final String mTempName;
	/** 実行中フラグ */
	private volatile boolean mIsRunning;
	private boolean mReleased;
	private int mLastTrackIndex = -1;
	/** エンコード済み映像データのrawファイル出力用 */
	private MediaRawFileWriter mVideoWriter;
	/** エンコード済み音声データのrawファイル出力用 */
	private MediaRawFileWriter mAudioWriter;
	/** トラックインデックスからMediaRawFileWriterを参照するための配列 */
	private final MediaRawFileWriter[] mMediaRawFileWriters = new MediaRawFileWriter[2];
	

	/**
	 * コンストラクタ
	 * @param context
	 * @param config
	 * @param output 最終出力先ファイル
	 * @param configFormatVideo
	 * @param configFormatAudio
	 */
	public MediaRawFileMuxer(@NonNull final Context context,
		@Nullable final VideoConfig config,
		@NonNull final DocumentFile output,
		@Nullable final MediaFormat configFormatVideo,
		@Nullable final MediaFormat configFormatAudio) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<Context>(context);
		mVideoConfig = config != null ? config : new VideoConfig();
		mOutputDoc = output;
		mTempName = FileUtils.getDateTimeString();
		mConfigFormatVideo = configFormatVideo;
		mConfigFormatAudio = configFormatAudio;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * 関連するリソースを破棄する
	 */
	@Override
	public void release() {
		synchronized (mSync) {
			if (!mReleased) {
				mReleased = true;
				if (DEBUG) Log.v(TAG, "release:");
				if (mVideoWriter != null) {
					mVideoWriter.release();
					mVideoWriter = null;
				}
				if (mAudioWriter != null) {
					mAudioWriter.release();
					mAudioWriter = null;
				}
				mMediaRawFileWriters[0] = mMediaRawFileWriters[1] = null;
			}
		}
		if (DEBUG) Log.v(TAG, "release:finished");
	}
	
	/**
	 * IMuxerを開始する
	 */
	@Override
	public void start() {
		if (DEBUG) Log.v(TAG, "start:");
		synchronized (mSync) {
			checkReleased();
			if (mIsRunning) {
				throw new IllegalStateException("already started");
			}
			if (mLastTrackIndex < 0) {
				throw new IllegalStateException("no track added");
			}
			mIsRunning = true;
		}
	}
	
	/**
	 * IMuxerを停止する
	 */
	@Override
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
		synchronized (mSync) {
			mIsRunning = false;
			mLastTrackIndex = 0;
		}
	}
	
	/**
	 * 一時rawファイルからmp4ファイルを生成する・
	 * mp4ファイル生成終了まで返らないので注意
	 */
	@Override
	public void build() throws IOException {
		if (DEBUG) Log.v(TAG, "build:");
		final Context context = getContext();
		final String tempDir = getTempDir();
		if (DEBUG) Log.v(TAG, "build:tempDir=" + tempDir);
		try {
			final PostMuxBuilder builder = new PostMuxBuilder(mVideoConfig.useMediaMuxer());
			builder.buildFromRawFile(context, tempDir, mOutputDoc);
		} finally {
			delete(new File(tempDir));
		}
		if (DEBUG) Log.v(TAG, "build:finished");
	}
	
	/**
	 * 実行中かどうかを取得
	 * @return
	 */
	@Override
	public boolean isStarted() {
		synchronized (mSync) {
			return !mReleased && mIsRunning;
		}
	}
	
	/**
	 * MediaCodecのエンコーダーの準備ができた時のトラック追加処理
	 * @param format
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	@Override
	public int addTrack(@NonNull final MediaFormat format)
		throws IllegalArgumentException, IllegalStateException {

		if (DEBUG) Log.v(TAG, "addTrack:" + format);
		checkReleased();
		if (mIsRunning) {
			throw new IllegalStateException("already started");
		}

		final Context context = getContext();
		final String tempDir = getTempDir();
		if (DEBUG) Log.v(TAG, "addTrack:tempDir=" + tempDir);
		final String mime = format.containsKey(MediaFormat.KEY_MIME)
			? format.getString(MediaFormat.KEY_MIME) : null;
		if (!TextUtils.isEmpty(mime)) {
			synchronized (mSync) {
				final int trackIndex = mLastTrackIndex + 1;
				if (mime.startsWith("video/")) {
					// 映像エンコーダーの時
					if (mVideoWriter == null) {
						try {
							mMediaRawFileWriters[trackIndex] = mVideoWriter
								= MediaRawFileWriter.newInstance(context,
									PostMuxCommon.TYPE_VIDEO,
									mConfigFormatVideo != null ? mConfigFormatVideo : format,
									format,
								tempDir);
							mLastTrackIndex = trackIndex;
							if (DEBUG) Log.v(TAG, "addTrack:mLastTrackIndex=" + mLastTrackIndex);
							return trackIndex;
						} catch (final IOException e) {
							throw new IllegalArgumentException(e);
						}
					} else {
						throw new IllegalArgumentException("Video track is already added");
					}
				} else if (mime.startsWith("audio/")) {
					// 音声エンコーダーの時
					if (mAudioWriter == null) {
						try {
							mMediaRawFileWriters[trackIndex] = mAudioWriter
								= MediaRawFileWriter.newInstance(context,
									PostMuxCommon.TYPE_AUDIO,
									mConfigFormatAudio != null ? mConfigFormatAudio : format,
									format,
								tempDir);
							mLastTrackIndex = trackIndex;
							if (DEBUG) Log.v(TAG, "addTrack:mLastTrackIndex=" + mLastTrackIndex);
							return trackIndex;
						} catch (final IOException e) {
							throw new IllegalArgumentException(e);
						}
					} else {
						throw new IllegalArgumentException("Audio track is already added");
					}
				} else {
					throw new IllegalArgumentException("Unexpected mime type=" + mime);
				}
			}
		} else {
			throw new IllegalArgumentException("Mime is null");
		}
	}
	
	/**
	 * エンコード済みデータの出力処理
	 * 実際の処理はMediaRawFileWriterで行う
	 * @param trackIndex
	 * @param buffer
	 * @param info
	 */
	@Override
	public void writeSampleData(final int trackIndex,
		@NonNull final ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {
	
		checkReleased();
		if (!mIsRunning) {
			throw new IllegalStateException("Can't write, muxer is not started");
		}
		if (trackIndex < 0 || trackIndex > mLastTrackIndex) {
			throw new IllegalArgumentException("Invalid trackIndex=" + trackIndex);
		}
		if ((info.size < 0) || (info.offset < 0)
			|| ((info.offset + info.size) > buffer.capacity())
			|| (info.presentationTimeUs < 0) )  {
			
			throw new IllegalArgumentException("bufferInfo must specify a" +
				" valid buffer offset, size and presentation time");
		}

		final MediaRawFileWriter writer;
		synchronized (mSync) {
			writer = mMediaRawFileWriters[trackIndex];
		}
		if (writer != null) {
			try {
				writer.writeSampleData(buffer, info);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
	}
	
	@Nullable
	protected Context getContext() {
		return mWeakContext.get();
	}

	/**
	 * 破棄されたかどうかをチェックして破棄されていればIllegalStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void checkReleased() throws IllegalStateException {
		synchronized (mSync) {
			if (mReleased) {
				throw new IllegalStateException("already released");
			}
		}
	}
	
	/**
	 * 一時ファイル用のディレクトリパスを取得
	 * @return
	 */
	private String getTempDir() {
		if (DEBUG) Log.v(TAG, "getTempDir:");
		final Context context = getContext();
		try {
			return context.getDir(mTempName, Context.MODE_PRIVATE).getAbsolutePath();
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		return new File(
			Environment.getDataDirectory(), mTempName).getAbsolutePath();
	}

	/**
	 * 再帰的に指定したファイル・ディレクトリを削除する
	 * @param path
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static final void delete(@Nullable final File path) {
		if (DEBUG) Log.v(TAG, "delete:" + path);
		if (path != null) {
			try {
				if (path.isDirectory()) {
					final File[] files = path.listFiles();
					final int n = files != null ? files.length : 0;
					for (int i = 0; i < n; i++)
						delete(files[i]);
				}
				path.delete();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
}
