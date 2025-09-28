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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.RendererTarget;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.AbstractVideoEncoder;
import com.serenegiant.media.EncoderListener2;
import com.serenegiant.media.IRecorder;
import com.serenegiant.media.MediaCodecUtils;
import com.serenegiant.media.MediaReaper;

import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

/**
 * MediaCodecの映像エンコーダーでエンコードするためのGLPipeline/AbstractVideoEncoder実装
 * パイプライン → EncodePipeline (→ パイプライン)
 *                → IRecorderへ映像書き込み
 */
public class EncodePipeline extends AbstractVideoEncoder implements GLPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EncodePipeline.class.getSimpleName();

	/**
	 * 排他制御用
	 */
	@NonNull
	private final ReentrantLock mLock = new ReentrantLock();
	@NonNull
	private final GLManager mManager;
	@Nullable
	private GLPipeline mParent;
	@Nullable
	private GLPipeline mPipeline;
	private volatile boolean mReleased;

	@Nullable
	private GLDrawer2D mDrawer;
	@Nullable
	private RendererTarget mRendererTarget;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param recorder
	 * @param listener
	 */
	public EncodePipeline(
		@NonNull final GLManager manager,
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener2 listener) {

		super(MediaCodecUtils.MIME_VIDEO_AVC, recorder, listener);
		mManager = manager;
	}

	@Override
	public final void release() {
		if (!mReleased) {
			mReleased = true;
			if (DEBUG) Log.v(TAG, "release:");
			internalRelease();
		}
		super.release();
	}

	@CallSuper
	protected void internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:");
		mReleased = true;
		releaseTarget();
		final GLPipeline pipeline;
		mLock.lock();
		try {
			pipeline = mPipeline;
			mPipeline = null;
			mParent = null;
		} finally {
			mLock.unlock();
		}
		if (pipeline != null) {
			pipeline.release();
		}
	}

	/**
	 * GLPipelineの実装
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		setVideoSize(width, height);
		final GLPipeline pipeline = getPipeline();
		if (pipeline != null) {
			pipeline.resize(width, height);
		}
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Override
	public boolean isValid() {
		return !mReleased && mManager.isValid();
	}

	/**
	 * GLPipelineの実装
	 * パイプラインチェーンに組み込まれているかどうかを取得
	 * @return
	 */
	@Override
	public boolean isActive() {
		mLock.lock();
		try {
			return !mReleased && (mParent != null);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void setParent(@Nullable final GLPipeline parent) {
		if (DEBUG) Log.v(TAG, "setParent:" + parent);
		mLock.lock();
		try {
			mParent = parent;
		} finally {
			mLock.unlock();
		}
	}

	@Nullable
	@Override
	public GLPipeline getParent() {
		mLock.lock();
		try {
			return mParent;
		} finally {
			mLock.unlock();
		}
	}

	/**
	 * GLPipelineの実装
	 * @param pipeline
	 */
	@Override
	public void setPipeline(@Nullable final GLPipeline pipeline) {
		if (DEBUG) Log.v(TAG, "setPipeline:" + pipeline);
		mLock.lock();
		try {
			mPipeline = pipeline;
		} finally {
			mLock.unlock();
		}
		if (pipeline != null) {
			pipeline.setParent(this);
			pipeline.resize(mWidth, mHeight);
		}
	}

	/**
	 * GLPipelineの実装
	 * @return
	 */
	@Nullable
	public GLPipeline getPipeline() {
		mLock.lock();
		try {
			return mPipeline;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void remove() {
		if (DEBUG) Log.v(TAG, "remove:");
		GLPipeline parent;
		mLock.lock();
		try {
			parent = mParent;
			if (mParent instanceof DistributePipeline) {
				// 親がDistributePipelineの時は自分を取り除くだけ
				((DistributePipeline) mParent).removePipeline(this);
			} else if (mParent != null) {
				// その他のGLPipelineの時は下流を繋ぐ
				mParent.setPipeline(mPipeline);
			}
			mParent = null;
			mPipeline = null;
		} finally {
			mLock.unlock();
		}
		if (parent != null) {
			parent = GLPipeline.findFirst(parent);
			parent.refresh();
		}
	}

	private int cnt;
	/**
	 * GLPipelineの実装
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isGLES3,
		final boolean isOES,
		final int width, final int height,
		final int texId, @NonNull @Size(min=16) final float[] texMatrix) {

		@Nullable
		final GLPipeline pipeline;
		@NonNull
		final GLDrawer2D drawer;
		@Nullable
		final RendererTarget target;
		mLock.lock();
		try {
			pipeline = mPipeline;
			if ((mDrawer == null) || (isOES != mDrawer.isOES())) {
				// 初回またはGLPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
				if (mDrawer != null) {
					mDrawer.release();
				}
				if (DEBUG) Log.v(TAG, "onFrameAvailable:create drawer");
				mDrawer = GLDrawer2D.create(mManager.isGLES3(), isOES);
			}
			drawer = mDrawer;
			target = mRendererTarget;
		} finally {
			mLock.unlock();
		}
		if (pipeline != null) {
			// 次のGLPipelineへつなぐ
			pipeline.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix);
		}
		if (!mReleased && !isRequestStop()) {
			if ((target != null)
				&& target.canDraw()) {
				target.draw(drawer, GLES20.GL_TEXTURE0, texId, texMatrix);
				if (DEBUG && (++cnt % 100) == 0) {
					Log.v(TAG, "onFrameAvailable:" + cnt);
				}
			}
		}
		frameAvailableSoon();
	}

	@Override
	public void refresh() {
		final GLPipeline pipeline;
		mLock.lock();
		try {
			pipeline = mPipeline;
		} finally {
			mLock.unlock();
		}
		if (pipeline != null) {
			pipeline.refresh();
		}
	}

	/**
	 * エンコーダーへの映像入力用surfaceへの転送用にRendererTargetオブジェクトを生成する
	 * @param surface
	 * @param maxFps
	 */
	private void createTarget(@NonNull final Surface surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:surface=" + surface + ",maxFps=" + maxFps);
		mManager.runOnGLThread(() -> {
			mLock.lock();
			try {
				if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
					// すでにRendererTargetが生成されていて描画先surfaceが変更された時
					mRendererTarget.release();
					mRendererTarget = null;
				}
				if ((mRendererTarget == null)
					&& RendererTarget.isSupportedSurface(surface)) {
					mRendererTarget = RendererTarget.newInstance(
						mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
				}
			} finally {
				mLock.unlock();
			}
		});
	}

	private void releaseTarget() {
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
			if (DEBUG) Log.v(TAG, "releaseTarget:");
			if (mManager.isValid()) {
				try {
					mManager.runOnGLThread(() -> {
						if (drawer != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release drawer");
							drawer.release();
						}
						if (target != null) {
							if (DEBUG) Log.v(TAG, "releaseTarget:release target");
							target.release();
						}
					});
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			} else if (DEBUG) {
				Log.w(TAG, "releaseTarget:unexpectedly GLManager is already released!");
			}
		}
	}

//--------------------------------------------------------------------------------

	@Override
	public void stop() {
		releaseTarget();
		super.stop();
	}

	/**
	 * AbstractEncoderの実装
	 * @param listener
	 * @return
	 * @throws Exception
	 */
	@Override
	protected Encoder internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
		if (DEBUG) Log.v(TAG, "internalPrepare:");
        mTrackIndex = -1;
        final MediaCodecInfo codecInfo = MediaCodecUtils.selectVideoEncoder(MediaCodecUtils.MIME_VIDEO_AVC);
		if (codecInfo == null) {
			throw new IllegalArgumentException("Unable to find an appropriate codec for " + MediaCodecUtils.MIME_VIDEO_AVC);
		}
		if ((mWidth < MIN_WIDTH) || (mHeight < MIN_HEIGHT)) {
			throw new IllegalArgumentException("Wrong video size(" + mWidth + "x" + mHeight + ")");
		}
		if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());
//		/*if (DEBUG) */dumpProfileLevel(VIDEO_MIME_TYPE, codecInfo);
        final boolean mayFail
        	= ((mWidth >= 1000) || (mHeight >= 1000));

        final MediaFormat format = MediaFormat.createVideoFormat(MediaCodecUtils.MIME_VIDEO_AVC, mWidth, mHeight);

        // MediaCodecに適用するパラメータを設定する。誤った設定をするとMediaCodec#configureが
        // 復帰不可能な例外を生成する
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate > 0
        	? mBitRate : getConfig().getBitrate(mWidth, mHeight));
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate > 0
        	? mFramerate : getConfig().captureFps());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameIntervals > 0
        	? mIFrameIntervals : getConfig().calcIFrameIntervals());
		if (DEBUG) Log.d(TAG, "format: " + format);

        // 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
        // エンコーダーへの入力に使うSurfaceを取得する
        final MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaCodecUtils.MIME_VIDEO_AVC);
		mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        final Surface surface = mediaCodec.createInputSurface();	// API >= 18
		mediaCodec.start();
		final MediaReaper reaper = new MediaReaper.VideoReaper(mediaCodec, listener, mWidth, mHeight);
		createTarget(surface, getConfig().getCaptureFps());
		return new Encoder(mediaCodec, reaper, mayFail);
	}

}
