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
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;

import com.serenegiant.system.BuildCheck;

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
		public IMuxer createMuxer(
			@NonNull final Context context,
			final boolean useMediaMuxer,
			@NonNull final DocumentFile file) throws IOException;
	}

	/**
	 * デフォルトのIMuxerFactory実装
	 */
	public static class DefaultFactory implements IMuxerFactory {
		@SuppressLint("NewApi")
		public IMuxer createMuxer(@NonNull final Context context,
			final boolean useMediaMuxer,
			@NonNull final DocumentFile file) throws IOException {

			final Uri uri = file.getUri();
			IMuxer result = MediaMuxerWrapper.newInstance(context, file, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			if (result == null) {
				throw new IOException("Unsupported muxer type");
			}
			return result;
		}
	}
}
