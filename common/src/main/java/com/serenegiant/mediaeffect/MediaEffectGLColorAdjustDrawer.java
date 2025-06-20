package com.serenegiant.mediaeffect;
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

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * 色調整可能なMediaEffectGLDrawer.MediaEffectSingleDrawer実装
 * MediaEffectSingleDrawerを継承しているので、使用できるテクスチャは1つだけ
 */
public class MediaEffectGLColorAdjustDrawer
	extends MediaEffectGLDrawer.MediaEffectSingleDrawer {

	private final int muColorAdjustLoc;		// 色調整
	private final int muColorMatrixLoc;		// 4x4色変換行列
	private final int muParamsLoc;			// 補正パラメータ
	private float mColorAdjust;				// 色調整オフセット
	@Size(value=16)
	@NonNull
	protected final float[] mColorMatrix = new float[16];
	@NonNull
	protected final float[] mParams;

	public MediaEffectGLColorAdjustDrawer(final String fss) {
		this(false, VERTEX_SHADER_ES2, fss, 0);
	}

	public MediaEffectGLColorAdjustDrawer(
		final boolean isOES, final String fss) {

		this(isOES, VERTEX_SHADER_ES2, fss, 0);
	}

	public MediaEffectGLColorAdjustDrawer(
		final boolean isOES, final String vss, final String fss,
		final int numParams) {

		super(isOES, vss, fss);
		mParams = new float[numParams];
		int uColorAdjust = GLES20.glGetUniformLocation(getProgram(), "uColorAdjust");
		if (uColorAdjust < 0) {
			uColorAdjust = -1;
		}
		muColorAdjustLoc = uColorAdjust;
		int muColorMatrix = GLES20.glGetUniformLocation(getProgram(), "uColorMatrix");
		if (muColorMatrix < 0) {
			muColorMatrix = -1;
		}
		muColorMatrixLoc = muColorMatrix;
		int uParams = GLES20.glGetUniformLocation(getProgram(), "uParams");
		if (uParams < 0) {
			uParams = -1;
		}
		muParamsLoc = uParams;
		Matrix.setIdentityM(mColorMatrix, 0);
		setParams(null, 0);
	}

	public void setColorAdjust(final float adjust) {
		synchronized (mSync) {
			mColorAdjust = adjust;
		}
	}

	/**
	 * 色変換行列をセット
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	public void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		synchronized (mSync) {
			if ((colorMatrix != null) && (colorMatrix.length >= 16 + offset)) {
				System.arraycopy(colorMatrix, offset, mColorMatrix, 0, mColorMatrix.length);
			} else {
				Matrix.setIdentityM(mColorMatrix, 0);
			}
		}
	}

	/**
	 * 補正パラメータをセット
	 * paramsがnullまたは必要な個数に満たない場合は0クリアする
	 * @param params
	 * @param offset
	 */
	public void setParams(@Nullable @Size(min=16) final float[] params, final int offset) {
		synchronized (mSync) {
			if ((params != null) && (params.length >= mParams.length + offset)) {
				System.arraycopy(params, offset, mParams, 0, mParams.length);
			} else {
				Arrays.fill(mParams, 0.0f);
			}
		}
	}

	@Override
	protected void preDraw(@NonNull final int[] texIds,
		final float[] texMatrix, final int offset) {

		super.preDraw(texIds, texMatrix, offset);
		// 色調整オフセット
		if (muColorAdjustLoc >= 0) {
			synchronized (mSync) {
				GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
			}
		}
		if (muColorMatrixLoc >= 0) {
			GLES20.glUniformMatrix4fv(muColorMatrixLoc, 1, false, mColorMatrix, 0);
		}
		final int n = mParams.length;
		if ((muParamsLoc >= 0) && (n > 0)) {
			GLES20.glUniform1fv(muParamsLoc, n, mParams, 0);
		}
	}
}
