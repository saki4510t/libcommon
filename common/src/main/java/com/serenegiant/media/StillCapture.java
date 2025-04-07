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

import android.graphics.Bitmap;

import com.serenegiant.system.BuildCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * 静止画撮影用の定数・ユーティリティメソッド
 */
public interface StillCapture {
	public static final int DEFAULT_CAPTURE_COMPRESSION = 80;

	public static final int OUTPUT_FORMAT_JPEG = 0;	// Bitmap.CompressFormat.JPEG
	public static final int OUTPUT_FORMAT_PNG = 1;	// Bitmap.CompressFormat.PNG
	public static final int OUTPUT_FORMAT_WEBP = 2;	// Bitmap.CompressFormat.WEBP

	@IntDef({OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_PNG, OUTPUT_FORMAT_WEBP})
	@Retention(RetentionPolicy.SOURCE)
	public @interface StillCaptureFormat {}

	/**
	 * パス文字列の拡張子を調べて静止画圧縮フォーマットを取得する。
	 * jpeg(jpg)/png/webpのいずれでもなければIllegalArgumentExceptionを投げる
	 * @param outputFileName
	 * @return StillCaptureFormatの定数のどれか
	 * @throws IllegalArgumentException
	 */
	@StillCaptureFormat
	public static int getCaptureFormat(@NonNull final String outputFileName)
		throws IllegalArgumentException {

		int result;
		final String _fileName = outputFileName.toLowerCase();
		if (_fileName.endsWith(".jpg") || _fileName.endsWith(".jpeg")) {
			result = OUTPUT_FORMAT_JPEG;
		} else if (_fileName.endsWith(".png")) {
			result = OUTPUT_FORMAT_PNG;
		} else if (_fileName.endsWith(".webp")) {
			result = OUTPUT_FORMAT_WEBP;
		} else {
			throw new IllegalArgumentException("unknown compress format(extension)");
		}
		return result;
	}

	/**
	 * 静止画圧縮フォーマットをBitmap.CompressFormatに変換する
	 * @param captureFormat
	 * @param compress
	 * @return
	 */
	public static Bitmap.CompressFormat getCaptureFormat(
		@StillCaptureFormat final int captureFormat,
		final int compress) {

		Bitmap.CompressFormat result;
		switch (captureFormat) {
		case OUTPUT_FORMAT_PNG -> result = Bitmap.CompressFormat.PNG;
		case OUTPUT_FORMAT_WEBP -> {
			if (BuildCheck.isAndroid11()) {
				if (compress == 100) {
					result = Bitmap.CompressFormat.WEBP_LOSSLESS;
				} else {
					result = Bitmap.CompressFormat.WEBP_LOSSY;
				}
			} else {
				result = Bitmap.CompressFormat.WEBP;
			}
		}
		case OUTPUT_FORMAT_JPEG -> result = Bitmap.CompressFormat.JPEG;
		default -> result = Bitmap.CompressFormat.JPEG;
		}

		return result;
	}

}
