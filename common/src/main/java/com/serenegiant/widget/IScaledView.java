package com.serenegiant.widget;

/**
 * コンテンツの拡大縮小方法をセット可能なViewのインターフェース
 */
public interface IScaledView {
	/** アスペクト比を保って最大化 */
	public static final int SCALE_MODE_KEEP_ASPECT = 0;
	/** 画面サイズに合わせて拡大縮小 */
	public static final int SCALE_MODE_STRETCH_TO_FIT = 1;
	/** アスペクト比を保って短辺がフィットするようにCROP_CENTER */
	public static final int SCALE_MODE_CROP = 2;

	/**
	 * 拡大縮小方法をセット
	 * @param scaleMode SCALE_MODE_KEEP_ASPECT, SCALE_MODE_STRETCH, SCALE_MODE_CROP
	 */
	public void setScaleMode(final int scaleMode);
	public int getScaleMode();
}
