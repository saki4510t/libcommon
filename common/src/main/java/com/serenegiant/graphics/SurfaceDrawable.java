package com.serenegiant.graphics;
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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EglTask;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.system.BuildCheck;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surface/SurfaceTexture経由で受け取った映像を表示するDrawable
 */
public class SurfaceDrawable extends Drawable {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceDrawable.class.getSimpleName();

	/**
	 * 映像取得用のSurfaceの生成時・破棄時のコールバック
	 */
	public interface Callback {
		public void onCreateSurface(@NonNull final Surface surface);
		public void onDestroySurface();
	}

	/**
	 * 描画要求
	 */
	private static final int REQUEST_DRAW = 1;
	/**
	 * 映像サイズ変更要求
	 */
	private static final int REQUEST_UPDATE_SIZE = 2;
	/**
	 * 映像取得用のSurface/Surfacetexture再生成要求
	 */
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	/**
	 * 排他処理用
	 */
	@NonNull
	private final Object mSync = new Object();
	/**
	 * Drawable#invalidateSelfをUIスレッド上で呼び出すためのHandler
	 */
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
	/**
	 * Surface/SurfaceTextureを経由して受け取った映像を保持するBitmap
	 */
	@NonNull
	private final Bitmap mBitmap;
	/**
	 * Drawable#drawでBitmapをDrawableへ描画する際の変換行列
	 */
	@NonNull
	private final Matrix mTransform = new Matrix();
	/**
	 * Drawableへの描画処理に使うPaint実装
	 */
	@NonNull
	private final Paint mPaint = new Paint();

