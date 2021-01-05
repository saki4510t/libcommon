package com.serenegiant.glpipeline;
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

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.AbstractDistributeTask;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.GLContext;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.IRendererHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.glutils.IRendererCommon.*;

/**
 * IPipelineSourceで更新されたテクスチャを分配描画するためのヘルパークラス
 * useSharedContext=falseでVideoSource + Distributor ≒ IRendererHolder/RendererHolder
 */
public class Distributor implements IPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = Distributor.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "Distributor";

	@NonNull
	private final IPipelineSource mSource;
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final GLManager mManager;

	@Nullable
	private final IRendererHolder.RenderHolderCallback mCallback;
	private final Object mSync = new Object();
	private final DistributeTask mDistributeTask;
	private volatile boolean isRunning;

	/**
	 * コンストラクタ
	 * 共有コンテキストを使わず引数のIPipelineSourceと同じコンテキスト上で実行する
	 * @param source
	 */
	public Distributor(@NonNull final IPipelineSource source) {
		this(source, null, false, false);
	}

	/**
	 * コンストラクタ
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param source
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ったマルチスレッド処理を行うかどう
	 */
	public Distributor(@NonNull final IPipelineSource source,
		@Nullable final IRendererHolder.RenderHolderCallback callback,
		final boolean useSharedContext) {

		this(source, callback, useSharedContext, false);
	}

	/**
	 * コンストラクタ
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param source
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ったマルチスレッド処理を行うかどう
	 * @param enableVsync vsyncに同期して描画要求するかどうか
	 */
	public Distributor(@NonNull final IPipelineSource source,
		@Nullable final IRendererHolder.RenderHolderCallback callback,
		final boolean useSharedContext, final boolean enableVsync) {

		mSource = source;
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				Distributor.this.mDistributeTask.handleRequest(msg.what, msg.arg1, msg.arg2, msg.obj);
				return true;
			}
		};
		final GLManager manager = source.getGLManager();
		mOwnManager = useSharedContext;
		final Handler glHandler;
		if (useSharedContext) {
			// 共有コンテキストを使ってマルチスレッド処理を行う時
			mManager = manager.createShared(handlerCallback);
			glHandler = mManager.getGLHandler();
		} else {
			// 映像提供元のGLコンテキスト上で実行する時
			mManager = manager;
			glHandler = manager.createGLHandler(handlerCallback);
		}
		mCallback = callback;
		mDistributeTask = new DistributeTask(
			mManager.getGLContext(), glHandler,
			source.getWidth(), source.getHeight(),
			enableVsync);
		source.add(mOnFrameAvailableListener);
		mDistributeTask.start(RENDERER_THREAD_NAME);
	}

	/**
	 * 関連するリソースを廃棄する
	 */
	@Override
	public void release() {
		mDistributeTask.release();
		mSource.remove(mOnFrameAvailableListener);
	}

	@NonNull
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
		if ((width > 0) && (height > 0)) {
			mDistributeTask.resize(width, height);
		}
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
		return mDistributeTask.width();
	}

	/**
	 * 分配描画のサイズ(高さ)を取得
	 * @return
	 */
	@Override
	public int getHeight() {
		return mDistributeTask.height();
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
		mDistributeTask.addSurface(id, surface);
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
		mDistributeTask.addSurface(id, surface, maxFps);
	}

	/**
	 * 分配描画用のSurfaceを削除要求する。
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	public void removeSurface(final int id) {
//		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		mDistributeTask.removeSurface(id);
	}

	/**
	 * 分配描画用のSurfaceを全て削除要求する
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll() {
//		if (DEBUG) Log.v(TAG, "removeSurfaceAll:id=" + id);
		mDistributeTask.removeSurfaceAll();
	}

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	public void clearSurface(final int id, final int color) {
		mDistributeTask.clearSurface(id, color);
	}

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 */
	public void clearSurfaceAll(final int color) {
		mDistributeTask.clearSurfaceAll(color);
	}

	/**
	 * モデルビュー変換行列をセットする
	 * @param id
	 * @param offset
	 * @param matrix
	 */
	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix) {
		mDistributeTask.setMvpMatrix(id, offset, matrix);
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	public void setMirror(@MirrorMode final int mirror) {
		mDistributeTask.mirror(mirror % MIRROR_NUM);
	}

	/**
	 * 現在のミラーモードを取得
	 * @return
	 */
	@MirrorMode
	public int getMirror() {
		return mDistributeTask.mirror();
	}

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	public boolean isEnabled(final int id) {
		return mDistributeTask.isEnabled(id);
	}

	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	public void setEnabled(final int id, final boolean enable) {
		mDistributeTask.setEnabled(id, enable);
	}

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame() {
		mDistributeTask.requestFrame();
	}

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount() {
		return mDistributeTask.getCount();
	}

	/**
	 * VideoSourceからのコールバックリスナーの実装
	 */
	private final VideoSource.OnFrameAvailableListener mOnFrameAvailableListener
		= new VideoSource.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final int texId, @NonNull final float[] texMatrix) {
			mDistributeTask.requestFrame();
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

	private class DistributeTask extends AbstractDistributeTask {
		@NonNull
		private final GLContext mGLContext;
		@NonNull
		private final Handler mGLHandler;
		private final boolean isGLES3;

		private volatile boolean isRunning;

		/**
		 * コンストラクタ:
		 * @param glContext
		 * @param glHandler
		 * @param width
		 * @param height
		 * @param enableVSync Choreographerを使ってvsync同期して映像更新するかどうか
		 */
		public DistributeTask(@NonNull final GLContext glContext,
			@NonNull final Handler glHandler,
			final int width, final int height,
			final boolean enableVSync) {

			super(width, height, enableVSync);
			mGLContext = glContext;
			mGLHandler = glHandler;
			isGLES3 = glContext.isGLES3();
			isRunning = true;
		}

		@Override
		public void release() {
			if (DEBUG) Log.v(TAG, "release:");
			if (isRunning) {
				isRunning = false;
				mGLHandler.post(new Runnable() {
					@Override
					public void run() {
						handleOnStop();
						if (mOwnManager && isValid()) {
							mManager.release();
						}
					}
				});
			}
			super.release();
		}

		@Override
		public void start(final String tag) {
			mGLHandler.post(new Runnable() {
				@Override
				public void run() {
					handleOnStart();
				}
			});
		}

		@Override
		public boolean waitReady() {
			return true;
		}

		@Override
		public boolean isRunning() {
			return isRunning && mManager.isValid();
		}

		@Override
		public boolean isFinished() {
			return !isRunning();
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
			return mGLHandler.sendMessage(mGLHandler.obtainMessage(request, arg1));
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

		@Override
		public EGLBase getEgl() {
			return mGLContext.getEgl();
		}

		@Override
		public GLContext getGLContext() {
			return mGLContext;
		}

		@Override
		public EGLBase.IContext getContext() {
			return mGLContext.getContext();
		}

		@Override
		public void makeCurrent() {
			mGLContext.makeDefault();
		}

		@Override
		public boolean isGLES3() {
			return mGLContext.isGLES3();
		}

		@Override
		public boolean isOES3() {
			return mGLContext.isOES3();
		}

		@Override
		public boolean isMasterSurfaceValid() {
			return true;
		}

		@Override
		public int getTexId() {
			return mSource.getTexId();
		}

		@Override
		public float[] getTexMatrix() {
			return mSource.getTexMatrix();
		}

		@Override
		public Object handleRequest(final int request,
			final int arg1, final int arg2, final Object obj) {

			return super.handleRequest(request, arg1, arg2, obj);
		}

		@Override
		protected void handleReCreateInputSurface() {
			if (mSource.isValid()) {
				callOnCreate(mSource.getInputSurface());
			}
		}

		@Override
		protected void handleReleaseInputSurface() {
			callOnDestroy();
		}

		@Override
		protected void handleUpdateTexture() {
			// do nothing
		}

		@Override
		public void notifyParent(final boolean isRunning) {
			synchronized (Distributor.this) {
				Distributor.this.isRunning = isRunning;
				Distributor.this.notifyAll();
			}
		}

		@Override
		public void callOnFrameAvailable() {
			Distributor.this.callOnFrameAvailable();
		}
	}

}
