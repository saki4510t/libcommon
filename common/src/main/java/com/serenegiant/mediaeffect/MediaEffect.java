package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.text.TextUtils;

public class MediaEffect implements IEffect {
	protected final EffectContext mEffectContext;
	protected Effect mEffect;
	protected boolean mEnabled = true;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 * @param effect_context
	 */
	public MediaEffect(final EffectContext effect_context, final String effectName) {
		mEffectContext = effect_context;
		final EffectFactory factory = effect_context.getFactory();
		if (TextUtils.isEmpty(effectName)) {
			mEffect = null;
		} else {
			mEffect = factory.createEffect(effectName);
		}
	}

	@Override
	public void apply(final int [] src_tex_ids, final int width, final int height, final int out_tex_id) {
		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src_tex_ids[0], width, height, out_tex_id);
		}
	}

	@Override
	public void apply(final ISource src) {
		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src.getSourceTexId()[0], src.getWidth(), src.getHeight(), src.getOutputTexId());
		}
	}

	@Override
	public void release() {
		if (mEffect != null) {
			mEffect.release();
			mEffect = null;
		}
	}

	@Override
	public MediaEffect resize(final int width, final int height) {
		// ignore
		return this;
	}

	protected MediaEffect setParameter(final String parameterKey, final Object value) {
		if ((mEffect != null) && (value != null)) {
			mEffect.setParameter(parameterKey, value);
		}
		return this;
	}

	@Override
	public boolean enabled() {
		return mEnabled;
	}

	@Override
	public IEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}
}
