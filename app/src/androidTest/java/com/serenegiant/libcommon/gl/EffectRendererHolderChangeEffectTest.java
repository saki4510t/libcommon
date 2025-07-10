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

import com.serenegiant.gl.GLEffect;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.EffectRendererHolder;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.StaticTextureSource;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.ThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.createGLSurfaceReceiver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 映像効果の切り替え処理が正常に実行できるかどうかのテスト
 * XXX 映像効果付与のシェーダーが正常にコンパイル＆描画できるかどうかのテストだけで
 *     実際に適用された映像効果の内容についてはテストできない
 */
@RunWith(AndroidJUnit4.class)
public class EffectRendererHolderChangeEffectTest {

	private static final String TAG = EffectRendererHolderChangeEffectTest.class.getSimpleName();

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
	 * 途中で映像ソースと映像効果を変更する
	 * XXX 映像効果付与のシェーダーが正常にコンパイル＆描画できるかどうかのテストだけで
	 *     実際に適用された映像効果の内容についてはテストできない
	 * Bitmap → ImageTextureSource → (Surface)
	 * 		→ EffectRendererHolder
	 * 			↓
	 * 			→ (Surface) → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void staticTextureSourceChangeTest() {
		final GLManager manager = mGLManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLSurfaceReceiver receiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);
		assertNotNull(receiver);
		final Surface readerSurface = receiver.getSurface();
		assertNotNull(readerSurface);

		// テストするEffectRendererHolderを生成
		final EffectRendererHolder rendererHolder = new EffectRendererHolder(WIDTH, HEIGHT, null);
		for (int i = GLEffect.EFFECT_NON; i < GLEffect.EFFECT_NUM; i++) {
			cnt.set(0);
			result.set(null);
			rendererHolder.setEffect(i);
			final Bitmap original = BitmapHelper.makeCheckBitmap(
				WIDTH, HEIGHT, 15 + i, 12, Bitmap.Config.ARGB_8888);
//			dump(bitmap);
			// 映像ソースとしてStaticTextureSourceを生成
			final StaticTextureSource source = new StaticTextureSource(manager, original, new Fraction(30));
			final Surface surface = rendererHolder.getSurface();
			assertNotNull(surface);
			// StaticTextureSource →　EffectRendererHolder　→ SurfaceReaderと繋ぐ
			source.addSurface(surface.hashCode(), surface, false);
			rendererHolder.addSurface(readerSurface.hashCode(), readerSurface, false);

			try {
				// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
				assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
				assertEquals(NUM_FRAMES, cnt.get());
				final Bitmap b = result.get();
	//			dump(b);
				assertNotNull(b);
				// EFFECT_NON以外は元のビットマップとは一致しないのでチェックしない
			} catch (final InterruptedException e) {
				Log.d(TAG, "interrupted", e);
			}
			source.removeSurface(surface.hashCode());
			rendererHolder.removeSurface(readerSurface.hashCode());
			ThreadUtils.NoThrowSleep(100L);
		}
	}

}
