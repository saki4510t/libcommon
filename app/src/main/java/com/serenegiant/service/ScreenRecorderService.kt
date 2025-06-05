package com.serenegiant.service
/*
 * ScreenRecordingSample
 * Sample project to capture and save audio from internal mic and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015-2025 saki t_saki@serenegiant.com
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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.serenegiant.libcommon.Const
import com.serenegiant.libcommon.MainActivity
import com.serenegiant.libcommon.R
import com.serenegiant.media.AudioSampler
import com.serenegiant.media.AudioSamplerEncoder
import com.serenegiant.media.Encoder
import com.serenegiant.media.EncoderListener2
import com.serenegiant.media.IAudioSampler
import com.serenegiant.media.IRecorder
import com.serenegiant.media.IRecorder.RecorderCallback
import com.serenegiant.media.IVideoEncoder
import com.serenegiant.media.MediaAVRecorder
import com.serenegiant.media.MediaFileUtils
import com.serenegiant.media.MediaScreenEncoder
import com.serenegiant.media.VideoConfig
import com.serenegiant.mediastore.MediaStoreUtils
import com.serenegiant.notification.NotificationCompat
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils
import com.serenegiant.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MediaProjectionからのスクリーンキャプチャ映像を録画するためのService実装
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecorderService : LifecycleService() {
	private lateinit var mMediaProjectionManager: MediaProjectionManager
	private val mVideoConfig = VideoConfig()
	private val mLock = ReentrantLock()
	private var mRecorder: IRecorder? = null
	private var mAudioSampler: IAudioSampler? = null

	override fun onCreate() {
		super.onCreate()
		if (DEBUG) Log.v(TAG, "onCreate:")
		mMediaProjectionManager =
			getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		NotificationCompat.releaseNotification(this)
		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		if (DEBUG) Log.v(TAG, "onBind:$intent")
		return null
	}

	@SuppressLint("MissingPermission")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		if (DEBUG) Log.v(TAG, "onStartCommand:$intent")
		var result = START_STICKY
		val action = intent?.action
		if (ACTION_START == action) {
			// 録画開始要求
			startScreenRecord(intent)
			updateStatus()
		} else if (ACTION_STOP == action || TextUtils.isEmpty(action)) {
			// 録画停止要求
			stopScreenRecord()
			updateStatus()
			result = START_NOT_STICKY
		} else if (ACTION_QUERY_STATUS == action) {
			// ステータス要求
			if (!updateStatus()) {
				// 録画中でなければ終了する
				stopSelf()
				result = START_NOT_STICKY
			}
		}
		return result
	}

	/**
	 * BaseServiceの抽象メソッドの実装
	 * @return
	 */
	@SuppressLint("InlinedApi")
	private fun contextIntent(): PendingIntent {
		var flags = 0
		if (BuildCheck.isAPI31()) {
			flags = flags or PendingIntent.FLAG_IMMUTABLE
		}
		return PendingIntent.getActivity(
			this, 0,
			Intent(this, MainActivity::class.java), flags
		)
	}

	//--------------------------------------------------------------------------------
	/**
	 * 録画設定を取得
	 * @return
	 */
	private fun requireConfig(): VideoConfig {
		return mVideoConfig
	}

	private fun updateStatus(): Boolean {
		val isRecording = mLock.withLock { mRecorder != null }
		val intent = Intent()
		intent.setAction(ACTION_QUERY_STATUS_RESULT)
		intent.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording)
		intent.putExtra(EXTRA_QUERY_RESULT_PAUSING, false)
		if (DEBUG) Log.v(TAG, "sendBroadcast:isRecording=$isRecording")
		LocalBroadcastManager.getInstance(this)
			.sendBroadcast(intent)
		return isRecording
	}

	/**
	 * start screen recording as .mp4 file
	 * @param intent
	 */
	@SuppressLint("InlinedApi")
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun startScreenRecord(intent: Intent) {
		if (DEBUG) Log.v(TAG, "startScreenRecord:$mRecorder")
		NotificationCompat.showNotification(this,
			NOTIFICATION,
			getString(R.string.notification_service),
			NOTIFICATION_ICON_ID, R.drawable.ic_recording_service,
			getString(R.string.notification_service),
			getString(R.string.app_name),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
			contextIntent()
		)
		lifecycleScope.launch(Dispatchers.Default) {
			val recorder = mLock.withLock { mRecorder }
			if (recorder == null) {
				// get MediaProjection
				val projection =
					mMediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
				if (projection != null) {
					val metrics = resources.displayMetrics
					var width = metrics.widthPixels
					var height = metrics.heightPixels
					if (!BuildCheck.isAndroid7()) {
						// targetSDK=24/Android7以降は1/2以外にリサイズすると録画映像が歪んでしまうのでスキップする
						if (width > height) {
							// 横長
							val scaleX = width / 1920f
							val scaleY = height / 1080f
							val scale = scaleX.coerceAtLeast(scaleY)
							width = (width / scale).toInt()
							height = (height / scale).toInt()
						} else {
							// 縦長
							val scaleX = width / 1080f
							val scaleY = height / 1920f
							val scale = scaleX.coerceAtLeast(scaleY)
							width = (width / scale).toInt()
							height = (height / scale).toInt()
						}
					}
					if (DEBUG) Log.v(TAG, String.format(
						"startRecording:(%d,%d)(%d,%d)",
						metrics.widthPixels, metrics.heightPixels,
						width, height))
					try {
						val outputFile = if (BuildCheck.isAPI29()) {
							// API29以降は対象範囲別ストレージ
							MediaStoreUtils.getContentDocument(
								this@ScreenRecorderService, "video/mp4",
								Environment.DIRECTORY_MOVIES + "/" + Const.APP_DIR,
								FileUtils.getDateTimeString() + ".mp4", null
							)
						} else {
							MediaFileUtils.getRecordingFile(
								this@ScreenRecorderService,
								Const.REQUEST_ACCESS_SD,
								Environment.DIRECTORY_MOVIES,
								"video/mp4",
								".mp4"
							)
						}
						if (DEBUG) Log.v(TAG, "startRecording:output=$outputFile")
						if (outputFile != null) {
							startRecorder(
								outputFile, projection,
								metrics.densityDpi, width, height
							)
						} else {
							throw IOException("could not access storage")
						}
					} catch (e: IOException) {
						Log.w(TAG, e)
					}
				}
			}
		}
	}

	/**
	 * stop screen recording
	 */
	private fun stopScreenRecord() {
		if (DEBUG) Log.v(TAG, "stopScreenRecord:$mRecorder")
		var needStop = true
		lifecycleScope.launch(Dispatchers.Default) {
			val recorder = mLock.withLock { mRecorder }
			if (recorder != null) {
				needStop = !releaseRecorder()
				// you should not wait here
			}
			if (needStop) {
				stopSelf()
			}
		}
	}

	/**
	 * create IRecorder instance for recording and prepare, start
	 * @param outputFile
	 * @param projection
	 */
	@Throws(IOException::class)
	private fun startRecorder(
		outputFile: DocumentFile,
		projection: MediaProjection,
		densityDpi: Int, width: Int, height: Int
	) {
		var recorder = mLock.withLock { mRecorder }
		if (DEBUG) Log.d(TAG, "startEncoder:recorder=$recorder")
		if (recorder == null) {
			try {
				recorder = createRecorder(
					outputFile,
					projection,
					densityDpi,
					width, height)
				recorder.prepare()
				recorder.startRecording()
				mLock.withLock {
					mRecorder = recorder
				}
			} catch (e: Exception) {
				Log.w(TAG, "startEncoder:", e)
				stopSampler()
				recorder?.stopRecording()
				throw e
			}
		}
		if (DEBUG) Log.v(TAG, "startEncoder:finished")
	}

	/**
	 * create recorder and related encoder
	 * @param outputFile
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("MissingPermission")
	@Throws(IOException::class)
	private fun createRecorder(
		outputFile: DocumentFile,
		projection: MediaProjection,
		densityDpi: Int, width: Int, height: Int
	): IRecorder {
		if (DEBUG) Log.v(TAG, "createRecorder:basePath=" + outputFile.uri)
		val recorder = MediaAVRecorder(this, mRecorderCallback, outputFile)
		if (DEBUG) Log.v(TAG, "createRecorder:create MediaScreenEncoder")
		val videoEncoder: IVideoEncoder =
			MediaScreenEncoder(recorder, mEncoderListener, projection, densityDpi) // API>=21
		videoEncoder.setVideoConfig(-1, 30, 10)
		videoEncoder.setVideoSize(width, height)
		if (PermissionUtils.hasAudio(this)) {
			// 音声取得のパーミッションがあるときだけ録音もする
			val sampler = AudioSampler(
				2,
				CHANNEL_COUNT, SAMPLE_RATE
			)
			mAudioSampler = sampler
			sampler.start()
			AudioSamplerEncoder(recorder, mEncoderListener, sampler)
		}
		if (DEBUG) Log.v(TAG, "createRecorder:finished")
		return recorder
	}

	private val mRecorderCallback = object : RecorderCallback {
		override fun onPrepared(recorder: IRecorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onPrepared:$recorder")
		}

		override fun onStarted(recorder: IRecorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onStarted:$recorder")
		}

		override fun onStopped(recorder: IRecorder) {
			if (DEBUG) Log.v(TAG, "RecorderCallback#onStopped:$recorder")
			stopSampler()
			mLock.withLock { mRecorder = null }
			try {
				lifecycleScope.launch(Dispatchers.Default) {
					delay(1000L)
					try {
						if (DEBUG) Log.v(TAG, "RecorderCallback#onStopped:release recorder")
						recorder.release()
					} catch (e: Exception) {
						Log.w(TAG, e)
					}
					stopSelf()
				}
			} catch (e: IllegalStateException) {
				// ignore, will be already released
				Log.w(TAG, e)
			}
			updateStatus()
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onStopped:finished")
		}

		override fun onError(e: Exception) {
			Log.w(TAG, e)
			releaseRecorder()
		}
	}
	private val mEncoderListener: EncoderListener2 = object : EncoderListener2 {
		override fun onStartEncode(
			encoder: Encoder, source: Surface?,
			mayFail: Boolean) {
			if (DEBUG) Log.v(TAG, "EncoderListener#onStartEncode:$encoder")
		}

		override fun onStopEncode(encoder: Encoder) {
			if (DEBUG) Log.v(TAG, "EncoderListener#onStopEncode:$encoder")
		}

		override fun onDestroy(encoder: Encoder) {
			if (DEBUG) Log.v(TAG, "EncoderListener#onDestroy:$encoder,recorder=$mRecorder")
		}

		override fun onError(e: Throwable) {
			Log.w(TAG, e)
			releaseRecorder()
		}
	}

	/**
	 * IRecorderを停止させ参照を解放する
	 * @return IRecorder#stopRecordingを呼び出したときはtrue
	 */
	private fun releaseRecorder(): Boolean {
		var result = false
		val recorder = mLock.withLock {
			val r = mRecorder
			mRecorder = null
			r
		}
		if ((recorder != null) && (recorder.isReady || recorder.isStarted)) {
			recorder.stopRecording()
			result = true
		}
		return result
	}

	/**
	 * オーディオサンプラーが動いていれば停止・解放する
	 */
	private fun stopSampler() {
		if (DEBUG) Log.v(TAG, "stopEncoder:")
		mAudioSampler?.release()
		mAudioSampler = null
		if (DEBUG) Log.v(TAG, "stopEncoder:finished")
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = ScreenRecorderService::class.java.simpleName
		private const val CHANNEL_COUNT = 1
		private const val SAMPLE_RATE = 44100
		private val NOTIFICATION = R.string.notification_service

		@DrawableRes
		private val NOTIFICATION_ICON_ID =
			if (BuildCheck.isAPI21()) R.drawable.ic_recording_service else R.mipmap.ic_launcher
		private const val BASE = "com.serenegiant.service.ScreenRecorderService."
		const val ACTION_START = BASE + "ACTION_START"
		const val ACTION_STOP = BASE + "ACTION_STOP"
		const val ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS"
		const val ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT"
		const val EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING"
		const val EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING"
	}
}
