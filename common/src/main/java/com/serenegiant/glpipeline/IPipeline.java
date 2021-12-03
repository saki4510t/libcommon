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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public interface IPipeline {
	/**
	 * 関係するリソースを破棄
	 */
	public void release();

	/**
	 * リサイズ要求
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	public void resize(final int width, final int height) throws IllegalStateException;

	/**
	 * オブジェクトが有効かどうかを取得
	 * @return
	 */
	public boolean isValid();

	/**
	 * 映像幅を取得
	 * @return
	 */
	public int getWidth();

	/**
	 * 映像高さを取得
	 * @return
	 */
	public int getHeight();

	/**
	 * 呼び出し元のIPipelineインスタンスを設定する
	 * @param parent
	 */
	public void setParent(@Nullable final IPipeline parent);

	/**
	 * 呼び出しh元のIPipelineインスタンスを取得する
	 * nullなら最上位(たぶんIPipelineSource)またはパイプラインに未接続
	 * @return
	 */
	@Nullable
	public IPipeline getParent();

	/**
	 * 次に呼び出すIPipelineインスタンスをセットする
	 * @param pipeline
	 */
	public void setPipeline(@Nullable final IPipeline pipeline);

	/**
	 * 次に呼び出すIPipelineインスタンス取得する
	 * @return
	 */
	@Nullable
	public IPipeline getPipeline();

	/**
	 * パイプラインチェーンから自分自身を取り除く
	 * 自分が最上位だとすべてのパイプラインが開放される
	 */
	public void remove();

	@WorkerThread
	public void onFrameAvailable(final boolean isOES, final int texId, @NonNull final float[] texMatrix);

	/**
	 * 指定したIPipelineの一番うしろにつながっているIPipelineを取得する。
	 * 後ろにつながっているIPipelineがなければ引数のIPipelineを返す
	 * @param pipeline
	 * @return
	 */
	public static IPipeline findLast(@NonNull final IPipeline pipeline) {
		IPipeline parent = pipeline;
		IPipeline next = parent.getPipeline();
		while (next != null) {
			parent = next;
			next = parent.getPipeline();
		}
		return parent;
	}

}
