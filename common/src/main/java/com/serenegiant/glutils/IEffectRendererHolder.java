package com.serenegiant.glutils;

import androidx.annotation.NonNull;

public interface IEffectRendererHolder extends IRendererHolder {
	/**
	 * 映像効果をセット
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	public void changeEffect(final int effect);

	/**
	 * 現在の映像効果番号を取得
	 * @return
	 */
	public int getCurrentEffect();

	/**
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param params
	 */
	public void setParams(@NonNull final float[] params);

	/**
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException effectが範囲外ならIllegalArgumentException生成
	 */
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalArgumentException;
}
