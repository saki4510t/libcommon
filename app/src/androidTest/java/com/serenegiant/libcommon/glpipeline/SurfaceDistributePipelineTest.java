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
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceDistributePipeline;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.graphics.BitmapHelper;
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

import static org.junit.Assert.*;
import static com.serenegiant.libcommon.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class SurfaceDistributePipelineTest {
	private static final String TAG = SurfaceRendererPipelineTest.class.getSimpleName();

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
	 * ImageSourcePipelineからの映像ソースをSurfaceDistributePipelineでSurfaceへ
	 * 描画してGLSurfaceReceiverで読み取れることを検証
	 * Bitmap -> ImageSourcePipeline
	 * 		→ SurfaceDistributePipeline → ProxyPipeline → GLSurface.wrap → glReadPixels → Bitmap
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceDistributePipelineTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;

		// テストするSurfaceDistributePipelineを生成
		final SurfaceDistributePipeline distributor = new SurfaceDistributePipeline(manager);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		GLPipeline.append(source, distributor);
		assertTrue(validatePipelineOrder(source, source, distributor));

		final Semaphore sem = new Semaphore(0);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLSurfaceReceiver receiver1 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(receiver1);
		final Surface surface1 = receiver1.getSurface();
		assertNotNull(surface1);
		distributor.addSurface(surface1.hashCode(), surface1, false);

		// 描画結果受け取り用にGLSurfaceReceiverを生成
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver receiver2 = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(receiver2);
		final Surface surface2 = receiver2.getSurface();
		assertNotNull(surface2);
		distributor.addSurface(surface2.hashCode(), surface2, false);

		// パイプライン経由での映像受け取り用にProxyPipelineを生成
		final AtomicReference<Bitmap> result3 = new AtomicReference<>();
		final AtomicInteger cnt3 = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(
			WIDTH, HEIGHT, NUM_FRAMES, sem, result3, cnt3);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, distributor, proxy));

		// addSurfaceは非同期なのでちょっとまたないと反映されない
		ThreadUtils.NoThrowSleep(100);
		// 正常にSurfaceを追加できたかどうかを確認
		assertEquals(2, distributor.getCount());

		try {
			assertTrue(sem.tryAcquire(3, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			distributor.release();
			// Surfaceが受け取った映像フレーム数を確認
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			assertTrue(cnt3.get() >= NUM_FRAMES);
			// 受け取った映像を検証
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
			final Bitmap resultBitmap3 = result3.get();
			assertNotNull(resultBitmap3);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap1));
			assertTrue(bitmapEquals(original, resultBitmap2));
			assertTrue(bitmapEquals(original, resultBitmap3));
		} catch (final InterruptedException e) {
			fail();
		}
	}

}
