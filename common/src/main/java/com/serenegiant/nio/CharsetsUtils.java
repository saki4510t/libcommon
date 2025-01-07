package com.serenegiant.nio;
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

import android.os.Build;

import com.serenegiant.system.BuildCheck;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;

/**
 * StandardCharsetsを使えと言われて変更するとAPI<19で「実行時に」クラッシュしてしまうので
 * APIレベルに関係なく定数を使えるようにするためのヘルパークラス
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public class CharsetsUtils {
	private CharsetsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * Eight-bit UCS Transformation Format
	 */
	@NonNull
	public static final Charset UTF8;
	/**
	 * Sixteen-bit UCS Transformation Format, byte order identified by an
	 * optional byte-order mark
	 */
	@NonNull
	public static final Charset UTF16;
	/**
	 * Sixteen-bit UCS Transformation Format, big-endian byte order
	 */
	@NonNull
	public static final Charset UTF16BE;
	/**
	 * Sixteen-bit UCS Transformation Format, little-endian byte order
	 */
	@NonNull
	public static final Charset UTF16LE;

	static {
		if (BuildCheck.isKitKat()) {
			UTF8 = StandardCharsets.UTF_8;
			UTF16 = StandardCharsets.UTF_16;
			UTF16BE = StandardCharsets.UTF_16BE;
			UTF16LE = StandardCharsets.UTF_16LE;
		} else {
			UTF8 = Charset.forName("UTF-8");
			UTF16 = Charset.forName("UTF-16");
			UTF16BE = Charset.forName("UTF-16BE");
			UTF16LE = Charset.forName("UTF-16LE");
		}
	}

}
