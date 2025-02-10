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
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.OnFramePipeline;
import com.serenegiant.glpipeline.ProxyPipeline;
import com.serenegiant.glutils.GLBitmapImageReader;
import com.serenegiant.glutils.ImageReader;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.HandlerThreadHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.bitmapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ImageSourcePipelineの映像供給(フレームコールバック呼び出し)の実体はImageTextureSourceなので
 * 供給する映像自体は問題ないはず(ImageTextureSourceTestで検証済み)
 */
@RunWith(AndroidJUnit4.class)
public class ImageSourcePipelineTest {
	private static final String TAG = ImageSourcePipelineTest.class.getSimpleName();

	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;
	private static final int MAX_FRAMES = 2;

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
	 * ImageSourceパイプラインが正常に映像ソースとして動作するかどうかを検証
	 * こっちはOnFramePipelineを経由してGLBitmapImageReaderで映像を受け取る
	 */
	@Test
	public void imageSourceTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final GLBitmapImageReader reader
			= new GLBitmapImageReader(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888, MAX_FRAMES);
		reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener<Bitmap>() {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onImageAvailable(@NonNull final ImageReader<Bitmap> reader) {
				final Bitmap bitmap = reader.acquireLatestImage();
				if (bitmap != null) {
					try {
						if (cnt.incrementAndGet() >= MAX_FRAMES) {
							if (sem.availablePermits() == 0) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler(TAG));

		final OnFramePipeline pipeline = new OnFramePipeline(reader);
		source.setPipeline(pipeline);

		try {
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
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
	 * ImageSourceパイプラインが正常に映像ソースとして動作するかどうかを検証
	 * こっちはProxyPipelineを経由して映像を受け取る
	 */
	@Test
	public void imageSourceTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		final GLManager manager = mManager;
		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final Semaphore sem = new Semaphore(0);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4).order(ByteOrder.LITTLE_ENDIAN);
		final ProxyPipeline proxy = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() >= 30) {
					source.setPipeline(null);
					if (sem.availablePermits() == 0) {
						// GLSurfaceを経由してテクスチャを読み取る
						final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
							isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
							GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
						surface.makeCurrent();
						final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
						sem.release();
					}
				}
			}
		};
		source.setPipeline(proxy);
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

}
