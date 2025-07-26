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
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.annotation.DrawableRes
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.serenegiant.libcommon.MainActivity
import com.serenegiant.libcommon.R
import com.serenegiant.media.AudioSampler
import com.serenegiant.media.IAudioSampler
import com.serenegiant.media.IAudioSampler.AudioSamplerCallback
import com.serenegiant.media.IMuxer
import com.serenegiant.media.MediaCodecUtils
import com.serenegiant.media.MediaMuxerWrapper
import com.serenegiant.media.MediaReaper
import com.serenegiant.media.MediaReaper.AudioReaper
import com.serenegiant.media.MediaReaper.ReaperListener
import com.serenegiant.media.MediaReaper.VideoReaper
import com.serenegiant.media.VideoConfig
import com.serenegiant.notification.NotificationCompat
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.ThreadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.withLock

/**
 * MP4として録音録画するためのService実装
 * 録画中にアプリが終了したときにMP4ファイルへ最後まで出力できずに
 * 再生できない録画ファイルになってしまう場合があるので、
 * 対策としてServiceとして実装してアプリのライフサイクルと
 * MP4ファイル出力を切り離すためにServiceとして実装する
 */
open class RecordingService : LifecycleService() {
	/**
	 * RecordingServiceのステート変更時のコールバックリスナー
	 */
	interface StateChangeListener {
		fun onStateChanged(
			service: RecordingService,
			state: Int
		)
	}

	//--------------------------------------------------------------------------------
	/**
	 * Binder class to access this local service
	 */
	inner class LocalBinder : Binder() {
		val service: RecordingService
			get() = this@RecordingService
	}

	//--------------------------------------------------------------------------------
	private val mListeners: MutableSet<StateChangeListener> = CopyOnWriteArraySet()
	private val binder: IBinder = LocalBinder()
	private val mLock = ReentrantLock()
	private var mVideoConfig = VideoConfig()
	private var mIntent = Intent()
	private var mState = STATE_UNINITIALIZED
	private var mIsBind = false

	@Volatile
	private var mIsEos = false

	// 動画関係
	@Volatile
	private var mUseVideo = false

	/** 動画のサイズ(録画する場合)  */
	private var mWidth = 0
	private var mHeight = 0
	private var mFrameRate = 0
	private var mBpp = 0f
	private var mVideoFormat: MediaFormat? = null
	private var mVideoEncoder: MediaCodec? = null
	private var mInputSurface: Surface? = null
	private var mVideoReaper: VideoReaper? = null

	// 音声関係
	@Volatile
	private var mUseAudio = false
	private var mAudioSampler: IAudioSampler? = null
	private var mIsOwnAudioSampler = false
	private var mSampleRate = 0
	private var mChannelCount = 0
	private var mAudioFormat: MediaFormat? = null
	private var mAudioEncoder: MediaCodec? = null
	private var mAudioReaper: AudioReaper? = null
	private var mMuxer: IMuxer? = null
	private var mVideoTrackIx = -1
	private var mAudioTrackIx = -1

	override fun onCreate() {
		super.onCreate()
		if (DEBUG) Log.v(TAG, "onCreate:")
		mLock.withLock {
			mState = STATE_UNINITIALIZED
			mIsBind = false
		}
		internalResetSettings()
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		mLock.withLock {
			mState = STATE_UNINITIALIZED
			mIsBind = false
		}
		if (DEBUG) Log.v(TAG, "onDestroy:releaseNotification")
		NotificationCompat.releaseNotification(this)
		mListeners.clear()
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (DEBUG) Log.i(TAG, "onStartCommand:startId=$startId: $intent")
		super.onStartCommand(intent, flags, startId)
		return START_STICKY
	}

