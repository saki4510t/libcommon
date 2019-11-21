package com.serenegiant.glpipeline;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EGLConst;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.IDrawer2D;
import com.serenegiant.glutils.IRendererCommon;
import com.serenegiant.glutils.IRendererTarget;
import com.serenegiant.glutils.RenderHolderCallback;
import com.serenegiant.glutils.RendererTarget;
import com.serenegiant.glutils.TextureWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.AbstractDistributeTask.*;
import static com.serenegiant.glutils.IRendererCommon.*;

/**
 * IPipelineSourceで更新されたテクスチャを分配描画するためのヘルパークラス
 */
public class Distributor implements IPipeline {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = Distributor.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "Distributor";

	private IPipelineSource mSource;
	private final GLManager mManager;
	private int mWidth, mHeight;

	@Nullable
	private final RenderHolderCallback mCallback;
	private final Object mSync = new Object();
	private final BaseRendererTask mRendererTask;
	private volatile boolean isRunning;

	/**
	 * コンストラクタ
	 * @param source
	 */
	public Distributor(@NonNull final IPipelineSource source) {
		mSource = source;
		mManager = source.getGLManager();	// XXX とりあえずはVideoSourceと同じスレッドを使う
		mWidth = source.getWidth();
		mHeight = source.getHeight();
		mCallback = null;
		mRendererTask = new BaseRendererTask(this, mWidth, mHeight,
			3, mManager.getGLContext().getContext(),
			 EGLConst.EGL_FLAG_RECORDABLE);
		source.add(mOnFrameAvailableListener);
		new Thread(mRendererTask, RENDERER_THREAD_NAME).start();
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
	}

	/**
	 * 関連するリソースを廃棄する
	 */
	@Override
	public void release() {
		mRendererTask.release();
		mSource.remove(mOnFrameAvailableListener);
	}

	@Override
	public GLManager getGLManager() throws IllegalStateException {
		return mManager;
	}

	/**
	 * サイズ変更要求
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	@Override
	public void resize(final int width, final int height)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
	}

	@Override
	public boolean isValid() {
		return mManager.isValid();
	}

	/**
	 * 分配描画のサイズ(幅)を取得
	 * @return
	 */
	@Override
	public int getWidth() {
		return mWidth;
	}

	/**
	 * 分配描画のサイズ(高さ)を取得
	 * @return
	 */
	@Override
	public int getHeight() {
		return mHeight;
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 */
	public void addSurface(final int id,
		final Object surface, final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException {

//		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		mRendererTask.addSurface(id, surface);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 * @param maxFps
	 */
	public void addSurface(final int id,
		final Object surface, final boolean isRecordable, final int maxFps)
			throws IllegalStateException, IllegalArgumentException {

//		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		mRendererTask.addSurface(id, surface, maxFps);
	}

	/**
	 * 分配描画用のSurfaceを削除要求する。
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	public void removeSurface(final int id) {
//		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		mRendererTask.removeSurface(id);
	}

	/**
	 * 分配描画用のSurfaceを全て削除要求する
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll() {
//		if (DEBUG) Log.v(TAG, "removeSurfaceAll:id=" + id);
		mRendererTask.removeSurfaceAll();
	}

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	public void clearSurface(final int id, final int color) {
		mRendererTask.clearSurface(id, color);
	}

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 */
	public void clearSurfaceAll(final int color) {
		mRendererTask.clearSurfaceAll(color);
	}

	/**
	 * モデルビュー変換行列をセットする
	 * @param id
	 * @param offset
	 * @param matrix
	 */
	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix) {
		mRendererTask.setMvpMatrix(id, offset, matrix);
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	public void setMirror(@MirrorMode final int mirror) {
		mRendererTask.mirror(mirror % MIRROR_NUM);
	}

	/**
	 * 現在のミラーモードを取得
	 * @return
	 */
	@MirrorMode
	public int getMirror() {
		return mRendererTask.mirror();
	}

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	public boolean isEnabled(final int id) {
		return mRendererTask.isEnabled(id);
	}

	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	public void setEnabled(final int id, final boolean enable) {
		mRendererTask.setEnabled(id, enable);
	}

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame() {
		mRendererTask.offer(REQUEST_DRAW);
	}

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount() {
		return mRendererTask.getCount();
	}

	/**
	 * VideoSourceからのコールバックリスナーの実装
	 */
	private final VideoSource.OnFrameAvailableListener mOnFrameAvailableListener
		= new VideoSource.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix) {
			mRendererTask.mIsFirstFrameRendered = true;
			mRendererTask.offer(REQUEST_DRAW, 0, 0, null);
		}
	} ;

//--------------------------------------------------------------------------------
	protected void callOnCreate(Surface surface) {
		if (mCallback != null) {
			try {
				mCallback.onCreate(surface);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	protected void callOnFrameAvailable() {
		if (mCallback != null) {
			try {
				mCallback.onFrameAvailable();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	protected void callOnDestroy() {
		if (mCallback != null) {
			try {
				mCallback.onDestroy();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}


	protected static class BaseRendererTask extends EglTask {

		@NonNull
		private final SparseArray<IRendererTarget>
			mTargets = new SparseArray<>();
		@NonNull
		private final Distributor mParent;
		private int mVideoWidth, mVideoHeight;
		@IRendererCommon.MirrorMode
		private int mMirror = MIRROR_NORMAL;
		private int mRotation = 0;
		private volatile boolean mIsFirstFrameRendered;

		protected IDrawer2D mDrawer;

		public BaseRendererTask(@NonNull final Distributor parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext sharedContext, final int flags) {

			super(maxClientVersion, sharedContext, flags);
			mParent = parent;
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
		@Override
		protected final void onStart() {
			if (DEBUG) Log.v(TAG, "onStart:");
//			Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
			internalOnStart();
			synchronized (mParent.mSync) {
				mParent.isRunning = true;
				mParent.mSync.notifyAll();
			}
//			if (DEBUG) Log.v(TAG, "onStart:finished");
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
			internalOnStop();
			handleRemoveAll();
//			if (DEBUG) Log.v(TAG, "onStop:finished");
		}

		@Override
		protected boolean onError(final Exception e) {
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

		protected Distributor getParent() {
			return mParent;
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
//			GLES20.glFlush();	// これなくても良さそう
			if (mIsFirstFrameRendered) {
				mParent.callOnFrameAvailable();
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
				final int texId = mParent.mSource.getTexId();
				final float[] texMatrix = mParent.mSource.getTexMatrix();
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

	} // BaseRendererTask

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
