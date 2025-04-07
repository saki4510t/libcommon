package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.graphics.SurfaceTexture;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLContext;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.OnFrameAvailableListener;

/**
 * 分配描画インターフェース
 * FIXME GLPipeline/IPipelineSourceを使うIRendererHolder実装を作る
 */
public interface IRendererHolder extends IMirror {

	/**
	 * IRendererHolderからのコールバックリスナー
	 */
	public interface RenderHolderCallback extends OnFrameAvailableListener {
		public default void onCreateSurface(Surface surface) {}
		public default void onDestroySurface() {}
	}

	public static class DefaultRenderHolderCallback implements RenderHolderCallback {
		@Override
		public void onFrameAvailable() {
		}
	};

	public static final RenderHolderCallback DEFAULT_CALLBACK = new DefaultRenderHolderCallback();

	/**
	 * 実行中かどうか
	 * @return
	 */
	public boolean isRunning();
	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	/**
	 * GLContextを取得する
	 * @return
	 */
	@NonNull
	public GLContext getGLContext();

	/**
	 * EGLBase.IContext<?>を取得する
	 * @return
	 * @deprecated use #getGLContext instead
	 */
	@Deprecated
	@Nullable
	public EGLBase.IContext<?> getContext();

	/**
	 * マスター用の映像を受け取るためのSurfaceを取得
	 * @return
	 */
	public Surface getSurface();

	/**
	 * マスター用の映像を受け取るためのSurfaceTextureを取得
	 * @return
	 */
	public SurfaceTexture getSurfaceTexture();

	/**
	 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
	 */
	public void reset();

	/**
	 * マスター映像サイズをサイズ変更要求
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height)
		throws IllegalStateException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通は#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通は#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 * @param maxFps nullまたは0以下なら制限しない
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを削除
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	public void removeSurface(final int id);
	
	/**
	 * 分配描画用のSurfaceを全て削除
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll();
	
	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	public void clearSurface(final int id, final int color);
	
	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 */
	public void clearSurfaceAll(final int color);
	
	/**
	 * モデルビュー変換行列をセット
	 * @param id
	 * @param offset
	 * @param matrix offset以降に16要素以上
	 */
	public void setMvpMatrix(final int id,
		final int offset, @NonNull @Size(min=16) final float[] matrix);

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	public boolean isEnabled(final int id);
	
	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	public void setEnabled(final int id, final boolean enable);

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame();

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount();

	/**
	 * レンダリングスレッド上で指定したタスクを実行する
	 * @param task
	 */
	public void queueEvent(@NonNull final Runnable task);
}
