package com.serenegiant.glutils;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.Surface;

import com.serenegiant.glutils.es2.GLHelper;
import com.serenegiant.utils.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.IRendererCommon.*;
import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

public abstract class RendererTaskDelegator extends EglTaskDelegator
	implements SurfaceTexture.OnFrameAvailableListener,
		Choreographer.FrameCallback {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = RendererTaskDelegator.class.getSimpleName();

	static final int REQUEST_DRAW = 1;
	static final int REQUEST_UPDATE_SIZE = 2;
	static final int REQUEST_ADD_SURFACE = 3;
	static final int REQUEST_REMOVE_SURFACE = 4;
	static final int REQUEST_REMOVE_SURFACE_ALL = 12;
	static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
	static final int REQUEST_MIRROR = 6;
	static final int REQUEST_ROTATE = 7;
	static final int REQUEST_CLEAR = 8;
	static final int REQUEST_CLEAR_ALL = 9;
	static final int REQUEST_SET_MVP = 10;

	@NonNull
	private final SparseArray<IRendererTarget>
		mTargets = new SparseArray<>();
	private final boolean mVSync;
	private int mVideoWidth, mVideoHeight;
	@NonNull
	final float[] mTexMatrix = new float[16];
	int mTexId;
	private SurfaceTexture mMasterTexture;
	private Surface mMasterSurface;
	@IRendererCommon.MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mRotation = 0;
	private volatile boolean mIsFirstFrameRendered;
	private volatile boolean mHasNewFrame;

	protected IDrawer2D mDrawer;

	/**
	 * コンストラクタ
	 *
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 */
	public RendererTaskDelegator(
		final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags,
		final boolean vSync) {

		super(maxClientVersion, sharedContext, flags);
		mVSync = vSync;
		mVideoWidth = width > 0 ? width : 640;
		mVideoHeight = height > 0 ? height : 480;
	}

	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	protected final void onStart() {
		if (DEBUG) Log.v(TAG, "onStart:");
//			Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
		handleReCreateMasterSurface();
		internalOnStart();
		notifyRunning(true);
		if (mVSync) {
			if (DEBUG) Log.v(TAG, "onStart:Choreographer#postFrameCallback");
//			mAsyncHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					Choreographer.getInstance().postFrameCallback(AbstractRendererHolder.BaseRendererTask.this);
//				}
//			});
		}
//			if (DEBUG) Log.v(TAG, "onStart:finished");
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop");
//		if (mVSync) {
//			mAsyncHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					Choreographer.getInstance().removeFrameCallback(AbstractRendererHolder.BaseRendererTask.this);
//				}
//			});
//		}
		notifyRunning(false);
		makeCurrent();
		internalOnStop();
		handleReleaseMasterSurface();
		handleRemoveAll();
//			if (DEBUG) Log.v(TAG, "onStop:finished");
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

//--------------------------------------------------------------------------------
	/**
	 * 実際の描画処理
	 */
	@WorkerThread
	protected void handleDraw() {
//		removeRequest(REQUEST_DRAW);
		if ((mMasterSurface == null) || (!mMasterSurface.isValid())) {
			Log.e(TAG, "checkMasterSurface:invalid master surface");
			offer(REQUEST_RECREATE_MASTER_SURFACE, 0, 0, null);
			return;
		}
		if (mIsFirstFrameRendered) {
			try {
				makeCurrent();
				if (mHasNewFrame) {
					mHasNewFrame = false;
					handleUpdateTexture();
				}
			} catch (final Exception e) {
				Log.e(TAG, "draw:thread id =" + Thread.currentThread().getId(), e);
				offer(REQUEST_RECREATE_MASTER_SURFACE, 0, 0, null);
				return;
			}
			preprocess();
			handleDrawTargets();
		}
		// Egl保持用のSurfaceへ描画しないとデッドロックする端末対策
		makeCurrent();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//			GLES20.glFlush();	// これなくても良さそう
		if (mIsFirstFrameRendered) {
			onFrameAvailable();
		}
	}

	@WorkerThread
	protected void handleUpdateTexture() {
		mMasterTexture.updateTexImage();
		mMasterTexture.getTransformMatrix(mTexMatrix);
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
			for (int i = n - 1; i >= 0; i--) {
				final IRendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.canDraw()) {
					try {
						onDrawTarget(target, mTexId, mTexMatrix);
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
//			if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
	}

	/**
	 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
	 */
	@WorkerThread
	protected void checkTarget() {
//			if (DEBUG) Log.v(TAG, "checkTarget");
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
//			if (DEBUG) Log.v(TAG, "checkTarget:finished");
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
	 * マスターSurfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateMasterSurface() {
		makeCurrent();
		handleReleaseMasterSurface();
		makeCurrent();
		mTexId = GLHelper.initTex(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST);
		mMasterTexture = new SurfaceTexture(mTexId);
		mMasterSurface = new Surface(mMasterTexture);
		if (BuildCheck.isAndroid4_1()) {
			mMasterTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
		}
		mMasterTexture.setOnFrameAvailableListener(this);
		onCreate(mMasterSurface);
	}

	/**
	 * マスターSurfaceを破棄する
	 */
	@WorkerThread
	protected void handleReleaseMasterSurface() {
		if (mMasterSurface != null) {
			try {
				mMasterSurface.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mMasterSurface = null;
			onDestroy();
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
			GLHelper.deleteTex(mTexId);
			mTexId = 0;
		}
	}

	/**
	 * マスター映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		mVideoWidth = width;
		mVideoHeight = height;
		if (BuildCheck.isAndroid4_1()) {
			mMasterTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
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

	/**
	 * TextureSurfaceで映像を受け取った際のコールバックリスナー
	 * (SurfaceTexture#OnFrameAvailableListenerインターフェースの実装)
	 */
	@Override
	public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
		mIsFirstFrameRendered = true;
		mHasNewFrame = true;
		if (!mVSync) {
			offer(REQUEST_DRAW, 0, 0, null);
		}
	}

	@Override
	public void doFrame(final long frameTimeNanos) {
		Choreographer.getInstance().postFrameCallbackDelayed(this, 0);
		if (mHasNewFrame) {
			offer(REQUEST_DRAW, 0, 0, null);
		}
	}

	protected abstract void onCreate(final Surface surface);
	protected abstract void onFrameAvailable();
	protected abstract void onDestroy();
	protected abstract boolean offer(final int request, final int arg1, final int arg2, final Object obj);
	protected abstract void notifyRunning(final boolean isRunning);

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
