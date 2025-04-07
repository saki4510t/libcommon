package com.serenegiant.glutils;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * #onFrameAvailableだけを後から差し替えれるようにCallbackインターフェースから分離
 * GLコンテキストを保持したスレッド上で実行される
 */
public interface GLFrameAvailableCallback {
	/**
	 * 映像をテクスチャとして受け取ったときの処理
	 * @param isGLES3
	 * @param isOES
	 * @param width
	 * @param height,
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @Size(min=16) @NonNull final float[] texMatrix);
}
