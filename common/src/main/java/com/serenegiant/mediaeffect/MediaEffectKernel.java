package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import static com.serenegiant.glutils.ShaderConst.*;

public class MediaEffectKernel extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectKernel";

	public MediaEffectKernel() {
		super(new MediaEffectKernel3x3Drawer(false, VERTEX_SHADER, MediaEffectKernel3x3Drawer.FRAGMENT_SHADER_2D));
	}

	public MediaEffectKernel(final float[] kernel) {
		this();
		setParameter(kernel, 0.0f);
	}

	public MediaEffectKernel(final float[] kernel, final float color_adjust) {
		this();
		setParameter(kernel, color_adjust);
	}

	@Override
	public MediaEffectKernel resize(final int width, final int height) {
		super.resize(width, height);
		setTexSize(width, height);
		return this;
	}

	public void setKernel(final float[] values, final float colorAdj) {
		((MediaEffectKernel3x3Drawer)mDrawer).setKernel(values, colorAdj);
	}

	public void setColorAdjust(final float adjust) {
		((MediaEffectKernel3x3Drawer)mDrawer).setColorAdjust(adjust);
	}

	/**
	 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
	 */
	public void setTexSize(final int width, final int height) {
		mDrawer.setTexSize(width, height);
	}

	/**
	 * synonym of setKernel
	 * @param kernel
	 * @param color_adjust
	 * @return
	 */
	public MediaEffectKernel setParameter(final float[] kernel, final float color_adjust) {
		setKernel(kernel, color_adjust);
		return this;
	}
}
