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

import android.graphics.Bitmap;
import android.media.effect.EffectContext;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.mediaeffect.BitmapEffector;
import com.serenegiant.mediaeffect.EffectsBuilder;
import com.serenegiant.mediaeffect.IMediaEffect;
import com.serenegiant.mediaeffect.MediaEffectAutoFix;
import com.serenegiant.mediaeffect.MediaEffectGLNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.bitmapEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BitmapEffectorTest {
	private static final String TAG = BitmapEffectorTest.class.getSimpleName();

	private static final int WIDTH = 200;
	private static final int HEIGHT = 200;

	/**
	 * 映像効果無しの場合に一致するかどうかをテスト
	 * EffectBuilderを未指定(空リスト)
	 */
	@Test
	public void bitmapEffectorTest1() {
		// b0とb1は同じ内容のビットマップ, b2は異なる内容のビットマップ
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 18,
			Bitmap.Config.ARGB_8888);

		final BitmapEffector effector = new BitmapEffector();
		try {
			final Bitmap b1 = effector.apply(b0, 1000L);
			effector.release();
			// 映像効果無しなので一致するはず
			assertTrue(bitmapEquals(b0, b1));
		} catch (InterruptedException | TimeoutException e) {
			fail();
		}
	}

	/**
	 * 映像効果無しの場合に一致するかどうかをテスト
	 * MediaEffectAutoFixを指定
	 */
	@Test
	public void bitmapEffectorTest2() {
		// b0とb1は同じ内容のビットマップ, b2は異なる内容のビットマップ
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 18,
			Bitmap.Config.ARGB_8888);

		final BitmapEffector effector = new BitmapEffector(
			new EffectsBuilder() {
				@NonNull
				@Override
				public List<IMediaEffect> buildEffects(@NonNull final EffectContext effectContext) {
					final List<IMediaEffect> effects = new ArrayList<>();
					// 映像効果無しの映像効果を適用
					effects.add(new MediaEffectAutoFix(effectContext, 0.0f));
					return effects;
				}
			}
		);
		try {
			final Bitmap b1 = effector.apply(b0, 1000L);
			effector.release();
			// 映像効果無しなので一致するはず
			assertTrue(bitmapEquals(b0, b1));
		} catch (InterruptedException | TimeoutException e) {
			fail();
		}
	}

	/**
	 * 映像効果無しの場合に一致するかどうかをテスト
	 * MediaEffectGLNullを指定
	 */
	@Test
	public void bitmapEffectorTest3() {
		// b0とb1は同じ内容のビットマップ, b2は異なる内容のビットマップ
		final Bitmap b0 = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 18,
			Bitmap.Config.ARGB_8888);

		final BitmapEffector effector = new BitmapEffector(
			new EffectsBuilder() {
				@NonNull
				@Override
				public List<IMediaEffect> buildEffects(@NonNull final EffectContext effectContext) {
					final List<IMediaEffect> effects = new ArrayList<>();
					// 映像効果無しの映像効果を適用
					effects.add(new MediaEffectGLNull());
					return effects;
				}
			}
		);
		try {
			final Bitmap b1 = effector.apply(b0, 1000L);
			effector.release();
			// 映像効果無しなので一致するはず
			assertTrue(bitmapEquals(b0, b1));
		} catch (InterruptedException | TimeoutException e) {
			fail();
		}
	}
}
