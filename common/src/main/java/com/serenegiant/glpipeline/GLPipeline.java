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

import com.serenegiant.gl.GLConst;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 映像受け取り・映像効果付与・プレビュー表示等を動的に組み合わせて逐次処理するためのインターフェース定義
 */
public interface GLPipeline extends GLConst {
	static final String TAG = GLPipeline.class.getSimpleName();

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
	 * 呼び出し元のGLPipelineインスタンスを設定する
	 * @param parent
	 */
	public void setParent(@Nullable final GLPipeline parent);

	/**
	 * 呼び出しh元のGLPipelineインスタンスを取得する
	 * nullなら最上位(たぶんGLPipelineSource)またはパイプラインに未接続
	 * @return
	 */
	@Nullable
	public GLPipeline getParent();

	/**
	 * 次に呼び出すGLPipelineインスタンスをセットする
	 * 既にパイプラインがセットされている場合は置き換える
	 * @param pipeline
	 */
	public void setPipeline(@Nullable final GLPipeline pipeline);

	/**
	 * 次に呼び出すGLPipelineインスタンス取得する
	 * @return
	 */
	@Nullable
	public GLPipeline getPipeline();

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
	 * 指定したGLPipelineの一番うしろにつながっているGLPipelineを取得する。
	 * 後ろにつながっているGLPipelineがなければ引数のGLPipelineを返す
	 * @param pipeline
	 * @return
	 */
	@NonNull
	public static GLPipeline findLast(@NonNull final GLPipeline pipeline) {
		GLPipeline parent = pipeline;
		GLPipeline next = parent.getPipeline();
		while (next != null) {
			parent = next;
			next = parent.getPipeline();
		}
		return parent;
	}

	/**
	 * 指定したGLPipelineの一番前につながっているGLPipeline(通常はGLPipelineSourceのはず)を取得する。
	 * 前につながっているGLPipelineがなければ引数のGLPipelineを返す
	 * @param pipeline
	 * @return
	 */
	@NonNull
	public static GLPipeline findFirst(@NonNull final GLPipeline pipeline) {
		GLPipeline current = pipeline;
		GLPipeline parent = current.getParent();
		while (parent != null) {
			current = parent;
			parent = current.getParent();
		}
		return current;
	}

	/**
	 * パイプラインチェーンに含まれる指定したGLPipelineオブジェクトを取得する
	 * 複数存在する場合は最初に見つかったものを返す
	 * @param pipeline
	 * @param clazz GLPipelineを実装したクラスのClassオブジェクト
	 * @return 見つからなければnull
	 */
	@Nullable
	public static <T extends GLPipeline> T find(@NonNull final GLPipeline pipeline, @NonNull final Class<T> clazz) {
		// パイプラインチェーンの先頭を取得
		GLPipeline p = findFirst(pipeline);
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
	 * パイプラインチェーンの最後に指定したGLPipelineを追加する
	 * @param chain
	 * @param pipeline
	 * @return パイプラインチェーンの一番最後のGLPipeline(=追加したGLPipeline)を返す
	 */
	@NonNull
	public static GLPipeline append(@NonNull final GLPipeline chain, @NonNull final GLPipeline pipeline) {
		final GLPipeline last = findLast(chain);
		last.setPipeline(pipeline);
		return pipeline;
	}

	/**
	 * targetで指定したGLPipelineにpipelineで指定したGLPipelineをつなげる
	 * GLPipeline#setPipelineと違ってtargetに既にパイプラインガセットされているときはつなぎ替える
	 * @param target
	 * @param pipeline
	 * @return 追加したGLPipeline
	 */
	@NonNull
	public static GLPipeline insert(@NonNull final GLPipeline target, @NonNull final GLPipeline pipeline) {
		final GLPipeline p = target.getPipeline();
		if (p == null) {
			// targetの後ろにパイプラインが存在していないときは#setPipeline
			target.setPipeline(pipeline);
		} else if (p != pipeline) {
			// targetに自分以外のパイプラインがセットされているとき
			// 挿入するパイプライン(チェーン)の一番後ろにtargetの後ろのパイプライン(チェーン)を繋ぐ
			final GLPipeline last = findLast(pipeline);
			target.setPipeline(pipeline);
			last.setPipeline(p);
		}
		return pipeline;
	}

	/**
	 * 指定したGLPipelineからのパイプラインチェーンを角カッコでくくったカンマ区切りの文字列に変換する
	 * @param root
	 * @return
	 */
	@NonNull
	public static String pipelineString(@NonNull final GLPipeline root) {
		final StringBuilder sb = new StringBuilder("[");
		GLPipeline pipeline = root;
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
	 * ProxyPipelineの継承クラスの場合はGLPipeline#removeを呼び出したときに自動的に呼び出される
	 * @param root
	 * @return
	 */
	public static boolean validatePipelineChain(@NonNull final GLPipeline root) {
		boolean result = true;
		GLPipeline pipeline = root;
		while (pipeline != null) {
			final GLPipeline next = pipeline.getPipeline();
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
