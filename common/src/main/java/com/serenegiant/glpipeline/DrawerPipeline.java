package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 任意のGLDrawer2D(とその継承クラス)を使って描画するGLPipeline実装
 * 描画先のsurfaceにnullを指定すると描画後のテクスチャを次のGLPipelineへ送る
 */
public class DrawerPipeline extends ProxyPipeline
	implements GLSurfacePipeline, IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = DrawerPipeline.class.getSimpleName();

	/**
	 * GLDrawer2D生成・破棄のコールバックリスナー
	 */
	public interface Callback {
		@WorkerThread
		@NonNull
		public GLDrawer2D createDrawer(
			@NonNull final GLManager glManager, final boolean isOES);
		@WorkerThread
		public void releaseDrawer(
			@NonNull final GLManager glManager, @NonNull final GLDrawer2D drawer);
		@WorkerThread
		@Nullable
		public GLDrawer2D onResize(
			@NonNull final GLManager glManager, @Nullable final GLDrawer2D drawer,
			final int width, final int height);
	}

	/**
	 * デフォルトのCallbackインターフェースの実装
	 */
	public static Callback DEFAULT_CALLBACK = new Callback() {
		@NonNull
		@Override
		public GLDrawer2D createDrawer(@NonNull final GLManager glManager, final boolean isOES) {
			return GLDrawer2D.create(glManager.isGLES3(), isOES);
		}

		@Override
		public void releaseDrawer(@NonNull final GLManager glManager, @NonNull final GLDrawer2D drawer) {
			drawer.release();
		}

		@Nullable
		@Override
		public GLDrawer2D onResize(
			@NonNull final GLManager glManager, @Nullable final GLDrawer2D drawer,
			final int width, final int height) {

			if (drawer != null) {
				drawer.release();
			}
			return null;
		}
	};

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;
	@NonNull
	private final Callback mCallback;
	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * 描画処理した後のテクスチャを次のGLPipelineへ送るかSurfaceへ描画するか
	 * setSurfaceで有効な描画先Surfaceをセットしていればfalse、セットしていなければtrue
	 */
	private volatile boolean mDrawOnly;
	/**
	 * 描画処理した結果を次のGLPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface offscreenSurface;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param callback
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager, @NonNull final Callback callback)
		throws IllegalStateException, IllegalArgumentException {

		this(manager,  callback, null, null);
	}

	/**
	 * コンストラクタ
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param manager
	 * @param callback
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public DrawerPipeline(
		@NonNull final GLManager manager, @NonNull final Callback callback,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
		mCallback = callback;
		manager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				createTargetOnGL(surface, maxFps);
			}
		});
	}

	@Override
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		if (isValid()) {
			releaseAll();
		}
		super.internalRelease();
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え, 最大フレームレートの制限をしない
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setSurface(@Nullable final Object surface)
		throws IllegalStateException, IllegalArgumentException {

		setSurface(surface, null);
	}

	/**
	 * GLSurfacePipelineの実装
	 * 描画先のSurfaceを差し替え
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Override
	public void setSurface(
		@Nullable final Object surface,
		@Nullable final Fraction maxFps) throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setSurface:" + surface);
		if (!isValid()) {
			throw new IllegalStateException("already released?");
		}
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				createTargetOnGL(surface, maxFps);
			}
		});
	}

	/**
	 * 描画先のSurfaceをセットしているかどうか
	 * #isEffectOnlyの符号反転したのものと実質的には同じ
	 * @return
	 */
	@Override
	public boolean hasSurface() {
		synchronized (mSync) {
			return mRendererTarget != null;
		}
	}

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	@Override
	public int getId() {
		synchronized (mSync) {
			return mRendererTarget != null ? mRendererTarget.getId() : 0;
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	/**
	 * 描画処理した後のテクスチャを次のGLPipelineへ送るかSurfaceへ描画するか
	 * コンストラクタまたはsetSurfaceで描画先のsurfaceにnullを指定するとtrue
	 * @return
	 */
	public boolean isDrawOnly() {
		return mDrawOnly;
	}

	@Override
	public void setMirror(@MirrorMode final int mirror) {
		synchronized (mSync) {
			if (mMirror != mirror) {
				mMirror = mirror;
				mManager.runOnGLThread(() -> {
					if (mRendererTarget != null) {
						mRendererTarget.setMirror(IMirror.flipVertical(mirror));
					}
				});
			}
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		synchronized (mSync) {
			return mMirror;
		}
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		if ((mDrawer == null) || (isOES != mDrawer.isOES())) {
			// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
			if (mDrawer != null) {
				mCallback.releaseDrawer(mManager, mDrawer);
			}
			if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
			mDrawer = mCallback.createDrawer(mManager, isOES);
		}
		@Nullable
		final RendererTarget target;
		synchronized (mSync) {
			target = mRendererTarget;
		}
		if ((target != null)
			&& target.canDraw()) {
			target.draw(mDrawer, GLES20.GL_TEXTURE0, texId, texMatrix);
		}
		if (mDrawOnly && (offscreenSurface != null)) {
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:effectOnly," + cnt);
			}
			// 描画処理した後のたテクスチャを次へ渡す
			super.onFrameAvailable(offscreenSurface.isOES(), offscreenSurface.getTexId(), offscreenSurface.getTexMatrix());
		} else {
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:" + cnt);
			}
			// こっちはオリジナルのテクスチャを渡す
			super.onFrameAvailable(isOES, texId, texMatrix);
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		if (DEBUG) Log.v(TAG, "refresh:");
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		// XXX パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
					if (mDrawer != null) {
						mCallback.releaseDrawer(mManager, mDrawer);
						mDrawer = null;
					}
				}
			});
		}
	}

	@CallSuper
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		super.resize(width, height);
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		mManager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				if (DEBUG) Log.v(TAG, "resize#run:");
				mDrawer = mCallback.onResize(mManager, mDrawer, width, height);
			}
		});
	}

