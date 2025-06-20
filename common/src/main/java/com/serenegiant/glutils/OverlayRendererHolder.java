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
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EGLConst;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.gl.ShaderConst.*;

/**
 * ビットマップで指定したオーバーレイ画像を重ねて表示するAbstractRendererHolder実装
 * ビットマップのアルファ値に応じてアルファブレンドされる
 */
public class OverlayRendererHolder extends AbstractRendererHolder {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = OverlayRendererHolder.class.getSimpleName();

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param callback
	 */
	public OverlayRendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EGLConst.EGL_FLAG_RECORDABLE,
			callback);
	}

	/**
	 * コンストラクタ
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @param callback
	 */
	public OverlayRendererHolder(final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {

		super(width, height, maxClientVersion, sharedContext, flags, callback, null);
		setOverlay(0, null);
	}

	/**
	 * 描画タスクを生成
	 * @param width
	 * @param height
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param flags
	 * @return
	 */
	@NonNull
	@Override
	protected BaseRendererTask createRendererTask(
		final int width, final int height,
		final int maxClientVersion,
		@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
		@NonNull GLDrawer2D.DrawerFactory drawerFactory) {

		return new OverlayRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags, drawerFactory);
	}

	public void setOverlay(final int id, @Nullable final Bitmap overlay) {
		if (DEBUG) Log.v(TAG, "setOverlay:" + overlay);
		((OverlayRendererTask)mRendererTask).setOverlay(id, overlay);
	}

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
	private static final String MY_FRAGMENT_SHADER_EXT_ES2
		= String.format(FRAGMENT_SHADER_BASE_ES2,
			SHADER_VERSION_ES2, HEADER_OES_ES2, SAMPLER_OES_ES2, SAMPLER_OES_ES2);

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
	private static final String MY_FRAGMENT_SHADER_EXT_ES3
		= String.format(FRAGMENT_SHADER_BASE_ES3,
			SHADER_VERSION_ES3, HEADER_OES_ES3, SAMPLER_OES_ES3, SAMPLER_OES_ES3);

	private static final int REQUEST_UPDATE_OVERLAY = 100;

	/**
	 * 描画タスク
	 */
	private static final class OverlayRendererTask extends BaseRendererTask {
		@Size(value=16)
		@NonNull
		private final float[] mTexMatrixOverlay = new float[16];
		private int mOverlayTexId;
		private SurfaceTexture mOverlayTexture;
		private Surface mOverlaySurface;

		/**
		 * コンストラクタ
		 * @param parent
		 * @param width
		 * @param height
		 * @param maxClientVersion
		 * @param sharedContext
		 * @param flags
		 */
		public OverlayRendererTask(@NonNull final AbstractRendererHolder parent,
			final int width, final int height,
			final int maxClientVersion,
			@Nullable final EGLBase.IContext<?> sharedContext, final int flags,
			@Nullable GLDrawer2D.DrawerFactory factory) {

			super(parent, width, height,
				maxClientVersion, sharedContext, flags, factory);
			if (DEBUG) Log.v(TAG, String.format("OverlayRendererTask(%dx%d)", width, height));
		}

		public void setOverlay(final int id, @Nullable final Bitmap overlay) {
			checkFinished();
			offer(REQUEST_UPDATE_OVERLAY, id, 0, overlay);
		}

//================================================================================
// ワーカースレッド上での処理
//================================================================================
		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@WorkerThread
		@Override
		protected void internalOnStart() {
			super.internalOnStart();
			final GLDrawer2D drawer = getDrawer();
			if (DEBUG) Log.v(TAG, "internalOnStart:" + drawer);
			if (drawer != null) {
				if (isGLES3()) {
					internalOnStartES3(drawer);
				} else {
					internalOnStartES2(drawer);
				}
			}
		}

		/**
		 * internalOnStartの下請け、GLES2用
		 */
		@WorkerThread
		private void internalOnStartES2(@NonNull final GLDrawer2D drawer) {
			if (DEBUG) Log.v(TAG, String.format("internalOnStartES2:init overlay texture(%dx%d)",
				width(), height()));
			if (DEBUG) Log.v(TAG, "internalOnStartES2:shader=" + MY_FRAGMENT_SHADER_EXT_ES2);
			drawer.updateShader(MY_FRAGMENT_SHADER_EXT_ES2);
			final int uTex1 = drawer.glGetUniformLocation("sTexture");
			GLES20.glUniform1i(uTex1, 0);
			if (DEBUG) Log.v(TAG, "internalOnStart:uTex1=" + uTex1);

			final int uTex2 = drawer.glGetUniformLocation("sTexture2");
			mOverlayTexId = GLUtils.initTex(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE1,
				GLES20.GL_LINEAR, GLES20.GL_LINEAR,
				GLES20.GL_CLAMP_TO_EDGE);
			mOverlayTexture = new SurfaceTexture(mOverlayTexId);
			mOverlayTexture.setDefaultBufferSize(width(), height());
			mOverlaySurface = new Surface(mOverlayTexture);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mOverlayTexId);
			GLES20.glUniform1i(uTex2, 1);
			if (DEBUG) Log.v(TAG, "internalOnStart:uTex2=" + uTex2);
		}

		/**
		 * internalOnStartの下請け、GLES3用
		 */
		@WorkerThread
		private void internalOnStartES3(@NonNull final GLDrawer2D drawer) {
			if (DEBUG) Log.v(TAG, String.format("internalOnStartES3:init overlay texture(%dx%d)",
				width(), height()));
			if (DEBUG) Log.v(TAG, "internalOnStartES3:shader=" + MY_FRAGMENT_SHADER_EXT_ES3);

			drawer.updateShader(MY_FRAGMENT_SHADER_EXT_ES3);
			final int uTex1 = drawer.glGetUniformLocation("sTexture");
			GLES30.glUniform1i(uTex1, 0);
			if (DEBUG) Log.v(TAG, "internalOnStart:uTex1=" + uTex1);

			final int uTex2 = drawer.glGetUniformLocation("sTexture2");
			mOverlayTexId = GLUtils.initTex(
				GL_TEXTURE_EXTERNAL_OES,
				GLES30.GL_TEXTURE1,
				GLES30.GL_LINEAR, GLES30.GL_LINEAR,
				GLES30.GL_CLAMP_TO_EDGE);
			mOverlayTexture = new SurfaceTexture(mOverlayTexId);
			mOverlayTexture.setDefaultBufferSize(width(), height());
			mOverlaySurface = new Surface(mOverlayTexture);
			GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
			GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mOverlayTexId);
			GLES30.glUniform1i(uTex2, 1);
			if (DEBUG) Log.v(TAG, "internalOnStart:uTex2=" + uTex2);
		}

		@WorkerThread
		@Override
		protected void internalOnStop() {
			if (DEBUG) Log.v(TAG, "internalOnStop:");
			if (mOverlayTexture != null) {
				mOverlayTexture.release();
				mOverlayTexture = null;
			}
			mOverlaySurface = null;
			if (mOverlayTexId >= 0) {
				GLUtils.deleteTex(mOverlayTexId);
				mOverlayTexId = GL_NO_TEXTURE;
			}
			super.internalOnStop();
		}

		@WorkerThread
		@Override
		protected boolean handleRequest(@NonNull final Message msg) {
			boolean result = true;
			if (msg.what == REQUEST_UPDATE_OVERLAY) {
				handleUpdateOverlay(msg.arg1, (Bitmap)msg.obj);
			} else {
				result = super.handleRequest(msg);
			}
			return result;
		}

		@Override
		protected void handleUpdateTexture() {
			super.handleUpdateTexture();
			mOverlayTexture.updateTexImage();
			mOverlayTexture.getTransformMatrix(mTexMatrixOverlay);
		}

		@Override
		protected void handleResize(final int width, final int height) {
			super.handleResize(width, height);
			if (DEBUG) Log.v(TAG, String.format("handleResize:(%dx%d)", width, height));
			if (mOverlayTexture != null) {
				mOverlayTexture.setDefaultBufferSize(width(), height());
			}
		}

		@SuppressLint("NewApi")
		@WorkerThread
		private void handleUpdateOverlay(final int targetId, @NonNull final Bitmap overlay) {
			if (DEBUG) Log.v(TAG, "handleUpdateOverlay:" + overlay);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mOverlayTexId);
			try {
				final Canvas canvas = mOverlaySurface.lockCanvas(null);
				try {
					if (overlay != null) {
						canvas.drawBitmap(overlay, 0, 0, null);
					} else if (DEBUG) {
						// DEBUGフラグtrueでオーバーレイ映像が設定されていないときは全面を薄赤色にする
						canvas.drawColor(0x7fff0000);	// ARGB
					} else {
						// DEBUGフラグfalseでオーバーレイ映像が設定されていなければ全面透過
						canvas.drawColor(0x00000000);	// ARGB
					}
				} finally {
					mOverlaySurface.unlockCanvasAndPost(canvas);
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			requestFrame(true, getTexId(), getTexMatrix());
		}
	}

}
