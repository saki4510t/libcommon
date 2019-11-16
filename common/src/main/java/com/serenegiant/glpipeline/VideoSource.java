package com.serenegiant.glpipeline;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.GLContext;
import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.es2.GLHelper;
import com.serenegiant.utils.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * 映像をSurface/SurfaceTextureとして受け取って
 * 他のPipelineからテクスチャとして利用可能とするためのヘルパークラス
 */
public class VideoSource implements IPipelineSource {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = VideoSource.class.getSimpleName();

	/**
	 * VideoSourceからのコールバックリスナー
	 */
	public interface VideoSourceCallback {
		/**
		 * 映像受け取り用のSurfaceが生成された
		 * @param surface
		 */
		@WorkerThread
		public void onCreate(@NonNull final  Surface surface);

		/**
		 * テキスチャが更新された
		 */
		@WorkerThread
		public void onFrameAvailable();

		/**
		 * 映像受け取り用のSurfaceが破棄された
		 */
		@WorkerThread
		public void onDestroy();
	}

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;

	private static final int REQUEST_UPDATE_TEXTURE = 1;
	private static final int REQUEST_UPDATE_SIZE = 2;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 3;

	@NonNull
	private final GLManager mManager;
	@NonNull
	private final GLContext mGLContext;
	private final Handler mGLHandler;
	@NonNull
	private final VideoSourceCallback mCallback;

	@NonNull
	private final float[] mTexMatrix = new float[16];
	private int mTexId;
	private SurfaceTexture mMasterTexture;
	private Surface mMasterSurface;
	private int mVideoWidth, mVideoHeight;

	/**
	 * コンストラクタ
	 * @param manager
	 * @param callback
	 */
	public VideoSource(@NonNull final GLManager manager,
		@NonNull final VideoSourceCallback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mManager = manager.createShared(new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull final Message msg) {
				return VideoSource.this.handleMessage(msg);
			}
		});
		mGLContext = mManager.getGLContext();
		mGLHandler = mManager.getGLHandler();
		mCallback = callback;
		mVideoWidth = DEFAULT_WIDTH;
		mVideoHeight = DEFAULT_HEIGHT;
		mGLHandler.sendEmptyMessage(REQUEST_RECREATE_MASTER_SURFACE);
	}

	/**
	 * IPipelineの実装
	 * 関連するリソースを廃棄する
	 */
	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (mManager.isValid()) {
			mGLHandler.post(new Runnable() {
				@Override
				public void run() {
					handleReleaseMasterSurface();
					mManager.release();
				}
			});
		}
	}

	/**
	 * IPipelineの実装
	 * GLManagerを取得する
	 * @return
	 */
	@NonNull
	@Override
	public GLManager getGLManager() throws IllegalStateException {
		checkValid();
		return mManager;
	}

	/**
	 * IPipelineの実装
	 * リサイズ要求
	 * @param width
	 * @param height
	 */
	@Override
	public void resize(final int width, final int height) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "resize:");
		checkValid();
		mGLHandler.sendMessage(mGLHandler.obtainMessage(REQUEST_UPDATE_SIZE, width, height));
	}

	/**
	 * IPipelineの実装
	 * VideoSourceオブジェクトが有効かどうかを取得
	 * @return
	 */
	@Override
	public boolean isValid() {
		return mManager.isValid();
	}

	/**
	 * IPipelineの実装
	 * 映像幅を取得
	 * @return
	 */
	@Override
	public int getWidth() {
		return mVideoWidth;
	}

	/**
	 * IPipelineの実装
	 * 映像高さを取得
	 * @return
	 */
	@Override
	public int getHeight() {
		return mVideoHeight;
	}

	/**
	 * IPipelineSourceの実装
	 * 映像入力用のSurfaceTextureを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public SurfaceTexture getInputSurfaceTexture() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:" + mMasterTexture);
		checkValid();
		if (mMasterTexture == null) {
			throw new IllegalStateException("has no master surface");
		}
		return mMasterTexture;
	}

	/**
	 * IPipelineSourceの実装
	 * 映像入力用のSurfaceを取得
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	@Override
	public Surface getInputSurface() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "getInputSurface:" + mMasterSurface);
		checkValid();
		if (mMasterSurface == null) {
			throw new IllegalStateException("has no master surface");
		}
		return mMasterSurface;
	}

	/**
	 * IPipelineSourceの実装
	 * テクスチャ名を取得
	 * @return
	 */
	@Override
	public int getTexId() {
		return mTexId;
	}

	/**
	 * IPipelineSourceの実装
	 * テクスチャ変換行列を取得
	 * @return
	 */
	@Override
	public float[] getTexMatrix() {
		return mTexMatrix;
	}

