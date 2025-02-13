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
import com.serenegiant.glutils.SurfaceProxy;
import com.serenegiant.graphics.BitmapHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SurfaceProxyTest {
	private static final String TAG = SurfaceProxyTest.class.getSimpleName();

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

	/**
	 * SurfaceProxyReaderWriter => GLSurfaceReceiverと接続して
	 * SurfaceProxyReaderWriterから取得したSurfaceへCanvas#drawBitmapでビットマップを
	 * 書き込んで、書き込んだビットマップとで読み込んだビットマップが一致するかどうかを検証する
	 */
	@Test
	public void surfaceProxyReaderWriterTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// SurfaceProxyReaderWriterを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, true);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyReaderWriter);
		final Surface inputSurface = proxy.getInputSurface();
		assertNotNull(inputSurface);

		// 映像受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface receiverSurface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, MAX_IMAGES, sem, result, cnt);
		assertNotNull(receiverSurface);
		// SurfaceProxyReaderWriterへ映像読み取り用Surfaceをセット
		proxy.setSurface(receiverSurface);
		// 初段のSurfaceProxyReaderWriterの入力用Surfaceへ静止画を描き込む
		inputImagesAsync(original, inputSurface, MAX_IMAGES);

		try {
			assertTrue(sem.tryAcquire(MAX_IMAGES * 50, TimeUnit.MILLISECONDS));
			assertEquals(MAX_IMAGES, cnt.get());
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
	 * SurfaceProxyGLES => GLSurfaceReceiver => GLBitmapImageReaderと接続して
	 * SurfaceProxyGLESから取得したSurfaceへCanvas#drawBitmapでビットマップを
	 * 書き込んで、書き込んだビットマップとGLBitmapImageReaderで読み込んだ
	 * ビットマップが一致するかどうかを検証する
	 */
	@Test
	public void surfaceProxyGLESTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// SurfaceProxyGLESを生成
		final SurfaceProxy proxy = SurfaceProxy.newInstance(WIDTH, HEIGHT, false);
		assertTrue(proxy instanceof SurfaceProxy.SurfaceProxyGLES);
		final Surface inputSurface = proxy.getInputSurface();	// このSurfaceはSurfaceTexture由来なのでOESテクスチャ
		assertNotNull(inputSurface);

		// 映像受け取り用にGLSurfaceReceiverを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final Surface receiverSurface = createGLSurfaceReceiverSurface(
			new GLManager(), WIDTH, HEIGHT, MAX_IMAGES, sem, result, cnt);
		assertNotNull(receiverSurface);
		// プロキシに映像読み取り用Surfaceをセット
		proxy.setSurface(receiverSurface);
		// 初段のSurfaceProxyGLESの入力用Surfaceへ静止画を描き込む
		inputImagesAsync(original, inputSurface, MAX_IMAGES);

		try {
			assertTrue(sem.tryAcquire(MAX_IMAGES * 50, TimeUnit.MILLISECONDS));
			assertEquals(MAX_IMAGES, cnt.get());
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
