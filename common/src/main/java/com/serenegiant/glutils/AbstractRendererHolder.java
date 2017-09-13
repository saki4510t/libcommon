package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.utils.BuildCheck;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

public abstract class AbstractRendererHolder implements IRendererHolder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = AbstractRendererHolder.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "RendererHolder";
	private static final String CAPTURE_THREAD_NAME = "CaptureTask";

	protected static final int REQUEST_DRAW = 1;
	protected static final int REQUEST_UPDATE_SIZE = 2;
	protected static final int REQUEST_ADD_SURFACE = 3;
	protected static final int REQUEST_REMOVE_SURFACE = 4;
	protected static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
	protected static final int REQUEST_MIRROR = 6;
	protected static final int REQUEST_ROTATE = 7;
	protected static final int REQUEST_CLEAR = 8;
	protected static final int REQUEST_CLEAR_ALL = 9;
	protected static final int REQUEST_SET_MVP = 10;

	protected final Object mSync = new Object();
	private final RenderHolderCallback mCallback;
	protected volatile boolean isRunning;

	private File mCaptureFile;
	private int mCaptureCompression;
	protected final RendererTask mRendererTask;

	protected AbstractRendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {
		
		mCallback = callback;
		mRendererTask = createRendererTask(width, height);
		new Thread(mRendererTask, RENDERER_THREAD_NAME).start();
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
		startCaptureTask();
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
		mRendererTask.checkMasterSurface();
	}

	/**
	 * マスター映像サイズをサイズ変更要求
	 * @param width
	 * @param height
	 */
	@Override
	public void resize(final int width, final int height) {
		mRendererTask.resize(width, height);
	}

	/**
	 * ミラーモードをセット
	 * @param mirror
	 */
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		mRendererTask.mirror(mirror % MIRROR_NUM);
	}
	
	/**
	 * 現在のミラーモードを取得
	 * @return
	 */
	@Override
	@MirrorMode
	public int getMirror() {
		return mRendererTask.mirror();
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface
	 * @param isRecordable
	 */
	@Override
	public void addSurface(final int id, final Object surface, final boolean isRecordable) {
//		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		mRendererTask.addSurface(id, surface);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface
	 * @param isRecordable
	 * @param maxFps
	 */
	@Override
	public void addSurface(final int id, final Object surface, final boolean isRecordable, final int maxFps) {
//		if (DEBUG) Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
		mRendererTask.addSurface(id, surface, maxFps);
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	@Override
	public void removeSurface(final int id) {
//		if (DEBUG) Log.v(TAG, "removeSurface:id=" + id);
		mRendererTask.removeSurface(id);
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
	public void clearSurfaceAll(final int color) {
		mRendererTask.clearSurfaceAll(color);
	}

	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix) {
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
		mRendererTask.removeRequest(REQUEST_DRAW);
		mRendererTask.offer(REQUEST_DRAW);
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
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 * @param path
	 */
	@Override
	public void captureStillAsync(final String path) {
		captureStillAsync(path, 90);
	}
	
	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 * @param path
	 * @param captureCompression
	 */
	@Override
	public void captureStillAsync(final String path, final int captureCompression) {
//		if (DEBUG) Log.v(TAG, "captureStill:" + path);
		final File file = new File(path);
		synchronized (mSync) {
			mCaptureFile = file;
			mCaptureCompression = captureCompression;
			mSync.notifyAll();
		}
	}

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param path
	 */
	@Override
	public void captureStill(final String path) {
		captureStill(path, 90);
	}
	
	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param path
	 */
	@Override
	public void captureStill(final String path, final int captureCompression) {
//		if (DEBUG) Log.v(TAG, "captureStill:" + path);
		final File file = new File(path);
		synchronized (mSync) {
			mCaptureFile = file;
			mCaptureCompression = captureCompression;
			mSync.notifyAll();
			try {
//				if (DEBUG) Log.v(TAG, "静止画撮影待ち");
				mSync.wait();
			} catch (final InterruptedException e) {
				// ignore
			}
		}
//		if (DEBUG) Log.v(TAG, "captureStill終了");
	}

//--------------------------------------------------------------------------------
	@NonNull
	protected abstract RendererTask createRendererTask(final int width, final int height);
	
	protected void startCaptureTask() {
		new Thread(mCaptureTask, CAPTURE_THREAD_NAME).start();
		synchronized (mSync) {
			if (!isRunning) {
				try {
					mSync.wait();
				} catch (final InterruptedException e) {
				}
			}
		}
	}
	
	protected void notifyCapture() {
		synchronized (mCaptureTask) {
			// キャプチャタスクに映像が更新されたことを通知
			mCaptureTask.notify();
		}
	}

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

//--------------------------------------------------------------------------------
	protected abstract static class BaseRendererTask extends EglTask {
		private final Object mClientSync = new Object();
		private final SparseArray<RendererSurfaceRec> mClients = new SparseArray<RendererSurfaceRec>();
		private final AbstractRendererHolder mParent;
		private int mVideoWidth, mVideoHeight;
		final float[] mTexMatrix = new float[16];
		int mTexId;
		private SurfaceTexture mMasterTexture;
		private Surface mMasterSurface;
		@MirrorMode
		private int mMirror = MIRROR_NORMAL;
		private int mRotation = 0;
		
		public BaseRendererTask(final AbstractRendererHolder parent, final int width, final int height) {
			super(3, null, EglTask.EGL_FLAG_RECORDABLE);
			mParent = parent;
			mVideoWidth = width;
			mVideoHeight = height;
		}

		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@Override
		protected final void onStart() {
//			if (DEBUG) Log.v(TAG, "onStart:");
			handleReCreateMasterSurface();
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
		@Override
		protected void onStop() {
//				if (DEBUG) Log.v(TAG, "onStop");
			synchronized (mParent.mSync) {
				mParent.isRunning = false;
				mParent.mSync.notifyAll();
			}
			makeCurrent();
			internalOnStop();
			handleReleaseMasterSurface();
			handleRemoveAll();
//			if (DEBUG) Log.v(TAG, "onStop:finished");
		}
		
		@Override
		protected boolean onError(final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
			return false;
		}
		
		protected abstract void internalOnStart();
		protected abstract void internalOnStop();

		@Override
		protected Object processRequest(final int request, final int arg1, final int arg2, final Object obj) {
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
			case REQUEST_RECREATE_MASTER_SURFACE:
				handleReCreateMasterSurface();
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
	
		/**
		 * マスター映像取得用のSurfaceを取得
		 * @return
		 */
		public Surface getSurface() {
	//			if (DEBUG) Log.v(TAG, "getSurface:" + mMasterSurface);
			checkMasterSurface();
			return mMasterSurface;
		}
	
		/**
		 * マスター映像受け取り用のSurfaceTextureを取得
		 * @return
		 */
		public SurfaceTexture getSurfaceTexture() {
	//		if (DEBUG) Log.v(TAG, "getSurfaceTexture:" + mMasterTexture);
			checkMasterSurface();
			return mMasterTexture;
		}
	
		/**
		 * 分配描画用のSurfaceを追加
		 * @param id
		 * @param surface
		 */
		public void addSurface(final int id, final Object surface) {
			addSurface(id, surface, -1);
		}
	
		/**
		 * 分配描画用のSurfaceを追加
		 * @param id
		 * @param surface
		 */
		public void addSurface(final int id, final Object surface, final int maxFps) {
			checkFinished();
			if (!((surface instanceof SurfaceTexture) || (surface instanceof Surface) || (surface instanceof SurfaceHolder))) {
				throw new IllegalArgumentException("Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
			}
			synchronized (mClientSync) {
				if (mClients.get(id) == null) {
					for ( ; isRunning() ; ) {
						if (offer(REQUEST_ADD_SURFACE, id, maxFps, surface)) {
							try {
								mClientSync.wait();
							} catch (final InterruptedException e) {
								// ignore
							}
							break;
						} else {
							try {
								mClientSync.wait(10);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}
			}
		}
	
		/**
		 * 分配描画用のSurfaceを削除
		 * @param id
		 */
		public void removeSurface(final int id) {
			synchronized (mClientSync) {
				if (mClients.get(id) != null) {
					for ( ; isRunning() ; ) {
						if (offer(REQUEST_REMOVE_SURFACE, id)) {
							try {
								mClientSync.wait();
							} catch (final InterruptedException e) {
								// ignore
							}
							break;
						} else {
							try {
								mClientSync.wait(10);
							} catch (InterruptedException e) {
								break;
							}
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
			synchronized (mClientSync) {
				final RendererSurfaceRec rec = mClients.get(id);
				return rec != null && rec.isEnabled();
			}
		}
		
		public void setEnabled(final int id, final boolean enable) {
			synchronized (mClientSync) {
				final RendererSurfaceRec rec = mClients.get(id);
				if (rec != null) {
					rec.setEnabled(enable);
				}
			}
		}
	
		/**
		 * 分配描画用のSurfaceの数を取得
		 * @return
		 */
		public int getCount() {
			synchronized (mClientSync) {
				return mClients.size();
			}
		}
	
		/**
		 * リサイズ
		 * @param width
		 * @param height
		 */
		public void resize(final int width, final int height) {
			checkFinished();
			if ((mVideoWidth != width) || (mVideoHeight != height)) {
				offer(REQUEST_UPDATE_SIZE, width, height);
			}
		}
	
		protected int width() {
			return mVideoWidth;
		}
		
		protected int height() {
			return mVideoHeight;
		}
		
		public void mirror(final int mirror) {
			checkFinished();
			if (mMirror != mirror) {
				offer(REQUEST_MIRROR, mirror);
			}
		}

		@MirrorMode
		public int mirror() {
			return mMirror;
		}
		
		/**
		 * 分配描画用のマスターSurfaceが有効かどうかをチェックして無効なら再生成する
		 */
		public void checkMasterSurface() {
			checkFinished();
			if ((mMasterSurface == null) || (!mMasterSurface.isValid())) {
				Log.d(TAG, "checkMasterSurface:invalid master surface");
				offerAndWait(REQUEST_RECREATE_MASTER_SURFACE, 0, 0, null);
			}
		}
	
		protected void checkFinished() {
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
		protected void handleDraw() {
			if ((mMasterSurface == null) || (!mMasterSurface.isValid())) {
				Log.e(TAG, "checkMasterSurface:invalid master surface");
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			try {
				makeCurrent();
				mMasterTexture.updateTexImage();
				mMasterTexture.getTransformMatrix(mTexMatrix);
			} catch (final Exception e) {
				Log.e(TAG, "draw:thread id =" + Thread.currentThread().getId(), e);
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			mParent.notifyCapture();
			preprocess();
			// 各Surfaceへ描画する
			synchronized (mClientSync) {
				final int n = mClients.size();
				RendererSurfaceRec client;
				for (int i = n - 1; i >= 0; i--) {
					client = mClients.valueAt(i);
					if ((client != null) && client.canDraw()) {
						try {
							onDrawClient(client, mTexId, mTexMatrix);
						} catch (final Exception e) {
							// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
							mClients.removeAt(i);
							client.release();
						}
					}
				}
			}
			mParent.callOnFrameAvailable();
			makeCurrent();
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glFlush();
		}

		protected abstract void preprocess();
		protected abstract void onDrawClient(final RendererSurfaceRec client, final int texId, final float[] texMatrix);
		
		/**
		 * 指定したIDの分配描画先Surfaceを追加する
		 * @param id
		 * @param surface
		 */
		protected void handleAddSurface(final int id, final Object surface, final int maxFps) {
	//			if (DEBUG) Log.v(TAG, "handleAddSurface:id=" + id);
			checkSurface();
			synchronized (mClientSync) {
				RendererSurfaceRec client = mClients.get(id);
				if (client == null) {
					try {
						client = RendererSurfaceRec.newInstance(getEgl(), surface, maxFps);
						setMirror(client, mMirror);
						mClients.append(id, client);
					} catch (final Exception e) {
						Log.w(TAG, "invalid surface: surface=" + surface, e);
					}
				} else {
					Log.w(TAG, "surface is already added: id=" + id);
				}
				mClientSync.notifyAll();
			}
		}
	
		/**
		 * 指定したIDの分配描画先Surfaceを破棄する
		 * @param id
		 */
		protected void handleRemoveSurface(final int id) {
	//			if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
			synchronized (mClientSync) {
				final RendererSurfaceRec client = mClients.get(id);
				if (client != null) {
					mClients.remove(id);
					if (client.isValid()) {
						client.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
					}
					client.release();
				}
				checkSurface();
				mClientSync.notifyAll();
			}
		}
				
		/**
		 * 念の為に分配描画先のSurfaceを全て破棄する
		 */
		protected void handleRemoveAll() {
	//			if (DEBUG) Log.v(TAG, "handleRemoveAll:");
			synchronized (mClientSync) {
				final int n = mClients.size();
				RendererSurfaceRec client;
				for (int i = 0; i < n; i++) {
					client = mClients.valueAt(i);
					if (client != null) {
						if (client.isValid()) {
							client.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
						}
						client.release();
					}
				}
				mClients.clear();
			}
	//			if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
		}
	
		/**
		 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
		 */
		protected void checkSurface() {
	//			if (DEBUG) Log.v(TAG, "checkSurface");
			synchronized (mClientSync) {
				final int n = mClients.size();
				for (int i = 0; i < n; i++) {
					final RendererSurfaceRec client = mClients.valueAt(i);
					if ((client != null) && !client.isValid()) {
						final int id = mClients.keyAt(i);
	//						if (DEBUG) Log.i(TAG, "checkSurface:found invalid surface:id=" + id);
						mClients.valueAt(i).release();
						mClients.remove(id);
					}
				}
			}
	//			if (DEBUG) Log.v(TAG, "checkSurface:finished");
		}
	
		/**
		 * 指定したIDの分配描画用Surfaceを指定した色で塗りつぶす
		 * @param id
		 * @param color
		 */
		protected void handleClear(final int id, final int color) {
			synchronized (mClientSync) {
				final RendererSurfaceRec client = mClients.get(id);
				if ((client != null) && client.isValid()) {
					client.clear(color);
				}
			}
		}
		
		/**
		 * 分配描画用Surface全てを指定した色で塗りつぶす
		 * @param color
		 */
		protected void handleClearAll(final int color) {
			synchronized (mClientSync) {
				final int n = mClients.size();
				for (int i = 0; i < n; i++) {
					final RendererSurfaceRec client = mClients.valueAt(i);
					if ((client != null) && client.isValid()) {
						client.clear(color);
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
		protected void handleSetMvp(final int id,
			final int offset, final Object mvp) {

			if ((mvp instanceof float[]) && (((float[]) mvp).length >= 16 + offset)) {
				final float[] array = (float[])mvp;
				synchronized (mClientSync) {
					final RendererSurfaceRec client = mClients.get(id);
					if ((client != null) && client.isValid()) {
						System.arraycopy(array, offset, client.mMvpMatrix, 0, 16);
					}
				}
			}
		}
		
		/**
		 * マスターSurfaceを再生成する
		 */
		@SuppressLint("NewApi")
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
			mMasterTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
			mParent.callOnCreate(mMasterSurface);
		}
	
		/**
		 * マスターSurfaceを破棄する
		 */
		protected void handleReleaseMasterSurface() {
			if (mMasterSurface != null) {
				mMasterSurface = null;
				mParent.callOnDestroy();
			}
			if (mMasterTexture != null) {
				mMasterTexture.release();
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
		protected void handleResize(final int width, final int height) {
	//			if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
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
		protected void handleMirror(final int mirror) {
			mMirror = mirror;
			synchronized (mClientSync) {
				final int n = mClients.size();
				for (int i = 0; i < n; i++) {
					final RendererSurfaceRec client = mClients.valueAt(i);
					if (client != null) {
						setMirror(client, mirror);
					}
				}
			}
		}
	
		/**
		 * handleMirrorの下請け
		 * @param client
		 * @param mirror
		 */
		protected void setMirror(final RendererSurfaceRec client, final int mirror) {
			RendererHolder.setMirror(client.mMvpMatrix, mirror);
		}
	
		protected void handleRotate(final int id, final int degree) {
			// FIXME 未実装
		}
		
		/**
		 * TextureSurfaceで映像を受け取った際のコールバックリスナー
		 */
		private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
				offer(REQUEST_DRAW);
			}
		};

	}
	
	/**
	 * ワーカースレッド上でOpenGL|ESを用いてマスター映像を分配描画するためのインナークラス
	 */
	protected abstract static class RendererTask extends BaseRendererTask {

		protected GLDrawer2D mDrawer;

		public RendererTask(final AbstractRendererHolder parent, final int width, final int height) {
			super(parent, width, height);
		}

		@Override
		protected void internalOnStart() {
			mDrawer = new GLDrawer2D(true);
		}
		
		@Override
		protected void internalOnStop() {
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
		}

		@Override
		protected void preprocess() {
		}
		
		@Override
		protected void onDrawClient(final RendererSurfaceRec client, final int texId, final float[] texMatrix) {
			client.draw(mDrawer, texId, texMatrix);
		}
	}

//--------------------------------------------------------------------------------

	protected void setupCaptureDrawer(final GLDrawer2D drawer) {
	}
	
	/**
	 * 静止画を非同期でキャプチャするためのRunnable
	 */
	private final Runnable mCaptureTask = new Runnable() {
    	EGLBase eglBase;
    	EGLBase.IEglSurface captureSurface;
    	GLDrawer2D drawer;
		final float[] mMvpMatrix = new float[16];

    	@Override
		public void run() {
//			if (DEBUG) Log.v(TAG, "captureTask start");
			synchronized (mSync) {
				// 描画スレッドが実行されるまで待機
				if (!isRunning) {
					try {
						mSync.wait();
					} catch (final InterruptedException e) {
					}
				}
			}
			init();
			try {
				if (eglBase.getGlVersion() > 2) {
					captureLoopGLES3();
				} else {
					captureLoopGLES2();
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			} finally {
				// release resources
				release();
			}
//			if (DEBUG) Log.v(TAG, "captureTask finished");
		}

		private final void init() {
	    	eglBase = EGLBase.createFrom(3, mRendererTask.getContext(), false, 0, false);
	    	captureSurface = eglBase.createOffscreen(mRendererTask.width(), mRendererTask.height());
			Matrix.setIdentityM(mMvpMatrix, 0);
	    	drawer = new GLDrawer2D(true);
			setupCaptureDrawer(drawer);
		}

		private final void captureLoopGLES2() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			File captureFile = null;
			int captureCompression = 90;
//			if (DEBUG) Log.v(TAG, "captureTask loop");
			for (; isRunning ;) {
				synchronized (mSync) {
					if (captureFile == null) {
						if (mCaptureFile == null) {
							try {
								mSync.wait();
							} catch (final InterruptedException e) {
								break;
							}
						}
						if (mCaptureFile != null) {
//							if (DEBUG) Log.i(TAG, "静止画撮影要求を受け取った");
							captureFile = mCaptureFile;
							mCaptureFile = null;
							captureCompression = mCaptureCompression;
							if ((captureCompression <= 0) || (captureCompression >= 100)) {
								captureCompression = 90;
							}
						}
						continue;
					}
					if (buf == null | width != mRendererTask.width() || height != mRendererTask.height()) {
						width = mRendererTask.width();
						height = mRendererTask.height();
						buf = ByteBuffer.allocateDirect(width * height * 4);
				    	buf.order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = eglBase.createOffscreen(width, height);
					}
					if (isRunning) {
						setMirror(mMvpMatrix, mRendererTask.mirror());
						mMvpMatrix[5] *= -1.0f;	// flip up-side down
						drawer.setMvpMatrix(mMvpMatrix, 0);
						captureSurface.makeCurrent();
						drawer.draw(mRendererTask.mTexId, mRendererTask.mTexMatrix, 0);
						captureSurface.swap();
				        buf.clear();
				        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
//				        if (DEBUG) Log.v(TAG, "save pixels to file:" + captureFile);
				        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
				        if (captureFile.toString().endsWith(".jpg")) {
				        	compressFormat = Bitmap.CompressFormat.JPEG;
				        }
				        BufferedOutputStream os = null;
						try {
					        try {
					            os = new BufferedOutputStream(new FileOutputStream(captureFile));
					            final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
						        buf.clear();
					            bmp.copyPixelsFromBuffer(buf);
					            bmp.compress(compressFormat, captureCompression, os);
					            bmp.recycle();
					            os.flush();
					        } finally {
					            if (os != null) os.close();
					        }
						} catch (final FileNotFoundException e) {
							Log.w(TAG, "failed to save file", e);
						} catch (final IOException e) {
							Log.w(TAG, "failed to save file", e);
						}
					}
//					if (DEBUG) Log.i(TAG, "静止画撮影終了");
					captureFile = null;
					mSync.notifyAll();
				}	// end of synchronized (mSync)
			}	// end of for (; isRunning ;)
			synchronized (mSync) {
				mSync.notifyAll();
			}
		}

		// FIXME これはGL|ES3のPBOとglMapBufferRange/glUnmapBufferを使うように変更する
		private final void captureLoopGLES3() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			File captureFile = null;
			int captureCompression = 90;
//			if (DEBUG) Log.v(TAG, "captureTask loop");
			for (; isRunning ;) {
				synchronized (mSync) {
					if (captureFile == null) {
						if (mCaptureFile == null) {
							try {
								mSync.wait();
							} catch (final InterruptedException e) {
								break;
							}
						}
						if (mCaptureFile != null) {
//							if (DEBUG) Log.i(TAG, "静止画撮影要求を受け取った");
							captureFile = mCaptureFile;
							mCaptureFile = null;
							captureCompression = mCaptureCompression;
							if ((captureCompression <= 0) || (captureCompression >= 100)) {
								captureCompression = 90;
							}
						}
						continue;
					}
					if (buf == null | width != mRendererTask.width() || height != mRendererTask.height()) {
						width = mRendererTask.width();
						height = mRendererTask.height();
						buf = ByteBuffer.allocateDirect(width * height * 4);
				    	buf.order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = eglBase.createOffscreen(width, height);
					}
					if (isRunning) {
						setMirror(mMvpMatrix, mRendererTask.mirror());
						mMvpMatrix[5] *= -1.0f;	// flip up-side down
						drawer.setMvpMatrix(mMvpMatrix, 0);
						captureSurface.makeCurrent();
						drawer.draw(mRendererTask.mTexId, mRendererTask.mTexMatrix, 0);
						captureSurface.swap();
				        buf.clear();
				        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
//				        if (DEBUG) Log.v(TAG, "save pixels to file:" + captureFile);
				        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
				        if (captureFile.toString().endsWith(".jpg")) {
				        	compressFormat = Bitmap.CompressFormat.JPEG;
				        }
				        BufferedOutputStream os = null;
						try {
					        try {
					            os = new BufferedOutputStream(new FileOutputStream(captureFile));
					            final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
						        buf.clear();
					            bmp.copyPixelsFromBuffer(buf);
					            bmp.compress(compressFormat, captureCompression, os);
					            bmp.recycle();
					            os.flush();
					        } finally {
					            if (os != null) os.close();
					        }
						} catch (final FileNotFoundException e) {
							Log.w(TAG, "failed to save file", e);
						} catch (final IOException e) {
							Log.w(TAG, "failed to save file", e);
						}
					}
//					if (DEBUG) Log.i(TAG, "静止画撮影終了");
					captureFile = null;
					mSync.notifyAll();
				}	// end of synchronized (mSync)
			}	// end of for (; isRunning ;)
			synchronized (mSync) {
				mSync.notifyAll();
			}
		}

		private final void release() {
			if (captureSurface != null) {
				captureSurface.makeCurrent();
				if (drawer != null) {
					drawer.release();
				}
				captureSurface.release();
				captureSurface = null;
			}
			if (drawer != null) {
				drawer.release();
				drawer = null;
			}
			if (eglBase != null) {
				eglBase.release();
				eglBase = null;
			}
		}
	};

//================================================================================
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
}
