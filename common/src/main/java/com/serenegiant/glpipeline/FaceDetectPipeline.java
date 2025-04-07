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
import android.graphics.Matrix;
import android.media.FaceDetector;
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.HandlerThreadHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 顔検出をしてコールバックするGLPipeline実装
 * パイプライン → FaceDetectPipeline (→ パイプライン)
 *                → 顔認識 → onDetectedコールバック呼び出し
 */
public class FaceDetectPipeline extends ProxyPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = FaceDetectPipeline.class.getSimpleName();

	/**
	 * 最大fps未指定時のデフォルトの顔検出フレームレート
	 */
	private static final float DEFAULT_MAX_FPS = 1.0f;

	/**
	 * 顔検出したときのコールバックリスナー
	 * 任意のスレッド上で実行される
	 * ただしEGL/GLレンダリングコンテキストはないので直接OpenGL|ES関係の処理はできない
	 */
	public interface OnDetectedListener {
		@WorkerThread
		public void onDetected(
			/*@NonNull final Bitmap debugBitmap,*/
			final int num, final FaceDetector.Face[] faces,
			final int width, final int height);
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final GLManager mManager;
	/**
	 * 検出処理を行う最大フレームレート
	 * (処理が間に合わなければこれよりも遅くなる可能性がある)
	 */
	private final float mMaxFps;
	/**
	 * 最大で検出する顔の数
	 */
	private final int mMaxDetectNum;
	/**
	 * コールバックリスなー
	 */
	@NonNull
	private final OnDetectedListener mListener;
	/**
	 * 検出結果を受け取るFace配列
	 */
	private final FaceDetector.Face[] mDetected;
	/**
	 * 顔検出処理を非同期で行うためのHandlerThreadHandler
	 */
	@NonNull
	private final HandlerThreadHandler mAsyncHandler = HandlerThreadHandler.createHandler(TAG);

	/**
	 * 受け取ったテキスチャをオフスクリーンへ転送するためのGLDrawer2D
	 */
	@Nullable
	private GLDrawer2D mDrawer;
	/**
	 * オフスクリーンをラップするRendererTargetオブジェクト
	 * 本質的にはラップせずに自前でフレームレート調整すればいいけど少しでも手間を省くためにラップして使う
	 */
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * 受け取ったテクスチャをBitmapへ変換するためのワーク用のオフスクリーン
	 */
	@Nullable
	private EGLBase.IEglSurface offscreen;
	/**
	 * オフスクリーンからBitmapへ転送する際のワーク用
	 */
	@Nullable
	private ByteBuffer mWorkBuffer;
	/**
	 * 顔検出処理へ引き渡すBitmapオブジェクト
	 */
	@Nullable
	private Bitmap mWorkBitmap;
	/**
	 * 顔検出のためのFaceDetectorオブジェクト
	 */
	@Nullable
	private FaceDetector mDetector;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param maxFps 未指定(null)または0以下の場合はDEFAULT_MAX_FPS(=1fps)になる
	 * @param maxDetectNum 顔検出する最大数, 1以上
	 * @param listener
	 */
	public FaceDetectPipeline(
		@NonNull final GLManager manager,
		@Nullable final Fraction maxFps,
		@IntRange(from=1) final int maxDetectNum,
		@NonNull final OnDetectedListener listener) {

		mManager = manager;
		mMaxFps = maxFps != null ? maxFps.asFloat() : DEFAULT_MAX_FPS;
		mMaxDetectNum = maxDetectNum;
		mListener = listener;
		mDetected = new FaceDetector.Face[maxDetectNum];
	}

	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		if (isValid()) {
			releaseTarget();
			mAsyncHandler.removeCallbacksAndMessages(null);
			mAsyncHandler.quit();
		}
		super.internalRelease();
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

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		if (isActive()) {
			@NonNull
			final GLDrawer2D drawer;
			@Nullable
			final RendererTarget target;
			mLock.lock();
			try {
				if ((mDrawer == null) || isOES != mDrawer.isOES()) {
					// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
					if (mDrawer != null) {
						mDrawer.release();
					}
					if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
					mDrawer = GLDrawer2D.create(mManager.isGLES3(), isOES);
				}
				drawer = mDrawer;
				if ((mRendererTarget == null)
					|| (mRendererTarget.width() != width)
					|| (mRendererTarget.height() != height)) {
					createTarget();
				}
				target = mRendererTarget;
			} finally {
				mLock.unlock();
			}
			if ((target != null) && target.canDraw()) {
				// API1からあるFaceDetectorはBitmapからしか検出できないのでテキスチャをオフスクリーンへ描画して
				// それを読み取ってBitmapに変換して検出処理を行う
				// オフスクリーンへ描画
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				offscreen.makeCurrent();
				// オフスクリーンから読み取る
				mWorkBuffer.clear();
				GLES20.glReadPixels(0, 0,
					width, height,
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mWorkBuffer);
				// Bitmapへ代入
				mWorkBuffer.clear();
				mAsyncHandler.removeCallbacks(mDetectTask);
				mLock.lock();
				try {
					if (mWorkBitmap != null) {
						mWorkBitmap.copyPixelsFromBuffer(mWorkBuffer);
						mAsyncHandler.post(mDetectTask);
					}
				} finally {
					mLock.unlock();
				}
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:" + cnt);
				}
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		// XXX パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
				GLDrawer2D drawer;
				mLock.lock();
				try {
					drawer = mDrawer;
					mDrawer = null;
				} finally {
					mLock.unlock();
				}
				if (drawer != null) {
					drawer.release();
				}
			});
		}
	}

	/**
	 * 描画先のSurfaceを生成
	 */
	@WorkerThread
	private void createTarget() {
		if (DEBUG) Log.v(TAG, "createTarget:");
		final int width = getWidth();
		final int height = getHeight();
		mLock.lock();
		try {
			if (mRendererTarget != null) {
				mRendererTarget.release();
				mRendererTarget = null;
			}
			if (offscreen != null) {
				offscreen.release();
				offscreen = null;
			}
			if (mWorkBitmap != null) {
				mWorkBitmap.recycle();
				mWorkBitmap = null;
			}
			if (DEBUG) Log.v(TAG, "createTarget:create IEglSurface as work offscreen");
			offscreen = mManager.getEgl().createOffscreen(width, height);
			mRendererTarget = RendererTarget.newInstance(
				mManager.getEgl(), offscreen, mMaxFps > 0 ? mMaxFps : DEFAULT_MAX_FPS);
			final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
			mWorkBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
			mWorkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			mDetector = new FaceDetector(width, height, mMaxDetectNum);
		} finally {
			mLock.unlock();
		}
	}

	@WorkerThread
	private void releaseTarget() {
		final GLDrawer2D drawer;
		final RendererTarget target;
		final EGLBase.IEglSurface surface;
		mLock.lock();
		try {
			drawer = mDrawer;
			mDrawer = null;
			target = mRendererTarget;
			mRendererTarget = null;
			surface = offscreen;
			offscreen = null;
			if (mWorkBitmap != null) {
				mWorkBitmap.recycle();
				mWorkBitmap = null;
			}
			mWorkBuffer = null;
			mDetector = null;
		} finally {
			mLock.unlock();
		}
		if ((drawer != null) || (target != null)) {
			if (DEBUG) Log.v(TAG, "releaseTarget:");
			if (mManager.isValid()) {
				try {
					mManager.runOnGLThread(() -> {
						if (drawer != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release drawer");
							drawer.release();
						}
						if (target != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release target");
							target.release();
						}
						if (surface != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release work surface");
							surface.release();
						}
					});
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			} else if (DEBUG) {
				Log.w(TAG, "releaseTarget:unexpectedly GLManager is already released!");
			}
		}
	}

	/**
	 * 顔検出処理を非同期で行うためのRunnable実装
	 */
	private final Runnable mDetectTask = new Runnable() {
		private final Matrix m = new Matrix();
		@WorkerThread
		@Override
		public void run() {
			final Bitmap bitmap565;
			final int width, height;
			mLock.lock();
			try {
				// FaceDetectorはRGB565でないと検出できないので変換＆コピーする
				if (mWorkBitmap != null) {
					bitmap565 = mWorkBitmap.copy(Bitmap.Config.RGB_565, true);
					width = getWidth();
					height = getHeight();
				} else {
					return;
				}
			} finally {
				mLock.unlock();
			}
			// OpenGL|ESから読み取った映像は通常とは上下反転しているのでひっくり返す
			m.preScale(1, -1);
			final Bitmap flippedBitmap = Bitmap.createBitmap(bitmap565, 0, 0, width, height, m, true);
			bitmap565.recycle();
			final int n = mDetector.findFaces(flippedBitmap, mDetected);
			if (n > 0) {
				mListener.onDetected(/*flippedBitmap,*/ n, mDetected, width, height);
			}
			flippedBitmap.recycle();
		}
	};
}
