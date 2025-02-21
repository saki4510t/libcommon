package com.serenegiant.gl;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @deprecated GLEffectDrawer2Dを使うこと
 */
@Deprecated
public class EffectDrawer2D extends GLEffectDrawer2D{
	public EffectDrawer2D(final boolean isGLES3, final boolean isOES) {
		super(isGLES3, isOES);
	}

	public EffectDrawer2D(final boolean isGLES3, final boolean isOES, @Nullable final EffectListener effectListener) {
		super(isGLES3, isOES, effectListener);
	}

	public EffectDrawer2D(final boolean isGLES3, final float[] vertices, final float[] texcoord, final boolean isOES) {
		super(isGLES3, vertices, texcoord, isOES);
	}

	public EffectDrawer2D(final boolean isGLES3, final float[] vertices, final float[] texcoord, final boolean isOES, @Nullable final EffectListener effectListener) {
		super(isGLES3, vertices, texcoord, isOES, effectListener);
	}

	protected EffectDrawer2D(final boolean isGLES3, final boolean isOES, @NonNull final float[] vertices, @NonNull final float[] texcoord, @Nullable final String vs, @Nullable final String fs, @Nullable final EffectListener effectListener) {
		super(isGLES3, isOES, vertices, texcoord, vs, fs, effectListener);
	}
}
