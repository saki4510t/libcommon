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
	public static float dpToPixels(@NonNull final DisplayMetrics metrics, final float dp) {
		return dp * metrics.density;
	}

	/**
	 * dpをピクセルに変換
	 * @param metrics
	 * @param dp
	 * @return
	 */
	public static double dpToPixels(@NonNull final DisplayMetrics metrics, final double dp) {
		return dp * metrics.density;
	}

	/**
	 * dpをピクセルに変換
	 * @param metrics
	 * @param dp
	 * @return
	 */
	public static int dpToPixelsInt(@NonNull final DisplayMetrics metrics, final float dp) {
		return Math.round((dpToPixels(metrics, dp)));
	}

	/**
	 * dpをピクセルに変換
	 * @param metrics
	 * @param dp
	 * @return
	 */
	public static int dpToPixelsInt(@NonNull final DisplayMetrics metrics, final double dp) {
		return (int)Math.round((dpToPixels(metrics, dp)));
	}

	/**
	 * dpをピクセルに変換
	 * @param context
	 * @param dp
	 * @return
	 */
	public static float dpToPixels(@NonNull final Context context, final float dp) {
		return dpToPixels(context.getResources().getDisplayMetrics(), dp);
	}

	/**
	 * dpをピクセルに変換
	 * @param context
	 * @param dp
	 * @return
	 */
	public static double dpToPixels(@NonNull final Context context, final double dp) {
		return dpToPixels(context.getResources().getDisplayMetrics(), dp);
	}

	/**
	 * dpをピクセル(int)に変換
	 * @param context
	 * @param dp
	 * @return
	 */
	public static int dpToPixelsInt(@NonNull final Context context, final float dp) {
		return dpToPixelsInt(context.getResources().getDisplayMetrics(), dp);
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static float pixelsToDp(@NonNull final DisplayMetrics metrics, final float pixels) {
		return pixels / metrics.density;
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static double pixelsToDp(@NonNull final DisplayMetrics metrics, final double pixels) {
		return pixels / metrics.density;
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static float pixelsToDp(@NonNull final Context context, final float pixels) {
		return pixelsToDp(context.getResources().getDisplayMetrics(), pixels);
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static double pixelsToDp(@NonNull final Context context, final double pixels) {
		return pixelsToDp(context.getResources().getDisplayMetrics(), pixels);
	}

//--------------------------------------------------------------------------------
	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static float spToPixels(@NonNull final DisplayMetrics metrics, final float sp) {
		return sp * metrics.scaledDensity;
	}

	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static double spToPixels(@NonNull final DisplayMetrics metrics, final double sp) {
		return sp * metrics.scaledDensity;
	}

	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final DisplayMetrics metrics, final float sp) {
		return Math.round(spToPixels(metrics, sp));
	}

	/**
	 * spをピクセルに変換
	 * @param metrics
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final DisplayMetrics metrics, final double sp) {
		return (int)Math.round(spToPixels(metrics, sp));
	}

	/**
	 * spをピクセルに変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static float spToPixels(@NonNull final Context context, final float sp) {
		return spToPixels(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * spをピクセルに変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static double spToPixels(@NonNull final Context context, final double sp) {
		return spToPixels(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * spをピクセル(int)に変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final Context context, final float sp) {
		return spToPixelsInt(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * spをピクセル(int)に変換
	 * @param context
	 * @param sp
	 * @return
	 */
	public static int spToPixelsInt(@NonNull final Context context, final double sp) {
		return spToPixelsInt(context.getResources().getDisplayMetrics(), sp);
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static float pixelsToSp(@NonNull final DisplayMetrics metrics, final float pixels) {
		return pixels / metrics.scaledDensity;
	}

	/**
	 * ピクセルをdpに変換
	 * @param metrics
	 * @param pixels
	 * @return
	 */
	public static double pixelsToSp(@NonNull final DisplayMetrics metrics, final double pixels) {
		return pixels / metrics.scaledDensity;
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static float pixelsToSp(@NonNull final Context context, final float pixels) {
		return pixelsToSp(context.getResources().getDisplayMetrics(), pixels);
	}

	/**
	 * ピクセルをdpに変換
	 * @param context
	 * @param pixels
	 * @return
	 */
	public static double pixelsToSp(@NonNull final Context context, final double pixels) {
		return pixelsToSp(context.getResources().getDisplayMetrics(), pixels);
	}
}
