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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.GLSurface;
import com.serenegiant.glutils.GLUtils;
import com.serenegiant.glutils.RendererTarget;
import com.serenegiant.glutils.es2.GLHelper;
import com.serenegiant.math.Fraction;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * OpenGL|ESのシェーダーを使って映像にマスク処理するIPipeline実装
 * 描画先のsurfaceにnullを指定するとマスク処理したテクスチャを次のIPipelineへ送る
 */
public class MaskPipeline extends ProxyPipeline implements ISurfacePipeline {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = MaskPipeline.class.getSimpleName();

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;

	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;
	/**
	 * マスク処理してそのまま次のIPipelineへ送るかSurfaceへ描画するか
	 * setSurfaceで有効な描画先Surfaceをセットしていればfalse、セットしていなければtrue
	 */
	private volatile boolean mMaskOnly;
	/**
	 * 映像効果付与してそのまま次のIPipelineへ送る場合のワーク用GLSurface
	 */
	@Nullable
	private GLSurface work;
	@Nullable
	private Bitmap mMaskBitmap;
	// XXX ここはGLSurfaceを使った方が良いかも？
	@Size(min=16)
	@NonNull
	private final float[] mTexMatrixMask = new float[16];
	private int mMaskTexId = NO_TEXTURE;
	@Nullable
	private SurfaceTexture mMaskTexture;
	@Nullable
	private Surface mMaskSurface;

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
	 * @param surface nullまたはSurface/SurfaceHolder/SurfaceTexture/SurfaceView
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
		if ((surface != null) && !GLUtils.isSupportedSurface(surface)) {
			throw new IllegalArgumentException("Unsupported surface type!," + surface);
		}
		mManager = manager;
		manager.runOnGLThread(new Runnable() {
			@WorkerThread
			@Override
			public void run() {
				final boolean isGLES3 = manager.isGLES3();
				mDrawer = createDrawerOnGL( mManager.isGLES3(), true);
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
	 * ISurfacePipelineの実装
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
	 * ISurfacePipelineの実装
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
	 * マスク処理をせずに次のIPipelineへ送るだけかどうか
	 * コンストラクタまたはsetSurfaceで描画先のsurfaceにnullを指定するとtrue
	 * @return
	 */
	public boolean isMaskOnly() {
		return mMaskOnly;
	}

	@CallSuper
	@Override
	public void remove() {
		super.remove();
		releaseAll();
	}

	private int cnt;
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isOES, final int texId,
		@NonNull @Size(min=16) final float[] texMatrix) {

		if ((mDrawer == null) || (isOES != mDrawer.isOES())) {
			// 初回またはIPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
			releaseDrawerOnGL();
			if (DEBUG) Log.v(TAG, "onFrameAvailable:create GLDrawer2D");
			mDrawer = createDrawerOnGL( mManager.isGLES3(), isOES);
		}
		@NonNull
		final GLDrawer2D drawer = mDrawer;
		@Nullable
		final RendererTarget target;
		@Nullable
		final Bitmap bitmap;
		synchronized (mSync) {
			target = mRendererTarget;
			bitmap = mMaskBitmap;
		}
		if ((bitmap != null) && (mMaskTexture == null)) {
			createMaskTextureOnGL(bitmap);
		}
		if ((target != null)
			&& target.canDraw()) {
			target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
		}
		if (mMaskOnly && (work != null)) {
			if (DEBUG && (++cnt % 100) == 0) {
				Log.v(TAG, "onFrameAvailable:effectOnly," + cnt);
			}
			// 映像効果付与したテクスチャを次へ渡す
			super.onFrameAvailable(work.isOES(), work.getTexId(), work.getTexMatrix());
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
					releaseDrawerOnGL();
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
				if (mMaskTexture != null) {
					mMaskTexture.setDefaultBufferSize(width, height);
				}
			}
		});
	}

