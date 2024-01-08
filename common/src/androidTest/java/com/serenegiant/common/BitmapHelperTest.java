package com.serenegiant.common;
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

import static com.serenegiant.common.TestUtils.*;

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

	@Test
	public void bitmapEqualsTest() {
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		final Bitmap b1 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		final Bitmap b2 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 19,
			Bitmap.Config.ARGB_8888);
		assertTrue(bitmapEquals(b0, b1));
		assertFalse(bitmapEquals(b0, b2));
	}

	@Test
	public void copy() {
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		// copyBitmapのコピー先をnullとした場合は内部で自動生成
		final Bitmap dst = BitmapHelper.copyBitmap(b0, null);
		assertTrue(bitmapEquals(b0, dst));
		// あらかじめ割り当てたビットマップへコピーする場合
		final Bitmap dst1 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		BitmapHelper.copyBitmap(b0, dst1);
		assertTrue(bitmapEquals(b0, dst1));
	}

	/**
	 * BitmapHelper#invertAlphaのテスト
	 */
	@Test
	public void invertAlpha() {
		final Bitmap b0 = BitmapHelper.genMaskImage(
			0, WIDTH, HEIGHT,60,
			Color.RED,0, 100);
		final Bitmap inverted = BitmapHelper.invertAlpha(b0);
		// 2回アルファ値を反転させると元と一致するはず
		assertTrue(bitmapEquals(b0, BitmapHelper.invertAlpha(inverted)));
	}

	/**
	 * BitmapHelper#applyMirrorのテスト
	 */
	@Test
	public void applyMirror() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);

		final Bitmap normal = BitmapHelper.applyMirror(original, IMirror.MIRROR_NORMAL);
		assertTrue(bitmapEquals(original, normal));

		// 左右反転
		final Bitmap flipHorizontal = BitmapHelper.applyMirror(original, IMirror.MIRROR_HORIZONTAL);
		assertFalse(bitmapEquals(original, flipHorizontal));
		// 2回左右反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipHorizontal, IMirror.MIRROR_HORIZONTAL)));

		// 上下反転
		final Bitmap flipVertical = BitmapHelper.applyMirror(original, IMirror.MIRROR_VERTICAL);
		assertFalse(bitmapEquals(original, flipVertical));
		// 2回上下反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipVertical, IMirror.MIRROR_VERTICAL)));

		// 上下左右反転
		final Bitmap flipBoth = BitmapHelper.applyMirror(original, IMirror.MIRROR_BOTH);
		assertFalse(bitmapEquals(original, flipBoth));
		// 2回上下反転させると元と一致するはず
		assertTrue(bitmapEquals(original, BitmapHelper.applyMirror(flipBoth, IMirror.MIRROR_BOTH)));

		// 上下左右反転を左右反転して上下反転
		assertTrue(bitmapEquals(original,
			BitmapHelper.applyMirror(
				BitmapHelper.applyMirror(flipBoth, IMirror.MIRROR_HORIZONTAL),
				IMirror.MIRROR_VERTICAL)));
	}

}
