package com.serenegiant.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;

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
			final int w = a.getWidth();
			final int h = a.getHeight();
			result = true;
LOOP:		for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
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

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param bitmap
	 */
	public static void dump(@NonNull final Bitmap bitmap) {
		final StringBuilder sb = new StringBuilder();
		final int w = bitmap.getWidth();
		final int h = bitmap.getHeight();
		Log.i(TAG, String.format("dump:(%dx%d)", w, h));
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				final int cl = bitmap.getColor(x, y).toArgb();
				if (cl != 0) {
					sb.append(String.format("%08x", cl));
				}
			}
		}
		Log.i(TAG, "dump:" + sb);
	}

	/**
	 * ビットマップを上下反転
	 * @param bitmap
	 * @return
	 */
	public static Bitmap flipVertical(@NonNull final Bitmap bitmap) {
		return BitmapHelper.applyMirror(bitmap, IMirror.MIRROR_VERTICAL);
	}

	/**
	 * 非同期で指定したSurfaceへCanvasを使って指定した枚数指定したBitmapを書き込む
	 * @param bitmap
	 * @param surface
	 * @param num_images
	 */
	public static void inputImagesAsync(@NonNull final Bitmap bitmap, @NonNull final Surface surface, final int num_images) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final Rect inOutDirty = new Rect();
				for (int i = 0; i < num_images; i++) {
					if (surface.isValid()) {
						final Canvas canvas = surface.lockCanvas(inOutDirty);
						try {
							if (canvas != null) {
								try {
									canvas.drawBitmap(bitmap, 0, 0, null);
									Thread.sleep(30);
								} finally {
									surface.unlockCanvasAndPost(canvas);
								}
							}
						} catch (Exception e) {
							break;
						}
					} else {
						break;
					}
				}
			}
		}).start();
	}
}
