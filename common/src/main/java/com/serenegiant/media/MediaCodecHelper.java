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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.serenegiant.utils.BufferHelper;
import com.serenegiant.system.BuildCheck;

import java.util.ArrayList;
import java.util.List;

/**
 * XXX MediaCodecUtilsへ集約したので今後はMediaCodecUtilsを使うこと
 */
@Deprecated
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaCodecHelper {
	@SuppressWarnings("deprecation")
	private static final String TAG = MediaCodecHelper.class.getSimpleName();

	@Deprecated
	public static final String MIME_VIDEO_AVC = "video/avc";		// h.264
	@Deprecated
	public static final String MIME_AUDIO_AAC = "audio/mp4a-latm";	// AAC
	@Deprecated
	@SuppressLint("InlinedApi")
	public static final int BUFFER_FLAG_KEY_FRAME
		= BuildCheck.isLollipop() ? MediaCodec.BUFFER_FLAG_KEY_FRAME : MediaCodec.BUFFER_FLAG_SYNC_FRAME;
	/** codec specific dataのスタートマーカー = AnnexBのスタートマーカーと同じ */
	@Deprecated
	public static final byte[] START_MARKER = BufferHelper.ANNEXB_START_MARK;


	/**
	 * 指定したMIMEに一致する最初の動画エンコード用コーデックを選択する
	 * もし使用可能なのがなければnullを返す
	 * @param mimeType
	 */
	@Deprecated
	@Nullable
	public static MediaCodecInfo selectVideoCodec(final String mimeType) {
		//noinspection deprecation
		return selectVideoEncoder(mimeType);
	}

	/**
	 * 指定したMIMEで使用可能がcodecの一覧の中から先頭のものを取得する
	 * もし使用可能なのがなければnullを返す
	 * @param mimeType
	 */
	@Deprecated
	@Nullable
	public static MediaCodecInfo selectVideoEncoder(final String mimeType) {
		// コーデックの一覧を取得
		//noinspection deprecation
		final int numCodecs = getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
			final MediaCodecInfo codecInfo = getCodecInfoAt(i);

			if (!codecInfo.isEncoder()) {	// エンコーダーでない(=デコーダー)はスキップする
				continue;
			}
			// エンコーダーの一覧からMIMEが一致してカラーフォーマットが使用可能なものを選択する
/*			// こっちの方法で選択すると
			// W/OMXCodec(20608): Failed to set standard component role 'video_encoder.avc'.
			// って表示されて、OMX.Nvidia.mp4.encoder
			// が選択される
			final MediaCodecInfo.CodecCapabilities caps;
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			try {
				caps = getCodecCapabilities(codecInfo, mimeType);
			} finally {
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			}
			final int[] colorFormats = caps.colorFormats;
			final int n = colorFormats != null ? colorFormats.length : 0;
			int colorFormat;
			for (int j = 0; j < n; j++) {
				colorFormat = colorFormats[j];
				if (isRecognizedVideoFormat(colorFormat)) {
					mColorFormat = colorFormat;
					return codecInfo;
				}
			} */
// こっちで選択すると、xxx.h264.encoderが選択される
			final String[] types = codecInfo.getSupportedTypes();
			final int n = types.length;
			int format;
			for (int j = 0; j < n; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
//                	if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
					//noinspection deprecation
					format = selectColorFormat(codecInfo, mimeType);
					if (format > 0) {
						return codecInfo;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * 指定したmimeに対応するビデオコーデックのエンコーダー一覧を取得する
	 * @param mimeType
	 * @return
	 */
	@Deprecated
	@NonNull
	public static List<MediaCodecInfo> getVideoEncoderInfos(final String mimeType) {
		final List<MediaCodecInfo> result = new ArrayList<>();
		// コーデックの一覧を取得
		//noinspection deprecation
		final int numCodecs = getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
			final MediaCodecInfo codecInfo = getCodecInfoAt(i);

			if (!codecInfo.isEncoder()) {	// エンコーダーでない(=デコーダー)はスキップする
				continue;
			}
			// エンコーダーの一覧からMIMEが一致してカラーフォーマットが使用可能なものを選択する
/*			// こっちの方法で選択すると
			// W/OMXCodec(20608): Failed to set standard component role 'video_encoder.avc'.
			// って表示されて、OMX.Nvidia.mp4.encoder
			// が選択される
			final MediaCodecInfo.CodecCapabilities caps;
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			try {
				caps = getCodecCapabilities(codecInfo, mimeType);
			} finally {
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			}
			final int[] colorFormats = caps.colorFormats;
			final int n = colorFormats != null ? colorFormats.length : 0;
			int colorFormat;
			for (int j = 0; j < n; j++) {
				colorFormat = colorFormats[j];
				if (isRecognizedVideoFormat(colorFormat)) {
					result.add(codecInfo);
				}
			} */
// こっちで選択すると、xxx.h264.encoderが選択される
			final String[] types = codecInfo.getSupportedTypes();
			final int n = types.length;
			int format;
			for (int j = 0; j < n; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
//                	if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
					//noinspection deprecation
					format = selectColorFormat(codecInfo, mimeType);
					if (format > 0) {
						result.add(codecInfo);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 使用可能なカラーフォーマットを設定
	 */
	@Deprecated
	public static int[] recognizedFormats;
	static {
		//noinspection deprecation
		recognizedFormats = new int[] {
			MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
			MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
			MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
		};
	}

	/**
	 * 指定したカラーフォーマットをこのEncoderで使用可能かどうかを返す
	 * @param colorFormat
	 * @return
	 */
	@Deprecated
    public static final boolean isRecognizedVideoFormat(final int colorFormat) {
		//noinspection deprecation
    	final int n = recognizedFormats != null ? recognizedFormats.length : 0;
    	for (int i = 0; i < n; i++) {
			//noinspection deprecation
    		if (recognizedFormats[i] == colorFormat) {
    			return true;
    		}
    	}
    	return false;
    }

	/**
	 * codecがサポートしているカラーフォーマットの中から最初に使用可能なものを選択して返す
	 * 使用可能なカラーフォーマットはrecognizedFormatsに設定する(Encoderで設定しているのはYUV420PlanarかYUV420SemiPlanar)
	 * 使用可能なものが無ければ0を返す
	 */
	@Deprecated
	public static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
		int result = 0;
		//noinspection deprecation
		final MediaCodecInfo.CodecCapabilities capabilities = getCodecCapabilities(codecInfo, mimeType);
		final int[] colorFormats = capabilities.colorFormats;
		final int n = colorFormats.length;
		int colorFormat;
		for (int i = 0; i < n; i++) {
			colorFormat = colorFormats[i];
			//noinspection deprecation
			if (isRecognizedVideoFormat(colorFormat)) {
				result = colorFormat;
				break;	// if (!DEBUG) break;
			}
		}
        return result;
    }

	/**
	 * コーデックの一覧をログに出力する
	 */
	@Deprecated
	public static final void dumpVideoCodecEncoders() {
		//noinspection deprecation
		dumpEncoders();
	}

	/**
	 * エンコード用コーデックの一覧をログに出力する
	 */
	@Deprecated
	public static final void dumpEncoders() {
    	// コーデックの一覧を取得
		//noinspection deprecation
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);	// API >= 16

            if (!codecInfo.isEncoder()) {	// エンコーダーでない(デコーダー)はとばす
                continue;
            }
			// コーデックの一覧を出力する
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
            	Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME:" + types[j]);
            	// カラーフォーマットを出力する
				//noinspection deprecation
            	selectColorFormat(codecInfo, types[j]);
            }
        }
    }

	/**
	 * デコード用コーデックの一覧をログに出力する
	 */
	@Deprecated
	public static final void dumpDecoders() {
    	// コーデックの一覧を取得
		//noinspection deprecation
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);	// API >= 16

            if (codecInfo.isEncoder()) {	// エンコーダーはとばす
                continue;
            }
            // コーデックの一覧を出力する
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
            	Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME:" + types[j]);
            	// カラーフォーマットを出力する
				//noinspection deprecation
            	selectColorFormat(codecInfo, types[j]);
            }
        }
    }

    /**
     * Returns true if the specified color format is semi-planar YUV.  Throws an exception
     * if the color format is not recognized (e.g. not YUV).
     */
	@Deprecated
    public static final boolean isSemiPlanarYUV(final int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                return false;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
                return true;
            default:
                throw new RuntimeException("unknown format " + colorFormat);
        }
    }

