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

import android.view.View
import com.serenegiant.math.Fraction
import com.serenegiant.media.OnFrameAvailableListener

/**
 * CameraDelegatorの親Viewがサポートしないといけないインターフェースメソッド
 */
interface ICameraView {
	fun getView(): View
	// GLSurfaceView
	fun onResume()
	fun onPause()

	fun setVideoSize(width: Int, height: Int)

	fun addListener(listener: OnFrameAvailableListener)
	fun removeListener(listener: OnFrameAvailableListener)

	fun getScaleMode(): Int
	fun setScaleMode(mode: Int)

	fun getVideoWidth(): Int
	fun getVideoHeight(): Int

	fun addSurface(id: Int, surface: Any, isRecordable: Boolean, maxFps: Fraction? = null)
	fun removeSurface(id: Int)

	fun isRecordingSupported(): Boolean
}
