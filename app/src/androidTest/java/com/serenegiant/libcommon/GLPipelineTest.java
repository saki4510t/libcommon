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

import com.serenegiant.glpipeline.CapturePipeline;
import com.serenegiant.glpipeline.DistributePipeline;
import com.serenegiant.glpipeline.DrawerPipeline;
import com.serenegiant.glpipeline.EffectPipeline;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.ProxyPipeline;
import com.serenegiant.glpipeline.SurfaceRendererPipeline;
import com.serenegiant.glpipeline.VideoSourcePipeline;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;

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
	 * パイプラインの接続・切断・検索が正常に動作するかどうかを確認
	 */
	@Test
	public void insertRemoveAppendFindTest() {
		final ProxyPipeline src = new ProxyPipeline();
		final ProxyPipeline dst1 = new ProxyPipeline();
		final ProxyPipeline dst2 = new ProxyPipeline();
		final ProxyPipeline dst3 = new ProxyPipeline();

		assertTrue(validatePipelineOrder(src, src));
		// パイプラインがきちんと伝播して呼び出されるかどうかを確認
		src.setPipeline(dst1);
		assertTrue(validatePipelineOrder(src, src, dst1));

		dst1.setPipeline(dst2);
		assertTrue(validatePipelineOrder(src, src, dst1, dst2));

		dst2.setPipeline(dst3);
		assertTrue(validatePipelineOrder(src, src, dst1, dst2, dst3));

		// 中間のパイプラインを除去
		dst2.remove();
		assertTrue(validatePipelineOrder(src, src, dst1, dst3));

		// 一番後ろのパイプライを除去
		dst3.remove();
		assertTrue(validatePipelineOrder(src, src, dst1));

		// src直後にdst2を挿入
		GLPipeline.insert(src, dst2);
		assertTrue(validatePipelineOrder(src, src, dst2, dst1));

		// 一番後ろに追加
		GLPipeline.append(src, dst3);
		assertTrue(validatePipelineOrder(src, src, dst2, dst1, dst3));

		// GLPipeline#findFirst
		assertEquals(src, GLPipeline.findFirst(dst3));

		// GLPipeline#findLast
		assertEquals(dst3, GLPipeline.findLast(src));

		// GLPipeline#find
		assertEquals(src, GLPipeline.find(dst3, ProxyPipeline.class));

		// 全てを除去
		dst1.remove(); dst2.remove(); dst3.remove();
		// あらかじめ繋いだパイプラインを一括で追加
		dst1.setPipeline(dst2); dst2.setPipeline(dst3);
		src.setPipeline(dst1);
		assertTrue(validatePipelineOrder(src, src, dst1, dst2, dst3));
		// 全てを除去
		dst1.remove(); dst2.remove(); dst3.remove();
		// あらかじめ繋いだパイプラインを一括でinsert
		src.setPipeline(dst1);
		dst2.setPipeline(dst3);
		GLPipeline.insert(src, dst2);
		assertTrue(validatePipelineOrder(src, src, dst2, dst3, dst1));
	}

	/**
	 * 正常にパイプラインに伝播して呼び出されるかどうかを確認
	 */
	@Test
	public void proxyPipelineTest() {
		final ProxyPipeline src = new ProxyPipeline();
		final AtomicInteger cnt1 = new AtomicInteger();
		final ProxyPipeline dst1 = new ProxyPipeline() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				cnt1.incrementAndGet();
			}
		};
		final AtomicInteger cnt2 = new AtomicInteger();
		final ProxyPipeline dst2 = new ProxyPipeline() {
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				cnt2.incrementAndGet();
			}
		};
		final AtomicInteger cnt3 = new AtomicInteger();
		final ProxyPipeline dst3 = new ProxyPipeline() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				cnt3.incrementAndGet();
			}
		};

		// パイプラインがきちんと伝播して呼び出されるかどうかを確認
		src.setPipeline(dst1);
		final int LOOP_NUM = 10;
		final float[] texMatrix = new float[16];
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM, cnt1.get());
		assertEquals(0, cnt2.get());
		assertEquals(0, cnt3.get());

		dst1.setPipeline(dst2);
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 2, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(0, cnt3.get());

		dst2.setPipeline(dst3);
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 3, cnt1.get());
		assertEquals(LOOP_NUM * 2, cnt2.get());
		assertEquals(LOOP_NUM, cnt3.get());

		// カウンタをリセット
		cnt1.set(0); cnt2.set(0); cnt3.set(0);

		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM, cnt3.get());

		// 中間のパイプラインを除去したときに正常に動作するかどうかを確認
		dst2.remove();
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 2, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM * 2, cnt3.get());

		// 一番後ろのパイプライを除去したときに正常に動作するかどうかを確認
		dst3.remove();
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 3, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM * 2, cnt3.get());
	}

	/**
	 * ImageSourceパイプラインが正常に映像ソースとして動作するかどうかを検証
	 */
	@Test
	public void imageSourceTest() {
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

	/**
	 * DistributePipelineで複数のGLPipelineへの分配処理が動作するかどうかを検証
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
		final ByteBuffer buffer1 = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4).order(ByteOrder.LITTLE_ENDIAN);
		final ProxyPipeline pipeline1 = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() >= 30) {
					this.remove();
					if (sem1.availablePermits() == 0) {
						// GLSurfaceを経由してテクスチャを読み取る
						final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
							isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
							GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
						surface.makeCurrent();
						final ByteBuffer buf = GLUtils.glReadPixels(buffer1, WIDTH, HEIGHT);
						sem1.release();
					}
				}
			}
		};
		distributor.addPipeline(pipeline1);

		final Semaphore sem2 = new Semaphore(0);
		final ByteBuffer buffer2 = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4).order(ByteOrder.LITTLE_ENDIAN);
		final ProxyPipeline pipeline2 = new ProxyPipeline(WIDTH, HEIGHT) {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() >= 30) {
					this.remove();
					if (sem2.availablePermits() == 0) {
						// GLSurfaceを経由してテクスチャを読み取る
						final GLSurface surface = GLSurface.wrap(manager.isGLES3(),
							isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
							GLES20.GL_TEXTURE4, texId, WIDTH, HEIGHT, false);
						surface.makeCurrent();
						final ByteBuffer buf = GLUtils.glReadPixels(buffer2, WIDTH, HEIGHT);
						sem2.release();
					}
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
	 * ImageSourceからの映像をSurfacePipelineでSurfaceへ転送
	 * SurfacePipelineをカメラ等の映像限とみなしてVideoSourceで映像ソースとして
	 * 供給できるかどうかを検証
	 */
	@Test
	public void surfacePipelineVideoSourcePipelineTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline - SurfaceRendererPipeline → (Surface) → VideoSourcePipeline - ProxyPipeline → テクスチャ読み取り

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final SurfaceRendererPipeline surfacePipeline = new SurfaceRendererPipeline(manager);
		// OpenGLの描画を経由するとビットマップが上下反転してしまうのであらかじめ上下判定設定を適用
		surfacePipeline.setMirror(IMirror.MIRROR_VERTICAL);
		source.setPipeline(surfacePipeline);

		final VideoSourcePipeline videoSourcePipeline = new VideoSourcePipeline(manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					surfacePipeline.setSurface(surface);
				}

				@Override
				public void onDestroy() {
					surfacePipeline.setSurface(null);
				}
			});

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
						// ここに来るのはVideoSourceからのテクスチャなのでisOES=trueのはず
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
		videoSourcePipeline.setPipeline(proxy);

		// SurfacePipelineとVideoSourceの間はSurfaceを経由したやりとりだけでGLPipelineとして接続しているわけではない
		assertTrue(validatePipelineOrder(source, source, surfacePipeline));
		assertTrue(validatePipelineOrder(videoSourcePipeline, videoSourcePipeline, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap result = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			result.copyPixelsFromBuffer(buffer);
//			dump(result);
			// GLDrawer2Dのテクスチャ座標配列で上下反転させないときはこっち
			assertTrue(bitmapEquals(original, flipVertical(result), true));
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * EffectPipelineが動作するかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * FIXME これはいつも失敗する(生成されるビットマップが一致しない)
	 *       でも#effectPipeline2と#effectPipeline3は成功する
	 */
	@Test
	public void effectPipelineTest1() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline - EffectPipeline → ProxyPipeline → テクスチャ読み取り

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);

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
						// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
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

		source.setPipeline(effectPipeline1);
		effectPipeline1.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, effectPipeline1, proxy));

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
	 * EffectPipelineを複数連結理をしたときに想定通りに動作になるかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 */
	@Test
	public void effectPipelineTest2() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline → EffectPipeline → EffectPipeline → ProxyPipeline → テクスチャ読み取り

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);

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
						// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
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
	 * EffectPipelineを複数連結理をしたときに想定通りに動作になるかどうかを検証
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 */
	@Test
	public void effectPipelineTest3() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline → EffectPipeline → EffectPipeline → EffectPipeline → ProxyPipeline → テクスチャ読み取り

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final EffectPipeline effectPipeline1 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline2 = new EffectPipeline(manager);
		final EffectPipeline effectPipeline3 = new EffectPipeline(manager);

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
						// ここに来るのはEffectPipelineからのテクスチャなのでisOES=falseのはず
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

	/**
	 * DrawerPipelineが動作するかどうかを検証
	 */
	@Test
	public void drawerPipelineTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline - DrawerPipeline → ProxyPipeline → テクスチャ読み取り

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final DrawerPipeline drawerPipeline = new DrawerPipeline(manager, DrawerPipeline.DEFAULT_CALLBACK);

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
						// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
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

		source.setPipeline(drawerPipeline);
		drawerPipeline.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, drawerPipeline, proxy));

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

	@Test
	public void capturePipelineOneshotTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		// ImageSourcePipeline → CapturePipeline

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Semaphore sem = new Semaphore(0);
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				result.set(Bitmap.createBitmap(bitmap));
				cnt.incrementAndGet();
				sem.release();
			}
			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				sem.release();
			}
		});

		source.setPipeline(capturePipeline);

		assertTrue(validatePipelineOrder(source, source, capturePipeline));

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

	@Test
	public void capturePipelineMultipleTest() {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final int NUM_TRIGGERS = 9;
		// ImageSourcePipeline → CapturePipeline

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final Semaphore sem = new Semaphore(0);
		final AtomicInteger cnt = new AtomicInteger();
		final CapturePipeline capturePipeline = new CapturePipeline(new CapturePipeline.Callback() {
			@Override
			public void onCapture(@NonNull final Bitmap bitmap) {
				if (cnt.incrementAndGet() >= NUM_TRIGGERS) {
					result.set(Bitmap.createBitmap(bitmap));
					sem.release();
				}
			}

			@Override
			public void onError(@NonNull final Throwable t) {
				Log.w(TAG, t);
				sem.release();
			}
		});


		source.setPipeline(capturePipeline);

		assertTrue(validatePipelineOrder(source, source, capturePipeline));

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

//--------------------------------------------------------------------------------
	/**
	 * パイプラインチェーン内の順番を検証する
	 * @param head
	 * @param args
	 * @return
	 */
	private boolean validatePipelineOrder(@NonNull final GLPipeline head, @NonNull GLPipeline... args) {
		boolean result = true;
		final int n = args.length;
		int cnt = 0;
		GLPipeline p = GLPipeline.findFirst(head);
		for (int i = 0; i < n; i++) {
			if (p != args[i]) {
				Log.w(TAG, "パイプラインチェーン内の順番が違う");
				result = false;
				break;
			}
			if (++cnt < n) {
				p = p.getPipeline();
			}
		}
		if (p.getPipeline() != null) {
			Log.w(TAG, "パイプラインチェーン内のパイプラインの数が違う");
			result = false;
		}
		return result;
	}

}
