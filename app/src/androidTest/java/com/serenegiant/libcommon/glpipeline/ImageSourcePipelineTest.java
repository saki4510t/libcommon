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
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.OnFramePipeline;
import com.serenegiant.glutils.GLBitmapImageReader;
import com.serenegiant.glutils.GLFrameAvailableCallback;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.graphics.MatrixUtils;
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

/**
 * ImageSourcePipelineの映像供給(フレームコールバック呼び出し)の実体はImageTextureSourceなので
 * 供給する映像自体は問題ないはず(ImageTextureSourceTestで検証済み)
 */
@RunWith(AndroidJUnit4.class)
public class ImageSourcePipelineTest {
	private static final String TAG = ImageSourcePipelineTest.class.getSimpleName();

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
	 * ImageSourcePipelineが正常に映像ソースとして動作するかどうかを検証
	 * こっちはOnFramePipelineを経由してGLBitmapImageReaderで映像を受け取る
	 * Bitmap → ImageSourcePipeline
	 * 			→ OnFramePipeline
	 * 				↓
	 * 				コールバック → GLBitmapImageReader → Bitmap
	 */
	@Test
	public void imageSourceTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLBitmapImageReader reader
			= new GLBitmapImageReader(WIDTH, HEIGHT, NUM_FRAMES);
		reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener<Bitmap>() {
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
		}, HandlerThreadHandler.createHandler(TAG));

		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final OnFramePipeline pipeline = new OnFramePipeline(reader);
		source.setPipeline(pipeline);
		assertTrue(validatePipelineOrder(source, source, pipeline));

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertTrue(cnt.get() >= NUM_FRAMES);
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
	 * ImageSourcePipelineが正常に映像ソースとして動作するかどうかを検証
	 * こっちはProxyPipelineを経由して映像を受け取る
	 * Bitmap → ImageSourcePipeline
	 * 			→ ProxyPipeline → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageSourceTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		source.setPipeline(proxy);
		assertTrue(validatePipelineOrder(source, source, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * ImageSourcePipelineが正常に映像ソースとして動作するかどうかを検証
	 * こっちはOnFramePipelineを経由してFrameAvailableCallbackコールバックで
	 * テクスチャから映像を読み取る
	 * Bitmap → ImageSourcePipeline
	 * 			→ OnFramePipeline
	 * 				↓
	 * 				コールバック → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageSourceTest3() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLFrameAvailableCallback callback = new GLFrameAvailableCallback() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3, final boolean isOES,
				final int width, final int height,
				final int texId, @NonNull final float[] texMatrix) {

				// ここはGLコンテキスト内
				if (cnt.incrementAndGet() == NUM_FRAMES) {
					Log.v(TAG, "onFrameAvailable:create Bitmap from texture, isOES=" + isOES + ",texMatrix=" + MatrixUtils.toGLMatrixString(texMatrix));
					result.set(GLUtils.glCopyTextureToBitmap(
						false, width, height, texId, texMatrix, null));
					sem.release();
				}
			}
		};

		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final OnFramePipeline pipeline = new OnFramePipeline(callback);
		source.setPipeline(pipeline);
		assertTrue(validatePipelineOrder(source, source, pipeline));

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertTrue(cnt.get() >= NUM_FRAMES);
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
