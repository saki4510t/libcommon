package com.serenegiant.glpipeline;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.GLUtils;
import com.serenegiant.glutils.RendererTarget;
import com.serenegiant.math.Fraction;
import com.serenegiant.media.AbstractVideoEncoder;
import com.serenegiant.media.EncoderListener;
import com.serenegiant.media.IRecorder;
import com.serenegiant.media.MediaCodecUtils;
import com.serenegiant.media.MediaReaper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * MediaCodecの映像エンコーダーでエンコードするためのIPipeline/AbstractVideoEncoder実装
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EncodePipeline extends AbstractVideoEncoder implements IPipeline {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = EncodePipeline.class.getSimpleName();

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final GLManager mManager;
	@Nullable
	private IPipeline mParent;
	@Nullable
	private IPipeline mPipeline;
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
		@NonNull final EncoderListener listener) {

		super(MediaCodecUtils.MIME_VIDEO_AVC, recorder, listener);
		mManager = manager;
		manager.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				synchronized (mSync) {
					mDrawer = GLDrawer2D.create(manager.isGLES3(), true);
				}
			}
		});
	}

	@Override
	public void release() {
		if (!mReleased) {
			mReleased = true;
			releaseTarget();
		}
		super.release();
	}

	/**
	 * IPipelineの実装
	 * @param width
	 * @param height
	 * @throws IllegalStateException
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		setVideoSize(width, height);
		final IPipeline pipeline = getPipeline();
		if (pipeline != null) {
			pipeline.resize(width, height);
		}
	}

	/**
	 * IPipelineの実装
	 * @return
	 */
	@Override
	public boolean isValid() {
		return !mReleased && mManager.isValid();
	}

	@Override
	public void setParent(@Nullable final IPipeline parent) {
		synchronized (mSync) {
			mParent = parent;
		}
	}

	@Nullable
	@Override
	public IPipeline getParent() {
		synchronized (mSync) {
			return mParent;
		}
	}

	/**
	 * IPipelineの実装
	 * @param pipeline
	 */
	@Override
	public void setPipeline(@Nullable final IPipeline pipeline) {
		synchronized (mSync) {
			mPipeline = pipeline;
		}
		if (pipeline != null) {
			pipeline.setParent(this);
			pipeline.resize(mWidth, mHeight);
		}
	}

	/**
	 * IPipelineの実装
	 * @return
	 */
	@Nullable
	public IPipeline getPipeline() {
		synchronized (mSync) {
			return mPipeline;
		}
	}

	@Override
	public void remove() {
		synchronized (mSync) {
			if (mParent != null) {
				mParent.setPipeline(mPipeline);
			}
			mParent = null;
			mPipeline = null;
		}
		releaseTarget();
	}

	private int cnt;
	/**
	 * IPipelineの実装
	 * @param isOES
	 * @param texId
	 * @param texMatrix
	 */
	@WorkerThread
	@Override
	public void onFrameAvailable(
		final boolean isOES,
		final int texId, @NonNull final float[] texMatrix) {

		final IPipeline pipeline;
		synchronized (mSync) {
			pipeline = mPipeline;
		}
		if (pipeline != null) {
			// 次のIPipelineへつなぐ
			pipeline.onFrameAvailable(isOES, texId, texMatrix);
		}
		if (!mReleased && !mRequestStop) {
			synchronized (mSync) {
				if ((mRendererTarget != null)
					&& mRendererTarget.isEnabled()
					&& mRendererTarget.isValid()) {
					if ((mDrawer == null) || (isOES != mDrawer.isOES())) {
						// 初回またはIPipelineを繋ぎ変えたあとにテクスチャが変わるかもしれない
						if (mDrawer != null) {
							mDrawer.release();
						}
						mDrawer = GLDrawer2D.create(mManager.isGLES3(), isOES);
					}
					mRendererTarget.draw(mDrawer, texId, texMatrix);
					if (DEBUG && (++cnt % 100) == 0) {
						Log.v(TAG, "onFrameAvailable:" + cnt);
					}
				}
			}
		}
		frameAvailableSoon();
	}

	/**
	 * エンコーダーへの映像入力用surfaceへの転送用にRendererTargetオブジェクトを生成する
	 * @param surface
	 * @param maxFps
	 */
	private void createTarget(@NonNull final Surface surface, @Nullable final Fraction maxFps) {
		if (DEBUG) Log.v(TAG, "createTarget:surface=" + surface + ",maxFps=" + maxFps);
		mManager.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				synchronized (mSync) {
					if ((mRendererTarget != null) && (mRendererTarget.getSurface() != surface)) {
						// すでにRendererTargetが生成されていて描画先surfaceが変更された時
						mRendererTarget.release();
						mRendererTarget = null;
					}
					if ((mRendererTarget == null)
						&& GLUtils.isSupportedSurface(surface)) {
						mRendererTarget = RendererTarget.newInstance(
							mManager.getEgl(), surface, maxFps != null ? maxFps.asFloat() : 0);
					}
				}
			}
		});
	}

	private void releaseTarget() {
		if (DEBUG) Log.v(TAG, "releaseTarget:");
		if (mManager.isValid()) {
			try {
				mManager.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						synchronized (mSync) {
							if (mDrawer != null) {
								mDrawer.release();
								mDrawer = null;
							}
							if (mRendererTarget != null) {
								mRendererTarget.release();
								mRendererTarget = null;
							}
						}
					}
				});
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
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
	 * @return
	 */
	@Override
	public int getCaptureFormat() {
		return 0; // AbstractUVCCamera.CAPTURE_RGB565;
	}

	/**
	 * AbstractEncoderの実装
	 * @param listener
	 * @return
	 * @throws Exception
	 */
	@Override
	protected boolean internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
		if (DEBUG) Log.v(TAG, "internalPrepare:");
        mTrackIndex = -1;
        mIsCapturing = true;

        final MediaCodecInfo codecInfo = MediaCodecUtils.selectVideoEncoder(MediaCodecUtils.MIME_VIDEO_AVC);
        if (codecInfo == null) {
			if (DEBUG) Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return true;
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
        mMediaCodec = MediaCodec.createEncoderByType(MediaCodecUtils.MIME_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        final Surface surface = mMediaCodec.createInputSurface();	// API >= 18
        mMediaCodec.start();
		mReaper = new MediaReaper.VideoReaper(mMediaCodec, listener, mWidth, mHeight);
		createTarget(surface, getConfig().getCaptureFps());
		return mayFail;
	}

	@Override
	public void signalEndOfInputStream() {
		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
		if (mMediaCodec != null) {
			mMediaCodec.signalEndOfInputStream();    // API >= 18
		}
	}

}
