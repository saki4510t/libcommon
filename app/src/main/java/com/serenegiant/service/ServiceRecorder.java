package com.serenegiant.service;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.media.IAudioSampler;
import com.serenegiant.system.BuildCheck;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

/**
 * RecordingServiceへアクセスをカプセル化するためのヘルパークラス
 */
public class ServiceRecorder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ServiceRecorder.class.getSimpleName();

	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_BINDING = 1;
	public static final int STATE_BIND = 2;
	public static final int STATE_UNBINDING = 3;

	/**
	 * 常態が変化したときのコールバックリスナー
	 */
	public interface Callback {
		/**
		 * 録画サービスと接続した
		 */
		public void onConnected();

		/**
		 * エンコーダーの準備が整った
		 * このタイミングでRecorderへ動画・音声データ供給開始する
		 */
		public void onPrepared();

		/**
		 * 録画可能
		 * このタイミングでIServiceRecorder#startを呼び出す
		 */
		public void onReady();

		/**
		 * 録画サービスから切断された
		 */
		public void onDisconnected();
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final Callback mCallback;
	@NonNull
	protected final Object mServiceSync = new Object();

	private volatile boolean mReleased = false;
	private int mState = STATE_UNINITIALIZED;
	@Nullable
	private RecordingService mService;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 */
	@SuppressLint("NewApi")
	public ServiceRecorder(@NonNull final Context context,
		@NonNull final Callback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<>(context);
		mCallback = callback;

		final Intent serviceIntent = createServiceIntent(context);
		if (BuildCheck.isOreo()) {
			context.startForegroundService(serviceIntent);
		} else {
			context.startService(serviceIntent);
		}
		doBindService();
	}

	/**
	 * デストラクタ
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関係するリソースを破棄する
	 */
	public void release() {
		if (!mReleased) {
			mReleased = true;
			if (DEBUG) Log.v(TAG, "release:");
			internalRelease();
		}
	}

	/**
	 * サービスとバインドして使用可能になっているかどうかを取得
	 * @return
	 */
	public boolean isReady() {
		synchronized (mServiceSync) {
			return !mReleased && (mService != null);
		}
	}

	/**
	 * 録画中かどうかを取得
	 * @return
	 */
	public boolean isRecording() {
		final RecordingService service = peekService();
		return !mReleased && (service != null) && service.isRecording();
	}

	/**
	 * 録画設定をセット
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @throws IllegalStateException
	 */
	public void setVideoSettings(final int width, final int height,
		final int frameRate, final float bpp) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "setVideoSettings:");
		checkReleased();
		final RecordingService service = getService();
		if (service != null) {
			service.setVideoSettings(width, height, frameRate, bpp);
		}
	}

	/**
	 * 録音用のIAudioSamplerをセット
	 * #writeAudioFrameと排他使用
	 * @param sampler
	 */
	public void setAudioSampler(@NonNull final IAudioSampler sampler)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "setAudioSampler:");
		checkReleased();
		final RecordingService service = getService();
		if (service != null) {
			service.setAudioSampler(sampler);
		}
	}

	/**
	 * 録音設定
	 * #setAudioSamplerで設置したIAudioSamplerの設定が優先される
	 * @param sampleRate
	 * @param channelCount
	 * @throws IllegalStateException
	 */
	public void setAudioSettings(final int sampleRate, final int channelCount)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "setAudioSettings:");
		checkReleased();
		final RecordingService service = getService();
		if (service != null) {
			service.setAudioSettings(sampleRate, channelCount);
		}
	}

	/**
	 * 録画録音の準備
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void prepare() throws IllegalStateException, IOException {
		if (DEBUG) Log.v(TAG, "prepare:");

		final RecordingService service = getService();
		if (service != null) {
			service.prepare();
		}
	}

	/**
	 * 録画開始
	 * @param outputDir 出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void start(@NonNull final DocumentFile outputDir, @NonNull final String name)
		throws IllegalStateException, IOException {

		if (DEBUG) Log.v(TAG, "start:outputDir=" + outputDir);
		checkReleased();
		final RecordingService service = getService();
		if (service != null) {
			service.start(outputDir, name);
		} else {
			throw new IllegalStateException("start:service is not ready");
		}
	}

	/**
	 * 録画終了
	 */
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
		final RecordingService service = getService();
		if (service != null) {
			service.stop();
		}
	}

	/**
	 * 録画用の映像を入力するためのSurfaceを取得
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Nullable
	public Surface getInputSurface() {
		if (DEBUG) Log.v(TAG, "getInputSurface:");
		checkReleased();
		final RecordingService service = getService();
		return service != null ? service.getInputSurface() : null;
	}

	/**
	 * 録画用の映像フレームが準備できた時に録画サービスへ通知するためのメソッド
	 */
	public void frameAvailableSoon() {
//		if (DEBUG) Log.v(TAG, "frameAvailableSoon:");
		final RecordingService service = getService();
		if (!mReleased && (service != null)) {
			service.frameAvailableSoon();
		}
	}

	/**
	 * 録音用の音声データを書き込む
	 * #setAudioSamplerと排他使用
	 * @param buffer
	 * @param presentationTimeUs
	 * @throws IllegalStateException
	 */
	public void writeAudioFrame(@NonNull final ByteBuffer buffer,
		final long presentationTimeUs) {

//		if (DEBUG) Log.v(TAG, "writeAudioFrame:");
		checkReleased();
		final RecordingService service = getService();
		if (service != null) {
			service.writeAudioFrame(buffer, presentationTimeUs);
		}
	}

