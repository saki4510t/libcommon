package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.opengl.GLES20;

import static com.serenegiant.glutils.ShaderConst.*;

public class MediaEffectColorAdjustDrawer extends MediaEffectDrawer {
	private int muColorAdjustLoc;		// 色調整
	private float mColorAdjust;

	public MediaEffectColorAdjustDrawer(final String fss) {
		this(false, VERTEX_SHADER, fss);
	}

	public MediaEffectColorAdjustDrawer(final boolean isOES, final String fss) {
		this(isOES, VERTEX_SHADER, fss);
	}

	public MediaEffectColorAdjustDrawer(final boolean isOES, final String vss, final String fss) {
		super(isOES, vss, fss);
		muColorAdjustLoc = GLES20.glGetUniformLocation(getProgram(), "uColorAdjust");
		if (muColorAdjustLoc < 0) {
			muColorAdjustLoc = -1;
		}
	}

	public void setColorAdjust(final float adjust) {
		synchronized (mSync) {
			mColorAdjust = adjust;
		}
	}

	@Override
	protected void preDraw(final int tex_id, final float[] tex_matrix, final int offset) {
		super.preDraw(tex_id, tex_matrix, offset);
		// 色調整オフセット
		if (muColorAdjustLoc >= 0) {
			GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
		}
	}
}
