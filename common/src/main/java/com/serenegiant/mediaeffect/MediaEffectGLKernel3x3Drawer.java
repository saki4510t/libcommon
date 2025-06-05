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
import androidx.annotation.NonNull;

import com.serenegiant.gl.GLUtils;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * カーネル行列を用いた映像フィルタ処理
 * MediaEffectSingleDrawerを継承しているので、使用できるテクスチャは1つだけ
 */
public class MediaEffectGLKernel3x3Drawer extends MediaEffectGLColorAdjustDrawer {

	public static final int KERNEL_SIZE = 9;
	private final int muKernelLoc;		// カーネル行列(float配列)
	private final int muTexOffsetLoc;	// テクスチャオフセット(カーネル行列用)
	private final float[] mKernel = new float[KERNEL_SIZE * 2];	// Inputs for convolution filter based shaders
	private final float[] mTexOffset = new float[KERNEL_SIZE * 2];
	private int mKernelSize = KERNEL_SIZE;
	private float mTexWidth;
	private float mTexHeight;

	public MediaEffectGLKernel3x3Drawer(final String fss) {
		this(false, VERTEX_SHADER_ES2, fss);
	}

	public MediaEffectGLKernel3x3Drawer(final boolean isOES, final String fss) {
		this(isOES, VERTEX_SHADER_ES2, fss);
	}

	public MediaEffectGLKernel3x3Drawer(final boolean isOES, final String vss, final String fss) {
		super(isOES, vss, fss, 0);
		muKernelLoc = GLES20.glGetUniformLocation(getProgram(), "uKernel");
		if (muKernelLoc < 0) {
			// no kernel in this one
			muTexOffsetLoc = -1;
		} else {
			// has kernel, must also have tex offset and color adj
			muTexOffsetLoc = GLES20.glGetUniformLocation(getProgram(), "uTexOffset");
//			GLUtils.checkLocation(muTexOffsetLoc, "uTexOffset");	// 未使用だと削除されてしまうのでチェックしない

			setKernel(KERNEL_NULL, 0f);
			setTexSize(256, 256);

//			GLUtils.checkLocation(muColorAdjustLoc, "uColorAdjust");	// 未使用だと削除されてしまうのでチェックしない
		}
	}

	@Override
	protected void preDraw(@NonNull final int[] texIds,
						   final float[] texMatrix, final int offset) {

		super.preDraw(texIds, texMatrix, offset);
		// カーネル関数(行列)
		if (muKernelLoc >= 0) {
			GLES20.glUniform1fv(muKernelLoc, mKernelSize, mKernel, 0);
			GLUtils.checkGlError("set kernel");
		}
		// テクセルオフセット
		if (muTexOffsetLoc >= 0) {
			GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
		}
	}

	public void setKernel(final float[] values, final float colorAdj) {
		if ((values == null) || (values.length < KERNEL_SIZE)) {
			throw new IllegalArgumentException("Kernel size is "
				+ (values != null ? values.length : 0) + " vs. " + KERNEL_SIZE);
		}
		synchronized (mSync) {
			if (values.length >= KERNEL_SIZE * 2) {
				mKernelSize = KERNEL_SIZE * 2;
			} else {
				mKernelSize = KERNEL_SIZE;
			}
			System.arraycopy(values, 0, mKernel, 0, mKernelSize);
			setColorAdjust(colorAdj);
		}
	}

	/**
	 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
	 */
	@Override
	public void setTexSize(final int width, final int height) {
		synchronized (mSync) {
			if ((mTexWidth != width) || (mTexHeight != height)) {
				mTexWidth = width;
				mTexHeight = height;
				final float rw = 1.0f / width;
				final float rh = 1.0f / height;

				mTexOffset[0] = -rw;	mTexOffset[1] = -rh;
				mTexOffset[2] = 0f;		mTexOffset[3] = -rh;
				mTexOffset[4] = rw;		mTexOffset[5] = -rh;

				mTexOffset[6] = -rw;	mTexOffset[7] = 0f;
				mTexOffset[8] = 0f;		mTexOffset[9] = 0f;
				mTexOffset[10] = rw;	mTexOffset[11] = 0f;

				mTexOffset[12] = -rw;	mTexOffset[13] = rh;
				mTexOffset[14] = 0f;	mTexOffset[15] = rh;
				mTexOffset[16] = rw;	mTexOffset[17] = rh;
			}
		}
	}

}
