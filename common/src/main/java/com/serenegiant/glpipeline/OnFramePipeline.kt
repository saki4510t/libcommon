package com.serenegiant.glpipeline

/**
 * 映像が更新されたときのコースバックインターフェースを呼び出すだけのIPipeline実装
 */
class OnFramePipeline(private val mListener: OnFrameAvailableListener)
: ProxyPipeline() {
	/**
	 * 映像が更新されたときの通知用コールバックリスナー
	 */
	interface OnFrameAvailableListener {
		fun onFrameAvailable()
	}

	override fun onFrameAvailable(isOES: Boolean, texId: Int, texMatrix: FloatArray) {
		super.onFrameAvailable(isOES, texId, texMatrix)
		mListener.onFrameAvailable()
	}
}