//--------------------------------------------------------------------------------
	private void releaseAll() {
		if (DEBUG) Log.v(TAG, "releaseAll:");
		if (mManager.isValid()) {
			try {
				mManager.runOnGLThread(new Runnable() {
					@WorkerThread
					@Override
					public void run() {
						if (DEBUG) Log.v(TAG, "releaseAll#run:");
						synchronized (mSync) {
							if (mRendererTarget != null) {
								if (DEBUG) Log.v(TAG, "releaseAll:release target");
								mRendererTarget.release();
								mRendererTarget = null;
							}
							if (offscreenSurface != null) {
								if (DEBUG) Log.v(TAG, "releaseAll:release work");
								offscreenSurface.release();
								offscreenSurface = null;
							}
						}
						if (mDrawer != null) {
							mCallback.releaseDrawer(mManager, mDrawer);
						}
						mDrawer = null;
					}
				});
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		} else if (DEBUG) {
			Log.w(TAG, "releaseAll:unexpectedly GLManager is already released!");
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
		synchronized (mSync) {
			if ((mRendererTarget == null) || (mRendererTarget.getSurface() != surface)) {
				if (mRendererTarget != null) {
					mRendererTarget.release();
					mRendererTarget = null;
				}
				if (offscreenSurface != null) {
					offscreenSurface.release();
					offscreenSurface = null;
				}
				if (GLUtils.isSupportedSurface(surface)) {
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
					mDrawOnly = false;
				} else if (isValid()) {
					if (DEBUG) Log.v(TAG, "createTarget:create GLSurface as work texture");
					offscreenSurface = GLSurface.newInstance(mManager.isGLES3(), getWidth(), getHeight());
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), offscreenSurface, maxFps != null ? maxFps.asFloat() : 0);
					mDrawOnly = true;
				}
				if (mRendererTarget != null) {
					mRendererTarget.setMirror(IMirror.flipVertical(mMirror));
				}
			}
		}
	}

}
