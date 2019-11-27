package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

public interface IDrawer2D {
	/**
	 * 関連するリソースを廃棄する
	 */
	public void release();

	/**
	 * モデルビュー変換行列を取得
	 * 内部配列を直接返すので変更時は要注意
	 * @return
	 */
	@NonNull
	public float[] getMvpMatrix();

	/**
	 * モデルビュー変換行列を設定
	 * @param matrix
	 * @param offset
	 * @return
	 */
	public IDrawer2D setMvpMatrix(@NonNull final float[] matrix, final int offset);

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void copyMvpMatrix(@NonNull final float[] matrix, final int offset);

	/**
	 * 描画実行
	 * @param texId
	 * @param tex_matrix
	 * @param offset
	 */
	public void draw(final int texId, @Nullable final float[] tex_matrix, final int offset);

	/**
	 * IGLSurfaceオブジェクトを描画するためのヘルパーメソッド
	 * IGLSurfaceオブジェクトで管理しているテクスチャ名とテクスチャ座標変換行列を使って描画する
	 * @param surface
	 */
	public void draw(@NonNull final IGLSurface surface);
}
