package com.serenegiant.libcommon;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
	public static boolean bitmapEquals(@NonNull final Bitmap a, @NonNull final Bitmap b) {
		return bitmapEquals(a, b, false);
	}

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @param dumpOnError
	 * @return
	 */
	public static boolean bitmapEquals(@NonNull final Bitmap a, @NonNull final Bitmap b, final boolean dumpOnError) {
		boolean result = false;
		final int width = a.getWidth();
		final int height = a.getHeight();
		if ((width == b.getWidth())
			&& (height == b.getHeight()
			&& (a.getConfig() == b.getConfig()))) {
			result = true;
LOOP:		for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (!a.getColor(x, y).equals(b.getColor(x, y))) {
						Log.w(TAG, String.format("ピクセルが違う@(%d,%d)sz(%dx%d),a=0x%08x,b=0x%08x",
							x, y, width, height,
							a.getColor(x, y).toArgb(),
							b.getColor(x, y).toArgb()));
						result = false;
						break LOOP;
					}
				}
			}
			if (!result && dumpOnError) {
				dump(TAG, "a=", a);
				dump(TAG, "b=", b);
			}
		} else {
			Log.w(TAG, String.format("ピクセルが違うa(%dx%d),b=(%dx%d))",
				width, height, b.getWidth(), b.getHeight()));
		}
		return result;
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param bitmap
	 */
	public static void dump(@NonNull final Bitmap bitmap) {
		dump(TAG, null, bitmap);
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param tag
	 * @param bitmap
	 */
	public static void dump(@Nullable final String tag, @Nullable final String prefix, @NonNull final Bitmap bitmap) {
		final StringBuilder sb = new StringBuilder();
		final int w = bitmap.getWidth();
		final int h = bitmap.getHeight();
		final String t = (TextUtils.isEmpty(tag) ? TAG : tag);
		final String header = (TextUtils.isEmpty(prefix) ? "dump:" : prefix);
		Log.i(t, String.format("%s(%dx%d)", header, w, h));
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				final int cl = bitmap.getColor(x, y).toArgb();
				if (cl != 0) {
					sb.append(String.format("%08x", cl));
				}
			}
		}
		Log.i(t, header + sb);
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
