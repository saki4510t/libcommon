package com.serenegiant.libcommon.glpipeline;
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
import com.serenegiant.glpipeline.EffectPipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.graphics.BitmapHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EffectPipelineTest {
	private static final String TAG = EffectPipelineTest.class.getSimpleName();

	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;
	private static final int NUM_FRAMES = 50;

	@Nullable
	private GLManager mManager;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
		mManager = new GLManager();
		assertTrue(mManager.isValid());
		mManager.getEgl();
		assertEquals(1, mManager.getMasterWidth());
		assertEquals(1, mManager.getMasterHeight());
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		if (mManager != null) {
			mManager.release();
			mManager = null;
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline pipeline1 = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineを2つ連結処理したときに想定通りに動作になるかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ EffectPipeline
	 * 					→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline pipeline1 = new EffectPipeline(manager);
		final EffectPipeline pipeline2 = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(pipeline2);
		pipeline2.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineを3つ連結処理したときに想定通りに動作になるかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ EffectPipeline
	 * 					→ EffectPipeline
	 * 						→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline pipeline1 = new EffectPipeline(manager);
		final EffectPipeline pipeline2 = new EffectPipeline(manager);
		final EffectPipeline pipeline3 = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(pipeline2);
		pipeline2.setPipeline(pipeline3);
		pipeline3.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, pipeline3, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → EffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void effectPipelineOESTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "EffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "EffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するEffectPipelineを生成
		final EffectPipeline pipeline1 = new EffectPipeline(manager);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → EffectPipeline → EffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void effectPipelineOESTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "EffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "EffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するEffectPipelineを生成
		final EffectPipeline pipeline1 = new EffectPipeline(manager);
		final EffectPipeline pipeline2 = new EffectPipeline(manager);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, pipeline2);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → EffectPipeline → EffectPipeline → EffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void effectPipelineOESTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "EffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "EffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するEffectPipelineを生成
		final EffectPipeline pipeline1 = new EffectPipeline(manager);
		final EffectPipeline pipeline2 = new EffectPipeline(manager);
		final EffectPipeline pipeline3 = new EffectPipeline(manager);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, pipeline2);
		GLPipeline.append(source, pipeline3);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, pipeline3, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineへ繋いだProxyPipelineとEffectPipelineへセットしたSurfaceの
	 * 両方へ映像が転送されることを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				↓
	 * 				→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 * 				→ (Surface) → GLSurfaceReceiver	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineWithSurfaceTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		// テスト対象のEffectPipelineを生成
		final EffectPipeline pipeline = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);

		// パイプラインを経由した映像の受け取り用にProxyPipelineを生成する
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(proxy);
		pipeline.setPipeline(proxy);

		// Surfaceを経由した映像の受け取り用にGLSurfaceReceiverを生成する
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver bitmapReceiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(bitmapReceiver);
		final Surface receiverSurface = bitmapReceiver.getSurface();
		assertNotNull(receiverSurface);
		pipeline.setSurface(receiverSurface);

		source.setPipeline(pipeline);
		assertTrue(validatePipelineOrder(source, source, pipeline, proxy));

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
			assertTrue(bitmapEquals(original, resultBitmap1, true));
			assertTrue(bitmapEquals(original, resultBitmap2, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → EffectPipeline
	 * 								 	↓
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 * 									→ (Surface) → GLSurfaceReceiver	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void drawerPipelineOESTestWithSurface1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "EffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "EffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するEffectPipelineを生成
		final EffectPipeline pipeline1 = new EffectPipeline(manager);

		// パイプラインを経由した映像の受け取り用にProxyPipelineを生成する
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(proxy);
		pipeline1.setPipeline(proxy);

		// Surfaceを経由した映像の受け取り用にGLSurfaceReceiverを生成する
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver bitmapReceiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(bitmapReceiver);
		final Surface receiverSurface = bitmapReceiver.getSurface();
		assertNotNull(receiverSurface);
		pipeline1.setSurface(receiverSurface);

		source.setPipeline(pipeline1);
		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
			assertTrue(bitmapEquals(original, resultBitmap1, true));
			assertTrue(bitmapEquals(original, resultBitmap2, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}
}
