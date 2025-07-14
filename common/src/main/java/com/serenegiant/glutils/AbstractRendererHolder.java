package com.serenegiant.glutils;
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
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.math.Fraction;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.ThreadUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.serenegiant.gl.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * 分配描画インターフェースの共通部分を実装する抽象クラス
 */
public abstract class AbstractRendererHolder implements IRendererHolder {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = AbstractRendererHolder.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "RendererHolder";

	@NonNull
	private final Object mSync = new Object();
	@Nullable
	private final RenderHolderCallback mCallback;
	private volatile boolean isRunning;
	@NonNull
	protected final BaseRendererTask mRendererTask;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 */
	protected AbstractRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback,
		@Nullable GLDrawer2D.DrawerFactory drawerFactory) {

		mCallback = callback;
		final GLDrawer2D.DrawerFactory factory  = drawerFactory != null ? drawerFactory : GLDrawer2D.DEFAULT_FACTORY;
		mRendererTask = createRendererTask(width, height,
			maxClientVersion, sharedContext, flags, factory);
		mRendererTask.start(RENDERER_THREAD_NAME);
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
	}

//--------------------------------------------------------------------------------
// IRendererHolderの実装
	@Override
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	@Override
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		mRendererTask.release();
		synchronized (mSync) {
			isRunning = false;
			mSync.notifyAll();
		}
//		if (DEBUG) Log.v(TAG, "release:finished");
	}

	@Override
	@NonNull
	public GLContext getGLContext() {
		return mRendererTask.getGLContext();
	}

	@Deprecated
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public EGLBase.IContext<?> getContext() {
		return mRendererTask.getContext();
	}

	@Deprecated
	public int getGlVersion() {
		return mRendererTask.getGlVersion();
	}

	/**
	 * マスター用の映像を受け取るためのSurfaceを取得
	 * @return
	 */
	@Override
	public Surface getSurface() {
		return mRendererTask.getSurface();
	}

	/**
	 * マスター用の映像を受け取るためのSurfaceTextureを取得
	 * @return
	 */
	@Override
	public SurfaceTexture getSurfaceTexture() {
		return mRendererTask.getSurfaceTexture();
	}

	/**
	 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
	 */
	@Override
	public void reset() {
		mRendererTask.requestRecreateMasterSurface();
	}

	/**
	 * マスター映像サイズをサイズ変更要求
	 * @param width
	 * @param height
	 */
	@Override
	public void resize(final int width, final int height)
		throws IllegalStateException {

		mRendererTask.resize(width, height);
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mRendererTask.setMirror(mirror % MIRROR_NUM);
	}
	
	/**
	 * 現在のミラーモードを取得
	 * @return
	 */
	@Override
	@MirrorMode
	public int getMirror() {
		return mRendererTask.getMirror();
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param isRecordable
	 */
	@Override
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
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable, @Nullable final Fraction maxFps)
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
	@Override
	public void removeSurface(final int id) {
//		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		mRendererTask.removeSurface(id);
	}

	/**
	 * 分配描画用のSurfaceを全て削除要求する
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	@Override
	public void removeSurfaceAll() {
//		if (DEBUG) Log.v(TAG, "removeSurfaceAll:id=" + id);
		mRendererTask.removeSurfaceAll();
	}
	
	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	@Override
	public void clearSurface(final int id, final int color) {
		mRendererTask.clearSurface(id, color);
	}

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 */
	@Override
	public void clearSurfaceAll(final int color) {
		mRendererTask.clearSurfaceAll(color);
	}

	@Override
	public void setMvpMatrix(final int id,
		final int offset, @NonNull @Size(min=16) final float[] matrix) {
		mRendererTask.setMvpMatrix(id, offset, matrix);
	}

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	@Override
	public boolean isEnabled(final int id) {
		return mRendererTask.isEnabled(id);
	}
	
	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	@Override
	public void setEnabled(final int id, final boolean enable) {
		mRendererTask.setEnabled(id, enable);
	}

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	@Override
	public void requestFrame() {
		mRendererTask.requestFrame(true, mRendererTask.mTexId, mRendererTask.mTexMatrix);
	}

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	@Override
	public int getCount() {
		return mRendererTask.getCount();
	}

	/**
	 * レンダリングスレッド上で指定したタスクを実行する
	 * @param task
	 */
	@Override
	public void queueEvent(@NonNull final Runnable task) {
		mRendererTask.queueEvent(task);
	}

