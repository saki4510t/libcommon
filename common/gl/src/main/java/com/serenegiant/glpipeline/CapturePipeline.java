package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLOffscreen;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.utils.Pool;
import com.serenegiant.utils.ThreadPool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.graphics.IMirror.MIRROR_VERTICAL;

/**
 * 静止画キャプチャ機能を追加したProxyPipeline実装
 * このクラスはアップストリームからのテクスチャを変更せずそのまま次のパイプランへ送る
 * パイプライン → CapturePipeline (→ パイプライン)
 *                → Bitmap → onCaptureコールバック呼び出し
 */
public class CapturePipeline extends ProxyPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = CapturePipeline.class.getSimpleName();

	/**
	 * キャプチャ時のコールバックリスナー
	 */
	public interface Callback {
		/**
		 * キャプチャ時のコールバック
		 * GLスレッド上で呼び出されるので可能な限り早く終了すること
		 * 引数のBitmapはOOM抑制・高速化のためにビットマッププールで管理＆再利用するので
		 * このコールバックを抜けた後にアクセスしないようにすること(必要であればコピー)
		 * &Bitmap#recycleを可能な限り呼び出さないこと
		 * @param bitmap
		 */
		@WorkerThread
		public void onCapture(@NonNull final Bitmap bitmap);

		/**
		 * キャプチャ時にエラーが発生したときのコールバック
		 * ワーカースレッド上で呼び出される
		 * @param t
		 */
		@WorkerThread
		public void onError(@NonNull final Throwable t);
	}

	@NonNull
	private final GLManager mManager;
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final Callback mCallback;

	/**
	 * キャプチャ回数
	 * -1: 無制限, 0: 無効, 1以上: 指定回数
	 */
	private int mNumCaptures;
	/**
	 * キャプチャ周期[ミリ秒]
	 */
	private long mIntervalsMs;

	/**
	 * キャプチャした回数
	 */
	private int mCaptureCnt;
	/**
	 * キャプチャしたシステム時刻[ミリ秒]
	 */
	private long mLastCaptureMs;
	/**
	 * 映像読み取り用のワークバッファ
	 */
	@Nullable
	private ByteBuffer mWorkBuffer;
	/**
	 * モデルビュー変換行列
	 */
	@Size(value=16)
	@NonNull
	private final float[] mMvpMatrix = new float[16];
	@Nullable
	private GLDrawer2D mDrawer;
	/**
	 * オフスクリーン描画用のGLOffscreen
	 */
	@Nullable
	private GLOffscreen mGLOffscreen = null;

	/**
	 * OOM抑制・高速化のためにキャプチャに使うBitmapを管理するビットマッププール
	 */
	private final Pool<Bitmap> mPool = new Pool<>(0, 4) {
		@NonNull
		@Override
		protected Bitmap createObject(@Nullable final Object... args) {
			final int w = (int)args[0];
			final int h = (int)args[1];
			return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		}

		@Override
		public void release(@NonNull final Bitmap bitmap) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			super.release(bitmap);
		}

		@Nullable
		@Override
		public Bitmap obtain(@Nullable final Object... args) {
			final int w = (int)args[0];
			final int h = (int)args[1];
			Bitmap result = super.obtain(args);
			while ((result != null)
				&& isActive()
				&& (result.isRecycled()
					|| (result.getWidth() != w)
					|| (result.getHeight() != h))) {
				// サイズが違う・リサイクルされてしまっているBitmapは返さない
				release(result);
				result = super.obtain(args);
			}
			return result;
		}
	};

	/**
	 * コンストラクタ
	 */
	public CapturePipeline(@NonNull final GLManager manager, @NonNull final Callback callback) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ");
		mManager = manager;
		mCallback = callback;
		mCaptureCnt = mNumCaptures = 0;
		mLastCaptureMs = mIntervalsMs = 0L;
		Matrix.setIdentityM(mMvpMatrix, 0);
	}

	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		releaseAll();
		super.internalRelease();
	}

	public void setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		final GLDrawer2D drawer = mDrawer;
		if (drawer != null) {
			drawer.setMvpMatrix(matrix, offset);
		}
	}

	/**
	 * 1回だけキャプチャ要求
	 */
	public void trigger() {
		trigger(1, 0L);
	}

	/**
	 * 指定した条件でキャプチャ要求
	 * @param numCaptures キャプチャ回数, -1: 無制限, 0: 無効, 1以上: 指定回数
	 * @param intervalsMs 複数回キャプチャする場合の周期[ミリ秒]
	 */
	public void trigger(final int numCaptures, final long intervalsMs) {
		if (DEBUG) Log.v(TAG, "trigger:num=" + numCaptures + ",intervalsMs=" + intervalsMs);
		mLock.lock();
		try {
			mCaptureCnt = 0;
			mNumCaptures = numCaptures;
			mIntervalsMs = intervalsMs;
			mLastCaptureMs = 0L;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * キャプチャ中であればキャンセルする
	 */
	public void cancel() {
		if (DEBUG) Log.v(TAG, "cancel");
		mLock.lock();
		try {
			mCaptureCnt = mNumCaptures = 0;
			mLastCaptureMs = mIntervalsMs = 0L;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		// キャプチャするかどうかを判定
		final long current = System.currentTimeMillis();
		final boolean needCapture;
		mLock.lock();
		try {
			needCapture = (mNumCaptures != 0)
				&& ((mNumCaptures < 0) || (mCaptureCnt < mNumCaptures))
				&& (current - mLastCaptureMs > mIntervalsMs);
			if (needCapture) {
				mLastCaptureMs = current;
				mCaptureCnt++;
			}
		} finally {
			mLock.unlock();
		}
		if (needCapture) {
			doCapture(isGLES3,
				width, height,
				isOES, texId, texMatrix);
		}
	}

	/**
	 * キャプチャを実行
	 * @param isGLES3
	 * @param width
	 * @param height
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 */
	private void doCapture(
		final boolean isGLES3,
		final int width, final int height,
		final boolean isOES,
		final int texId, @NonNull final float[] texMatrix) {

		final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
		if ((mWorkBuffer == null) || (mWorkBuffer.capacity() < bytes)) {
			mWorkBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
		}
		try {
			if (DEBUG) Log.v(TAG, "doCapture:texMatrix=" + MatrixUtils.toGLMatrixString(texMatrix) + ",isOES=" + isOES);
			mManager.makeDefault();
			if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
				// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "doCapture:create GLDrawer2D,mvpMatrix=" + MatrixUtils.toGLMatrixString(mMvpMatrix));
				mDrawer = GLDrawer2D.create(isGLES3, isOES);
				mDrawer.setMvpMatrix(mMvpMatrix, 0);
				if (!isOES) {
					if (DEBUG) Log.v(TAG, "doCapture:flip vertical");
					// XXX DrawerPipelineTestでGL_TEXTURE_2D/GL_TEXTURE_EXTERNAL_OESを映像ソースとして
					//     GLUtils#glCopyTextureToBitmapでBitmap変換時のテクスチャ変換行列適用と
					//     DrawerPipelineを0, 1, 2, 3個連結した場合の結果から全ての組み合わせでテストが通るのは、
					//     GLUtils#glCopyTextureToBitmapとは逆で、
					//     ・GL_TEXTURE_EXTERNAL_OESの時はそのまま
					//     ・GL_TEXTURE_2Dの時は上下反転させないとだめみたい
					mDrawer.setMirror(MIRROR_VERTICAL);
				}
			}
			if ((mGLOffscreen == null)
				|| ((mGLOffscreen.getWidth() != width) || (mGLOffscreen.getHeight() != height))) {
				// オフスクリーン描画用のGLOffscreenが存在しないかリサイズされたとき
				if (mGLOffscreen != null) {
					mGLOffscreen.release();
				}
				if (DEBUG) Log.v(TAG, "doCapture:create GLOffscreen");
				mManager.makeDefault();
				mGLOffscreen = GLOffscreen.newInstance(
					isGLES3, GLES20.GL_TEXTURE0,
					width, height);
			}
			final GLDrawer2D drawer = mDrawer;
			final GLOffscreen offscreen = mGLOffscreen;
			if ((drawer != null) && (offscreen != null)) {
				// オフスクリーンSurfaceへ描画
				offscreen.makeCurrent();
				// 本来は映像が全面に描画されるので#glClearでクリアする必要はないけど
				// ハングアップする機種があるのでクリアしとく
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
				// テキスチャ変換行列とモデルビュー変換行列で描画
				drawer.draw(GLES20.GL_TEXTURE0, texId, texMatrix, 0);
				// glReadPixelsでBitmapへ読み込む
				mWorkBuffer.clear();
				GLUtils.glReadPixels(mWorkBuffer, width, height);
				offscreen.swap();
				final Bitmap bitmap = mPool.obtain(width, height);
				if (bitmap != null) {
					try {
						// コールバック用のBitmapへ書き込む
						bitmap.copyPixelsFromBuffer(mWorkBuffer);
						// これをスレッドプールで呼び出すと優先順位が低くてなかなか呼び出されずにプールが空になってしまう
						mCallback.onCapture(bitmap);
					} finally {
						if (bitmap.isRecycled()) {
							mPool.release(bitmap);
						} else {
							mPool.recycle(bitmap);
						}
					}
				} else if (DEBUG) {
					Log.d(TAG, "doCapture:couldn't get bitmap from pool");
				}
			}
		} catch (final Exception e) {
			ThreadPool.queueEvent(() -> {
				mCallback.onError(e);
			});
		}
	}

	private void releaseAll() {
		if (DEBUG) Log.v(TAG, "releaseAll:");
		if (mManager.isValid()) {
			final CountDownLatch latch = new CountDownLatch(1);
			try {
				mManager.runOnGLThread(() -> {
					if (DEBUG) Log.v(TAG, "releaseAll#run:");
					try {
						mManager.makeDefault();
						if (mDrawer != null) {
							mDrawer.release();
							mDrawer = null;
						}
						if (mGLOffscreen != null) {
							mGLOffscreen.release();
							mGLOffscreen = null;
						}
					} finally {
						latch.countDown();
					}
				});
				if (!latch.await(1000L, TimeUnit.MILLISECONDS)) {
					Log.v(TAG, "releaseAll:timeout");
				}
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		} else if (DEBUG) {
			Log.w(TAG, "releaseAll:unexpectedly GLManager is already released!");
		}
	}

}
