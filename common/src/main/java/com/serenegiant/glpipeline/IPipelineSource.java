package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface IPipelineSource extends IPipeline {
	/**
	 * テクスチャが更新されたときの通知用コールバックリスナー
	 */
	public interface OnFrameAvailableListener {
		/**
		 * テキスチャが更新された
		 */
		@WorkerThread
		public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix);
	}

	/**
	 * PipelineSourceからのコールバックリスナー
	 */
	public interface PipelineSourceCallback {
		/**
		 * 映像受け取り用のSurfaceが生成された
		 * @param surface
		 */
		@WorkerThread
		public void onCreate(@NonNull final  Surface surface);

		/**
		 * 映像受け取り用のSurfaceが破棄された
		 */
		@WorkerThread
		public void onDestroy();
	}

	/**
	 * 映像入力用のSurfaceTextureを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public SurfaceTexture getInputSurfaceTexture() throws IllegalStateException;

	/**
	 * 映像入力用のSurfaceを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public Surface getInputSurface() throws IllegalStateException;

	/**
	 * テクスチャ名を取得
	 * @return
	 */
	public int getTexId();


	/**
	 * テクスチャ変換行列を取得
	 * @return
	 */
	public float[] getTexMatrix();

	/**
	 * OnFrameAvailableListenerを登録
	 * @param listener
	 */
	public void add(final OnFrameAvailableListener listener);

	/**
	 * OnFrameAvailableListenerを登録解除
	 * @param listener
	 */
	public void remove(final OnFrameAvailableListener listener);
}
