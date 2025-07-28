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
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.IRendererHolder;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

/**
 * RendererHolderでSurfaceとして受け取った映像をSurfaceへ分配描画できることを検証
 */
@RunWith(AndroidJUnit4.class)
public class RendererHolderNewTest {
	private static final String TAG = RendererHolderNewTest.class.getSimpleName();

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
	 * RendererHolderがSurfaceを経由して受け取った映像をSurfaceへ描画できることを検証
	 * Bitmap → inputImagesAsync → (Surface)
	 * 	→ RendererHolder
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceDistributorTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final RendererHolder rendererHolder = new RendererHolder(
			WIDTH, HEIGHT,
			IRendererHolder.DEFAULT_CALLBACK);
		final Surface inputSurface = rendererHolder.getSurface();
		assertNotNull(inputSurface);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);
		rendererHolder.addSurface(surface.hashCode(), surface, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(1, rendererHolder.getCount());

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			rendererHolder.release();
			// Surfaceが受け取った映像フレーム数を確認
			assertTrue(cnt.get() >= NUM_FRAMES);
			// 受け取った映像を検証
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * RendererHolderがSurfaceを経由して受け取った映像を複数のSurfaceへ分配描画できることを検証
	 * Bitmap → inputImagesAsync → (Surface)
	 * 	→ RendererHolder
	 * 		↓
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceDistributorTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		final RendererHolder rendererHolder = new RendererHolder(
			WIDTH, HEIGHT,
			IRendererHolder.DEFAULT_CALLBACK);
		final Surface inputSurface = rendererHolder.getSurface();
		assertNotNull(inputSurface);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		rendererHolder.addSurface(surface1.hashCode(), surface1, false, null);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		rendererHolder.addSurface(surface2.hashCode(), surface2, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			rendererHolder.release();
			// Surfaceが受け取った映像フレーム数を確認
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
	 * RendererHolderがSurfaceを経由して受け取った映像をSurfaceへ描画できることを検証
	 * 映像ソースを途中で変更する
	 * Bitmap → inputImagesAsync → (Surface)
	 * 	→ RendererHolder
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceDistributorChangeSourceTest() {
		final GLManager manager = mGLManager;

		final RendererHolder rendererHolder = new RendererHolder(
			WIDTH, HEIGHT,
			IRendererHolder.DEFAULT_CALLBACK);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);
		rendererHolder.addSurface(surface.hashCode(), surface, false, null);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(1, rendererHolder.getCount());

		try {
			for (int i = 0; i < 3; i++) {
				Log.v(TAG, "surfaceDistributorChangeSourceTest:" + i);
				cnt.set(0);
				final Bitmap original = BitmapHelper.makeCheckBitmap(
					WIDTH, HEIGHT, 15, 12 + i, Bitmap.Config.ARGB_8888);
//				dump(bitmap);
				final Surface inputSurface = rendererHolder.getSurface();
				assertNotNull(inputSurface);

				final AtomicBoolean requestStop = new AtomicBoolean();
				inputImagesAsync(original, inputSurface, NUM_FRAMES + 5, requestStop);

				try {
					assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
					requestStop.set(true);
					// Surfaceが受け取った映像フレーム数を確認
					assertTrue(cnt.get() >= NUM_FRAMES);
					// 受け取った映像を検証
					final Bitmap resultBitmap = result.get();
					assertNotNull(resultBitmap);
					// 元のビットマップと同じかどうかを検証
					assertTrue(bitmapEquals(original, resultBitmap));
					// 映像入力用SurfaceTexture/Surfaceを強制的に再生成させる
					rendererHolder.reset();
				} catch (final InterruptedException e) {
					fail();
				}
				// XXX ここでちょっと待機しないとなぜか2回目のinputImagesAsyncが即終了してしまう時がある
				//     inputImagesAsyncへ渡しているAtomicBooleanがコラプトしてる…
				ThreadUtils.NoThrowSleep(100L);
			}
		} catch (final Exception e) {
			fail();
		} finally {
			rendererHolder.release();
		}
	}

	/**
	 * Bitmap → StaticTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void staticTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		final Surface inputSurface = rendererHolder.getSurface();
		assertNotNull(inputSurface);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		rendererHolder.addSurface(surface1.hashCode(), surface1, false);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		rendererHolder.addSurface(surface2.hashCode(), surface2, false);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		// 映像ソースとしてStaticTextureSourceを生成
		final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));
		source.addSurface(inputSurface.hashCode(), inputSurface, false);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.removeSurface(inputSurface.hashCode());
			source.release();
			rendererHolder.release();
			// Surfaceが受け取った映像フレーム数を確認
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
	 * Bitmap → ImageTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageTextureSourceTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);
		final Surface inputSurface = rendererHolder.getSurface();
		assertNotNull(inputSurface);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		rendererHolder.addSurface(surface1.hashCode(), surface1, false);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		rendererHolder.addSurface(surface2.hashCode(), surface2, false);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		// 映像ソースとしてImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(new GLManager(), original, new Fraction(30));
		source.setSurface(inputSurface);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			rendererHolder.release();
			// Surfaceが受け取った映像フレーム数を確認
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
	 * 映像ソースを切り替えても正常に描画できることを検証
	 * Bitmap → StaticTextureSource → (Surface)
	 * 		→ RendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void staticTextureSourceChangeSourceTest() {
		final GLManager manager = mGLManager;

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		rendererHolder.addSurface(surface1.hashCode(), surface1, false);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		rendererHolder.addSurface(surface2.hashCode(), surface2, false);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		try {
			for (int i = 0; i < 3; i++) {
				Log.v(TAG, "staticTextureSourceRestartTest:" + i);
				cnt1.set(0);
				cnt2.set(0);
				final Bitmap original = BitmapHelper.makeCheckBitmap(
					WIDTH, HEIGHT, 15 + i, 12, Bitmap.Config.ARGB_8888);
//				dump(bitmap);
				// 映像ソースとしてStaticTextureSourceを生成
				final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));
				final SurfaceTexture st = rendererHolder.getSurfaceTexture();
				final Surface inputSurface = new Surface(st);
				assertNotNull(inputSurface);
				source.addSurface(inputSurface.hashCode(), inputSurface, false);

				try {
					assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
					source.removeSurface(inputSurface.hashCode());
					source.release();
					// Surfaceが受け取った映像フレーム数を確認
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
				} finally {
					inputSurface.release();
				}
				// XXX #resetかclearSurfaceAllを入れないと2巡目でエラーになる
				//     #getSurface/#getSurfaceTexture内でclearSurfaceAllを
				//     呼んでもだめだった
//				rendererHolder.reset();
				rendererHolder.clearSurfaceAll(0xff000000);
				ThreadUtils.NoThrowSleep(100L);
			}
		} finally {
			rendererHolder.release();
		}
	}

	/**
	 * IRendererHolder#clearColorAll呼び出しで分配先Surfaceが更新されることを確認
	 * RendererHolder
	 * 		↓
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void clearColorTest() {
		final GLManager manager = mGLManager;

		// テストするRendererHolderを生成
		final RendererHolder rendererHolder = new RendererHolder(WIDTH, HEIGHT, null);

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1, true);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		rendererHolder.addSurface(surface1.hashCode(), surface1, false);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2, true);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		rendererHolder.addSurface(surface2.hashCode(), surface2, false);

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, rendererHolder.getCount());

		try {
			for (int i = 0; i < 3; i++) {
				Log.v(TAG, "clearColorTest:" + i);
				cnt1.set(0);
				cnt2.set(0);

//				final int color = 0xfdca7431;	// 0xfdcc7531になる
//				final int color = 0x12345678;	// 0x12ffffffになる
//				final int color = 0x87654321;	// 0x87bf7f3eになる
				final int color = 0xff000000 + 0x7f << i;
				final AtomicBoolean requestStop = new AtomicBoolean();
				clearRendererAsync(rendererHolder, color, NUM_FRAMES + 5, requestStop);
				try {
					assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
					requestStop.set(true);
					// Surfaceが受け取った映像フレーム数を確認
					assertTrue(cnt1.get() >= NUM_FRAMES);
					assertTrue(cnt2.get() >= NUM_FRAMES);
					// 受け取った映像を検証
					final Bitmap resultBitmap1 = result1.get();
					assertNotNull(resultBitmap1);
					final Bitmap resultBitmap2 = result2.get();
					assertNotNull(resultBitmap2);
					assertTrue(bitmapEquals(resultBitmap1, resultBitmap2, false, true));
					// 0x00〜0xffのA,R,G,Bの値を0.0f〜1.0fの浮動小数点にして塗りつぶすので元の32ビット整数で指定した値とは完全には一致しない
//					assertTrue(bitmapFilledColor(resultBitmap1, color, false, true));
//					assertTrue(bitmapFilledColor(resultBitmap2, color, false, true));
				} catch (final InterruptedException e) {
					fail();
				}
				// XXX #resetかclearSurfaceAllを入れないと2巡目でエラーになる
				//     #getSurface/#getSurfaceTexture内でclearSurfaceAllを
				//     呼んでもだめだった
//				rendererHolder.reset();
				rendererHolder.clearSurfaceAll(0xff000000);
				ThreadUtils.NoThrowSleep(100L);
			}
		} finally {
			rendererHolder.release();
		}
	}
}
