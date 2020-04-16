package com.serenegiant.view;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;

import com.serenegiant.common.R;
import com.serenegiant.view.animation.ResizeAnimation;

import java.lang.annotation.Retention;
import java.util.Locale;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Snackbar風にViewをアニメーションさせるためのヘルパークラス
 * XXX 直接の親がConstraintLayoutのときはうまく動かない
 * (アニメーション開始/終了時にConstraintLayout全体にターゲットViewが一瞬表示される)
 * 正常にアニメーションを行うにはターゲットViewのleft, top, right, bottomのいずれかが親Viewに対して固定されている必要がある
 * ex. RelativeLayout内でlayout_alignParentBottom="true"を指定するなど
 */
public class ViewSlider {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ViewSlider.class.getSimpleName();

	/**
	 * 垂直方向にアニメーション
	 */
	public static final int VERTICAL = 0;
	/**
	 * 水平方向にアニメーション
	 */
	public static final int HORIZONTAL = 1;

	@IntDef({
		VERTICAL,
		HORIZONTAL
	})
	@Retention(SOURCE)
	public @interface Orientation {}

	/**
	 * スライドイン/スライドアウト時のアニメーション時間のデフォルト[ミリ秒]
	 */
	public static final int DEFAULT_DURATION_RESIZE_MS = 300;

//--------------------------------------------------------------------------------
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

	private final int mDurationResizeMs;

