package com.serenegiant.glutils;
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
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;

import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.ThreadUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.IRendererCommon.*;

public abstract class AbstractDistributeTask {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractDistributeTask.class.getSimpleName();

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_ADD_SURFACE = 3;
	private static final int REQUEST_REMOVE_SURFACE = 4;
	private static final int REQUEST_REMOVE_SURFACE_ALL = 12;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
	private static final int REQUEST_MIRROR = 6;
	private static final int REQUEST_ROTATE = 7;
	private static final int REQUEST_CLEAR = 8;
	private static final int REQUEST_CLEAR_ALL = 9;
	private static final int REQUEST_SET_MVP = 10;

	@NonNull
	private final SparseArray<IRendererTarget>
		mTargets = new SparseArray<>();
	private int mVideoWidth, mVideoHeight;
	/**
	 * Choreographerによるvsync同期して映像更新するかどうか
	 */
	private final boolean mEnableVSync;
	private final Handler mChoreographerHandler;
	@IRendererCommon.MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mRotation = 0;
	private volatile boolean isFirstFrameRendered;
	private volatile boolean mHasNewFrame;
	private volatile boolean mReleased;
	protected GLDrawer2D mDrawer;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param enableVSync Choreographerを使ってvsync同期して映像更新するかどうか
	 */
	protected AbstractDistributeTask(final int width, final int height,
		final boolean enableVSync) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:enableVSync=" + enableVSync);
		mVideoWidth = width > 0 ? width : 640;
		mVideoHeight = height > 0 ? height : 480;
		mEnableVSync = enableVSync;
		mReleased = false;
		if (enableVSync) {
			mChoreographerHandler = HandlerThreadHandler.createHandler(TAG);
		} else {
			mChoreographerHandler = null;
		}
	}

	/**
	 * 関連するリソースを破棄する
	 */
	public synchronized void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (!mReleased) {
			mReleased = true;
			if (mChoreographerHandler != null) {
				try {
					mChoreographerHandler.removeCallbacksAndMessages(null);
					mChoreographerHandler.getLooper().quit();
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
		}
	}

	/**
	 * 描画要求する
	 */
	public void requestFrame() {
		mHasNewFrame = isFirstFrameRendered = true;
		if (!mEnableVSync) {
			// vsync同期しないときはここで描画要求する
			// vsync動悸するときはChoreographer.FrameCallbackから描画要求する
			offer(REQUEST_DRAW, 0, 0, null);
		}
	}

	/**
	 * 映像受け取り用のマスターサーフェースの再生成要求する
	 */
	public void requestRecreateMasterSurface() {
		offer(REQUEST_RECREATE_MASTER_SURFACE);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 */
	public void addSurface(final int id, final Object surface)
		throws IllegalStateException, IllegalArgumentException {

		addSurface(id, surface, -1);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 */
	public void addSurface(final int id,
		final Object surface, final int maxFps)
			throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "addSurface:" + id);
		checkFinished();
		if (!GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException(
				"Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
		}
		synchronized (mTargets) {
			if (mTargets.get(id) == null) {
				for ( ; isRunning() ; ) {
					if (offer(REQUEST_ADD_SURFACE, id, maxFps, surface)) {
						try {
							mTargets.wait();
						} catch (final InterruptedException e) {
							// ignore
						}
						break;
					} else {
						// キューに追加できなかった時は待機する
						try {
							mTargets.wait(5);
						} catch (final InterruptedException e) {
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:" + id);
		synchronized (mTargets) {
			if (mTargets.get(id) != null) {
				for ( ; isRunning() ; ) {
					if (offer(REQUEST_REMOVE_SURFACE, id)) {
						try {
							mTargets.wait();
						} catch (final InterruptedException e) {
							// ignore
						}
						break;
					} else {
						// キューに追加できなかった時は待機する
						try {
							mTargets.wait(5);
						} catch (final InterruptedException e) {
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 分配描画用のSurfaceを全て削除する
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll() {
		if (DEBUG) Log.v(TAG, "removeSurfaceAll:");
		synchronized (mTargets) {
			for ( ; isRunning() ; ) {
				if (offer(REQUEST_REMOVE_SURFACE_ALL)) {
					try {
						mTargets.wait();
					} catch (final InterruptedException e) {
						// ignore
					}
					break;
				} else {
					// キューに追加できなかった時は待機する
					try {
						mTargets.wait(5);
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
		}
	}

	/**
	 * 指定したIDの分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	public void clearSurface(final int id, final int color)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "clearSurface:" + id + ",cl=" + color);
		checkFinished();
		offer(REQUEST_CLEAR, id, color);
	}

	public void clearSurfaceAll(final int color)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "clearSurfaceAll:" + color);
		checkFinished();
		offer(REQUEST_CLEAR_ALL, color);
	}

	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix)
			throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setMvpMatrix:" + id);
		checkFinished();
		if (matrix.length >= offset + 16) {
			offer(REQUEST_SET_MVP, id, offset, matrix);
		} else {
			throw new IllegalArgumentException("matrix is too small, should be longer than offset + 16");
		}
	}

	public boolean isEnabled(final int id) {
		if (DEBUG) Log.v(TAG, "isEnabled:" + id);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			return target != null && target.isEnabled();
		}
	}

	public void setEnabled(final int id, final boolean enable) {
		if (DEBUG) Log.v(TAG, "setEnabled:" + id + ",enable=" + enable);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if (target != null) {
				target.setEnabled(enable);
			}
		}
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

	/**
	 * リサイズ
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height)
		throws IllegalStateException {

		checkFinished();
		if ( ((width > 0) && (height > 0))
			&& ((mVideoWidth != width) || (mVideoHeight != height)) ) {

			offer(REQUEST_UPDATE_SIZE, width, height);
		}
	}

	public void mirror(final int mirror) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "mirror:" + mirror);
		checkFinished();
		if (mMirror != mirror) {
			offer(REQUEST_MIRROR, mirror);
		}
	}

	@IRendererCommon.MirrorMode
	public int mirror() {
		return mMirror;
	}

	public int width() {
		return mVideoWidth;
	}

	public int height() {
		return mVideoHeight;
	}

	/**
	 * vsyncに同期して描画要求を行うためのChoreographer.FrameCallback実装
	 */
	private final Choreographer.FrameCallback mFrameCallback
		= new Choreographer.FrameCallback() {
		@Override
		public void doFrame(final long frameTimeNanos) {
			offer(REQUEST_DRAW, 0, 0, null);
			if (!mReleased && isRunning()) {
				Choreographer.getInstance().postFrameCallback(this);
			}
		}
	};

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
//--------------------------------------------------------------------------------
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
		internalOnStart();
		notifyParent(true);
		if ((mEnableVSync) && (mChoreographerHandler != null)) {
			mChoreographerHandler.post(new Runnable() {
				@Override
				public void run() {
					Choreographer.getInstance().postFrameCallback(mFrameCallback);
				}
			});
		}
//		if (DEBUG) Log.v(TAG, "handleOnStart:finished");
	}

	@WorkerThread
	protected void internalOnStart() {
		if (DEBUG) Log.v(TAG, "internalOnStart:");
		mDrawer = GLDrawer2D.create(isOES3(), true);
		handleReCreateInputSurface();
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStop() {
		if (DEBUG) Log.v(TAG, "onStop");
		if ((mEnableVSync) && (mChoreographerHandler != null)) {
			mChoreographerHandler.post(new Runnable() {
				@Override
				public void run() {
					Choreographer.getInstance().removeFrameCallback(mFrameCallback);
				}
			});
		}
		notifyParent(false);
		makeCurrent();
		internalOnStop();
		handleRemoveAll();
//		if (DEBUG) Log.v(TAG, "onStop:finished");
	}

	@WorkerThread
	protected void internalOnStop() {
		if (DEBUG) Log.v(TAG, "internalOnStop:");
		handleReleaseInputSurface();
		handleRemoveAll();
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
	}

	@WorkerThread
	protected boolean handleOnError(final Exception e) {
		if (DEBUG) Log.w(TAG, e);
		return false;
	}

	@WorkerThread
	protected Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

//		if (DEBUG) Log.v(TAG, "handleRequest:" + request);
		switch (request) {
		case REQUEST_DRAW:
			handleDraw();
			break;
		case REQUEST_UPDATE_SIZE:
			handleResize(arg1, arg2);
			break;
		case REQUEST_ADD_SURFACE:
			handleAddSurface(arg1, obj, arg2);
			break;
		case REQUEST_REMOVE_SURFACE:
			handleRemoveSurface(arg1);
			break;
		case REQUEST_REMOVE_SURFACE_ALL:
			handleRemoveAll();
			break;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
			break;
		case REQUEST_MIRROR:
			handleMirror(arg1);
			break;
		case REQUEST_ROTATE:
			handleRotate(arg1, arg2);
			break;
		case REQUEST_CLEAR:
			handleClear(arg1, arg2);
			break;
		case REQUEST_CLEAR_ALL:
			handleClearAll(arg1);
			break;
		case REQUEST_SET_MVP:
			handleSetMvp(arg1, arg2, (float[])obj);
			break;
		}
		return null;
	}

//	private int drawCnt;
	/**
	 * 実際の描画処理
	 */
	@WorkerThread
	protected void handleDraw() {
//		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		removeRequest(REQUEST_DRAW);
		if (!isMasterSurfaceValid()) {
			Log.e(TAG, "handleDraw:invalid master surface");
			offer(REQUEST_RECREATE_MASTER_SURFACE);
			return;
		}

		if (isFirstFrameRendered) {
			try {
				makeCurrent();
				if (mHasNewFrame) {
					mHasNewFrame = false;
					handleUpdateTexture();
					if (isGLES3()) {
						GLES30.glFlush();
					} else {
						GLES20.glFlush();
					}
					ThreadUtils.NoThrowSleep(0, 0);
				}
			} catch (final Exception e) {
				Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			handleDrawTargets(getTexId(), getTexMatrix());
		}

		// Egl保持用のSurfaceへ描画しないとデッドロックする端末対策
		makeCurrent();
		if (isGLES3()) {
			GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
			GLES30.glFlush();	// これなくても良さそう?
		} else {
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glFlush();	// これなくても良さそう?
		}
		if (isFirstFrameRendered) {
			callOnFrameAvailable();
		}
	}

	/**
	 * 各Surfaceへ描画する
	 */
	@WorkerThread
	protected void handleDrawTargets(final int texId, @NonNull final float[] texMatrix) {
//		if (DEBUG) Log.v(TAG, "handleDrawTargets:");
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = n - 1; i >= 0; i--) {
				final IRendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.canDraw()) {
					try {
						onDrawTarget(target, texId, texMatrix);
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
						// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
						mTargets.removeAt(i);
						target.release();
					}
				}
			}
		}
	}

	/**
	 * Surface1つの描画処理
	 * @param target
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	protected void onDrawTarget(@NonNull final IRendererTarget target,
		final int texId, @NonNull final float[] texMatrix) {

//		if (DEBUG) Log.v(TAG, "onDrawTarget:");
		target.draw(mDrawer, texId, texMatrix);
	}

	/**
	 * 映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		mVideoWidth = width;
		mVideoHeight = height;
	}

	/**
	 * 指定したIDの分配描画先Surfaceを追加する
	 * @param id
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param maxFps
	 */
	@WorkerThread
	protected void handleAddSurface(final int id,
		final Object surface, final int maxFps) {

		if (DEBUG) Log.v(TAG, "handleAddSurface:" + id);
		checkTarget();
		synchronized (mTargets) {
			IRendererTarget target = mTargets.get(id);
			if (target == null) {
				try {
					target = createRendererTarget(id, getEgl(), surface, maxFps);
					GLUtils.setMirror(target.getMvpMatrix(), mMirror);
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
	@NonNull
	protected IRendererTarget createRendererTarget(final int id,
		@NonNull final EGLBase egl,
		@NonNull final Object surface, final int maxFps) {

		if (DEBUG) Log.v(TAG, "createRendererTarget:" + id);
		return RendererTarget.newInstance(getEgl(), surface, maxFps);
	}

	/**
	 * 指定したIDの分配描画先Surfaceを破棄する
	 * @param id
	 */
	@WorkerThread
	protected void handleRemoveSurface(final int id) {
		if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if (target != null) {
				mTargets.remove(id);
				if (target.isValid()) {
					target.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
				}
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
	protected void handleRemoveAll() {
		if (DEBUG) Log.v(TAG, "handleRemoveAll:");
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final IRendererTarget target = mTargets.valueAt(i);
				if (target != null) {
					if (target.isValid()) {
						target.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
					}
					target.release();
				}
			}
			mTargets.clear();
			mTargets.notifyAll();
		}
		if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
	}

	/**
	 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
	 */
	@WorkerThread
	protected void checkTarget() {
		if (DEBUG) Log.v(TAG, "checkTarget:");
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
	 * 指定したIDの分配描画用Surfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	@WorkerThread
	protected void handleClear(final int id, final int color) {
		if (DEBUG) Log.v(TAG, "handleClear:" + id);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				target.clear(color);
			}
		}
	}

	/**
	 * 分配描画用Surface全てを指定した色で塗りつぶす
	 * @param color
	 */
	@WorkerThread
	protected void handleClearAll(final int color) {
		if (DEBUG) Log.v(TAG, "handleClearAll:");
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final IRendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.isValid()) {
					target.clear(color);
				}
			}
		}
	}

	/**
	 * モデルビュー変換行列を適用する
	 * @param id
	 * @param offset
	 * @param mvp
	 */
	@WorkerThread
	protected void handleSetMvp(final int id,
		final int offset, @NonNull final float[] mvp) {

		if (DEBUG) Log.v(TAG, "handleSetMvp:" + id);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				System.arraycopy(mvp, offset, target.getMvpMatrix(), 0, 16);
			}
		}
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	@WorkerThread
	protected void handleMirror(final int mirror) {
		if (DEBUG) Log.v(TAG, "handleMirror:" + mirror);
		mMirror = mirror;
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final IRendererTarget target = mTargets.valueAt(i);
				if (target != null) {
					GLUtils.setMirror(target.getMvpMatrix(), mirror);
				}
			}
		}
	}

	/**
	 * 描画する映像の回転を設定
	 * @param id
	 * @param degree
	 */
	@WorkerThread
	protected void handleRotate(final int id, final int degree) {
		if (DEBUG) Log.v(TAG, "handleRotate:" + id);
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if (target != null) {
				GLUtils.setRotation(target.getMvpMatrix(), degree);
			}
		}
	}

	protected void checkFinished() throws IllegalStateException {
		if (isFinished()) {
			throw new IllegalStateException("already finished");
		}
	}

//--------------------------------------------------------------------------------
	public abstract void start(final String tag);
	public abstract boolean waitReady();
	public abstract boolean isRunning();
	public abstract boolean isFinished();
	public abstract boolean offer(final int request);
	public abstract boolean offer(final int request, final Object obj);
	public abstract boolean offer(final int request, final int arg1);
	public abstract boolean offer(final int request, final int arg1, final int arg2);
	public abstract boolean offer(final int request, final int arg1, final int arg2, final Object obj);
	public abstract void removeRequest(final int request);

	public abstract EGLBase getEgl();
	public abstract GLContext getGLContext();
	public abstract EGLBase.IContext getContext();
	public abstract void makeCurrent();
	public abstract boolean isGLES3();
	public abstract boolean isOES3();

	public abstract boolean isMasterSurfaceValid();
	public abstract int getTexId();
	public abstract float[] getTexMatrix();
	/**
	 * 映像入力用Surfaceを再生成する
	 */
	@WorkerThread
	protected abstract void handleReCreateInputSurface();
	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@WorkerThread
	protected abstract void handleReleaseInputSurface();
	/**
	 * テクスチャを更新
	 */
	@WorkerThread
	protected abstract void handleUpdateTexture();

	public abstract void notifyParent(final boolean isRunning);
	public abstract void callOnFrameAvailable();

}
