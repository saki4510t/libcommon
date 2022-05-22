package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import com.serenegiant.glutils.GLImageReceiver;
import com.serenegiant.glutils.GLBitmapImageReader;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.glutils.SurfaceProxy;
import com.serenegiant.graphics.BitmapHelper;
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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.common.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SurfaceProxyTest {
	private static final String TAG = GLImageReceiverTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int MAX_IMAGES = 200;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@Test
	public void surfaceProxyReaderWriter() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// SurfaceProxyReaderWriterを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, true);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyReaderWriter);
		final Surface inputSurface = proxy.getInputSurface();
		assertNotNull(inputSurface);

		// 映像読み取り用にSurfaceReaderを準備
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
						if (cnt.incrementAndGet() >= (int)(MAX_IMAGES * 0.9f)) {
							proxy.setSurface(null);
							if (sem.availablePermits() == 0) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler());

		final GLImageReceiver receiver = new GLImageReceiver(WIDTH, HEIGHT, reader);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);
		// プロキシに映像読み取り用Surfaceをセット
		proxy.setSurface(readerSurface);

		// プロキシへ映像を書き込み
		inputImagesAsync(original, inputSurface, MAX_IMAGES);

		try {
			assertTrue(sem.tryAcquire(MAX_IMAGES * 32, TimeUnit.MILLISECONDS));
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitMapEquals(original, b));
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void surfaceProxyGLES() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// SurfaceProxyReaderWriterを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, false);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyGLES);
		final Surface inputSurface = proxy.getInputSurface();
		assertNotNull(inputSurface);

		// 映像読み取り用にSurfaceReaderを準備
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
						if (cnt.incrementAndGet() >= (int)(MAX_IMAGES * 0.9f)) {
							proxy.setSurface(null);
							if (sem.availablePermits() == 0) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler());

		final GLImageReceiver receiver = new GLImageReceiver(WIDTH, HEIGHT, reader);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);
		// プロキシに映像読み取り用Surfaceをセット
		proxy.setSurface(readerSurface);

		inputImagesAsync(original, inputSurface, MAX_IMAGES);

		try {
			assertTrue(sem.tryAcquire(MAX_IMAGES * 32, TimeUnit.MILLISECONDS));
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitMapEquals(original, b));
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

}
