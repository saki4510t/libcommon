package com.serenegiant.gl
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

import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.serenegiant.gl.GLConst.TexUnit

/**
 * コンピュートシェーダー・フラグメントシェーダーを使ったGPGPU処理のためのインターフェース
 */
interface GLCompute {
	/**
	 * 関係するリソースを破棄する
	 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
	 */
	@WorkerThread
	fun release()

	/**
	 * 計算実行計算
	 * EGL|GLコンテキストを保持するスレッド上で呼び出すこと
	 * @param width 映像幅、ピクセル
	 * @param height 映像高さ、ピクセル
	 * @param texUnit テクスチャユニット
	 * @param texId テクスチャID
	 * @param texMatrix テクスチャ変換行列
	 * @param texOffset テクスチャ変換行列のオフセット
	 */
	@WorkerThread
	fun compute(
		width: Int, height: Int,
		@TexUnit texUnit: Int, texId: Int,
		@Size(min = 16) texMatrix: FloatArray?,
		texOffset: Int
	)
}
