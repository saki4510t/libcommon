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

import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.glutils.AbstractDistributeTask;
import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLManager;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.math.Fraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * GLPipelineSourceで更新されたテクスチャをSurfaceへ分配描画するためのヘルパークラス
 * useSharedContext=falseでVideoSource + SurfaceDistributePipeline ≒ IRendererHolder/RendererHolder
 * 分配描画が必要ない場合または分配先が少ない場合はSurfaceRendererPipelineの方が負荷が少ないかもしれない
 * パイプライン → SurfaceDistributePipeline (→ パイプライン)
 *                → Surface
 *                → Surface
 *                → ...
 */
public class SurfaceDistributePipeline extends ProxyPipeline implements IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SurfaceDistributePipeline.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "SurfaceDistributePipeline";

	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final GLManager mManager;
	@NonNull
	private final DistributeTask mDistributeTask;
	private volatile boolean isRunning;

	/**
	 * コンストラクタ
	 * 共有コンテキストを使わず引数のGLManagerと同じコンテキスト上で実行する
	 * @param manager
	 */
	public SurfaceDistributePipeline(@NonNull final GLManager manager) {
		this(manager, false, null);
	}

	/**
	 * コンストラクタ
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param manager
	 * @param useSharedContext 共有コンテキストを使ったマルチスレッド処理を行うかどう
	 */
	public SurfaceDistributePipeline(
		@NonNull final GLManager manager, final boolean useSharedContext,
		@Nullable GLDrawer2D.DrawerFactory factory) {

		super();
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				mDistributeTask.handleRequest(msg);
				return true;
			}
		};
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
		mDistributeTask = new DistributeTask(
			mManager.getGLContext(), glHandler,
			getWidth(), getHeight(), factory);
		mDistributeTask.start(RENDERER_THREAD_NAME);
	}

	/**
	 * 関連するリソースを廃棄する
	 */
	@Override
	protected void internalRelease() {
		if (isValid()) {
			mDistributeTask.release();
		}
		super.internalRelease();
	}

	@NonNull
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

		super.resize(width, height);
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		if ((width > 0) && (height > 0)) {
			mDistributeTask.resize(width, height);
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mDistributeTask.isRunning();
	}

	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		mDistributeTask.requestFrame(isOES, texId, texMatrix);
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
		final Object surface, final boolean isRecordable, @Nullable final Fraction maxFps)
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
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mDistributeTask.setMirror(mirror % MIRROR_NUM);
	}

	/**
	 * 現在のミラーモードを取得
	 * @return
	 */
	@MirrorMode
	@Override
	public int getMirror() {
		return mDistributeTask.getMirror();
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
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount() {
		return mDistributeTask.getCount();
	}

//--------------------------------------------------------------------------------

	/**
	 * 分配描画処理の実体
	 * AbstractDistributeTaskから継承せずに自前実装にかえるかも?
	 */
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
		 */
		public DistributeTask(@NonNull final GLContext glContext,
			@NonNull final Handler glHandler,
			final int width, final int height,
			@Nullable GLDrawer2D.DrawerFactory factory) {

			super(width, height, factory);
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
			return mGLContext.getEgl();
		}

		@NonNull
		@Override
		public GLContext getGLContext() {
			return mGLContext;
		}

		@NonNull
		@Override
		public EGLBase.IContext<?> getContext() {
			return mGLContext.getContext();
		}

		@Override
		public int getGlVersion() {
			return mGLContext.getGlVersion();
		}

		@Override
		public void makeCurrent() {
			mGLContext.makeDefault();
		}

		@Override
		public boolean isGLES3() {
			return isGLES3;
		}

		@Override
		public boolean isOES3Supported() {
			return mGLContext.isOES3Supported();
		}

		@Override
		public boolean isMasterSurfaceValid() {
			return true;
		}

		@WorkerThread
		@Override
		public void requestFrame(final boolean isOES, final int texId, @NonNull final float[] texMatrix) {
			if (mOwnManager || !isFirstFrameRendered()) {
				// 共有コンテキストを使う時は常にsuper#requestFrameを呼ぶ(別スレッドでの描画なので)
				// 最初のフレームはフラグ更新のためにsuper#requestFrameを呼ぶ
				super.requestFrame(isOES, texId, texMatrix);
			} else {
				// 2フレーム目以降は高速化のために描画前の処理をスキップして直接#handleDrawTargetsを呼ぶ
				super.handleDrawTargets(isOES, texId, texMatrix);
				makeCurrent();
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			}
		}

		@Override
		protected boolean handleRequest(@NonNull final Message msg) {
			// これはGLManagerから取得したワーカースレッド用Handlerのメッセージコールバックから呼び出せるようにoverride
			return super.handleRequest(msg);
		}

		@Override
		protected void handleReCreateInputSurface() {
		}

		@Override
		protected void handleReleaseInputSurface() {
		}

		@Override
		protected void handleUpdateTexture() {
			// do nothing
		}

		@Override
		public void notifyParent(final boolean isRunning) {
			synchronized (SurfaceDistributePipeline.this) {
				SurfaceDistributePipeline.this.isRunning = isRunning;
				SurfaceDistributePipeline.this.notifyAll();
			}
		}

		@Override
		public void callOnFrameAvailable() {
		}
	}

}
