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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.gl.GLTexture;
import com.serenegiant.math.Fraction;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * OpenGL|ESのシェーダーを使って映像にマスク処理するGLPipeline実装
 * 描画先のsurfaceにnullを指定するとマスク処理したテクスチャを次のGLPipelineへ送る
 *              マスク用静止画
 *                 ↓
 * パイプライン → MaskPipeline (→ パイプライン)
 *                (→ Surface)
 */
public class MaskPipeline extends ProxyPipeline implements GLSurfacePipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = MaskPipeline.class.getSimpleName();

	@NonNull
	private final GLManager mManager;

	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * マスク処理してそのまま次のGLPipelineへ送るかSurfaceへ描画するか
	 * setSurfaceで有効な描画先Surfaceをセットしていればfalse、セットしていなければtrue
	 */
	private volatile boolean mMaskOnly;
	/**
	 * 映像効果付与してそのまま次のGLPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface work;
	@Nullable
	private Bitmap mMaskBitmap;
	@Nullable
	private GLTexture mMaskTexture;
	/**
	 * マスク用GLTexture更新要求
	 */
	private volatile boolean mRequestUpdateMask;
	private int mSurfaceId = 0;
	/**
	 * モデルビュー変換行列
	 */
	@Size(value=16)
	@NonNull
	private final float[] mMvpMatrix = new float[16];

	/**
	 * コンストラクタ
	 * @param manager
	 * @param manager
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public MaskPipeline(@NonNull final GLManager manager)
			throws IllegalStateException, IllegalArgumentException {
		this(manager,  null, null);
	}

	/**
	 * コンストラクタ
	 * @param manager
	 * 対応していないSurface形式の場合はIllegalArgumentExceptionを投げる
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps 最大フレームレート, nullまたはFraction#ZEROなら制限なし
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public MaskPipeline(
		@NonNull final GLManager manager,
		@Nullable final Object surface, @Nullable final Fraction maxFps)
			throws IllegalStateException, IllegalArgumentException {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if ((surface != null) && !RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
		Matrix.setIdentityM(mMvpMatrix, 0);
		manager.runOnGLThread(() -> {
			createTargetOnGL(surface, maxFps);
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
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
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
		if ((surface != null) && !RendererTarget.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager.runOnGLThread(() -> {
			createTargetOnGL(surface, maxFps);
		});
	}

	/**
	 * 描画先のSurfaceをセットしているかどうか
	 * #isEffectOnlyの符号反転したのものと実質的には同じ
	 * @return
	 */
	@Override
	public boolean hasSurface() {
		mLock.lock();
		try {
			return mSurfaceId != 0;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * セットされているSurface識別用のidを取得
	 * @return Surfaceがセットされていればそのid(#hashCode)、セットされていなければ0を返す
	 */
	@Override
	public int getId() {
		mLock.lock();
		try {
			return mSurfaceId;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public boolean isValid() {
		return super.isValid() && mManager.isValid();
	}

	/**
	 * マスク処理をせずに次のGLPipelineへ送るだけかどうか
	 * コンストラクタまたはsetSurfaceで描画先のsurfaceにnullを指定するとtrue
	 * @return
	 */
	public boolean isMaskOnly() {
		return mMaskOnly;
	}

	public void setMvpMatrix(@NonNull @Size(min=16) final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		final GLDrawer2D drawer = mDrawer;
		if (drawer != null) {
			drawer.setMvpMatrix(matrix, offset);
		}
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		if ((mDrawer == null) || (isGLES3 != mDrawer.isGLES3) || (isOES != mDrawer.isOES())) {
			// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
			releaseDrawerOnGL();
			if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
			mDrawer = createDrawerOnGL(isGLES3, isOES);
			mDrawer.setMvpMatrix(mMvpMatrix, 0);
			mDrawer.setMirror(IMirror.MIRROR_VERTICAL);
		}
		@NonNull
		final GLDrawer2D drawer = mDrawer;
		@Nullable
		final RendererTarget target = mRendererTarget;
		@Nullable
		final Bitmap bitmap = mMaskBitmap;
		if (mRequestUpdateMask) {
			mRequestUpdateMask = false;
			if (bitmap != null) {
				createMaskTextureOnGL(bitmap);
			} else {
				releaseMaskOnGL();
			}
		}
		if ((target != null)
			&& target.canDraw()) {
			if (mMaskTexture != null) {
				mMaskTexture.bindTexture();
			}
			target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
		}
		if (mMaskOnly && (work != null)) {
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:effectOnly," + cnt);
			}
			// 映像効果付与したテクスチャを次へ渡す
			super.onFrameAvailable(isGLES3, work.isOES(), width, height, work.getTexId(), work.getTexMatrix());
		} else {
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:" + cnt);
			}
			// こっちはオリジナルのテクスチャを渡す
			super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		if (DEBUG) Log.v(TAG, "refresh:");
		// XXX #removeでパイプラインチェーンのどれかを削除するとなぜか映像が表示されなくなってしまうことへのワークアラウンド
		//     パイプライン中のどれかでシェーダーを再生成すると表示されるようになる
		if (isValid()) {
			mManager.runOnGLThread(() -> {
				if (DEBUG) Log.v(TAG, "refresh#run:release drawer");
				releaseDrawerOnGL();
			});
		}
	}

	@CallSuper
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		super.resize(width, height);
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		mManager.runOnGLThread(() -> {
			if (DEBUG) Log.v(TAG, "resize#run:");
			releaseMaskOnGL();
		});
	}

	/**
	 * 合成時のマスク用Bitmapをセット
	 * このビットマップのアルファ値を映像入力2のマスクとして使う
	 * マスクのアルファ値が0なら完全透過
	 * アルファ場大きくなるにつれて元映像が表示されなくなりマスク映像が濃く表示される
	 * @param bitmap
	 */
	public void setMask(@Nullable final Bitmap bitmap) {
		if (DEBUG) Log.v(TAG, "setMask:");
		mManager.runOnGLThread(() -> {
			mMaskBitmap = bitmap;
			mRequestUpdateMask = true;
		});
	}

//--------------------------------------------------------------------------------
	private void releaseAll() {
		if (DEBUG) Log.v(TAG, "releaseAll:");
		mLock.lock();
		try {
			mSurfaceId = 0;
		} finally {
			mLock.unlock();
		}
		if (mManager.isValid()) {
			try {
				mManager.runOnGLThread(() -> {
					if (DEBUG) Log.v(TAG, "releaseAll#run:");
					mMaskBitmap = null;
					if (mRendererTarget != null) {
						if (DEBUG) Log.v(TAG, "releaseAll:release target");
						mRendererTarget.release();
						mRendererTarget = null;
					}
					if (work != null) {
						if (DEBUG) Log.v(TAG, "releaseAll:release work");
						work.release();
						work = null;
					}
					releaseDrawerOnGL();
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
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView/TextureWrapper/IGLSurface/ISurfaceのいずれかまたはその子クラス
	 * @param maxFps
	 */
	@WorkerThread
	private void createTargetOnGL(@Nullable final Object surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTargetOnGL:" + surface);
		if ((mRendererTarget == null) || (mRendererTarget.getSurface() != surface)) {
			mSurfaceId = 0;
			if (mRendererTarget != null) {
				mRendererTarget.release();
				mRendererTarget = null;
			}
			if (work != null) {
				work.release();
				work = null;
			}
			if (RendererTarget.isSupportedSurface(surface)) {
				mRendererTarget = RendererTarget.newInstance(
					mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
				mMaskOnly = false;
			} else if (isValid()) {
				if (DEBUG) Log.v(TAG, "createTargetOnGL:create GLSurface as work texture");
				work = GLSurface.newInstance(
					mManager.isGLES3(), GLES20.GL_TEXTURE0,
					getWidth(), getHeight());
				mRendererTarget = RendererTarget.newInstance(
					mManager.getEgl(), work, maxFps != null ? maxFps.asFloat() : 0);
				mMaskOnly = true;
			}
			if (mRendererTarget != null) {
				mLock.lock();
				try {
					mSurfaceId = mRendererTarget.getId();
				} finally {
					mLock.unlock();
				}
			}
		}
	}

	@WorkerThread
	private static GLDrawer2D createDrawerOnGL(final boolean isGLES3, final boolean isOES) {
		if (DEBUG) Log.v(TAG, "createDrawerOnGL:isGLES3=" + isGLES3 + ",isOES=" + isOES);
		return GLDrawer2D.create(isGLES3, isOES,
			isOES ? (isGLES3 ? MY_FRAGMENT_SHADER_EXT_ES3 : MY_FRAGMENT_SHADER_EXT_ES2)
			: (isGLES3 ? MY_FRAGMENT_SHADER_ES3 : MY_FRAGMENT_SHADER_ES2));
	}

	@WorkerThread
	private void releaseDrawerOnGL() {
		if (DEBUG) Log.v(TAG, "releaseDrawerOnGL:");
		if (mDrawer != null) {
			mDrawer.release();
			mDrawer = null;
		}
		releaseMaskOnGL();
	}

	@WorkerThread
	private void createMaskTextureOnGL(@NonNull final Bitmap bitmap) {
		if (DEBUG) Log.v(TAG, "createMaskTextureOnGL:");
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if ((mMaskTexture == null)
			|| (mMaskTexture.getWidth() != width)
			|| (mMaskTexture.getHeight() != height)) {
			// 最初またはマスクのサイズが変更されたときはGLTextureを生成する
			releaseMaskOnGL();
			mMaskTexture = GLTexture.newInstance(GLES20.GL_TEXTURE1, width, height, GLES20.GL_LINEAR);
			mMaskTexture.bindTexture();
			final int uTex2 = mDrawer.glGetUniformLocation("sTexture2");
			GLES20.glUniform1i(uTex2, GLUtils.gLTextureUnit2Index(GLES20.GL_TEXTURE1));
		}
		if (DEBUG) Log.v(TAG, "createMaskTextureOnGL:loadBitmap");
		mMaskTexture.loadBitmap(bitmap);
	}

	@WorkerThread
	private void releaseMaskOnGL() {
		if (DEBUG) Log.v(TAG, "releaseMaskOnGL:");
		if (mMaskTexture != null) {
			mMaskTexture.release();
			mMaskTexture = null;
		}
	}

	/*
	 * XXX パイプラインの前から来る映像がOESの場合でもマスク映像は常にGL_TEXTURE_2Dなので
	 *     sTexture2のサンプラーは常にSAMPLER_2D(=sampler2D)にしないといけないので注意
	 */

	private static final String FRAGMENT_SHADER_BASE_ES2 =
		"""
		%s
		%s
		precision highp float;
		varying vec2 vTextureCoord;
		uniform %s sTexture;
		uniform %s sTexture2;
		void main() {
			highp vec4 tex1 = texture2D(sTexture, vTextureCoord);
			highp vec4 tex2 = texture2D(sTexture2, vTextureCoord);
			gl_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);
		}
		""";
//		"%s" +
//		"precision highp float;\n" +
//		"varying vec2 vTextureCoord;\n" +
//		"uniform %s sTexture;\n" +	// 入力テクスチャA
//		"uniform %s sTexture2;\n" +	// 入力テクスチャB
//		"void main() {\n" +
//		"    highp vec4 tex1 = texture2D(sTexture, vTextureCoord);\n" +
//		"    highp vec4 tex2 = texture2D(sTexture2, vTextureCoord);\n" +
//		"    gl_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);\n" +
//		"}\n";
	private static final String MY_FRAGMENT_SHADER_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_2D_ES2, SAMPLER_2D_ES2, SAMPLER_2D_ES2);
	private static final String MY_FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, SAMPLER_2D_ES2);

	private static final String FRAGMENT_SHADER_BASE_ES3 =
		"""
		%s
		%s
		precision highp float;
		in vec2 vTextureCoord;
		uniform %s sTexture;
		uniform %s sTexture2;
		layout(location = 0) out vec4 o_FragColor;
		void main() {
			highp vec4 tex1 = texture(sTexture, vTextureCoord);
			highp vec4 tex2 = texture(sTexture2, vTextureCoord);
			o_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);
		}
		""";
//		"%s\n" +
//		"precision highp float;\n" +
//		"in vec2 vTextureCoord;\n" +
//		"uniform %s sTexture;\n" +	// 入力テクスチャA
//		"uniform %s sTexture2;\n" +	// 入力テクスチャB
//		"layout(location = 0) out vec4 o_FragColor;\n" +
//		"void main() {\n" +
//		"    highp vec4 tex1 = texture(sTexture, vTextureCoord);\n" +
//		"    highp vec4 tex2 = texture(sTexture2, vTextureCoord);\n" +
//		"    o_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);\n" +
//		"}\n";

	private static final String MY_FRAGMENT_SHADER_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_2D_ES3, SAMPLER_2D_ES3, SAMPLER_2D_ES3);
	private static final String MY_FRAGMENT_SHADER_EXT_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, SAMPLER_2D_ES3);

}
