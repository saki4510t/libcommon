package com.serenegiant.graphics;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.IRendererCommon;
import com.serenegiant.system.BuildCheck;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surface/SurfaceTexture経由で受け取った映像を表示するDrawable
 */
public class SurfaceDrawable extends Drawable {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceDrawable.class.getSimpleName();

	public interface Callback {
		public void onCreateSurface(@NonNull final Surface surface);
		public void onDestroySurface();
	}

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	@NonNull
	private final Object mSync = new Object();
	/**
	 * 画像サイズ(コンストラクタで設定)
	 */
	private final int mImageWidth, mImageHeight;
	@NonNull
	private final Callback mCallback;
	@NonNull
	private final EglTask mEglTask;
	@NonNull
	private final Bitmap mBitmap;
	@NonNull
	private final Matrix mTransform = new Matrix();
	@NonNull
	private final Paint mPaint = new Paint();
	@NonNull
	final float[] mTexMatrix = new float[16];
	private final ByteBuffer mWorkBuffer;
	private int mTexId;
	private SurfaceTexture mMasterTexture;
	private Surface mMasterSurface;
	private EGLBase.IEglSurface mWorkSurface;
	private GLDrawer2D mDrawer;

	/**
	 * Drawableの外形サイズ
	 */
	private int mWidth, mHeight;

	/**
	 * コンストラクタ
	 * @param imageWidth
	 * @param imageHeight
	 */
	public SurfaceDrawable(final int imageWidth, final int imageHeight,
		@NonNull final Callback callback) {

		mWidth = mImageWidth = imageWidth;
		mHeight = mImageHeight = imageHeight;
		mCallback = callback;
		mEglTask = new EglTask(3, null, 0) {
			@Override
			protected void onStart() {
				handleOnStart();
			}

			@Override
			protected void onStop() {
				handleOnStop();
			}

			@Override
			protected Object processRequest(final int request,
				final int arg1, final int arg2, final Object obj)
					throws TaskBreak {

				return handleRequest(request, arg1, arg2, obj);
			}
		};
		mBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		mWorkBuffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4);
		new Thread(mEglTask, TAG).start();
		mEglTask.offer(REQUEST_RECREATE_MASTER_SURFACE);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		mEglTask.release();
	}

	@Override
	public void draw(@NonNull final Canvas canvas) {
		synchronized (mBitmap) {
			canvas.drawBitmap(mBitmap, mTransform, mPaint);
		}
	}

	@Override
	public void setAlpha(final int alpha) {
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable final ColorFilter filter) {
		mPaint.setColorFilter(filter);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
		@NonNull
		final Rect bounds = getBounds();
		mWidth = bounds.width();
		mHeight = bounds.height();
		final float scaleX = mWidth / (float)mBitmap.getWidth();
		final float scaleY = mHeight / (float)mBitmap.getHeight();
		mTransform.reset();
		mTransform.postScale(scaleX, scaleY);
	}

	@Override
	public int getIntrinsicWidth() {
		return mWidth;
	}

	@Override
	public int getIntrinsicHeight() {
		return mHeight;
	}

//--------------------------------------------------------------------------------
	public boolean isSurfaceValid() {
		return (mMasterSurface != null) && (mMasterSurface.isValid());
	}

	public Surface getSurface() {
		synchronized (mSync) {
			return mMasterSurface;
		}
	}

	public SurfaceTexture getSurfaceTexture() {
		synchronized (mSync) {
			return mMasterTexture;
		}
	}

