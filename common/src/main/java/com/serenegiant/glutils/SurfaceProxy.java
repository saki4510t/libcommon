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
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLConst;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * Surfaceで受け取った映像をSurfaceへ引き渡すためのヘルパークラス
 * SurfaceViewやGLSurfaceView, TextureViewだとSurface/SurfaceTextureの生成のタイミングや
 * ライフサイクルに縛りがあるので縛りのない処理を組み合わせるのが無駄に複雑になる。
 * このクラスを通してSurfaceへアクセスすることでView側の縛りに依存しない記述が可能になる。
 * RendererHolder系での同様のことが可能であるが、分配描画が不要な場合にはオーバースペックなので
 * 単純なプロキシするだけのクラスとして追加。
 */
public abstract class SurfaceProxy implements GLConst, IMirror {
	private static final String TAG = SurfaceProxy.class.getSimpleName();

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * preferReaderWriter=trueでAPI>=23ならandroid.media.ImageReader/ImageWriterを
	 * 使った実装を使う。falseならOpenGL|ESとSurfaceTextureを使う。
	 * ただしImageReader/ImageWriterを使う場合は次の制限がある。
	 * ・動的なリサイズができない(破棄＆生成が必要)
	 * ・ミラー設定ができない
	 * ・フレームレート制限ができない
	 * ・Surfaceで受け取る映像フォーマットがPixelFormat.RGBA_8888固定(かもしれない)
	 * @param width
	 * @param height
	 * @param preferReaderWriter　可能な場合にandroid.media.ImageReader/ImageWriterを使うかどうか
	 * @return
	 */
	public static SurfaceProxy newInstance(
		final int width, final int height,
		final boolean preferReaderWriter) {

		if (preferReaderWriter && BuildCheck.isAPI23()) {
			return new SurfaceProxyReaderWriter(width, height);
		} else {
			return new SurfaceProxyGLES(width, height);
		}
	}

	/**
	 * 排他制御用
	 */
	@NonNull
	protected final ReentrantLock mLock = new ReentrantLock();
	private volatile boolean mReleased = false;
	private int mWidth, mHeight;
	@MirrorMode
	protected int mMirror = MIRROR_NORMAL;

	private SurfaceProxy(final int width, final int height) {
		mWidth = Math.max(width, 1);
		mHeight = Math.max(height, 1);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public final void release() {
		if (!mReleased) {
			mReleased = true;
			internalRelease();
		}
	}

	@CallSuper
	protected void internalRelease() {
		mReleased = true;
	}

	@CallSuper
	public boolean isValid() {
		return !mReleased;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	@MirrorMode
	@Override
	public int getMirror() {
		mLock.lock();
		try {
			return mMirror;
		} finally {
			mLock.unlock();
		}
	}

	@CallSuper
	public void resize(final int width, final int height) {
		mWidth = Math.max(width, 1);
		mHeight = Math.max(height, 1);
	}

	/**
	 * 映像入力用のSurfaceを取得
	 * @return
	 */
	@NonNull
	public abstract Surface getInputSurface();

	/**
	 * 映像受け取り用Surfaceを設定
	 * @param surface
	 */
	public void setSurface(@Nullable final Object surface) {
		setSurface(surface, null);
	}

	/**
	 * 映像受け取り用Surfaceを設定
	 * @param surface
	 * @param fps 最大フレームレート, nullまたは0以下なら無制限
	 */
	public abstract void setSurface(@Nullable final Object surface, @Nullable final Fraction fps);

//--------------------------------------------------------------------------------
	/**
	 * ImageReader(API>=19)とImageWriter(API>=23)を組み合わせてsurfaceの
	 * プロキシを行うSurfaceProxy実装
	 * XXX 途中で映像サイズを変更できない。
	 * XXX 今のところSurfaceの映像データフォーマットはPixelFormat.RGBA_8888固定
	 *     SurfaceTextureからの映像やCanvasで書き込む場合はPixelFormat.RGBA_8888
	 * XXX 最大フレームレート制限ができない
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	public static class SurfaceProxyReaderWriter extends SurfaceProxy {
		private static final boolean DEBUG = false;	// set false on production
		private static final String TAG = SurfaceProxyReaderWriter.class.getSimpleName();
		/**
		 * 同時に扱えるイメージの数, 2以上
		 */
		private static final int MAX_IMAGES = 2;

		private final Handler mAsyncHandler;
		private ImageReader mImageReader;
		private ImageWriter mImageWriter;

		@SuppressLint("WrongConstant")
		private SurfaceProxyReaderWriter(final int width, final int height) {
			super(width, height);
			mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
			mImageReader = ImageReader.newInstance(getWidth(), getHeight(),  PixelFormat.RGBA_8888, MAX_IMAGES);
			mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
				private int cnt = 0;
				@Override
				public void onImageAvailable(final ImageReader reader) {
					if (DEBUG) Log.v(TAG, "onImageAvailable:" + (++cnt));
					// 入力用SurfaceからImageReaderが受け取ったImageを
					// ImageWriterへ書き込んでSurfaceへコピーする
					final Image image = reader.acquireLatestImage();
					if (image != null) {
						mLock.lock();
						try {
							if (mImageWriter != null) {
								if (DEBUG) Log.v(TAG, "onImageAvailable:queueInputImage");
								mImageWriter.queueInputImage(image); // 自動的にcloseされる
							} else {
								image.close();
							}
						} finally {
							mLock.unlock();
						}
					}
				}
			}, mAsyncHandler);
		}

