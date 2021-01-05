package com.serenegiant.mediaeffect;
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

import androidx.annotation.NonNull;

import com.serenegiant.glutils.GLSurface;

public class MediaEffectGLTwoPassBase extends MediaEffectGLBase {

	protected final MediaEffectKernel3x3Drawer mDrawer2;
	protected GLSurface mOutputOffscreen2;

	public MediaEffectGLTwoPassBase(final int numTex,
									final boolean isOES, final String fss) {

		super(numTex, isOES, fss);
		mDrawer2 = null;
	}

	public MediaEffectGLTwoPassBase(final int numTex,
									final String vss, final String fss) {

		super(numTex, false, vss, fss);
		mDrawer2 = null;
	}

	public MediaEffectGLTwoPassBase(final int numTex,
									final boolean isOES, final String vss, final String fss) {

		super(numTex, isOES, vss, fss);
		mDrawer2 = null;
	}

	public MediaEffectGLTwoPassBase(final int numTex, final boolean isOES,
									final String vss1, final String fss1,
									final String vss2, final String fss2) {

		super(numTex, isOES, vss1, fss1);
		if (!vss1.equals(vss2) || !fss1.equals(fss2)) {
			mDrawer2 = new MediaEffectKernel3x3Drawer(isOES, vss2, fss2);
		} else {
			mDrawer2 = null;
		}
	}

	@Override
	public void release() {
		if (mDrawer2 != null) {
			mDrawer2.release();
		}
		if (mOutputOffscreen2 != null) {
			mOutputOffscreen2.release();
			mOutputOffscreen2 = null;
		}
		super.release();
	}

	@Override
	public MediaEffectGLBase resize(final int width, final int height) {
		super.resize(width, height);
		// ISourceを使う時は出力用オフスクリーンは不要なのと
		// ISourceを使わない時は描画時にチェックして生成するのでresize時には生成しないように変更
/*		if ((mOutputOffscreen2 == null) || (width != mOutputOffscreen2.getWidth())
			|| (height != mOutputOffscreen2.getHeight())) {
			if (mOutputOffscreen2 != null)
				mOutputOffscreen2.release();
			mOutputOffscreen2 = new GLSurface(width, height, false);
		} */
		if (mDrawer2 != null) {
			mDrawer2.setTexSize(width, height);
		}
		return this;
	}

	/**
	 * If you know the source texture came from MediaSource,
	 * using #apply(MediaSource) is much efficient instead of this
	 * @param src_tex_ids
	 * @param width
	 * @param height
	 * @param out_tex_id
	 */
	@Override
	public void apply(@NonNull final int [] src_tex_ids,
		final int width, final int height, final int out_tex_id) {

		if (!mEnabled) return;
		// パス1
		if (mOutputOffscreen == null) {
			mOutputOffscreen = GLSurface.newInstance(false, width, height, false);
		}
		mOutputOffscreen.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, mOutputOffscreen.copyTexMatrix(), 0);
		} finally {
			mOutputOffscreen.swap();
		}

		if (mOutputOffscreen2 == null) {
			mOutputOffscreen2 = GLSurface.newInstance(false, width, height, false);
		}
		// パス2
		if ((out_tex_id != mOutputOffscreen2.getTexId())
			|| (width != mOutputOffscreen2.getWidth())
			|| (height != mOutputOffscreen2.getHeight())) {
			mOutputOffscreen2.assignTexture(out_tex_id, width, height);
		}
		mOutputOffscreen2.makeCurrent();
		final int[] ids = new int[] { mOutputOffscreen.getTexId() };
		try {
			if (mDrawer2 != null) {
				mDrawer2.apply(ids, mOutputOffscreen2.copyTexMatrix(), 0);
			} else {
				mDrawer.apply(ids, mOutputOffscreen2.copyTexMatrix(), 0);
			}
		} finally {
			mOutputOffscreen2.swap();
		}
	}

	@Override
	public void apply(@NonNull final int[] src_tex_ids,
		@NonNull final GLSurface output) {


		if (!mEnabled) return;
		// パス1
		if (mOutputOffscreen == null) {
			mOutputOffscreen = GLSurface.newInstance(false,
				output.getWidth(), output.getHeight(), false);
		}
		mOutputOffscreen.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, mOutputOffscreen.copyTexMatrix(), 0);
		} finally {
			mOutputOffscreen.swap();
		}
		// パス2
		output.makeCurrent();
		final int[] ids = new int[] { mOutputOffscreen.getTexId() };
		try {
			if (mDrawer2 != null) {
				mDrawer2.apply(ids, output.copyTexMatrix(), 0);
			} else {
				mDrawer.apply(ids, output.copyTexMatrix(), 0);
			}
		} finally {
			output.swap();
		}
	}
	
	@Override
	public void apply(final ISource src) {
		if (!mEnabled) return;
		final GLSurface output_tex = src.getOutputTexture();
		final int[] src_tex_ids = src.getSourceTexId();
		final int width = src.getWidth();
		final int height = src.getHeight();
		// パス1
		if (mOutputOffscreen == null) {
			mOutputOffscreen = GLSurface.newInstance(false, width, height, false);
		}
		mOutputOffscreen.makeCurrent();
		try {
			mDrawer.apply(src_tex_ids, mOutputOffscreen.copyTexMatrix(), 0);
		} finally {
			mOutputOffscreen.swap();
		}
		// パス2
		output_tex.makeCurrent();
		final int[] ids = new int[] { mOutputOffscreen.getTexId() };
		try {
			if (mDrawer2 != null) {
				mDrawer2.apply(ids, output_tex.copyTexMatrix(), 0);
			} else {
				mDrawer.apply(ids, output_tex.copyTexMatrix(), 0);
			}
		} finally {
			output_tex.swap();
		}
	}
}
