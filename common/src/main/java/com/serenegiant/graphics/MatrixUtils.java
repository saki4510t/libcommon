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
import androidx.annotation.Nullable;
import androidx.annotation.Size;

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

	/**
	 * android.graphics.Matrixの3x3行列をOpenGLの4x4(列優先)行列に変換する
	 * (アフィン変換のみ)
	 * |a11 a12 a13|  |0 1 2|      |a11 a12   0 a13|   |0 4 8  12|
	 * |a21 a22 a23|  |3 4 5|      |a21 a22   0 a23|   |1 5 9  13|
	 * |a31 a32 a33|  |6 7 8| =>   |  0   0   1   0|   |2 6 10 14|
	 *                             |a31 a32   0 a33|   |3 7 11 15|
	 * @param transform
	 * @param result
	 * @return
	 */
	@NonNull
	@Size(min=16)
	public static float[] toGLMatrix(@NonNull final Matrix transform,
		@NonNull @Size(min=16) final float[] result,
		@NonNull @Size(min=9) final float[] aMatrix) {

		transform.getValues(aMatrix);
		result[ 0] = aMatrix[Matrix.MSCALE_X];
		result[ 1] = aMatrix[Matrix.MSKEW_Y];
		result[ 2] = 0;
		result[ 3] = aMatrix[Matrix.MPERSP_0];
		result[ 4] = aMatrix[Matrix.MSKEW_X];
		result[ 5] = aMatrix[Matrix.MSCALE_Y];
		result[ 6] = 0;
		result[ 7] = aMatrix[Matrix.MPERSP_1];
		result[ 8] = 0;
		result[ 9] = 0;
		result[10] = 1;
		result[11] = 0;
		result[12] = aMatrix[Matrix.MTRANS_X];
		result[13] = aMatrix[Matrix.MTRANS_Y];
		result[14] = 0;
		result[15] = aMatrix[Matrix.MPERSP_2];
		return result;
	}

	/**
	 * OpenGLの4x4(列優先)行列をandroid.graphics.Matrixの3x3行列に変換する
	 * (アフィン変換のみ)
	 * @param transform
	 * @param result
	 * @param aMatrix
	 * @return
	 */
	public static Matrix toAndroidMatrix(
		@NonNull @Size(min=16)final float[] transform,
		@NonNull final Matrix result,
		@NonNull @Size(min=9) final float[] aMatrix) {

		aMatrix[Matrix.MSCALE_X] = transform[ 0];
		aMatrix[Matrix.MSKEW_Y] = transform[ 1];
		aMatrix[Matrix.MPERSP_0] = transform[ 3];
		aMatrix[Matrix.MSKEW_X] = transform[ 4];
		aMatrix[Matrix.MSCALE_Y] = transform[ 5];
		aMatrix[Matrix.MPERSP_1] = transform[ 7];
		aMatrix[Matrix.MTRANS_X] = transform[12];
		aMatrix[Matrix.MTRANS_Y] = transform[13];
		aMatrix[Matrix.MPERSP_2] = transform[15];
		result.setValues(aMatrix);

		return result;
	}
}
