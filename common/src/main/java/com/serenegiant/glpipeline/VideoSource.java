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

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.GLContext;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.ThreadUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * 映像をSurface/SurfaceTextureとして受け取って
 * 他のPipelineからテクスチャとして利用可能とするためのヘルパークラス
 * useSharedContext=falseでVideoSource + Distributor ≒ IRendererHolder/RendererHolder
 */
public class VideoSource implements IPipelineSource {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = VideoSource.class.getSimpleName();

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;

	private static final int REQUEST_UPDATE_TEXTURE = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 3;

	@NonNull
	private final Set<OnFrameAvailableListener> mOnFrameAvailableListeners
		= new CopyOnWriteArraySet<>();
	@NonNull
	private final GLManager mManager;
	/**
	 * 自分用のGLManagerを保持しているかどうか
	 */
	private final boolean mOwnManager;
	@NonNull
	private final GLContext mGLContext;
	@NonNull
	private final Handler mGLHandler;
	@NonNull
	private final PipelineSourceCallback mCallback;
	private final boolean isGLES3;

	@NonNull
	private final float[] mTexMatrix = new float[16];
	private int mTexId;
	private SurfaceTexture mInputTexture;
	private Surface mInputSurface;
	private int mVideoWidth, mVideoHeight;

	/**
	 * コンストラクタ
	 * 引数のGLManagerのスレッド上で動作する
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 */
	public VideoSource(@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback) {

		this(manager, width, height, callback, false);
	}

	/**
	 * コンストラクタ
	 * useSharedContext=falseなら引数のGLManagerのスレッド上で動作する
	 * useSharedContext=trueなら共有コンテキストを使って専用スレッド上で動作する
	 * XXX useSharedContext = trueで共有コンテキストを使ったマルチスレッド処理を有効にするとGPUのドライバー内でクラッシュする端末がある
	 * @param manager
	 * @param width
	 * @param height
	 * @param callback
	 * @param useSharedContext 共有コンテキストを使ってマルチスレッドで処理を行うかどうか
	 */
	public VideoSource(@NonNull final GLManager manager,
		final int width, final int height,
		@NonNull final PipelineSourceCallback callback,
		final boolean useSharedContext) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");

		mOwnManager = useSharedContext;
		final Handler.Callback handlerCallback
			= new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return VideoSource.this.handleMessage(msg);
			}
		};
		if (useSharedContext) {
			mManager = manager.createShared(handlerCallback);
			mGLHandler = mManager.getGLHandler();
		} else {
			mManager = manager;
			mGLHandler = manager.createGLHandler(handlerCallback);
		}
		mGLContext = mManager.getGLContext();
		isGLES3 = mGLContext.isGLES3();
		mCallback = callback;
		if ((width > 0) || (height > 0)) {
			mVideoWidth = width;
			mVideoHeight = height;
		} else {
			mVideoWidth = DEFAULT_WIDTH;
			mVideoHeight = DEFAULT_HEIGHT;
		}
		mGLHandler.sendEmptyMessage(REQUEST_RECREATE_MASTER_SURFACE);
	}

	/**
	 * IPipelineの実装
	 * 関連するリソースを廃棄する
	 */
	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (isValid()) {
			mGLHandler.post(new Runnable() {
				@Override
				public void run() {
					handleReleaseInputSurface();
					if (mOwnManager) {
						mManager.release();
					}
				}
			});
		}
	}

	/**
	 * IPipelineの実装
	 * GLManagerを取得する
	 * @return
	 */
	@NonNull
	@Override
	public GLManager getGLManager() throws IllegalStateException {
		checkValid();
		return mManager;
	}

	/**
	 * IPipelineの実装
	 * リサイズ要求
	 * @param width
	 * @param height
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "resize:");
		checkValid();
		if ((width > 0) && (height > 0)
			&& ((width != mVideoWidth) || (height != mVideoHeight))) {

			mGLHandler.sendMessage(mGLHandler.obtainMessage(REQUEST_UPDATE_SIZE, width, height));
		}
	}

	/**
	 * IPipelineの実装
	 * VideoSourceオブジェクトが有効かどうかを取得
	 * @return
	 */
	@Override
	public boolean isValid() {
		return mManager.isValid() && (mInputSurface != null) && mInputSurface.isValid();
	}

	/**
	 * IPipelineの実装
	 * 映像幅を取得
	 * @return
	 */
	@Override
	public int getWidth() {
		return mVideoWidth;
	}

	/**
	 * IPipelineの実装
	 * 映像高さを取得
	 * @return
	 */
	@Override
	public int getHeight() {
		return mVideoHeight;
	}

	/**
	 * IPipelineSourceの実装
	 * 映像入力用のSurfaceTextureを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public SurfaceTexture getInputSurfaceTexture() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:" + mInputTexture);
		checkValid();
		if (mInputTexture == null) {
			throw new IllegalStateException("has no master surface");
		}
		return mInputTexture;
	}

	/**
	 * IPipelineSourceの実装
	 * 映像入力用のSurfaceを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public Surface getInputSurface() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurface:" + mInputSurface);
		checkValid();
		if (mInputSurface == null) {
			throw new IllegalStateException("has no master surface");
		}
		return mInputSurface;
	}

	/**
	 * IPipelineSourceの実装
	 * テクスチャ名を取得
	 * @return
	 */
	@Override
	public int getTexId() {
		return mTexId;
	}

	/**
	 * IPipelineSourceの実装
	 * テクスチャ変換行列を取得
	 * @return
	 */
	@Override
	public float[] getTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * IPipelineSourceの実装
	 * OnFrameAvailableListenerを登録
	 * @param listener
	 */
	@Override
	public void add(final OnFrameAvailableListener listener) {
		mOnFrameAvailableListeners.add(listener);
	}

	/**
	 * IPipelineSourceの実装
	 * OnFrameAvailableListenerを登録解除
	 * @param listener
	 */
	@Override
	public void remove(final OnFrameAvailableListener listener) {
		mOnFrameAvailableListeners.remove(listener);
	}
