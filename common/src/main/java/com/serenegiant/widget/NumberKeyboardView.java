package com.serenegiant.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/**
 * 数字入力用キーボードViewクラス
 */
public class NumberKeyboardView extends KeyboardView {
	private static final boolean DEBUG = true;    // set false on production
	private static final String TAG = NumberKeyboardView.class.getSimpleName();

	/**
	 *
	 */
	public interface OnNumberChangedListener {
		public void onNumberChanged(final int keyCode, final String inserted);
	}

	/**
	 * コンストラクタ
	 * @param context
	 */
	public NumberKeyboardView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public NumberKeyboardView(final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public NumberKeyboardView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

}
