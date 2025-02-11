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
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glpipeline.DrawerPipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.ProxyPipeline;
import com.serenegiant.graphics.BitmapHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;
import static com.serenegiant.libcommon.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class DrawerPipelineTest {
	private static final String TAG = DrawerPipelineTest.class.getSimpleName();

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
	 * DrawerPipelineが動作するかどうかを検証
	 * ImageSourcePipeline - DrawerPipeline → ProxyPipeline → テクスチャ読み取り
	 */
	@Test
	public void drawerPipelineTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final ByteBuffer buffer = allocateBuffer(WIDTH, HEIGHT);
		final ProxyPipeline proxy = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == 30) {
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		GLPipeline.append(source, drawerPipeline);
		GLPipeline.append(source, proxy);

		assertTrue(validatePipelineOrder(source, source, drawerPipeline, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			source.release();
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(result);
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
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
		final ByteBuffer buffer = allocateBuffer(WIDTH, HEIGHT);
		final ProxyPipeline proxy = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == 30) {
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, proxy);

		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			source.release();
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(result);
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * DrawerPipelineが動作するかどうかを検証
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
		final ByteBuffer buffer = allocateBuffer(WIDTH, HEIGHT);
		final ProxyPipeline proxy = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == 30) {
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		final DrawerPipeline drawerPipeline1 = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		final DrawerPipeline drawerPipeline2 = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		final DrawerPipeline drawerPipeline3 = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);
		GLPipeline.append(source, drawerPipeline1);
		GLPipeline.append(source, drawerPipeline2);
		GLPipeline.append(source, drawerPipeline3);
		GLPipeline.append(source, proxy);

		assertTrue(validatePipelineOrder(source, source, drawerPipeline1, drawerPipeline2, drawerPipeline3, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			source.release();
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(result);
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}
}
