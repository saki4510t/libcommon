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
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceRendererPipeline;
import com.serenegiant.graphics.BitmapHelper;
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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GLSurfaceReceiverTest {
	private static final String TAG = GLSurfaceReceiverTest.class.getSimpleName();

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
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLSurfaceReceiverで
	 * 読み取れることを検証
	 * Bitmap -> inputImagesAsync
	 * 				↓
	 * 				Surface -> GLSurfaceReceiver
	 * 							-> GLBitmapImageReader -> Bitmap
	 */
	@Test
	public void surfaceReaderTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = new GLManager();
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result);
		assertNotNull(surface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * ImageSourcePipelineからの映像ソースをSurfaceRendererPipelineでテクスチャとして受け取って
	 * GLSurfaceReceiverのSurfaceへ書き込んでGLBitmapImageReaderでビットマップへ変換して
	 * 読み取れることを検証
	 * Bitmap -> ImageSourcePipeline
	 * 				-> SurfaceRendererPipeline
	 * 					↓
	 * 					-> Surface -> GLSurfaceReceiver -> GLBitmapImageReader -> Bitmap
	 */
	@Test
	public void pipelineReaderTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = new GLManager();
		assertTrue(manager.isValid());

		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final SurfaceRendererPipeline renderer = new SurfaceRendererPipeline(manager);
		GLPipeline.append(source, renderer);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result);
		assertNotNull(surface);

		renderer.setSurface(surface);
		assertTrue(validatePipelineOrder(source, source, renderer));

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
			fail();
		}
	}

	/**
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLSurfaceReceiverで
	 * 読み取れることを検証
	 * 途中で映像ソースを切り替える場合
	 * FIXME 1回目はOKだけど映像ソースを切り替えるとビットマップが一致しない、1回目のビットマップになってる
	 * Bitmap -> inputImagesAsync
	 * 				↓
	 * 				Surface -> GLSurfaceReceiver
	 * 							-> GLBitmapImageReader -> Bitmap
	 */
	@Test
	public void surfaceReaderChangeSourceTest() {
		final GLManager manager = new GLManager();
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface surface = createGLSurfaceReceiverSurface(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(surface);

		for (int i = 0; i < 3; i++) {
			Log.v(TAG, "surfaceReaderChangeSourceTest:" + i);
			cnt.set(0);
			result.set(null);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15, 12 + i, Bitmap.Config.ARGB_8888);
//			dump(bitmap);

			final AtomicBoolean requestStop = new AtomicBoolean();
			inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

			try {
				assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				requestStop.set(true);
				assertTrue(cnt.get() >= NUM_FRAMES);
				final Bitmap resultBitmap = result.get();
				assertNotNull(resultBitmap);
				// 元のビットマップと同じかどうかを検証
				assertTrue(bitmapEquals(original, resultBitmap));	// i=0はOKだけどi=1でエラーになる
			} catch (final InterruptedException e) {
				fail();
			}
			ThreadUtils.NoThrowSleep(100L);
		}
	}
}
