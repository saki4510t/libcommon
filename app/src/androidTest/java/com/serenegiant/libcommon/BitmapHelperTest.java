package com.serenegiant.libcommon;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

import static com.serenegiant.libcommon.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class BitmapHelperTest {
	private static final String TAG = BitmapHelperTest.class.getSimpleName();

	private static final int WIDTH = 200;
	private static final int HEIGHT = 200;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	/**
	 * BitmapHelper.makeCheckBitmapが生成したビットマップの比較テスト
	 */
	@Test
	public void bitmapEqualsTest() {
		// b0とb1は同じ内容のビットマップ, b2は異なる内容のビットマップ
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		assertEquals(WIDTH, b0.getWidth());
		assertEquals(HEIGHT, b0.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b0.getConfig());

		final Bitmap b1 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		assertEquals(WIDTH, b1.getWidth());
		assertEquals(HEIGHT, b1.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b1.getConfig());

		final Bitmap b2 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 19,
			Bitmap.Config.ARGB_8888);
		assertEquals(WIDTH, b2.getWidth());
		assertEquals(HEIGHT, b2.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b2.getConfig());

		assertTrue(bitmapEquals(b0, b1));
		assertFalse(bitmapEquals(b0, b2));
	}

	@Test
	public void makeFilledBitmapTest() {
		final int color = 0x7f123456;
		final Bitmap b = BitmapHelper.makeFilledBitmap(
			WIDTH, HEIGHT, color,
			Bitmap.Config.ARGB_8888);

		assertEquals(WIDTH, b.getWidth());
		assertEquals(HEIGHT, b.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b.getConfig());
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				assertEquals(color, b.getColor(x, y).toArgb());
			}
		}
	}

	@Test
	public void makeGradientBitmapTest() {
		final int color1 = 0xff000000;
		final int color2 = 0xffffffff;
		final Bitmap b = BitmapHelper.makeGradientBitmap(
			WIDTH, HEIGHT, color1, color2,
			Bitmap.Config.ARGB_8888);

		assertEquals(WIDTH, b.getWidth());
		assertEquals(HEIGHT, b.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b.getConfig());
		// グラデーションだと指定した色そのものにならなくてうまくチェックできない
//		assertEquals(color1, b.getColor(0, 0).toArgb());
//		assertEquals(color2, b.getColor(WIDTH, HEIGHT).toArgb());
	}

	/**
	 * BitmapHelper#copyBitmapのテスト
	 */
	@Test
	public void copyTest() {
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		assertEquals(WIDTH, b0.getWidth());
		assertEquals(HEIGHT, b0.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, b0.getConfig());

		// copyBitmapのコピー先をnullとした場合は内部で自動生成
		final Bitmap dst = BitmapHelper.copyBitmap(b0, null);
		assertEquals(WIDTH, dst.getWidth());
		assertEquals(HEIGHT, dst.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, dst.getConfig());
		assertTrue(bitmapEquals(b0, dst));

		// あらかじめ割り当てたビットマップへコピーする場合
		final Bitmap dst1 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		BitmapHelper.copyBitmap(b0, dst1);
		assertEquals(WIDTH, dst1.getWidth());
		assertEquals(HEIGHT, dst1.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, dst1.getConfig());
		assertTrue(bitmapEquals(b0, dst1));
	}

	/**
	 * BitmapHelper#invertAlphaのテスト
	 */
	@Test
	public void invertAlphaTest() {
		final Bitmap b0 = BitmapHelper.genMaskImage(
			0, WIDTH, HEIGHT,60,
			Color.RED,0, 100);
		final Bitmap inverted = BitmapHelper.invertAlpha(b0);
		assertEquals(WIDTH, inverted.getWidth());
		assertEquals(HEIGHT, inverted.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, inverted.getConfig());
		// 2回アルファ値を反転させると元と一致するはず
		assertTrue(bitmapEquals(b0, BitmapHelper.invertAlpha(inverted)));
	}

	/**
	 * BitmapHelper#applyMirrorのテスト
	 */
	@Test
	public void applyMirrorTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		assertEquals(WIDTH, original.getWidth());
		assertEquals(HEIGHT, original.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, original.getConfig());

		final Bitmap normal = BitmapHelper.applyMirror(original, IMirror.MIRROR_NORMAL);
		assertTrue(bitmapEquals(original, normal));
		assertEquals(WIDTH, normal.getWidth());
		assertEquals(HEIGHT, normal.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, normal.getConfig());

		// 左右反転
		final Bitmap flipHorizontal = BitmapHelper.applyMirror(original, IMirror.MIRROR_HORIZONTAL);
		assertFalse(bitmapEquals(original, flipHorizontal));
		// 2回左右反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipHorizontal, IMirror.MIRROR_HORIZONTAL)));
		assertEquals(WIDTH, flipHorizontal.getWidth());
		assertEquals(HEIGHT, flipHorizontal.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, flipHorizontal.getConfig());

		// 上下反転
		final Bitmap flipVertical = BitmapHelper.applyMirror(original, IMirror.MIRROR_VERTICAL);
		assertFalse(bitmapEquals(original, flipVertical));
		// 2回上下反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipVertical, IMirror.MIRROR_VERTICAL)));
		assertEquals(WIDTH, flipVertical.getWidth());
		assertEquals(HEIGHT, flipVertical.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, flipVertical.getConfig());

		// 上下左右反転
		final Bitmap flipBoth = BitmapHelper.applyMirror(original, IMirror.MIRROR_BOTH);
		assertFalse(bitmapEquals(original, flipBoth));
		// 2回上下反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipBoth, IMirror.MIRROR_BOTH)));
		assertEquals(WIDTH, flipBoth.getWidth());
		assertEquals(HEIGHT, flipBoth.getHeight());
		assertEquals(Bitmap.Config.ARGB_8888, flipBoth.getConfig());

		// 上下左右反転を左右反転して上下反転
		assertTrue(bitmapEquals(original,
			BitmapHelper.applyMirror(
				BitmapHelper.applyMirror(flipBoth, IMirror.MIRROR_HORIZONTAL),
				IMirror.MIRROR_VERTICAL)));
	}

	/**
	 * 元々のBitmapで確保したメモリーサイズより大きくしようとするとIllegalArgumentExceptionを投げる
	 */
	@Test(expected = IllegalArgumentException.class)
	public void reconfigureMakeBiggerTest1() {
		// 初期
		final Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		// 縦横を2倍
		bitmap.reconfigure(WIDTH * 2, HEIGHT * 2, Bitmap.Config.ARGB_8888);
	}

	/**
	 * 元々のBitmapで確保したメモリーサイズより大きくしようとするとIllegalArgumentExceptionを投げる
	 * 縦横は同じだけどConfigがRGB565からARGB_8888でアロケーション済みメモリーが足りないのでIllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void reconfigureMakeBiggerTest2() {
		// 初期
		final Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);
		// サイズは同じでRGB_565からARGB_8888
		bitmap.reconfigure(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
	}

	/**
	 * 元々のBitmapで確保したメモリーサイズより小さくするのはOK
	 */
	@Test
	public void reconfigureMakeSmallerTest1() {
		// 初期
		final Bitmap bitmap = Bitmap.createBitmap(WIDTH * 2, HEIGHT * 2, Bitmap.Config.ARGB_8888);
		// 縦横を半分
		bitmap.reconfigure(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
	}

	/**
	 * 元々のBitmapで確保したメモリーサイズより小さくするのはOK
	 * 縦横は同じだけどConfigがARGB_8888からRGB565だとメモリーが余るのでOK
	 */
	@Test
	public void reconfigureMakeSmallerTest2() {
		// 初期
		final Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		// サイズは同じでARGB_8888からRGB_565
		bitmap.reconfigure(WIDTH, HEIGHT, Bitmap.Config.RGB_565);
	}
}
