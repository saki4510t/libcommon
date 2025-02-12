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
import android.view.Surface;

import com.serenegiant.glpipeline.DistributePipeline;
import com.serenegiant.glpipeline.EffectPipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.ProxyPipeline;
import com.serenegiant.glpipeline.SurfaceRendererPipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
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
public class GLPipelineTest {
	private static final String TAG = GLPipelineTest.class.getSimpleName();

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

		final Semaphore sem1 = new Semaphore(0);
		final ByteBuffer buffer1 = allocateBuffer(WIDTH, HEIGHT);
		final ProxyPipeline pipeline1 = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == 30) {
					this.remove();
					// GLSurfaceを経由してテクスチャを読み取る
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer1, WIDTH, HEIGHT);
					sem1.release();
				}
			}
		};
		distributor.addPipeline(pipeline1);

		final Semaphore sem2 = new Semaphore(0);
		final ByteBuffer buffer2 = allocateBuffer(WIDTH, HEIGHT);
		final ProxyPipeline pipeline2 = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == 30) {
					this.remove();
					// GLSurfaceを経由してテクスチャを読み取る
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer2, WIDTH, HEIGHT);
					sem2.release();
				}
			}
		};
		distributor.addPipeline(pipeline2);

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem1.tryAcquire(1200, TimeUnit.MILLISECONDS));
			assertTrue(sem2.tryAcquire(1200, TimeUnit.MILLISECONDS));
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result1 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result1.copyPixelsFromBuffer(buffer1);
//			dump(result1);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, result1, true));

			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result2 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result2.copyPixelsFromBuffer(buffer2);
//			dump(result2);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, result2, true));
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
					source.setPipeline(null);
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはSurfaceSourcePipelineからのテクスチャなのでisOES=trueのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};
		GLPipeline.append(surfaceSource, proxy);

		// SurfacePipelineとSurfaceSourcePipelineの間はSurfaceを経由したやりとりだけで
		// GLPipelineとして接続しているわけではない
		assertTrue(validatePipelineOrder(source, source, renderer));
		assertTrue(validatePipelineOrder(surfaceSource, surfaceSource, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(result);
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないときはこっち
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline = new EffectPipeline(manager);

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
					source.setPipeline(null);
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		source.setPipeline(effectPipeline);
		effectPipeline.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
			assertTrue(bitmapEquals(original, result, true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineを2つ連結処理したときに想定通りに動作になるかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ EffectPipeline
	 * 					→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);

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
					source.setPipeline(null);
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		source.setPipeline(effectPipeline1);
		effectPipeline1.setPipeline(effectPipeline2);
		effectPipeline2.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline1, effectPipeline2, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
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
	 * EffectPipelineを3つ連結処理したときに想定通りに動作になるかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ EffectPipeline
	 * 				→ EffectPipeline
	 * 					→ EffectPipeline
	 * 						→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void effectPipelineTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline3 = new EffectPipeline(manager);

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
					source.setPipeline(null);
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, WIDTH, HEIGHT);
					sem.release();
				}
			}
		};

		source.setPipeline(effectPipeline1);
		effectPipeline1.setPipeline(effectPipeline2);
		effectPipeline2.setPipeline(effectPipeline3);
		effectPipeline3.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline1, effectPipeline2, effectPipeline3, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
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
