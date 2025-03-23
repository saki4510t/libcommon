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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

import com.serenegiant.glutils.IMirror;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import static com.serenegiant.glutils.IMirror.*;

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
	public static float getRotate(@NonNull @Size(min=9) final float[] mat) {
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
	public static float getScale(@NonNull @Size(min=9) final float[] mat) {
		final float scaleX = mat[Matrix.MSCALE_X];
		final float skewY = mat[Matrix.MSKEW_Y];
		return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * @param mvp
	 * @param mirror
	 */
	public static void setMirror(@NonNull @Size(min=16) final float[] mvp, @IMirror.MirrorMode final int mirror) {
		switch (mirror) {
		case MIRROR_NORMAL -> {
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = Math.abs(mvp[5]);
		}
		case MIRROR_HORIZONTAL -> {
			mvp[0] = -Math.abs(mvp[0]);    // flip left-right
			mvp[5] = Math.abs(mvp[5]);
		}
		case MIRROR_VERTICAL -> {
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = -Math.abs(mvp[5]);    // flip up-side down
		}
		case MIRROR_BOTH -> {
			mvp[0] = -Math.abs(mvp[0]);    // flip left-right
			mvp[5] = -Math.abs(mvp[5]);    // flip up-side down
		}
		}
	}

	/**
	 * 左右・上下反転をセット
	 * @param matrix
	 * @param mirror
	 */
	public static void setMirror(@NonNull final Matrix matrix, @IMirror.MirrorMode final int mirror) {
		switch (mirror) {
		case IMirror.MIRROR_HORIZONTAL -> matrix.preScale(-1, 1);
		case IMirror.MIRROR_VERTICAL -> matrix.preScale(1, -1);
		case IMirror.MIRROR_BOTH -> matrix.preScale(-1, -1);
		case IMirror.MIRROR_NORMAL -> {}
		default -> {}
		}
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * @param mvp
	 * @param degrees
	 */
	public static void rotate(@NonNull @Size(min=16) final float[] mvp, final int degrees) {
		android.opengl.Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * @param mvp
	 * @param degrees
	 */
	public static void setRotation(@NonNull @Size(min=16) final float[] mvp, final int degrees) {
		android.opengl.Matrix.setIdentityM(mvp, 0);
		android.opengl.Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
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
	 * android.graphics.Matrixの3x3行列をOpenGLの4x4(列優先)行列に変換する
	 * @param matrix
	 * @return
	 */
	@NonNull
	@Size(min=16)
	public static float[] toGLMatrix(@NonNull final Matrix matrix) {
		return toGLMatrix(matrix, new float[16], new float[9]);
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

	/**
	 * OpenGLの4x4(列優先)行列をandroid.graphics.Matrixの3x3行列に変換する
	 * (アフィン変換のみ)
	 * @param transform
	 * @return
	 */
	public static Matrix toAndroidMatrix(@NonNull @Size(min=16)final float[] transform) {
		final Matrix result = new Matrix();
		final float[] work = new float[9];
		return toAndroidMatrix(transform, result, work);
	}

	/**
	 * OpenGL|ESの4x4行列を列優先で文字列化
	 * @param transform
	 * @return
	 */
	public static String toGLMatrixString(
		@NonNull @Size(min=16) final float[] transform) {

		return "GLMatrix[" +
			transform[0] + ", " +
			transform[1] + ", " +
			transform[2] + ", " +
			transform[3] +
			"][" +
			transform[4] + ", " +
			transform[5] + ", " +
			transform[6] + ", " +
			transform[7] +
			"][" +
			transform[8] + ", " +
			transform[9] + ", " +
			transform[10] + ", " +
			transform[11] +
			"][" +
			transform[12] + ", " +
			transform[13] + ", " +
			transform[14] + ", " +
			transform[15] +
			']';
	}

	/**
	 * AndroidのMatrix(3x3)をOpenGLの行列と同じ列優先で文字列に変換する
	 * Android Matrix(3x3)			Open GL Matrix(4x4)
	 * |a11 a12 a13|  |0 1 2|      |a11 a12   0 a13|   |0 4 8  12|
	 * |a21 a22 a23|  |3 4 5|      |a21 a22   0 a23|   |1 5 9  13|
	 * |a31 a32 a33|  |6 7 8|      |  0   0   1   0|   |2 6 10 14|
	 *                             |a31 a32   0 a33|   |3 7 11 15|
	 * @param matrix
	 * @return
	 */
	public static String toString(@NonNull final Matrix matrix) {
		final float[] work = new float[9];
		matrix.getValues(work);
		return "AMatrix[" +
			work[0] + ", " +
			work[3] + ", " +
			work[6] + ", " +
			"][" +
			work[1] + ", " +
			work[4] + ", " +
			work[7] + ", " +
			"][" +
			work[2] +
			work[5] + ", " +
			work[6] +
			']';
	}

	public static boolean compare(
		@NonNull final Matrix matrix,
		@NonNull @Size(min=16)final float[] mat,
		final float eps) {

		final float[] work = new float[9];
		matrix.getValues(work);

		return (Math.abs(work[Matrix.MSCALE_X] - mat[0]) < eps)
			&& (Math.abs(work[Matrix.MSKEW_Y] - mat[1]) < eps)
			&& (Math.abs(work[Matrix.MPERSP_0] - mat[3]) < eps)
			&& (Math.abs(work[Matrix.MSKEW_X] - mat[4]) < eps)
			&& (Math.abs(work[Matrix.MSCALE_Y] - mat[5]) < eps)
			&& (Math.abs(work[Matrix.MPERSP_1] - mat[7]) < eps)
			&& (Math.abs(work[Matrix.MTRANS_X] - mat[12]) < eps)
			&& (Math.abs(work[Matrix.MTRANS_Y] - mat[13]) < eps)
			&& (Math.abs(work[Matrix.MPERSP_2] - mat[15]) < eps);
	}
//--------------------------------------------------------------------------------
	/**
	 * ImageView.ScaleTypeと同じ
	 */
	public enum ScaleType {
		MATRIX(0),
		FIT_XY(1),
		FIT_START(2),
		FIT_CENTER(3),
		FIT_END(4),
		CENTER(5),
		CENTER_CROP(6),
		CENTER_INSIDE(7);

		ScaleType(final int id) {
			this.id = id;
		}

		final int id;
	}

	private static final Map<ImageView.ScaleType, ScaleType> sScaleTypeMap = new HashMap<>();
	static {
		sScaleTypeMap.put(ImageView.ScaleType.MATRIX, ScaleType.MATRIX);
		sScaleTypeMap.put(ImageView.ScaleType.FIT_XY, ScaleType.FIT_XY);
		sScaleTypeMap.put(ImageView.ScaleType.FIT_START, ScaleType.FIT_START);
		sScaleTypeMap.put(ImageView.ScaleType.FIT_CENTER, ScaleType.FIT_CENTER);
		sScaleTypeMap.put(ImageView.ScaleType.FIT_END, ScaleType.FIT_END);
		sScaleTypeMap.put(ImageView.ScaleType.CENTER, ScaleType.CENTER);
		sScaleTypeMap.put(ImageView.ScaleType.CENTER_CROP, ScaleType.CENTER_CROP);
		sScaleTypeMap.put(ImageView.ScaleType.CENTER_INSIDE, ScaleType.CENTER_INSIDE);
	}

	/**
	 * MatrixUtils.ScaleTypeからImageView.ScaleTypeへ変換
	 * @param scaleType
	 * @return
	 */
	@NonNull
	public static ImageView.ScaleType toImageViewScaleType(@NonNull final ScaleType scaleType) {
		for (final  Map.Entry<ImageView.ScaleType, ScaleType> entry: sScaleTypeMap.entrySet()) {
			if (entry.getValue() == scaleType) {
				return entry.getKey();
			}
		}
		return ImageView.ScaleType.CENTER_CROP;
	}

	/**
	 * ImageView.ScaleTypeからMatrixUtils.ScaleTypeへ変換
	 * @param scaleType
	 * @return
	 */
	@NonNull
	public static ScaleType fromImageViewScaleType(@NonNull final ImageView.ScaleType scaleType) {
		if (sScaleTypeMap.containsKey(scaleType)) {
			return sScaleTypeMap.get(scaleType);
		} else {
			return ScaleType.CENTER_CROP;
		}
	}

	/**
	 * 指定したスケーリング方法で描画用のMatrixを設定する
	 * @param scaleType
	 * @param bounds
	 * @param drawMatrix
	 * @param dwidth
	 * @param dheight
	 */
	public static void updateDrawMatrix(
		@NonNull final ImageView.ScaleType scaleType,
		@NonNull final Matrix drawMatrix,
		@NonNull final Rect bounds,
		final float dwidth, final float dheight) {

		updateDrawMatrix(fromImageViewScaleType(scaleType),
			drawMatrix,
			bounds.width(), bounds.height(),
			dwidth, dheight);
	}

	/**
	 * 指定したスケーリング方法で描画用のMatrixを設定する
	 * @param scaleType
	 * @param drawMatrix
	 * @param vwidth
	 * @param vheight
	 * @param dwidth
	 * @param dheight
	 */
	public static void updateDrawMatrix(
		@NonNull final ScaleType scaleType,
		@NonNull final Matrix drawMatrix,
		final float vwidth, final float vheight,
		final float dwidth, final float dheight) {

	    if ((dwidth <= 0) || (dheight <= 0) || (vwidth <= 0) || (vheight <= 0)) {
			drawMatrix.reset();
	        return;
	    }

		if (scaleType == ScaleType.CENTER_CROP) {
			final float scale;
			float dx = 0, dy = 0;

			if (dwidth * vheight > vwidth * dheight) {
				scale = vheight / dheight;
				dx = (vwidth - dwidth * scale) * 0.5f;
			} else {
				scale = vwidth / dwidth;
				dy = (vheight - dheight * scale) * 0.5f;
			}

			drawMatrix.setScale(scale, scale);
			drawMatrix.postTranslate(Math.round(dx), Math.round(dy));
		} else if (scaleType == ScaleType.CENTER_INSIDE) {
			final float scale;
			if (dwidth <= vwidth && dheight <= vheight) {
				scale = 1.0f;
			} else {
				scale = Math.min(vwidth / dwidth, vheight / dheight);
			}

			final float dx = Math.round((vwidth - dwidth * scale) * 0.5f);
			final float dy = Math.round((vheight - dheight * scale) * 0.5f);

			drawMatrix.setScale(scale, scale);
			drawMatrix.postTranslate(dx, dy);
		} else if (scaleType == ScaleType.CENTER) {
			drawMatrix.setTranslate(
				Math.round((vwidth - dwidth) * 0.5f),
				Math.round((vheight - dheight) * 0.5f));
		} else {
			final RectF dstBounds = new RectF(0, 0, vwidth, vheight);
			final RectF srcBounds = new RectF(0, 0, dwidth, dheight);
			switch (scaleType) {
			case FIT_XY ->
				drawMatrix.setRectToRect(srcBounds, dstBounds, Matrix.ScaleToFit.FILL);
			case FIT_START ->
				drawMatrix.setRectToRect(srcBounds, dstBounds, Matrix.ScaleToFit.START);
			case FIT_CENTER ->
				drawMatrix.setRectToRect(srcBounds, dstBounds, Matrix.ScaleToFit.CENTER);
			case FIT_END ->
				drawMatrix.setRectToRect(srcBounds, dstBounds, Matrix.ScaleToFit.END);
			default -> { // do nothing
			}
			}
		}
	}

}