//================================================================================
	/**
	 * 指定したMIMEに一致する最初の音声エンコード用コーデックを選択する
	 * @param mimeType
	 * @return
	 */
	@Deprecated
	@Nullable
	public static final MediaCodecInfo selectAudioCodec(final String mimeType) {
		//noinspection deprecation
		return selectAudioEncoder(mimeType);
	}
	
	/**
	 * 指定したMIMEに一致する最初の音声エンコード用コーデックを選択する
	 * 対応するものがなければnullを返す
	 * @param mimeType
	 * @return
	 */
	@Deprecated
	@Nullable
	public static final MediaCodecInfo selectAudioEncoder(final String mimeType) {
//    	if (DEBUG) Log.v(TAG, "selectAudioCodec:");

 		MediaCodecInfo result = null;
 		// コーデックの一覧を取得
		//noinspection deprecation
		final int numCodecs = getCodecCount();
LOOP:	for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
	     	final MediaCodecInfo codecInfo = getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {	// エンコーダーでない(=デコーダー)はスキップする
				continue;
			}
			final String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
//				if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
				if (types[j].equalsIgnoreCase(mimeType)) {
					result = codecInfo;
					break LOOP;
				}
			}
		}
		return result;
	}

	/**
	 * 指定したmimeに対応する音声コーデックのエンコーダー一覧を取得する
	 * @param mimeType
	 * @return
	 */
	@Deprecated
	@NonNull
	public static List<MediaCodecInfo> getAudioEncoderInfos(final String mimeType) {
		final List<MediaCodecInfo> result = new ArrayList<>();
		
		// コーデックの一覧を取得
		//noinspection deprecation
		final int numCodecs = getCodecCount();
LOOP:	for (int i = 0; i < numCodecs; i++) {
			//noinspection deprecation
			final MediaCodecInfo codecInfo = getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {	// エンコーダーでない(=デコーダー)はスキップする
				continue;
			}
			final String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
//				if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
				if (types[j].equalsIgnoreCase(mimeType)) {
					result.add(codecInfo);
				}
			}
		}
		return result;
	}
