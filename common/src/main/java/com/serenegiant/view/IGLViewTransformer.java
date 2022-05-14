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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * OpenGL|ESを使った表示内容のトランスフォームを行うViewの場合
 */
public interface IGLViewTransformer extends IContentTransformer {
	/**
	 * トランスフォームマトリックスのコピーを取得
	 *
	 * @param transform nullなら内部で新しいfloat配列を生成して返す, nullでなければコピーする
	 * @return
	 */
	@Size(min = 16)
	@NonNull
	public float[] getTransform(@Nullable @Size(min = 16) final float[] transform);

	/**
	 * トランスフォームマトリックスをセットする
	 *
	 * @param transform nullなら単位行列をセットする
	 * @return
	 */
	@NonNull
	public IGLViewTransformer setTransform(@Nullable @Size(min = 16) final float[] transform);

	/**
	 * Viewからトランスフォームマトリックスを取得する
	 *
	 * @param saveAsDefault
	 * @return
	 */
	@NonNull
	public IGLViewTransformer updateTransform(final boolean saveAsDefault);

	/**
	 * デフォルトのトランスフォームマトリックスを設定する
	 *
	 * @param transform nullなら単位行列になる
	 * @return
	 */
	@NonNull
	public IGLViewTransformer setDefault(@Nullable @Size(min = 16) final float[] transform);

	/**
	 * トランスフォームマトリックスを初期化する
	 *
	 * @return
	 */
	@NonNull
	public IGLViewTransformer reset();
}

