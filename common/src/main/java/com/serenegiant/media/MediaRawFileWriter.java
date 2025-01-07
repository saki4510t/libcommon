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
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

/**
 * MediaCodecからのエンコード済みのフレームデータをrawファイルへ出力するクラス
 * 映像エンコード処理と音声エンコード処理とmux処理を同時に実行すると
 * 映像and/or音声が正常に記録されない端末がいくつかあるので、
 * 一度一時ファイルへ書き出しておいてエンコード終了後にmux処理を
 * 行うPostMux関係のクラス
 */
abstract class MediaRawFileWriter extends PostMuxCommon {
	private static final boolean DEBUG = false; // FIXME set false on production
	private static final String TAG = MediaRawFileWriter.class.getSimpleName();

	/**
	 * インスタンス生成用のヘルパーメソッド
	 * @param context
	 * @param mediaType
	 * @param configFormat
	 * @param outputFormat
	 * @param tempDir アプリケーションプライベートな一時ファイル保存用ディレクトリ
	 * @return
	 * @throws IOException
	 */
	public static MediaRawFileWriter newInstance(
		@NonNull final Context context,
		@MediaType final int mediaType,
		@NonNull final MediaFormat configFormat,
		@NonNull final MediaFormat outputFormat,
		@NonNull final String tempDir) throws IOException {

		return switch (mediaType) {
			case TYPE_VIDEO ->
				new MediaRawVideoWriter(context, configFormat, outputFormat, tempDir);
			case TYPE_AUDIO ->
				new MediaRawAudioWriter(context, configFormat, outputFormat, tempDir);
			default ->
				throw new IOException("Unexpected media type=" + mediaType);
		};
	}
	
//================================================================================

	/**
	 * 動画データ出力用
	 */
	private static class MediaRawVideoWriter extends MediaRawFileWriter {
		public MediaRawVideoWriter(@NonNull final Context context,
			@NonNull final MediaFormat configFormat,
			@NonNull final MediaFormat outputFormat,
			@NonNull final String tempDir) throws IOException {

			super(context, configFormat, outputFormat, tempDir, VIDEO_NAME);
		}
	}
	
	/**
	 * 音声データ出力用
	 */
	private static class MediaRawAudioWriter extends MediaRawFileWriter {
		public MediaRawAudioWriter(@NonNull final Context context,
			@NonNull final MediaFormat configFormat,
			@NonNull final MediaFormat outputFormat,
			@NonNull final String tempDir) throws IOException {

			super(context, configFormat,outputFormat, tempDir, AUDIO_NAME);
		}
	}
	
//================================================================================
	private DataOutputStream mOut;
	private int mFrameCounts;
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param configFormat
	 * @param outputFormat
	 * @param tempDir アプリケーションプライベートな一時ファイル保存用ディレクトリ
	 * @param name 一時ファイル名(パスを含まず)
	 * @throws IOException
	 */
	private MediaRawFileWriter(
		@NonNull final Context context,
		@NonNull final MediaFormat configFormat,
		@NonNull final MediaFormat outputFormat,
		@NonNull final String tempDir,
		@NonNull final String name) throws IOException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mOut = new DataOutputStream(new BufferedOutputStream(
			new FileOutputStream(tempDir.endsWith("/")
				? tempDir + name : tempDir + "/" + name, false)));
		writeFormat(mOut, configFormat, outputFormat);
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
	 * 関係するリソースを破棄
	 */
	public synchronized void release() {
		if (mOut != null) {
			if (DEBUG) Log.v(TAG, "release:");
			try {
				mOut.flush();
				mOut.close();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mOut = null;
			if (DEBUG) Log.v(TAG, "release:finished");
		}
	}
	
	/** リアロケーション避けにワーク用byte配列を保持する */
	private byte[] temp;
	
	/**
	 * エンコード済みのフレームデータの出力処理
	 * @param buffer
	 * @param info
	 * @throws IOException
	 */
	public synchronized void writeSampleData(
		@NonNull final ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) throws IOException {

		if (info.size != 0) {
			if ((temp == null) || (temp.length < info.size)) {
				temp = new byte[info.size];
			}
			mFrameCounts++;
			writeStream(mOut, 0, mFrameCounts, info, buffer, temp);
		}
	}

}