	@SuppressLint("InlinedApi")
	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		if (DEBUG) Log.v(TAG, "onBind:intent=$intent")
		// XXX API21未満はVectorDrawableを通知領域のスモールアイコンにできない
		if (mState == STATE_UNINITIALIZED) {
			state = STATE_INITIALIZED
		}
		mIsBind = true
		mIntent = intent
		return binder
	}

	override fun onUnbind(intent: Intent): Boolean {
		if (DEBUG) Log.d(TAG, "onUnbind:$intent")
		mIsBind = false
		mIntent = Intent()
		mListeners.clear()
		checkStopSelf()
		if (DEBUG) Log.v(TAG, "onUnbind:finished")
		return false // onRebind使用不可
	}

	//--------------------------------------------------------------------------------
	/**
	 * 録画中かどうかを取得する。
	 * このクラスでは#isRunningと同じ値を返すがこちらは
	 * オーバーライド不可
	 * @return
	 */
	val isRecording: Boolean
		get() {
			mLock.withLock {
				val state: Int = state
				return ((state == STATE_PREPARING)
					|| (state == STATE_PREPARED)
					|| (state == STATE_READY)
					|| (state == STATE_RECORDING))
			}
		}
	/**
	 * 録画サービスの処理を実行中かどうかを返す
	 * このクラスでは#isRecordingと同じ値を返す。
	 * こちらはオーバーライド可能でサービスの自己終了判定に使う。
	 * 録画中では無いが録画の前後処理中などで録画サービスを
	 * 終了しないようにしたい時に下位クラスでオーバーライドする。
	 * @return true: サービスの自己終了しない
	 */
	private val isRunning: Boolean
		get() {
			mLock.withLock {
				val state: Int = state
				return (isRecording
					|| (state == STATE_SCAN_FILE))
			}
		}

	//--------------------------------------------------------------------------------
	/**
	 * ステート変更時のコールバックリスナーを登録する
	 * @param listener
	 */
	fun addListener(listener: StateChangeListener) {
		if (DEBUG) Log.v(TAG, "addListener:$listener")
		mListeners.add(listener)
	}

	/**
	 * ステート変更時のコールバックリスナーを登録解除する
	 * @param listener
	 */
	fun removeListener(listener: StateChangeListener) {
		if (DEBUG) Log.v(TAG, "removeListener:$listener")
		mListeners.remove(listener)
	}

	/**
	 * 録画設定
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 */
	@Throws(IllegalStateException::class)
	fun setVideoSettings(
		width: Int, height: Int,
		frameRate: Int, bpp: Float
	) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSettings:(%dx%d)@%d",
			width, height, frameRate))
		if (state != STATE_INITIALIZED) {
			throw IllegalStateException("state=$state")
		}
		mWidth = width
		mHeight = height
		mFrameRate = frameRate
		mBpp = bpp
		mUseVideo = true
	}

	/**
	 * 録音用のIAudioSamplerをセット
	 * #writeAudioFrameと排他使用
	 * @param sampler
	 */
	@Throws(IllegalStateException::class)
	fun setAudioSampler(sampler: IAudioSampler) {
		if (DEBUG) Log.v(TAG, "setAudioSampler:$sampler")
		if (state != STATE_INITIALIZED) {
			throw IllegalStateException()
		}
		releaseOwnAudioSampler()
		if (mAudioSampler !== sampler) {
			mAudioSampler?.removeCallback(mAudioSamplerCallback)
			mAudioSampler = sampler
			mChannelCount = 0
			mSampleRate = 0
		}
	}

	/**
	 * 録音設定
	 * #setAudioSamplerで設置したIAudioSamplerの設定が優先される
	 * @param sampleRate
	 * @param channelCount
	 */
	@Throws(IllegalStateException::class)
	fun setAudioSettings(sampleRate: Int, channelCount: Int) {
		if (DEBUG) Log.v(TAG, String.format("setAudioSettings:sampling=%d,channelCount=%d",
			sampleRate, channelCount))
		if (state != STATE_INITIALIZED) {
			throw IllegalStateException("state=$state")
		}
		if ((mSampleRate != sampleRate) || (mChannelCount != channelCount)) {
			createOwnAudioSampler(sampleRate, channelCount)
			mSampleRate = sampleRate
			mChannelCount = channelCount
		}
		mUseAudio = true
	}

	/**
	 * 録音用の音声データを書き込む
	 * #setAudioSamplerと排他使用
	 * @param buffer position/limitを正しくセットしておくこと
	 * @param presentationTimeUs
	 */
	@Throws(IllegalStateException::class, UnsupportedOperationException::class)
	fun writeAudioFrame(buffer: ByteBuffer, presentationTimeUs: Long) {
//		if (DEBUG) Log.v(TAG, "writeAudioFrame:");
		if (state < STATE_PREPARED) {
			throw IllegalStateException()
		}
		if (mAudioSampler != null) {
			throw UnsupportedOperationException("audioSampler is already set")
		}
		encodeAudio(buffer, presentationTimeUs)
	}

	/**
	 * 録画の準備
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@SuppressLint("MissingPermission")
	@Throws(IllegalStateException::class, IOException::class)
	fun prepare() {
		if (DEBUG) Log.v(TAG, "prepare:")
		lifecycleScope.launch(Dispatchers.Default) {
			if (state != STATE_INITIALIZED) {
				throw IllegalStateException("state=$state")
			}
			state = STATE_PREPARING
			try {
				if (DEBUG) Log.v(TAG, "prepare:start")
				if ((mWidth > 0) && (mHeight > 0)) {
					// 録画する時
					if (mFrameRate <= 0) {
						mFrameRate = requireConfig().captureFps()
					}
					if (mBpp <= 0) {
						mBpp = requireConfig().getBitrate(mWidth, mHeight).toFloat()
					}
					internalPrepare(mWidth, mHeight, mFrameRate, mBpp)
					createEncoder(mWidth, mHeight, mFrameRate, mBpp)
				}
				if (mAudioSampler != null) {
					mSampleRate = mAudioSampler!!.samplingFrequency
					mChannelCount = mAudioSampler!!.channels
					mAudioSampler!!.addCallback(mAudioSamplerCallback)
				}
				if ((mSampleRate > 0)
					&& (mChannelCount == 1) || (mChannelCount == 2)
				) {
					// 録音する時
					internalPrepare(mSampleRate, mChannelCount)
					createEncoder(mSampleRate, mChannelCount)
				}
				if (((mAudioSampler != null)
						&& !mAudioSampler!!.isStarted)) {
					if (DEBUG) Log.v(TAG, "prepare:start audio sampler")
					mAudioSampler!!.start()
				}
				state = STATE_PREPARED
			} catch (e: IllegalStateException) {
				releaseEncoder()
				throw e
			} catch (e: IOException) {
				releaseEncoder()
				throw e
			}
		}
	}

	/**
	 * 録画開始
	 * @param output 出力ファイル
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Throws(IllegalStateException::class, IOException::class)
	fun start(output: DocumentFile) {
		if (DEBUG) Log.v(TAG, "start:")
		lifecycleScope.launch(Dispatchers.Default) {
			if ((!mUseVideo || (mVideoFormat != null)) && (!mUseAudio || (mAudioFormat != null))) {
				internalStart(output, mVideoFormat, mAudioFormat)
			} else {
				throw IllegalStateException("Not all MediaFormat received.")
			}
			state = STATE_RECORDING
		}
	}

	/**
	 * 録画終了
	 */
	fun stop() {
		if (DEBUG) Log.v(TAG, "stop:")
		lifecycleScope.launch(Dispatchers.Default) {
			if (mAudioEncoder != null) {
				signalEndOfInputStream(mAudioEncoder!!)
			}
			internalStop()
			internalResetSettings()
		}
	}

	/**
	 * 映像入力用のSurfaceを取得する
	 * @return
	 * @throws IllegalStateException #prepareと#startの間以外で呼ぶとIllegalStateExceptionを投げる
	 */
	@get:Throws(IllegalStateException::class)
	val inputSurface: Surface?
		get() {
			if (DEBUG) Log.v(TAG, "getInputSurface:")
			if (state == STATE_PREPARED) {
				frameAvailableSoon()
				return mLock.withLock {
					mInputSurface
				}
			} else {
				throw IllegalStateException()
			}
		}

	/**
	 * 入力映像が準備できた時に録画サービスへ通知するためのメソッド
	 * MediaReaper#frameAvailableSoonを呼ぶためのヘルパーメソッド
	 */
	fun frameAvailableSoon() {
		mVideoReaper?.frameAvailableSoon()
	}

	//--------------------------------------------------------------------------------
	/**
	 * BaseServiceの抽象メソッドの実装
	 * サービスノティフィケーションを選択した時に実行されるPendingIntentの生成
	 * 普通はMainActivityを起動させる。
	 * デフォルトはnullを返すだけでノティフィケーションを選択しても何も実行されない。
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

	private val intent: Intent
		get() = mIntent

	/**
	 * 録画設定を取得
	 * @return
	 */
	private fun requireConfig(): VideoConfig {
		return mVideoConfig
	}

	/**
	 * 録画サービスの現在の状態フラグを取得
	 * @return
	 */
	private var state: Int
		get() {
			return mLock.withLock {
				mState
			}
		}
		private set(newState) {
			var changed: Boolean
			mLock.withLock {
				changed = mState != newState
				mState = newState
			}
			if (changed) {
				try {
					lifecycleScope.launch(Dispatchers.Default) {
						for (listener: StateChangeListener in mListeners) {
							try {
								listener.onStateChanged(this@RecordingService, newState)
							} catch (e: Exception) {
								mListeners.remove(listener)
							}
						}
					}
				} catch (e: Exception) {
					Log.w(TAG, e)
				}
			}
		}

	/**
	 * 録画サービスを自己終了できるかどうかを確認して
	 * 終了可能であればService#stopSelfを呼び出す
	 */
	private fun checkStopSelf() {
		if (DEBUG) Log.v(TAG, "checkStopSelf:mIsBind=$mIsBind,isRunning=$isRunning")
		lifecycleScope.launch {
			if (canStopSelf(mIsBind or isRunning)) {
				if (DEBUG) Log.v(TAG, "stopSelf")
				state = STATE_RELEASING
				try {
					lifecycleScope.launch(Dispatchers.Default) {
						if (DEBUG) Log.v(TAG, "checkStopSelf:releaseNotification")
						NotificationCompat.releaseNotification(this@RecordingService,
							NOTIFICATION,
							getString(R.string.notification_service))
						stopSelf()
					}
				} catch (e: Exception) {
					state = STATE_UNINITIALIZED
					Log.w(TAG, e)
				}
			}
		}
	}

	/**
	 * サービスを終了可能かどうかを確認
	 * @return 終了可能であればtrue
	 */
	private fun canStopSelf(isRunning: Boolean): Boolean {
		if (DEBUG) Log.v(TAG, ("canStopSelf:isRunning=$isRunning,isBonded=$mIsBind"))
		return !isRunning
	}
	//--------------------------------------------------------------------------------
	/**
	 * 録画設定をリセット
	 */
	private fun internalResetSettings() {
		if (DEBUG) Log.v(TAG, "internalResetSettings:")
		mFrameRate = -1
		mHeight = -1
		mWidth = -1
		mBpp = -1.0f
		mUseAudio = false
		mUseVideo = false
		mIsEos = false
	}

	/**
	 * 録画準備の実態, mSyncをロックして呼び出される
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Throws(IllegalStateException::class, IOException::class)
	protected fun internalPrepare(
		width: Int, height: Int,
		frameRate: Int, bpp: Float
	) {
		if (DEBUG) Log.v(TAG, "internalPrepare:video")
	}

	/**
	 * 録音準備の実態, mSyncをロックして呼び出される
	 * @param sampleRate
	 * @param channelCount
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Throws(IllegalStateException::class, IOException::class)
	protected fun internalPrepare(sampleRate: Int, channelCount: Int) {
		if (DEBUG) Log.v(TAG, "internalPrepare:audio")
	}

	/**
	 * 録画用のMediaCodecのエンコーダーを生成
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun createEncoder(
		width: Int, height: Int,
		frameRate: Int, bpp: Float
	) {
		if (DEBUG) Log.v(TAG, "createEncoder:video")
		val codecInfo = MediaCodecUtils.selectVideoEncoder(MediaCodecUtils.MIME_VIDEO_AVC)
			?: throw IOException("Unable to find an appropriate codec for " + MediaCodecUtils.MIME_VIDEO_AVC)
		val format = MediaFormat.createVideoFormat(MediaCodecUtils.MIME_VIDEO_AVC, width, height)
		// MediaCodecに適用するパラメータを設定する。
		// 誤った設定をするとMediaCodec#configureが復帰不可能な例外を生成する
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
			MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) // API >= 18
		format.setInteger(MediaFormat.KEY_BIT_RATE,
			requireConfig().getBitrate(width, height, frameRate, bpp))
		format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // IFrameの間隔は1秒にする
		if (DEBUG) Log.d(TAG, "createEncoder:video format:$format")

		// 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
		val videoEncoder = MediaCodec.createEncoderByType(MediaCodecUtils.MIME_VIDEO_AVC)
		mVideoEncoder = videoEncoder
		videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
		// エンコーダーへの入力に使うSurfaceを取得する
		mLock.withLock {
			mInputSurface = videoEncoder.createInputSurface() // API >= 18
		}
		videoEncoder.start()
		mVideoReaper = VideoReaper(
			videoEncoder, mReaperListener,
			width, height)
		if (DEBUG) Log.v(TAG, "createEncoder:finished")
	}

	/**
	 * 録音用のMediaCodecエンコーダーを生成
	 * @param sampleRate
	 * @param channelCount
	 */
	@Throws(IOException::class)
	protected fun createEncoder(sampleRate: Int, channelCount: Int) {
		if (DEBUG) Log.v(TAG, "createEncoder:audio")
		val codecInfo = MediaCodecUtils.selectAudioEncoder(MediaCodecUtils.MIME_AUDIO_AAC)
			?: throw IOException("Unable to find an appropriate codec for " + MediaCodecUtils.MIME_AUDIO_AAC)
		val format = MediaFormat.createAudioFormat(
			MediaCodecUtils.MIME_AUDIO_AAC, sampleRate, channelCount
		)
		format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
		format.setInteger(
			MediaFormat.KEY_CHANNEL_MASK,
			if (mChannelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
		)
		format.setInteger(MediaFormat.KEY_BIT_RATE, 64000 /*FIXMEパラメータにする*/)
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)
		// MediaCodecに適用するパラメータを設定する。
		// 誤った設定をするとMediaCodec#configureが復帰不可能な例外を生成する
		if (DEBUG) Log.d(TAG, "createEncoder:audio format:$format")

		// 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
		mAudioEncoder = MediaCodec.createEncoderByType(MediaCodecUtils.MIME_AUDIO_AAC)
		mAudioEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
		mAudioEncoder!!.start()
		mAudioReaper = AudioReaper(
			mAudioEncoder!!, mReaperListener, sampleRate, channelCount
		)
		if (DEBUG) Log.v(TAG, "createEncoder:finished")
	}

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private var prevInputPTSUs: Long = -1
	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * System.nanoTime()を1000で割ってマイクロ秒にしただけ(切り捨て)
	 * @return
	 */
	private val inputPTSUs: Long
		get() {
			var result = System.nanoTime() / 1000L
			if (result <= prevInputPTSUs) {
				result = prevInputPTSUs + 9643
			}
			prevInputPTSUs = result
			return result
		}

	/**
	 * エンコーダーを破棄
	 */
	protected fun releaseEncoder() {
		if (DEBUG) Log.v(TAG, "releaseEncoder:")
		val videoReaper: VideoReaper?
		val audioReaper: AudioReaper?
		mLock.withLock {
			videoReaper = mVideoReaper
			mVideoReaper = null
			audioReaper = mAudioReaper
			mAudioReaper = null
			mInputSurface = null
		}
		videoReaper?.release()
		mVideoEncoder = null
		audioReaper?.release()
		mAudioEncoder = null
		releaseOwnAudioSampler()
		internalResetSettings()
		if (DEBUG) Log.v(TAG, "releaseEncoder:finished")
	}

	private fun createOwnAudioSampler(sampleRate: Int, channelCount: Int) {
		if (DEBUG) Log.v(TAG, String.format(
				"createOwnAudioSampler:sampling=%d,channelCount=%d",
				sampleRate, channelCount)
		)
		releaseOwnAudioSampler()
		if (sampleRate > 0 && ((channelCount == 1) || (channelCount == 2))) {
			if (DEBUG) Log.v(TAG, "createOwnAudioSampler:create AudioSampler")
			mAudioSampler = AudioSampler(
				2,
				channelCount, sampleRate
			)
			mIsOwnAudioSampler = true
		}
	}

	private fun releaseOwnAudioSampler() {
		if (DEBUG) Log.v(TAG, "releaseOwnAudioSampler:own=$mIsOwnAudioSampler,$mAudioSampler")
		if (mAudioSampler != null) {
			mAudioSampler!!.removeCallback(mAudioSamplerCallback)
			if (mIsOwnAudioSampler) {
				mAudioSampler!!.release()
				mAudioSampler = null
				mChannelCount = 0
				mSampleRate = 0
			}
		}
		mIsOwnAudioSampler = false
	}

	/**
	 * #startの実態, mSyncをロックして呼ばれる
	 * @param output 出力ファイル
	 * @param videoFormat
	 * @param audioFormat
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	@Throws(IOException::class)
	protected fun internalStart(
		output: DocumentFile,
		videoFormat: MediaFormat?,
		audioFormat: MediaFormat?
	) {
		if (DEBUG) Log.v(TAG, "internalStart:")
		MediaMuxerWrapper.USE_MEDIASTORE_OUTPUT_STREAM = USE_MEDIASTORE_OUTPUT_STREAM && BuildCheck.isAndroid8()
		val muxer: MediaMuxerWrapper =
			MediaMuxerWrapper.newInstance(this, output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
				?: throw IllegalArgumentException()
		mVideoTrackIx = if (videoFormat != null) muxer.addTrack(videoFormat) else -1
		mAudioTrackIx = if (audioFormat != null) muxer.addTrack(audioFormat) else -1
		if (DEBUG) Log.v(TAG, "internalStart:videoTackIx=$mVideoTrackIx,audioTrackIx=$mAudioTrackIx")
		mLock.withLock {
			mMuxer = muxer
		}
		muxer.start()
	}

	/**
	 * 録画終了の実態, mSyncをロックして呼ばれる
	 */
	private fun internalStop() {
		if (DEBUG) Log.v(TAG, "internalStop:")
		val muxer = mLock.withLock {
			val r = mMuxer
			mMuxer = null
			r
		}
		if (muxer != null) {
			try {
				if (DEBUG) Log.v(TAG, "internalStop:stop muxer,$muxer")
				muxer.stop()
			} catch (e: Exception) {
				Log.w(TAG, e)
			}
			lifecycleScope.launch(Dispatchers.Default) {
				try {
					if (DEBUG) Log.v(TAG, "internalStop:release muxer")
					muxer.release()
					if (DEBUG) Log.v(TAG, "internalStop:muxer released")
				} catch (e: Exception) {
					Log.w(TAG, e)
				}
			}
		}
		if (DEBUG) Log.v(TAG, "internalStop:state=$state")
		if (state == STATE_RECORDING) {
			state = STATE_INITIALIZED
		}
		checkStopSelf()
	}

	/**
	 * エンコード済みのフレームデータを書き出す
	 * mSyncをロックしていないので必要に応じでロックすること
	 * @param reaper
	 * @param buffer
	 * @param info
	 * @param ptsUs
	 */
	protected fun onWriteSampleData(
		reaper: MediaReaper,
		buffer: ByteBuffer, info: MediaCodec.BufferInfo,
		ptsUs: Long) {
//		if (DEBUG) Log.v(TAG, "onWriteSampleData:");
		var muxer = mLock.withLock {
			mMuxer
		}
		var i = 0
		while (isRecording && (i < 100) && ((muxer == null) || !muxer.isStarted())) {
			ThreadUtils.NoThrowSleep(10L)
			muxer = mLock.withLock {
				mMuxer
			}
			i++
		}
		if (muxer != null) {
			when (reaper.reaperType()) {
				MediaReaper.REAPER_VIDEO -> muxer.writeSampleData(mVideoTrackIx, buffer, info)
				MediaReaper.REAPER_AUDIO -> muxer.writeSampleData(mAudioTrackIx, buffer, info)
				else -> if (DEBUG) Log.v(TAG, "onWriteSampleData:unexpected reaper type")
			}
		} else if (isRecording) {
			if (DEBUG) Log.v(TAG, ("onWriteSampleData:muxer is not set yet,state=$state,reaperType=${reaper.reaperType()}"))
			if (!mIsBind) {
				stop()
			}
		}
	}

	protected fun onError(t: Throwable?) {
		Log.w(TAG, t)
		stop()
	}

	//--------------------------------------------------------------------------------
	/**
	 * MediaReaperからのコールバックリスナーの実装
	 */
	private val mReaperListener = object : ReaperListener {
		override fun writeSampleData(
			reaper: MediaReaper,
			byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo
		) {
//			if (DEBUG) Log.v(TAG, "writeSampleData:");
			try {
				val ptsUs: Long = inputPTSUs
				onWriteSampleData(reaper, byteBuf, bufferInfo, ptsUs)
			} catch (e: Exception) {
				this@RecordingService.onError(e)
			}
		}

		override fun onOutputFormatChanged(
			reaper: MediaReaper,
			format: MediaFormat
		) {
			if (DEBUG) Log.v(TAG, "onOutputFormatChanged:$format")
			if (DEBUG) MediaCodecUtils.dump(format)
			when (reaper.reaperType()) {
				MediaReaper.REAPER_VIDEO -> mVideoFormat = format
				MediaReaper.REAPER_AUDIO -> mAudioFormat = format
			}
			if (((!mUseVideo || (mVideoFormat != null))
					&& (!mUseAudio || (mAudioFormat != null)))
			) {
				// 映像と音声のMediaFormatがそろった
				state = STATE_READY
			}
		}

		override fun onStop(reaper: MediaReaper) {
			if (DEBUG) Log.v(TAG, "onStop:")
			mLock.withLock {
				releaseEncoder()
			}
		}

		override fun onError(reaper: MediaReaper, t: Throwable) {
			this@RecordingService.onError(t)
		}
	}

	//--------------------------------------------------------------------------------
	/**
	 * AudioSampleからのコールバックリスナー
	 */
	private val mAudioSamplerCallback = object : AudioSamplerCallback {
		override fun onData(buffer: ByteBuffer, presentationTimeUs: Long) {
			encodeAudio(buffer, presentationTimeUs)
		}

		override fun onError(t: Throwable) {
			this@RecordingService.onError(t)
		}
	}

	/**
	 * 音声データをエンコード
	 * 既に終了しているか終了指示が出てれば何もしない
	 * @param buffer
	 * @param presentationTimeUs
	 */
	private fun encodeAudio(
		buffer: ByteBuffer,
		presentationTimeUs: Long
	) {
		val reaper: AudioReaper?
		val encoder: MediaCodec?
		mLock.withLock {
			reaper = mAudioReaper
			encoder = mAudioEncoder
		}
		if (!isRecording || (reaper == null) || (encoder == null)) {
			// 既に終了しているか終了指示が出てれば何もしない
			if (DEBUG) Log.d(TAG, ("encodeAudio:isRunning=" + isRunning
					+ ",reaper=" + reaper + ",encoder=" + encoder))
			return
		}
		if (buffer.remaining() > 0) {
			// 音声データを受け取った時はエンコーダーへ書き込む
			try {
				encode(encoder, buffer, presentationTimeUs)
				reaper.frameAvailableSoon()
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
				// ignore
			}
		} else if (DEBUG) {
			Log.v(TAG, "encodeAudio:zero length")
		}
	}

	/**
	 * バイト配列をエンコードする場合
	 * @param buffer
	 * @param presentationTimeUs ［マイクロ秒］
	 */
	@Suppress("DEPRECATION")
	private fun encode(
		encoder: MediaCodec,
		buffer: ByteBuffer?, presentationTimeUs: Long
	) {
		if (BuildCheck.isLollipop()) {
			encodeV21(encoder, buffer, presentationTimeUs)
		} else {
			val length = buffer?.remaining() ?: 0
			val inputBuffers = encoder.inputBuffers
			while (isRecording && !mIsEos) {
				val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
				if (inputBufferIndex >= 0) {
					val inputBuffer = inputBuffers[inputBufferIndex]
					inputBuffer.clear()
					if (buffer != null) {
						inputBuffer.put(buffer)
					}
					//	            	if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
					if (length <= 0) {
						// エンコード要求サイズが0の時はEOSを送信
						mIsEos = true
//		            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
						encoder.queueInputBuffer(
							inputBufferIndex, 0, 0,
							presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
						)
					} else {
						encoder.queueInputBuffer(
							inputBufferIndex, 0, length,
							presentationTimeUs, 0
						)
					}
					break
//				} else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
					// 送れるようになるまでループする
					// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
				}
			}
		}
	}

	/**
	 * バイト配列をエンコードする場合(API21/Android5以降)
	 * @param buffer
	 * @param presentationTimeUs ［マイクロ秒］
	 */
	@SuppressLint("NewApi")
	private fun encodeV21(
		encoder: MediaCodec,
		buffer: ByteBuffer?, presentationTimeUs: Long) {
		val length = buffer?.remaining() ?: 0
		while (isRecording && !mIsEos) {
			val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
			if (inputBufferIndex >= 0) {
				val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
				inputBuffer!!.clear()
				if (buffer != null) {
					inputBuffer.put(buffer)
				}
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
				if (length <= 0) {
					// エンコード要求サイズが0の時はEOSを送信
					mIsEos = true
//	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
					encoder.queueInputBuffer(
						inputBufferIndex, 0, 0,
						presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
					)
				} else {
					encoder.queueInputBuffer(
						inputBufferIndex, 0, length,
						presentationTimeUs, 0
					)
				}
				break
//			} else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// 送れるようになるまでループする
				// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
			}
		}
	}

	/**
	 * 指定したMediaCodecエンコーダーへEOSを送る(音声エンコーダー用)
	 * @param encoder
	 */
	private fun signalEndOfInputStream(encoder: MediaCodec) {
		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=$encoder")
		// MediaCodec#signalEndOfInputStreamはBUFFER_FLAG_END_OF_STREAMフラグを付けて
		// 空のバッファをセットするのと等価である
		// ・・・らしいので空バッファを送る。encode内でBUFFER_FLAG_END_OF_STREAMを付けてセットする
		encode(encoder, null, inputPTSUs)
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = RecordingService::class.java.simpleName

		/**
		 * MediaStoreOutputStreamを使って出力するかどうか(Android8以降のみ有効)
		 */
		private const val USE_MEDIASTORE_OUTPUT_STREAM = false
		private val NOTIFICATION = R.string.notification_service

		@DrawableRes
		private val NOTIFICATION_ICON_ID =
			if (BuildCheck.isAPI21()) R.drawable.ic_recording_service else R.mipmap.ic_launcher
		private const val TIMEOUT_MS = 10L
		private const val TIMEOUT_USEC = TIMEOUT_MS * 1000L // 10ミリ秒

		// ステート定数, XXX 継承クラスは100以降を使う
		const val STATE_UNINITIALIZED = -1
		const val STATE_INITIALIZED = 0
		const val STATE_PREPARING = 1
		const val STATE_PREPARED = 2
		const val STATE_READY = 3
		const val STATE_RECORDING = 4
		const val STATE_SCAN_FILE = 6
		const val STATE_RELEASING = 9
	}
}
