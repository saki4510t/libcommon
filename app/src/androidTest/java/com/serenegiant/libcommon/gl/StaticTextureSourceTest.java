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
import com.serenegiant.glutils.GLSurfaceReceiver;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class StaticTextureSourceTest {
	private static final String TAG = StaticTextureSourceTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 120;

	@Nullable
	private GLManager mGLManager;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
		mGLManager = new GLManager();
		assertTrue(mGLManager.isValid());
		mGLManager.getEgl();
		assertEquals(1, mGLManager.getMasterWidth());
		assertEquals(1, mGLManager.getMasterHeight());
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		if (mGLManager != null) {
			mGLManager.release();
			mGLManager = null;
		}
	}

	/**
	 * StaticTextureSourceから1つのSurfaceへ映像を書き込みできることを確認
	 * Bitmap → StaticTextureSource
	 * 				↓
	 * 				→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);
		source.addSurface(surface.hashCode(), surface, false);
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertEquals(NUM_FRAMES, cnt.get());
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
	 * Bitmap → StaticTextureSource
	 * 				↓
	 * 				→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 				→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));
		final Semaphore sem = new Semaphore(0);

		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		source.addSurface(surface1.hashCode(), surface1, false);

		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
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

	@Test(timeout = 30000)
	public void frameRate5Test() {
		frameRate(mGLManager, 5);
	}

	@Test(timeout = 20000)
	public void frameRate10Test() {
		frameRate(mGLManager, 10);
	}

	@Test
	public void frameRate15Test() {
		frameRate(mGLManager, 15);
	}

	@Test
	public void frameRate20Test() {
		frameRate(mGLManager, 20);
	}

	@Test
	public void frameRate24Test() {
		frameRate(mGLManager, 24);
	}

	@Test
	public void frameRate30Test() {
		frameRate(mGLManager, 30);
	}

	@Test
	public void frameRate33Test() {
		frameRate(mGLManager, 33);
	}

	@Test
	public void frameRate35Test() {
		frameRate(mGLManager, 35);
	}

	@Test
	public void frameRate45Test() {
		frameRate(mGLManager, 45);
	}

	@Test
	public void frameRate50Test() {
		frameRate(mGLManager, 50);
	}

	@Test
	public void frameRate60Test() {
		frameRate(mGLManager, 60);
	}

	/**
	 * フレームレートを指定して想定通りのフレームレート±10%になるかどうかを確認
	 * Bitmap → StaticTextureSource
	 * 				↓
	 * 				→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * @param manager
	 * @param requestFps
	 */
	private static void frameRate(@NonNull final GLManager manager, final int requestFps) {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		// 映像ソース用にImageTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(requestFps));
		// 映像受け取り用Surfaceをセット
		source.addSurface(surface.hashCode(), surface, false);
		try {
			final long startTimeNs = System.nanoTime();
			assertTrue(sem.tryAcquire(NUM_FRAMES * ((1000 / requestFps) + 20L), TimeUnit.MILLISECONDS));
			final long endTimeNs = System.nanoTime();
			assertEquals(NUM_FRAMES, cnt.get());
			final int n = cnt.get();
			final float fps = (n * 1000000000f) / (endTimeNs - startTimeNs);
			Log.i(TAG, "numFrames=" + n);
			Log.i(TAG, "fps=" + fps + "/" + requestFps);
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
			// フレームレートが指定値の±10%以内にはいっているかどうか
			assertTrue((fps > requestFps * 0.90f) && (fps < requestFps * 1.1f));
		} catch (final InterruptedException e) {
			fail();
		}
	}
}
