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

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.serenegiant.gl.GLSurface;

public class MediaEffect implements IMediaEffect {
	protected final EffectContext mEffectContext;
	protected Effect mEffect;
	protected boolean mEnabled = true;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 * @param effectContext
	 */
	public MediaEffect(final EffectContext effectContext, final String effectName) {
		mEffectContext = effectContext;
		final EffectFactory factory = effectContext.getFactory();
		if (TextUtils.isEmpty(effectName)) {
			mEffect = null;
		} else {
			mEffect = factory.createEffect(effectName);
		}
	}

	@Override
	public void apply(@NonNull final int [] srcTexIds,
		final int width, final int height, final int outTexId) {

		if (mEnabled && (mEffect != null)) {
			mEffect.apply(srcTexIds[0], width, height, outTexId);
		}
	}

	@Override
	public void apply(@NonNull final int [] srcTexIds,
		@NonNull final GLSurface output) {

		if (mEnabled && (mEffect != null)) {
			mEffect.apply(srcTexIds[0],
				output.getWidth(), output.getHeight(),
				output.getTexId());
		}
	}

	@Override
	public void apply(final ISource src) {
		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src.getSourceTexId()[0],
				src.getWidth(), src.getHeight(),
				src.getOutputTexId());
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
	public IMediaEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}
}
