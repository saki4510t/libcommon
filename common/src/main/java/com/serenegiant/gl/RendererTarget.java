package com.serenegiant.gl;
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

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.system.Time;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * Surface等の描画先のオブジェクトと関係する設定を保持するためのホルダークラス
 * 同じ内容のクラスだったからEffectRendererHolder/RendererHolderのインナークラスを外に出した
 */
public class RendererTarget implements IMirror {
	private static final boolean DEBUG = false;
	private static final String TAG = RendererTarget.class.getSimpleName();

	/**
	 * 対応しているSurfaceかどうかを確認
	 * Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラスならtrue
	 * GLUtils#isSupportedSurfaceがtrueを返すクラスに加えてISurfaceとTextureWrapperを含む
	 * @param surface
	 * @return
	 */
	public static boolean isSupportedSurface(@Nullable final Object surface) {
		return ((surface instanceof Surface)
			|| (surface instanceof SurfaceHolder)
			|| (surface instanceof SurfaceTexture)
			|| (surface instanceof SurfaceView))
			|| (surface instanceof ISurface)
			|| (surface instanceof TextureWrapper);
	}

	/**
	 * ファクトリーメソッド
	 * @param egl
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps 0以下なら最大描画フレームレート制限なし, あまり正確じゃない
	 * @return
	 */
	public static RendererTarget newInstance(
		@NonNull final EGLBase egl,
		@NonNull final Object surface, final float maxFps) {

		if (DEBUG) Log.v(TAG, "newInstance:maxFps=" + maxFps);
		return (maxFps > 0)
			? new RendererTargetHasWait(egl, surface, maxFps)
			: new RendererTarget(egl, surface);	// no limitation of maxFps
	}

	/** 元々の分配描画用Surface */
	private Object mSurface;
	private final boolean mOwnSurface;
	/** 分配描画用Surfaceを元に生成したOpenGL|ESで描画する為のEglSurface */
	private ISurface mTargetSurface;
	@Size(value=16)
	@NonNull
	private final float[] mMvpMatrix = new float[16];
	@Size(value=16)
	@NonNull
	private final float[] mWorkMatrix = new float[16];
	private volatile boolean mEnable = true;
	@MirrorMode
	private int mMirror = MIRROR_NORMAL;

	/**
	 * コンストラクタ, ファクトリーメソッドの使用を強制するためprotected
	 * @param egl
	 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 */
	protected RendererTarget(@NonNull final EGLBase egl, @NonNull final Object surface) {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mSurface = surface;
		if (surface instanceof ISurface) {	// IGLSurfaceはISurfaceの子なのでここに含む
			mTargetSurface = (ISurface)surface;
			mOwnSurface = false;
		} else if (surface instanceof final TextureWrapper wrapper) {
			mTargetSurface = GLSurface.wrap(egl.isGLES3(),
				wrapper.texTarget, wrapper.texUnit, wrapper.texId,
				wrapper.width, wrapper.height, false);
			mOwnSurface = true;
		} else if (GLUtils.isSupportedSurface(surface)) {
			mTargetSurface = egl.createFromSurface(surface);
			mOwnSurface = true;
		} else {
			throw new IllegalArgumentException("Unsupported surface," + surface);
		}
		Matrix.setIdentityM(mMvpMatrix, 0);
	}

	/**
	 * 生成したEglSurface等を破棄する
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (mOwnSurface && (mTargetSurface != null)) {
			mTargetSurface.release();
		}
		mTargetSurface = null;
		mSurface = null;
	}

	@Nullable
	public Object getSurface() {
		return mSurface;
	}

	/**
	 * Surface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	public int getId() {
		return mSurface != null ? mSurface.hashCode() : 0;
	}

	/**
	 * Surfaceが有効かどうかを取得する
	 * @return
	 */
	public boolean isValid() {
		return (mTargetSurface != null) && mTargetSurface.isValid();
	}

	/**
	 * Surfaceへの描画が有効かどうかを取得する
	 * @return
	 */
	public boolean isEnabled() {
		return mEnable;
	}
	
	/**
	 * Surfaceへの描画を一時的に有効/無効にする
	 * @param enable
	 */
	public void setEnabled(final boolean enable) {
		mEnable = enable;
	}

	/**
	 * 描画可能かどうかを取得
	 * @return
	 */
	public boolean canDraw() {
		return mEnable && (mTargetSurface != null) && mTargetSurface.isValid();
	}

	/**
	 * 内部で使うモデルビュー変換行列を取得
	 * コピーではないので変更時は注意
	 * @return
	 */
	@Size(value=16)
	@NonNull
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * モデルビュー変換行列に左右・上下反転をセット
	 * @param mirror
	 */
	@Override
	public void setMirror(@IMirror.MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirror:" + mirror);
		@MirrorMode
		final int _mirror = mirror % IMirror.MIRROR_NUM;
		if (_mirror != mMirror) {
			mMirror = _mirror;
			MatrixUtils.setMirror(mMvpMatrix, _mirror);
		}
	}

