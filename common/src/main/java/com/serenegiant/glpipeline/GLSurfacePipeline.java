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

import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Surfaceへの描画が可能なGLPipelineインターフェース
 */
public interface GLSurfacePipeline extends GLPipeline {
	/**
	 * 描画先のSurfaceを差し替え, 最大フレームレートの制限をしない
	 * すでにsurfaceがセットされている時に#setSurfaceで違うsurfaceをセットすると古いsurfaceは破棄される
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void setSurface(@Nullable final Object surface)
		throws IllegalStateException, IllegalArgumentException;

	/**
	 * 描画先のsurfaceをセットする
	 * すでにsurfaceがセットされている時に#setSurfaceで違うsurfaceをセットすると古いsurfaceは破棄される
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void setSurface(
		@Nullable final Object surface,
		@Nullable final Fraction maxFps) throws IllegalStateException, IllegalArgumentException;

	/**
	 * すでにsurfaceがセットされているかどうか
	 * surfaceがセットされている時に#setSurfaceで違うsurfaceをセットすると古いsurfaceは破棄される
	 * @return
	 */
	public boolean hasSurface();

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	public int getId();

	/**
	 * 指定したGLPipelineが含まれるパイプラインチェーンから指定したidを持つGLSurfacePipelineを探す
	 * @param pipeline
	 * @param id
	 * @return 見つかればGLSurfacePipeline, 見つからなければnull
	 */
	@Nullable
	public static GLSurfacePipeline findById(@NonNull final GLPipeline pipeline, final int id) {
		@NonNull
		GLPipeline p = GLPipeline.findFirst(pipeline);
		while (p != null) {
			if ((p instanceof GLSurfacePipeline)
				&& ((GLSurfacePipeline)p).getId() == id) {
				return (GLSurfacePipeline)p;
			}
			p = p.getPipeline();
		}
		return null;
	}
}