//--------------------------------------------------------------------------------
	@NonNull
	private Intent createServiceIntent(@NonNull Context context) {
		return new Intent(RecordingService.class.getName())
			.setPackage(context.getPackageName());
	}

	/**
	 * #releaseの実態
	 */
	private void internalRelease() {
		mCallback.onDisconnected();
		stop();
		doUnBindService();
	}

	/**
	 * Releaseされたかどうかをチェックして、
	 * ReleaseされていればIllegalStateExceptionを投げる
	 *
	 * @throws IllegalStateException
	 */
	private void checkReleased() throws IllegalStateException {
		if (mReleased) {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * 録画サービスとの接続状態取得用のリスナーの実装
	 */
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			if (DEBUG) Log.v(TAG, "onServiceConnected:name=" + name);
			synchronized (mServiceSync) {
				if (mState == STATE_BINDING) {
					mState = STATE_BIND;
				}
				mService = ((RecordingService.LocalBinder)service).getService();
				mServiceSync.notifyAll();
				if (mService != null) {
					mService.addListener(mStateChangeListener);
				}
			}
			mCallback.onConnected();
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (DEBUG) Log.v(TAG, "onServiceDisconnected:name=" + name);
			mCallback.onDisconnected();
			synchronized (mServiceSync) {
				if (mService != null) {
					mService.removeListener(mStateChangeListener);
				}
				mState = STATE_UNINITIALIZED;
				mService = null;
				mServiceSync.notifyAll();
			}
		}
	};

	/**
	 * Bind client to camera connection service
	 */
	private void doBindService() {
		if (DEBUG) Log.v(TAG, "doBindService:");
		final Context context = mWeakContext.get();
		if (context != null) {
			synchronized (mServiceSync) {
				if ((mState == STATE_UNINITIALIZED) && (mService == null)) {
					mState = STATE_BINDING;
					final Intent intent = createServiceIntent(context);
					if (DEBUG) Log.v(TAG, "call Context#bindService");
					final boolean result = context.bindService(intent,
						mServiceConnection, Context.BIND_AUTO_CREATE);
					if (!result) {
						mState = STATE_UNINITIALIZED;
						Log.w(TAG, "failed to bindService");
					}
				}
			}
		}
	}

	/**
	 * Unbind from camera service
	 */
	private void doUnBindService() {
		final boolean needUnbind;
		synchronized (mServiceSync) {
			needUnbind = mService != null;
			mService = null;
			if (mState == STATE_BIND) {
				mState = STATE_UNBINDING;
			}
		}
		if (needUnbind) {
			if (DEBUG) Log.v(TAG, "doUnBindService:");
			final Context context = mWeakContext.get();
			if (context != null) {
				if (DEBUG) Log.v(TAG, "call Context#unbindService");
				context.unbindService(mServiceConnection);
			}
		}
	}

	/**
	 * 接続中の録画サービスを取得する。
	 * バインド中なら待機する。
	 *
	 * @return
	 */
	@Nullable
	private RecordingService getService() {
//		if (DEBUG) Log.v(TAG, "getService:");
		RecordingService result = null;
		synchronized (mServiceSync) {
			if ((mState == STATE_BINDING) || (mState == STATE_BIND)) {
				if (mService == null) {
					try {
						mServiceSync.wait();
					} catch (final InterruptedException e) {
						Log.w(TAG, e);
					}
				}
				result = mService;
			}
		}
//		if (DEBUG) Log.v(TAG, "getService:finished:" + result);
		return result;
	}

	/**
	 * 接続中の録画サービスを取得する。
	 * 接続中でも待機しない。
	 *
	 * @return
	 */
	@Nullable
	private RecordingService peekService() {
		synchronized (mServiceSync) {
			return mService;
		}
	}

	/**
	 * 録画サービスの状態が変化したときのコールバックリスナーの実装
	 */
	private final RecordingService.StateChangeListener
		mStateChangeListener = new RecordingService.StateChangeListener() {
		@Override
		public void onStateChanged(
			@NonNull final RecordingService service, final int state) {

			ServiceRecorder.this.onStateChanged(service, state);
		}
	};

	/**
	 * 録画サービスでステートが変化したときのコールバック処理の実体
	 *
	 * @param service
	 * @param state
	 */
	private void onStateChanged(
		@NonNull final RecordingService service, final int state) {

		switch (state) {
		case RecordingService.STATE_INITIALIZED:
			if (DEBUG) Log.v(TAG, "onStateChanged:STATE_INITIALIZED");
			break;
		case RecordingService.STATE_PREPARING:
			if (DEBUG) Log.v(TAG, "onStateChanged:STATE_PREPARING");
			break;
		case RecordingService.STATE_PREPARED:
			mCallback.onPrepared();
			break;
		case RecordingService.STATE_READY:
			if (DEBUG) Log.v(TAG, "onStateChanged:STATE_READY");
			mCallback.onReady();
			break;
		case RecordingService.STATE_RECORDING:
			if (DEBUG) Log.v(TAG, "onStateChanged:STATE_RECORDING");
			break;
		case RecordingService.STATE_RELEASING:
			if (DEBUG) Log.v(TAG, "onStateChanged:STATE_RELEASING");
			break;
		default:
			if (DEBUG) Log.d(TAG, "onStateChanged:unknown state " + state);
			break;
		}
	}
}