	/**
	 * 合成時のマスク用Bitmapをセット
	 * このビットマップのアルファ値を映像入力2のマスクとして使う
	 * @param bitmap
	 */
	public void setMask(@Nullable final Bitmap bitmap) {
		if (DEBUG) Log.v(TAG, "setMask:");
		synchronized (mSync) {
			mMaskBitmap = bitmap;
		}
		if (bitmap == null) {
			mManager.runOnGLThread(new Runnable() {
				@WorkerThread
				@Override
				public void run() {
					releaseMaskOnGL();
				}
			});
		}
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
							if (work != null) {
								if (DEBUG) Log.v(TAG, "releaseAll:release work");
								work.release();
								work = null;
							}
						}
						releaseDrawerOnGL();
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
				if (work != null) {
					work.release();
					work = null;
				}
				if (GLUtils.isSupportedSurface(surface)) {
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
					mMaskOnly = false;
				} else if (isValid()) {
					if (DEBUG) Log.v(TAG, "createTarget:create GLSurface as work texture");
					work = GLSurface.newInstance(mManager.isGLES3(), getWidth(), getHeight());
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), work, maxFps != null ? maxFps.asFloat() : 0);
					mMaskOnly = true;
				}
			}
		}
	}

	@WorkerThread
	private static GLDrawer2D createDrawerOnGL(final boolean isGLES3, final boolean isOES) {
		if (DEBUG) Log.v(TAG, "createDrawerOnGL:");
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
		if (mMaskTexture == null) {
			if (DEBUG) Log.v(TAG, "createMaskTextureOnGL:create texture for mask");
			final int uTex2 = mDrawer.glGetUniformLocation("sTexture2");
			mMaskTexId = GLHelper.initTex(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE1,
				GLES20.GL_LINEAR, GLES20.GL_LINEAR,
				GLES20.GL_CLAMP_TO_EDGE);
			mMaskTexture = new SurfaceTexture(mMaskTexId);
			mMaskTexture.setDefaultBufferSize(getWidth(), getHeight());
			mMaskSurface = new Surface(mMaskTexture);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mMaskTexId);
			GLES20.glUniform1i(uTex2, 1);
		}
		if (DEBUG) Log.v(TAG, "createMaskTextureOnGL:write mask bitmap to mask texture");
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mMaskTexId);
		try {
			final Canvas canvas = mMaskSurface.lockCanvas(null);
			try {
				if (bitmap != null) {
					canvas.drawBitmap(bitmap, 0, 0, null);
				} else if (DEBUG) {
					// DEBUGフラグtrueでオーバーレイ映像が設定されていないときは全面を薄赤色にする
					canvas.drawColor(0x7fff0000);	// ARGB
				} else {
					// DEBUGフラグfalseでオーバーレイ映像が設定されていなければ全面透過
					canvas.drawColor(0x00000000);	// ARGB
				}
			} finally {
				mMaskSurface.unlockCanvasAndPost(canvas);
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	@WorkerThread
	private void releaseMaskOnGL() {
		if (DEBUG) Log.v(TAG, "releaseMaskOnGL:");
		if (mMaskTexture != null) {
			mMaskTexture.release();
			mMaskTexture = null;
		}
		mMaskSurface = null;
		if (mMaskTexId >= 0) {
			GLHelper.deleteTex(mMaskTexId);
			mMaskTexId = NO_TEXTURE;
		}
	}

	private static final String FRAGMENT_SHADER_BASE_ES2
		= SHADER_VERSION_ES2 +
		"%s" +
		"precision highp float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +	// 入力テクスチャA
		"uniform %s sTexture2;\n" +	// 入力テクスチャB
		"void main() {\n" +
		"    highp vec4 tex1 = texture2D(sTexture, vTextureCoord);\n" +
		"    highp vec4 tex2 = texture2D(sTexture2, vTextureCoord);\n" +
		"    gl_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);\n" +
		"}\n";
	private static final String MY_FRAGMENT_SHADER_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			HEADER_2D, SAMPLER_2D, SAMPLER_2D);
	private static final String MY_FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			HEADER_OES_ES2, SAMPLER_OES, SAMPLER_OES);

	private static final String FRAGMENT_SHADER_BASE_ES3
		= SHADER_VERSION_ES3 +
		"%s" +
		"precision highp float;\n" +
		"in vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +	// 入力テクスチャA
		"uniform %s sTexture2;\n" +	// 入力テクスチャB
		"layout(location = 0) out vec4 o_FragColor;\n" +
		"void main() {\n" +
		"    highp vec4 tex1 = texture(sTexture, vTextureCoord);\n" +
		"    highp vec4 tex2 = texture(sTexture2, vTextureCoord);\n" +
		"    o_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a), tex1.a);\n" +
		"}\n";

	private static final String MY_FRAGMENT_SHADER_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			HEADER_2D, SAMPLER_2D, SAMPLER_2D);
	private static final String MY_FRAGMENT_SHADER_EXT_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			HEADER_OES_ES3, SAMPLER_OES, SAMPLER_OES);

}
