package com.serenegiant.service;
/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015-2022 saki t_saki@serenegiant.com
 *
 * File name: ScreenRecorderService.java
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
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.libcommon.Const;
import com.serenegiant.libcommon.MainActivity;
import com.serenegiant.libcommon.R;
import com.serenegiant.media.AbstractAudioEncoder;
import com.serenegiant.media.AudioSampler;
import com.serenegiant.media.AudioSamplerEncoder;
import com.serenegiant.media.Encoder;
import com.serenegiant.media.EncoderListener;
import com.serenegiant.media.IAudioSampler;
import com.serenegiant.media.IRecorder;
import com.serenegiant.media.IVideoEncoder;
import com.serenegiant.media.MediaAVRecorder;
import com.serenegiant.media.MediaFileUtils;
import com.serenegiant.media.MediaScreenEncoder;
import com.serenegiant.media.VideoConfig;
import com.serenegiant.mediastore.MediaStoreUtils;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.FileUtils;

import java.io.IOException;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * MediaProjectionからのスクリーンキャプチャ映像を録画するためのService実装
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenRecorderService extends BaseService {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = ScreenRecorderService.class.getSimpleName();

	private static final int CHANNEL_COUNT = 1;
	private static final int SAMPLE_RATE = 44100;
	private static final int NOTIFICATION = R.string.notification_service;
	@DrawableRes
	private static final int NOTIFICATION_ICON_ID = BuildCheck.isAPI21()
		? R.drawable.ic_recording_service : R.mipmap.ic_launcher;

	private static final String BASE = "com.serenegiant.service.ScreenRecorderService.";
	public static final String ACTION_START = BASE + "ACTION_START";
	public static final String ACTION_STOP = BASE + "ACTION_STOP";
	public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
	public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
	public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
	public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";

	private MediaProjectionManager mMediaProjectionManager;
	@Nullable
	private VideoConfig mVideoConfig;
	@Nullable
	private IRecorder mRecorder;
	@Nullable
	private IAudioSampler mAudioSampler;

	public ScreenRecorderService() {
		super();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.v(TAG, "onCreate:");
		mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		releaseNotification();
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(@NonNull final Intent intent) {
		super.onBind(intent);
		if (DEBUG) Log.v(TAG, "onBind:" + intent);
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);
		if (DEBUG) Log.v(TAG, "onStartCommand:" + intent);
		int result = START_STICKY;
		final String action = intent != null ? intent.getAction() : null;
		if (ACTION_START.equals(action)) {
			// 録画開始要求
			startScreenRecord(intent);
			updateStatus();
		} else if (ACTION_STOP.equals(action) || TextUtils.isEmpty(action)) {
			// 録画停止要求
			stopScreenRecord();
			updateStatus();
			result = START_NOT_STICKY;
		} else if (ACTION_QUERY_STATUS.equals(action)) {
			// ステータス要求
			if (!updateStatus()) {
				// 録画中でなければ終了する
				stopSelf();
				result = START_NOT_STICKY;
			}
		}
		return result;
	}

	/**
	 * BaseServiceの抽象メソッドの実装
	 * @return
	 */
	@Override
	protected IntentFilter createIntentFilter() {
		return null;
	}

	/**
	 * BaseServiceの抽象メソッドの実装
	 * @param context
	 * @param intent
	 */
	@Override
	protected void onReceiveLocalBroadcast(final Context context, final Intent intent) {

	}

	/**
	 * BaseServiceの抽象メソッドの実装
	 * @return
	 */
	@SuppressLint("InlinedApi")
	@Override
	protected PendingIntent contextIntent() {
		int flags = 0;
		if (BuildCheck.isAPI31()) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}
		return PendingIntent.getActivity(this, 0,
			new Intent(this, MainActivity.class), flags);
	}

