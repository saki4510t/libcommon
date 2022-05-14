package com.serenegiant.view;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * android.graphics.Matrixを使った表示内容のトランスフォームを行うViewの場合
 */
public interface IViewTransformer extends IContentTransformer {
	/**
	 * トランスフォームマトリックスのコピーを取得
	 *
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	public Matrix getTransform(@Nullable Matrix transform);

	/**
	 * トランスフォームマトリックスをセットする
	 *
	 * @param transform nullなら単位行列をセットする
	 * @return
	 */
	@NonNull
	public IViewTransformer setTransform(@Nullable Matrix transform);

	/**
	 * Viewからトランスフォームマトリックスを取得する
	 *
	 * @param saveAsDefault
	 * @return
	 */
	@NonNull
	public IViewTransformer updateTransform(final boolean saveAsDefault);

	/**
	 * デフォルトのトランスフォームマトリックスを設定する
	 *
	 * @param transform nullなら単位行列になる
	 * @return
	 */
	@NonNull
	public IViewTransformer setDefault(@Nullable final Matrix transform);

	/**
	 * トランスフォームマトリックスを初期化する
	 *
	 * @return
	 */
	@NonNull
	public IViewTransformer reset();
}