//--------------------------------------------------------------------------------
	protected void checkValid() throws IllegalStateException {
		if (!mManager.isValid()) {
			throw new IllegalStateException("Already released");
		}
	}

//--------------------------------------------------------------------------------
	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
//		if (DEBUG) Log.v(TAG, "handleMessage:" + msg);
		switch (msg.what) {
		case REQUEST_UPDATE_TEXTURE:
			handleUpdateTex();
			return true;
		case REQUEST_UPDATE_SIZE:
			handleResize(msg.arg1, msg.arg2);
			return true;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
			mCallback.onCreate(mInputSurface);
			return true;
		default:
			return false;
		}
	}

	/**
	 * テクスチャを更新してonFrameAvailableコールバックメソッドを呼び出す
	 */
	@WorkerThread
	protected void handleUpdateTex() {
//		if (DEBUG) Log.v(TAG, "handleUpdateTex:");
		if (isGLES3) {
			handleUpdateTexES3();
		} else {
			handleUpdateTexES2();
		}
	}

	/**
	 * handleUpdateTexの下請け、ES2用
	 */
	protected void handleUpdateTexES2() {
		makeDefault();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glFlush();
		if (mInputTexture != null) {
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
			GLES20.glFlush();
			ThreadUtils.NoThrowSleep(0, 0);
			callOnFrameAvailable();
		}
	}

	/**
	 * handleUpdateTexの下請け、ES3用
	 */
	protected void handleUpdateTexES3() {
		makeDefault();
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glFlush();
		if (mInputTexture != null) {
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
			GLES30.glFlush();
			ThreadUtils.NoThrowSleep(0, 0);
			callOnFrameAvailable();
		}
	}

	/**
	 * handleUpdateTexの下請け
	 */
	private void callOnFrameAvailable() {
		for (final OnFrameAvailableListener listener: mOnFrameAvailableListeners) {
			try {
				listener.onFrameAvailable(mTexId, mTexMatrix);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
				mOnFrameAvailableListeners.remove(listener);
			}
		}
	}

	/**
	 * 映像入力用Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		makeDefault();
		handleReleaseInputSurface();
		makeDefault();
		if (mGLContext.isOES3()) {
			GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			mTexId = com.serenegiant.glutils.es3.GLHelper.initTex(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE0, GLES30.GL_NEAREST);
		} else {
			GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			mTexId = com.serenegiant.glutils.es2.GLHelper.initTex(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
		}
		mInputTexture = new SurfaceTexture(mTexId);
		mInputSurface = new Surface(mInputTexture);
		if (BuildCheck.isAndroid4_1()) {
			mInputTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
		}
		if (BuildCheck.isLollipop()) {
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mGLHandler);	// API>=21
		} else {
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
		mCallback.onCreate(mInputSurface);
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		if (mInputSurface != null) {
			mCallback.onDestroy();
			try {
				mInputSurface.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mInputSurface = null;
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
			if (isGLES3) {
				com.serenegiant.glutils.es3.GLHelper.deleteTex(mTexId);
			} else {
				com.serenegiant.glutils.es2.GLHelper.deleteTex(mTexId);
			}
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
		if ((mInputSurface == null) || !mInputSurface.isValid()) {
			handleReCreateInputSurface();
		}
		if (BuildCheck.isAndroid4_1()) {
			mInputTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
		}
	}

	/**
	 * 映像受け取り用のSurfaceTextureからのコールバック
	 */
	private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener
		= new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
			if (isValid()) {
				mGLHandler.sendEmptyMessage(REQUEST_UPDATE_TEXTURE);
			}
		}
	};

	@WorkerThread
	protected void makeDefault() {
		mGLContext.makeDefault();
	}

}
