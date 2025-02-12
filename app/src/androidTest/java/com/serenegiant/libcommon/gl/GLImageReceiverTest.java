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
import com.serenegiant.glutils.GLImageReceiver;
import com.serenegiant.glutils.GLBitmapImageReader;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.HandlerThreadHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GLImageReceiverTest {
	private static final String TAG = GLImageReceiverTest.class.getSimpleName();

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

	/**
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLImageReceiverで
	 * 読み取れることを検証
	 * Bitmap -> inputImagesAsync
	 * 				↓
	 * 				Surface -> GLImageReceiver
	 * 							-> GLSurface.wrap -> glReadPixels -> ByteBuffer
	 */
	@Test
	public void surfaceReaderTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = new GLManager();
		final Semaphore sem = new Semaphore(0);
		final ByteBuffer buffer = allocateBuffer(WIDTH, HEIGHT);
		// 映像受け取り用にSurfaceReaderを生成
		final Surface surface = createGLImageReceiverSurface(
			manager, WIDTH, HEIGHT, 10, sem, buffer);
		assertNotNull(surface);
		inputImagesAsync(original, surface, 10);

		try {
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
			assertNotNull(result);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, result));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLImageReceiver
	 * (とGLBitmapImageReader)で読み取れることを検証
	 * Bitmap -> inputImagesAsync
	 * 				↓
	 * 				Surface -> GLImageReceiver
	 * 							-> GLBitmapImageReader -> Bitmap
	 */
	@Test
	public void surfaceReaderTest2() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

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
						if (cnt.incrementAndGet() == 5) {
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

		inputImagesAsync(original, surface, 10);

		try {
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
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
	 * ImageSourcePipelineからの映像ソースをSurfaceRendererPipelineでテクスチャとして受け取って
	 * GLImageReceiverのSurfaceへ書き込んでGLBitmapImageReaderでビットマップへ変換して
	 * 読み取れることを検証
	 * Bitmap -> ImageSourcePipeline
	 * 				-> SurfaceRendererPipeline
	 * 					↓
	 * 					-> Surface -> GLImageReceiver -> GLBitmapImageReader -> Bitmap
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
		final GLBitmapImageReader reader
			= new GLBitmapImageReader(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888, 2);
		reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener<Bitmap>() {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
				final Bitmap bitmap = reader.acquireLatestImage();
				if (bitmap != null) {
					try {
						if (cnt.incrementAndGet() == 5) {
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
		renderer.setSurface(surface);
		assertTrue(validatePipelineOrder(source, source, renderer));

		try {
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
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
}
