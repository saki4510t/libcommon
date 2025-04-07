package com.serenegiant.glutils;
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

import com.serenegiant.gl.GLManager;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Surfaceを経由して映像をテクスチャとして受け取るためのクラスの基本部分を実装
 * @deprecated GLSurfaceReceiverを使うこと
 */
@Deprecated
public class GLImageReceiver extends GLSurfaceReceiver {
	public interface FrameAvailableCallback extends GLFrameAvailableCallback {}
	public interface Callback extends GLSurfaceReceiver.Callback {}

	/**
	 * コンストラクタ
	 * 映像入力用Surfacetexture/Surfaceが生成されるまで実行がブロックされる
	 * @param glManager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public GLImageReceiver(
		@NonNull final GLManager glManager,
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@NonNull final Callback callback) {

		super(glManager, width, height, callback);
	}

}
