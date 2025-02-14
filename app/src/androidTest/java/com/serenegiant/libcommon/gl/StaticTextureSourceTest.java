package com.serenegiant.libcommon.gl;
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
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.StaticTextureSource;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

import static com.serenegiant.libcommon.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class StaticTextureSourceTest {
	private static final String TAG = StaticTextureSourceTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 50;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	/**
	 * StaticTextureSourceから1つのSurfaceへ映像を書き込みできることを確認
	 */
	@Test
	public void imageTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);
		final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface surface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(surface);
		source.addSurface(surface.hashCode(), surface, false);
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertTrue(cnt.get() >= NUM_FRAMES);
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.removeSurface(surface.hashCode());
	}

	/**
	 * StaticTextureSourceから2つのSurfaceへ映像を書き込みできることを確認
	 */
	@Test
	public void imageTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);
		final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
		final Semaphore sem = new Semaphore(0);

		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);

		source.addSurface(surface1.hashCode(), surface1, false);

		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, 30, sem, result2, cnt2);
		assertNotNull(surface2);
		source.addSurface(surface2.hashCode(), surface2, false);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertTrue(cnt1.get() >= NUM_FRAMES);
			final Bitmap b1 = result1.get();
//			dump(b1);
			assertNotNull(b1);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b1));

			assertTrue(cnt2.get() >= NUM_FRAMES);
			final Bitmap b2 = result2.get();
//			dump(b1);
			assertNotNull(b2);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b2));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.removeSurface(surface1.hashCode());
		source.removeSurface(surface2.hashCode());
	}
}
