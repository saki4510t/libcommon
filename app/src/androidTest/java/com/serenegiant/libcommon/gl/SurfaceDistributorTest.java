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

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.IRendererHolder;
import com.serenegiant.glutils.ImageTextureSource;
import com.serenegiant.glutils.StaticTextureSource;
import com.serenegiant.glutils.SurfaceDistributor;
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

/**
 * SurfaceDistributorでSurfaceとして受け取った映像をSurfaceへ分配描画できることを検証
 */
@RunWith(AndroidJUnit4.class)
public class SurfaceDistributorTest {
	private static final String TAG = SurfaceDistributorTest.class.getSimpleName();

	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;
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
	 * SurfaceDistributorがSurfaceを経由して受け取った映像をSurfaceへ描画できることを検証
	 */
	@Test
	public void surfaceDistributorTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		// 描画結果受け取り用にSurfaceを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(surface);
		distributor.addSurface(surface.hashCode(), surface, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(1, distributor.getCount());

		final Surface inputSurface = distributor.getSurface();
		assertNotNull(inputSurface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

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
	 * SurfaceDistributorがSurfaceを経由して受け取った映像を複数のSurfaceへ分配描画できることを検証
	 */
	@Test
	public void surfaceDistributorTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		distributor.addSurface(surface1.hashCode(), surface1, false, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		distributor.addSurface(surface2.hashCode(), surface2, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, distributor.getCount());

		// Surface経由してBitmapを書き込み
		final Surface inputSurface = distributor.getSurface();
		assertNotNull(inputSurface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

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
	 * SurfaceDistributorがSurfaceを経由して受け取った映像をSurfaceへ描画できることを検証
	 * 映像ソースを途中で変更する
	 * FIXME 1回目はOKだけど映像ソースを変えた後でビットマップが一致しない
	 *       GLSurfaceReceiverの問題かも
	 */
	@Test
	public void surfaceDistributorChangeSourceTest() {
		final GLManager manager = mGLManager;

		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		// 描画結果受け取り用にSurfaceを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(surface);
		distributor.addSurface(surface.hashCode(), surface, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(1, distributor.getCount());

		for (int i = 0; i < 3; i++) {
			Log.v(TAG, "surfaceDistributorTest2:" + i);
			cnt.set(0);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15, 12 + i, Bitmap.Config.ARGB_8888);
//			dump(bitmap);
			final Surface inputSurface = distributor.getSurface();
			assertNotNull(inputSurface);

			final AtomicBoolean requestStop = new AtomicBoolean();
			inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

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
	}

	@Test
	public void staticTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// テストするSurfaceDistributorを生成
		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		distributor.addSurface(surface1.hashCode(), surface1, false, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		distributor.addSurface(surface2.hashCode(), surface2, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, distributor.getCount());

		// 映像ソースとしてStaticTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
		final Surface inputSurface = distributor.getSurface();
		assertNotNull(inputSurface);
		source.addSurface(inputSurface.hashCode(), inputSurface, false);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.removeSurface(inputSurface.hashCode());
			source.release();
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
			Log.d(TAG, "interrupted", e);
		}
		source.removeSurface(inputSurface.hashCode());
	}

	@Test
	public void imageTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// テストするSurfaceDistributorを生成
		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		distributor.addSurface(surface1.hashCode(), surface1, false, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		distributor.addSurface(surface2.hashCode(), surface2, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, distributor.getCount());

		// 映像ソースとしてImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(new GLManager(), original, new Fraction(30));

		final Surface inputSurface = distributor.getSurface();
		assertNotNull(inputSurface);
		source.setSurface(inputSurface);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
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
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * 映像ソースを切り替えても正常に描画できることを検証
	 * FIXME 1回目はOKだけど映像ソースを変えた後でビットマップが一致しない
	 *       GLSurfaceReceiverの問題かも
	 */
	@Test
	public void staticTextureSourceChangeSourceTest() {
		final GLManager manager = mGLManager;

		// テストするSurfaceDistributorを生成
		final SurfaceDistributor distributor = new SurfaceDistributor(
			manager, false,
			WIDTH, HEIGHT,
			GLDrawer2D.DEFAULT_FACTORY,
			IRendererHolder.DEFAULT_CALLBACK);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final Surface surface1 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(surface1);
		distributor.addSurface(surface1.hashCode(), surface1, false, null);

		// 描画結果受け取り用にSurfaceを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final Surface surface2 = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(surface2);
		distributor.addSurface(surface2.hashCode(), surface2, false, null);

		// 1回目
		{
			Log.v(TAG, "staticTextureSourceRestartTest:0");
			cnt1.set(0);
			cnt2.set(0);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//			dump(bitmap);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
			final Surface inputSurface = distributor.getSurface();
			assertNotNull(inputSurface);
			// StaticTextureSource →　SurfaceDistributorと繋ぐ
			source.addSurface(inputSurface.hashCode(), inputSurface, false);

			try {
				assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				source.removeSurface(inputSurface.hashCode());
				source.release();
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
		// 映像ソースを切り替える
		for (int i = 1; i < 3; i++) {
			Log.v(TAG, "staticTextureSourceRestartTest:" + i);
			cnt1.set(0);
			cnt2.set(0);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15, 12 + i, Bitmap.Config.ARGB_8888);
//			dump(bitmap);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(original, new Fraction(30));
			final Surface inputSurface = distributor.getSurface();
			assertNotNull(inputSurface);
			// StaticTextureSource →　SurfaceDistributorと繋ぐ
			source.addSurface(inputSurface.hashCode(), inputSurface, false);

			try {
				assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				source.removeSurface(inputSurface.hashCode());
				source.release();
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

}
