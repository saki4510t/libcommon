package com.serenegiant.view.animation;
/*
 * This class originally came from Android Developers.
 * https://developer.android.com/training/animation/screen-slide.html
 * Content and code samples on this page are subject to the licenses described in the Content License
 */

import android.os.Build;
import android.view.View;

public class DepthPageTransformer {
	private static final float MIN_SCALE = 0.75f;

	public void transformPage(final View view, final float position) {
		int pageWidth = view.getWidth();

		if (position < -1) { // [-Infinity,-1)
			// This page is way off-screen to the left.
			view.setAlpha(0f);

		} else if (position <= 0) { // [-1,0]
			// Use the default slide transition when moving to the left page
			view.setAlpha(1f);
			view.setTranslationX(0f);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				view.setTranslationZ(0f);
			}
			view.setScaleX(1f);
			view.setScaleY(1f);

		} else if (position <= 1) { // (0,1]
			// Fade the page out.
			view.setAlpha(1 - position);

			// Counteract the default slide transition
			view.setTranslationX(pageWidth * -position);
			// Move it behind the left page
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				view.setTranslationZ(-1f);
			}

			// Scale the page down (between MIN_SCALE and 1)
			float scaleFactor = MIN_SCALE
				+ (1 - MIN_SCALE) * (1 - Math.abs(position));
			view.setScaleX(scaleFactor);
			view.setScaleY(scaleFactor);

		} else { // (1,+Infinity]
			// This page is way off-screen to the right.
			view.setAlpha(0f);
		}
	}
}
