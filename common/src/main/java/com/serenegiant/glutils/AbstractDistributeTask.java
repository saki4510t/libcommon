package com.serenegiant.glutils;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.IRendererCommon.*;

public abstract class AbstractDistributeTask {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = AbstractDistributeTask.class.getSimpleName();

	public static final int REQUEST_DRAW = 1;
	public static final int REQUEST_ADD_SURFACE = 3;
	public static final int REQUEST_REMOVE_SURFACE = 4;
	public static final int REQUEST_REMOVE_SURFACE_ALL = 12;
	public static final int REQUEST_MIRROR = 6;
	public static final int REQUEST_ROTATE = 7;
	public static final int REQUEST_CLEAR = 8;
	public static final int REQUEST_CLEAR_ALL = 9;
	public static final int REQUEST_SET_MVP = 10;

	@NonNull
	private final SparseArray<IRendererTarget>
		mTargets = new SparseArray<>();
	private int mVideoWidth, mVideoHeight;
	@IRendererCommon.MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mRotation = 0;
	private volatile boolean mIsFirstFrameRendered;

	protected IDrawer2D mDrawer;

	public AbstractDistributeTask(final int width, final int height) {

		mVideoWidth = width > 0 ? width : 640;
		mVideoHeight = height > 0 ? height : 480;
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

		checkFinished();
		if (!((surface instanceof SurfaceTexture)
			|| (surface instanceof Surface)
			|| (surface instanceof SurfaceHolder)
			|| (surface instanceof TextureWrapper))) {

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
	public void clearSurface(final int id, final int color) {
		checkFinished();
		offer(REQUEST_CLEAR, id, color);
	}

	public void clearSurfaceAll(final int color) {
		checkFinished();
		offer(REQUEST_CLEAR_ALL, color);
	}

	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix) {
		checkFinished();
		offer(REQUEST_SET_MVP, id, offset, matrix);
	}

	public boolean isEnabled(final int id) {
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			return target != null && target.isEnabled();
		}
	}

	public void setEnabled(final int id, final boolean enable) {
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

	public void mirror(final int mirror) {
		checkFinished();
		if (mMirror != mirror) {
			offer(REQUEST_MIRROR, mirror);
		}
	}

	@IRendererCommon.MirrorMode
	public int mirror() {
		return mMirror;
	}

	protected int width() {
		return mVideoWidth;
	}

	protected int height() {
		return mVideoHeight;
	}

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
//		if (DEBUG) Log.v(TAG, "handleOnStart:finished");
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected void handleOnStop() {
		if (DEBUG) Log.v(TAG, "onStop");
		notifyParent(false);
		makeCurrent();
		internalOnStop();
		handleRemoveAll();
//		if (DEBUG) Log.v(TAG, "onStop:finished");
	}

	protected boolean handleOnError(final Exception e) {
		if (DEBUG) Log.w(TAG, e);
		return false;
	}

	@WorkerThread
	protected void internalOnStart() {
		mDrawer = createDrawer(isGLES3());
	}

	@WorkerThread
	protected void internalOnStop() {
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
	}

	@WorkerThread
	protected Object handleRequest(final int request,
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
		case REQUEST_REMOVE_SURFACE_ALL:
			handleRemoveAll();
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
			handleSetMvp(arg1, arg2, obj);
			break;
		}
		return null;
	}

	protected void checkFinished() throws IllegalStateException {
		if (isFinished()) {
			throw new IllegalStateException("already finished");
		}
	}

	/**
	 * 実際の描画処理
	 */
	@WorkerThread
	protected void handleDraw() {
		removeRequest(REQUEST_DRAW);
		if (mIsFirstFrameRendered) {
			preprocess();
			handleDrawTargets();
		}
		// Egl保持用のSurfaceへ描画しないとデッドロックする端末対策
		makeCurrent();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//		GLES20.glFlush();	// これなくても良さそう
		if (mIsFirstFrameRendered) {
			callOnFrameAvailable();
		}
	}

	@WorkerThread
	protected void preprocess() {
	}

