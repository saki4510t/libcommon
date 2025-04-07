package com.serenegiant.glpipeline

import androidx.annotation.Size
import com.serenegiant.glutils.GLFrameAvailableCallback
import com.serenegiant.glutils.GLSurfaceReceiver

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

/**
 * 映像が更新されたときのコースバックインターフェースを呼び出すだけのProxyPipeline実装
 * ProxyPipelineを無名クラスで生成して#onFrameAvailableをオーバーライドするのとほぼ等価
 * パイプライン → OnFramePipeline (→ パイプライン)
 *                → onFrameAvailableコールバック呼び出し
 */
class OnFramePipeline(private val mListener: GLFrameAvailableCallback)
: ProxyPipeline() {
	override fun onFrameAvailable(
		isGLES3: Boolean,
		isOES: Boolean,
		width: Int, height: Int,
		texId: Int, @Size(min = 16) texMatrix: FloatArray) {

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix)
		mListener.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix)
	}
}
