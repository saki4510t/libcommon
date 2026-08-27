package com.serenegiant.media
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.os.Parcel
import android.os.Parcelable
import com.serenegiant.math.Fraction
import com.serenegiant.system.BuildCheck
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * 従来はプレビュー解像度=動画の解像度の設定用に使用していたが
 * 今後は動画解像度用のみに使用してプレビュー解像度はDeviceSettingから取得する
 */
class VideoConfig : Parcelable, Cloneable {
	/**
	 * BPP(Bits Per Pixel)
	 * (0.050/0.075/0.100/0.125/0.150/0.175/0.200/0.225/0.25)
	 */
	private var mBPP: Float

	/**
	 * I-frame(単独で圧縮された単独再生可能な一番劣化の少ないキーフレーム)間の秒数@30fps
	 */
	private var mIframeIntervalsS: Float

	/**
	 * I-Frameの間隔 300 = 30fpsの時に10秒間隔 = 300フレームに1回
	 */
	private var mNumFramesBetweenIframeOn30fps: Float

	/**
	 * エンコード時のFPS
	 */
	private var mCaptureFps: Int

	/**
	 * 最大録画時間(ミリ秒), 負数=制限なし
	 */
	private var mMaxDuration: Long

	/**
	 * trueならMediaMuxerを使う、 falseならVideoMuxerを使う
	 * ・・・VideoMuxerを使ってnative側での最大録画時間チェックを有効にするため常にfalse
	 */
	private var mUseMediaMuxer: Boolean

	/**
	 * SurfaceEncoderを使って動画キャプチャをするかどうか
	 */
	private var mUseSurfaceCapture: Boolean

	/**
	 * コンストラクタ
	 * @param src デフォルトはDEFAULT_CONFIG
	 */
	@JvmOverloads
	constructor(src: VideoConfig = DEFAULT_CONFIG) {
		mBPP = src.mBPP
		mIframeIntervalsS = src.mIframeIntervalsS
		mNumFramesBetweenIframeOn30fps = src.mNumFramesBetweenIframeOn30fps
		mCaptureFps = src.mCaptureFps
		mMaxDuration = src.mMaxDuration
		mUseMediaMuxer = src.mUseMediaMuxer
		mUseSurfaceCapture = src.mUseSurfaceCapture
	}

	/**
	 * DEFAULT_CONFIG生成用コンストラクタ
	 * @param bpp
	 * @param iframeIntervalsS
	 * @param numFramesBetweenIframeOn30fps
	 * @param captureFps
	 * @param maxDuration
	 * @param useMediaMuxer
	 * @param useSurfaceCapture
	 */
	private constructor(
		bpp: Float, iframeIntervalsS: Float,
		numFramesBetweenIframeOn30fps: Float, captureFps: Int,
		maxDuration: Long,
		useMediaMuxer: Boolean, useSurfaceCapture: Boolean
	) {
		this.mBPP = bpp
		this.mIframeIntervalsS = iframeIntervalsS
		this.mNumFramesBetweenIframeOn30fps = numFramesBetweenIframeOn30fps
		this.mCaptureFps = captureFps
		this.mMaxDuration = maxDuration
		this.mUseMediaMuxer = useMediaMuxer
		this.mUseSurfaceCapture = useSurfaceCapture
	}

	/**
	 * Parcelable用のコンストラクタ
	 * Parcelable関係の実装
	 * @param src
	 */
	constructor(src: Parcel) {
		mBPP = src.readFloat()
		mIframeIntervalsS = src.readFloat()
		mNumFramesBetweenIframeOn30fps = src.readFloat()
		mCaptureFps = src.readInt()
		mMaxDuration = src.readLong()
		mUseMediaMuxer = src.readByte().toInt() != 0
		mUseSurfaceCapture = src.readByte().toInt() != 0
	}

	@Throws(CloneNotSupportedException::class)
	public override fun clone(): VideoConfig {
		return super.clone() as VideoConfig
	}

	/**
	 * セッター
	 * @param src
	 * @return
	 */
	fun set(src: VideoConfig): VideoConfig {
		if (src != this) {
			mBPP = src.mBPP
			mIframeIntervalsS = src.mIframeIntervalsS
			mNumFramesBetweenIframeOn30fps = src.mNumFramesBetweenIframeOn30fps
			mCaptureFps = src.mCaptureFps
			mMaxDuration = src.mMaxDuration
			mUseMediaMuxer = src.mUseMediaMuxer
			mUseSurfaceCapture = src.mUseSurfaceCapture
		}

		return this
	}

