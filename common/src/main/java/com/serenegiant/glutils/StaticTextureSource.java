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

package com.serenegiant.glutils;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.glutils.es2.GLHelper;

/**
 * MediaCodecのデコーダーでデコードした動画やカメラからの映像の代わりに、
 * 静止画をSurfaceへ出力するためのクラス
 * FIXME GLES30対応を実装する
 */
public class StaticTextureSource {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = StaticTextureSource.class.getSimpleName();

	private final Object mSync = new Object();
	private RendererTask mRendererTask;
	private volatile boolean isRunning;

	/**
	 * フレームレート指定付きコンストラクタ
	 * @param fps
	 */
	public StaticTextureSource(final float fps) {
		this(null, fps);
	}

	/**
	 * ソースの静止画を指定したコンストラクタ, フレームレートは10fps固定
	 * @param bitmap
	 */
	public StaticTextureSource(@Nullable final Bitmap bitmap) {
		this(bitmap, 10.0f);
	}

	/**
	 * ソースの静止画とフレームレートを指定可能なコンストラクタ
	 * @param bitmap
	 * @param fps
	 */
	public StaticTextureSource(@Nullable final Bitmap bitmap, final float fps) {
		final int width = bitmap != null ? bitmap.getWidth() : 1;
		final int height = bitmap != null ? bitmap.getHeight() : 1;
		mRendererTask = new RendererTask(this, width, height, fps);
		new Thread(mRendererTask, TAG).start();
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
		setBitmap(bitmap);
	}

	/**
	 * 実行中かどうか
	 * @return
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		isRunning = false;
		synchronized (mSync) {
			mSync.notifyAll();
		}
		if (mRendererTask != null) {
			mRendererTask.release();
		}
		synchronized (mSync) {
			mRendererTask = null;
			mSync.notifyAll();
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

		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		synchronized (mSync) {
			mRendererTask.addSurface(id, surface);
		}
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
		synchronized (mSync) {
			mRendererTask.addSurface(id, surface, maxFps);
		}
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		synchronized (mSync) {
			mRendererTask.removeSurface(id);
		}
	}

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame() {
		synchronized (mSync) {
			mRendererTask.removeRequest(REQUEST_DRAW);
			mRendererTask.offer(REQUEST_DRAW);
			mSync.notify();
		}
	}

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount() {
		return mRendererTask.getCount();
	}

	/**
	 * ソース静止画を指定
	 * 既にセットされていれば古いほうが破棄される
	 * @param bitmap nullなら何もしない
	 */
	public void setBitmap(final Bitmap bitmap) {
		if (DEBUG) Log.v(TAG, "setBitmap:bitmap=" + bitmap);
		if (bitmap != null) {
			synchronized (mSync) {
				mRendererTask.setBitmap(bitmap);
			}
		}
	}

	/**
	 * ソース静止画の幅を取得
	 * @return 既にreleaseされていれば0
	 */
	public int getWidth() {
		synchronized (mSync) {
			return mRendererTask != null ? mRendererTask.mVideoWidth : 0;
		}
	}

	/**
	 * ソース静止画の高さを取得
	 * @return  既にreleaseされていれば0
	 */
	public int getHeight() {
		synchronized (mSync) {
			return mRendererTask != null ? mRendererTask.mVideoHeight : 0;
		}
	}

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_ADD_SURFACE = 3;
	private static final int REQUEST_REMOVE_SURFACE = 4;
	private static final int REQUEST_SET_BITMAP = 7;

	private static class RendererTask extends EglTask {
		private final SparseArray<IRendererTarget> mTargets
			= new SparseArray<>();
		private final StaticTextureSource mParent;
		private final long mIntervalsNs;
		private GLDrawer2D mDrawer;
		private int mVideoWidth, mVideoHeight;
		private GLSurface mImageSource;

		public RendererTask(final StaticTextureSource parent,
			final int width, final int height, final float fps) {

			super(GLUtils.getSupportedGLVersion(), null, 0);
			mParent = parent;
			mVideoWidth = width;
			mVideoHeight = height;
			mIntervalsNs = fps <= 0 ? 100000000L : (long)(1000000000L / fps);
		}

		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@WorkerThread
		@Override
		protected void onStart() {
			if (DEBUG) Log.v(TAG, "onStart:");
			mDrawer = GLDrawer2D.create(false, false);		// GL_TEXTURE_EXTERNAL_OESを使わない
			synchronized (mParent.mSync) {
				mParent.isRunning = true;
				mParent.mSync.notifyAll();
			}
			new Thread(mParent.mOnFrameTask, TAG).start();
			if (DEBUG) Log.v(TAG, "onStart:finished");
		}