	/**
	 * 各Surfaceへ描画する
	 */
	@WorkerThread
	protected void handleDrawTargets() {
		synchronized (mTargets) {
			final int n = mTargets.size();
			final int texId = getTexId();
			final float[] texMatrix = getTexMatrix();
			for (int i = n - 1; i >= 0; i--) {
				final IRendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.canDraw()) {
					try {
						onDrawTarget(target, texId, texMatrix);
					} catch (final Exception e) {
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
		final int texId, final float[] texMatrix) {

		target.draw(mDrawer, texId, texMatrix);
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

		if (DEBUG) Log.v(TAG, "handleAddSurface:id=" + id);
		checkTarget();
		synchronized (mTargets) {
			IRendererTarget target = mTargets.get(id);
			if (target == null) {
				try {
					target = createRendererTarget(id, getEgl(), surface, maxFps);
					setMirror(target.getMvpMatrix(), mMirror);
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
//		if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
	}

	/**
	 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
	 */
	@WorkerThread
	protected void checkTarget() {
//		if (DEBUG) Log.v(TAG, "checkTarget");
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final IRendererTarget target = mTargets.valueAt(i);
				if ((target != null) && !target.isValid()) {
					final int id = mTargets.keyAt(i);
//						if (DEBUG) Log.i(TAG, "checkTarget:found invalid surface:id=" + id);
					mTargets.valueAt(i).release();
					mTargets.remove(id);
				}
			}
		}
//		if (DEBUG) Log.v(TAG, "checkTarget:finished");
	}

	/**
	 * 指定したIDの分配描画用Surfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	@WorkerThread
	protected void handleClear(final int id, final int color) {
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
		final int offset, final Object mvp) {

		if ((mvp instanceof float[]) && (((float[]) mvp).length >= 16 + offset)) {
			final float[] array = (float[])mvp;
			synchronized (mTargets) {
				final IRendererTarget target = mTargets.get(id);
				if ((target != null) && target.isValid()) {
					System.arraycopy(array, offset, target.getMvpMatrix(), 0, 16);
				}
			}
		}
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	@WorkerThread
	protected void handleMirror(final int mirror) {
		mMirror = mirror;
		synchronized (mTargets) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final IRendererTarget target = mTargets.valueAt(i);
				if (target != null) {
					setMirror(target.getMvpMatrix(), mirror);
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
		synchronized (mTargets) {
			final IRendererTarget target = mTargets.get(id);
			if (target != null) {
				setRotation(target.getMvpMatrix(), degree);
			}
		}
	}

//--------------------------------------------------------------------------------
	public abstract boolean isRunning();
	public abstract boolean isFinished();
	public abstract boolean offer(final int request);
	public abstract boolean offer(final int request, final Object obj);
	public abstract boolean offer(final int request, final int arg1, final int arg2);
	public abstract boolean offer(final int request, final int arg1, final int arg2, final Object obj);
	public abstract void removeRequest(final int request);

	public abstract EGLBase getEgl();
	public abstract void makeCurrent();
	public abstract boolean isGLES3();
	public abstract int getTexId();
	public abstract float[] getTexMatrix();

	public abstract void notifyParent(final boolean isRunning);
	public abstract void callOnFrameAvailable();

//================================================================================
	@WorkerThread
	protected static void setMirror(final float[] mvp, final int mirror) {
		switch (mirror) {
		case MIRROR_NORMAL:
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = Math.abs(mvp[5]);
			break;
		case MIRROR_HORIZONTAL:
			mvp[0] = -Math.abs(mvp[0]);	// flip left-right
			mvp[5] = Math.abs(mvp[5]);
			break;
		case MIRROR_VERTICAL:
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = -Math.abs(mvp[5]);	// flip up-side down
			break;
		case MIRROR_BOTH:
			mvp[0] = -Math.abs(mvp[0]);	// flip left-right
			mvp[5] = -Math.abs(mvp[5]);	// flip up-side down
			break;
		}
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * @param mvp
	 * @param degrees
	 */
	protected static void rotate(final float[] mvp, final int degrees) {
		if ((degrees % 180) != 0) {
			Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
		}
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * @param mvp
	 * @param degrees
	 */
	protected static void setRotation(final float[] mvp, final int degrees) {
		Matrix.setIdentityM(mvp, 0);
		if ((degrees % 180) != 0) {
			Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
		}
	}

	@NonNull
	protected static IDrawer2D createDrawer(final boolean isGLES3) {
		if (isGLES3) {
			return new com.serenegiant.glutils.es3.GLDrawer2D(true);
		} else {
			return new com.serenegiant.glutils.es2.GLDrawer2D(true);
		}
	}
}