	/**
	 * このVideoConfigインスタンスの値をDEFAULT_CONFIGの値に戻す
	 * @return
	 */
	fun reset(): VideoConfig {
		return set(DEFAULT_CONFIG)
	}

	/**
	 * 最大録画時間(ミリ秒), 負数=制限なし
	 * @return
	 */
	fun maxDuration(): Long {
		return mMaxDuration
	}

	/**
	 * 最大録画時間(ミリ秒), 負数=制限なし
	 * @param duration
	 * @return
	 */
	fun setMaxDuration(duration: Long): VideoConfig {
		mMaxDuration = duration

		return this
	}

	/**
	 * MediaMuxerを使うかどうかを取得
	 * @return　API<18なら常時false
	 */
	fun useMediaMuxer(): Boolean {
		return mUseMediaMuxer
	}

	/**
	 * MediaMuxerを使うかどうかをセット、ただしAPI>=18のみ有効
	 * @param use
	 * @return
	 */
	fun setUseMediaMuxer(use: Boolean): VideoConfig {
		mUseMediaMuxer = use and BuildCheck.isAPI18()

		return this
	}

	/**
	 * Surface経由での動画エンコードを使うかどうかを取得
	 * @return　API<18なら常時false
	 */
	fun useSurfaceCapture(): Boolean {
		return mUseSurfaceCapture
	}

	/**
	 * Surface経由での動画エンコードを使うかどうかをセット、ただしAPI>=18のみ
	 * @param use
	 * @return
	 */
	fun setUseSurfaceCapture(use: Boolean): VideoConfig {
		mUseSurfaceCapture = use and BuildCheck.isAPI18()
		return this
	}

	/**
	 * エンコード時のFPSを設定
	 * @param fps
	 * @return
	 */
	fun setCaptureFps(fps: Int): VideoConfig {
		mCaptureFps = if (fps > FPS_MAX) FPS_MAX else max(fps.toDouble(), FPS_MIN.toDouble()).toInt()
		return this
	}

	/**
	 * エンコード時のFPSを取得
	 * @return
	 */
	fun captureFps(): Int {
		return if (mCaptureFps > FPS_MAX) FPS_MAX
			else max(mCaptureFps.toDouble(), FPS_MIN.toDouble())
				.toInt()
	}

	/**
	 * エンコード時のFPSをFractionとして取得
	 * @return
	 */
	val captureFps: Fraction
		get() = Fraction(captureFps(), 1)

	/**
	 * I-Frameの間隔(秒)@30fpsをセット
	 * @param iFrameIntervalSecs
	 * @return
	 */
	fun setIFrameIntervals(iFrameIntervalSecs: Float): VideoConfig {
		mIframeIntervalsS = iFrameIntervalSecs
		mNumFramesBetweenIframeOn30fps = mIframeIntervalsS * 30f

		return this
	}

	/**
	 * I-Frameの間隔(秒)@30fpsを取得
	 * @return
	 */
	fun iFrameIntervals(): Int {
		return mIframeIntervalsS.toInt()
	}

	/**
	 * I-Frameの間隔(秒)@30fpsを取得
	 * @return
	 */
	val iFrameIntervals: Fraction
		get() = Fraction(mIframeIntervalsS.toDouble())

	/**
	 * エンコード時のFPSにおけるI-Frame間隔を取得
	 * @param maxIFrameIntervals デフォルトはIFRAME_MAX
	 * @return
	 */
	@JvmOverloads
	fun calcIFrameIntervals(maxIFrameIntervals: Float = IFRAME_MAX): Int {
		val fps = captureFps()
		val iFrameIntervals = try {
			if (fps <= IFRAME_MIN) IFRAME_MIN
			else ceil((mNumFramesBetweenIframeOn30fps / fps).toDouble()).toFloat()
		} catch (e: Exception) {
			mIframeIntervalsS
		}.coerceAtMost(maxIFrameIntervals)
			.coerceAtLeast(IFRAME_MIN)
	//	Log.d(TAG, "iframe_intervals=$iFrameIntervals")
		return iFrameIntervals.toInt()
	}

