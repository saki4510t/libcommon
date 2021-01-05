package com.serenegiant.media;
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

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 従来はプレビュー解像度=動画の解像度の設定用に使用していたが
 * 今後は動画解像度用のみに使用してプレビュー解像度はDeviceSettingから取得する
 */
public class VideoConfig implements Parcelable, Cloneable {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = VideoConfig.class.getSimpleName();

	public static final float BPP_MIN = 0.01f;
	public static float BPP_MAX = 0.30f;

	public static final int FPS_MIN = 2;
	public static final int FPS_MAX = 121;

	private static final int IFRAME_MIN = 1;
	private static final int IFRAME_MAX = 30;

	public static final VideoConfig DEFAULT_CONFIG = new VideoConfig(
		0.25f, 10, 10 * 30.0f,
		15, 30 * 1000L,
		(Build.VERSION.SDK_INT >= 18), (Build.VERSION.SDK_INT >= 18)
	);

	/**
	 * DEFAULT_CONFIGを引き継いだ新しいVideoConfigオブジェクトを生成
	 * @return
	 */
	@Deprecated
	public static VideoConfig createDefault() {
		return new VideoConfig(DEFAULT_CONFIG);
	}

	/**
	 * BPP(Bits Per Pixel)
	 * (0.050/0.075/0.100/0.125/0.150/0.175/0.200/0.225/0.25)
	 */
	private float BPP;

	/**
	 * I-frame(単独で圧縮された単独再生可能な一番劣化の少ないキーフレーム)間の秒数@30fps
	 */
    private float mIframeIntervalsS;
	/**
	 * I-Frameの間隔 300 = 30fpsの時に10秒間隔 = 300フレームに1回
	 */
	private float mNumFramesBetweenIframeOn30fps;

	/**
	 * エンコード時のFPS
	 */
	private int mCaptureFps;
	/**
	 * 最大録画時間[ミリ秒], 負数=制限なし
	 */
	private long mMaxDuration;

	/**
	 * trueならMediaMuxerを使う、 falseならVideoMuxerを使う
	 * ・・・VideoMuxerを使ってnative側での最大録画時間チェックを有効にするため常にfalse
	 */
	private boolean mUseMediaMuxer;

	/**
	 * SurfaceEncoderを使って動画キャプチャをするかどうか
	 */
	private boolean mUseSurfaceCapture;

