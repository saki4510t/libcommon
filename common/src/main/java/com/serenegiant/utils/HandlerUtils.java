package com.serenegiant.utils;

import android.os.Handler;

import androidx.annotation.Nullable;

/**
 * Handler用のユーティリティクラス
 */
public class HandlerUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = HandlerUtils.class.getSimpleName();

	private HandlerUtils() {
		// インスタンス化を防止するためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * 指定したHandlerが終了している(メッセージ送信・Runnable実行等を受け付けない)かどうかを取得
	 * このメソッドを実行するとsendEmptyMessageでwhat=0を送信する可能性があるので注意
	 * @param handler
	 * @return
	 */
	public static boolean isTerminated(@Nullable final Handler handler) {
		// Handler#getLooperとLooper#getThreadはNonNullなのでHandlerがnullでなければthread変数もnullじゃない
		final Thread thread = handler != null ? handler.getLooper().getThread() : null;
		// XXX sendEmptyMessageでwhat=0を送って返り値がfalseならHandler/Looperが終了しているとみなす
		return (handler == null) || (thread == null)
			|| !thread.isAlive() || !handler.sendEmptyMessage(0);
	}
}
