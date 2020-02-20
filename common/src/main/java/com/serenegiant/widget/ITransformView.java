package com.serenegiant.widget;
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

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * android.graphics.Matrixを使った表示内容の
 * 拡大縮小平行移動回転可能なView用インターフェース
 */
public interface ITransformView {
	@NonNull
	public View getView();
	@NonNull
	public Matrix getTransform(@Nullable Matrix transform);
	public void setTransform(@Nullable Matrix transform);
	/**
	 * View表示内容の大きさを取得
	 * @return
	 */
	public RectF getBounds();
	/**
	 * View表内容の拡大縮小回転平行移動を初期化時の追加処理
	 * 親Viewデフォルトの拡大縮小率にトランスフォームマトリックスを設定させる
	 */
	public void onInit();
}