	/**
	 * デフォルトコンストラクタ
	 * 生成時のDEFAULT_CONFIGの設定値を引き継ぐ
	 */
	public VideoConfig() {
		this(DEFAULT_CONFIG);
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	public VideoConfig(@NonNull final VideoConfig src) {
		BPP = src.BPP;
		mIframeIntervalsS = src.mIframeIntervalsS;
		mNumFramesBetweenIframeOn30fps = src.mNumFramesBetweenIframeOn30fps;
		mCaptureFps = src.mCaptureFps;
		mMaxDuration = src.mMaxDuration;
		mUseMediaMuxer = src.mUseMediaMuxer;
		mUseSurfaceCapture = src.mUseSurfaceCapture;
	}

	/**
	 * DEFAULT_CONFIG生成用コンストラクタ
	 * @param BPP
	 * @param mIframeIntervalsS
	 * @param mNumFramesBetweenIframeOn30fps
	 * @param mCaptureFps
	 * @param mMaxDuration
	 * @param mUseMediaMuxer
	 * @param mUseSurfaceCapture
	 */
	private VideoConfig(final float BPP, final float mIframeIntervalsS,
		final float mNumFramesBetweenIframeOn30fps, final int mCaptureFps,
		final long mMaxDuration,
		final boolean mUseMediaMuxer, final boolean mUseSurfaceCapture) {

		this.BPP = BPP;
		this.mIframeIntervalsS = mIframeIntervalsS;
		this.mNumFramesBetweenIframeOn30fps = mNumFramesBetweenIframeOn30fps;
		this.mCaptureFps = mCaptureFps;
		this.mMaxDuration = mMaxDuration;
		this.mUseMediaMuxer = mUseMediaMuxer;
		this.mUseSurfaceCapture = mUseSurfaceCapture;
	}

	@NonNull
	@Override
	public VideoConfig clone() throws CloneNotSupportedException {
		return (VideoConfig) super.clone();
	}

	/**
	 * セッター
	 * @param src
	 * @return
	 */
	public VideoConfig set(@NonNull final VideoConfig src) {
		BPP = src.BPP;
		mIframeIntervalsS = src.mIframeIntervalsS;
		mNumFramesBetweenIframeOn30fps = src.mNumFramesBetweenIframeOn30fps;
		mCaptureFps = src.mCaptureFps;
		mMaxDuration = src.mMaxDuration;
		mUseMediaMuxer = src.mUseMediaMuxer;
		mUseSurfaceCapture = src.mUseSurfaceCapture;

		return this;
	}

	/**
	 * 最大録画時間[ミリ秒], 負数=制限なし
	 * @return
	 */
	public long maxDuration() {
		return mMaxDuration;
	}

	/**
	 * 最大録画時間[ミリ秒], 負数=制限なし
	 * @param duration
	 * @return
	 */
	@NonNull
	public VideoConfig setMaxDuration(final long duration) {
		mMaxDuration = duration;

		return this;
	}

	/**
	 * MediaMuxerを使うかどうかを取得
	 * @return　API<18なら常時false
	 */
	public boolean useMediaMuxer() {
		return mUseMediaMuxer;
	}

	/**
	 * MediaMuxerを使うかどうかをセット、ただしAPI>=18のみ
	 * @param use
	 * @return
	 */
	@NonNull
	public VideoConfig setUseMediaMuxer(final boolean use) {
		mUseMediaMuxer = use & (Build.VERSION.SDK_INT >= 18);

		return this;
	}

	/**
	 * Surface経由での動画エンコードを使うかどうかを取得
	 * @return　API<18なら常時false
	 */
	public boolean useSurfaceCapture() {
		return mUseSurfaceCapture;
	}

	/**
	 * Surface経由での動画エンコードを使うかどうかをセット、ただしAPI>=18のみ
	 * @param use
	 * @return
	 */
	@NonNull
	public VideoConfig setUseSurfaceCapture(final boolean use) {
		mUseSurfaceCapture = use & (Build.VERSION.SDK_INT >= 18);

		return this;
	}

	/**
	 * エンコード時のFPS
	 * @param fps
	 * @return
	 */
	@NonNull
	public VideoConfig setCaptureFps(final int fps) {
		mCaptureFps = fps > FPS_MAX ? FPS_MAX : Math.max(fps, FPS_MIN);

		return this;
	}

	/**
	 * エンコード時のFPS
	 * @return
	 */
	public int captureFps() {
		return mCaptureFps > FPS_MAX ? FPS_MAX : Math.max(mCaptureFps, FPS_MIN);
	}

	/**
	 * I-Frameの間隔[秒]@30fpsをセット
	 * @param iFrameIntervalSecs
	 * @return
	 */
	@NonNull
	public VideoConfig setIFrameIntervals(final float iFrameIntervalSecs) {
		mIframeIntervalsS = iFrameIntervalSecs;
		mNumFramesBetweenIframeOn30fps = mIframeIntervalsS * 30.f;

		return this;
	}

	/**
	 * I-Frameの間隔[秒]@30fpsを取得
	 * @return
	 */
	public int iFrameIntervals() {
		return (int)mIframeIntervalsS;
	}

	/**
	 * エンコード時のFPSにおけるI-Frame間隔を取得
	 * @return
	 */
	public final int calcIFrameIntervals() {
		final int fps = captureFps();
		float iframe;
		try {
			if (fps < 2)
				iframe = IFRAME_MIN;
			else
				iframe = (float)Math.ceil(mNumFramesBetweenIframeOn30fps / fps);
		} catch (final Exception e) {
			iframe = mIframeIntervalsS;
		}
		if ((int)iframe < IFRAME_MIN) iframe = IFRAME_MIN;
		else if ((int)iframe > IFRAME_MAX) iframe = IFRAME_MAX;
//		Log.d(TAG, "iframe_intervals=" + iframe);
		return (int)iframe;
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @return
	 */
	private static int calcBitrate(final int width, final int height,
		final int frameRate, final float bpp) {

		int r = (int)(Math.floor(bpp * frameRate * width * height / 1000 / 100) * 100) * 1000;
		if (r < 200000) r = 200000;
		else if (r > 20000000) r = 20000000;
//		Log.d(TAG, String.format("bitrate=%d[kbps]", r / 1024));
		return r;
	}


	/**
	 * エンコード時のFPSにおけるビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @return
	 */
	public int getBitrate(final int width, final int height) {
		return calcBitrate(width, height, captureFps(), BPP);
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @return
	 */
	public int getBitrate(final int width, final int height, final int frameRate) {
		return calcBitrate(width, height, frameRate, BPP);
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @return
	 */
	public int getBitrate(final int width, final int height,
		final int frameRate, final float bpp) {

		return calcBitrate(width, height, frameRate, bpp);
	}

	/**
	 * BPPを計算
	 * @param width
	 * @param height
	 * @param captureFps
	 * @param bitrate
	 * @return
	 */
	private static float calcBPP(final int width, final int height,
		final int captureFps, final int bitrate) {

		return bitrate / (float)(captureFps * width * height);
	}

	/**
	 * BPPを計算
	 * captureFpsは#getCaptureFpsを使用
	 * @param width
	 * @param height
	 * @param bitrate
	 * @return
	 */
	public float calcBPP(final int width, final int height,
		final int bitrate) {

		return calcBPP(width, height, captureFps(), bitrate);
	}

	/**
	 * BPPをセット
	 * @param width
	 * @param height
	 * @param bitrate
	 * @throws IllegalArgumentException
	 */
	@NonNull
	public VideoConfig setBPP(final int width, final int height, final int bitrate)
		throws IllegalArgumentException {

		setBPP(calcBPP(width, height, bitrate));

		return this;
	}

	/**
	 * BPPをセット
	 * @param bpp [BPP_MIN==0.01f, BPP_MAX]
	 * @throws IllegalArgumentException
	 */
	@NonNull
	public VideoConfig setBPP(final float bpp) throws IllegalArgumentException {
		if ((bpp < BPP_MIN) || (bpp > BPP_MAX)) {
			throw new IllegalArgumentException("bpp should be within [BPP_MIN, BPP_MAX]");
		}
		BPP = bpp;

		return this;
	}

	/**
	 * 現在のBPP設定を取得
	 * @return
	 */
	public float bpp() {
		return BPP;
	}

	/**
	 * 現在の設定で生成される概略ファイルサイズを計算[バイト/分]
	 * 音声データ分は含まない
	 * @param width
	 * @param height
	 * @return
	 */
	public int getSizeRate(final int width, final int height) {
		final int bitrate = getBitrate(width, height);
		return bitrate * 60 / 8;	// bits/sec -> bytes/min
	}

	@NonNull
	@Override
	public String toString() {
		return "VideoConfig{" +
			"BPP=" + BPP +
			", mIframeIntervalsS=" + mIframeIntervalsS +
			", mNumFramesBetweenIframeOn30fps=" + mNumFramesBetweenIframeOn30fps +
			", mCaptureFps=" + mCaptureFps +
			", mMaxDuration=" + mMaxDuration +
			", mUseMediaMuxer=" + mUseMediaMuxer +
			", mUseSurfaceCapture=" + mUseSurfaceCapture +
			'}';
	}
//================================================================================
// Parcelable関係の実装

	/**
	 * Parcelable用のコンストラクタ
	 * @param in
	 */
	protected VideoConfig(final Parcel in) {
		BPP = in.readFloat();
		mIframeIntervalsS = in.readFloat();
		mNumFramesBetweenIframeOn30fps = in.readFloat();
		mCaptureFps = in.readInt();
		mMaxDuration = in.readLong();
		mUseMediaMuxer = in.readByte() != 0;
		mUseSurfaceCapture = in.readByte() != 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeFloat(BPP);
		dest.writeFloat(mIframeIntervalsS);
		dest.writeFloat(mNumFramesBetweenIframeOn30fps);
		dest.writeInt(mCaptureFps);
		dest.writeLong(mMaxDuration);
		dest.writeByte((byte) (mUseMediaMuxer ? 1 : 0));
		dest.writeByte((byte) (mUseSurfaceCapture ? 1 : 0));
	}

	public static final Creator<VideoConfig> CREATOR = new Creator<VideoConfig>() {
		@Override
		public VideoConfig createFromParcel(Parcel in) {
			return new VideoConfig(in);
		}

		@Override
		public VideoConfig[] newArray(int size) {
			return new VideoConfig[size];
		}
	};
}