//================================================================================
	@Deprecated
	public static final int getCodecCount() {
		return MediaCodecUtils.getCodecCount();
	}

	@Deprecated
	public static final List<MediaCodecInfo> getCodecs() {
		return MediaCodecUtils.getCodecs();
	}

	@Deprecated
	public static final MediaCodecInfo getCodecInfoAt(final int ix) {
		return MediaCodecUtils.getCodecInfoAt(ix);
	}

	@Deprecated
	public static MediaCodecInfo.CodecCapabilities getCodecCapabilities(
		final MediaCodecInfo codecInfo, final String mimeType) {

		return MediaCodecUtils.getCodecCapabilities(codecInfo, mimeType);
	}

	/**
	 * プロファイル・レベルが低ければtrueを返す
	 * @param mimeType
	 * @param info
	 * @return
	 */
	@Deprecated
	public static boolean checkProfileLevel(final String mimeType,
		final MediaCodecInfo info) {

		if (info != null) {
			if (mimeType.equalsIgnoreCase("video/avc")) {
				//noinspection deprecation
				final MediaCodecInfo.CodecCapabilities caps = getCodecCapabilities(info, mimeType);
				final MediaCodecInfo.CodecProfileLevel[] profileLevel = caps.profileLevels;
				for (int j = 0; j < profileLevel.length; j++) {
					if (profileLevel[j].level >= MediaCodecInfo.CodecProfileLevel.AVCLevel5)
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * プロファイルレベルと文字列にする
	 * @param mimeType
	 * @param profileLevel
	 * @return
	 */
	@Deprecated
	public static String getProfileLevelString(final String mimeType,
		final MediaCodecInfo.CodecProfileLevel profileLevel) {

		String result = null;
		if (mimeType.equalsIgnoreCase("video/avc")) {
			switch (profileLevel.profile) {
			// from OMX_VIDEO_AVCPROFILETYPE
			case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline:	// 0x01;
				result = "profile:AVCProfileBaseline"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileMain:		// 0x02;
				result = "profile:AVCProfileMain"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileExtended:	// 0x04;
				result = "profile:AVCProfileExtended"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh:		// 0x08;
				result = "profile:AVCProfileHigh"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10:		// 0x10;
				result = "profile:AVCProfileHigh10"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422:	// 0x20;
				result = "profile:AVCProfileHigh422"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444:	// 0x40;
				result = "profile:AVCProfileHigh444"; break;
			default:
				result = "profile:unknown " + profileLevel.profile; break;
			}
			switch (profileLevel.level) {
			// from OMX_VIDEO_AVCLEVELTYPE
			case MediaCodecInfo.CodecProfileLevel.AVCLevel1:		// 0x01;
				result = result + ",level=AVCLevel1"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel1b:		// 0x02;
				result = result + ",level=AVCLevel1b"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel11:		// 0x04;
				result = result + ",level=AVCLevel11"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel12:		// 0x08;
				result = result + ",level=AVCLevel12"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel13:		// 0x10;
				result = result + ",level=AVCLevel13"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel2:		// 0x20;
				result = result + ",level=AVCLevel2"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel21:		// 0x40;
				result = result + ",level=AVCLevel21"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel22:		// 0x80;
				result = result + ",level=AVCLevel22"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel3:		// 0x100;
				result = result + ",level=AVCLevel3"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel31:		// 0x200;
				result = result + ",level=AVCLevel31"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel32:		// 0x400;
				result = result + ",level=AVCLevel32"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel4:		// 0x800;
				result = result + ",level=AVCLevel4"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel41:		// 0x1000;
				result = result + ",level=AVCLevel41"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel42:		// 0x2000;
				result = result + ",level=AVCLevel42"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel5:		// 0x4000;
				result = result + ",level=AVCLevel5"; break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel51:		// 0x8000;
				result = result + ",level=AVCLevel51"; break;
			default:
				result = result + ",level=unknown " + profileLevel.level; break;
			}
		} else if (mimeType.equalsIgnoreCase("video/h263")) {
			switch (profileLevel.profile) {
			// from OMX_VIDEO_H263PROFILETYPE
			case MediaCodecInfo.CodecProfileLevel.H263ProfileBaseline:				// 0x01;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileH320Coding:			// 0x02;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileBackwardCompatible:	// 0x04;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV2:					// 0x08;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV3:					// 0x10;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileHighCompression:		// 0x20;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileInternet:				// 0x40;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileInterlace:				// 0x80;
			case MediaCodecInfo.CodecProfileLevel.H263ProfileHighLatency:			// 0x100;
			default:
				result = "profile:unknown " + profileLevel.profile; break;
			}
			switch (profileLevel.level) {
			// from OMX_VIDEO_H263LEVELTYPE
			case MediaCodecInfo.CodecProfileLevel.H263Level10:					// 0x01;
			case MediaCodecInfo.CodecProfileLevel.H263Level20:					// 0x02;
			case MediaCodecInfo.CodecProfileLevel.H263Level30:					// 0x04;
			case MediaCodecInfo.CodecProfileLevel.H263Level40:					// 0x08;
			case MediaCodecInfo.CodecProfileLevel.H263Level45:					// 0x10;
			case MediaCodecInfo.CodecProfileLevel.H263Level50:					// 0x20;
			case MediaCodecInfo.CodecProfileLevel.H263Level60:					// 0x40;
			case MediaCodecInfo.CodecProfileLevel.H263Level70:					// 0x80;
			default:
				result = result + ",level=unknown " + profileLevel.level; break;
			}
		} else if (mimeType.equalsIgnoreCase("video/mpeg4")) {
			switch (profileLevel.profile) {
			// from OMX_VIDEO_MPEG4PROFILETYPE
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimple:			// 0x01;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleScalable:	// 0x02;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileCore:				// 0x04;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileMain:				// 0x08;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileNbit:				// 0x10;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileScalableTexture:	// 0x20;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleFace:		// 0x40;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleFBA:		// 0x80;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileBasicAnimated:	// 0x100;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileHybrid:			// 0x200;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedRealTime:	// 0x400;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileCoreScalable:		// 0x800;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedCoding:	// 0x1000;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedCore:		// 0x2000;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedScalable:	// 0x4000;
			case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedSimple:	// 0x8000;
			default:
				result = "profile:unknown " + profileLevel.profile; break;
			}
			switch (profileLevel.level) {
			// from OMX_VIDEO_MPEG4LEVELTYPE
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level0:			// 0x01;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level0b:			// 0x02;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level1:			// 0x04;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level2:			// 0x08;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level3:			// 0x10;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level4:			// 0x20;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level4a:			// 0x40;
			case MediaCodecInfo.CodecProfileLevel.MPEG4Level5:			// 0x80;
			default:
				result = result + ",level=unknown " + profileLevel.level; break;
			}
		} else if (mimeType.equalsIgnoreCase("audio/aac")) {
			// from OMX_AUDIO_AACPROFILETYPE
			switch (profileLevel.level) {
			case MediaCodecInfo.CodecProfileLevel.AACObjectMain:		// 1;
			case MediaCodecInfo.CodecProfileLevel.AACObjectLC:			// 2;
			case MediaCodecInfo.CodecProfileLevel.AACObjectSSR:			// 3;
			case MediaCodecInfo.CodecProfileLevel.AACObjectLTP:			// 4;
			case MediaCodecInfo.CodecProfileLevel.AACObjectHE:			// 5;
			case MediaCodecInfo.CodecProfileLevel.AACObjectScalable:	// 6;
			case MediaCodecInfo.CodecProfileLevel.AACObjectERLC:		// 17;
			case MediaCodecInfo.CodecProfileLevel.AACObjectLD:			// 23;
			case MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS:		// 29;
			case MediaCodecInfo.CodecProfileLevel.AACObjectELD:			// 39;
			default:
				result = "profile:unknown " + profileLevel.profile; break;
			}
		} else if (mimeType.equalsIgnoreCase("video/vp8")) {
			switch (profileLevel.profile) {
			// from OMX_VIDEO_VP8PROFILETYPE
			case MediaCodecInfo.CodecProfileLevel.VP8ProfileMain:		// 0x01;
			default:
				result = "profile:unknown " + profileLevel.profile; break;
			}
			switch (profileLevel.level) {
			// from OMX_VIDEO_VP8LEVELTYPE
			case MediaCodecInfo.CodecProfileLevel.VP8Level_Version0:	// 0x01;
			case MediaCodecInfo.CodecProfileLevel.VP8Level_Version1:	// 0x02;
			case MediaCodecInfo.CodecProfileLevel.VP8Level_Version2:	// 0x04;
			case MediaCodecInfo.CodecProfileLevel.VP8Level_Version3:	// 0x08;
			default:
				result = result + ",level=unknown " + profileLevel.level; break;
			}
		}

		return result;
	}

	/**
	 * codec specific dataの先頭マーカー位置を検索
	 * @param array
	 * @param offset
	 * @return
	 */
	@Deprecated
	public static final int findStartMarker(@NonNull final byte[] array, final int offset) {
		//noinspection deprecation
		return BufferHelper.byteComp(array, offset, START_MARKER, START_MARKER.length);
	}
}
