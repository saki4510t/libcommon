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
import com.serenegiant.glutils.RendererHolder;
import com.serenegiant.glutils.ImageTextureSource;
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

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RendererHolderTest {

	private static final String TAG = RendererHolderTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@Test
	public void staticTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface readerSurface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, 5, sem, result, cnt);
		assertNotNull(readerSurface);

		// 映像ソースとしてStaticTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		final Surface surface = rendererHolder.getSurface();
		assertNotNull(surface);
		// StaticTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
		source.addSurface(surface.hashCode(), surface, false);
		rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			assertEquals(5, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないので結果も上下入れ替わる
			assertTrue(bitmapEquals(original, flipVertical(b)));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.removeSurface(surface.hashCode());
		rendererHolder.removeSurface(readerSurface.hashCode());
	}

	@Test
	public void imageTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface readerSurface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, 5, sem, result, cnt);
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
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			assertEquals(5, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないので結果も上下入れ替わる
			assertTrue(bitmapEquals(original, flipVertical(b)));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
		source.setSurface(null);
		rendererHolder.removeSurface(readerSurface.hashCode());
	}

	@Test
	public void staticTextureSourceRestartTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface readerSurface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, 5, sem, result, cnt);
		assertNotNull(readerSurface);

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		{
			cnt.set(0);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
			final Surface surface = rendererHolder.getSurface();
			assertNotNull(surface);
			// StaticTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
			source.addSurface(surface.hashCode(), surface, false);
			rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

			try {
				// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
				assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
				assertEquals(5, cnt.get());
				final Bitmap b = result.get();
	//			dump(b);
				assertNotNull(b);
				// 元のビットマップと同じかどうかを検証
				// GLDrawer2Dのテクスチャ座標配列で上下反転させないので結果も上下入れ替わる
				assertTrue(bitmapEquals(original, flipVertical(b)));
			} catch (final InterruptedException e) {
				Log.d(TAG, "interrupted", e);
			}
			source.removeSurface(surface.hashCode());
			rendererHolder.removeSurface(readerSurface.hashCode());
		}
		// 一度繋いだ映像ソースが破棄された後新しい映像ソースを繋ぐ
		{
			cnt.set(0);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
			final Surface surface = rendererHolder.getSurface();
			assertNotNull(surface);
			// StaticTextureSource →　RendererHolder　→ SurfaceReaderと繋ぐ
			source.addSurface(surface.hashCode(), surface, false);
			rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);
			try {
				// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
				assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
				assertEquals(5, cnt.get());
				final Bitmap b = result.get();
	//			dump(b);
				assertNotNull(b);
				// 元のビットマップと同じかどうかを検証
				// GLDrawer2Dのテクスチャ座標配列で上下反転させないので結果も上下入れ替わる
				assertTrue(bitmapEquals(original, flipVertical(b)));
			} catch (final InterruptedException e) {
				Log.d(TAG, "interrupted", e);
			}
			source.removeSurface(surface.hashCode());
			rendererHolder.removeSurface(readerSurface.hashCode());
		}

	}
}
