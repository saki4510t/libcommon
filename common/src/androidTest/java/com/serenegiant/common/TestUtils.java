package com.serenegiant.common;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

public class TestUtils {
	private static final String TAG = BitmapHelperTest.class.getSimpleName();

	private TestUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean bitMapEquals(@NonNull final Bitmap a, @NonNull final Bitmap b) {
		boolean result = false;
		if ((a.getWidth() == b.getWidth())
			&& (a.getHeight() == b.getHeight()
			&& (a.getConfig() == b.getConfig()))) {
			result = true;
LOOP:		for (int y = a.getHeight() - 1; y >= 0; y--) {
				for (int x = a.getWidth() - 1; x >= 0; x--) {
					if (!a.getColor(x, y).equals(b.getColor(x, y))) {
						Log.w(TAG, String.format("ピクセルが違う@(%dx%d),a=0x%08x,b=0x%08x",
							x, y, a.getColor(x, y).toArgb(), b.getColor(x, y).toArgb()));
						result = false;
						break LOOP;
					}
				}
			}
		} else {
			Log.w(TAG, String.format("ピクセルが違うa(%dx%d),b=(%dx%d))",
				a.getWidth(), a.getHeight(), b.getWidth(), b.getHeight()));
		}
		return result;
	}

}
