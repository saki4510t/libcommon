package com.serenegiant.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.egl.EGLBase;
import com.serenegiant.egl.EglTask;
import com.serenegiant.gl.GLDrawer2D;
import com.serenegiant.utils.HandlerThreadHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MediaProjectionからの映像をエンコードするためのAbstractVideoEncoder実装
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaScreenEncoder extends AbstractVideoEncoder {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = MediaScreenEncoder.class.getSimpleName();

	// parameters for recording
	private static final String MIME = MediaCodecUtils.MIME_VIDEO_AVC;

	private MediaProjection mMediaProjection;
	private final int mDensity;
	private Surface mInputSurface;
	private final Handler mHandler;

	/**
	 * コンストラクタ
	 *
	 * @param recorder
	 * @param listener
	 * @param projection
	 */
	public MediaScreenEncoder(
		@NonNull final IRecorder recorder,
		@NonNull final EncoderListener listener,
		@NonNull final MediaProjection projection,
		final int density) {

		super(MIME, recorder, listener);
		mMediaProjection = projection;
		mDensity = density;
		mHandler = HandlerThreadHandler.createHandler(TAG);
	}

	@Override
	protected boolean internalPrepare(@NonNull final MediaReaper.ReaperListener listener) throws Exception {
		if (DEBUG) Log.v(TAG, "internalPrepare:");
		mTrackIndex = -1;
		mIsCapturing = true;

		final MediaCodecInfo codecInfo = MediaCodecUtils.selectVideoEncoder(MIME);
		if (codecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME);
			return true;
		}
		if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());
