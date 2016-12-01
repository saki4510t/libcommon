package com.serenegiant.widget;

/**
 * アスペクト比を一定に保つView用のインターフェースを定義
 */
public interface IAspectRatioView {
	public void setAspectRatio(double aspectRatio);
	public void setAspectRatio(final int width, final int height);
	public double getAspectRatio();
}