	/**
	 * アニメーション時の最大幅
	 */
	private int mTargetMaxWidth;
	/**
	 * アニメーション時の最大高さ
	 */
	private int mTargetMaxHeight;
	/**
	 * アニメーション時の最小幅
	 */
	private int mTargetMinWidth;
	/**
	 * アニメーション時の最小高さ
	 */
	private int mTargetMinHeight;
	/**
	 * アニメーションの方向を指定
	 */
	@Orientation
	private int mOrientation;

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param viewId アニメーションさせるViewのid
	 * @throws IllegalArgumentException
	 */
	public ViewSlider(@NonNull final View parent, @IdRes final int viewId)
		throws IllegalArgumentException {

		this(parent, viewId, VERTICAL, DEFAULT_DURATION_RESIZE_MS);
	}

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param viewId アニメーションさせるViewのid
	 * @param orientation
	 * @throws IllegalArgumentException
	 */
	public ViewSlider(@NonNull final View parent, @IdRes final int viewId,
		@Orientation final int orientation) throws IllegalArgumentException {

		this(parent, viewId, orientation, DEFAULT_DURATION_RESIZE_MS);
	}

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param viewId アニメーションさせるViewのid
	 * @param orientation アニメーションの方向
	 * @param resizeDuration
	 * @throws IllegalArgumentException
	 */
	public ViewSlider(@NonNull final View parent, @IdRes final int viewId,
		@Orientation final int orientation, final int resizeDuration) throws IllegalArgumentException {

		this(parent, parent.findViewById(viewId), orientation, resizeDuration);
	}

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param target アニメーションさせるView
	 * @param orientation アニメーションの方向
	 * @param resizeDuration
	 * @throws IllegalArgumentException
	 */
	public ViewSlider(@NonNull final View parent, @NonNull final View target,
		@Orientation final int orientation, final int resizeDuration) throws IllegalArgumentException {

		mParent = parent;
		if (parent.getClass().getSimpleName().equals("ConstraintLayout")) {
			// XXX 親ViewがConstraintLayoutだと上手く動かない
			Log.w(TAG, "If parent is ConstraintLayout, ViewSlider will not work well.");
		}
		mTarget = target;
		if (mTarget == null) {
			throw new IllegalArgumentException("Target view not found");
		}
		mDurationResizeMs = resizeDuration > 0 ? resizeDuration : DEFAULT_DURATION_RESIZE_MS;
		mOrientation = orientation;
		mTargetMaxWidth = mTarget.getWidth();
		mTargetMaxHeight = mTarget.getHeight();
		mTargetMinWidth = mTargetMinHeight = 0;
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:size(%dx%d)", mTargetMaxWidth, mTargetMaxHeight));
		mParent.addOnLayoutChangeListener(mOnLayoutChangeListener);
		mTarget.addOnLayoutChangeListener(mOnLayoutChangeListener);
		// XXX ViewのvisibilityがGONEだとサイズが0になってしまうのでINVISIBLEに変更する
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

	/**
	 * 破棄時の処理
	 */
	public void release() {
		mTarget.clearAnimation();
		mTarget.removeOnLayoutChangeListener(mOnLayoutChangeListener);
		mParent.removeOnLayoutChangeListener(mOnLayoutChangeListener);
	}

	/**
	 * ターゲットViewを取得
	 * @return
	 */
	@NonNull
	public View getTargetView() {
		return mTarget;
	}

	/**
	 * アニメーションの方向を指定
	 * @return
	 */
	@Orientation
	public int getOrientation() {
		return mOrientation;
	}

	/**
	 * 現在設定されているアニメーションの方向を取得
	 * @param orientation
	 */
	public void setOrientation(@Orientation final int orientation) {
		mOrientation = orientation;
	}

	/**
	 * ターゲットViewのアニメーション時の最大サイズ(幅)を指定
	 * 設定しない場合にはターゲットViewのサイズが最初に決定されたときの大きさになる
	 * @param width
	 * @throws IllegalArgumentException
	 */
	public void setTargetWidth(@IntRange(from = 0) final int width)
		throws IllegalArgumentException {

		if (width >= 0) {
			mTargetMaxWidth = width;
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * ターゲットViewのアニメーション時の最大サイズ(高さ)を指定
	 * 設定しない場合にはターゲットViewのサイズが最初に決定されたときの大きさになる
	 * @param height
	 * @throws IllegalArgumentException
	 */
	public void setTargetHeight(@IntRange(from = 0) final int height)
		throws IllegalArgumentException {

		if (height >= 0) {
			mTargetMaxHeight = height;
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * アニメーション時の最大サイズを現在のターゲットViewのサイズにセットする
	 */
	public void resetTargetSize() {
		setTargetSize(mTarget.getWidth(), mTarget.getHeight());
	}

	/**
	 * アニメーション時の最大サイズをセットする
	 * @param width
	 * @param height
	 * @throws IllegalArgumentException
	 */
	public void setTargetSize(
		@IntRange(from = 0) final int width, @IntRange(from = 0) final int height)
			throws IllegalArgumentException {

		if ((width >= 0) && (height >= 0)) {
			mTargetMaxWidth = width;
			mTargetMaxHeight = height;
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * アニメーション時の最小サイズをセットする
	 * @param width
	 * @param height
	 * @throws IllegalArgumentException
	 */
	public void setMinSize(
		@IntRange(from = 0) final int width, @IntRange(from = 0) final int height)
			throws IllegalArgumentException {

		if ((width >= 0) && (height >= 0)) {
			mTargetMinWidth = width;
			mTargetMinHeight = height;
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * ターゲットViewのgetVisibilityの値を返す
	 * これがView#VISIBLEを返してもViewの幅または高さの少なくとも一方が0のときは見えないので
	 * 見えているかどうかのチェックには#isVisibleを使うほうが良い
	 * @return
	 */
	@ViewUtils.Visibility
	public int getVisibility() {
		return mTarget.getVisibility();
	}

	/**
	 * ターゲットViewが見えているかどうか
	 * Visibility==VISIBLEでViewのサイズが0でないときのみtrue
	 * @return
	 */
	public boolean isVisible() {
		return (mTarget.getVisibility() == View.VISIBLE)
			&& (mTarget.getWidth() > 0) && (mTarget.getHeight() > 0);
	}

	/**
	 * ターゲットViewをスライドイン
	 * @param autoHideDurationMs
	 */
	public void show(final long autoHideDurationMs) {
		mTarget.post(new Runnable() {
			@Override
			public void run() {
				if (DEBUG) Log.i(TAG, String.format(Locale.US,
					"show:size(%d,%d)",
						mTarget.getWidth(), mTarget.getHeight()));
				mTarget.clearAnimation();
				final ResizeAnimation expandAnimation;
				if (mOrientation == VERTICAL) {
					expandAnimation = new ResizeAnimation(mTarget,
						mTargetMaxWidth, mTargetMinHeight,
						mTargetMaxWidth, mTargetMaxHeight);
				} else {
					expandAnimation = new ResizeAnimation(mTarget,
						mTargetMinWidth, mTargetMaxHeight,
						mTargetMaxWidth, mTargetMaxHeight);
				}
				expandAnimation.setDuration(mDurationResizeMs);
				expandAnimation.setAnimationListener(mAnimationListener);
				mTarget.setTag(R.id.visibility, 1);
				mTarget.setTag(R.id.auto_hide_duration, autoHideDurationMs);
				mTarget.postDelayed(new Runnable() {
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
	 */
	public void hide() {
		hide(mDurationResizeMs);
	}

	/**
	 * ターゲットViewをスライドアウト
	 * @param durationMs 0以下ならアニメーションせずにすぐにINVISIBLEにする
	 */
	public void hide(final long durationMs) {
		mTarget.post(new Runnable() {
			@Override
			public void run() {
				if (mTarget.getVisibility() == View.VISIBLE) {
					if (DEBUG) Log.v(TAG,
						String.format(Locale.US, "hide:size(%d,%d)",
						mTarget.getWidth(), mTarget.getHeight()));
					mTarget.clearAnimation();
					if (durationMs > 0) {
						final ResizeAnimation collapseAnimation;
						if (mOrientation == VERTICAL) {
							collapseAnimation = new ResizeAnimation(mTarget,
								mTargetMaxWidth, mTarget.getHeight(),
								mTargetMaxWidth, mTargetMinHeight);
						} else {
							collapseAnimation = new ResizeAnimation(mTarget,
								mTarget.getWidth(), mTargetMaxHeight,
								mTargetMinWidth, mTargetMaxHeight);
						}
						collapseAnimation.setDuration(durationMs);
						collapseAnimation.setAnimationListener(mAnimationListener);
						mTarget.setTag(R.id.visibility, 0);
						mTarget.startAnimation(collapseAnimation);
					} else {
						if (mOrientation == VERTICAL) {
							ViewUtils.requestResize(mTarget, mTargetMaxWidth, mTargetMinHeight);
						} else {
							ViewUtils.requestResize(mTarget, mTargetMinWidth, mTargetMaxHeight);
						}
						mTarget.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	private final View.OnLayoutChangeListener mOnLayoutChangeListener
		= new View.OnLayoutChangeListener() {
		@Override
		public void onLayoutChange(final View v,
			final int left, final int top, final int right, final int bottom,
			final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {

			if (DEBUG) Log.v(TAG, String.format(
				"onLayoutChange:%s(%d,%d - %d,%d) → (%d,%d - %d,%d)",
					v.getClass().getSimpleName(),
					oldLeft, oldTop, oldRight, oldBottom,
					left, top, right, bottom));
			if (v == mTarget) {
				if (mTargetMaxWidth <= 0) {
					mTargetMaxWidth = v.getWidth();
				}
				if (mTargetMaxHeight <= 0) {
					mTargetMaxHeight = v.getHeight();
				}
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
							hide(mDurationResizeMs);
						}
					}, duration);
				}
			} else {
				mTarget.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		public void onAnimationRepeat(final Animation animation) {
			if (DEBUG) Log.v(TAG, "onAnimationRepeat:");
		}
	};
}
