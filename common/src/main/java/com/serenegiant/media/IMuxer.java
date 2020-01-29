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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

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

	public interface IMuxerFactory {
		public IMuxer createMuxer(final boolean useMediaMuxer, final String output_oath) throws IOException;
		public IMuxer createMuxer(final boolean useMediaMuxer, final int fd) throws IOException;
		public IMuxer createMuxer(@NonNull final Context context, final boolean useMediaMuxer,
			@NonNull final DocumentFile file) throws IOException;
	}

	public static class DefaultFactory implements IMuxerFactory {
		@SuppressLint("InlinedApi")
		public IMuxer createMuxer(final boolean useMediaMuxer, final String output_oath) throws IOException {
			IMuxer result;
			if (useMediaMuxer) {
				result = new MediaMuxerWrapper(output_oath,
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
			if (useMediaMuxer) {
				if (BuildCheck.isOreo()) {
					final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fd);
					result = new MediaMuxerWrapper(pfd.getFileDescriptor(),
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else {
					throw new RuntimeException("createMuxer from fd does not support now");
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

			IMuxer result = null;
			if (useMediaMuxer) {
				if (BuildCheck.isOreo()) {
					result = new MediaMuxerWrapper(context.getContentResolver()
						.openFileDescriptor(file.getUri(), "rw").getFileDescriptor(),
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else {
					final String path = UriHelper.getPath(context, file.getUri());
					final File f = new File(UriHelper.getPath(context, file.getUri()));
					if (/*!f.exists() &&*/ f.canWrite()) {
						// 書き込めるファイルパスを取得できればそれを使う
						result = new MediaMuxerWrapper(path,
							MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
					} else {
						Log.w("IMuxer", "cant't write to the file, try to use VideoMuxer instead");
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
