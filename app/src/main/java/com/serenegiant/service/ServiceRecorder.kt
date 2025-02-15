package com.serenegiant.service
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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.documentfile.provider.DocumentFile
import com.serenegiant.media.IAudioSampler
import com.serenegiant.service.RecordingService.LocalBinder
import com.serenegiant.service.RecordingService.StateChangeListener
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.ThreadUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.withLock

/**
 * RecordingServiceへアクセスをカプセル化するためのヘルパークラス
 */
open class ServiceRecorder @SuppressLint("NewApi") constructor(
	context: Context,
	callback: Callback
) {
	/**
	 * 常態が変化したときのコールバックリスナー
	 */
	interface Callback {
		/**
		 * 録画サービスと接続した
		 */
		fun onConnected()

		/**
		 * エンコーダーの準備が整った
		 * このタイミングでRecorderへ動画・音声データ供給開始する
		 */
		fun onPrepared()

		/**
		 * 録画可能
		 * このタイミングでIServiceRecorder#startを呼び出す
		 */
		fun onReady()

		/**
		 * 録画サービスから切断された
		 */
		fun onDisconnected()
	}

	//--------------------------------------------------------------------------------
	private val mWeakContext: WeakReference<Context>
	private val mCallback: Callback
	/**
	 * 録画サービスとの接続状態取得用のリスナーの実装
	 */
	private val mServiceConnection: ServiceConnection
	protected val mServiceLock = ReentrantLock()
	private val mScope = CoroutineScope(Job() + Dispatchers.Default +  CoroutineName("scope_$TAG"))

	@Volatile
	private var mReleased = false
	private var mState = STATE_UNINITIALIZED
	@Volatile
	private var mService: RecordingService? = null

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mWeakContext = WeakReference(context)
		mCallback = callback
		mServiceConnection = object : ServiceConnection {
			override fun onServiceConnected(name: ComponentName, service: IBinder) {
				if (DEBUG) Log.v(TAG, "onServiceConnected:name=$name")
				mServiceLock.withLock {
					if (mState == STATE_BINDING) {
						mState = STATE_BIND
					}
					mService = (service as LocalBinder).service
					mService?.addListener(mStateChangeListener)
				}
				mScope.launch {
					mCallback.onConnected()
				}
			}

			override fun onServiceDisconnected(name: ComponentName) {
				if (DEBUG) Log.v(TAG, "onServiceDisconnected:name=$name")
				if (mScope.isActive) {
					mScope.launch {
						mCallback.onDisconnected()
					}
				}
				mServiceLock.withLock {
					mState = STATE_UNINITIALIZED
					mService?.removeListener(mStateChangeListener)
					mService = null
				}
			}
		}
		val serviceIntent = createServiceIntent(context)
		context.startService(serviceIntent)
		mScope.launch {
			doBindService()
		}
	}

	/**
	 * 関係するリソースを破棄する
	 */
	suspend fun release() {
		if (!mReleased) {
			mReleased = true
			if (DEBUG) Log.v(TAG, "release:")
			internalRelease()
		}
	}

	/**
	 * サービスとバインドして使用可能になっているかどうかを取得
	 * @return
	 */
	val isReady: Boolean
		get() {
			mServiceLock.withLock {
				return !mReleased && mService != null
			}
		}

	/**
	 * 録画中かどうかを取得
	 * @return
	 */
	val isRecording: Boolean
		get() {
			val service = peekService()
			return !mReleased && service != null && service.isRecording
		}

	/**
	 * 録画設定をセット
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @throws IllegalStateException
	 */
	@Throws(IllegalStateException::class)
	suspend fun setVideoSettings(
		width: Int, height: Int,
		frameRate: Int, bpp: Float
	) {
		if (DEBUG) Log.v(TAG, "setVideoSettings:")
		checkReleased()
		val service = getService()
		service?.setVideoSettings(width, height, frameRate, bpp)
	}

	/**
	 * 録音用のIAudioSamplerをセット
	 * #writeAudioFrameと排他使用
	 * @param sampler
	 */
	@Throws(IllegalStateException::class)
	suspend fun setAudioSampler(sampler: IAudioSampler) {
		if (DEBUG) Log.v(TAG, "setAudioSampler:")
		checkReleased()
		val service = getService()
		service?.setAudioSampler(sampler)
	}

	/**
	 * 録音設定
	 * #setAudioSamplerで設置したIAudioSamplerの設定が優先される
	 * @param sampleRate
	 * @param channelCount
	 * @throws IllegalStateException
	 */
	@Throws(IllegalStateException::class)
	suspend fun setAudioSettings(sampleRate: Int, channelCount: Int) {
		if (DEBUG) Log.v(TAG, "setAudioSettings:")
		checkReleased()
		val service = getService()
		service?.setAudioSettings(sampleRate, channelCount)
	}

	/**
	 * 録画録音の準備
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Throws(IllegalStateException::class, IOException::class)
	suspend fun prepare() {
		if (DEBUG) Log.v(TAG, "prepare:")
		val service = getService()
		service?.prepare()
	}

	/**
	 * 録画開始
	 * @param output 出力ファイル
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Throws(IllegalStateException::class, IOException::class)
	suspend fun start(output: DocumentFile) {
		if (DEBUG) Log.v(TAG, "start:output=" + output + ",uri=" + output.uri)
		checkReleased()
		val service = getService()
		if (service != null) {
			service.start(output)
		} else {
			throw IllegalStateException("start:service is not ready")
		}
	}

	/**
	 * 録画終了
	 */
	suspend fun stop() {
		if (DEBUG) Log.v(TAG, "stop:")
		val service = getService()
		service?.stop()
	}

	/**
	 * 録画用の映像を入力するためのSurfaceを取得
	 * @return
	 */
	suspend fun getInputSurface(): Surface? {
		if (DEBUG) Log.v(TAG, "getInputSurface:")
		checkReleased()
		val service = getService()
		return service?.inputSurface
	}

	/**
	 * 録画用の映像フレームが準備できた時に録画サービスへ通知するためのメソッド
	 */
	fun frameAvailableSoon() {
//		if (DEBUG) Log.v(TAG, "frameAvailableSoon:");
		mScope.launch {
			val service = getService()
			if (!mReleased && service != null) {
				service.frameAvailableSoon()
			}
		}
	}

	/**
	 * 録音用の音声データを書き込む
	 * #setAudioSamplerと排他使用
	 * @param buffer
	 * @param presentationTimeUs
	 * @throws IllegalStateException
	 */
	fun writeAudioFrame(
		buffer: ByteBuffer,
		presentationTimeUs: Long
	) {
//		if (DEBUG) Log.v(TAG, "writeAudioFrame:");
		checkReleased()
		mScope.launch {
			val service = getService()
			service?.writeAudioFrame(buffer, presentationTimeUs)
		}
	}

	//--------------------------------------------------------------------------------
	private fun createServiceIntent(context: Context): Intent {
		if (DEBUG) Log.v(TAG, "createServiceIntent:")
		return Intent(RecordingService::class.java.name)
			.setPackage(context.packageName)
	}

	/**
	 * #releaseの実態
	 */
	private suspend fun internalRelease() {
		if (DEBUG) Log.v(TAG, "internalRelease:")
		mCallback.onDisconnected()
		stop()
		doUnBindService()
		mScope.cancel()
	}

	/**
	 * Releaseされたかどうかをチェックして、
	 * ReleaseされていればIllegalStateExceptionを投げる
	 *
	 * @throws IllegalStateException
	 */
	@Throws(IllegalStateException::class)
	private fun checkReleased() {
		check(!mReleased) { "already released" }
	}

	/**
	 * Bind client to camera connection service
	 */
	private fun doBindService() {
		if (DEBUG) Log.v(TAG, "doBindService:")
		val context = mWeakContext.get()
		if (context != null) {
			val needBind = mServiceLock.withLock {
				if (mState == STATE_UNINITIALIZED && mService == null) {
					mState = STATE_BINDING
					true
				} else {
					false
				}
			}
			if (needBind) {
				val intent = createServiceIntent(context)
				if (DEBUG) Log.v(TAG, "call Context#bindService,connection=$mServiceConnection")
				val result = context.bindService(
					intent,
					mServiceConnection, Context.BIND_AUTO_CREATE
				)
				if (!result) {
					mState = STATE_UNINITIALIZED
					Log.w(TAG, "failed to bindService")
				}
			}
		}
	}

	/**
	 * Unbind from camera service
	 */
	private fun doUnBindService() {
		val needUnbind = mServiceLock.withLock {
			val b = mService != null
			mService = null
			if (mState == STATE_BIND) {
				mState = STATE_UNBINDING
			}
			b
		}
		if (DEBUG) Log.v(TAG, "doUnBindService:needUnbind=$needUnbind")
		if (needUnbind) {
			if (DEBUG) Log.v(TAG, "doUnBindService:")
			val context = mWeakContext.get()
			if (context != null) {
				if (DEBUG) Log.v(TAG, "call Context#unbindService")
				context.unbindService(mServiceConnection)
			}
		}
	}

	/**
	 * 接続中の録画サービスを取得する。
	 * バインド中なら待機する。
	 *
	 * @return
	 */
	private suspend fun getService(): RecordingService? {
//		if (DEBUG) Log.v(TAG, "getService:");
		var result: RecordingService? = null
		while (!mReleased && (result == null)) {
			delay(100)
			result = mServiceLock.withLock {
				if (mState == STATE_BINDING || mState == STATE_BIND) {
					mService
				} else {
					if (DEBUG) Log.v(TAG, "getService:state is not BIND/BINDING,$mState")
					null
				}
			}
		}
		if (DEBUG && (result == null)) Log.v(TAG, "getService:finished with null");
		return result
	}

	/**
	 * 接続中の録画サービスを取得する。
	 * 接続中でも待機しない。
	 *
	 * @return
	 */
	private fun peekService(): RecordingService? {
		return mServiceLock.withLock {
			mService
		}
	}

	/**
	 * 録画サービスの状態が変化したときのコールバックリスナーの実装
	 */
	private val mStateChangeListener: StateChangeListener = object : StateChangeListener {
		override fun onStateChanged(
			service: RecordingService, state: Int
		) {
			this@ServiceRecorder.onStateChanged(service, state)
		}
	}

	/**
	 * 録画サービスでステートが変化したときのコールバック処理の実体
	 *
	 * @param service
	 * @param state
	 */
	private fun onStateChanged(service: RecordingService, state: Int) {
		when (state) {
			RecordingService.STATE_INITIALIZED ->
				if (DEBUG) Log.v(TAG, "onStateChanged:STATE_INITIALIZED")

			RecordingService.STATE_PREPARING ->
				if (DEBUG) Log.v(TAG, "onStateChanged:STATE_PREPARING")

			RecordingService.STATE_PREPARED -> mCallback.onPrepared()
			RecordingService.STATE_READY -> {
				if (DEBUG) Log.v(TAG, "onStateChanged:STATE_READY")
				mCallback.onReady()
			}

			RecordingService.STATE_RECORDING ->
				if (DEBUG) Log.v(TAG, "onStateChanged:STATE_RECORDING")

			RecordingService.STATE_RELEASING ->
				if (DEBUG) Log.v(TAG, "onStateChanged:STATE_RELEASING")

			else -> if (DEBUG) Log.d(TAG, "onStateChanged:unknown state $state")
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = ServiceRecorder::class.java.simpleName
		const val STATE_UNINITIALIZED = 0
		const val STATE_BINDING = 1
		const val STATE_BIND = 2
		const val STATE_UNBINDING = 3
	}
}
