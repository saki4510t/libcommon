package com.serenegiant.app;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
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
 *
*/

public class PendingIntentCompat {
	private PendingIntentCompat() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	// PendingIntent.FLAG_MUTABLEはコンパイラー>=31でしか存在しないので30未満でもビルドできるように自前で定義
	public static final int FLAG_MUTABLE = 1<<25;	// == PendingIntent.FLAG_MUTABLE
}