	// ここから下はEGL|GLコンテキスト上でのみアクセスする
	/**
	 * SurfaceTextureからテクスチャとして受け取る映像のテクスチャ変換行列
	 */
	@Size(min=16)
	@NonNull
	final float[] mTexMatrix = new float[16];
	/**
	 * テクスチャの読み込みに使うワーク用バッファ
	 */
	private ByteBuffer mWorkBuffer;
	/**
	 * テクスチャID
	 */
	private int mTexId;
	/**
	 * 映像受け取り用のSurfaceTexture
	 */
	private SurfaceTexture mInputTexture;
	/**
	 * 映像受け取り用のSurface
	 */
	private Surface mInputSurface;
	/**
	 * SurfaceTextureで映像を受け取ったテクスチャをオフスクリーン描画するためのGLSurface
	 * XXX SurfaceTextureへ割り当てたテクスチャをGLSurfaceのバックバッファとしてラップして
	 *     アクセスすることも可能だけどその場合はテクスチャ変換行列を別途Bitmapへ適用することに
	 *     なる=中間に1つ余分にBitmapを生成することになるので、一旦オフスクリーン描画する方が
	 *     パフォーマンス的によい
	 */
	private GLSurface mOffscreenSurface;
	/**
	 * オフスクリーンのGLSurfaceへ描画するためのRendererTarget
	 */
	private RendererTarget mTarget;
	/**
	 * オフスクリーン描画用のGLDrawer
	 */
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

		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:(%dx%d)", imageWidth, imageHeight));
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
		final int bytes = imageWidth * imageHeight * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
		mWorkBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
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
		if (DEBUG) Log.v(TAG, "setAlpha:" + alpha);
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable final ColorFilter filter) {
		if (DEBUG) Log.v(TAG, "setColorFilter:" + filter);
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
	protected void onBoundsChange(@NonNull final Rect bounds) {
		super.onBoundsChange(bounds);
		if (DEBUG) Log.v(TAG, "onBoundsChange:" + bounds);
		synchronized (mSync) {
			updateTransformMatrix();
		}
	}

	protected EGLBase getEgl() {
		return mEglTask.getEgl();
	}

	@Deprecated
	@SuppressWarnings("deprecation")
	protected EGLBase.IContext<?> getContext() {
		return mEglTask.getContext();
	}

	protected boolean isGLES3() {
		return mEglTask.isGLES3();
	}

	protected boolean isOES3Supported() {
		return mEglTask.isOES3Supported();
	}

	protected int getTexId() {
		return mTexId;
	}

	@Size(min=16)
	@NonNull
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
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurface();
	}

	/**
	 * ワーカースレッド上でのイベントハンドラ
	 * @param request
	 * @param arg1
	 * @param arg2
	 * @param obj
	 * @return
	 */
	@WorkerThread
	protected Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

		switch (request) {
		case REQUEST_DRAW -> handleDraw();
		case REQUEST_UPDATE_SIZE -> handleResize(arg1, arg2);
		case REQUEST_RECREATE_MASTER_SURFACE -> handleReCreateInputSurface();
		default -> { if (DEBUG) Log.v(TAG, "handleRequest:" + request); }
		}
		return null;
	}

	private int drawCnt;

	/**
	 * 受け取ったテクスチャをオフスクリーン描画してBitmapへ読み込む
	 */
	@WorkerThread
	protected void handleDraw() {
		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		mEglTask.removeRequest(REQUEST_DRAW);
		try {
			mEglTask.makeCurrent();
			GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			mEglTask.swap();
			return;
		}
		if ((mOffscreenSurface == null) || (mOffscreenSurface.getWidth() != mImageWidth)
			|| (mOffscreenSurface.getHeight() != mHeight)) {
			handleReleaseOffscreen();
			mOffscreenSurface = GLSurface.newInstance(isGLES3(), GLES20.GL_TEXTURE0, mImageWidth, mImageHeight);
			mTarget = RendererTarget.newInstance(getEgl(), mOffscreenSurface, 0.0f);
		}
		if ((mOffscreenSurface != null) && (mTarget != null)) {
			// オフスクリーンSurfaceへ描画
			mTarget.draw(mDrawer, GLES20.GL_TEXTURE0, mTexId, mTexMatrix);
			// オフスクリーンのフレームバッファへ切り替え
			mOffscreenSurface.makeCurrent();
			// オフスクリーンのバックバッファから読み込み
			mWorkBuffer = GLUtils.glReadPixels(mWorkBuffer, mImageWidth, mImageHeight);
		}
		mEglTask.swap();
		// Bitmapへ代入
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
			synchronized (mSync) {
				mBitmap.reconfigure(width, height, Bitmap.Config.ARGB_8888);
				final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
				mWorkBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
				handleReleaseOffscreen();
				mImageWidth = width;
				mImageHeight = height;
				updateTransformMatrix();
			}
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
		mEglTask.makeCurrent();
		handleReleaseInputSurface();
		mEglTask.makeCurrent();
		mDrawer = GLDrawer2D.create(isGLES3(), true);
		mTexId = GLUtils.initTex(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
		mInputTexture = new SurfaceTexture(mTexId);
		mInputSurface = new Surface(mInputTexture);
		if (DEBUG) Log.v(TAG, String.format("handleReCreateInputSurface:video(%dx%d),intrinsic(%dx%d)",
			mImageWidth, mImageHeight, getIntrinsicWidth(), getIntrinsicHeight()));
		if (BuildCheck.isAndroid4_1()) {
			// XXX getIntrinsicWidth/getIntrinsicHeightの代わりにmImageWidth/mImageHeightを使うべきかも?
			mInputTexture.setDefaultBufferSize(getIntrinsicWidth(), getIntrinsicHeight());
		}
		mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		onCreateSurface(mInputSurface);
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		handleReleaseOffscreen();
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
			GLUtils.deleteTex(mTexId);
			mTexId = 0;
		}
	}

	private void handleReleaseOffscreen() {
		if (DEBUG) Log.v(TAG, "handleReleaseOffscreen:");
		if (mTarget != null) {
			mTarget.release();
			mTarget = null;
		}
		if (mOffscreenSurface != null) {
			mOffscreenSurface.release();
			mOffscreenSurface = null;
		}
	}

	/**
	 * Drawable#drawでBitmapをDrawableへ描画する際の変換行列を更新
	 */
	private void updateTransformMatrix() {
		@NonNull
		final Rect bounds = getBounds();
		mWidth = bounds.width();
		mHeight = bounds.height();
		final float scaleX = mWidth / (float)mBitmap.getWidth();
		final float scaleY = mHeight / (float)mBitmap.getHeight();
		mTransform.reset();
		mTransform.postScale(scaleX, scaleY);
		if (DEBUG) Log.v(TAG, "updateTransformMatrix:" + mTransform);
	}

	/**
	 * Callback#onCreateSurface呼び出しのためのヘルパーメソッド
	 * @param surface
	 */
	private void onCreateSurface(@NonNull final Surface surface) {
		if (DEBUG) Log.v(TAG, "onCreateSurface:" + surface);
		mCallback.onCreateSurface(surface);
	}

	/**
	 * Callback#onDestroySurface呼び出しのためのヘルパーメソッド
	 */
	private void onDestroySurface() {
		if (DEBUG) Log.v(TAG, "onDestroySurface:");
		mCallback.onDestroySurface();
	}

	/**
	 * SurfaceTextureが映像を受け取ったときのコールバックリスナー
	 */
	private final SurfaceTexture.OnFrameAvailableListener
		mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onFrameAvailable:");
			mEglTask.offer(REQUEST_DRAW);
		}
	};

}
