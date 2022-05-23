/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EglTask;
import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.GLTexture;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

/**
 * MediaCodecのデコーダーでデコードした動画やカメラからの映像の代わりに、
 * 静止画をSurfaceへ出力するためのクラス
 * ImageTextureSourceと違って複数のsurfaceへ分配描画する
 * 出力先Surfaceが1つだけならImageTextureSourceの方が効率的
 * FIXME GLES30対応を実装する
 */
public class StaticTextureSource implements GLConst, IMirror {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = StaticTextureSource.class.getSimpleName();

	private final Object mSync = new Object();
	private RendererTask mRendererTask;

	/**
	 * ソースの静止画を指定したコンストラクタ, フレームレートは10fps固定
	 * @param bitmap
	 */
	public StaticTextureSource(@Nullable final Bitmap bitmap) {
		this(bitmap, new Fraction(10));
	}

	/**
	 * フレームレート指定付きコンストラクタ
	 * @param fps nullなら10fps
	 */
	public StaticTextureSource(@Nullable final Fraction fps) {
		this(null, fps);
	}

	/**
	 * ソースの静止画とフレームレートを指定可能なコンストラクタ
	 * @param bitmap
	 * @param fps nullなら10fps
	 */
	public StaticTextureSource(@Nullable final Bitmap bitmap, @Nullable final Fraction fps) {
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
		synchronized (mSync) {
			return (mRendererTask != null) && mRendererTask.isRunning();
		}
	}

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		final RendererTask task;
		synchronized (mSync) {
			task = mRendererTask;
			mRendererTask = null;
			mSync.notifyAll();
		}
		if (task != null) {
			task.release();
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
		final RendererTask task;
		synchronized (mSync) {
			task = mRendererTask;
		}
		if (task != null) {
			task.addSurface(id, surface, maxFps);
		}
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		final RendererTask task;
		synchronized (mSync) {
			task = mRendererTask;
		}
		if (task != null) {
			task.removeSurface(id);
		}
	}

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame() {
		final RendererTask task;
		synchronized (mSync) {
			task = mRendererTask;
		}
		if (task != null) {
			task.removeRequest(REQUEST_DRAW);
			task.offer(REQUEST_DRAW);
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
			final RendererTask task;
			synchronized (mSync) {
				task = mRendererTask;
			}
			if (task != null) {
				task.setBitmap(bitmap);
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

	@Override
	public void setMirror(@MirrorMode final int mirror) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "mirror:" + mirror);
		final RendererTask task;
		synchronized (mSync) {
			task = mRendererTask;
		}
		if (task != null) {
			if (task.mMirror != mirror) {
				task.offer(REQUEST_MIRROR, mirror);
			}
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		synchronized (mSync) {
			return mRendererTask != null ? mRendererTask.mMirror : MIRROR_NORMAL;
		}
	}

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_ADD_SURFACE = 3;
	private static final int REQUEST_REMOVE_SURFACE = 4;
	private static final int REQUEST_MIRROR = 6;
	private static final int REQUEST_SET_BITMAP = 7;

	private static class RendererTask extends EglTask {
		private final SparseArray<RendererTarget> mTargets
			= new SparseArray<>();
		private final StaticTextureSource mParent;
		private final long mIntervalsNs;
		private GLDrawer2D mDrawer;
		private int mVideoWidth, mVideoHeight;
		private GLTexture mImageSource;
		@MirrorMode
		private int mMirror = MIRROR_NORMAL;

		public RendererTask(final StaticTextureSource parent,
			final int width, final int height, @Nullable final Fraction fps) {

			super(GLUtils.getSupportedGLVersion(), null, 0);
			mParent = parent;
			mVideoWidth = width;
			mVideoHeight = height;
			final float _fps = fps != null ? fps.asFloat() : 0.0f;
			mIntervalsNs = _fps <= 0 ? 100000000L : (long)(1000000000L / _fps);
		}

		/** 初期化処理 */
		@WorkerThread
		protected void onInit(final int arg1, final int arg2, final Object obj) {
			super.onInit(arg1, arg2, obj);
			if (DEBUG) Log.v(TAG, "onInit:");
		}

		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@WorkerThread
		@Override
		protected void onStart() {
			if (DEBUG) Log.v(TAG, "onStart:");
			mDrawer = GLDrawer2D.create(false, false);		// GL_TEXTURE_EXTERNAL_OESを使わない
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
		protected boolean onError(final Throwable e) {
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
			case REQUEST_MIRROR:
				handleMirror(arg1);
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
		 * @param maxFps 0以下なら未指定, 1000未満ならその値、1000以上なら1000.0fで割ったものを最大フレームレートとする
		 */
		public void addSurface(final int id, final Object surface, final int maxFps) {
			if (DEBUG) Log.v(TAG, "RendererTask#addSurface:id=" + id + ",surface=" + surface);
			checkFinished();
			if (!GLUtils.isSupportedSurface(surface)) {
				throw new IllegalArgumentException(
					"Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
			}
			synchronized (mTargets) {
				while (mTargets.get(id) == null) {
					if (DEBUG) Log.v(TAG, "RendererTask#addSurface:wait for");
					if (offer(REQUEST_ADD_SURFACE, id, maxFps, surface)) {
						try {
							if (DEBUG) Log.v(TAG, "RendererTask#addSurface:mTargets.wait");
							mTargets.wait(1000);
						} catch (final InterruptedException e) {
							break;
						}
						break;
					} else {
						try {
							if (DEBUG) Log.v(TAG, "RendererTask#addSurface:mTargets.wait");
							mTargets.wait(10);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}
			if (DEBUG) Log.v(TAG, "RendererTask#addSurface:finished");
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
						final RendererTarget target = mTargets.valueAt(i);
						if ((target != null) && target.canDraw()) {
							try {
								target.draw(mDrawer, GLES20.GL_TEXTURE0, texId, null); // target.draw(mDrawer, GLES20.GL_TEXTURE0, mTexId, mTexMatrix);
								GLUtils.checkGlError("handleDraw");
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
		 * @param maxFps 0以下なら未指定, 1000未満ならその値、1000以上なら1000.0fで割ったものを最大フレームレートとする
		 */
		@WorkerThread
		private void handleAddSurface(final int id, final Object surface, final int maxFps) {
			if (DEBUG) Log.v(TAG, "handleAddSurface:id=" + id);
			checkTarget();
			synchronized (mTargets) {
				RendererTarget target = mTargets.get(id);
				if (target == null) {
					try {
						target = createRendererTarget(id, getEgl(), surface, maxFps);
						target.setMirror(mMirror);
						mTargets.append(id, target);
					} catch (final Exception e) {
						Log.w(TAG, "invalid surface: surface=" + surface, e);
					}
				} else {
					Log.w(TAG, "surface is already added: id=" + id);
				}
				mTargets.notifyAll();
			}
			if (DEBUG) Log.v(TAG, "handleAddSurface:finished");
		}

		/**
		 * IRendererTargetインスタンスを生成する
		 * このクラスではRendererTarget.newInstanceを呼ぶだけ
		 * @param id
		 * @param egl
		 * @param surface
		 * @param maxFps 0以下なら未指定, 1000未満ならその値、1000以上なら1000.0fで割ったものを最大フレームレートとする
		 * @return
		 */
		private RendererTarget createRendererTarget(final int id,
			@NonNull final EGLBase egl,
			final Object surface, final float maxFps) {
			if (DEBUG) Log.v(TAG, "createRendererTarget:");
			return RendererTarget.newInstance(egl, surface, maxFps > 1000 ? maxFps / 1000.0f : maxFps);
		}

		/**
		 * 指定したIDの分配描画先Surfaceを破棄する
		 * @param id
		 */
		@WorkerThread
		private void handleRemoveSurface(final int id) {
			if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
			synchronized (mTargets) {
				final RendererTarget target = mTargets.get(id);
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
					final RendererTarget target = mTargets.valueAt(i);
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
					final RendererTarget target = mTargets.valueAt(i);
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

		@WorkerThread
		private void handleMirror(@MirrorMode final int mirror) {
			if (DEBUG) Log.v(TAG, "handleMirror:" + mirror);
			mMirror = mirror;
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if (target != null) {
					GLUtils.setMirror(target.getMvpMatrix(), mirror);
				}
			}
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
				mImageSource = GLTexture.newInstance(GLES20.GL_TEXTURE0, width, height);
				GLUtils.checkGlError("handleSetBitmap");
			}
			mImageSource.loadBitmap(bitmap);
			mVideoWidth = width;
			mVideoHeight = height;
		}

	}

	/**
	 * 一定時間おきに描画要求を送るためのRunnable
	 */
	private final Runnable mOnFrameTask = new Runnable() {
		@Override
		public void run() {
			final long ms = mRendererTask.mIntervalsNs / 1000000L;
			final int ns = (int)(mRendererTask.mIntervalsNs % 1000000L);
			while (isRunning()) {
				try {
					final RendererTask task;
					synchronized (mSync) {
						mSync.wait(ms, ns);
						task = mRendererTask;
					}
					if (task != null && task.mImageSource != null) {
						task.removeRequest(REQUEST_DRAW);
						task.offer(REQUEST_DRAW);
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	};

}