//--------------------------------------------------------------------------------
	protected EGLBase getEgl() {
		return mEglTask.getEgl();
	}

	protected EGLBase.IContext getContext() {
		return mEglTask.getContext();
	}

	protected boolean isGLES3() {
		return mEglTask.isGLES3();
	}

	protected int getTexId() {
		return mTexId;
	}

	protected float[] getTexMatrix() {
		return mTexMatrix;
	}

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
		// OESテクスチャを直接ハンドリングできないのでオフスクリーンへ描画して読み込む
		mWorkSurface = mEglTask.getEgl().createOffscreen(mImageWidth, mImageHeight);
		mDrawer = GLDrawer2D.create(isGLES3(), true);
		mDrawer.setMirror(IRendererCommon.MIRROR_VERTICAL);
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
		if (mWorkSurface != null) {
			mWorkSurface.release();
			mWorkSurface = null;
		}
		handleReleaseMasterSurface();
	}

	@WorkerThread
	protected Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

		switch (request) {
		case REQUEST_DRAW:
			handleDraw();
			break;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateMasterSurface();
			break;
		default:
			if (DEBUG) Log.v(TAG, "handleRequest:" + request);
			break;
		}
		return null;
	}

	private int drawCnt;

	@WorkerThread
	protected void handleDraw() {
		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		mEglTask.removeRequest(REQUEST_DRAW);
		try {
			mEglTask.makeCurrent();
			mMasterTexture.updateTexImage();
			mMasterTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			return;
		}
		// オフスクリーンへ描画開始
		mWorkSurface.makeCurrent();
		// OESテクスチャをオフスクリーンへ描画
		mDrawer.draw(mTexId, mTexMatrix, 0);
		// オフスクリーンから読み取り
		mWorkBuffer.clear();
		GLES20.glReadPixels(0, 0, mImageWidth, mImageHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mWorkBuffer);
//		// オフスクリーンへ描画終了
//		mWorkSurface.swap(Time.nanoTime());	// XXX これは不要...呼ぶとなぜかGPUのドライバー内でクラッシュするし
		// 何も書き込まないとハングアップする端末対策
		mEglTask.makeCurrent();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		// Bitmapへ代入
		mWorkBuffer.clear();
		synchronized (mBitmap) {
			mBitmap.copyPixelsFromBuffer(mWorkBuffer);
		}
		invalidateSelf();
	}

	/**
	 * マスターSurfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateMasterSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateMasterSurface:");
		synchronized (mSync) {
			mEglTask.makeCurrent();
			handleReleaseMasterSurface();
			mEglTask.makeCurrent();
			if (isGLES3()) {
				mTexId = com.serenegiant.glutils.es3.GLHelper.initTex(
					GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE0, GLES30.GL_NEAREST);
			} else {
				mTexId = com.serenegiant.glutils.es2.GLHelper.initTex(
					GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			}
			mMasterTexture = new SurfaceTexture(mTexId);
			mMasterSurface = new Surface(mMasterTexture);
			if (BuildCheck.isAndroid4_1()) {
				mMasterTexture.setDefaultBufferSize(getIntrinsicWidth(), getIntrinsicHeight());
			}
			mMasterTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
		onCreateSurface(mMasterSurface);
	}

	/**
	 * マスターSurfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReleaseMasterSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseMasterSurface:");
		synchronized (mSync) {
			if (mMasterSurface != null) {
				try {
					mMasterSurface.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mMasterSurface = null;
				onDestroySurface();
			}
			if (mMasterTexture != null) {
				try {
					mMasterTexture.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mMasterTexture = null;
			}
			if (mTexId != 0) {
				if (isGLES3()) {
					com.serenegiant.glutils.es3.GLHelper.deleteTex(mTexId);
				} else {
					com.serenegiant.glutils.es2.GLHelper.deleteTex(mTexId);
				}
				mTexId = 0;
			}
		}
	}

	private void onCreateSurface(@NonNull final Surface surface) {
		if (DEBUG) Log.v(TAG, "onCreateSurface:" + surface);
		mCallback.onCreateSurface(surface);
	}

	private void onDestroySurface() {
		if (DEBUG) Log.v(TAG, "onDestroySurface:");
		mCallback.onDestroySurface();
	}

	private final SurfaceTexture.OnFrameAvailableListener
		mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onFrameAvailable:");
			mEglTask.offer(REQUEST_DRAW);
		}
	};

}