		@Override
		protected void internalRelease() {
			mLock.lock();
			try {
				if (mImageWriter != null) {
					mImageWriter.close();
					mImageWriter = null;
				}
				if (mImageReader != null) {
					mImageReader.close();
					mImageReader = null;
				}
			} finally {
				mLock.unlock();
			}
			HandlerUtils.NoThrowQuit(mAsyncHandler);
			super.internalRelease();
		}

		@NonNull
		@Override
		public Surface getInputSurface() {
			mLock.lock();
			try {
				if ((mImageReader != null) && isValid()) {
					// ImageReaderからのSurfaceを映像入力用に返す
					return mImageReader.getSurface();
				} else {
					throw new IllegalStateException("already released?");
				}
			} finally {
				mLock.unlock();
			}
		}

		/**
		 * 映像受け取り用Surfaceを設定
		 * ImageReader/ImageWriterを使う場合は最大フレームレート指定は無視される
		 * @param surface
		 * @param fps 最大フレームレート, nullまたは0以下なら無制限(SurfaceProxyReaderWriterでは無視される)
		 */
		@Override
		public void setSurface(@Nullable final Object surface, @Nullable final Fraction fps) {
			if (DEBUG) Log.v(TAG, "setSurface:" + surface);
			mLock.lock();
			try {
				if (mImageWriter != null) {
					mImageWriter.close();
					mImageWriter = null;
				}
				if (surface instanceof Surface) {
					if (DEBUG) Log.v(TAG, "setSurface:create ImageWriter");
					mImageWriter = ImageWriter.newInstance((Surface)surface, MAX_IMAGES);
					// XXX ImageReaderからのImageをキューに入れるだけならOnImageReleasedListenerやdequeueInputImageは不要
//					mImageWriter.setOnImageReleasedListener(mOnImageReleasedListener, mAsyncHandler);
				} else if (surface != null) {
					throw new IllegalArgumentException("Unsupported surface type," + surface);
				}
			} finally {
				mLock.unlock();
			}
		}

		@Override
		public void resize(final int width, final int height) {
			super.resize(width, height);
			throw new UnsupportedOperationException("SurfaceProxyReaderWriter does not support #resize");
		}

		@Override
		public void setMirror(final int mirror) {
			throw new UnsupportedOperationException("SurfaceProxyReaderWriter does not support #setMirror");
		}

//		private final ImageWriter.OnImageReleasedListener
//			mOnImageReleasedListener = new ImageWriter.OnImageReleasedListener() {
//			private int cnt = 0;
//			@Override
//			public void onImageReleased(final ImageWriter writer) {
//				if (DEBUG) Log.v(TAG, "onImageReleased:" + (++cnt));
//				final Image image = mImageWriter.dequeueInputImage();
//				if (image != null) {
//					image.close();
//				}
//			}
//		};
	}

