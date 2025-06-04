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

import static com.serenegiant.gl.ShaderConst.FRAGMENT_SHADER_DOCUMENTARY_ES2;

/**
 * ドキュメンタリーフィルター
 */
public class MediaEffectGLDocumentary extends MediaEffectGLBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectGLDocumentary";

	public MediaEffectGLDocumentary() {
		super(new MediaEffectGLColorAdjustDrawer(FRAGMENT_SHADER_DOCUMENTARY_ES2));
		setParameter(1f);
	}

	/**
	 * @param scale The scale of vignetting. between 0 and 1. 0 means no change.
	 * @return
	 */
	public void setParameter(final float scale) {
		((MediaEffectGLColorAdjustDrawer)mDrawer).setColorAdjust(scale);
	}

}
