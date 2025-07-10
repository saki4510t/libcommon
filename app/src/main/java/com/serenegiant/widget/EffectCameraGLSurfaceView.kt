package com.serenegiant.widget
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

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.serenegiant.glutils.EffectRendererHolder
import com.serenegiant.gl.GLEffect
import com.serenegiant.gl.GLEffectDrawer2D
import com.serenegiant.glutils.IRendererHolder
import com.serenegiant.glutils.IRendererHolder.RenderHolderCallback

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
class EffectCameraGLSurfaceView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null)
		: AbstractCameraGLSurfaceView(context, attrs) {

	var effect: Int
	get() {
		val rendererHolder = rendererHolder
		if (DEBUG) Log.v(TAG, "getEffect:$rendererHolder")
		return if (rendererHolder is EffectRendererHolder) rendererHolder.effect else 0
	}

	set(effect) {
		if (DEBUG) Log.v(TAG, "setEffect:$effect")
		if (effect >= 0) {
			post {
				val rendererHolder = rendererHolder
				if (rendererHolder is EffectRendererHolder) {
					rendererHolder.effect = effect
				}
			}
		}
	}

	override fun createRendererHolder(
		width: Int, height: Int,
		callback: RenderHolderCallback?): IRendererHolder {

		if (DEBUG) Log.v(TAG, "createRendererHolder:")
		return EffectRendererHolder(width, height, glVersion, null, 0, callback,
			GLEffectDrawer2D.DEFAULT_EFFECT_FACTORY)
	}

	companion object {
		private const val DEBUG = false // TODO set false on release
		private val TAG = EffectCameraGLSurfaceView::class.java.simpleName
	}
}
