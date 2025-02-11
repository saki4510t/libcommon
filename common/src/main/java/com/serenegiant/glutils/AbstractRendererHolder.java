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
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EglTask;
import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.gl.ISurface;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.serenegiant.gl.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * 分配描画インターフェースの共通部分を実装する抽象クラス
 */
public abstract class AbstractRendererHolder implements IRendererHolder {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = AbstractRendererHolder.class.getSimpleName();
	private static final String RENDERER_THREAD_NAME = "RendererHolder";
	private static final String CAPTURE_THREAD_NAME = "CaptureTask";

	@NonNull
	private final Object mSync = new Object();
	@Nullable
	private final RenderHolderCallback mCallback;
	private volatile boolean isRunning;

	@Nullable
	private OutputStream mCaptureStream;
	@StillCaptureFormat
	private int mCaptureFormat;
	@IntRange(from = 1L,to = 99L)
	private int mCaptureCompression = DEFAULT_CAPTURE_COMPRESSION;
	private OnCapturedListener mOnCapturedListener;
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
		 @Nullable final RenderHolderCallback callback) {

		mCallback = callback;
		mRendererTask = createRendererTask(width, height,
			maxClientVersion, sharedContext, flags);
		mRendererTask.start(RENDERER_THREAD_NAME);
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
		mRendererTask.checkMasterSurface();
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

	/**
	 * パス文字列の拡張子を調べて静止画圧縮フォーマットを取得する。
	 * jpeg(jpg)/png/webpのいずれでもなければIllegalArgumentExceptionを投げる
	 * @param path
	 * @return
	 * @throws IllegalArgumentException
	 */
	@StillCaptureFormat
	private static int getCaptureFormat(@NonNull final String path)
		throws IllegalArgumentException {

		int result;
		final String _path = path.toLowerCase();
		if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
			result = OUTPUT_FORMAT_JPEG;
		} else if (path.endsWith(".png")) {
			result = OUTPUT_FORMAT_PNG;
		} else if (path.endsWith(".webp")) {
			result = OUTPUT_FORMAT_WEBP;
		} else {
			throw new IllegalArgumentException("unknown compress format(extension)");
		}
		return result;
	}
	
	/**
	 * 静止画圧縮フォーマットをBitmap.CompressFormatに変換する
	 * @param captureFormat
	 * @return
	 */
	private static Bitmap.CompressFormat getCaptureFormat(
		@StillCaptureFormat final int captureFormat) {

		Bitmap.CompressFormat result = switch (captureFormat) {
			case OUTPUT_FORMAT_PNG -> Bitmap.CompressFormat.PNG;
			case OUTPUT_FORMAT_WEBP -> Bitmap.CompressFormat.WEBP;
			case OUTPUT_FORMAT_JPEG -> Bitmap.CompressFormat.JPEG;
			default -> Bitmap.CompressFormat.JPEG;
		};
		return result;
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
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags);
	
