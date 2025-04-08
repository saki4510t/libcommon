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

package com.serenegiant.glutils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.OnFrameAvailableListener;

/**
 * MediaCodecのデコーダーでデコードした動画やカメラからの映像の代わりに、
 * 静止画をSurfaceへ出力するためのクラス
 * ImageTextureSourceと違って複数のsurfaceへ分配描画する
 * 出力先Surfaceが1つだけならImageTextureSourceの方が効率的
 */
public class StaticTextureSource implements GLConst, IMirror {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = StaticTextureSource.class.getSimpleName();

	@NonNull
	private final GLManager mManager;
	private final boolean mOwnGLManager;
	@NonNull
	private final GLTextureSource mSource;
	@NonNull
	private final GLSurfaceRenderer mRenderer;
	@Nullable
	private OnFrameAvailableListener mListener;

	/**
	 * ソースの静止画を指定したコンストラクタ, フレームレートは10fps固定
	 * @param bitmap
	 */
	public StaticTextureSource(@Nullable final Bitmap bitmap) {
		this(null, bitmap, new Fraction(10));
	}

	/**
	 * フレームレート指定付きコンストラクタ
	 * @param fps nullなら10fps
	 */
	public StaticTextureSource(@Nullable final Fraction fps) {
		this(null, null, fps);
	}

	/**
	 * ソースの静止画とフレームレートを指定可能なコンストラクタ
	 * @param bitmap
	 * @param fps nullなら10fps
	 */
	public StaticTextureSource(
		@Nullable final GLManager manager,
		@Nullable final Bitmap bitmap,
		@Nullable final Fraction fps) {

		final int width = bitmap != null ? bitmap.getWidth() : 1;
		final int height = bitmap != null ? bitmap.getHeight() : 1;
		if (DEBUG) Log.v(TAG, "コンストラクタ:" + bitmap);
		mOwnGLManager = (manager == null);
		mManager = mOwnGLManager ? new GLManager() : manager;
		mRenderer = new GLSurfaceRenderer(mManager, width, height, GLDrawer2D.DEFAULT_FACTORY);
		mSource = new GLTextureSource(mManager, bitmap, fps, mRenderer);
	}

	/**
	 * 実行中かどうか
	 * @return
	 */
	public boolean isRunning() {
		return mSource.isValid();
	}

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mSource.release();
		mRenderer.release();
		if (mOwnGLManager) {
			mManager.release();
		}
		if (DEBUG) Log.v(TAG, "release:finished");
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface
	 * @param isRecordable
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable) {
		addSurface(id, surface, isRecordable, -1);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id
	 * @param surface
	 * @param isRecordable
	 * @param maxFps コンストラクタで指定した値より大きくしても速く描画されるわけではない
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable, final int maxFps) {

		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		mRenderer.addSurface(id, surface, new Fraction(maxFps));
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		mRenderer.removeSurface(id);
	}

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount() {
		return mRenderer.getCount();
	}

	/**
	 * ソース静止画を指定
	 * 既にセットされていれば古いほうが破棄される
	 * @param bitmap nullなら何もしない
	 */
	public void setBitmap(final Bitmap bitmap) {
		if (DEBUG) Log.v(TAG, "setBitmap:bitmap=" + bitmap);
		mSource.setSource(bitmap, null);
	}

	/**
	 * ソース静止画の幅を取得
	 * @return 既にreleaseされていれば0
	 */
	public int getWidth() {
		return mSource.getWidth();
	}

	/**
	 * ソース静止画の高さを取得
	 * @return  既にreleaseされていれば0
	 */
	public int getHeight() {
		return mSource.getHeight();
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "mirror:" + mirror);
		mRenderer.setMirror(mirror);
	}

	@MirrorMode
	@Override
	public int getMirror() {
		return mRenderer.getMirror();
	}

}
