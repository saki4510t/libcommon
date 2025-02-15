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

import com.serenegiant.gl.GLManager;
import com.serenegiant.glpipeline.EffectPipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.graphics.BitmapHelper;

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

import static com.serenegiant.libcommon.TestUtils.bitmapEquals;
import static com.serenegiant.libcommon.TestUtils.createImageReceivePipeline;
import static com.serenegiant.libcommon.TestUtils.validatePipelineOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

		final EffectPipeline effectPipeline = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(effectPipeline);
		effectPipeline.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline, proxy));

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

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(effectPipeline1);
		effectPipeline1.setPipeline(effectPipeline2);
		effectPipeline2.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline1, effectPipeline2, proxy));

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

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline3 = new EffectPipeline(manager);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(effectPipeline1);
		effectPipeline1.setPipeline(effectPipeline2);
		effectPipeline2.setPipeline(effectPipeline3);
		effectPipeline3.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline1, effectPipeline2, effectPipeline3, proxy));

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

}