	@MirrorMode
	@Override
	public int getMirror() {
		return mMirror;
	}

	public int width() {
		return mTargetSurface != null ? mTargetSurface.getWidth() : 0;
	}

	public int height() {
		return mTargetSurface != null ? mTargetSurface.getHeight() : 0;
	}

	/**
	 * このRendererTargetが保持する描画先(Surface等)へIDrawer2Dを使って指定したテクスチャを描画する
	 * @param drawer
	 * @param textId
	 * @param texMatrix
	 */
	public void draw(@NonNull final GLDrawer2D drawer,
		@GLConst.TexUnit final int texUnit, final int textId,
		@Nullable @Size(min=16) final float[] texMatrix) {

		if (mTargetSurface != null) {
			mTargetSurface.makeCurrent();
			mTargetSurface.setViewPort(0, 0, mTargetSurface.getWidth(), mTargetSurface.getHeight());
			// 本来は映像が全面に描画されるので#glClearでクリアする必要はないけど
			// ハングアップする機種があるのでクリアしとく
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			// GLDrawer2Dのモデルビュー変換行列と引数のモデルビュー変換行列(=RendererTargetへ適用されたモデルビュー変換行列)を乗算
			// 乗算の順がこれでいいかは要検討
			Matrix.multiplyMM(mWorkMatrix, 0, drawer.getMvpMatrix(), 0, mMvpMatrix, 0);
			// 乗算したモデルビュー変換行列で描画
			drawer.draw(texUnit, textId, texMatrix, 0, mWorkMatrix, 0);
			mTargetSurface.swap();
		}
	}

	/**
	 * 指定した色で全面を塗りつぶす
	 * @param color
	 */
	public void clear(final int color) {
		if (mTargetSurface != null) {
			mTargetSurface.makeCurrent();
			mTargetSurface.setViewPort(0, 0, mTargetSurface.getWidth(), mTargetSurface.getHeight());
			GLES20.glClearColor(
				((color & 0x00ff0000) >>> 16) / 255.0f,	// R
				((color & 0x0000ff00) >>>  8) / 255.0f,	// G
				((color & 0x000000ff)) / 255.0f,		// B
				((color & 0xff000000) >>> 24) / 255.0f	// A
			);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			mTargetSurface.swap();
		}
	}
	
	/**
	 * #drawの代わりにOpenGL|ES2を使って自前で描画する場合は
	 * #makeCurrentでレンダリングコンテキストを切り替えてから
	 * 描画後#swapを呼ぶ
	 */
	public void makeCurrent() throws IllegalStateException {
		check();
		mTargetSurface.makeCurrent();
		mTargetSurface.setViewPort(0, 0, mTargetSurface.getWidth(), mTargetSurface.getHeight());
	}

	/**
	 * #drawの代わりにOpenGL|ES2を使って自前で描画する場合は
	 * #makeCurrentでレンダリングコンテキストを切り替えてから
	 * 描画後#swapを呼ぶ
	 */
	public void swap() throws IllegalStateException {
		check();
		mTargetSurface.swap();

	}

	/**
	 * mTargetSurfaceの有効無効を確認して無効ならIllegalStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void check() throws IllegalStateException {
		if ((mTargetSurface == null) || !mTargetSurface.isValid()) {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * フレームレート制限のための時間チェックを追加したRendererTargetクラス
	 */
	static class RendererTargetHasWait extends RendererTarget {
		private long mNextDraw;
		private final long mIntervalsNs;
		private final long mIntervalsDeltaNs;

		/**
		 * コンストラクタ, ファクトリーメソッドの使用を強制するためprivate
		 * @param egl
		 * @param surface Surface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapperのいずれか
		 * @param maxFps 正数
		 */
		protected RendererTargetHasWait(
			@NonNull final EGLBase egl,
			@NonNull final Object surface, final float maxFps) {

			super(egl, surface);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
			mIntervalsNs = Math.round(1000000000.0 / maxFps);
			mIntervalsDeltaNs = -Math.round(mIntervalsNs * 0.03);	// 3%ならショートしても良いことにする
			mNextDraw = Time.nanoTime() + mIntervalsNs;
		}

		/**
		 * 描画可能かどうかを取得
		 * フレームレートを制限するため前回の描画から一定時間経過したときのみtrue
		 * @return
		 */
		@Override
		public boolean canDraw() {
			return super.canDraw() && (Time.nanoTime() - mNextDraw > mIntervalsDeltaNs);
		}

		/**
		 * このRendererTargetが保持する描画先(Surface等)へIDrawer2Dを使って指定したテクスチャを描画する
		 * @param drawer
		 * @param textId
		 * @param texMatrix
		 */
		@Override
		public void draw(@NonNull final GLDrawer2D drawer,
			@GLConst.TexUnit final int texUnit, final int textId,
			@Nullable @Size(min=16) final float[] texMatrix) {

			mNextDraw = Time.nanoTime() + mIntervalsNs;
			super.draw(drawer, texUnit, textId, texMatrix);
		}
	}

}
