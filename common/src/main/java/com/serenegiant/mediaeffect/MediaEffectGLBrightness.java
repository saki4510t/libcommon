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

import static com.serenegiant.gl.ShaderConst.*;

/** 明るさ調整([-1.0f,+1.0f], RGB各成分に単純加算), 0だと無調整 */
public class MediaEffectGLBrightness extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLBrightness";

	public MediaEffectGLBrightness() {
		this(0.0f);
	}

	public MediaEffectGLBrightness(final float brightness) {
		super(new MediaEffectGLColorAdjustDrawer(FRAGMENT_SHADER_BRIGHTNESS_ES2));
		setParameter(brightness);
	}

	/**
	 * 露出調整
	 * @param brightness [-1.0f,+1.0f], RGB各成分に単純加算)
	 * @return
	 */
	public MediaEffectGLBrightness setParameter(final float brightness) {
		setEnable(brightness != 0.0f);
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorAdjust(brightness);
		return this;
	}
}
