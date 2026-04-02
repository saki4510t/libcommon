package com.serenegiant.libcommon.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceRendererPipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.GLSurfaceReceiver;
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
public class SurfaceRendererPipelineTest {
	private static final String TAG = SurfaceRendererPipelineTest.class.getSimpleName();

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
	 * ImageSourcePipelineからの映像ソースをImageSourcePipelineでSurfaceへ
	 * 描画してGLSurfaceReceiverで読み取れることを検証
	 * ImageSourcePipelineからなのでSurfaceRendererPipelineへの映像ソースはGL_TEXTURE_2Dテクスチャ
	 * Bitmap -> ImageSourcePipeline
	 * 		→ SurfaceRendererPipeline
	 * 			↓
	 * 			Surface → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceRendererPipelineTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final SurfaceRendererPipeline renderer = new SurfaceRendererPipeline(manager);
		GLPipeline.append(source, renderer);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		renderer.setSurface(surface);
		assertTrue(validatePipelineOrder(source, source, renderer));

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
			fail();
		} finally {
			renderer.release();
		}
	}

	/**
	 * SurfaceSourcePipelineからの映像ソースをImageSourcePipelineでSurfaceへ
	 * 描画してGLSurfaceReceiverで読み取れることを検証
	 * SurfaceSourcePipelineからなのSurfaceRendererPipelineへの映像ソースはGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync → (Surface) → SurfaceSourcePipeline
	 *		→ SurfaceRendererPipeline
	 * 			↓
	 * 			Surface → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void surfaceRendererPipelineOESTest() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		final Semaphore sourceSem = new Semaphore(0);
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "CapturePipelineTest#onCreate:" + surface);
					sourceSem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "CapturePipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sourceSem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 映像を受け取ってBitmapに変換するためのGLSurfaceReceiverを準備
		final Semaphore cntSem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, cntSem, result, cnt);
		assertNotNull(receiver);
		final Surface surface = receiver.getSurface();
		assertNotNull(surface);

		// テストするSurfaceRendererPipelineを生成
		final SurfaceRendererPipeline renderer = new SurfaceRendererPipeline(manager);
		renderer.setSurface(surface);

		// パイプラインを接続
		// 映像ソースがSurfaceSourcePipelineなのでGL_TEXTURE_EXTERNAL_OESテクスチャを受け取る
		source.setPipeline(renderer);
		assertTrue(validatePipelineOrder(source, source, renderer));

		final AtomicBoolean requestStop = new AtomicBoolean();

		// 実際の映像はSurfaceを経由して映像を書き込む
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(cntSem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, b, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
			fail();
		} finally {
			renderer.release();
		}
	}

	/**
	 * ImageSourcePipelineからの映像をSurfaceRendererPipelineでSurfaceへ転送
	 * Surfaceの映像をカメラ等のからの映像とみなしてSurfaceSourcePipelineで映像ソースとして
	 * 供給できるかどうかを検証
	 * ImageSourcePipelineからなのでSurfaceRendererPipelineへの映像ソースはGL_TEXTURE_2Dテクスチャ
	 * Bitmap → ImageSourcePipeline
	 * 		→ SurfaceRendererPipeline
	 * 			↓
	 * 			Surface → SurfaceSourcePipeline
	 * 						→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void imageSourceToSurfaceSourcePipelineTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// パイプラインからテクスチャを受け取ってSurfaceへ描画するSurfaceRendererPipelineを生成して追加
		final SurfaceRendererPipeline renderer = new SurfaceRendererPipeline(manager);
		// Surfaceから映像を受け取ってパイプラインへ流すためのSurfaceSourcePipelineを生成
		// Surfaceが生成されたらSurfaceRendererPipelineの描画先として設定する
		final SurfaceSourcePipeline surfaceSource = new SurfaceSourcePipeline(manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					renderer.setSurface(surface);
				}

				@Override
				public void onDestroy() {
					renderer.setSurface(null);
				}
			});

		// 映像受け取り用のパイプラインを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		GLPipeline.append(source, renderer);
		GLPipeline.append(surfaceSource, proxy);

		// SurfacePipelineとSurfaceSourcePipelineの間はSurfaceを経由したやりとりだけで
		// GLPipelineとして接続しているわけではない
		assertTrue(validatePipelineOrder(source, source, renderer));
		assertTrue(validatePipelineOrder(surfaceSource, surfaceSource, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないときはこっち
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		} finally {
			source.release();
			renderer.release();
		}
	}

}
