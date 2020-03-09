package com.serenegiant.view;

import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * 親ViewにTouchDelegateがセットされている場合に保存しておいて
 * 自分の処理のあとに元々のTouchDelegateを呼び出す処理を追加したTouchDelegate
 */
public class ChainedTouchDelegate extends TouchDelegate {

	/**
	 * コンストラクタ呼び出し時に親ViewにセットされていたTouchDelegate
	 */
	@Nullable
	private final TouchDelegate mParentTouchDelegate;

	/**
	 * コンストラクタ
	 * @param parent 親View
	 * @param target TouchDelegateの対象となるView
	 * @param bounds 処理するタッチ領域
	 */
	public ChainedTouchDelegate(@NonNull final View parent,
		final @NonNull View target, @NonNull  final Rect bounds) {
		super(bounds, target);

		mParentTouchDelegate = parent.getTouchDelegate();
		parent.setTouchDelegate(this);
	}

	@Override
	public boolean onTouchEvent(@NonNull final MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		if (!result && (mParentTouchDelegate != null)) {
			result = mParentTouchDelegate.onTouchEvent(event);
		}
		return result;
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	@Override
	public boolean onTouchExplorationHoverEvent(@NonNull final MotionEvent event) {
		boolean result = super.onTouchExplorationHoverEvent(event);
		if (!result && (mParentTouchDelegate != null)) {
			result = mParentTouchDelegate.onTouchExplorationHoverEvent(event);
		}
		return result;
	}
}
