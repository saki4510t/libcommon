package com.serenegiant.graphics;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class ShaderDrawable extends Drawable {

	private final Paint mPaint;
    private final DrawFilter mDrawFilter;
	private Shader mShader;

	public ShaderDrawable() {
		this(0, 0);
	}
	/**
	 * clearflagsにはPaint.XXX_FLAGをセット
	 * @param clearflags
	 */
	public ShaderDrawable(final int clearflags) {
		this(clearflags, 0);
	}

	/**
	 * clearflags, setFlagsにはPaint.XXX_FLAGをセット
	 * @param clearflags
	 * @param setFlags
	 */
	public ShaderDrawable(final int clearflags, final int setFlags) {
		mPaint = new Paint();
		mDrawFilter = new PaintFlagsDrawFilter(clearflags, setFlags);
	}

	@Override
	public void draw(final Canvas canvas) {
		if (mShader != null) {
			final int count = canvas.save();
			final DrawFilter org = canvas.getDrawFilter();
	        canvas.setDrawFilter(mDrawFilter);
	        mPaint.setShader(mShader);
	        canvas.drawPaint(mPaint);
	        canvas.setDrawFilter(org);
			canvas.restoreToCount(count);
		}
	}

	@Override
	public void setAlpha(final int alpha) {
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
		mPaint.setColorFilter(cf);
	}

	@Override
	@SuppressLint("Override")
	public ColorFilter getColorFilter() {
		return mPaint.getColorFilter();
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	public void setBounds(final RectF bounds) {
		super.setBounds((int)bounds.left, (int)bounds.top, (int)bounds.right, (int)bounds.bottom);
	}

	public void setBounds(final float left, final float top, final float right, final float bottom) {
		super.setBounds((int)left, (int)top, (int)right, (int)bottom);
	}

	public Shader setShader(final Shader shader) {
		if (mShader != shader) {
			mShader = shader;
		}
		return shader;
	}

	public Shader getShader() {
		return mShader;
	}
}
