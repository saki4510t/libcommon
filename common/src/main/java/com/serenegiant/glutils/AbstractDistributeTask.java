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
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.gl.GLContext;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLEffectDrawer2D;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.ThreadUtils;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * 分配描画処理の共通部分
 */
public abstract class AbstractDistributeTask implements IMirror {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractDistributeTask.class.getSimpleName();

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_ADD_SURFACE = 3;
	private static final int REQUEST_REMOVE_SURFACE = 4;
	private static final int REQUEST_REMOVE_SURFACE_ALL = 12;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
	private static final int REQUEST_MIRROR = 6;
	private static final int REQUEST_ROTATE = 7;
	private static final int REQUEST_CLEAR = 8;
	private static final int REQUEST_CLEAR_ALL = 9;
	private static final int REQUEST_SET_MVP = 10;

	@NonNull
	private final Object mSync = new Object();
	/**
	 * 描画先のRendererTargetを保持するSparseArrayインスタンス
	 * add/removeを除いて描画スレッド上からしか読み書きしないので
	 * 基本的には排他制御は不要(add/remove処理時のみ排他制御する)
	 */
	@NonNull
	private final SparseArray<RendererTarget>
		mTargets = new SparseArray<>();
	@NonNull
	private final GLDrawer2D.DrawerFactory mDrawerFactory;
	private int mVideoWidth, mVideoHeight;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;
	private int mRotation = 0;
	private volatile boolean mIsFirstFrameRendered;
	private volatile boolean mHasNewFrame;
	private volatile boolean mReleased;
	private GLDrawer2D mDrawer;

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 */
	protected AbstractDistributeTask(
		final int width, final int height,
		@Nullable GLDrawer2D.DrawerFactory factory) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mVideoWidth = width > 0 ? width : 640;
		mVideoHeight = height > 0 ? height : 480;
		mDrawerFactory = factory != null ? factory : GLDrawer2D.DEFAULT_FACTORY;
		mReleased = false;
	}

	/**
	 * 関連するリソースを破棄する
	 */
	public synchronized void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (!mReleased) {
			mReleased = true;
		}
	}

	public boolean isFirstFrameRendered() {
		return mIsFirstFrameRendered;
	}

	/**
	 * 描画要求する
	 */
	@AnyThread
	public void requestFrame(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		mHasNewFrame = mIsFirstFrameRendered = true;
		offer(REQUEST_DRAW, texId, isOES ? 1 : 0, texMatrix);
	}

	/**
	 * 映像受け取り用のマスターサーフェースの再生成要求する
	 */
	@AnyThread
	public void requestRecreateMasterSurface() {
		offer(REQUEST_RECREATE_MASTER_SURFACE);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 */
	@AnyThread
	public void addSurface(final int id, final Object surface)
		throws IllegalStateException, IllegalArgumentException {

		addSurface(id, surface, null);
	}

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
	 * @param maxFps nullまたはasFloatが0以下なら未指定, それ以外なら最大フレームレート
	 */
	@AnyThread
	public void addSurface(final int id,
		final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "addSurface:" + id);
		checkFinished();
		if (!RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException(
				"Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
		}
		RendererTarget target;
		synchronized (mTargets) {
			target = mTargets.get(id);
		}
		if (target == null) {
			final TargetSurface _surface = new TargetSurface(id, surface, maxFps);
			while (isRunning() && !isFinished()) {
				if (offer(REQUEST_ADD_SURFACE, _surface)) {
					// 追加時は待機しなくて良さそう
					break;
				} else {
					// タスク実行中ならofferに失敗しないのでここにはこないはず
					synchronized (mSync) {
						try {
							mSync.wait(5);
						} catch (final InterruptedException e) {
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 分配描画用のSurfaceを削除
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	@AnyThread
	public void removeSurface(final int id) {
		if (DEBUG) Log.v(TAG, "removeSurface:" + id);
		while (isRunning()) {
			synchronized (mSync) {
				if (offer(REQUEST_REMOVE_SURFACE, id)) {
					try {
						mSync.wait();
					} catch (final InterruptedException e) {
						// ignore
					}
					break;
				} else {
					// タスク実行中ならofferに失敗しないのでここにはこないはず
					try {
						mSync.wait(5);
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
		}
	}

	/**
	 * 分配描画用のSurfaceを全て削除する
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	@AnyThread
	public void removeSurfaceAll() {
		if (DEBUG) Log.v(TAG, "removeSurfaceAll:");
		while (isRunning() && !isFinished()) {
			synchronized (mSync) {
				if (offer(REQUEST_REMOVE_SURFACE_ALL)) {
					try {
						mSync.wait();
					} catch (final InterruptedException e) {
						// ignore
					}
					break;
				} else {
					// タスク実行中ならofferに失敗しないのでここにはこないはず
					try {
						mSync.wait(5);
					} catch (final InterruptedException e) {
						break;
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
	@AnyThread
	public void clearSurface(final int id, final int color)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "clearSurface:" + id + ",cl=" + color);
		checkFinished();
		offer(REQUEST_CLEAR, id, color);
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
		checkFinished();
		offer(REQUEST_CLEAR_ALL, color);
	}

	@AnyThread
	public void setMvpMatrix(final int id,
		final int offset, @NonNull @Size(min=16) final float[] matrix)
			throws IllegalStateException, IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setMvpMatrix:" + id);
		checkFinished();
		if (matrix.length >= offset + 16) {
			offer(REQUEST_SET_MVP, id, offset, matrix);
		} else {
			throw new IllegalArgumentException("matrix is too small, should be longer than offset + 16");
		}
	}

	@AnyThread
	public boolean isEnabled(final int id) {
		if (DEBUG) Log.v(TAG, "isEnabled:" + id);
		synchronized (mTargets) {
			final RendererTarget target = mTargets.get(id);
			return target != null && target.isEnabled();
		}
	}

	@AnyThread
	public void setEnabled(final int id, final boolean enable) {
		if (DEBUG) Log.v(TAG, "setEnabled:" + id + ",enable=" + enable);
		synchronized (mTargets) {
			final RendererTarget target = mTargets.get(id);
			if (target != null) {
				target.setEnabled(enable);
			}
		}
	}

	/**
	 * 分配描画用のSurfaceの数を取得
	 * @return
	 */
	@AnyThread
	public int getCount() {
		synchronized (mTargets) {
			return mTargets.size();
		}
	}

	/**
	 * リサイズ
	 * @param width
	 * @param height
	 */
	@AnyThread
	public void resize(final int width, final int height)
		throws IllegalStateException {

		checkFinished();
		if ( ((width > 0) && (height > 0))
			&& ((mVideoWidth != width) || (mVideoHeight != height)) ) {

			offer(REQUEST_UPDATE_SIZE, width, height);
		}
	}

	@AnyThread
	@Override
	public void setMirror(@MirrorMode final int mirror) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "mirror:" + mirror);
		checkFinished();
		if (mMirror != mirror) {
			offer(REQUEST_MIRROR, mirror);
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		return mMirror;
	}

	@AnyThread
	public int width() {
		return mVideoWidth;
	}

	@AnyThread
	public int height() {
		return mVideoHeight;
	}

	@Deprecated
	public void rotation(final int degree) {
		if (DEBUG) Log.v(TAG, "mirror:" + degree);
		checkFinished();
		if (mRotation != degree) {
			offer(REQUEST_ROTATE, degree);
		}
	}

	@Deprecated
	@AnyThread
	public int rotation() {
		return mRotation;
	}

	public GLDrawer2D getDrawer() {
		synchronized (mSync) {
			return mDrawer;
		}
	}

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
//--------------------------------------------------------------------------------
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
		internalOnStart();
		notifyParent(true);
//		if (DEBUG) Log.v(TAG, "handleOnStart:finished");
	}

	@WorkerThread
	protected void internalOnStart() {
		if (DEBUG) Log.v(TAG, "internalOnStart:");
		synchronized (mSync) {
			mDrawer = mDrawerFactory.create(isGLES3(), true);
			// XXX GLSurfaceRendererのテスト結果からするとGL_TEXTURE_EXTERNAL_OESの時に上下反転させないとだめみたい
			//     ただしSurfaceTexture経由で受け取ったテクスチャなのでここに来るのは常にGL_TEXTURE_EXTERNAL_OESなので
			//     常に上下反転させる
			mDrawer.setMirror(MIRROR_VERTICAL);
			if (mDrawer instanceof GLEffectDrawer2D) {
				((GLEffectDrawer2D) mDrawer).setTexSize(width(), height());
			}
		}
		handleReCreateInputSurface();
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	protected final void handleOnStop() {
		if (DEBUG) Log.v(TAG, "onStop");
		notifyParent(false);
		makeCurrent();
		internalOnStop();
		handleRemoveAll();
//		if (DEBUG) Log.v(TAG, "onStop:finished");
	}

	@WorkerThread
	protected void internalOnStop() {
		if (DEBUG) Log.v(TAG, "internalOnStop:");
		handleReleaseInputSurface();
		handleRemoveAll();
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
	}

	@WorkerThread
	protected boolean handleOnError(final Exception e) {
		if (DEBUG) Log.w(TAG, e);
		return false;
	}

	@WorkerThread
	protected boolean handleRequest(@NonNull final Message msg) {
//		if (DEBUG) Log.v(TAG, "handleRequest:" + request);
		switch (msg.what) {
		case REQUEST_DRAW -> handleDraw(msg.arg2 != 0, msg.arg1, (float[]) msg.obj);
		case REQUEST_UPDATE_SIZE -> handleResize(msg.arg1, msg.arg2);
		case REQUEST_ADD_SURFACE -> {
			if (msg.obj instanceof TargetSurface) {
				handleAddSurface((TargetSurface) msg.obj);
			}
		}
		case REQUEST_REMOVE_SURFACE -> handleRemoveSurface(msg.arg1);
		case REQUEST_REMOVE_SURFACE_ALL -> handleRemoveAll();
		case REQUEST_RECREATE_MASTER_SURFACE -> handleReCreateInputSurface();
		case REQUEST_MIRROR -> handleMirror(0, msg.arg1);
		case REQUEST_ROTATE -> handleRotate(msg.arg1, msg.arg2);
		case REQUEST_CLEAR -> handleClear(msg.arg1, msg.arg2);
		case REQUEST_CLEAR_ALL -> handleClear(0, msg.arg1);
		case REQUEST_SET_MVP -> handleSetMvp(msg.arg1, msg.arg2, (float[]) msg.obj);
		}

		return false;
	}

	/**
	 * 実際の描画処理
	 */
	@WorkerThread
	private void handleDraw(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

//		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		removeRequest(REQUEST_DRAW);
		if (!isMasterSurfaceValid()) {
			Log.e(TAG, "handleDraw:invalid master surface");
			offer(REQUEST_RECREATE_MASTER_SURFACE);
			return;
		}

		if (mIsFirstFrameRendered) {
			try {
				makeCurrent();
				if (mHasNewFrame) {
					mHasNewFrame = false;
					handleUpdateTexture();
					GLES20.glFlush();
					ThreadUtils.NoThrowSleep(0, 0);
				}
			} catch (final Exception e) {
				Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
				offer(REQUEST_RECREATE_MASTER_SURFACE);
				return;
			}
			handleDrawTargets(isOES, texId, texMatrix);
		}

		// Egl保持用のSurfaceへ描画しないとデッドロックする端末対策
		makeCurrent();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glFlush();	// これなくても良さそう?
		if (mIsFirstFrameRendered) {
			callOnFrameAvailable();
		}
	}

	/**
	 * 各Surfaceへ描画する
	 */
	@WorkerThread
	protected void handleDrawTargets(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

//		if (DEBUG) Log.v(TAG, "handleDrawTargets:");
		final int n = mTargets.size();
		final GLDrawer2D drawer;
		synchronized (mSync) {
			if ((mDrawer == null) || (mDrawer.isOES() != isOES)) {
				if (mDrawer != null) {
					mDrawer.release();
				}
				mDrawer = mDrawerFactory.create(isGLES3(), isOES);
				if (mDrawer instanceof GLEffectDrawer2D) {
					((GLEffectDrawer2D) mDrawer).setTexSize(width(), height());
				}
			}
			drawer = mDrawer;
		}
		for (int i = n - 1; i >= 0; i--) {
			final RendererTarget target = mTargets.valueAt(i);
			if ((target != null) && target.canDraw()) {
				try {
					target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
					// removeSurfaceが呼ばれなかったかremoveSurfaceを呼ぶ前に破棄されてしまった
					synchronized (mTargets) {
						// removeSurface/removeSurfaceAllを別スレッドが参照する可能性があるので排他制御する
						mTargets.removeAt(i);
					}
					target.release();
				}
			}
		}
	}

	/**
	 * 映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		mVideoWidth = width;
		mVideoHeight = height;
		if (mDrawer instanceof GLEffectDrawer2D) {
			((GLEffectDrawer2D) mDrawer).setTexSize(width, height);
		}
	}

	/**
	 * 指定したIDの分配描画先Surfaceを追加する
	 * @param ts
	 */
	@WorkerThread
	private void handleAddSurface(@NonNull final TargetSurface ts) {

		if (DEBUG) Log.v(TAG, "handleAddSurface:" + ts.id);
		checkTarget();
		RendererTarget target = mTargets.get(ts.id);
		if (target == null) {
			try {
				target = createRendererTarget(getEgl(), ts.id, ts.surface, ts.maxFps);
				target.setMirror(mMirror);
				synchronized (mTargets) {
					mTargets.append(ts.id, target);
				}
			} catch (final Exception e) {
				Log.w(TAG, "invalid surface: surface=" + ts, e);
			}
		} else {
			Log.w(TAG, "surface is already added: id=" + ts.id);
		}
		synchronized (mSync) {
			mSync.notify();
		}
	}

	/**
	 * IRendererTargetインスタンスを生成する
	 * このクラスではRendererTarget.newInstanceを呼ぶだけ
	 * @param egl
	 * @param id
	 * @param surface
	 * @param maxFps 最大フレームレート, nullまたは0以下なら未指定
	 * @return
	 */
	@WorkerThread
	@NonNull
	private RendererTarget createRendererTarget(
		@NonNull final EGLBase egl,
		final int id,
		@NonNull final Object surface, @Nullable final Fraction maxFps) {

		if (DEBUG) Log.v(TAG, "createRendererTarget:" + id);
		return RendererTarget.newInstance(egl, surface, maxFps != null ? maxFps.asFloat() : -1.0f);
	}

	/**
	 * 指定したIDの分配描画先Surfaceを破棄する
	 * @param id
	 */
	@WorkerThread
	private void handleRemoveSurface(final int id) {
		if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
		final RendererTarget target = mTargets.get(id);
		if (target != null) {
			mTargets.remove(id);
			if (target.isValid()) {
				target.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
			}
			target.release();
		}
		checkTarget();
		synchronized (mSync) {
			mSync.notify();
		}
	}

	/**
	 * 念の為に分配描画先のSurfaceを全て破棄する
	 */
	@WorkerThread
	private void handleRemoveAll() {
		if (DEBUG) Log.v(TAG, "handleRemoveAll:");
		synchronized (mTargets) {
			final int n = mTargets.size();
			if (DEBUG) Log.i(TAG, "handleRemoveAll:n=" + n);
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if (target != null) {
					if (target.isValid()) {
						target.clear(0);	// XXX 黒で塗りつぶし, 色指定できるようにする?
					}
					target.release();
				}
			}
			mTargets.clear();
		}
		synchronized (mSync) {
			mSync.notify();
		}
		if (DEBUG) Log.v(TAG, "handleRemoveAll:finished");
	}

	/**
	 * 分配描画先のSurfaceが有効かどうかをチェックして無効なものは削除する
	 */
	@WorkerThread
	private void checkTarget() {
		if (DEBUG) Log.v(TAG, "checkTarget:");
		final int n = mTargets.size();
		for (int i = 0; i < n; i++) {
			final RendererTarget target = mTargets.valueAt(i);
			if ((target != null) && !target.isValid()) {
				final int id = mTargets.keyAt(i);
				if (DEBUG) Log.i(TAG, "checkTarget:found invalid surface:id=" + id);
				mTargets.remove(id);
				target.release();
			}
		}
		if (DEBUG) Log.v(TAG, "checkTarget:finished");
	}

	/**
	 * 指定したIDの分配描画用Surfaceを指定した色で塗りつぶす
	 * @param id 0なら全てのSurfaceを対象にする
	 * @param color
	 */
	@WorkerThread
	private void handleClear(final int id, final int color) {
		if (DEBUG) Log.v(TAG, "handleClear:" + id);
		if (id == 0) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.isValid()) {
					target.clear(color);
				}
			}
		} else {
			final RendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				target.clear(color);
			}
		}
	}

	/**
	 * モデルビュー変換行列を適用する
	 * @param id 0なら全てのSurfaceを対象にする
	 * @param offset
	 * @param mvp
	 */
	@WorkerThread
	private void handleSetMvp(final int id,
		final int offset, @NonNull @Size(min=16) final float[] mvp) {

		if (DEBUG) Log.v(TAG, "handleSetMvp:" + id);
		if (id == 0) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.isValid()) {
					System.arraycopy(mvp, offset, target.getMvpMatrix(), 0, 16);
				}
			}
		} else {
			final RendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				System.arraycopy(mvp, offset, target.getMvpMatrix(), 0, 16);
			}
		}
	}

	/**
	 * ミラーモードをセット
	 * @param id 0なら全てのSurfaceを対象にする
	 * @param mirror
	 */
	@WorkerThread
	private void handleMirror(final int id, @MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "handleMirror:" + mirror);
		mMirror = mirror;
		if (id == 0) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.isValid()) {
					target.setMirror(mirror);
				}
			}
		} else {
			final RendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				target.setMirror(mirror);
			}
		}
	}

	/**
	 * 描画する映像の回転を設定
	 * @param id 0なら全てのSurfaceを対象にする
	 * @param degree
	 */
	@WorkerThread
	private void handleRotate(final int id, final int degree) {
		if (DEBUG) Log.v(TAG, "handleRotate:" + id);
		mRotation = degree;
		if (id == 0) {
			final int n = mTargets.size();
			for (int i = 0; i < n; i++) {
				final RendererTarget target = mTargets.valueAt(i);
				if ((target != null) && target.isValid()) {
					MatrixUtils.setRotation(target.getMvpMatrix(), degree);
				}
			}
		} else {
			final RendererTarget target = mTargets.get(id);
			if ((target != null) && target.isValid()) {
				MatrixUtils.setRotation(target.getMvpMatrix(), degree);
			}
		}
	}

	protected void checkFinished() throws IllegalStateException {
		if (isFinished()) {
			throw new IllegalStateException("already finished");
		}
	}

	/**
	 * 描画先Surfaceとその設定のホルダークラス
	 */
	private static class TargetSurface {
		private final int id;
		@NonNull
		private final Object surface;
		@Nullable
		private final Fraction maxFps;

		private TargetSurface(final int id, @NonNull final Object surface, final int maxFps) {
			this(id, surface, makeFraction(maxFps));
		}

		private TargetSurface(final int id, @NonNull final Object surface, @Nullable final Fraction maxFps) {
			this.id = id;
			this.surface = surface;
			this.maxFps = maxFps;
		}

		@NonNull
		@Override
		public String toString() {
			return "TargetSurface{" +
				"id=" + id +
				", surface=" + surface +
				", maxFps=" + maxFps +
				'}';
		}

		@Nullable
		private static Fraction makeFraction(final int v) {
			if (v < 0) {
				return null;
			} else if (v > 1000) {
				return new Fraction(v, 1000);
			} else {
				return new Fraction(v, 1);
			}
		}
	}
