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

import android.content.Context;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.serenegiant.mediastore.MediaStoreOutputStream;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.UriHelper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

/**
 * MediaMuxerをIMuxerインターフェースでラップ
 */
public class MediaMuxerWrapper implements IMuxer {
	private static final String TAG = MediaMuxerWrapper.class.getSimpleName();

	/**
	 * API26以上29未満の時にMediaStoreOutputStreamを使うかどうか
	 * API29以上でDocumentFileがcontent uriを示している場合は
	 * この設定に関係なく常にMediaStoreOutputStreamを使う
	 * デフォルトはtrue
	 */
	public static boolean USE_MEDIASTORE_OUTPUT_STREAM = true;

	/**
	 * インスタンス生成用ヘルパーメソッド
	 * MediaMuxerWrapperの個別のコンストラクタ呼び出しを自前で実装する代わりに
	 * APIレベルの応じて自動的にコンストラクタを選択する
	 * @param context
	 * @param output
	 * @param format
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("NewAPI")
	@Nullable
	public static MediaMuxerWrapper newInstance(
		@NonNull final Context context,
		@NonNull final DocumentFile output,
		final int format) throws IOException {

		final Uri uri = output.getUri();
		if (BuildCheck.isAPI29() && UriHelper.isContentUri(uri)) {
			return new MediaMuxerWrapper(new MediaStoreOutputStream(context, output), format);
		} else if (BuildCheck.isAPI26()) {
			if (USE_MEDIASTORE_OUTPUT_STREAM) {
				return new MediaMuxerWrapper(new MediaStoreOutputStream(context, output), format);
			} else {
				return new MediaMuxerWrapper(context.getContentResolver().openFileDescriptor(uri, "rw").getFileDescriptor(), format);
			}
		} else {
			final String path = UriHelper.getPath(context, uri);
			final File f = new File(path);
			if (f.canWrite()) {
				return new MediaMuxerWrapper(path, format);
			}
		}
		return null;
	}

	@NonNull
	private final MediaMuxer mMuxer;
	@Nullable
	private final OutputStream mOutputStream;
	private volatile boolean mIsStarted;
	private boolean mReleased;

	/**
	 * 出力先をファイルパス文字列で指定するコンストラクタ
	 * @param outputPath
	 * @param format
	 * @throws IOException
	 */
	public MediaMuxerWrapper(@NonNull final String outputPath, final int format)
		throws IOException {

		mMuxer = new MediaMuxer(outputPath, format);
		mOutputStream = null;
	}

	/**
	 * 出力先をFileDescriptorで指定するコンストラクタ
	 * @param fd
	 * @param format
	 * @throws IOException
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public MediaMuxerWrapper(final FileDescriptor fd, final int format)
		throws IOException {

		mMuxer = new MediaMuxer(fd, format);
		mOutputStream = null;
	}

	/**
	 * 出力先をOutputStreamで指定するコンストラクタ
	 * @param output
	 * @param format
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public MediaMuxerWrapper(
		@NonNull final FileOutputStream output,
		final int format)
			throws IOException {

		mMuxer = new MediaMuxer(output.getFD(), format);
		mOutputStream = output;
	}

	/**
	 * 出力先をOutputStreamで指定するコンストラクタ
	 * @param output
	 * @param format
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public MediaMuxerWrapper(
		@NonNull final MediaStoreOutputStream output,
		final int format)
			throws IOException {

		mMuxer = new MediaMuxer(output.getFd(), format);
		mOutputStream = output;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	@Override
	public int addTrack(@NonNull final MediaFormat format) {
		return mMuxer.addTrack(format);
	}

	@Override
	public void writeSampleData(final int trackIndex,
		@NonNull final ByteBuffer byteBuf, @NonNull final BufferInfo bufferInfo) {

		if (!mReleased) {
			mMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
		}
	}

	@Override
	public void start() {
		mMuxer.start();
		mIsStarted = true;
	}

	@Override
	public void stop() {
		if (mIsStarted) {
			mIsStarted = false;
			mMuxer.stop();
		}
	}

	@Override
	public void release() {
		mIsStarted = false;
		if (!mReleased) {
			mReleased = true;
			try {
				mMuxer.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			try {
				if (mOutputStream != null) {
					mOutputStream.close();
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public boolean isStarted() {
		return mIsStarted && !mReleased;
	}

}
