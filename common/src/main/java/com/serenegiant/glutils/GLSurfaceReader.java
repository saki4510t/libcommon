package com.serenegiant.glutils;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.BuildCheck;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.WorkerThread;

import static com.serenegiant.glutils.GLConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Surfaceを経由して映像をテクスチャとして受け取るためのクラスの基本部分を実装した抽象クラス
 * @param <T>
 */
public abstract class GLSurfaceReader<T> {
	private static final boolean DEBUG = false;
	private static final String TAG = GLSurfaceReader.class.getSimpleName();

	private static final int REQUEST_DRAW = 1;
	private static final int REQUEST_RECREATE_MASTER_SURFACE = 5;

	/**
	 * 映像を取得可能になったときに呼ばれるコールバックリスナー
	 * @param <T>
	 */
	public interface OnImageAvailableListener<T> {
		public void onImageAvailable(@NonNull final GLSurfaceReader<T> reader);
	}

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final Object mReleaseLock = new Object();
	private final int mWidth;
	private final int mHeight;
	private final int mMaxImages;
	private volatile boolean mReleased = false;
	private boolean mIsReaderValid = false;
	@NonNull
	private final EglTask mEglTask;
	@Nullable
	private OnImageAvailableListener<T> mListener;
	@Nullable
	private Handler mListenerHandler;
	@Size(min=16)
	@NonNull
	final float[] mTexMatrix = new float[16];
	private int mTexId;
	private SurfaceTexture mInputTexture;
	private Surface mInputSurface;

