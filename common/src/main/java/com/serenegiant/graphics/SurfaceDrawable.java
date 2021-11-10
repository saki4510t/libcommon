package com.serenegiant.graphics;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLUtils;
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
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	/**
	 * 画像サイズ
	 */
	private int mImageWidth, mImageHeight;
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
	private ByteBuffer mWorkBuffer;
	private int mTexId;
	private SurfaceTexture mInputTexture;
	private Surface mInputSurface;
	private GLDrawer2D mDrawer;

	/**
	 * Drawableの外形サイズ
	 */
	private int mWidth, mHeight;

	/**
	 * コンストラクタ
	 * @param imageWidth
	 * @param imageHeight
	 * @param callback
	 */
	public SurfaceDrawable(final int imageWidth, final int imageHeight,
		@NonNull final Callback callback) {

		this(imageWidth, imageHeight, GLUtils.getSupportedGLVersion(), callback);
	}

	/**
	 * コンストラクタ
	 * @param imageWidth
	 * @param imageHeight
	 * @param maxClientVersion
	 * @param callback
	 */
	public SurfaceDrawable(final int imageWidth, final int imageHeight,
		final int maxClientVersion,
		@NonNull final Callback callback) {

		mWidth = mImageWidth = imageWidth;
		mHeight = mImageHeight = imageHeight;
		mCallback = callback;
		mEglTask = new EglTask(maxClientVersion,
			null, 0,
			imageWidth, imageHeight) {

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
	public int getIntrinsicWidth() {
		return mWidth;
	}

	@Override
	public int getIntrinsicHeight() {
		return mHeight;
	}

//--------------------------------------------------------------------------------
	public boolean isSurfaceValid() {
		return (mInputSurface != null) && (mInputSurface.isValid());
	}

	public Surface getSurface() {
		synchronized (mSync) {
			return mInputSurface;
		}
	}

	public SurfaceTexture getSurfaceTexture() {
		synchronized (mSync) {
			return mInputTexture;
		}
	}

	/**
	 * 映像サイズを変更要求
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height) {
		if ((width != mImageWidth) || (height != mImageHeight)) {
			mEglTask.offer(REQUEST_UPDATE_SIZE, width, height);
		}
	}
//--------------------------------------------------------------------------------
	@Override
	protected void onBoundsChange(final Rect bounds) {
		super.onBoundsChange(bounds);
		updateTransformMatrix();
	}

	protected EGLBase getEgl() {
		return mEglTask.getEgl();
	}

	protected EGLBase.IContext getContext() {
		return mEglTask.getContext();
	}

	protected boolean isGLES3() {
		return mEglTask.isGLES3();
	}

	protected boolean isOES3() {
		return mEglTask.isOES3();
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
		mDrawer = GLDrawer2D.create(isOES3(), true);
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
		handleReleaseInputSurface();
	}

	@WorkerThread
	protected Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

		switch (request) {
		case REQUEST_DRAW:
			handleDraw();
			break;
		case REQUEST_UPDATE_SIZE:
			handleResize(arg1, arg2);
			break;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
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
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			return;
		}
		// OESテクスチャをオフスクリーン(マスターサーフェース)へ描画
		mDrawer.draw(mTexId, mTexMatrix, 0);
		// オフスクリーンから読み取り
		mWorkBuffer.clear();
		GLES20.glReadPixels(0, 0,
			mImageWidth, mImageHeight,
			GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mWorkBuffer);
		// Bitmapへ代入
		mWorkBuffer.clear();
		synchronized (mBitmap) {
			mBitmap.copyPixelsFromBuffer(mWorkBuffer);
		}
		mUIHandler.removeCallbacks(mInvalidateSelfOnUITask);
		mUIHandler.post(mInvalidateSelfOnUITask);
	}

	/**
	 * invalidateSelfをUIスレッド上で実行しないと例外を投げる端末があるので
	 * UI/メインスレッド上でinvalidateSelfを呼ぶためのRunnable実装
	 */
	private final Runnable mInvalidateSelfOnUITask = new Runnable() {
		@Override
		public void run() {
			invalidateSelf();	// UIスレッド上でないと例外を投げる端末がある
		}
	};

	/**
	 * 映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		if ((mImageWidth != width) || (mImageHeight != height)) {
			mBitmap.reconfigure(width, height, Bitmap.Config.ARGB_8888);
			mWorkBuffer = ByteBuffer.allocateDirect(width * height * 4);
			mImageWidth = width;
			mImageHeight = height;
			updateTransformMatrix();
			if (BuildCheck.isAndroid4_1() && (mInputTexture != null)) {
				// XXX getIntrinsicWidth/getIntrinsicHeightの代わりにmImageWidth/mImageHeightを使うべきかも?
				mInputTexture.setDefaultBufferSize(getIntrinsicWidth(), getIntrinsicHeight());
			}
		}
	}

	/**
	 * 映像入力用SurfaceTexture/Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		synchronized (mSync) {
			mEglTask.makeCurrent();
			handleReleaseInputSurface();
			mEglTask.makeCurrent();
			if (isOES3()) {
				mTexId = com.serenegiant.glutils.es3.GLHelper.initTex(
					GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE0, GLES30.GL_NEAREST);
			} else {
				mTexId = com.serenegiant.glutils.es2.GLHelper.initTex(
					GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			}
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			if (BuildCheck.isAndroid4_1()) {
				// XXX getIntrinsicWidth/getIntrinsicHeightの代わりにmImageWidth/mImageHeightを使うべきかも?
				mInputTexture.setDefaultBufferSize(getIntrinsicWidth(), getIntrinsicHeight());
			}
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
		onCreateSurface(mInputSurface);
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		synchronized (mSync) {
			if (mInputSurface != null) {
				try {
					mInputSurface.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputSurface = null;
				onDestroySurface();
			}
			if (mInputTexture != null) {
				try {
					mInputTexture.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputTexture = null;
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

	private void updateTransformMatrix() {
		@NonNull
		final Rect bounds = getBounds();
		mWidth = bounds.width();
		mHeight = bounds.height();
		final float scaleX = mWidth / (float)mBitmap.getWidth();
		final float scaleY = mHeight / (float)mBitmap.getHeight();
		mTransform.reset();
		mTransform.postScale(scaleX, scaleY);
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
