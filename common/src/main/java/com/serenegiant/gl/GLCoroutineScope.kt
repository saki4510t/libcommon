package com.serenegiant.gl
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

import android.util.Log
import com.serenegiant.egl.EGLBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private const val DEBUG = false
private val TAG = GLCoroutineScope::class.java.simpleName

abstract class GLCoroutineScope internal constructor(): CoroutineScope {
	abstract val glContext: GLContext
	val egl: EGLBase
		get() = glContext.egl

	val isGLES3: Boolean
		get() = glContext.isGLES3

	fun makeDefault() {
		glContext.makeDefault()
	}

	fun swap() {
		glContext.swap()
	}
}

private class GLCoroutineScopeImpl(
	override val glContext: GLContext,
	override val coroutineContext: CoroutineContext
) : GLCoroutineScope() {
	private val mOwnGLContext = !glContext.isInitialized

	init {
		runBlocking(coroutineContext) {
			if (DEBUG) Log.v(TAG, "initialize gl context")
			if (!glContext.isInitialized) {
				glContext.initialize()
			}
		}
		launch {
			try {
				awaitCancellation()
			} finally {
				if (mOwnGLContext) {
					if (DEBUG) Log.v(TAG, "release gl context")
					glContext.release()
				}
			}
		}
	}
}

/**
 * EGL/GL|ESコンテキストを保持するシングルスレッド上で実行するためのCoroutineScopeを取得する
 */
fun glCoroutineScope(): GLCoroutineScope {
	val executor = Executors.newSingleThreadExecutor()
	return GLCoroutineScopeImpl(
		GLContext(),
		SupervisorJob() + executor.asCoroutineDispatcher())
}

/**
 * 初期化済みのGLManagerからGLCoroutineScopeを取得する拡張関数
 */
fun GLManager.glCoroutineScope(): GLCoroutineScope {
	return GLCoroutineScopeImpl(
		glContext,
		SupervisorJob() + glHandler.asCoroutineDispatcher())
}