		/**
		 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
		 */
		@WorkerThread
		@Override
		protected void onStop() {
			if (DEBUG) Log.v(TAG, "onStop");
			synchronized (mParent.mSync) {
				mParent.isRunning = false;
				mParent.mSync.notifyAll();
			}
			makeCurrent();
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			if (mImageSource != null) {
				mImageSource.release();
				mImageSource = null;
			}
			handleRemoveAll();
			if (DEBUG) Log.v(TAG, "onStop:finished");
		}

		@Override
		protected boolean onError(final Exception e) {
			if (DEBUG) Log.w(TAG, e);
			return false;
		}

		@WorkerThread
		@Override
		protected Object processRequest(final int request,
			final int arg1, final int arg2, final Object obj) {

			switch (request) {
			case REQUEST_DRAW:
				handleDraw();
				break;
			case REQUEST_ADD_SURFACE:
				handleAddSurface(arg1, obj, arg2);
				break;
			case REQUEST_REMOVE_SURFACE:
				handleRemoveSurface(arg1);
				break;
			case REQUEST_SET_BITMAP:
				handleSetBitmap((Bitmap)obj);
				break;
			}
			return null;
		}

		/**
		 * 分配描画用のSurfaceを追加
		 * @param id
		 * @param surface
		 */
		public void addSurface(final int id, final Object surface) {
			addSurface(id, surface, -1);
		}

