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
import com.serenegiant.glutils.RendererHolder;
import com.serenegiant.glutils.ImageTextureSource;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RendererHolderTest {

	private static final String TAG = RendererHolderTest.class.getSimpleName();

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
	 * Bitmap → StaticTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void staticTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);

		// 映像ソースとしてStaticTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		final Surface surface = rendererHolder.getSurface();
		assertNotNull(surface);
		// StaticTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
		source.addSurface(surface.hashCode(), surface, false);
		rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
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
		rendererHolder.removeSurface(readerSurface.hashCode());
	}

	/**
	 * Bitmap → ImageTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);

		// 映像ソースとしてImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(new GLManager(), original, new Fraction(30));

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		final Surface surface = rendererHolder.getSurface();
		assertNotNull(surface);
		// ImageTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
		source.setSurface(surface);
		rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないので結果も上下入れ替わる
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.setSurface(null);
		rendererHolder.removeSurface(readerSurface.hashCode());
	}

	/**
	 * Bitmap → ImageTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageTextureSourceTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface readerSurface1 = receiver1.getSurface();
		assertNotNull(readerSurface1);

		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface readerSurface2 = receiver2.getSurface();
		assertNotNull(readerSurface1);

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		// 分配描画用にSurfaceを追加
		rendererHolder.addSurface(readerSurface1.hashCode(), readerSurface1, false);
		rendererHolder.addSurface(readerSurface2.hashCode(), readerSurface2, false);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		final Surface surface = rendererHolder.getSurface();
		assertNotNull(surface);

		// 映像ソースとしてImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(new GLManager(), original, new Fraction(30));
		// ImageTextureSource →　RendererHolder　→ GLSurfaceReceiverと繋ぐ
		source.setSurface(surface);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt1.get());
			assertEquals(NUM_FRAMES, cnt2.get());
			final Bitmap b = result1.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.setSurface(null);
		rendererHolder.removeSurface(readerSurface1.hashCode());
	}

	/**
	 * 途中で映像ソースを変更する
	 * Bitmap → ImageTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void staticTextureSourceChangeTest() {
		final GLManager manager = mGLManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		for (int i = 0; i < 3; i++) {
			cnt.set(0);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15 + i, 12, Bitmap.Config.ARGB_8888);
//			dump(bitmap);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));
			final Surface surface = rendererHolder.getSurface();
			assertNotNull(surface);
			// StaticTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
			source.addSurface(surface.hashCode(), surface, false);
			rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

			try {
				// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
				assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
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
			rendererHolder.removeSurface(readerSurface.hashCode());
			ThreadUtils.NoThrowSleep(100L);
		}
	}

}
