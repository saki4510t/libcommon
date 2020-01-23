package com.serenegiant.system;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

import com.serenegiant.utils.BrightnessHelper;

import androidx.annotation.NonNull;

public class ScreenUtils {

	private ScreenUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 画面の自動消灯のON/OFF
	 * @param activity
	 * @param onoff
	 */
	public static void setKeepScreenOn(@NonNull final Activity activity, final boolean onoff) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Window window = activity.getWindow();
				if (onoff) {
					window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				} else {
					window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					BrightnessHelper.setBrightness(activity,
						WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);	// システム設定に戻す
				}
			}
		});
	}
}
