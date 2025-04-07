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
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.utils.Pool;
import com.serenegiant.utils.ThreadPool;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * 静止画キャプチャ機能を追加したProxyPipeline実装
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
	 * 映像読み取り用のワークビットマップ
	 */
	@Nullable
	private Bitmap mWorkBitmap;

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
	public CapturePipeline(@NonNull final Callback callback) {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ");
		mCallback = callback;
		mCaptureCnt = mNumCaptures = 0;
		mLastCaptureMs = mIntervalsMs = 0L;
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
				getWidth(), getHeight(),
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
			mWorkBuffer = ByteBuffer.allocateDirect(bytes);
		}
		if ((mWorkBitmap == null) || (mWorkBitmap.getByteCount() < bytes)) {
			mWorkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		}
		try {
			if (DEBUG) Log.v(TAG, "texMatrix=" + MatrixUtils.toGLMatrixString(texMatrix) + ",isOES=" + isOES);
			// GLSurfaceを使ったオフスクリーンへバックバッファとしてテクスチャを割り当てる
			final GLSurface readSurface = GLSurface.wrap(isGLES3,
				isOES ? GL_TEXTURE_EXTERNAL_OES : GLConst.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE4, texId, width, height, false);
			try {
				// テクスチャをバックバッファとするオフスクリーンからglReadPixelsでByteBufferへ画像データを読み込む
				readSurface.makeCurrent();
				mWorkBuffer.clear();
				GLUtils.glReadPixels(mWorkBuffer, width, height);
			} finally {
				readSurface.release();
			}
			final Bitmap bitmap = mPool.obtain(width, height);
			if (bitmap != null) {
				try {
					final Matrix matrix = MatrixUtils.toAndroidMatrix(texMatrix);
					if (isOES || !matrix.isIdentity()) {
						if (isOES) {
							// FIXME GL_TEXTURE_EXTERNAL_OESの時はなぜか上下反転させないといけない？
							//       GL_TEXTURE_2Dの時に反転させると結果が一致しない
							MatrixUtils.setMirror(matrix, IMirror.MIRROR_VERTICAL);
						}
						// ワーク用Bitmapへ書き込む
						mWorkBitmap.copyPixelsFromBuffer(mWorkBuffer);
						// テクスチャ変換行列を適用する
						// XXX ここでBitmapが生成されるのを避けるのは困難
						//     ワーク用のBitmapをもう1つ用意してそれをバックバッファとするCanvasを作って
						//     matrixを適用してCanvasへ描画してBitmap#copyPixelsToBufferだといけそうだけど
						//     上下反転以外だと画像がおかしくなりそう
						final Bitmap scaled = Bitmap.createBitmap(mWorkBitmap, 0, 0, width, height, matrix, true);
						// バッファに書き戻す
						mWorkBuffer.clear();
						scaled.copyPixelsToBuffer(mWorkBuffer);
						mWorkBuffer.flip();
						scaled.recycle();
					}
					// コールバック用のBitmapへ書き込む
					bitmap.copyPixelsFromBuffer(mWorkBuffer);
					// これをスレッドプールで呼び出すと優先順位が低くてなかなか呼び出されずにプールが空になってしまう
					mCallback.onCapture(bitmap);
				} catch (final Exception e) {
					Log.w(TAG, e);
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
		} catch (final Exception e) {
			ThreadPool.queueEvent(() -> {
				mCallback.onError(e);
			});
		}
	}

}
