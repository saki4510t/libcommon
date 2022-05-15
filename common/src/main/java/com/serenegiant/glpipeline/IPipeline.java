package com.serenegiant.glpipeline;
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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 映像受け取り・映像効果付与・プレビュー表示等を動的に組み合わせて逐次処理するためのインターフェース定義
 */
public interface IPipeline {
	static final String TAG = IPipeline.class.getSimpleName();

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
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	public boolean isActive();

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

	/**
	 * 新しく映像を受け取ったときの処理
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix);

	/**
	 * パイプラインチェーンからパイプラインが削除されたので更新要求する
	 * #removeで先頭のパイプラインを取得してチェーン中のパイプライン全てに対して順に呼び出される
	 */
	public void refresh();

	/**
	 * 指定したIPipelineの一番うしろにつながっているIPipelineを取得する。
	 * 後ろにつながっているIPipelineがなければ引数のIPipelineを返す
	 * @param pipeline
	 * @return
	 */
	@NonNull
	public static IPipeline findLast(@NonNull final IPipeline pipeline) {
		IPipeline parent = pipeline;
		IPipeline next = parent.getPipeline();
		while (next != null) {
			parent = next;
			next = parent.getPipeline();
		}
		return parent;
	}

	/**
	 * 指定したIPipelineの一番前につながっているIPipeline(通常はIPipelineSourceのはず)を取得する。
	 * 前につながっているIPipelineがなければ引数のIPipelineを返す
	 * @param pipeline
	 * @return
	 */
	@NonNull
	public static IPipeline findFirst(@NonNull final IPipeline pipeline) {
		IPipeline current = pipeline;
		IPipeline parent = current.getParent();
		while (parent != null) {
			current = parent;
			parent = current.getParent();
		}
		return current;
	}

	/**
	 * パイプラインチェーンに含まれる指定したIPipelineオブジェクトを取得する
	 * 複数存在する場合は最初に見つかったものを返す
	 * @param pipeline
	 * @param clazz IPipelineを実装したクラスのClassオブジェクト
	 * @return 見つからなければnull
	 */
	@Nullable
	public static <T extends IPipeline> T find(@NonNull final IPipeline pipeline, @NonNull final Class<T> clazz) {
		// パイプラインチェーンの先頭を取得
		IPipeline p = findFirst(pipeline);
		// 指定したクラスが見つかるまで順番にたどる
		while (p != null) {
			if (p.getClass() == clazz) {
				return clazz.cast(p);
			}
			p = p.getPipeline();
		}
		return null;
	}

	/**
	 * 指定したIPipelineからのパイプラインチェーンを角カッコでくくったカンマ区切りの文字列に変換する
	 * @param root
	 * @return
	 */
	@NonNull
	public static String pipelineString(@NonNull final IPipeline root) {
		final StringBuilder sb = new StringBuilder("[");
		IPipeline pipeline = root;
		while (pipeline != null) {
			if (pipeline != root) {
				sb.append(',');
			}
			sb.append(pipeline);
			pipeline = pipeline.getPipeline();
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * パイプラインチェーンが正しく繋がっているかどうかを検証
	 * ProxyPipelineの継承クラスの場合はIPipeline#removeを呼び出したときに自動的に呼び出される
	 * @param root
	 * @return
	 */
	public static boolean validatePipelineChain(@NonNull final IPipeline root) {
		boolean result = true;
		IPipeline pipeline = root;
		while (pipeline != null) {
			final IPipeline next = pipeline.getPipeline();
			if ((next != null) && (next.getParent() != pipeline)) {
				Log.v(TAG, "validatePipelineChain:found wrong chain" + pipeline
					+ "=>" + next + (next != null ? "(" + next.getParent() + ")" : ""));
				next.setParent(pipeline);
				result = false;
			}
			pipeline = next;
		}
		return result;
	}
}
