package com.serenegiant.glpipeline;
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

import androidx.annotation.NonNull;

/**
 * 映像をSurface/SurfaceTextureとして受け取って
 * 他のPipelineからテクスチャとして利用可能とするためのヘルパークラス
 * useSharedContext=VideoSourcePipeline + SurfaceDistributePipeline ≒ IRendererHolder/RendererHolder
 * 映像 → Surface → VideoSourcePipeline (→ パイプライン)
 * Surfaceからの映像をパイプラインソースとして利用することがわかりやすいように
 * VideoSourcePipelineをSurfaceSourcePipelineへリネームして、
 * 互換性のためにVideoSourcePipeline自体はSurfaceSourcePipelineの
 * シノニムとしたので原則としてSurfaceSourcePipelineを使うこと
 * @deprecated SurfaceSourcePipelineを使うこと
 */
@Deprecated
public class VideoSourcePipeline extends SurfaceSourcePipeline {
	/**
	 * コンストラクタ
	 * 引数のGLManagerのスレッド上で動作する
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public VideoSourcePipeline(
		@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback) {

		super(manager, width, height, callback, false);
	}

	/**
	 * コンストラクタ
	 * useSharedContext=falseなら引数のGLManagerのスレッド上で動作する
	 * useSharedContext=trueなら共有コンテキストを使って専用スレッド上で動作する
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ってマルチスレッドで処理を行うかどうか
	 */
	public VideoSourcePipeline(
		@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback,
		final boolean useSharedContext) {

		super(manager, width, height, callback, useSharedContext);
	}

}
