package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.utils.BuildCheck;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * Hold shared texture that has camera frame and draw them to registered surface if needs<br>
 */
public class RendererHolder implements IRendererHolder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = RendererHolder.class.getSimpleName();

	private final Object mSync = new Object();
	private final RenderHolderCallback mCallback;
	private volatile boolean isRunning;
	private File mCaptureFile;

	private final RendererTask mRendererTask;

	public RendererHolder(final int width, final int height, @Nullable final RenderHolderCallback callback) {
//		if (DEBUG) Log.v(TAG, "Constructor");
		mCallback = callback;
		mRendererTask = new RendererTask(this, width, height);
		new Thread(mRendererTask, TAG).start();
		if (!mRendererTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
		new Thread(mCaptureTask, "CaptureTask").start();
		synchronized (mSync) {
			if (!isRunning) {
				try {
					mSync.wait();
				} catch (final InterruptedException e) {
				}
			}
		}
//		if (DEBUG) Log.v(TAG, "Constructor:finished");
	}

//================================================================================
// IRendererHolderの実装
//================================================================================
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
	public void setMirror(final int mirror) {
		mRendererTask.mirror(mirror % MIRROR_NUM);
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
//		if (DEBUG) Log.v(TAG, "captureStill:" + path);
		final File file = new File(path);
		synchronized (mSync) {
			mCaptureFile = file;
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
//		if (DEBUG) Log.v(TAG, "captureStill:" + path);
		final File file = new File(path);
		synchronized (mSync) {
			mCaptureFile = file;
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

//================================================================================
// 実装
//================================================================================
	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_ADD_SURFACE = 3;
	private static final int REQUEST_REMOVE_SURFACE = 4;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
	private static final int REQUEST_MIRROR = 6;

	/**
	 * ワーカースレッド上でOpenGL|ESを用いてマスター映像を分配描画するためのインナークラス
	 * TODO EffectRendererHolder#RendererTaskの共通部分をまとめる
	 */
	private static final class RendererTask extends EglTask {

		private final Object mClientSync = new Object();
		private final SparseArray<RendererSurfaceRec> mClients = new SparseArray<RendererSurfaceRec>();
		private final RendererHolder mParent;
		private GLDrawer2D mDrawer;
		private int mTexId;
		private SurfaceTexture mMasterTexture;
		final float[] mTexMatrix = new float[16];
		private Surface mMasterSurface;
		private int mVideoWidth, mVideoHeight;
		private int mMirror = -1;

		public RendererTask(final RendererHolder parent, final int width, final int height) {
			super(3, null, EglTask.EGL_FLAG_RECORDABLE);
			mParent = parent;
			mVideoWidth = width;
			mVideoHeight = height;
		}

		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@Override
		protected void onStart() {
//			if (DEBUG) Log.v(TAG, "onStart:");
			mDrawer = new GLDrawer2D(true);		// GL_TEXTURE_EXTERNAL_OESを使う
			handleReCreateMasterSurface();
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
//			if (DEBUG) Log.v(TAG, "onStop");
			synchronized (mParent.mSync) {
				mParent.isRunning = false;
				mParent.mSync.notifyAll();
			}
			makeCurrent();
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			handleReleaseMasterSurface();
			handleRemoveAll();
//			if (DEBUG) Log.v(TAG, "onStop:finished");
		}

		@Override
		protected boolean onError(final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
			return false;
		}

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
//			if (DEBUG) Log.v(TAG, "getSurfaceTexture:" + mMasterTexture);
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
				if ((surface != null) && (mClients.get(id) == null)) {
					for ( ; ; ) {
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
					for ( ; ; ) {
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

		public void mirror(final int mirror) {
			checkFinished();
			if (mMirror != mirror) {
				offer(REQUEST_MIRROR, mirror);
			}
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

		private void checkFinished() {
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
		private void handleDraw() {
			if ((mMasterSurface == null) || (!mMasterSurface.isValid())) {
				Log.w(TAG, "checkMasterSurface:invalid master surface");
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			try {
				makeCurrent();
				mMasterTexture.updateTexImage();
				mMasterTexture.getTransformMatrix(mTexMatrix);
			} catch (final Exception e) {
				Log.w(TAG, "draw:thread id =" + Thread.currentThread().getId(), e);
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			synchronized (mParent.mCaptureTask) {
				// キャプチャタスクに映像が更新されたことを通知
				mParent.mCaptureTask.notify();
			}
			// 各Surfaceへ描画する
			synchronized (mClientSync) {
				final int n = mClients.size();
				RendererSurfaceRec client;
				for (int i = n - 1; i >= 0; i--) {
					client = mClients.valueAt(i);
					if ((client != null) && client.canDraw()) {
						try {
							client.draw(mDrawer, mTexId, mTexMatrix);
						} catch (final Exception e) {
							// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
							mClients.removeAt(i);
							client.release();
						}
					}
				}
			}
			if (mParent.mCallback != null) {
				try {
					mParent.mCallback.onFrameAvailable();
				} catch (final Exception e) {
				}
			}
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glFlush();
		}

		/**
		 * 指定したIDの分配描画先Surfaceを追加する
		 * @param id
		 * @param surface
		 */
		private void handleAddSurface(final int id, final Object surface, final int maxFps) {
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
		private void handleRemoveSurface(final int id) {
//			if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
			synchronized (mClientSync) {
				final RendererSurfaceRec client = mClients.get(id);
				if (client != null) {
					mClients.remove(id);
					client.release();
				}
				checkSurface();
				mClientSync.notifyAll();
			}
		}

		/**
		 * 念の為に分配描画先のSurfaceを全て破棄する
		 */
		private void handleRemoveAll() {
//			if (DEBUG) Log.v(TAG, "handleRemoveAll:");
			synchronized (mClientSync) {
				final int n = mClients.size();
				RendererSurfaceRec client;
				for (int i = 0; i < n; i++) {
					client = mClients.valueAt(i);
					if (client != null) {
						makeCurrent();
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
		private void checkSurface() {
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
		 * マスターSurfaceを再生成する
		 */
		@SuppressLint("NewApi")
		private void handleReCreateMasterSurface() {
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
			try {
				if (mParent.mCallback != null) {
					mParent.mCallback.onCreate(mMasterSurface);
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}

		/**
		 * マスターSurfaceを破棄する
		 */
		private void handleReleaseMasterSurface() {
			try {
				if (mParent.mCallback != null) {
					mParent.mCallback.onDestroy();
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mMasterSurface = null;
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
		private void handleResize(final int width, final int height) {
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
		private void handleMirror(final int mirror) {
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
		private void setMirror(final RendererSurfaceRec client, final int mirror) {
			final float[] mvp = client.mMvpMatrix;
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
		 * TextureSurfaceで映像を受け取った際のコールバックリスナー
		 */
		private final OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
				offer(REQUEST_DRAW);
			}
		};

	}

	/**
	 * 静止画を非同期でキャプチャするためのRunnable
	 */
	private final Runnable mCaptureTask = new Runnable() {
    	EGLBase eglBase;
    	EGLBase.IEglSurface captureSurface;
    	GLDrawer2D drawer;

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
			if (eglBase.getGlVersion() > 2) {
				captureLoopGLES3();
			} else {
				captureLoopGLES2();
			}
			// release resources
			release();
//			if (DEBUG) Log.v(TAG, "captureTask finished");
		}

		private final void init() {
	    	eglBase = EGLBase.createFrom(3, mRendererTask.getContext(), false, 0, false);
	    	captureSurface = eglBase.createOffscreen(mRendererTask.mVideoWidth, mRendererTask.mVideoHeight);
	    	drawer = new GLDrawer2D(true);
	    	drawer.getMvpMatrix()[5] *= -1.0f;	// flip up-side down
		}

		private final void captureLoopGLES2() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			File captureFile = null;
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
						}
						continue;
					}
					if (buf == null | width != mRendererTask.mVideoWidth || height != mRendererTask.mVideoHeight) {
						width = mRendererTask.mVideoWidth;
						height = mRendererTask.mVideoHeight;
						buf = ByteBuffer.allocateDirect(width * height * 4);
				    	buf.order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = eglBase.createOffscreen(width, height);
					}
					if (isRunning) {
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
					            bmp.compress(compressFormat, 90, os);
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
		}

		// FIXME これはGL|ES3のPBOとglMapBufferRange/glUnmapBufferを使うように変更する
		private final void captureLoopGLES3() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			File captureFile = null;
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
						}
						continue;
					}
					if (buf == null | width != mRendererTask.mVideoWidth || height != mRendererTask.mVideoHeight) {
						width = mRendererTask.mVideoWidth;
						height = mRendererTask.mVideoHeight;
						buf = ByteBuffer.allocateDirect(width * height * 4);
				    	buf.order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = eglBase.createOffscreen(width, height);
					}
					if (isRunning) {
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
					            bmp.compress(compressFormat, 90, os);
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

}