	public GLSurfaceReader(
		@IntRange(from=1) final int width, @IntRange(from=1) final int height,
		@IntRange(from=2) final int maxImages) {

		mWidth = width;
		mHeight = height;
		mMaxImages = maxImages;
		final Semaphore sem = new Semaphore(0);
		// GLDrawer2Dでマスターサーフェースへ描画しなくなったのでEglTask内で保持する
		// マスターサーフェースは最小サイズ(1x1)でOK
		mEglTask = new EglTask(GLUtils.getSupportedGLVersion(), null, 0) {
			@Override
			protected void onStart() {
				handleOnStart();
			}

			@Override
			protected void onStop() {
				handleOnStop();
			}

			@Override
			protected Object processRequest(final int request,
				final int arg1, final int arg2, final Object obj)
					throws TaskBreak {
				if (DEBUG) Log.v(TAG, "processRequest:");
				final Object result =  handleRequest(request, arg1, arg2, obj);
				if ((request == REQUEST_RECREATE_MASTER_SURFACE)
					&& (sem.availablePermits() == 0)) {
					sem.release();
				}
				return result;
			}
		};
		new Thread(mEglTask, TAG).start();
		if (!mEglTask.waitReady()) {
			// 初期化に失敗した時
			throw new RuntimeException("failed to start renderer thread");
		}
		mEglTask.offer(REQUEST_RECREATE_MASTER_SURFACE);
		try {
			final Surface surface;
			synchronized (mSync) {
				surface = mInputSurface;
			}
			if (surface == null) {
				if (sem.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
					mIsReaderValid = true;
				} else {
					throw new RuntimeException("failed to create surface");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
	public final void release() {
		if (!mReleased) {
			if (DEBUG) Log.v(TAG, "release:");
			mReleased = true;
			setOnImageAvailableListener(null, null);
			synchronized (mReleaseLock) {
				mEglTask.release();
				mIsReaderValid = false;
			}
			internalRelease();
		}
	}

	protected abstract void internalRelease();

	/**
	 * 映像サイズ(幅)を取得
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * 映像サイズ(高さ)を取得
	 * @return
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * 同時に取得できる最大の映像の数を取得
	 * @return
	 */
	public int getMaxImages() {
		return mMaxImages;
	}

	/**
	 * 映像受け取り用のSurfaceを取得
	 * 既に破棄されているなどしてsurfaceが取得できないときはIllegalStateExceptionを投げる
	 *
	 * @return
	 * @throws IllegalStateException
	 */
	@NonNull
	public Surface getSurface() throws IllegalStateException {
		synchronized (mSync) {
			if (mInputSurface == null) {
				throw new IllegalStateException("surface not ready, already released?");
			}
			return mInputSurface;
		}
	}

	/**
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener<T> listener,
		@Nullable final Handler handler) throws IllegalArgumentException {

		synchronized (mSync) {
			if (listener != null) {
				Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
				if (looper == null) {
					throw new IllegalArgumentException(
						"handler is null but the current thread is not a looper");
				}
				if (mListenerHandler == null || mListenerHandler.getLooper() != looper) {
					mListenerHandler = new Handler(looper);
				}
				mListener = listener;
			} else {
				mListener = null;
				mListenerHandler = null;
			}
		}
	}

	protected boolean isGLES3() {
		return mEglTask.isGLES3();
	}

	/**
	 * 最新の映像を取得する。最新以外の古い映像は全てrecycleされる。
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像が準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public abstract T acquireLatestImage() throws IllegalStateException;

	/**
	 * 次の映像を取得する
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像がが準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public abstract T acquireNextImage() throws IllegalStateException;

	/**
	 * 使った映像を返却して再利用可能にする
	 * @param image
	 */
	public abstract void recycle(T image);

//--------------------------------------------------------------------------------
// ワーカースレッド上での処理
	/**
	 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStart() {
		if (DEBUG) Log.v(TAG, "handleOnStart:");
	}

	/**
	 * ワーカースレッド終了時の処理(ここはまだワーカースレッド上)
	 */
	@WorkerThread
	private void handleOnStop() {
		if (DEBUG) Log.v(TAG, "handleOnStop:");
		handleReleaseInputSurface();
	}

	@WorkerThread
	private Object handleRequest(final int request,
		final int arg1, final int arg2, final Object obj) {

		switch (request) {
		case REQUEST_DRAW:
			handleDraw();
			break;
		case REQUEST_RECREATE_MASTER_SURFACE:
			handleReCreateInputSurface();
			break;
		default:
			if (DEBUG) Log.v(TAG, "handleRequest:" + request);
			break;
		}
		return null;
	}

	private int drawCnt;
	@WorkerThread
	private void handleDraw() {
		if (DEBUG && ((++drawCnt % 100) == 0)) Log.v(TAG, "handleDraw:" + drawCnt);
		mEglTask.removeRequest(REQUEST_DRAW);
		try {
			mEglTask.makeCurrent();
			// 何も描画しないとハングアップする端末があるので適当に塗りつぶす
			GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
			mEglTask.swap();
			mInputTexture.updateTexImage();
			mInputTexture.getTransformMatrix(mTexMatrix);
		} catch (final Exception e) {
			Log.e(TAG, "handleDraw:thread id =" + Thread.currentThread().getId(), e);
			return;
		}
		onFrameAvailable(mTexId, mTexMatrix);
	}

	protected abstract void onFrameAvailable(final int texId, @Size(min=16) @NonNull final float[] texMatrix);

	protected void callOnFrameAvailable() {
		synchronized (mSync) {
			if (mListenerHandler != null) {
				mListenerHandler.removeCallbacks(mOnImageAvailableTask);
				mListenerHandler.post(mOnImageAvailableTask);
			} else if (DEBUG) {
				Log.w(TAG, "handleDraw: Unexpectedly listener handler is null!");
			}
		}
	}

	private final Runnable mOnImageAvailableTask = new Runnable() {
		@Override
		public void run() {
			synchronized (mSync) {
				if (mListener != null) {
					mListener.onImageAvailable(GLSurfaceReader.this);
				}
			}
		}
	};

	/**
	 * 映像入力用SurfaceTexture/Surfaceを再生成する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	protected void handleReCreateInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReCreateInputSurface:");
		synchronized (mSync) {
			mEglTask.makeCurrent();
			handleReleaseInputSurface();
			mEglTask.makeCurrent();
			mTexId = GLHelper.initTex(
				GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE0, GLES20.GL_NEAREST);
			mInputTexture = new SurfaceTexture(mTexId);
			mInputSurface = new Surface(mInputTexture);
			// XXX この時点ではSurfaceTextureへ渡したテクスチャへメモリーが割り当てられておらずGLSurfaceを生成できない。
			//     少なくとも1回はSurfaceTexture#updateTexImageが呼ばれた後でGLSurfaceでラップする
			if (BuildCheck.isAndroid4_1()) {
				mInputTexture.setDefaultBufferSize(mWidth, mHeight);
			}
			mInputTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
		}
	}

	/**
	 * 映像入力用Surfaceを破棄する
	 */
	@SuppressLint("NewApi")
	@WorkerThread
	@CallSuper
	protected void handleReleaseInputSurface() {
		if (DEBUG) Log.v(TAG, "handleReleaseInputSurface:");
		synchronized (mSync) {
			if (mInputSurface != null) {
				try {
					mInputSurface.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputSurface = null;
			}
			if (mInputTexture != null) {
				try {
					mInputTexture.release();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mInputTexture = null;
			}
			if (mTexId != 0) {
				GLHelper.deleteTex(mTexId);
				mTexId = 0;
			}
		}
	}

	private final SurfaceTexture.OnFrameAvailableListener
		mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onFrameAvailable:");
			mEglTask.offer(REQUEST_DRAW);
		}
	};
}