//--------------------------------------------------------------------------------
	/**
	 * 録画設定を取得
	 * @return
	 */
	@NonNull
	private VideoConfig requireConfig() {
		if (mVideoConfig == null) {
			mVideoConfig = new VideoConfig();
		}
		return mVideoConfig;
	}

	private boolean updateStatus() {
		final boolean isRecording;
		synchronized (mSync) {
			isRecording = (mRecorder != null);
		}
		final Intent result = new Intent();
		result.setAction(ACTION_QUERY_STATUS_RESULT);
		result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
		result.putExtra(EXTRA_QUERY_RESULT_PAUSING, false);
		if (DEBUG) Log.v(TAG, "sendBroadcast:isRecording=" + isRecording);
		sendLocalBroadcast(result);
		return isRecording;
	}

	/**
	 * start screen recording as .mp4 file
	 * @param intent
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void startScreenRecord(@NonNull final Intent intent) {
		if (DEBUG) Log.v(TAG, "startScreenRecord:" + mRecorder);
		synchronized (mSync) {
			if (mRecorder == null) {
				// get MediaProjection
			    final MediaProjection projection = mMediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent);
			    if (projection != null) {
				    final DisplayMetrics metrics = getResources().getDisplayMetrics();
					int width = metrics.widthPixels;
					int height = metrics.heightPixels;
					if (!BuildCheck.isAndroid7()) {
						// targetSDK=24/Android7以降は1/2以外にリサイズすると録画映像が歪んでしまうのでスキップする
						if (width > height) {
							// 横長
							final float scale_x = width / 1920f;
							final float scale_y = height / 1080f;
							final float scale = Math.max(scale_x,  scale_y);
							width = (int)(width / scale);
							height = (int)(height / scale);
						} else {
							// 縦長
							final float scale_x = width / 1080f;
							final float scale_y = height / 1920f;
							final float scale = Math.max(scale_x,  scale_y);
							width = (int)(width / scale);
							height = (int)(height / scale);
						}
					}
					if (DEBUG) Log.v(TAG, String.format("startRecording:(%d,%d)(%d,%d)", metrics.widthPixels, metrics.heightPixels, width, height));
					try {
						DocumentFile outputFile;
						if (BuildCheck.isAPI29()) {
							// API29以降は対象範囲別ストレージ
							outputFile = MediaStoreUtils.getContentDocument(
								this, "video/mp4",
								Environment.DIRECTORY_MOVIES + "/" + Const.APP_DIR,
								FileUtils.getDateTimeString() + ".mp4", null);
						} else {
							outputFile = MediaFileUtils.getRecordingFile(
								this, Const.REQUEST_ACCESS_SD, Environment.DIRECTORY_MOVIES, "video/mp4",".mp4");
						}
						if (DEBUG) Log.v(TAG, "startRecording:output=" + outputFile);
						if (outputFile != null) {
							startRecorder(outputFile, projection,
								metrics.densityDpi, width, height);
						} else {
							throw new IOException("could not access storage");
						}
						showNotification(NOTIFICATION,
							getString(R.string.notification_service),
							NOTIFICATION_ICON_ID, R.drawable.ic_recording_service,
							getString(R.string.notification_service),
							getString(R.string.app_name),
							contextIntent());
					} catch (final IOException e) {
						Log.w(TAG, e);
					}
			    }
			}
		}
	}

	/**
	 * stop screen recording
	 */
	private void stopScreenRecord() {
		if (DEBUG) Log.v(TAG, "stopScreenRecord:" + mRecorder);
		boolean needStop = true;
		synchronized (mSync) {
			if (mRecorder != null) {
				needStop = !releaseRecorder();
				// you should not wait here
			}
		}
		if (needStop) {
			stopSelf();
		}
	}

	/**
	 * create IRecorder instance for recording and prepare, start
	 * @param outputFile
	 * @param projection
	 */
	private void startRecorder(
		@NonNull final DocumentFile outputFile,
		@NonNull final MediaProjection projection,
		final int densityDpi, final int width, final int height) throws IOException {

		IRecorder recorder = mRecorder;
		if (DEBUG) Log.d(TAG, "startEncoder:recorder=" + recorder);
		if (recorder == null) {
			try {
				recorder = createRecorder(
					outputFile,
					projection,
					densityDpi,
					width, height);
				recorder.prepare();
				recorder.startRecording();
				mRecorder = recorder;
			} catch (final Exception e) {
				Log.w(TAG, "startEncoder:", e);
				stopSampler();
				if (recorder != null) {
					recorder.stopRecording();
				}
				mRecorder = null;
				throw e;
			}
		}
		if (DEBUG) Log.v(TAG, "startEncoder:finished");
	}

	/**
	 * create recorder and related encoder
	 * @param outputFile
	 * @return
	 * @throws IOException
	 */
	private IRecorder createRecorder(
		@NonNull final DocumentFile outputFile,
		@NonNull final MediaProjection projection,
		final int densityDpi, final int width, final int height) throws IOException {

		if (DEBUG) Log.v(TAG, "createRecorder:basePath=" + outputFile.getUri());
		IRecorder recorder = new MediaAVRecorder(
			this, mRecorderCallback, outputFile);
		if (DEBUG) Log.v(TAG, "createRecorder:create MediaScreenEncoder");
		final IVideoEncoder videoEncoder = new MediaScreenEncoder(recorder, mEncoderListener, projection, densityDpi); // API>=21
		videoEncoder.setVideoConfig(-1, 30, 10);
		videoEncoder.setVideoSize(width, height);
		mAudioSampler = new AudioSampler(2,
			CHANNEL_COUNT, SAMPLE_RATE,
			AbstractAudioEncoder.SAMPLES_PER_FRAME,
			AbstractAudioEncoder.FRAMES_PER_BUFFER);
		mAudioSampler.start();
		new AudioSamplerEncoder(recorder, mEncoderListener, 2, mAudioSampler);
		if (DEBUG) Log.v(TAG, "createRecorder:finished");
		return recorder;
	}

	private final IRecorder.RecorderCallback mRecorderCallback = new IRecorder.RecorderCallback() {
		@Override
		public void onPrepared(final IRecorder recorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onPrepared:" + recorder);
		}

		@Override
		public void onStarted(final IRecorder recorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onStarted:" + recorder);
		}

		@Override
		public void onStopped(final IRecorder recorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onStopped:" + recorder);
			stopSampler();
			mRecorder = null;
			try {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						if (recorder != null) {
							try {
								if (DEBUG) Log.v(TAG, "RecorderCallback#onStopped:release recorder");
								recorder.release();
							} catch (final Exception e) {
								Log.w(TAG, e);
							}
						}
						stopSelf();
					}
				}, 1000);
			} catch (final IllegalStateException e) {
				// ignore, will be already released
				Log.w(TAG, e);
			}
			updateStatus();
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onStopped:finished");
		}

		@Override
		public void onError(final Exception e) {
			Log.w(TAG, e);
			releaseRecorder();
		}
	};

	private final EncoderListener mEncoderListener = new EncoderListener() {
		@Override
		public void onStartEncode(
			@NonNull final Encoder encoder, Surface source,
			final int captureFormat, final boolean mayFail) {

			if (DEBUG) Log.v(TAG, "EncoderListener#onStartEncode:" + encoder);
		}

		@Override
		public void onStopEncode(@NonNull final Encoder encoder) {
			if (DEBUG) Log.v(TAG, "EncoderListener#onStopEncode:" + encoder);
		}

		@Override
		public void onDestroy(@NonNull final Encoder encoder) {
			if (DEBUG) Log.v(TAG, "EncoderListener#onDestroy:"
				+ encoder + ",mRecorder=" + mRecorder);
		}

		@Override
		public void onError(@NonNull final Throwable e) {
			Log.w(TAG, e);
			releaseRecorder();
		}
	};

	/**
	 * IRecorderを停止させ参照を解放する
	 * @return IRecorder#stopRecordingを呼び出したときはtrue
	 */
	private boolean releaseRecorder() {
		boolean result = false;
		synchronized (mSync) {
			if ((mRecorder != null) && (mRecorder.isReady() || mRecorder.isStarted())) {
				mRecorder.stopRecording();
				result = true;
			}
			mRecorder = null;
		}
		return result;
	}

	/**
	 * オーディオサンプラーが動いていれば停止・解放する
	 */
	private void stopSampler() {
		if (DEBUG) Log.v(TAG, "stopEncoder:");
		if (mAudioSampler != null) {
			mAudioSampler.release();
			mAudioSampler = null;
		}
		if (DEBUG) Log.v(TAG, "stopEncoder:finished");
	}

}
