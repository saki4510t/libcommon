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

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glpipeline.DrawerPipeline;
import com.serenegiant.glpipeline.GLPipeline;
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

/**
 * 映像ソース                 DrawerPipeline   テクスチャ反転    glCopyTextureToBitmap   結果
 * -------------------------------------------------------------------------------------
 * GL_TEXTURE_2D                0個               関係無          上下反転なし           OK
 * GL_TEXTURE_2D                1個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                2個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                3個               あり　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      0個               関係無          上下反転　　           OK
 * GL_TEXTURE_EXTERNAL_OES      1個               なし　          上下反転　　           OK
 * GL_TEXTURE_EXTERNAL_OES      2個               なし　          上下反転　　           OK
 * GL_TEXTURE_EXTERNAL_OES      3個               なし　          上下反転　　           OK
 * -------------------------------------------------------------------------------------
 * glCopyTextureToBitmapで常に上下反転なし(テクスチャ変換行列は適用)
 * GL_TEXTURE_2D                0個               関係無          上下反転なし           OK
 * GL_TEXTURE_2D                1個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                2個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                3個               あり　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      0個               関係無          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      1個               なし　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      2個               なし　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      3個               なし　          上下反転なし           OK
 * -------------------------------------------------------------------------------------
 * glCopyTextureToBitmapでテクスチャ変換行列+常に上下反転
 * GL_TEXTURE_2D                0個               関係無          上下反転　　           NG*
 * GL_TEXTURE_2D                1個               あり　          上下反転　　           NG*
 * GL_TEXTURE_2D                2個               あり　          上下反転　　           NG*
 * GL_TEXTURE_2D                3個               あり　          上下反転　　           NG*
 * GL_TEXTURE_EXTERNAL_OES      0個               関係無          上下反転　　           OK
 * GL_TEXTURE_EXTERNAL_OES      1個               なし　          上下反転　　           NG*
 * GL_TEXTURE_EXTERNAL_OES      2個               なし　          上下反転　　           NG*
 * GL_TEXTURE_EXTERNAL_OES      3個               なし　          上下反転　　           NG*
 * -------------------------------------------------------------------------------------
 * DrawerPipelineで常に上下反転なし, glCopyTextureToBitmapで常に上下反転なし(テクスチャ変換行列は適用)
 * GL_TEXTURE_2D                0個               関係無          上下反転なし           OK
 * GL_TEXTURE_2D                1個               なし　          上下反転なし           NG*
 * GL_TEXTURE_2D                2個               なし　          上下反転なし           OK
 * GL_TEXTURE_2D                3個               なし　          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      0個               関係無          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      1個               なし　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      2個               なし　          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      3個               なし　          上下反転なし           OK
 * -------------------------------------------------------------------------------------
 * DrawerPipelineで常に上下反転あり, glCopyTextureToBitmapで常に上下反転なし(テクスチャ変換行列は適用)
 * GL_TEXTURE_2D                0個               関係無          上下反転なし           OK
 * GL_TEXTURE_2D                1個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                2個               あり　          上下反転なし           OK
 * GL_TEXTURE_2D                3個               あり　          上下反転なし           OK
 * GL_TEXTURE_EXTERNAL_OES      0個               関係無          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      1個               あり　          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      2個               あり　          上下反転なし           NG*
 * GL_TEXTURE_EXTERNAL_OES      3個               あり　          上下反転なし           NG*
 */
@RunWith(AndroidJUnit4.class)
public class DrawerPipelineTest {
	private static final String TAG = DrawerPipelineTest.class.getSimpleName();

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
	 * これはDrawerPipelineを含んでないけどDrawerPipelineが0, 1, 2, 3個の場合を比較するため
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * ImageSourcePipeline → ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineTest0() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * ImageSourcePipeline - DrawerPipeline → ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			drawerPipeline.release();
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * ImageSourcePipeline - DrawerPipeline → DrawerPipeline → ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			drawerPipeline1.release();
			drawerPipeline2.release();
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * ImageSourcePipeline - DrawerPipeline → DrawerPipeline → DrawerPipeline → ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline3 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, drawerPipeline3);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, drawerPipeline3, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			drawerPipeline1.release();
			drawerPipeline2.release();
			drawerPipeline3.release();
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * これはDrawerPipelineを含んでないけどDrawerPipelineが0, 1, 2, 3個の場合を比較するため
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineOESTest0() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
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

		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → DrawerPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineOESTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
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

		// 検証するDrawerPipelineを生成
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → DrawerPipeline → DrawerPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineOESTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
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

		// 検証するDrawerPipelineを生成
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → DrawerPipeline → DrawerPipeline → DrawerPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineOESTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
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

		// 検証するDrawerPipelineを生成
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);
		final DrawerPipeline drawerPipeline3 = new DrawerPipeline(manager, GLDrawer2D.DEFAULT_FACTORY);

		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, drawerPipeline3);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, drawerPipeline3, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertTrue(bitmapEquals(original, resultBitmap, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

}