//--------------------------------------------------------------------------------
	public abstract void start(final String tag);
	public abstract boolean waitReady();
	public abstract boolean isRunning();
	public abstract boolean isFinished();
	public abstract boolean offer(final int request);
	public abstract boolean offer(final int request, final Object obj);
	public abstract boolean offer(final int request, final int arg1);
	public abstract boolean offer(final int request, final int arg1, final int arg2);
	public abstract boolean offer(final int request, final int arg1, final int arg2, final Object obj);
	public abstract void removeRequest(final int request);

	@NonNull
	public abstract EGLBase getEgl();
	@NonNull
	public abstract GLContext getGLContext();
	@Deprecated
	@NonNull
	public abstract EGLBase.IContext<?> getContext();
	public abstract int getGlVersion();
	public abstract void makeCurrent();
	public abstract boolean isGLES3();
	public abstract boolean isOES3Supported();

	public abstract boolean isMasterSurfaceValid();
	/**
	 * 映像入力用Surfaceを再生成する
	 */
	@WorkerThread
	protected abstract void handleReCreateInputSurface();
	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@WorkerThread
	protected abstract void handleReleaseInputSurface();
	/**
	 * テクスチャを更新
	 */
	@WorkerThread
	protected abstract void handleUpdateTexture();

	public abstract void notifyParent(final boolean isRunning);
	public abstract void callOnFrameAvailable();

}
