package com.serenegiant.libcommon;
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
import android.util.Log;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.MatrixUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MatrixTest {
	private static final String TAG = MatrixTest.class.getSimpleName();

	private static final double TO_RADIAN = (Math.PI / 180.0);
	private static final double TO_DEGREE = (180.0 / Math.PI);
	private static final float EPS = 0.000001f;

	@Test
	public void rotateTest() {
		final Matrix matrix = new Matrix();	// for android graphics
		final float[] mat = new float[16];	// for OpenGL
		for (int i = 0; i < 361; i += 90) {
			Log.v(TAG, "rotateTest:" + i);
			matrix.setRotate(i);
			// xy平面で回転=z軸周りで回転
			android.opengl.Matrix.setRotateM(mat, 0, i, 0.0f, 0.0f, 1.0f);
			assertTrue(compare(matrix, mat, true));
			final float[] glMat = MatrixUtils.toGLMatrix(matrix);
			assertTrue(compare(matrix, glMat, true));
			final Matrix m = MatrixUtils.toAndroidMatrix(glMat);
			assertTrue(compare(m, glMat, true));
		}
	}

	@Test
	public void mirrorTest() {
		final Matrix matrix = new Matrix();	// for android graphics
		final float[] mat = new float[16];	// for OpenGL

		for (int i = 0; i < IMirror.MIRROR_NUM; i++) {
			android.opengl.Matrix.setIdentityM(mat, 0);
			matrix.reset();
			MatrixUtils.setMirror(mat, i);
			MatrixUtils.setMirror(matrix, i);
			assertTrue(compare(matrix, mat, true));
			final float[] glMat = MatrixUtils.toGLMatrix(matrix);
			assertTrue(compare(matrix, glMat, true));
			final Matrix m = MatrixUtils.toAndroidMatrix(glMat);
			assertTrue(compare(m, glMat, true));
		}
	}

	public static boolean compare(
		@NonNull final Matrix matrix,
		@NonNull @Size(min=16)final float[] mat,
		final boolean dumpOnError) {

		final boolean result = MatrixUtils.compare(matrix, mat, EPS);
		if (!result && dumpOnError) {
			Log.i(TAG, "a=" + MatrixUtils.toString(matrix));
			Log.i(TAG, "b=" + MatrixUtils.toGLMatrixString(mat));
		}

		return result;
	}
}
