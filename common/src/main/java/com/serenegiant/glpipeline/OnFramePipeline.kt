package com.serenegiant.glpipeline

import androidx.annotation.Size

/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

/**
 * 映像が更新されたときのコースバックインターフェースを呼び出すだけのProxyPipeline実装
 */
class OnFramePipeline(private val mListener: com.serenegiant.glutils.OnFrameAvailableListener)
: ProxyPipeline() {
	/**
	 * 映像が更新されたときの通知用コールバックリスナー
	 * @deprecated com.serenegiant.glutils.OnFrameAvailableListenerのシノニムなので
	 *     直接com.serenegiant.glutils.OnFrameAvailableListenerを使うこと
	 */
	@Deprecated("use OnFrameAvailableListener in com.serenegiant.glutils package")
	interface OnFrameAvailableListener : com.serenegiant.glutils.OnFrameAvailableListener

	override fun onFrameAvailable(
		isOES: Boolean, texId: Int,
		@Size(min = 16) texMatrix: FloatArray) {

		super.onFrameAvailable(isOES, texId, texMatrix)
		mListener.onFrameAvailable()
	}
}