//		/*if (DEBUG) */dumpProfileLevel(VIDEO_MIME_TYPE, codecInfo);

		final MediaFormat format = MediaFormat.createVideoFormat(MIME, mWidth, mHeight);
		// MediaCodecに適用するパラメータを設定する。誤った設定をするとMediaCodec#configureが
		// 復帰不可能な例外を生成する
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
		format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate > 0
			? mBitRate : getConfig().getBitrate(mWidth, mHeight));
		format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate > 0
			? mFramerate : getConfig().captureFps());
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameIntervals > 0
			? mIFrameIntervals : getConfig().calcIFrameIntervals());
		if (DEBUG) Log.d(TAG, "format: " + format);

		// 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
		// エンコーダーへの入力に使うSurfaceを取得する
		mMediaCodec = MediaCodec.createEncoderByType(MIME);
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mInputSurface = mMediaCodec.createInputSurface();    // API >= 18
		mMediaCodec.start();
		mReaper = new MediaReaper.VideoReaper(mMediaCodec, listener, mWidth, mHeight);
		new Thread(mDrawTask, DrawTask.class.getSimpleName()).start();
		return false;
	}

	/**
	 * Releases encoder resources.
	 */
	@Override
	public void release() {
		if (DEBUG) Log.d(TAG, "release:");
		super.release();
		mInputSurface = null;
	}

	@Override
	public void start() {
		if (DEBUG) Log.d(TAG, "start:");
		super.start();
	}

	@Override
	public void stop() {
		if (DEBUG) Log.d(TAG, "stop:");
		mDrawTask.release();
		super.stop();
	}

	@Override
	public void signalEndOfInputStream() {
		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
		if (mMediaCodec != null) {
			mMediaCodec.signalEndOfInputStream();    // API >= 18
		}
	}

	private volatile boolean requestDraw;
	@NonNull
	private final DrawTask mDrawTask = new DrawTask(null, 0);

	/**
	 * MediaProjectionのVirtualDisplayから受け取った映像を
	 * MediaCodecの映像エンコーダーの入力Surfaceへ転送するためのEglTask実装
	 */
	private final class DrawTask extends EglTask {
		private VirtualDisplay display;
		private long intervals;
		private int mTexId;
		private SurfaceTexture mSourceTexture;
		private Surface mSourceSurface;
		private EGLBase.IEglSurface mEncoderSurface;
		private GLDrawer2D mDrawer;
		private final float[] mTexMatrix = new float[16];

		public DrawTask(@Nullable final EGLBase.IContext<?> sharedContext, final int flags) {
			super(sharedContext, flags);
		}

		@Override
		protected void onStart() {
			if (DEBUG) Log.d(TAG, String.format("DrawTask#onStart:(%dx%d)", mWidth, mHeight));
			mDrawer = GLDrawer2D.create(isGLES3(), true);
			mTexId = mDrawer.initTex(GLES20.GL_TEXTURE0);
			mSourceTexture = new SurfaceTexture(mTexId);
			mSourceTexture.setDefaultBufferSize(mWidth, mHeight);    // これを入れないと映像が取れない
			mSourceSurface = new Surface(mSourceTexture);
			mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
			mEncoderSurface = getEgl().createFromSurface(mInputSurface);

			if (DEBUG) Log.d(TAG, "DrawTask#onStart:setup VirtualDisplay");
			intervals = (long) (1000f / mFramerate);
			display = mMediaProjection.createVirtualDisplay(
				"Capturing Display",
				mWidth, mHeight, mDensity,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
				mSourceSurface, mCallback, mHandler);
			if (DEBUG) Log.v(TAG, "DrawTask#onStart:screen capture loop:display=" + display);
			// 録画タスクを起床
			queueEvent(mDrawTask);
		}

		@Override
		protected void onStop() {
			if (DEBUG) Log.v(TAG, "DrawTask#onStop:");
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			if (mSourceSurface != null) {
				mSourceSurface.release();
				mSourceSurface = null;
			}
			if (mSourceTexture != null) {
				mSourceTexture.release();
				mSourceTexture = null;
			}
			if (mEncoderSurface != null) {
				mEncoderSurface.release();
				mEncoderSurface = null;
			}
			makeCurrent();
			if (display != null) {
				if (DEBUG) Log.v(TAG, "DrawTask#onStop:release VirtualDisplay");
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					display.release();
				}
			}
			if (DEBUG) Log.v(TAG, "DrawTask#onStop:tear down MediaProjection");
			if (mMediaProjection != null) {
				mMediaProjection.stop();
				mMediaProjection = null;
			}
		}

		@Override
		protected boolean onError(final Throwable e) {
			if (DEBUG) Log.w(TAG, "DrawTask.onError:", e);
			return false;
		}

		@Override
		protected Object processRequest(final int request, final int arg1, final int arg2, final Object obj) {
			return null;
		}

		// TextureSurfaceで映像を受け取った際のコールバックリスナー
		private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener
			= new SurfaceTexture.OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//				if (DEBUG) Log.v(TAG, "DrawTask#onFrameAvailable:mIsRecording=" + mIsRecording);
				if (mIsCapturing) {
					synchronized (mSync) {
						requestDraw = true;
						mSync.notify();
					}
				}
			}
		};

		private final Runnable mDrawTask = new Runnable() {
			@Override
			public void run() {
//				if (DEBUG) Log.v(TAG, "DrawTask#draw:");
				boolean localRequestDraw;
				synchronized (mSync) {
					localRequestDraw = requestDraw;
					if (!localRequestDraw) {
						try {
							mSync.wait(intervals);
							localRequestDraw = requestDraw;
							requestDraw = false;
						} catch (final InterruptedException e) {
							return;
						}
					}
				}
				if (mIsCapturing && !mRequestStop) {
					if (localRequestDraw) {
						mSourceTexture.updateTexImage();
						mSourceTexture.getTransformMatrix(mTexMatrix);
					}
					// SurfaceTextureで受け取った画像をMediaCodecの入力用Surfaceへ描画する
					mEncoderSurface.makeCurrent();
					mDrawer.draw(GLES20.GL_TEXTURE0, mTexId, mTexMatrix, 0);
					mEncoderSurface.swap();
					// EGL保持用のオフスクリーンに描画しないとハングアップする機種の為のworkaround
					makeCurrent();
					GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
					GLES20.glFlush();
					frameAvailableSoon();
					queueEvent(this);
				} else {
					releaseSelf();
				}
//				if (DEBUG) Log.v(TAG, "DrawTask#draw:finished");
			}
		};

	}

	/**
	 * MediaProjection#createVirtualDisplayへ引き渡すVirtualDisplay.Callbackコールバックオブジェクト
	 */
	private final VirtualDisplay.Callback mCallback = new VirtualDisplay.Callback() {
		/**
		 * Called when the virtual display video projection has been
		 * paused by the system or when the surface has been detached
		 * by the application by calling setSurface(null).
		 * The surface will not receive any more buffers while paused.
		 */
		@Override
		public void onPaused() {
			if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onPaused:");
		}

		/**
		 * Called when the virtual display video projection has been
		 * resumed after having been paused.
		 */
		@Override
		public void onResumed() {
			if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onResumed:");
		}

		/**
		 * Called when the virtual display video projection has been
		 * stopped by the system.  It will no longer receive frames
		 * and it will never be resumed.  It is still the responsibility
		 * of the application to release() the virtual display.
		 */
		@Override
		public void onStopped() {
			if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onStopped:");
			mRequestStop = true;
		}
	};
}
