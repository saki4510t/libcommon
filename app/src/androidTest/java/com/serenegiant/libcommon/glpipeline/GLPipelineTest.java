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

import com.serenegiant.glpipeline.DistributePipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.SurfaceRendererPipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
import com.serenegiant.gl.GLManager;
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
public class GLPipelineTest {
	private static final String TAG = GLPipelineTest.class.getSimpleName();

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
	 * DistributePipelineで複数のGLPipelineへの分配処理が動作するかどうかを検証
	 * Bitmap → ImageSourcePipeline
	 * 				→ DistributePipeline
	 * 					↓
	 * 					→ ProxyPipeline → GLSurface.wrap → glReadPixels → Bitmap
	 * 					→ ProxyPipeline → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void distributePipelineTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final DistributePipeline distributor = new DistributePipeline();
		source.setPipeline(distributor);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLPipeline pipeline1 = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		distributor.addPipeline(pipeline1);

		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLPipeline pipeline2 = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		distributor.addPipeline(pipeline2);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt1.get());
			assertEquals(NUM_FRAMES, cnt2.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap1 = result1.get();
//			dump(resultBitmap1);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap1, true));

			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap2 = result2.get();
//			dump(resultBitmap2);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap2, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * ImageSourcePipelineからの映像をSurfaceRendererPipelineでSurfaceへ転送
	 * Surfaceの映像をカメラ等のからの映像とみなしてVideoSourcePipelineで映像ソースとして
	 * 供給できるかどうかを検証
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

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final SurfaceRendererPipeline renderer = new SurfaceRendererPipeline(manager);
		// OpenGLの描画を経由するとビットマップが上下反転してしまうのであらかじめ上下判定設定を適用
		GLPipeline.append(source, renderer);

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

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
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
		}
	}

}
