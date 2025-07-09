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
import androidx.annotation.Nullable;

import android.text.TextUtils;

public class MediaEffect implements IMediaEffect {
	@NonNull
	protected final EffectContext mEffectContext;
	@Nullable
	protected Effect mEffect;
	protected boolean mEnabled;

	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 * @param effectContext
	 * @param effectName
	 */
	public MediaEffect(@NonNull final EffectContext effectContext, final String effectName) {
		mEffectContext = effectContext;
		final EffectFactory factory = effectContext.getFactory();
		if (TextUtils.isEmpty(effectName)) {
			mEffect = null;
		} else {
			mEffect = factory.createEffect(effectName);
		}
		mEnabled = mEffect != null;
	}

	/**
	 * GLコンテキスト上で実行すること
	 * @param src
	 */
	@Override
	public void apply(@NonNull final ISource src) {
		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src.getSourceTexId()[0],
				src.getWidth(), src.getHeight(),
				src.getOutputTargetTexId());
		}
	}

	@Override
	public void release() {
		if (mEffect != null) {
			mEffect.release();
			mEffect = null;
		}
		mEnabled = false;
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
