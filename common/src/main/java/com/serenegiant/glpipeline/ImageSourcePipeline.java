package com.serenegiant.glpipeline;
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

import android.graphics.Bitmap;
import android.util.Log;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.ImageTextureSource;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.OnFrameAvailableListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 静止画(Bitmap)を映像ソースとするためのGLPipelineSource実装
 * これが生成する映像ソースのテクスチャはGL_TEXTURE_2D
 * ビットマップ → ImageSourcePipeline (→ パイプライン)
 */
public class ImageSourcePipeline extends ProxyPipeline implements GLPipelineSource {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ImageSourcePipeline.class.getSimpleName();

	private static final float DEFAULT_FPS = 30.0f;

	@NonNull
	private final GLManager mManager;

	/**
	 * GLTextureを使って静止画をテクスチャとして読み込みChoreographerを使って定期的に
	 * #onFrameAvailableを呼び出す代わりに、ImageTextureSource利用する(Surfaceはセットしない)
	 * ImageTextureSourceにはSurfaceをセットしなくてもGLTextureを使って静止画を
	 * テクスチャとして読み込みChoreographerを使って定期的に#OnFrameAvailableListener
	 * リスナーを呼び出す機能があるので。
	 */
	@NonNull
	private final ImageTextureSource mImageTextureSource;
	// 現在の映像サイズ
	private int mWidth = 0, mHeight = 0;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps nullの時は30fps相当
	 */
	public ImageSourcePipeline(
		@NonNull final GLManager manager,
		@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:" + bitmap);
		mManager = manager;
		mImageTextureSource = new ImageTextureSource(manager, bitmap, fps, new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable() {
				if (isActive()) {
					ImageSourcePipeline.this.onFrameAvailable(
						mManager.isGLES3(), false,
						getWidth(), getHeight(),
						mImageTextureSource.getTexId(), mImageTextureSource.getTexMatrix());
				}
			}
		});
	}

	/**
	 * GLPipelineの実装
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	@Override
	public boolean isActive() {
		mLock.lock();
		try {
			// 破棄されていない && 子と繋がっている
			return isValid() && (getPipeline() != null);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void setParent(@Nullable final GLPipeline parent) {
		super.setParent(parent);
		throw new UnsupportedOperationException("Can't set parent to GLPipelineSource");
	}

	@Override
	protected void internalRelease() {
		mImageTextureSource.release();
		super.internalRelease();
	}

	@NonNull
	@Override
	public GLManager getGLManager() throws IllegalStateException {
		return mManager;
	}

	/**
	 * テクスチャ名を取得する
	 * すでに#releaseが呼ばれたか映像ソース用のBitmapがセットされていないときはIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@Override
	public int getTexId() throws IllegalStateException {
		return mImageTextureSource.getTexId();
	}

	/**
	*  テキスチャ変換行列を取得する
	 * すでに#releaseが呼ばれたか映像ソース用のBitmapがセットされていないときはIllegalStateExceptionを投げる
	 * @return
	 * @throws IllegalStateException
	 */
	@Size(min=16)
	@NonNull
	@Override
	public float[] getTexMatrix() throws IllegalStateException {
		return mImageTextureSource.getTexMatrix();
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		mLock.lock();
		try {
			// 映像ソースが準備できていなければスキップする
			if (!isValid() || !mImageTextureSource.isValid()) return;
		} finally {
			mLock.unlock();
		}
		if (DEBUG && (++cnt % 100) == 0) {
			Log.v(TAG, "onFrameAvailable:" + cnt);
		}
		if ((mWidth != mImageTextureSource.getWidth()) || (mHeight != mImageTextureSource.getHeight())) {
			mWidth = mImageTextureSource.getWidth();
			mHeight = mImageTextureSource.getHeight();
			resize(mWidth, mHeight);
		}

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
	}

	/**
	 * 映像ソース用のBitmapをセット
	 * @param bitmap nullのときは後で#setSourceを呼び出さないと#onFrameAvailableが呼び出されない
	 * @param fps
	 */
	public void setSource(@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
		mImageTextureSource.setSource(bitmap, fps);
	}

}
