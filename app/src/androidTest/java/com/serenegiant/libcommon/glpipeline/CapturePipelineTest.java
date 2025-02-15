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
import com.serenegiant.glpipeline.CapturePipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
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

import static org.junit.Assert.*;
import static com.serenegiant.libcommon.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class CapturePipelineTest {
	private static final String TAG = CapturePipelineTest.class.getSimpleName();

	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;
	private static final int NUM_FRAMES = 10;

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
	 * CapturePipelineで1回ビットマップを取得するテスト
	 * ImageSourcePipelineからの映像ソースなのでTEXTURE_2Dテクスチャ
	 * Bitmap → ImageSourcePipeline → CapturePipeline → Bitmap
	 */
	@Test
	public void capturePipelineOneshotTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Semaphore sem = new Semaphore(0);
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() == 1) {
					result.set(Bitmap.createBitmap(bitmap));
					sem.release();
				}
			}
			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				result.set(null);
				sem.release();
			}
		});

		source.setPipeline(capturePipeline);
		assertTrue(validatePipelineOrder(source, source, capturePipeline));

		// 1回だけキャプチャ要求
		capturePipeline.trigger();
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			source.release();
//			dump(result);
			assertEquals(1, cnt.get());
			final Bitmap b = result.get();
			assertNotNull(b);
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * CapturePipelineで1回ビットマップを取得するテスト
	 * VideoSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				Surface → VideoSourcePipeline
	 * 							 → CapturePipeline → Bitmap
	 */
	@Test
	public void capturePipelineOneshotOESTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "videoSourcePipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "videoSourcePipelineTest#onDestroy:");
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

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() == 1) {
					result.set(Bitmap.createBitmap(bitmap));
					sem.release();
				}
			}
			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				result.set(null);
				sem.release();
			}
		});

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		source.setPipeline(capturePipeline);
		assertTrue(validatePipelineOrder(source, source, capturePipeline));

		// 1回だけキャプチャ要求
		capturePipeline.trigger();
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(1, cnt.get());
			final Bitmap b = result.get();
			assertNotNull(b);
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * CapturePipelineで複数回ビットマップを取得するテスト
	 * Bitmap → ImageSourcePipeline → CapturePipeline → Bitmap
	 */
	@Test
	public void capturePipelineMultipleTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Semaphore sem = new Semaphore(0);
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() == NUM_FRAMES) {
					result.set(Bitmap.createBitmap(bitmap));
					sem.release();
				}
			}

			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				result.set(null);
				sem.release();
			}
		});


		source.setPipeline(capturePipeline);
		assertTrue(validatePipelineOrder(source, source, capturePipeline));

		// 100ミリ秒間隔でNUM_TRIGGERS(=9)回キャプチャ要求
		capturePipeline.trigger(NUM_FRAMES, 100);
		try {
			// 9回x100ミリ秒なので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
			assertNotNull(b);
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * CapturePipelineで1回ビットマップを取得するテスト
	 * VideoSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				Surface → VideoSourcePipeline
	 * 							 → CapturePipeline → Bitmap
	 */
	@Test
	public void capturePipelineMultipleOESTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "videoSourcePipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "videoSourcePipelineTest#onDestroy:");
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

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() == NUM_FRAMES) {
					result.set(Bitmap.createBitmap(bitmap));
					sem.release();
				}
			}
			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				result.set(null);
				sem.release();
			}
		});

		// 実際の映像はSurfaceを経由して映像を書き込む
		// 100ミリ秒間隔でNUM_FRAMES枚キャプチャなので余分に書き込まないといけない
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES * 5, requestStop);

		source.setPipeline(capturePipeline);
		assertTrue(validatePipelineOrder(source, source, capturePipeline));

		// 100ミリ秒間隔でNUM_TRIGGERS(=9)回キャプチャ要求
		capturePipeline.trigger(NUM_FRAMES, 100);
		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 500L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
			assertNotNull(b);
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}
}
