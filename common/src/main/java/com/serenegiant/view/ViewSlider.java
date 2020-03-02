package com.serenegiant.view;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;

import com.serenegiant.common.R;
import com.serenegiant.view.animation.ResizeAnimation;

import java.util.Locale;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

/**
 * Snackbar風にViewをアニメーションさせるためのヘルパークラス
 */
public class ViewSlider {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = ViewSlider.class.getSimpleName();

	private static final int DURATION_RESIZE_MS = 300;

	/**
	 * 親View
	 */
	@NonNull
	private final View mParent;
	/**
	 * アニメーションさせるView
	 */
	@NonNull
	private final View mTarget;

	private int mTargetWidth;
	private int mTargetHeight;

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param viewId アニメーションさせるViewのid
	 * @throws IllegalArgumentException
	 */
	public ViewSlider(@NonNull final View parent, @IdRes final int viewId)
		throws IllegalArgumentException{

		mParent = parent;
		mTarget = parent.findViewById(viewId);
		if (mTarget == null) {
			throw new IllegalArgumentException("Target view not found");
		}
		mTargetWidth = mTarget.getWidth();
		mTargetHeight = mTarget.getHeight();
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:size(%dx%d)", mTargetWidth, mTargetHeight));
		mParent.addOnLayoutChangeListener(mOnLayoutChangeListener);
		mTarget.addOnLayoutChangeListener(mOnLayoutChangeListener);
		if (mTarget.getVisibility() == View.GONE) {
			mTarget.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public void release() {
		mTarget.clearAnimation();
		mTarget.removeOnLayoutChangeListener(mOnLayoutChangeListener);
	}

	/**
	 * ターゲットViewをスライドイン
	 * @param autoHideDurationMs
	 */
	public void show(final long autoHideDurationMs) {
		mParent.post(new Runnable() {
			@Override
			public void run() {
				if (DEBUG) Log.i(TAG, String.format(Locale.US,
					"show:size(%d,%d)",
						mTarget.getWidth(), mTarget.getHeight()));
				mTarget.clearAnimation();
				final ResizeAnimation expandAnimation
					= new ResizeAnimation(mTarget,
						mParent.getWidth(), 0,
						mParent.getWidth(), mTargetHeight);
				expandAnimation.setDuration(DURATION_RESIZE_MS);
				expandAnimation.setAnimationListener(mAnimationListener);
				mTarget.setTag(R.id.visibility, 1);
				mTarget.setTag(R.id.auto_hide_duration, autoHideDurationMs);
				mParent.postDelayed(new Runnable() {
					@Override
					public void run() {
						mTarget.setVisibility(View.VISIBLE);
						mTarget.startAnimation(expandAnimation);
					}
				}, 50);
			}
		});
	}

	/**
	 * ターゲットViewをスライドアウト
	 * @param durationMs
	 */
	public void hide(final long durationMs) {
		mParent.post(new Runnable() {
			@Override
			public void run() {
				if (mTarget.getVisibility() == View.VISIBLE) {
					if (DEBUG) Log.v(TAG,
						String.format(Locale.US, "hide:size(%d,%d)",
						mTarget.getWidth(), mTarget.getHeight()));
					mTarget.clearAnimation();
					final ResizeAnimation collapseAnimation
						= new ResizeAnimation(mTarget,
							mParent.getWidth(), mTarget.getHeight(),
							mParent.getWidth(), 0);
					collapseAnimation.setDuration(durationMs);
					collapseAnimation.setAnimationListener(mAnimationListener);
					mTarget.setTag(R.id.visibility, 0);
					mTarget.startAnimation(collapseAnimation);
				}
			}
		});
	}

	public void setTargetWidth(final int width) {
		mTargetWidth = width;
	}

	public void setTargetHeight(final int height) {
		mTargetHeight = height;
	}

	public int getVisibility() {
		return mTarget.getVisibility();
	}

	private final View.OnLayoutChangeListener mOnLayoutChangeListener
		= new View.OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(final View v,
			final int left, final int top, final int right, final int bottom,
			final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {

			if (DEBUG) Log.v(TAG, String.format("onLayoutChange:(%d,%d - %d,%d) → (%d,%d - %d,%d)",
				oldLeft, oldTop, oldRight, oldBottom,
				left, top, right, bottom) + ",view=" + v.getClass().getSimpleName());
			if (mTargetWidth <= 0) {
				mTargetWidth = v.getWidth();
			}
			if (mTargetHeight <= 0) {
				mTargetHeight = v.getHeight();
			}
		}
	};

	private final Animation.AnimationListener mAnimationListener
		= new Animation.AnimationListener() {
		@Override
		public void onAnimationStart(final Animation animation) {
			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}

		@Override
		public void onAnimationEnd(final Animation animation) {
			if (DEBUG) Log.v(TAG, "onAnimationEnd:");
			final Object visibility = mTarget.getTag(R.id.visibility);
			if ((visibility instanceof Integer) && ((Integer)visibility == 1)) {
				mTarget.setTag(R.id.visibility, 0);
				final Object durationObj = mTarget.getTag(R.id.auto_hide_duration);
				final long duration = (durationObj instanceof Long) ? (Long)durationObj : 0;
				if (duration > 0) {
					mTarget.postDelayed(new Runnable() {
						@Override
						public void run() {
							hide(DURATION_RESIZE_MS);
						}
					}, duration);
				}
			} else {
				mTarget.setVisibility(View.GONE);
			}
		}

		@Override
		public void onAnimationRepeat(final Animation animation) {
			if (DEBUG) Log.v(TAG, "onAnimationRepeat:");
		}
	};
}