//--------------------------------------------------------------------------------
	protected void checkValid() throws IllegalStateException {
		if (!mManager.isValid()) {
			throw new IllegalStateException("Already released");
		}
	}

//--------------------------------------------------------------------------------
	@WorkerThread
	protected boolean handleMessage(@NonNull final Message msg) {
//		if (DEBUG) Log.v(TAG, "handleMessage:" + msg);
		switch (msg.what) {
		case REQUEST_UPDATE_TEXTURE:
			handleUpdateTex();
			return true;
		case REQUEST_UPDATE_SIZE:
			handleResize(msg.arg1, msg.arg2);
			return true;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateMasterSurface();
			mCallback.onCreate(mMasterSurface);
			return true;
		default:
			return false;
		}
	}

	/**
	 * テクスチャを更新してonFrameAvailableコールバックメソッドを呼び出す
	 */
	@WorkerThread
	protected void handleUpdateTex() {
//		if (DEBUG) Log.v(TAG, "handleUpdateTex:");
		if (mMasterTexture != null) {
			mMasterTexture.updateTexImage();
			mMasterTexture.getTransformMatrix(mTexMatrix);
			mCallback.onFrameAvailable();
		}
	}

	/**
	 * マスターSurfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleReCreateMasterSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateMasterSurface:");
		makeDefault();
		handleReleaseMasterSurface();
		makeDefault();
		mTexId = GLHelper.initTex(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
		mMasterTexture = new SurfaceTexture(mTexId);
		mMasterSurface = new Surface(mMasterTexture);
		if (BuildCheck.isAndroid4_1()) {
			mMasterTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
		}
		if (BuildCheck.isLollipop()) {
			mMasterTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mGLHandler);	// API>=21
		} else {
			mMasterTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
		mCallback.onCreate(mMasterSurface);
	}

	/**
	 * マスターSurfaceを破棄する
	 */
	@WorkerThread
	protected void handleReleaseMasterSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseMasterSurface:");
		if (mMasterSurface != null) {
			mCallback.onDestroy();
			try {
				mMasterSurface.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mMasterSurface = null;
		}
		if (mMasterTexture != null) {
			try {
				mMasterTexture.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mMasterTexture = null;
		}
		if (mTexId != 0) {
			GLHelper.deleteTex(mTexId);
			mTexId = 0;
		}
	}

	/**
	 * マスター映像サイズをリサイズ
	 * @param width
	 * @param height
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	protected void handleResize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("handleResize:(%d,%d)", width, height));
		mVideoWidth = width;
		mVideoHeight = height;
		if ((mMasterSurface == null) || !mMasterSurface.isValid()) {
			handleReCreateMasterSurface();
		}
		if (BuildCheck.isAndroid4_1()) {
			mMasterTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
		}
	}

	/**
	 * 映像受け取り用のSurfaceTextureからのコールバック
	 */
	private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener
		= new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
			if (isValid()) {
				mGLHandler.sendEmptyMessage(REQUEST_UPDATE_TEXTURE);
			}
		}
	};

	@WorkerThread
	protected void makeDefault() {
		mGLContext.makeDefault();
	}

}