//--------------------------------------------------------------------------------
	protected void startCaptureTask() {
		new Thread(mCaptureTask, CAPTURE_THREAD_NAME).start();
		synchronized (mSync) {
			if (!isRunning) {
				try {
					mSync.wait();
				} catch (final InterruptedException e) {
					// ignore
				}
			}
		}
	}
	
	protected void notifyCapture() {
//		if (DEBUG) Log.v(TAG, "notifyCapture:");
		synchronized (mSync) {
			// キャプチャタスクに映像が更新されたことを通知
			mSync.notify();
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
	protected static class BaseRendererTask extends AbstractDistributeTask
		implements SurfaceTexture.OnFrameAvailableListener {

		@NonNull
		private final AbstractRendererHolder mParent;
		@NonNull
		private final EglTask mEglTask;
		@Size(min=16)
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

		@NonNull
		@Override
		public EGLBase getEgl() {
			return mEglTask.getEgl();
		}

		@NonNull
		@Override
		public GLContext getGLContext() {
			return mEglTask.getGLContext();
		}

		@Deprecated
		@SuppressWarnings("deprecation")
		@NonNull
		@Override
		public EGLBase.IContext<?> getContext() {
			return mEglTask.getContext();
		}

		@Override
		public int getGlVersion() {
			return mEglTask.getGlVersion();
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
		public boolean isOES3Supported() {
			return mEglTask.isOES3Supported();
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
			mParent.notifyCapture();
		}

		/**
		 * 映像入力用Surfaceを再生成する
		 */
		@SuppressLint("NewApi")
		@WorkerThread
		@Override
		protected void handleReCreateInputSurface() {
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
//			if (DEBUG) Log.v(TAG, "getSurface:" + mInputSurface);
			checkMasterSurface();
			return mInputSurface;
		}

		/**
		 * 映像受け取り用のSurfaceTextureを取得
		 * @return
		 */
		public SurfaceTexture getSurfaceTexture() {
//		if (DEBUG) Log.v(TAG, "getSurfaceTexture:" + mInputTexture);
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
			mEglTask.queueEvent(task);
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

//--------------------------------------------------------------------------------

	protected void setupCaptureDrawer(final GLDrawer2D drawer) {
	}
	
	/**
	 * 静止画を非同期でキャプチャするためのRunnable
	 */
	private final Runnable mCaptureTask = new Runnable() {
		private GLContext mContext;
		private ISurface captureSurface;
		private GLDrawer2D drawer;
		@Size(min=16)
		@NonNull
		private final float[] mMvpMatrix = new float[16];

    	@Override
		public void run() {
//			if (DEBUG) Log.v(TAG, "captureTask start");
			synchronized (mSync) {
				// 描画スレッドが実行されるまで待機
				for (; !isRunning && !mRendererTask.isFinished(); ) {
					try {
						mSync.wait(1000);
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
			if (isRunning) {
				init();
				try {
					if (mContext.isOES3Supported()) {
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
			}
//			if (DEBUG) Log.v(TAG, "captureTask finished");
		}

		private final void init() {
			mContext = new GLContext(mRendererTask.getGLContext());
			mContext.initialize();
	    	captureSurface = mContext.getEgl().createOffscreen(
	    		mRendererTask.width(), mRendererTask.height());
			Matrix.setIdentityM(mMvpMatrix, 0);
			drawer = GLDrawer2D.create(mContext.isOES3Supported(), true);
			setupCaptureDrawer(drawer);
		}

		private final void captureLoopGLES2() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			int captureCompression = DEFAULT_CAPTURE_COMPRESSION;
//			if (DEBUG) Log.v(TAG, "captureTask loop");
			for (; isRunning ;) {
				synchronized (mSync) {
					if (mCaptureStream == null) {
						try {
							mSync.wait();
						} catch (final InterruptedException e) {
							break;
						}
						if (mCaptureStream != null) {
//							if (DEBUG) Log.i(TAG, "静止画撮影要求を受け取った");
							captureCompression = mCaptureCompression;
							if ((captureCompression <= 0) || (captureCompression >= 100)) {
								captureCompression = 90;
							}
						} else {
							// 起床されたけどmCaptureStreamがnullだった
							continue;
						}
					}
					if (DEBUG) Log.v(TAG, "#captureLoopGLES2:start capture");
					boolean success = false;
					if ((buf == null)
						|| (width != mRendererTask.width())
						|| (height != mRendererTask.height())) {

						width = mRendererTask.width();
						height = mRendererTask.height();
						final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
						buf = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = mContext.getEgl().createOffscreen(width, height);
					}
					if (isRunning && (width > 0) && (height > 0)) {
						GLUtils.setMirror(mMvpMatrix, mRendererTask.getMirror());
						mMvpMatrix[5] *= -1.0f;	// flip up-side down
						drawer.setMvpMatrix(mMvpMatrix, 0);
						captureSurface.makeCurrent();
						drawer.draw(GLES20.GL_TEXTURE0, mRendererTask.mTexId, mRendererTask.mTexMatrix, 0);
						captureSurface.swap();
				        buf.clear();
				        GLES20.glReadPixels(0, 0, width, height,
				        	GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
//				        if (DEBUG) Log.v(TAG, "save pixels to file:" + captureFile);
				        final Bitmap.CompressFormat compressFormat
				        	= getCaptureFormat(mCaptureFormat);
						try {
					        try {
					            final Bitmap bmp = Bitmap.createBitmap(
					            	width, height, Bitmap.Config.ARGB_8888);
						        buf.clear();
					            bmp.copyPixelsFromBuffer(buf);
					            bmp.compress(compressFormat, captureCompression, mCaptureStream);
					            bmp.recycle();
								mCaptureStream.flush();
								success = true;
					        } finally {
					            mCaptureStream.close();
					        }
						} catch (final IOException e) {
							Log.w(TAG, "failed to save file", e);
						}
					} else if (isRunning) {
						Log.w(TAG, "#captureLoopGLES3:unexpectedly width/height is zero");
					}
					if (DEBUG) Log.i(TAG, "#captureLoopGLES2:静止画撮影終了");
					mCaptureStream = null;
					if (mOnCapturedListener != null) {
						try {
							mOnCapturedListener.onCaptured(AbstractRendererHolder.this, success);
						} catch (final Exception e) {
							if (DEBUG) Log.w(TAG, e);
						}
					}
					mOnCapturedListener = null;
					mSync.notifyAll();
				}	// end of synchronized (mSync)
			}	// end of for (; isRunning ;)
			synchronized (mSync) {
				mSync.notifyAll();
			}
		}

		private final void captureLoopGLES3() {
			int width = -1, height = -1;
			ByteBuffer buf = null;
			int captureCompression = 90;
//			if (DEBUG) Log.v(TAG, "captureTask loop");
			for (; isRunning ;) {
				synchronized (mSync) {
					if (mCaptureStream == null) {
						try {
							mSync.wait();
						} catch (final InterruptedException e) {
							break;
						}
						if (mCaptureStream != null) {
//							if (DEBUG) Log.i(TAG, "静止画撮影要求を受け取った");
							captureCompression = mCaptureCompression;
							if ((captureCompression <= 0) || (captureCompression >= 100)) {
								captureCompression = 90;
							}
						} else {
							// 起床されたけどmCaptureStreamがnullだった
							continue;
						}
					}
					if (DEBUG) Log.v(TAG, "#captureLoopGLES3:start capture");
					boolean success = false;
					if ((buf == null)
						|| (width != mRendererTask.width())
						|| (height != mRendererTask.height())) {

						width = mRendererTask.width();
						height = mRendererTask.height();
						final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
						buf = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
				    	if (captureSurface != null) {
				    		captureSurface.release();
				    		captureSurface = null;
				    	}
				    	captureSurface = mContext.getEgl().createOffscreen(width, height);
					}
					if (isRunning && (width > 0) && (height > 0)) {
						GLUtils.setMirror(mMvpMatrix, mRendererTask.getMirror());
						mMvpMatrix[5] *= -1.0f;	// flip up-side down
						drawer.setMvpMatrix(mMvpMatrix, 0);
						captureSurface.makeCurrent();
						drawer.draw(GLES20.GL_TEXTURE0, mRendererTask.mTexId, mRendererTask.mTexMatrix, 0);
						captureSurface.swap();
				        buf.clear();
						// FIXME これはGL|ES3のPBOとglMapBufferRange/glUnmapBufferを使うように変更する
				        GLES30.glReadPixels(0, 0, width, height,
							GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf);
//				        if (DEBUG) Log.v(TAG, "save pixels to file:" + captureFile);
						final Bitmap.CompressFormat compressFormat
							= getCaptureFormat(mCaptureFormat);
						try {
					        try {
					            final Bitmap bmp = Bitmap.createBitmap(
					            	width, height, Bitmap.Config.ARGB_8888);
						        buf.clear();
					            bmp.copyPixelsFromBuffer(buf);
					            bmp.compress(compressFormat, captureCompression, mCaptureStream);
					            bmp.recycle();
								mCaptureStream.flush();
								success = true;
					        } finally {
					            mCaptureStream.close();
					        }
						} catch (final IOException e) {
							Log.w(TAG, "failed to save file", e);
						}
					} else if (isRunning) {
						Log.w(TAG, "#captureLoopGLES3:unexpectedly width/height is zero");
					}
					if (DEBUG) Log.i(TAG, "#captureLoopGLES3:静止画撮影終了");
					mCaptureStream = null;
					if (mOnCapturedListener != null) {
						try {
							mOnCapturedListener.onCaptured(AbstractRendererHolder.this, success);
						} catch (final Exception e) {
							if (DEBUG) Log.w(TAG, e);
						}
					}
					mOnCapturedListener = null;
					mSync.notifyAll();
				}	// end of synchronized (mSync)
			}	// end of for (; isRunning ;)
			synchronized (mSync) {
				mSync.notifyAll();
			}
		}

		private void release() {
			if (captureSurface != null) {
				captureSurface.makeCurrent();
				captureSurface.release();
				captureSurface = null;
			}
			if (drawer != null) {
				drawer.release();
				drawer = null;
			}
			if (mContext != null) {
				mContext.release();
				mContext = null;
			}
		}
	};	// mCaptureTask

}