		/**
		 * 分配描画用のSurfaceを追加
		 * @param id
		 * @param surface
		 */
		public void addSurface(final int id, final Object surface, final int maxFps) {
			checkFinished();
			if (!GLUtils.isSupportedSurface(surface)) {
				throw new IllegalArgumentException(
					"Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
			}
			synchronized (mTargets) {
				if (mTargets.get(id) == null) {
					for ( ; ; ) {
						if (offer(REQUEST_ADD_SURFACE, id, maxFps, surface)) {
							try {
								mTargets.wait();
							} catch (final InterruptedException e) {
								// ignore
							}
							break;
						} else {
							try {
								mTargets.wait(10);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}
			}
		}

		/**
		 * 分配描画用のSurfaceを削除
		 * @param id
		 */
		public void removeSurface(final int id) {
			synchronized (mTargets) {
				if (mTargets.get(id) != null) {
					for ( ; ; ) {
						if (offer(REQUEST_REMOVE_SURFACE, id)) {
							try {
								mTargets.wait();
							} catch (final InterruptedException e) {
								// ignore
							}
							break;
						} else {
							try {
								mTargets.wait(10);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}
			}
		}

		/**
		 * ソース静止画をセット
		 * @param bitmap
		 */
		public void setBitmap(@NonNull final Bitmap bitmap) {
			offer(REQUEST_SET_BITMAP, bitmap);
		}

		/**
		 * 分配描画用のSurfaceの数を取得
		 * @return
		 */
		public int getCount() {
			synchronized (mTargets) {
				return mTargets.size();
			}
		}

		private void checkFinished() {
			if (isFinished()) {
				throw new RuntimeException("already finished");
			}
		}

//================================================================================
// ワーカースレッド上での処理
//================================================================================
		/**
		 * 実際の描画処理
		 */
		@WorkerThread
		private void handleDraw() {
//			if (DEBUG) Log.v(TAG, "handleDraw:");
			makeCurrent();
			// 各Surfaceへ描画する
			if (mImageSource != null) {
				final int texId = mImageSource.getTexId();
				synchronized (mTargets) {
					final int n = mTargets.size();
					for (int i = n - 1; i >= 0; i--) {
						final IRendererTarget target = mTargets.valueAt(i);
						if ((target != null) && target.canDraw()) {
							try {
								target.draw(mDrawer, texId, null); // target.draw(mDrawer, mTexId, mTexMatrix);
								GLHelper.checkGlError("handleDraw");
							} catch (final Exception e) {
								// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
								mTargets.removeAt(i);
								target.release();
							}
						}
					}
				}
			} else {
				Log.w(TAG, "mImageSource is not ready");
			}
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glFlush();
//			if (DEBUG) Log.v(TAG, "handleDraw:finish");
		}

		/**
		 * 指定したIDの分配描画先Surfaceを追加する
		 * @param id
		 * @param surface
		 */
		@WorkerThread
		private void handleAddSurface(final int id, final Object surface, final int maxFps) {
			if (DEBUG) Log.v(TAG, "handleAddSurface:id=" + id);
			checkTarget();
			synchronized (mTargets) {
				IRendererTarget target = mTargets.get(id);
				if (target == null) {
					try {
						target = createRendererTarget(id, getEgl(), surface, maxFps);
						mTargets.append(id, target);
					} catch (final Exception e) {
						Log.w(TAG, "invalid surface: surface=" + surface, e);
					}
				} else {
					Log.w(TAG, "surface is already added: id=" + id);
				}
				mTargets.notifyAll();
			}
		}

		/**
		 * IRendererTargetインスタンスを生成する
		 * このクラスではRendererTarget.newInstanceを呼ぶだけ
		 * @param id
		 * @param egl
		 * @param surface
		 * @param maxFps
		 * @return
		 */
		protected IRendererTarget createRendererTarget(final int id,
			@NonNull final EGLBase egl,
			final Object surface, final int maxFps) {

			return RendererTarget.newInstance(getEgl(), surface, maxFps);
		}

		/**
		 * 指定したIDの分配描画先Surfaceを破棄する
		 * @param id
		 */
		@WorkerThread
		private void handleRemoveSurface(final int id) {
			if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
			synchronized (mTargets) {
				final IRendererTarget target = mTargets.get(id);
				if (target != null) {
					mTargets.remove(id);
					target.release();
				}
				checkTarget();
				mTargets.notifyAll();
			}
		}

		/**
		 * 念の為に分配描画先のSurfaceを全て破棄する
		 */
		@WorkerThread
		private void handleRemoveAll() {
			if (DEBUG) Log.v(TAG, "handleRemoveAll:");
			synchronized (mTargets) {
				final int n = mTargets.size();
				for (int i = 0; i < n; i++) {
					final IRendererTarget target = mTargets.valueAt(i);
					if (target != null) {
						makeCurrent();
						target.release();
					}
				}
				mTargets.clear();
			}
			if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
		}

		/**
		 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
		 */
		@WorkerThread
		private void checkTarget() {
			if (DEBUG) Log.v(TAG, "checkTarget");
			synchronized (mTargets) {
				final int n = mTargets.size();
				for (int i = 0; i < n; i++) {
					final IRendererTarget target = mTargets.valueAt(i);
					if ((target != null) && !target.isValid()) {
						final int id = mTargets.keyAt(i);
						if (DEBUG) Log.i(TAG, "checkTarget:found invalid surface:id=" + id);
						mTargets.valueAt(i).release();
						mTargets.remove(id);
					}
				}
			}
			if (DEBUG) Log.v(TAG, "checkTarget:finished");
		}

		/**
		 * ソース静止画をセット
		 * @param bitmap
		 */
		@WorkerThread
		private void handleSetBitmap(final Bitmap bitmap) {
			if (DEBUG) Log.v(TAG, "handleSetBitmap:bitmap=" + bitmap);
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			if (mImageSource == null) {
				mImageSource = GLSurface.newInstance(false, width, height, false);
				GLHelper.checkGlError("handleSetBitmap");
				mImageSource.loadBitmap(bitmap);
			} else {
				mImageSource.loadBitmap(bitmap);
			}
			mVideoWidth = width;
			mVideoHeight = height;
		}

	}

	/**
	 * 一定時間おきに描画要求を送るためのRunnable
	 */
	private Runnable mOnFrameTask = new Runnable() {
		@Override
		public void run() {
			final long ms = mRendererTask.mIntervalsNs / 1000000L;
			final int ns = (int)(mRendererTask.mIntervalsNs % 1000000L);
			for (; isRunning; ) {
				if (mRendererTask == null) break;
				synchronized (mSync) {
					try {
						mSync.wait(ms, ns);
						if (mRendererTask.mImageSource != null) {
							mRendererTask.removeRequest(REQUEST_DRAW);
							mRendererTask.offer(REQUEST_DRAW);
							mSync.notify();
						}
					} catch (Exception e) {
						Log.w(TAG, e);
					}
				}
			}
		}
	};

}
