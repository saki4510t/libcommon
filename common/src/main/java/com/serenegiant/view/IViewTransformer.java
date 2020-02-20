package com.serenegiant.view;

import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * android.graphics.Matrixを使った表示内容のトランスフォームを行うViewの場合
 */
public interface IViewTransformer extends IContentTransformer {
	/**
	 * トランスフォームマトリックスのコピーを取得
	 *
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	public Matrix getTransform(@Nullable Matrix transform);

	/**
	 * トランスフォームマトリックスをセットする
	 *
	 * @param transform nullなら単位行列をセットする
	 * @return
	 */
	@NonNull
	public IViewTransformer setTransform(@Nullable Matrix transform);

	/**
	 * Viewからトランスフォームマトリックスを取得する
	 *
	 * @param saveAsDefault
	 * @return
	 */
	@NonNull
	public IViewTransformer updateTransform(final boolean saveAsDefault);

	/**
	 * デフォルトのトランスフォームマトリックスを設定する
	 *
	 * @param transform nullなら単位行列になる
	 * @return
	 */
	@NonNull
	public IViewTransformer setDefault(@Nullable final Matrix transform);

	/**
	 * トランスフォームマトリックスを初期化する
	 *
	 * @return
	 */
	@NonNull
	public IViewTransformer reset();
}