//--------------------------------------------------------------------------------

	/**
	 *
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @return
	 */
	@NonNull
	protected abstract BaseRendererTask createRendererTask(
		final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@NonNull GLDrawer2D.DrawerFactory drawerFactory);
	
//--------------------------------------------------------------------------------
	protected void callOnCreate(Surface surface) {
		if (mCallback != null) {
			try {
				mCallback.onCreateSurface(surface);
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
				mCallback.onDestroySurface();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

//--------------------------------------------------------------------------------
	protected static class BaseRendererTask extends AbstractDistributeTask
		implements SurfaceTexture.OnFrameAvailableListener {

		@NonNull
		private final AbstractRendererHolder mParent;
		@NonNull
		private final GLManager mGlManager;
		@NonNull
		private final Handler mGLHandler;
		@Size(value=16)
		@NonNull
		final float[] mTexMatrix = new float[16];
		private int mTexId;
		private SurfaceTexture mInputTexture;
		private Surface mInputSurface;

		/**
		 * コンストラクタ:
		 * @param parent
		 * @param width
		 * @param height
		 * @param maxClientVersion
		 * @param sharedContext
		 * @param flags
		 */
		public BaseRendererTask(@NonNull final AbstractRendererHolder parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
			@Nullable GLDrawer2D.DrawerFactory factory) {

			super(width, height, factory);
			mParent = parent;
			mGlManager = new GLManager(maxClientVersion, sharedContext, flags, null);
			mGLHandler = mGlManager.createGLHandler(new Handler.Callback() {
				@Override
				public boolean handleMessage(@NonNull final Message msg) {
					return handleRequest(msg);
				}
			});
			final Semaphore sem = new Semaphore(0);
			mGlManager.runOnGLThread(new Runnable() {
				@Override
				public void run() {
					try {
						handleOnStart();
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
					}
					sem.release();
				}
			});
			// ワーカースレッドの初期化待ち
			try {
				if (!sem.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
					// タイムアウトしたとき
				}
			} catch (final InterruptedException e) {
				// do nothing
			}
		}

		@Override
		public void release() {
			if (DEBUG) Log.v(TAG, "release:");
			if (isRunning()) {
				mGLHandler.postAtFrontOfQueue(new Runnable() {
					@Override
					public void run() {
						handleOnStop();
					}
				});
				ThreadUtils.NoThrowSleep(50L);
				mGlManager.release();
			}
			super.release();
		}

		@Override
		public void start(final String tag) {
		}

		@Override
		public boolean waitReady() {
			return mGlManager.isValid();
		}

		@Override
		public boolean isRunning() {
			return mGlManager.isValid();
		}

		@Override
		public boolean isFinished() {
			return !mGlManager.isValid();
		}

		@Override
		public boolean offer(final int request) {
			return mGLHandler.sendEmptyMessage(request);
		}

		@Override
		public boolean offer(final int request, final Object obj) {
			return mGLHandler.sendMessage(mGLHandler.obtainMessage(request, obj));
		}

		@Override
		public boolean offer(final int request, final int arg1) {
			return mGLHandler.sendMessage(mGLHandler.obtainMessage(request, arg1, 0));
		}

		@Override
		public boolean offer(final int request, final int arg1, final int arg2) {
			return mGLHandler.sendMessage(mGLHandler.obtainMessage(request, arg1, arg2));
		}

		@Override
		public boolean offer(final int request, final int arg1, final int arg2, final Object obj) {
			return mGLHandler.sendMessage(mGLHandler.obtainMessage(request, arg1, arg2, obj));
		}

		@Override
		public void removeRequest(final int request) {
			mGLHandler.removeMessages(request);
		}

		@NonNull
		@Override
		public EGLBase getEgl() {
			return mGlManager.getEgl();
		}

		@NonNull
		@Override
		public GLContext getGLContext() {
			return mGlManager.getGLContext();
		}

		@Deprecated
		@NonNull
		@Override
		public EGLBase.IContext<?> getContext() {
			return mGlManager.getGLContext().getContext();
		}

		@Override
		public int getGlVersion() {
			return mGlManager.getGLContext().getGlVersion();
		}

		@Override
		public void makeCurrent() {
			mGlManager.makeDefault();
		}

		@Override
		public boolean isGLES3() {
			return mGlManager.isGLES3();
		}

		@Override
		public boolean isOES3Supported() {
			return mGlManager.isGLES3();
		}

		@Override
		public boolean isMasterSurfaceValid() {
			return (mInputSurface != null) && (mInputSurface.isValid());
		}

		public int getTexId() {
			return mTexId;
		}

		public float[] getTexMatrix() {
			return mTexMatrix;
		}

		@Override
		public void notifyParent(final boolean isRunning) {
			if (DEBUG) Log.v(TAG, "notifyParent:" + isRunning);
			synchronized (mParent.mSync) {
				mParent.isRunning = isRunning;
				mParent.mSync.notifyAll();
			}
		}

		@Override
		public void callOnFrameAvailable() {
			mParent.callOnFrameAvailable();
		}

		@Override
		protected void handleDrawTargets(
			final boolean isOES,
			final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

			super.handleDrawTargets(isOES, texId, texMatrix);
		}

		/**
		 * 映像入力用Surfaceを再生成する
		 */
		@SuppressLint("NewApi")
		@WorkerThread
		@Override
		protected void handleReCreateInputSurface() {
			if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
			makeCurrent();
			handleReleaseInputSurface();
			makeCurrent();
			mTexId = GLUtils.initTex(
				GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(width(), height());
			}
			mInputTexture.setOnFrameAvailableListener(this);
			mParent.callOnCreate(mInputSurface);
		}

		/**
		 * 映像入力用Surfaceを破棄する
		 */
		@SuppressLint("NewApi")
		@WorkerThread
		@Override
		protected void handleReleaseInputSurface() {
			if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
			if (mInputSurface != null) {
				try {
					mInputSurface.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputSurface = null;
				mParent.callOnDestroy();
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

		@Override
		protected void handleUpdateTexture() {
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
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
			super.handleResize(width, height);
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(width, height);
			}
		}

		@NonNull
		public AbstractRendererHolder getParent() {
			return mParent;
		}

		/**
		 * マスター映像取得用のSurfaceを取得
		 * @return
		 */
		public Surface getSurface() {
			if (DEBUG) Log.v(TAG, "getSurface:" + mInputSurface);
			checkMasterSurface();
			return mInputSurface;
		}

		/**
		 * 映像受け取り用のSurfaceTextureを取得
		 * @return
		 */
		public SurfaceTexture getSurfaceTexture() {
			if (DEBUG) Log.v(TAG, "getSurfaceTexture:" + mInputTexture);
			checkMasterSurface();
			return mInputTexture;
		}

		/**
		 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
		 */
		public void reset() {
			checkMasterSurface();
		}

		/**
		 * 分配描画用のマスターSurfaceが有効かどうかをチェックして無効なら再生成する
		 */
		public void checkMasterSurface() {
			checkFinished();
			if ((mInputSurface == null) || (!mInputSurface.isValid())) {
				Log.d(TAG, "checkMasterSurface:invalid master surface");
				requestRecreateMasterSurface();
			}
		}

		/**
		 * レンダリングスレッド上で指定したタスクを実行する
		 * @param task
		 */
		public void queueEvent(@NonNull final Runnable task) {
			mGlManager.runOnGLThread(task);
		}

		/**
		 * TextureSurfaceで映像を受け取った際のコールバックリスナー
		 * (SurfaceTexture#OnFrameAvailableListenerインターフェースの実装)
		 */
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
			requestFrame(true, mTexId, mTexMatrix);
		}

	}	// BaseRendererTask

}
