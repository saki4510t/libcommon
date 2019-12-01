package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.AbstractDistributeTask;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EGLConst;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.IRendererHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.glutils.IRendererCommon.*;

/**
 * IPipelineSourceで更新されたテクスチャを分配描画するためのヘルパークラス
 */
public class Distributor implements IPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = Distributor.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "Distributor";

	private IPipelineSource mSource;
	private final GLManager mManager;
	private int mWidth, mHeight;

	@Nullable
	private final IRendererHolder.RenderHolderCallback mCallback;
	private final Object mSync = new Object();
	private final BaseRendererTask mRendererTask;
	private volatile boolean isRunning;

	/**
	 * コンストラクタ
	 * 共有コンテキストを使わず引数のIPipelineSourceと同じコンテキスト上で実行する
	 * @param source
	 */
	public Distributor(@NonNull final IPipelineSource source) {
		this(source, false);
	}

	/**
	 * コンストラクタ
	 * @param source
	 * @param useSharedContext 共有コンテキストを使ったマルチスレッド処理を行うかどう
	 */
	public Distributor(@NonNull final IPipelineSource source,
		final boolean useSharedContext) {

		mSource = source;
		final GLManager manager = source.getGLManager();
		if (useSharedContext) {
			mManager = manager.createShared(null);
		} else {
			mManager = manager;
		}
		mWidth = source.getWidth();
		mHeight = source.getHeight();
		mCallback = null;
		mRendererTask = new BaseRendererTask(this, mWidth, mHeight,
			3, mManager.getGLContext().getContext(),
			 EGLConst.EGL_FLAG_RECORDABLE);
		source.add(mOnFrameAvailableListener);
		mRendererTask.start(RENDERER_THREAD_NAME);
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
		mRendererTask.requestFrame();
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
			mRendererTask.requestFrame();
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

	protected static class BaseRendererTask extends AbstractDistributeTask {
		@NonNull
		private final Distributor mParent;
		@NonNull
		private final EglTask mEglTask;

		/**
		 * コンストラクタ:
		 * @param parent
		 * @param width
		 * @param height
		 * @param maxClientVersion
		 * @param sharedContext
		 * @param flags
		 */
		public BaseRendererTask(@NonNull final Distributor parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext sharedContext, final int flags) {

			super(width, height);
			mParent = parent;
			mEglTask = new EglTask(maxClientVersion, sharedContext, flags) {
				@Override
				protected void onStart() {
					handleOnStart();
				}

				@Override
				protected void onStop() {
					handleOnStop();
				}

				@Override
				protected Object processRequest(
					final int request, final int arg1, final int arg2, final Object obj)
						throws TaskBreak {

					return handleRequest(request, arg1, arg2, obj);
				}
			};
		}

		@Override
		public void release() {
			if (DEBUG) Log.v(TAG, "release:");
			mEglTask.release();
			super.release();
		}

		@Override
		public void start(final String tag) {
			new Thread(mEglTask, tag).start();
		}

		@Override
		public boolean waitReady() {
			return mEglTask.waitReady();
		}

		@Override
		public boolean isRunning() {
			return mEglTask.isRunning();
		}

		@Override
		public boolean isFinished() {
			return mEglTask.isFinished();
		}

		@Override
		public boolean offer(final int request) {
			return mEglTask.offer(request);
		}

		@Override
		public boolean offer(final int request, final Object obj) {
			return mEglTask.offer(request, obj);
		}

		@Override
		public boolean offer(final int request, final int arg1) {
			return mEglTask.offer(request, arg1);
		}

		@Override
		public boolean offer(final int request, final int arg1, final int arg2) {
			return mEglTask.offer(request, arg1, arg2);
		}

		@Override
		public boolean offer(final int request, final int arg1, final int arg2, final Object obj) {
			return mEglTask.offer(request, arg1, arg2, obj);
		}

		@Override
		public void removeRequest(final int request) {
			mEglTask.removeRequest(request);
		}

		@Override
		public EGLBase getEgl() {
			return mEglTask.getEgl();
		}

		@Override
		public EGLBase.IContext getContext() {
			return mEglTask.getContext();
		}

		@Override
		public void makeCurrent() {
			mEglTask.makeCurrent();
		}

		@Override
		public boolean isGLES3() {
			return mEglTask.isGLES3();
		}

		@Override
		public boolean isMasterSurfaceValid() {
			return true;
		}

		@Override
		public int getTexId() {
			return mParent.mSource.getTexId();
		}

		@Override
		public float[] getTexMatrix() {
			return mParent.mSource.getTexMatrix();
		}

		@Override
		protected void handleReCreateMasterSurface() {
			// do nothing
		}

		@Override
		protected void handleReleaseMasterSurface() {
			// do nothing
		}

		@Override
		protected void handleUpdateTexture() {
			// do nothing
		}

		@Override
		public void notifyParent(final boolean isRunning) {
			synchronized (mParent) {
				mParent.isRunning = isRunning;
				mParent.notifyAll();
			}
		}

		@Override
		public void callOnFrameAvailable() {
			mParent.callOnFrameAvailable();
		}
	}

}
