package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.serenegiant.mediastore.MediaStoreOutputStream;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.UriHelper;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

/**
 * MediaMuxerとVideoMuxerを共通で扱えるようにするためのインターフェース
 */
public interface IMuxer {

	public int addTrack(@NonNull final MediaFormat format);
	public void writeSampleData(final int trackIndex,
		@NonNull final ByteBuffer byteBuf,
		@NonNull final MediaCodec.BufferInfo bufferInfo);
	public void start();
	public void stop();
	public void release();
	public boolean isStarted();

	/**
	 * IMuxer生成処理を移譲するためのファクトリーインターフェース
	 */
	public interface IMuxerFactory {
		@Deprecated
		public IMuxer createMuxer(final boolean useMediaMuxer, final String outputPath) throws IOException;
		@Deprecated
		public IMuxer createMuxer(final boolean useMediaMuxer, final int fd) throws IOException;
		public IMuxer createMuxer(@NonNull final Context context, final boolean useMediaMuxer,
			@NonNull final DocumentFile file) throws IOException;
	}

	/**
	 * デフォルトのIMuxerFactory実装
	 */
	public static class DefaultFactory implements IMuxerFactory {
		@SuppressLint("InlinedApi")
		public IMuxer createMuxer(final boolean useMediaMuxer, final String outputPath) throws IOException {
			IMuxer result;
			if (useMediaMuxer && BuildCheck.isAPI18()) {	// MediaMuxerはAPI>=18
				result = new MediaMuxerWrapper(outputPath,
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			} else {
				throw new IOException("Unsupported muxer type");
//				result = new VideoMuxer(output_oath);
			}
			return result;
		}

		@SuppressLint("NewApi")
		public IMuxer createMuxer(final boolean useMediaMuxer, final int fd) throws IOException {
			IMuxer result;
			if (useMediaMuxer && BuildCheck.isAPI18()) {	// MediaMuxerはAPI>=18
				if (BuildCheck.isAPI29()) {
					// API29以上は対象範囲別ストレージなのでMediaStoreOutputStreamを使いたいところだけど
					// fdからMediaMuxerWrapperは生成できない
					throw new UnsupportedOperationException("createMuxer from fd does not support on API29 and later devices");
				} else if (BuildCheck.isAPI26()) {
					final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fd);
					result = new MediaMuxerWrapper(pfd.getFileDescriptor(),
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else {
					throw new UnsupportedOperationException("createMuxer from fd does not support on API<26");
				}
			} else {
				throw new IOException("Unsupported muxer type");
//				result = new VideoMuxer(fd);
			}
			return result;
		}

		@SuppressLint("NewApi")
		public IMuxer createMuxer(@NonNull final Context context,
			final boolean useMediaMuxer,
			@NonNull final DocumentFile file) throws IOException {

			final Uri uri = file.getUri();
			IMuxer result = null;
			if (useMediaMuxer && BuildCheck.isAPI18()) {	// MediaMuxerはAPI>=18
				if (BuildCheck.isAPI29() && UriHelper.isContentUri(uri)) {
					// API29以上は対象範囲別ストレージなのでMediaStoreOutputStreamを使って出力終了時にIS_PENDINGの更新を自動でする
					result = new MediaMuxerWrapper(
						new MediaStoreOutputStream(context, file),
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else if (BuildCheck.isAPI26()) {
					result = new MediaMuxerWrapper(context.getContentResolver()
						.openFileDescriptor(uri, "rw").getFileDescriptor(),
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else {
					final String path = UriHelper.getPath(context, uri);
					final File f = new File(path);
					if (/*!f.exists() &&*/ f.canWrite()) {
						// 書き込めるファイルパスを取得できればそれを使う
						result = new MediaMuxerWrapper(path,
							MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
					} else {
						Log.w("IMuxer", "can't write to the file, try to use VideoMuxer instead");
					}
				}
			}
			if (result == null) {
				throw new IOException("Unsupported muxer type");
//				result = new VideoMuxer(context.getContentResolver()
//					.openFileDescriptor(file.getUri(), "rw").getFd());
			}
			return result;
		}
	}
}
