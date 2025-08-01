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

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceCapture;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.HandlerThreadHandler;

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

import static com.serenegiant.libcommon.TestUtils.bitmapEquals;
import static com.serenegiant.libcommon.TestUtils.inputImagesAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class GLSurfaceCaptureTest {
	private static final String TAG = GLSurfaceCaptureTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 50;
	private static final long MAX_WAIT_MS = 20000L;

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
	 * GLSurfaceCaptureでSurfaceで受け取った映像をBitmapへキャプチャするテスト
	 * GLSurface#wrapでフレームバッファとして割り当てて読み取る
	 * XXX NEC PC-TE507FAW(ANDROID6)等フレームバッファへの割り当て時にエラーが発生して
	 *     正常に動作しない機種がある
	 */
	@Test
	public void gLSurfaceCaptureTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final GLSurfaceCapture capture = new GLSurfaceCapture(
			manager, WIDTH, HEIGHT,
			new ImageReader.OnImageAvailableListener<Bitmap>() {
				final AtomicInteger cnt = new AtomicInteger();
				@Override
				public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
					final Bitmap bitmap = reader.acquireLatestImage();
					if (bitmap != null) {
						try {
							if (cnt.incrementAndGet() == NUM_FRAMES) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						} finally {
							reader.recycle(bitmap);
						}
					}
				}
			},
			HandlerThreadHandler.createHandler(TAG),
			false/*useOffscreenRendering*/
		);
		final Surface surface = capture.getSurface();
		assertNotNull(surface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			capture.release();
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * GLSurfaceCaptureでSurfaceで受け取った映像をBitmapへキャプチャするテスト
	 * オフスクリーンへ描画してオフスクリーンから映像をビットマップとして読み取る
	 */
	@Test
	public void gLSurfaceCaptureOffscreenTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final GLSurfaceCapture capture = new GLSurfaceCapture(
			manager, WIDTH, HEIGHT,
			new ImageReader.OnImageAvailableListener<Bitmap>() {
				final AtomicInteger cnt = new AtomicInteger();
				@Override
				public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
					final Bitmap bitmap = reader.acquireLatestImage();
					if (bitmap != null) {
						try {
							if (cnt.incrementAndGet() == NUM_FRAMES) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						} finally {
							reader.recycle(bitmap);
						}
					}
				}
			},
			HandlerThreadHandler.createHandler(TAG),
			true/*useOffscreenRendering*/
		);
		final Surface surface = capture.getSurface();
		assertNotNull(surface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			capture.release();
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			fail();
		}
	}
}
