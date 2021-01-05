package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import java.io.IOException;

import androidx.annotation.NonNull;

public interface IGLSurface extends ISurface {
	/**
	 * バックバッファとして使っているテクスチャのテクスチャターゲット(GL_TEXTURE_2D等)を取得
	 * @return
	 */
	public int getTexTarget();

	/**
	 * バックバッファとして使っているテクスチャのテクスチャユニット(GL_TEXTURE0-GL_TEXTURE31)を取得
	 * @return
	 */
	public int getTexUnit();

	/**
	 * オフスクリーンテクスチャ名を取得
	 * このオフスクリーンへ書き込んだ画像をテクスチャとして使って他の描画を行う場合に使用できる
	 * @return
	 */
	public int getTexId();

	/**
	 * バックバッファとして使っているテクスチャの実際の幅を取得
	 * @return
	 */
	public int getTexWidth();

	/**
	 * バックバッファとして使っているテクスチャの実際の高さを取得
	 * @return
	 */
	public int getTexHeight();

	/**
	 * テクスチャ座標変換行列のコピーを取得
	 * @return
	 */
	public float[] copyTexMatrix();

	/**
	 * テクスチャ座標変換行列のコピーを取得
	 * 領域チェックしていないのでoffset位置から16個以上確保しておくこと
	 * @param matrix
	 * @param offset
	 */
	public void copyTexMatrix(final float[] matrix, final int offset);

	/**
	 * テクスチャ座標変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	public float[] getTexMatrix();

	/**
	 * 指定したファイルから画像をテクスチャに読み込む
	 * ファイルが存在しないか読み込めなければIOExceptionを生成
	 * @param filePath
	 * @throws IOException
	 */
	public void loadBitmap(@NonNull final String filePath) throws IOException;

	/**
	 * テクスチャにビットマップを読み込む
	 * @param bitmap
	 */
	public void loadBitmap(@NonNull final Bitmap bitmap);
}