//--------------------------------------------------------------------------------

	/**
	 * OpenGL|ESを使ってSurfaceで受け取った映像をSurfaceへ引き渡すためのSurfaceProxy実装
	 * 内部動作的には、
	 * ・GLSurfaceReceiverからSurface/SurfaceTextureを入力用Surfaceとして取得
	 * 　= OESテクスチャとして映像を受け取り
	 * ・出力用SurfaceをRendererTargetとしてOpenGL|ESでの描画先として
	 * 　今のところ出力用RendererTargetのモデルビュー変換行列を上下反転させてある
	 * ・GLDrawer2DでOESテクスチャを出力用SurfaceをRendererTargetへ描画
	 */
	public static class SurfaceProxyGLES extends SurfaceProxy {
		private static final boolean DEBUG = false;	// set false on production
		private static final String TAG = SurfaceProxyGLES.class.getSimpleName();

		@NonNull
		private final GLSurfaceReceiver mReceiver;
		@NonNull
		private final GLManager mManager;
		// 映像転送用
		@Nullable
		private GLDrawer2D mDrawer = null;
		@Nullable
		private RendererTarget mRendererTarget = null;

		/**
		 * コンストラクタ
		 * @param width
		 * @param height
		 */
		private SurfaceProxyGLES(
			final int width, final int height) throws IllegalStateException {
			this(new GLManager(), width, height);
		}

		/**
		 * コンストラクタ
		 * @param manager
		 * @param width
		 * @param height
		 */
		public SurfaceProxyGLES(
			@NonNull final GLManager manager,
			final int width, final int height) throws IllegalStateException {

			super(width, height);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");

			// 映像入力用Surface/SurfaceTextureが生成されるのを待機するためのSemaphore
			final Semaphore sem = new Semaphore(0);
			mReceiver = new GLSurfaceReceiver(
				manager,
				width, height,
				new GLSurfaceReceiver.Callback() {

				@WorkerThread
				@Override
				public void onInitialize() {
					if (DEBUG) Log.v(TAG, "onInitialize:");
				}

				@WorkerThread
				@Override
				public void onRelease() {
					if (DEBUG) Log.v(TAG, "onRelease:");
					releaseTargetOnGL();
				}

				@WorkerThread
				@Override
				public void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {
					if (DEBUG) Log.v(TAG, "onCreateInputSurface:");
					sem.release();
				}

				@WorkerThread
				@Override
				public void onReleaseInputSurface(@NonNull final Surface surface) {
					if (DEBUG) Log.v(TAG, "onReleaseInputSurface:");
				}

				@WorkerThread
				@Override
				public void onResize(final int width, final int height) {
					if (DEBUG) Log.v(TAG, "onResize:");
				}

				@WorkerThread
				@Override
				public void onFrameAvailable(
					final boolean isGLES3, final boolean isOES,
					final int width, final int height,
					final int texId, @NonNull final float[] texMatrix) {
					renderTargetOnGL(isGLES3, isOES, texId, texMatrix);
				}
			});
			// 映像入力用Surface/SurfaceTextureが生成されるのを待機
			try {
				if (!sem.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
					throw new IllegalStateException();
				}
			} catch (final InterruptedException e) {
				// ignore
			}
			// 利便性のためにコピーしておく
			mManager = mReceiver.getGLManager();
			getInputSurface();
		}

		@CallSuper
		protected void internalRelease() {
			if (DEBUG) Log.v(TAG, "internalRelease:" + this);
			if (isValid()) {
				mReceiver.release();
			}
			super.internalRelease();
		}

		/**
		 * GLPipelineの実装
		 * リサイズ要求
		 * @param width
		 * @param height
		 */
		public void resize(final int width, final int height) throws IllegalStateException {
			if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
			super.resize(width, height);
			checkValid();
			mReceiver.resize(width, height);
		}

		@Override
		public void setMirror(@MirrorMode final int mirror) {
			mLock.lock();
			try {
				if (mMirror != mirror) {
					mMirror = mirror;
					mManager.runOnGLThread(() -> {
						if (mRendererTarget != null) {
							mRendererTarget.setMirror(mirror);
						}
					});
				}
			} finally {
				mLock.unlock();
			}
		}

		/**
		 * GLPipelineの実装
		 * VideoSourceオブジェクトが有効かどうかを取得
		 * @return
		 */
		@Override
		public boolean isValid() {
			return super.isValid() && mReceiver.isValid();
		}

		/**
		 * GLPipelineSourceの実装
		 * 映像入力用のSurfaceTextureを取得
		 * @return
		 * @throws IllegalStateException
		 */
		@NonNull
		public SurfaceTexture getInputSurfaceTexture() throws IllegalStateException {
			checkValid();
			return mReceiver.getSurfaceTexture();
		}

		/**
		 * GLPipelineSourceの実装
		 * 映像入力用のSurfaceを取得
		 * @return
		 * @throws IllegalStateException
		 */
		@NonNull
		@Override
		public Surface getInputSurface() throws IllegalStateException {
			checkValid();
			return mReceiver.getSurface();
		}

		/**
		 * GLPipelineSourceの実装
		 * テクスチャ名を取得
		 * @return
		 */
		public int getTexId() {
			checkValid();
			return mReceiver.getTexId();
		}

		/**
		 * GLPipelineSourceの実装
		 * テクスチャ変換行列を取得
		 * @return
		 */
		@Size(value=16)
		@NonNull
		public float[] getTexMatrix() {
			checkValid();
			return mReceiver.getTexMatrix();
		}

		/**
		 * 描画先のSurfaceを差し替え, 最大フレームレートの制限を指定する
		 * すでにsurfaceがセットされている時に#setSurfaceで違うsurfaceをセットすると古いsurfaceは破棄される
		 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
		 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
		 * @param maxFps 最大フレームレート, nullまた0以下なら無制限
		 * @throws IllegalStateException
		 * @throws IllegalArgumentException
		 */
		@Override
		public void setSurface(@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

			if (DEBUG) Log.v(TAG, "setSurface:" + surface + ",maxFps=" + maxFps);
			checkValid();
			mManager.runOnGLThread(() -> {
				createTargetOnGL(surface, maxFps);
			});
		}

		@WorkerThread
		protected void makeDefault() {
			mManager.makeDefault();
		}

		protected void checkValid() throws IllegalStateException {
			if (!mReceiver.isValid()) {
				throw new IllegalStateException("Already released");
			}
		}

	//--------------------------------------------------------------------------------
		private int cnt = 0;
		private void renderTargetOnGL(
			final boolean isGLES3,
			final boolean isOES, final int texId,
			@NonNull @Size(min=16) final float[] texMatrix) {

//			if (DEBUG) Log.v(TAG, "renderTargetOnGL:" + cnt);
			@NonNull
			final GLDrawer2D drawer;
			@Nullable
			final RendererTarget target;
			mLock.lock();
			try {
				if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
					// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
					if (mDrawer != null) {
						mDrawer.release();
					}
					if (DEBUG) Log.v(TAG, "renderTargetOnGL:create GLDrawer2D");
					mDrawer = GLDrawer2D.create(isGLES3, isOES);
					mDrawer.setMirror(MIRROR_VERTICAL);	// XXX 今のところ上下反転させないとテストが通らない
				}
				drawer = mDrawer;
				target = mRendererTarget;
			} finally {
				mLock.unlock();
			}
			if ((target != null)
				&& target.canDraw()) {
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "renderTargetOnGL:" + cnt);
				}
			}
		}

		/**
		 * 描画先のSurfaceを生成
		 * @param surface
		 * @param maxFps
		 */
		@WorkerThread
		private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
			if (DEBUG) Log.v(TAG, "createTarget:" + surface);
			mLock.lock();
			try {
				if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
					// すでにRendererTargetが生成されていて描画先surfaceが変更された時
					if (DEBUG) Log.v(TAG, "createTargetOnGL:release RendererTarget");
					mRendererTarget.release();
					mRendererTarget = null;
				}
				if ((mRendererTarget == null) && (surface != null)) {
					if (DEBUG) Log.v(TAG, "createTargetOnGL:create RendererTarget");
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
				}
				if (mRendererTarget != null) {
					mRendererTarget.setMirror(mMirror);
				}
			} finally {
				mLock.unlock();
			}
		}

		@WorkerThread
		private void releaseTargetOnGL() {
			final GLDrawer2D drawer;
			final RendererTarget target;
			mLock.lock();
			try {
				drawer = mDrawer;
				mDrawer = null;
				target = mRendererTarget;
				mRendererTarget = null;
			} finally {
				mLock.unlock();
			}
			if ((drawer != null) || (target != null)) {
				if (DEBUG) Log.v(TAG, "releaseTargetOnGL:");
				if (mManager.isValid()) {
					try {
						mManager.runOnGLThread(() -> {
							if (drawer != null) {
								if (DEBUG) Log.v(TAG, "releaseTargetOnGL:release GLDrawer2D");
								drawer.release();
							}
							if (target != null) {
								if (DEBUG) Log.v(TAG, "releaseTargetOnGL:release RendererTarget");
								target.release();
							}
						});
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
					}
				} else if (DEBUG) {
					Log.w(TAG, "releaseTargetOnGL:unexpectedly GLManager is already released!");
				}
			}
		}
	}
}
