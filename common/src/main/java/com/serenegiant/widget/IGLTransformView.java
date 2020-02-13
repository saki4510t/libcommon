package com.serenegiant.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

public interface IGLTransformView {
	@NonNull
	public View getView();
	@Size(min=16)
	@NonNull
	public float[] getTransform(@Nullable @Size(min=16) final float[] transform);
	public void setTransform(@Nullable @Size(min=16) final float[] transform);
}
