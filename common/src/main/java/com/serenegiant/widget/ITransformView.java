package com.serenegiant.widget;

import android.graphics.Matrix;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * android.graphics.Matrixを使った表示内容のトランスフォーム可能なView
 */
public interface ITransformView {
	@NonNull
	public View getView();
	@NonNull
	public Matrix getTransform(@Nullable Matrix transform);
	public void setTransform(Matrix transform);
}