	/**
	 * ビットレートを計算(bps)
	 * @param width 映像幅(ピクセル)
	 * @param height 映像高さ(ピクセル)
	 * @param frameRate フレームレート、デフォルトはcaptureFps()
	 * @param bpp BPP(Bit Per Pixel), デフォルトはbpp()
	 * @param maxBitrate 最大ビットレート、デフォルトはBITRATE_MAX
	 * @return
	 */
	@JvmOverloads
	fun getBitrate(
		width: Int, height: Int,
		frameRate: Int = captureFps(),
		bpp: Float = bpp(),
		maxBitrate: Int = BITRATE_MAX
	): Int {
		return calcBitrate(width, height, frameRate, bpp, maxBitrate)
	}

	/**
	 * 現在の設定で生成される概略ファイルサイズを計算[バイト/分]
	 * 音声データ分は含まない
	 * @param width 映像幅(ピクセル)
	 * @param height 映像高さ(ピクセル)
	 * @param frameRate フレームレート、デフォルトはcaptureFps()
	 * @param bpp BPP(Bit Per Pixel), デフォルトはbpp()
	 * @param maxBitrate 最大ビットレート、デフォルトはBITRATE_MAX
	 * @return
	 */
	@JvmOverloads
	fun getSizeRate(
		width: Int, height: Int,
		frameRate: Int = captureFps(),
		bpp: Float = bpp(),
		maxBitrate: Int = BITRATE_MAX
	): Long {
		val bitrate = getBitrate(width, height, frameRate, bpp, maxBitrate)
		return bitrate * 60L / 8L // bits/sec -> bytes/min
	}

	/**
	 * 現在の設定で生成される分割録画用の概略最大ファイルサイズを計算[バイト/分]
	 * 最大録画時間優先にするならmaxDurationMinutesに最大録画時間をセットする
	 * ファイルサイズ優先にするならmaxDurationMinutesに1以下の値をセットする
	 * 音声データ分は含まない
	 * @param width 映像幅(ピクセル)
	 * @param height 映像高さ(ピクセル)
	 * @param maxDurationMinutes 最大録画時間、1以下なら最大録画時間制限しない, デフォルトは60分
	 * @param maxBytes 最大ファイルサイズ(バイト), デフォルトはMAX_FILE_BYTES
	 * @param frameRate フレームレート、デフォルトはcaptureFps()
	 * @param bpp BPP(Bit Per Pixel), デフォルトはbpp()
	 * @param maxBitrate 最大ビットレート、デフォルトはBITRATE_MAX
	 * @return
	 */
	@JvmOverloads
	fun getSplitRecordingBytes(
		width: Int, height: Int,
		maxDurationMinutes: Int = 60,
		maxBytes: Long = MAX_FILE_BYTES,
		frameRate: Int = captureFps(),
		bpp: Float = mBPP,
		maxBitrate: Int = BITRATE_MAX
	): Long {
		var result = 0L
		// 1分辺りの概算ファイルサイズ(バイト)
		val bytesPerMinute = getSizeRate(width, height, frameRate, bpp, maxBitrate)
		if (maxDurationMinutes > 1) {
			// 最大録画時間から1分刻みでmaxBytes未満になる値を探す
			for (duration in maxDurationMinutes downTo 1) {
				val bytes = bytesPerMinute * duration
				if (bytes < maxBytes) {
					result = bytes
					break
				}
			}
		} else {
			// 1分刻みの値にする
			result = (maxBytes / bytesPerMinute) * bytesPerMinute
		}

		return result
	}

	/**
	 * BPPをセット
	 * @param width 映像幅(ピクセル)
	 * @param height 映像高さ(ピクセル)
	 * @param bitrate ビットレート
	 * @throws IllegalArgumentException
	 */
	@Throws(IllegalArgumentException::class)
	fun setBPP(width: Int, height: Int, bitrate: Int): VideoConfig {
		setBPP(calcBPP(width, height, captureFps(), bitrate))

		return this
	}

	/**
	 * BPPをセット
	 * @param bpp [BPP_MIN==0.01f, BPP_MAX==0.3f]
	 * @throws IllegalArgumentException
	 */
	@Throws(IllegalArgumentException::class)
	fun setBPP(bpp: Float): VideoConfig {
		require(!((bpp < BPP_MIN) || (bpp > BPP_MAX))) { "bpp should be within [BPP_MIN, BPP_MAX]" }
		mBPP = bpp

		return this
	}

	/**
	 * 現在のBPP設定を取得
	 * @return
	 */
	fun bpp(): Float {
		return mBPP
	}

