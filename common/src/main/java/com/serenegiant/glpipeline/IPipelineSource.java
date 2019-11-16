package com.serenegiant.glpipeline;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;

public interface IPipelineSource extends IPipeline {
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
}
