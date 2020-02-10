package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import com.serenegiant.glutils.IRendererHolder
import com.serenegiant.glutils.IRendererHolder.RenderHolderCallback
import com.serenegiant.glutils.RendererHolder
import com.serenegiant.libcommon.BuildConfig

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
class CameraGLSurfaceView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
		: AbstractCameraGLSurfaceView(context, attrs, defStyle) {

	override fun createRendererHolder(
		width: Int, height: Int, callback: RenderHolderCallback?): IRendererHolder {

		return RendererHolder(width, height, glVersion, null, 0, BuildConfig.ENABLE_VSYNC, callback)
	}

	companion object {
		private const val DEBUG = false // TODO set false on release
		private val TAG = CameraGLSurfaceView::class.java.simpleName
	}

}