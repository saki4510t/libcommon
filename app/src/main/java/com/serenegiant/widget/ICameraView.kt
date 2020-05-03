package com.serenegiant.widget

import android.view.View

/**
 * CameraDelegatorの親Viewがサポートしないといけないインターフェースメソッド
 */
interface ICameraView {
	fun getView(): View
	// GLSurfaceView
	fun onResume()
	fun onPause()

	fun setVideoSize(width: Int, height: Int)

	fun addListener(listener: CameraDelegator.OnFrameAvailableListener)
	fun removeListener(listener: CameraDelegator.OnFrameAvailableListener)

	fun getScaleMode(): Int
	fun setScaleMode(mode: Int)

	fun getVideoWidth(): Int
	fun getVideoHeight(): Int

	fun addSurface(id: Int, surface: Any, isRecordable: Boolean)
	fun removeSurface(id: Int)
}