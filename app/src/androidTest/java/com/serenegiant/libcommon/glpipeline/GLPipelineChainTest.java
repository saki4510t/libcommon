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

import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ProxyPipeline;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;
import static com.serenegiant.libcommon.TestUtils.*;

/**
 * パイプラインの接続・切断・検索、およびコールバックチェーンが正常に動作するかどうかを確認
 */
@RunWith(AndroidJUnit4.class)
public class GLPipelineChainTest {
	private static final String TAG = GLPipelineChainTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;

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
				final boolean isOES,
				final int width, final int height,
				final int texId, @NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
				cnt1.incrementAndGet();
			}
		};
		final AtomicInteger cnt2 = new AtomicInteger();
		final ProxyPipeline dst2 = new ProxyPipeline() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES,
				final int width, final int height,
				final int texId, @NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
				cnt2.incrementAndGet();
			}
		};
		final AtomicInteger cnt3 = new AtomicInteger();
		final ProxyPipeline dst3 = new ProxyPipeline() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES,
				final int width, final int height,
				final int texId, @NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
				cnt3.incrementAndGet();
			}
		};

		// パイプラインがきちんと伝播して呼び出されるかどうかを確認
		src.setPipeline(dst1);
		final int LOOP_NUM = 10;
		final float[] texMatrix = new float[16];
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM, cnt1.get());
		assertEquals(0, cnt2.get());
		assertEquals(0, cnt3.get());

		dst1.setPipeline(dst2);
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 2, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(0, cnt3.get());

		dst2.setPipeline(dst3);
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false,  WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 3, cnt1.get());
		assertEquals(LOOP_NUM * 2, cnt2.get());
		assertEquals(LOOP_NUM, cnt3.get());

		// カウンタをリセット
		cnt1.set(0); cnt2.set(0); cnt3.set(0);

		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false,  WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM, cnt3.get());

		// 中間のパイプラインを除去したときに正常に動作するかどうかを確認
		dst2.remove();
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false,  WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 2, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM * 2, cnt3.get());

		// 一番後ろのパイプライを除去したときに正常に動作するかどうかを確認
		dst3.remove();
		for (int i = 0; i < LOOP_NUM; i++) {
			src.onFrameAvailable(false, false, WIDTH, HEIGHT, 0, texMatrix);
		}
		assertEquals(LOOP_NUM * 3, cnt1.get());
		assertEquals(LOOP_NUM, cnt2.get());
		assertEquals(LOOP_NUM * 2, cnt3.get());
	}
}
