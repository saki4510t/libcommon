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
import android.opengl.GLES20;

import com.serenegiant.gl.GLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GLManagerTest {
	private static final String TAG = GLManagerTest.class.getSimpleName();

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

	@Test
	public void glManagerTest1() {
		try {
			final GLManager manager = mManager;
			final Semaphore sem = new Semaphore(0);
			final AtomicBoolean booleanResult = new AtomicBoolean();
			final AtomicInteger intResult = new AtomicInteger();
			manager.runOnGLThread(() -> {
				GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
				intResult.set(GLES20.glGetError());
				booleanResult.set(manager.isGLThread());
				sem.release();
			});
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
			assertTrue(booleanResult.get());
			assertEquals(GLES20.GL_NO_ERROR, intResult.get());

			// 再初期化
			manager.reInitialize(null, 2, 3);
			assertEquals(2, manager.getMasterWidth());
			assertEquals(3, manager.getMasterHeight());

			manager.runOnGLThread(() -> {
				GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
				intResult.set(GLES20.glGetError());
				booleanResult.set(manager.isGLThread());
				sem.release();
			});
			assertTrue(sem.tryAcquire(1000, TimeUnit.MILLISECONDS));
			assertTrue(booleanResult.get());
			assertEquals(GLES20.GL_NO_ERROR, intResult.get());
		} catch (final Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	public void glManagerTest2() {
		final AtomicBoolean booleanResult = new AtomicBoolean();
		runOnGL((manager) -> {
			booleanResult.set(manager.isGLThread());
		}, booleanResult, 100);
		assertTrue(booleanResult.get());

		final AtomicInteger intResult = new AtomicInteger();
		runOnGL((manager) -> {
			GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
			intResult.set(GLES20.glGetError());
		}, intResult, 200);
		assertEquals(GLES20.GL_NO_ERROR, intResult.get());
	}

//--------------------------------------------------------------------------------
	/**
	 * GLコンテキスト上で実行するRunnable類似のインターフェース
	 */
	private interface GLRunnable {
		public void run(@NonNull final GLManager manager);
	}

	/**
	 * GLManager#runOnGLThreadでGLRunnableを実行して
	 * 結果が返ってくるの待機するためのヘルパーメソッド
	 * @param task
	 * @param result
	 * @param maxWaitMs
	 * @param <T>
	 * @return
	 */
	private <T> T runOnGL(
		@NonNull final GLRunnable task,
		@NonNull final T result,
		final long maxWaitMs) {

		final GLManager manager = mManager;
		assertNotNull(manager);
		final Semaphore sem = new Semaphore(0);
		manager.runOnGLThread(() -> {
			task.run(manager);
			sem.release();
		});
		try {
			assertTrue(sem.tryAcquire(maxWaitMs, TimeUnit.MILLISECONDS));
		} catch (final InterruptedException e) {
			throw new AssertionError(e);
		}
		return result;
	}
}
