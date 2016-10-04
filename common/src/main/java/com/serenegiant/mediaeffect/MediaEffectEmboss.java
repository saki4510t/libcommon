package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

public class MediaEffectEmboss extends MediaEffectKernel {

	private float mIntensity;
	public MediaEffectEmboss() {
		this(1.0f);
	}

	public MediaEffectEmboss(final float intensity) {
		super(new float[] {
				intensity * (-2.0f), -intensity, 0.0f,
				-intensity, 1.0f, intensity,
				0.0f, intensity, intensity * 2.0f,
			});
		mIntensity = intensity;
	}

	public MediaEffectEmboss setParameter(final float intensity) {
		if (mIntensity != intensity) {
			mIntensity = intensity;
			setParameter(new float[] {
				intensity * (-2.0f), -intensity, 0.0f,
				-intensity, 1.0f, intensity,
				0.0f, intensity, intensity * 2.0f,
			}, 0.0f);
		}
		return this;
	}
}
