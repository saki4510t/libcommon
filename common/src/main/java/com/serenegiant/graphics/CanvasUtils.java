package com.serenegiant.graphics;
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.serenegiant.system.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CanvasUtils {
	private CanvasUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * Canvas#saveLayer呼び出しのヘルパーメソッド
	 * パフォーマンスのために常にALL_SAVE_FLAGを指定すべきだけど
	 * API21未満ではフラグ指定なしのメソッドがないのでAPIレベルで分岐させる
	 * @param canvas
	 * @param bounds
	 * @param paint
	 * @return
	 */
	public static int saveLayer(
		@NonNull final Canvas canvas,
		@Nullable final RectF bounds,
		@Nullable final Paint paint) {
		if (BuildCheck.isLollipop()) {
			return canvas.saveLayer(bounds, paint);
		} else {
			return canvas.saveLayer(bounds, paint, Canvas.ALL_SAVE_FLAG);
		}
	}
}
