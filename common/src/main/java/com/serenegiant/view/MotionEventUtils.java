package com.serenegiant.view;

import android.os.Build;
import android.view.MotionEvent;

import com.serenegiant.utils.BitsHelper;

import java.util.Locale;

import androidx.annotation.NonNull;

/**
 * MotionEvent関係のヘルパーメソッドクラス
 */
public class MotionEventUtils {
	private MotionEventUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateにする
	}

	public static final int BUTTON_PRIMARY
		= MotionEvent.BUTTON_PRIMARY
		| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? MotionEvent.BUTTON_STYLUS_PRIMARY : 0);

	public static final int BUTTON_SECONDARY
		= MotionEvent.BUTTON_SECONDARY
		| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? MotionEvent.BUTTON_STYLUS_SECONDARY : 0);

	/**
	 * 指定したMotionEventのactionをACTION_XXX形式の文字列に変換する
	 * MotionEvent#actionToStringがAPI>=19なので後方互換性のためにバックポート
	 * @param event
	 * @return
	 */
	public static String getActionString(@NonNull final MotionEvent event) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			return MotionEvent.actionToString(event.getActionMasked());
		} else {
			final int action = event.getActionMasked();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				return "ACTION_DOWN";
			case MotionEvent.ACTION_UP:
				return "ACTION_UP";
			case MotionEvent.ACTION_CANCEL:
				return "ACTION_CANCEL";
			case MotionEvent.ACTION_OUTSIDE:
				return "ACTION_OUTSIDE";
			case MotionEvent.ACTION_MOVE:
				return "ACTION_MOVE";
			case MotionEvent.ACTION_HOVER_MOVE:
				return "ACTION_HOVER_MOVE";
			case MotionEvent.ACTION_SCROLL:
				return "ACTION_SCROLL";
			case MotionEvent.ACTION_HOVER_ENTER:
				return "ACTION_HOVER_ENTER";
			case MotionEvent.ACTION_HOVER_EXIT:
				return "ACTION_HOVER_EXIT";
			case MotionEvent.ACTION_BUTTON_PRESS:
				return "ACTION_BUTTON_PRESS";
			case MotionEvent.ACTION_BUTTON_RELEASE:
				return "ACTION_BUTTON_RELEASE";
			}

			final int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
				>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_DOWN:
				return "ACTION_POINTER_DOWN(" + index + ")";
			case MotionEvent.ACTION_POINTER_UP:
				return "ACTION_POINTER_UP(" + index + ")";
			default:
				return Integer.toString(action);
			}
		}
	}

	// Symbolic names of all button states in bit order from least significant
	// to most significant.
	private static final String[] BUTTON_SYMBOLIC_NAMES = new String[] {
		"BUTTON_PRIMARY",
		"BUTTON_SECONDARY",
		"BUTTON_TERTIARY",
		"BUTTON_BACK",
		"BUTTON_FORWARD",
		"BUTTON_STYLUS_PRIMARY",
		"BUTTON_STYLUS_SECONDARY",
		"0x00000080",
		"0x00000100",
		"0x00000200",
		"0x00000400",
		"0x00000800",
		"0x00001000",
		"0x00002000",
		"0x00004000",
		"0x00008000",
		"0x00010000",
		"0x00020000",
		"0x00040000",
		"0x00080000",
		"0x00100000",
		"0x00200000",
		"0x00400000",
		"0x00800000",
		"0x01000000",
		"0x02000000",
		"0x04000000",
		"0x08000000",
		"0x10000000",
		"0x20000000",
		"0x40000000",
		"0x80000000",
	};

	/**
	 * ボタンの押し下げ状態を文字列として返す
	 * @param event
	 * @return
	 */
	public static String getButtonStateString(@NonNull final MotionEvent event) {
		int buttonState = event.getButtonState();
		if (buttonState == 0) {
			return "[]";
		}
		final StringBuilder result = new StringBuilder("[");
		int i = 0;
		while (buttonState != 0) {
			final boolean isSet = (buttonState & 1) != 0;
			buttonState >>>= 1; // unsigned shift!
			if (isSet) {
				final String name = BUTTON_SYMBOLIC_NAMES[i];
				if (result.length() == 1) {
					result.append(name);
					if (buttonState == 0) {
						break;
					}
				} else {
					result.append('|');
					result.append(name);
				}
			}
			i += 1;
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * 指定されたボタンが全て押されているかどうかを取得
	 * @param event
	 * @param buttons
	 * @return
	 */
	public static boolean isPressed(@NonNull final MotionEvent event, final int buttons) {
		return isPressed(event.getButtonState(), buttons);
	}

	/**
	 * 指定されたボタンが全て押されているかどうかを取得
	 * @param buttonState
	 * @param buttons
	 * @return
	 */
	public static boolean isPressed(final int buttonState, final int buttons) {
		return (buttonState & buttons) == buttons;
	}

	/**
	 * 指定されたボタンのいずれかが押されているかどうかを取得
	 * @param event
	 * @param buttons
	 * @return
	 */
	public static boolean isPressedAny(@NonNull final MotionEvent event, final int buttons) {
		return isPressedAny(event.getButtonState(), buttons);
	}

	/**
	 * 指定されたボタンのいずれかが押されているかどうかを取得
	 * @param buttonState
	 * @param buttons
	 * @return
	 */
	public static boolean isPressedAny(final int buttonState, final int buttons) {
		return (buttonState & buttons) != 0;
	}

	/**
	 * 押されているボタンの数を返す
	 * @param event
	 * @return
	 */
	public static int getNumPressed(@NonNull final MotionEvent event) {
		return getNumPressed(event.getButtonState());
	}

	/**
	 * 押されているボタンの数を返す
	 * @param buttonState
	 * @return
	 */
	public static int getNumPressed(final int buttonState) {
		return BitsHelper.countBits(buttonState);
	}

	/**
	 * 指定したMotionEventの入力ソースが指定したクラスかどうかをチェック
	 * @param event
	 * @param sourceClass
	 * @return
	 */
	public static boolean isFromSource(@NonNull final MotionEvent event,
		final int sourceClass) {

		return (event.getSource() & sourceClass) == sourceClass;
	}

	/**
	 * MotionEventのデバッグログメッセージ用文字列を取得
	 * @param event
	 * @return
	 */
	public static String debugMotionEventString(@NonNull final MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();
		return String.format(Locale.US, "%s(%f,%f):,down=%d,event=%d,src=0x%08x",
			MotionEventUtils.getActionString(event),
			x, y,
			event.getDownTime(), event.getEventTime(), event.getSource());
	}
}
