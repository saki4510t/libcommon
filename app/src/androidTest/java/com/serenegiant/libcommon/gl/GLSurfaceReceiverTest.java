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
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceReceiver;
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

import androidx.annotation.Nullable;
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
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLSurfaceReceiverで
	 * 読み取れることを検証
	 * Bitmap -> inputImagesAsync
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceReaderTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mGLManager;
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			fail();
		}
	}

	/**
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLSurfaceReceiverで
	 * 読み取れることを検証
	 * 途中で映像ソースを切り替える場合
	 * Surfaceを再生成しない場合は1回目はOKだけど映像ソースを切り替えるとビットマップが一致しない、1回目のビットマップが返ってくる
	 * GLSurfaceReceiver#reCreateInputSurfaceでSurfaceTextureとSurfaceを再生成するとOK
	 * Bitmap -> inputImagesAsync
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceReaderChangeSourceTest1() {
		final GLManager manager = mGLManager;
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);

		for (int i = 0; i < 3; i++) {
			Log.v(TAG, "surfaceReaderChangeSourceTest1:" + i);
			cnt.set(0);
			result.set(null);
			final Surface surface = receiver.getSurface();
			assertNotNull(surface);
			final Bitmap original = BitmapHelper.makeGradientBitmap(
				WIDTH, HEIGHT, 0xff000000 + (0x003f0000) * i, 0xffffffff, Bitmap.Config.ARGB_8888);
//			dump(original);

			final AtomicBoolean requestStop = new AtomicBoolean();
			inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

			try {
				assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				requestStop.set(true);
				assertEquals(NUM_FRAMES, cnt.get());
				final Bitmap resultBitmap = result.get();
				assertNotNull(resultBitmap);
				assertFalse(resultBitmap.isRecycled());
				// 元のビットマップと同じかどうかを検証
				assertTrue(bitmapEquals(original, resultBitmap, true));
				resultBitmap.recycle();
				// 映像入力用SurfaceTexture/Surfaceを強制的に再生成させる
				receiver.reCreateInputSurface();
			} catch (final InterruptedException e) {
				fail();
			}
			ThreadUtils.NoThrowSleep(100L);
		}

		receiver.release();
	}

	/**
	 * inputImagesAsyncでCanvasを経由してSurfaceへ書き込んだBitmapをGLSurfaceReceiverで
	 * 読み取れることを検証
	 * 途中で映像ソースを切り替える場合
	 * Surfaceを再生成しない場合は1回目はOKだけど映像ソースを切り替えるとビットマップが一致しない、1回目のビットマップが返ってくる
	 * 一度破棄してSurfaceTextureからSurfaceを再生成するとOK
	 * Bitmap -> inputImagesAsync
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceReaderChangeSourceTest2() {
		final GLManager manager = mGLManager;
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);

		for (int i = 0; i < 3; i++) {
			Log.v(TAG, "surfaceReaderChangeSourceTest2:" + i);
			cnt.set(0);
			result.set(null);
			// 映像受け取り用SurfaceTextureを取得する
			final SurfaceTexture st = receiver.getSurfaceTexture();
			assertNotNull(st);
			final Surface surface = new Surface(st);
			assertNotNull(surface);
			try {
				final Bitmap original = BitmapHelper.makeGradientBitmap(
					WIDTH, HEIGHT, 0xff000000 + (0x003f0000) * i, 0xffffffff, Bitmap.Config.ARGB_8888);
//				dump(original);

				final AtomicBoolean requestStop = new AtomicBoolean();
				inputImagesAsync(original, surface, NUM_FRAMES, requestStop);

				assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				requestStop.set(true);
				assertEquals(NUM_FRAMES, cnt.get());
				final Bitmap resultBitmap = result.get();
				assertNotNull(resultBitmap);
				assertFalse(resultBitmap.isRecycled());
				// 元のビットマップと同じかどうかを検証
				assertTrue(bitmapEquals(original, resultBitmap, true));
				resultBitmap.recycle();
			} catch (final InterruptedException e) {
				fail();
			} finally {
				// ここでSurfaceを破棄しないと2回目以降Canvas経由で画像を書き込もうとしたときにalready connectedの例外生成する
				surface.release();
			}
			ThreadUtils.NoThrowSleep(100L);
		}

		receiver.release();
	}

}
