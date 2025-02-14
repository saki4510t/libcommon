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
import android.view.Surface;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.GLSurfaceRenderer;
import com.serenegiant.glutils.StaticTextureSource;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.ThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GLSurfaceRendererTest {
	private static final String TAG = GLSurfaceRendererTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 50;

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
	 * GLSurfaceRendererを使って受け取ったテクスチャをSurfaceへ描画できることを検証
	 */
	@Test
	public void gLSurfaceRendererTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 分配描画用にGLSurfaceRendererを生成
		final GLSurfaceRenderer renderer = new GLSurfaceRenderer(
			manager, WIDTH, HEIGHT, GLDrawer2D.DEFAULT_FACTORY);

		// 描画結果受け取り用にSurfaceを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(surface);
		renderer.addSurface(surface.hashCode(), surface, null);
		ThreadUtils.NoThrowSleep(100);
		assertEquals(1, renderer.getCount());

		// 映像書き込み用GLSurfaceReceiverを生成
		final GLSurfaceReceiver receiver = new GLSurfaceReceiver(
			manager, WIDTH, HEIGHT,
			new GLSurfaceReceiver.DefaultCallback(renderer));
		final Surface inputSurface = receiver.getSurface();
		assertNotNull(inputSurface);
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 10, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			assertTrue(cnt.get() >= NUM_FRAMES);
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * GLSurfaceRendererを使って受け取ったテクスチャを複数のSurfaceへ分配描画できることを検証
	 */
	@Test
	public void gLSurfaceRendererTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 分配描画用にGLSurfaceRendererを生成
		final GLSurfaceRenderer renderer = new GLSurfaceRenderer(
			manager, WIDTH, HEIGHT, GLDrawer2D.DEFAULT_FACTORY);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		renderer.addSurface(surface1.hashCode(), surface1, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		renderer.addSurface(surface2.hashCode(), surface2, null);

		// 複数のSurfaceを追加できたかどうかを確認
		ThreadUtils.NoThrowSleep(100);
		assertEquals(2, renderer.getCount());

		// 映像書き込み用GLSurfaceReceiverを生成
		final GLSurfaceReceiver receiver = new GLSurfaceReceiver(
			manager, WIDTH, HEIGHT,
			new GLSurfaceReceiver.DefaultCallback(renderer));
		final Surface inputSurface = receiver.getSurface();
		assertNotNull(inputSurface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 10, requestStop);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			// それぞれのSurfaceが受け取った映像フレーム数を確認
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			// 受け取った映像を検証
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap1));
			assertTrue(bitmapEquals(original, resultBitmap2));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * GLSurfaceRendererを使って受け取ったテクスチャを複数のSurfaceへ分配描画できることを検証
	 */
	@Test
	public void staticTextureSourceRestartTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 分配描画用にGLSurfaceRendererを生成
		final GLSurfaceRenderer renderer = new GLSurfaceRenderer(
			manager, WIDTH, HEIGHT, GLDrawer2D.DEFAULT_FACTORY);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		renderer.addSurface(surface1.hashCode(), surface1, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		renderer.addSurface(surface2.hashCode(), surface2, null);

		// 複数のSurfaceを追加できたかどうかを確認
		ThreadUtils.NoThrowSleep(100);
		assertEquals(2, renderer.getCount());

		// 映像ソースとしてStaticTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));

		// 映像書き込み用GLSurfaceReceiverを生成
		final GLSurfaceReceiver receiver = new GLSurfaceReceiver(
			manager, WIDTH, HEIGHT,
			new GLSurfaceReceiver.DefaultCallback(renderer));
		final Surface inputSurface = receiver.getSurface();
		assertNotNull(inputSurface);
		// StaticTextureSource →　GLSurfaceReceiverと繋ぐ
		source.addSurface(inputSurface.hashCode(), inputSurface, false);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			// それぞれのSurfaceが受け取った映像フレーム数を確認
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			// 受け取った映像を検証
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap1));
			assertTrue(bitmapEquals(original, resultBitmap2));
		} catch (final InterruptedException e) {
			fail();
		}
	}
}
