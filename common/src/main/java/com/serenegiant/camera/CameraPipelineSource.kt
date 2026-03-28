package com.serenegiant.camera
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

import androidx.annotation.RequiresPermission
import com.serenegiant.glpipeline.GLPipelineSource

/**
 * カメラ映像をソースとするGLPipelineSource
 */
interface CameraPipelineSource: GLPipelineSource {
	/**
	 * カメラへ接続する
	 */
	@RequiresPermission(android.Manifest.permission.CAMERA)
	fun connect()
	/**
	 * カメラ型切断する
	 */
	fun disconnect()
	/**
	 * カメラ映像の向きを画面に向きに合わせる
	 * OrientationEventListenerからの値を90度単位に丸めた値をセットする
	 */
	fun updateOrientation(orientation90: Int)
	/**
	 * 画面回転・カメラ回転を考慮したモデルビュー変換行列を取得する
	 */
	@androidx.annotation.Size(min = 16)
	fun getMvpMatrix(): FloatArray

	@CameraConst.FaceType
	val face: Int
	val supportedSizeList: List<CameraSize>
}