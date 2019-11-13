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

import android.graphics.Bitmap;

import java.io.IOException;

import androidx.annotation.NonNull;

public interface ITexture {
	public void release();

	public void makeCurrent();
	public void swap();

	public int getTexTarget();
	public int getTexId();

	/**
	 * テクスチャ座標変換行列を取得(内部配列をそのまま返すので変更時は要注意)
	 * @return
	 */
	public float[] getTexMatrix();
	public void getTexMatrix(float[] matrix, int offset);

	public int getTexWidth();
	public int getTexHeight();

	public void loadBitmap(@NonNull final String filePath) throws IOException;
	public void loadBitmap(@NonNull final Bitmap bitmap);
}
