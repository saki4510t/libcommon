package com.serenegiant.glpipeline;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface IPipelineSource extends IPipeline {
	/**
	 * テクスチャが更新されたときの通知用コールバックリスナー
	 */
	public interface OnFrameAvailableListener {
		/**
		 * テキスチャが更新された
		 */
		@WorkerThread
		public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix);
	}

	/**
	 * PipelineSourceからのコールバックリスナー
	 */
	public interface PipelineSourceCallback {
		/**
		 * 映像受け取り用のSurfaceが生成された
		 * @param surface
		 */
		@WorkerThread
		public void onCreate(@NonNull final  Surface surface);

		/**
		 * 映像受け取り用のSurfaceが破棄された
		 */
		@WorkerThread
		public void onDestroy();
	}

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

	/**
	 * OnFrameAvailableListenerを登録
	 * @param listener
	 */
	public void add(final OnFrameAvailableListener listener);

	/**
	 * OnFrameAvailableListenerを登録解除
	 * @param listener
	 */
	public void remove(final OnFrameAvailableListener listener);
}
