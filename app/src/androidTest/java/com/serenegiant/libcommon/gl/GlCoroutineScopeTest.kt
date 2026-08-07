package com.serenegiant.libcommon.gl
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serenegiant.gl.GLInfo
import com.serenegiant.gl.GLManager
import com.serenegiant.gl.glCoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GLCoroutineScopeのテスト
 */
@RunWith(AndroidJUnit4::class)
class GlCoroutineScopeTest {
	/**
	 * 新規スレッドでGLコンテキストを生成して実行するためのGLCoroutineScopeのテスト
	 */
	@Test
	fun glCoroutineScopeTest1() {
		val scope = glCoroutineScope()
		runBlocking(scope.coroutineContext) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("GlCoroutineScopeTest", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}
		scope.cancel()
	}

	/**
	 * 既存のGLManagerのGLコンテキスト上で実行するためのGLCoroutineScopeのテスト
	 */
	@Test
	fun glCoroutineScopeTest2() {
		val glManager = GLManager()
		val scope = glManager.glCoroutineScope()
		runBlocking(scope.coroutineContext) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("GlCoroutineScopeTest", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}
		scope.cancel()
		glManager.release()
	}

	/**
	 * テスト用のコルーチンからglCoroutineScopeのディスパッチャーへ切り替えてテスト
	 * ディスパッチャーを指定してlaunchを呼び出して帰ってきたJobをjoinで待機
	 */
	@Test
	fun glCoroutineScopeTest3() = runTest {
		val scope = glCoroutineScope()
		launch(scope.dispatcher) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("glCoroutineScopeTest3", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}.join()
		scope.cancel()
	}

	/**
	 * テスト用のコルーチンからGLManager#glCoroutineScopeのディスパッチャーへ切り替えてテスト
	 * ディスパッチャーを指定してlaunchを呼び出して帰ってきたJobをjoinで待機
	 */
	@Test
	fun glCoroutineScopeTest4() = runTest {
		val glManager = GLManager()
		val scope = glManager.glCoroutineScope()
		launch(scope.dispatcher) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("glCoroutineScopeTest4", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}.join()
		scope.cancel()
		glManager.release()
	}

	/**
	 * テスト用のコルーチンからglCoroutineScopeのディスパッチャーへ切り替えてテスト
	 * withContextを使用
	 */
	@Test
	fun glCoroutineScopeTest5() = runTest {
		val scope = glCoroutineScope()
		withContext(scope.dispatcher) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("glCoroutineScopeTest3", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}
		scope.cancel()
	}

	/**
	 * テスト用のコルーチンからGLManager#glCoroutineScopeのディスパッチャーへ切り替えてテスト
	 * withContextを使用
	 */
	@Test
	fun glCoroutineScopeTest6() = runTest {
		val glManager = GLManager()
		val scope = glManager.glCoroutineScope()
		withContext(scope.dispatcher) {
			try {
				val info = GLInfo.getOnGL(scope.egl)
//				Log.i("glCoroutineScopeTest4", info.toString())
				val glInfo = info.get("GL_INFO") as JSONObject?
				Assert.assertNotNull(glInfo)
				Assert.assertNotNull(glInfo!!.get("GL_VERSION"))
				val eglInfo = info.get("EGL_INFO") as JSONObject?
				Assert.assertNotNull(eglInfo)
				Assert.assertNotNull(eglInfo!!.get("EGL_VERSION"))
			} catch (e: Exception) {
				fail(e.message)
			}
		}
		scope.cancel()
		glManager.release()
	}
}
