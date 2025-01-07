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
import android.graphics.Color
import android.util.AttributeSet
import com.serenegiant.glutils.IRendererHolder
import com.serenegiant.glutils.IRendererHolder.RenderHolderCallback
import com.serenegiant.glutils.OverlayRendererHolder
import com.serenegiant.graphics.BitmapHelper

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
class OverlayCameraGLSurfaceView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null)
		: AbstractCameraGLSurfaceView(context, attrs) {

	@Synchronized
	override fun onResume() {
		super.onResume()
		val rendererHolder = rendererHolder
		if (rendererHolder is OverlayRendererHolder) {
			rendererHolder.setOverlay(0,
				BitmapHelper.genMaskImage(0,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
					60, Color.RED, 0, 100))
		}
	}

	override fun createRendererHolder(
		width: Int, height: Int,
		callback: RenderHolderCallback?): IRendererHolder {

		return OverlayRendererHolder(width, height, glVersion, null, 0, callback)
	}

	companion object {
		private const val DEBUG = false // TODO set false on release
		private val TAG = OverlayCameraGLSurfaceView::class.java.simpleName
	}
}
