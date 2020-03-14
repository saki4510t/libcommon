package com.serenegiant.system;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

/**
 * DisplayMetrics関係のヘルパーメソッド
 */
public class DisplayMetricsUtils {
	private DisplayMetricsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * dpをピクセルに変換
	 * @param metrics
	 * @param dp
	 * @return
	 */
	public static float dpToPixels(@NonNull final DisplayMetrics metrics, final int dp) {
		return dp * metrics.density;
	}

	/**
	 * dpをピクセルに変換
	 * @param metrics
	 * @param dp
	 * @return
	 */
	public static int dpToPixelsInt(@NonNull final DisplayMetrics metrics, final int dp) {
		return Math.round((dpToPixels(metrics, dp)));
	}

	/**
	 * dpをピクセルに変換
	 * @param context
	 * @param dp
	 * @return
	 */
	public static float dpToPixels(@NonNull final Context context, final int dp) {
		return dpToPixels(context.getResources().getDisplayMetrics(), dp);
	}

	/**
	 * dpをピクセル(int)に変換
	 * @param context
	 * @param dp
	 * @return
	 */
	public static int dpToPixelsInt(@NonNull final Context context, final int dp) {
		return dpToPixelsInt(context.getResources().getDisplayMetrics(), dp);
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static int pixelsToDp(@NonNull final DisplayMetrics metrics, final float pixels) {
		return Math.round(pixels / metrics.density + 0.5f);
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static int pixelsToDp(@NonNull final Context context, final float pixels) {
		return pixelsToDp(context.getResources().getDisplayMetrics(), pixels);
	}

//--------------------------------------------------------------------------------
	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static float spToPixels(@NonNull final DisplayMetrics metrics, final int sp) {
		return sp * metrics.scaledDensity;
	}

	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final DisplayMetrics metrics, final int sp) {
		return Math.round(spToPixels(metrics, sp));
	}

	/**
	 * spをピクセルに変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static float spToPixels(@NonNull final Context context, final int sp) {
		return spToPixels(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * spをピクセル(int)に変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final Context context, final int sp) {
		return spToPixelsInt(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static int pixelsToSp(@NonNull final DisplayMetrics metrics, final float pixels) {
		return Math.round(pixels / metrics.scaledDensity + 0.5f);
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static int pixelsToSp(@NonNull final Context context, final float pixels) {
		return pixelsToSp(context.getResources().getDisplayMetrics(), pixels);
	}

}
