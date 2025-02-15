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
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.SurfaceProxy;
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

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SurfaceProxyTest {
	private static final String TAG = SurfaceProxyTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 200;

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
	 * SurfaceProxyReaderWriterがSurfaceを経由して受け取った映像をGLSurfaceReceiverの
	 * Surfaceを経由してテクスチャスチャとして受け取った映像をBitmapへ変換したときに
	 * 元のビットマップと一致するかどうかを検証する
	 * Bitmap -> inputImagesAsync → (Surface)
	 * 	→ SurfaceProxyReaderWriter
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceProxyReaderWriterTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// SurfaceProxyReaderWriterを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, true);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyReaderWriter);
		final Surface inputSurface = proxy.getInputSurface();
		assertNotNull(inputSurface);

		// 映像受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface receiverSurface = receiver.getSurface();
		assertNotNull(receiverSurface);
		// SurfaceProxyReaderWriterへ映像読み取り用Surfaceをセット
		proxy.setSurface(receiverSurface);
		// 初段のSurfaceProxyReaderWriterの入力用Surfaceへ静止画を描き込む

		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}

	}

	/**
	 * SurfaceProxyGLESがSurfaceを経由して受け取った映像をGLSurfaceReceiverの
	 * Surfaceを経由してテクスチャスチャとして受け取った映像をBitmapへ変換したときに
	 * 元のビットマップと一致するかどうかを検証する
	 * Bitmap -> inputImagesAsync → (Surface)
	 * 	→ SurfaceProxyGLES
	 * 		→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceProxyGLESTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// SurfaceProxyGLESを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, false);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyGLES);
		final Surface inputSurface = proxy.getInputSurface();	// このSurfaceはSurfaceTexture由来なのでOESテクスチャ
		assertNotNull(inputSurface);

		// 映像受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface receiverSurface = receiver.getSurface();
		assertNotNull(receiverSurface);
		// プロキシに映像読み取り用Surfaceをセット
		proxy.setSurface(receiverSurface);
		// 初段のSurfaceProxyGLESの入力用Surfaceへ静止画を描き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

}
