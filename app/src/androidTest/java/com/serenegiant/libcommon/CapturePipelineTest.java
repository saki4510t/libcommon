package com.serenegiant.libcommon;
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
import com.serenegiant.glpipeline.CapturePipeline;
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

		final int NUM_TRIGGERS = 9;
		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Semaphore sem = new Semaphore(0);
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() == NUM_TRIGGERS) {
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
		capturePipeline.trigger(NUM_TRIGGERS, 100);
		try {
			// 9回x100ミリ秒なので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
//			dump(result);
			assertEquals(NUM_TRIGGERS, cnt.get());
			final Bitmap b = result.get();
			assertNotNull(b);
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}
}