	/**
	 * ビットレートを計算(bps)
	 * @param width 映像幅(ピクセル)
	 * @param height 映像高さ(ピクセル)
	 * @param frameRate 録画フレームレート
	 * @param bpp BPP(Bit Per Pixel)
	 * @param maxBitrate 最大フレームレート
	 * @return Intなので2048Mbps未満(2047.99...Mbps)
	 */
	private fun calcBitrate(
		width: Int, height: Int,
		frameRate: Int, bpp: Float, maxBitrate: Int
	): Int {
		var r = (floor((bpp * frameRate * width * height / 1000 / 100).toDouble()) * 100).toInt() * 1000
		if (r < BITRATE_MIN) r = BITRATE_MIN
		else if (r > maxBitrate) r = maxBitrate
//		Log.d(TAG, String.format("bitrate=%d[kbps]", r / 1024));
		return r
	}

	override fun toString(): String {
		return "VideoConfig{" +
			"BPP=" + mBPP +
			", mIframeIntervalsS=" + mIframeIntervalsS +
			", mNumFramesBetweenIframeOn30fps=" + mNumFramesBetweenIframeOn30fps +
			", mCaptureFps=" + mCaptureFps +
			", mMaxDuration=" + mMaxDuration +
			", mUseMediaMuxer=" + mUseMediaMuxer +
			", mUseSurfaceCapture=" + mUseSurfaceCapture +
			'}'
	}

	/**
	 * Parcelable関係の実装
	 */
	override fun describeContents(): Int {
		return 0
	}

	/**
	 * Parcelable関係の実装
	 * @param dst
	 * @param flags
	 */
	override fun writeToParcel(dst: Parcel, flags: Int) {
		dst.writeFloat(mBPP)
		dst.writeFloat(mIframeIntervalsS)
		dst.writeFloat(mNumFramesBetweenIframeOn30fps)
		dst.writeInt(mCaptureFps)
		dst.writeLong(mMaxDuration)
		dst.writeByte((if (mUseMediaMuxer) 1 else 0).toByte())
		dst.writeByte((if (mUseSurfaceCapture) 1 else 0).toByte())
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = VideoConfig::class.java.simpleName

		/**
		 * BPP(Bits Per Pixel)の最小値
		 */
		const val BPP_MIN = 0.01f

		/**
		 * BPP(Bits Per Pixel)の最大値
		 */
		const val BPP_MAX = 0.50f

		/**
		 * フレームレートの最小値
		 */
		const val FPS_MIN = 2

		/**
		 * フレームレートの最大値
		 */
		const val FPS_MAX = 121

		/**
		 * I-frame間隔(秒)の最小値
		 */
		const val IFRAME_MIN = 1.0f

		/**
		 * I-frame間隔(秒)の最大値
		 */
		const val IFRAME_MAX = 30.0f

		/**
		 * ビットレートの最小値(bps)
		 */
		const val BITRATE_MIN = 200000

		/**
		 * ビットレートの最大値(bps)
		 */
		const val BITRATE_MAX = 20000000

		/**
		 * FAT32を前提として1つの録画ファイルの最大サイズ
		 * 4GB未満なのでとりあえず4GB - 16MBにする
		 */
		const val MAX_FILE_BYTES = (4096 - 16) * 1024L * 1024L

		/**
		 * デフォルトのVideoConfig
		 */
		val DEFAULT_CONFIG = VideoConfig(
			bpp = 0.25f,
			iframeIntervalsS = 10f,
			numFramesBetweenIframeOn30fps = 10 * 30.0f,
			captureFps = 30,
			maxDuration = 30 * 1000L,  /*30秒*/
			useMediaMuxer = BuildCheck.isAPI18(),
			useSurfaceCapture = BuildCheck.isAPI18()
		)

		/**
		 * BPPを計算
		 * @param width 映像幅(ピクセル)
		 * @param height 映像幅(ピクセル)
		 * @param captureFps 録画時のフレームレート
		 * @param bitrate ビットレート
		 * @return
		 */
		private fun calcBPP(
			width: Int, height: Int,
			captureFps: Int, bitrate: Int
		): Float {
			return bitrate / (captureFps * width * height).toFloat()
		}

		/**
		 * Parcelable関係の実装
		 */
		@JvmField
		val CREATOR = object : Parcelable.Creator<VideoConfig> {
			override fun createFromParcel(src: Parcel): VideoConfig {
				return VideoConfig(src)
			}

			override fun newArray(size: Int): Array<VideoConfig?> {
				return arrayOfNulls(size)
			}
		}
	}
}
