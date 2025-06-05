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

import androidx.annotation.Nullable;
import androidx.annotation.Size;

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_COLOR_MATRIX_ES2;
import static com.serenegiant.gl.ShaderConst.VERTEX_SHADER_ES2;

/** GL|ESで色変換行列を適用するMediaEffectGLBase実装 */
public class MediaEffectGLColorMatrix extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLColorMatrix";

	/*
	 * R' | m11 m21 m31 m41 | R
	 * G' | m12 m22 m32 m42 | G
	 * B' | m13 m23 m33 m43 | B
	 * 1  | m14 m24 m34 m44 | 1
	 *
	 * R' = m11R + m21G + m31B + m41
	 * G' = m12R + m22G + m32B + m42
	 * B' = m13R + m23G + m33B + m43
	 * A' = A
	 */

	/**
	 * コンストラクタ
	 * 色変換行列として単位行列を割り当てる(=単純コピー)
	 */
	public MediaEffectGLColorMatrix() {
		this(null, 0);
	}

	/**
	 * コンストラクタ
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	public MediaEffectGLColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		super(new MediaEffectGLColorAdjustDrawer(
			false, FRAGMENT_SHADER_COLOR_MATRIX_ES2));
		setColorMatrix(colorMatrix, offset);
	}

	/**
	 * 色変換行列をセット
	 * 指定したcolorMatrixがnullまたは要素数が16+offsetよりも小さい場合は単位行列になる
	 * @param colorMatrix 色変換行列
	 * @param offset
	 */
	public void setColorMatrix(@Nullable @Size(min=16) final float[] colorMatrix, final int offset) {
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorMatrix(colorMatrix, offset);
	}
}
