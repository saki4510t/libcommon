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
import android.opengl.GLES20;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLEffectDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * テクスチャとして受け取った映像をSurfaceへ描画するためのクラスの基本部分を実装
 * AbstractRendererHolderの代替
 */
public class GLSurfaceRenderer implements GLFrameAvailableCallback, IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = GLSurfaceRenderer.class.getSimpleName();

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final GLManager mGLManager;
	@NonNull
	private final GLDrawer2D.DrawerFactory mDrawerFactory;
	private volatile boolean mReleased = false;

	// ここのフィールドはmLockによる排他制御が必要
	private int mWidth;
	private int mHeight;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;
	// これより下のフィールドはEGL/GLコンテキスト上でのみアクセスする(排他処理しない)
	@Nullable
	private GLDrawer2D mDrawer;
	@NonNull
	private final SparseArray<RendererTarget> mTargets = new SparseArray<>();

	/**
	 * コンストラクタ
	 * @param glManager
	 * @param width
	 * @param height
	 * @param drawerFactory
	 */
	public GLSurfaceRenderer(
		@NonNull final GLManager glManager,
		final int width, final int height,
		@NonNull final GLDrawer2D.DrawerFactory drawerFactory) {
		if (DEBUG) Log.v(TAG, "コンストラクタ");
		mGLManager = glManager;
		mWidth = width;
		mHeight = height;
		mDrawerFactory = drawerFactory;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関係するリソースを破棄する、再利用はできない
	 */
	public void release() {
		if (!mReleased) {
			mReleased = true;
			if (DEBUG) Log.v(TAG, "release:");
			if (mGLManager.isValid()) {
				mGLManager.getGLHandler().postAtFrontOfQueue(() -> {
					releaseOnGL();
				});
			}
		}
	}

	/**
	 * このクラス委オブジェクトが有効かどうかを取得する
	 * @return
	 */
	public boolean isValid() {
		return !mReleased && mGLManager.isValid();
	}

	/**
	 * IMirrorの実装
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 * @throws IllegalStateException
	 */
	@SuppressLint("WrongThread")
	@AnyThread
	@Override
	public void setMirror(@MirrorMode final int mirror) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "setMirror:" + mirror);
		checkValid();
		mLock.lock();
		try {
			if (mMirror != mirror) {
				mMirror = mirror;
				mGLManager.runOnGLThread(() -> setMirrorOnGL(0, mirror));
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * IMirrorの実装
	 * @return
	 */
	@AnyThread
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

	/**
	 * GLSurfaceReceiver.FrameAvailableCallbackの実装
	 * コンストラクタで引き渡したGLManagerまたはその共有GLコンテキスト上で
	 * 呼び出すこと
	 * @param isGLES3
	 * @param isOES
	 * @param width
	 * @param height
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3, final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull final float[] texMatrix) {

		if (isValid()) {
			if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
				// 初回または途中でテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
				mDrawer = mDrawerFactory.create(isGLES3, isOES);
				if (isOES) {
					// XXX DrawerPipelineと違ってGL_TEXTURE_EXTERNAL_OESの時に上下反転させないとだめみたい
					//     GLUtils#glCopyTextureToBitmapと同じ
					//     常に上下反転させると入力テクスチャがGL_TEXTURE_2Dの時に結果が上下反転してしまう
					mDrawer.setMirror(MIRROR_VERTICAL);
				}
				if (mDrawer instanceof GLEffectDrawer2D) {
					((GLEffectDrawer2D) mDrawer).setTexSize(width, height);
				}
			}
			resizeOnGL(width, height);
			final GLDrawer2D drawer = mDrawer;
			if (drawer != null) {
				drawOnGL(drawer, texId, texMatrix);
			}
		} else if (DEBUG) {
			Log.v(TAG, "onFrameAvailable:not valid, already released?");
		}
	}

	/**
	 * 映像サイズが変更されたときの処理
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	@SuppressLint("WrongThread")
	@AnyThread
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		checkValid();
		mLock.lock();
		try {
			if ((mWidth != width) && (mHeight != height)) {
				mGLManager.runOnGLThread(() -> {
					resizeOnGL(width, height);
				});
			}
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLDrawer2Dを強制的に再生成させる
	 * FIXME このクラスに必要かどうか要検討
	 *       もともとは#removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
	 *       パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
	 */
	@AnyThread
	public void refresh() {
		if (isValid()) {
			mGLManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
				GLDrawer2D drawer = mDrawer;
				mDrawer = null;
				if (drawer != null) {
					drawer.release();
				}
			});
		}
	}

	/**
	 * 描画先のSurfaceを追加
	 * @param id
	 * @param surface
	 * @param maxFps
	 * @throws IllegalStateException
	 */
	@AnyThread
	public void addSurface(
		final int id, @NonNull final Object surface,
		@Nullable final Fraction maxFps) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "addSurface:" + id + ",surface=" + surface);
		checkValid();
		if (!RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		if (isValid()) {
			mGLManager.runOnGLThread(() -> {
				final int mirror = getMirror();
				if (DEBUG) Log.v(TAG, "addSurface:create RendererTarget");
				final RendererTarget target = RendererTarget.newInstance(
					mGLManager.getEgl(),
					surface, maxFps != null ? maxFps.asFloat() : -1.0f);
				target.setMirror(mirror);
				mTargets.append(id, target);
			});
		}
	}

	/**
	 * 描画先のSurfaceを削除
	 * @param id
	 */
	@AnyThread
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:" + id);
		if (isValid()) {
			mGLManager.runOnGLThread(() -> {
				final RendererTarget target = mTargets.get(id);
				mTargets.remove(id);
				if (target != null) {
					target.release();
				}
			});
		}
	}

	@AnyThread
	public void removeSurfaceAll() {
		if (DEBUG) Log.v(TAG, "removeSurfaceAll:");
		if (isValid()) {
			mGLManager.runOnGLThread(() -> releaseOnGL());
		}
	}

	/**
	 * 指定したIDの分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id id=0なら描画先のSurface全てを指定した色で塗りつぶす、#clearSurfaceAllと同じ
	 * @param color
	 * @throws IllegalStateException
	 */
	@AnyThread
	public void clearSurface(final int id, final int color)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "clearSurface:" + id + ",cl=" + color);
		checkValid();
		mGLManager.runOnGLThread(() -> {
			if (id == 0) {
				final int n = mTargets.size();
				for (int i = 0; i < n; i++) {
					final RendererTarget target = mTargets.valueAt(i);
					target.clear(color);
				}
			} else {
				final RendererTarget target = mTargets.get(id);
				if (target != null) {
					target.clear(color);
				}
			}
		});
	}

	/**
	 * すべての分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 * @throws IllegalStateException
	 */
	@AnyThread
	public void clearSurfaceAll(final int color)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "clearSurfaceAll:" + color);
		checkValid();
		mGLManager.runOnGLThread(() -> {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				target.clear(color);
			}
		});

	}

	/**
	 * 指定したidに対応するSurfaceへモデルビュー変換行列を適用する
	 * @param id id=0なら全てのSurfaceへモデルビュー変換行列を適用する
	 * @param offset
	 * @param matrix
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@AnyThread
	public void setMvpMatrix(
		final int id,
		final int offset, @NonNull @Size(min=16) final float[] matrix)
		throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setMvpMatrix:" + id);
		checkValid();
		if ((matrix != null) && (matrix.length >= offset + 16)) {
			mGLManager.runOnGLThread(() -> {
				if (id == 0) {
					final int n = mTargets.size();
					for (int i = 0; i < n; i++) {
						final RendererTarget target = mTargets.valueAt(i);
						System.arraycopy(matrix, offset, target.getMvpMatrix(), 0, 16);
					}
				} else {
					final RendererTarget target = mTargets.get(id);
					if (target != null) {
						System.arraycopy(matrix, offset, target.getMvpMatrix(), 0, 16);
					}
				}
			});
		} else {
			throw new IllegalArgumentException("matrix is too small, should be longer than offset + 16");
		}
	}

	/**
	 * 指定したidに対応するSurfaceへの描画が有効かどうかを取得する
	 * このクラスオブジェクトが既に破棄されている場合や対応するSurfaceが見つからなければfalseを返す
	 * @param id
	 * @return
	 */
	@AnyThread
	public boolean isEnabled(final int id) {
		if (DEBUG) Log.v(TAG, "isEnabled:" + id);
		if (isValid()) {
			if (mGLManager.isGLThread()) {
				final RendererTarget target = mTargets.get(id);
				return (target != null) && target.isEnabled();
			} else {
				final CountDownLatch latch = new CountDownLatch(1);
				final AtomicBoolean result = new AtomicBoolean(false);
				mGLManager.getGLHandler().post(() -> {
					final RendererTarget target = mTargets.get(id);
					result.set((target != null) && target.isEnabled());
					latch.countDown();
				});
				try {
					if (latch.await(300L, TimeUnit.MILLISECONDS)) {
						return result.get();
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return false;
	}

	/**
	 * 指定したidに対応するSurfaceへの描画を一時的に有効/無効設定する
	 * @param id id=0なら全てのSurfaceへの描画を一時的に有効/無効設定する
	 * @param enable
	 */
	@AnyThread
	public void setEnabled(final int id, final boolean enable) {
		if (DEBUG) Log.v(TAG, "setEnabled:" + id + ",enable=" + enable);
		if (isValid()) {
			mGLManager.runOnGLThread(() -> {
				if (id == 0) {
					final int n = mTargets.size();
					for (int i = 0; i < n; i++) {
						final RendererTarget target = mTargets.valueAt(i);
						target.setEnabled(enable);
					}
				} else {
					final RendererTarget target = mTargets.get(id);
					if (target != null) {
						target.setEnabled(enable);
					}
				}
			});
		}
	}

	/**
	 * 分配描画用のSurfaceの数を取得
	 * @return
	 */
	@AnyThread
	public int getCount() {
		return mTargets.size();
	}

//--------------------------------------------------------------------------------
	/**
	 * 内部リソースを破棄する
	 * GLコンテキスト上で実行される
	 */
	@WorkerThread
	protected void releaseOnGL() {
		if (DEBUG) Log.v(TAG, "releaseOnGL:");
		// 描画先Surfaceを削除
		final int n = mTargets.size();
		for (int i = 0; i < n; i++) {
			final RendererTarget target = mTargets.valueAt(i);
			target.release();
		}
		mTargets.clear();
		// GLDrawer2Dがあれば削除
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
	}

	/**
	 * リサイズ処理
	 * GLコンテキスト上で実行される
	 * @param width
	 * @param height
	 */
	@WorkerThread
	protected void resizeOnGL(final int width, final int height) {
		if ((mWidth != width) || (mHeight != height)) {
			mWidth = width;
			mHeight = height;
			if (DEBUG) Log.v(TAG, "resizeOnGL:");
			// FIXME リサイズ処理…多分何もしない
		}
	}

	/**
	 * 描画先RendererTargetへミラーモードを適用
	 * @param id
	 * @param mirror
	 */
	@WorkerThread
	protected void setMirrorOnGL(final int id, @MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirrorOnGL:" + mirror);
		if (id == 0) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				target.setMirror(mirror);
			}
		} else {
			final RendererTarget target = mTargets.get(id);
			if (target != null) {
				target.setMirror(mirror);
			}
		}
	}

	private int cnt;

	/**
	 * 描画実行
	 * GLコンテキスト上で実行される
	 * @param drawer
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	protected void drawOnGL(
		@NonNull final GLDrawer2D drawer,
		final int texId, @NonNull final float[] texMatrix) {

//		if (DEBUG) Log.v(TAG, "drawOnGL:" + cnt);
		// RendererTargetへ描画実行
		final int n = mTargets.size();
		for (int i = n - 1; i >= 0; i--) {	// 列挙中に#removeAtを呼べるように後ろからアクセスする
			@NonNull
			final RendererTarget target = mTargets.valueAt(i);
			if (target.canDraw()) {
				if (DEBUG) Log.v(TAG, "drawOnGL:" + cnt);
				try {
					target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				} catch (final Exception e) {
					// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
					mTargets.removeAt(i);
					target.release();
				}
			} else if (DEBUG) {
				Log.v(TAG, "drawOnGL:id=" + target.getId() + " can not draw");
			}
		}
		if (DEBUG && (++cnt % 100) == 0) {
			Log.v(TAG, "drawOnGL:" + cnt);
		}
	}

	/**
	 * このクラスオブジェクトが有効かどうかを確認して無効ならIllegalStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void checkValid() throws IllegalStateException {
		if (!isValid()) {
			throw new IllegalStateException("already released?");
		}
	}

}
