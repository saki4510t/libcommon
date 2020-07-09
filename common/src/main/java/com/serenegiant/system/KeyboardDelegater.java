package com.serenegiant.system;

import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.serenegiant.widget.Keyboard;
import com.serenegiant.widget.KeyboardView;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

public abstract class KeyboardDelegater
	implements KeyboardView.OnKeyboardActionListener {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = KeyboardDelegater.class.getSimpleName();

	@NonNull
	private final EditText mEditText;
	@NonNull
	private final KeyboardView mKeyboardView;
	@XmlRes
	private final int mKeyboardLayoutRes;
	@Nullable
	private Keyboard mKeyboard;

	public KeyboardDelegater(
		@NonNull final EditText editText,
		@NonNull final KeyboardView keyboardView,
		@XmlRes final int keyboardLayoutRes) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mEditText = editText;
		mKeyboardView = keyboardView;
		mKeyboardLayoutRes = keyboardLayoutRes;
	}

	public void showKeyboard() {
		if (DEBUG) Log.v(TAG, "showKeyboard:");
		hideSystemSoftKeyboard();
		if (mKeyboard == null) {
			mKeyboard = new Keyboard(mEditText.getContext(), mKeyboardLayoutRes);
			mKeyboardView.setKeyboard(mKeyboard);
		}
		mKeyboardView.setEnabled(true);
		mKeyboardView.setPreviewEnabled(false);
		mKeyboardView.setOnKeyboardActionListener(this);
		final int visibility = mKeyboardView.getVisibility();
		if (visibility == View.GONE || visibility == View.INVISIBLE) {
			mKeyboardView.setVisibility(View.VISIBLE);
		}
	}

	public void hideKeyboard() {
		if (DEBUG) Log.v(TAG, "hideKeyboard:");
		int visibility = mKeyboardView.getVisibility();
		if (visibility == View.VISIBLE) {
			mKeyboardView.setVisibility(View.GONE);
		}
	}

	public void hideSystemSoftKeyboard() {

		if (DEBUG) Log.v(TAG, "hideSystemSoftKeyboard:");
		try {
			final Class<EditText> cls = EditText.class;
			final Method setShowSoftInputOnFocus
				= cls.getMethod("setShowSoftInputOnFocus", boolean.class);
			setShowSoftInputOnFocus.setAccessible(true);
			setShowSoftInputOnFocus.invoke(mEditText, false);
		} catch (final SecurityException e) {
			if (DEBUG) Log.w(TAG, e);
		} catch (final NoSuchMethodException e) {
			if (DEBUG) Log.w(TAG, e);
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		final InputMethodManager imm
			= ContextUtils.requireSystemService(mEditText.getContext(), InputMethodManager.class);
		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	}

//--------------------------------------------------------------------------------
	/**
	 * KEYCODE_DONEが押されたときの処理
	 */
	protected abstract void onOkClick();

	/**
	 * KEYCODE_CANCELが押されたときの処理
	 */
	protected abstract void onCancelClick();

//--------------------------------------------------------------------------------
	@Override
	public void onPress(final int primaryCode) {
		if (DEBUG) Log.v(TAG, "onPress:primaryCode=" + primaryCode);
	}

	@Override
	public void onRelease(final int primaryCode) {
		if (DEBUG) Log.v(TAG, "onRelease:primaryCode=" + primaryCode);
	}

	@Override
	public void onKey(final int primaryCode, final int[] keyCodes) {
		if (DEBUG) Log.v(TAG, "onKey:primaryCode=" + primaryCode);
		Editable editable = mEditText.getText();
		int start = mEditText.getSelectionStart();
		if (primaryCode == Keyboard.KEYCODE_DELETE) {
			// 削除(Delete)キー
			if (editable != null && editable.length() > 0) {
				if (start > 0) {
					editable.delete(start - 1, start);
				}
			}
		} else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
			// キャンセル(ESC)キー
			hideKeyboard();
			onCancelClick();
		} else if (primaryCode == Keyboard.KEYCODE_DONE) {
			// 完了(ENTER)キー
			hideKeyboard();
			onOkClick();
		} else {
			// それ以外のキー入力
			editable.insert(start, Character.toString((char) primaryCode));
		}
	}

	@Override
	public void onText(final CharSequence text) {
		if (DEBUG) Log.v(TAG, "onText:" + text);
	}

	@Override
	public void swipeLeft() {
		if (DEBUG) Log.v(TAG, "swipeLeft:");
	}

	@Override
	public void swipeRight() {
		if (DEBUG) Log.v(TAG, "swipeRight:");
	}

	@Override
	public void swipeDown() {
		if (DEBUG) Log.v(TAG, "swipeDown:");
	}

	@Override
	public void swipeUp() {
		if (DEBUG) Log.v(TAG, "swipeUp:");
	}

}
