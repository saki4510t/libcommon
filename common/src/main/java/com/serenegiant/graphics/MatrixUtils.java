package com.serenegiant.graphics;
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

import androidx.annotation.NonNull;

public class MatrixUtils {
	private MatrixUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * Matrixから回転角度を取得する
	 * @param matrix
	 * @return 回転角度[degree]
	 */
	public static float getRotate(@NonNull final Matrix matrix) {
		final float[] mat = new float[9];
		matrix.getValues(mat);
		return getRotate(mat);
	}

	/**
	 * Matrixから回転角度を取得する
	 * @param mat
	 * @return 回転角度[degree]
	 */
	public static float getRotate(@NonNull final float[] mat) {
		return Math.round(Math.atan2(mat[Matrix.MSKEW_X], mat[Matrix.MSCALE_X]) * (180 / Math.PI));
	}

	/**
	 * スキューを考慮して実際の拡大縮小率を取得する
	 * @param matrix
	 * @return
	 */
	public static float getScale(@NonNull final Matrix matrix) {
		final float[] mat = new float[9];
		matrix.getValues(mat);
		return getScale(mat);
	}

	/**
	 * スキューを考慮して実際の拡大縮小率を取得する
	 * @param mat
	 * @return
	 */
	public static float getScale(@NonNull final float[] mat) {
		final float scaleX = mat[Matrix.MSCALE_X];
		final float skewY = mat[Matrix.MSKEW_Y];
		return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
	}
}
