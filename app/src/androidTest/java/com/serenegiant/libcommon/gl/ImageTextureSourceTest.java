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
import com.serenegiant.glutils.GLImageReceiver;
import com.serenegiant.glutils.GLBitmapImageReader;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.glutils.ImageTextureSource;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.HandlerThreadHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ImageTextureSourceTest {
	private static final String TAG = ImageTextureSourceTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int MAX_FRAMES = 50;
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

	@Test
	public void imageTextureSourceTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// 映像受け取り用にGLImageReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Surface surface = createGLImageReceiverSurface(
			manager, WIDTH, HEIGHT, MAX_FRAMES, sem, result);
		assertNotNull(surface);

		// 映像ソース用にImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(manager, original, new Fraction(30));
		// 映像受け取り用Surfaceをセット
		source.setSurface(surface);
		try {
			assertTrue(sem.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	@Test
	public void imageTextureSourceTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final GLBitmapImageReader reader
			= new GLBitmapImageReader(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888, 2);
		reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener<Bitmap>() {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
				final Bitmap bitmap = reader.acquireLatestImage();
				if (bitmap != null) {
					try {
						if (cnt.incrementAndGet() == MAX_FRAMES) {
							result.set(Bitmap.createBitmap(bitmap));
							sem.release();
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler(TAG));

		final GLImageReceiver receiver = new GLImageReceiver(WIDTH, HEIGHT, reader);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		// 映像ソース用にImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(manager, original, new Fraction(30));
		// 映像受け取り用Surfaceをセット
		source.setSurface(surface);
		try {
			assertTrue(sem.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	@Test
	public void frameRate5Test() {
		frameRate(mManager, 5);
	}

	@Test
	public void frameRate10Test() {
		frameRate(mManager, 10);
	}

	@Test
	public void frameRate15Test() {
		frameRate(mManager, 15);
	}

	@Test
	public void frameRate20Test() {
		frameRate(mManager, 20);
	}

	@Test
	public void frameRate24Test() {
		frameRate(mManager, 24);
	}

	@Test
	public void frameRate30Test() {
		frameRate(mManager, 30);
	}

	@Test
	public void frameRate33Test() {
		frameRate(mManager, 33);
	}

	@Test
	public void frameRate35Test() {
		frameRate(mManager, 35);
	}

	@Test
	public void frameRate45Test() {
		frameRate(mManager, 45);
	}

	@Test
	public void frameRate50Test() {
		frameRate(mManager, 50);
	}

	@Test
	public void frameRate60Test() {
		frameRate(mManager, 60);
	}

	/**
	 * フレームレートを指定して想定通りのフレームレート±10%になるかどうかを確認
	 * @param manager
	 * @param requestFps
	 */
	private static void frameRate(@NonNull final GLManager manager, final int requestFps) {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger numFrames = new AtomicInteger();
		final GLBitmapImageReader reader
			= new GLBitmapImageReader(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888, 2);
		reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener<Bitmap>() {
			@Override
			public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
				final Bitmap bitmap = reader.acquireLatestImage();
				if (bitmap != null) {
					try {
						if (numFrames.incrementAndGet() == MAX_FRAMES) {
							result.set(Bitmap.createBitmap(bitmap));
							sem.release();
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler(TAG));

		final GLImageReceiver receiver = new GLImageReceiver(WIDTH, HEIGHT, reader);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		// 映像ソース用にImageTextureSourceを生成
		final ImageTextureSource source = new ImageTextureSource(manager, original, new Fraction(requestFps));
		// 映像受け取り用Surfaceをセット
		source.setSurface(surface);
		try {
			final long startTimeNs = System.nanoTime();
			assertTrue(sem.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
			final long endTimeNs = System.nanoTime();
			final int n = numFrames.get();
			final float fps = (n * 1000000000f) / (endTimeNs - startTimeNs);
			Log.i(TAG, "numFrames=" + n);
			Log.i(TAG, "fps=" + fps + "/" + requestFps);
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
			// フレームレートが指定値の±10%以内にはいっているかどうか
			assertTrue((fps > requestFps * 0.90f) && (fps < requestFps * 1.1f));
		} catch (final InterruptedException e) {
			fail();
		}
	}
}
