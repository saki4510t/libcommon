package com.serenegiant.media;
/*
 * Androusb
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import com.serenegiant.utils.BuildCheck;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
@SuppressWarnings("deprecation")
public abstract class AbstractVideoEncoder extends AbstractEncoder {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "AbstractVideoEncoder";

	public static final String MIME_AVC = "video/avc";
	/**
	 * 非圧縮image/videoで使用可能なカラーフォーマット
     * from OMX_IVCommon.h
	 * MediaCodecInfo.CodecCapabilities.COLOR_FormatXXXに一部のものは定義されている
     *  Unused                 : Placeholder value when format is N/A
     *  Monochrome             : black and white
     *  8bitRGB332             : Red 7:5, Green 4:2, Blue 1:0
     *  12bitRGB444            : Red 11:8, Green 7:4, Blue 3:0
     *  16bitARGB4444          : Alpha 15:12, Red 11:8, Green 7:4, Blue 3:0
     *  16bitARGB1555          : Alpha 15, Red 14:10, Green 9:5, Blue 4:0
     *  16bitRGB565            : Red 15:11, Green 10:5, Blue 4:0
     *  16bitBGR565            : Blue 15:11, Green 10:5, Red 4:0
     *  18bitRGB666            : Red 17:12, Green 11:6, Blue 5:0
     *  18bitARGB1665          : Alpha 17, Red 16:11, Green 10:5, Blue 4:0
     *  19bitARGB1666          : Alpha 18, Red 17:12, Green 11:6, Blue 5:0
     *  24bitRGB888            : Red 24:16, Green 15:8, Blue 7:0
     *  24bitBGR888            : Blue 24:16, Green 15:8, Red 7:0
     *  24bitARGB1887          : Alpha 23, Red 22:15, Green 14:7, Blue 6:0
     *  25bitARGB1888          : Alpha 24, Red 23:16, Green 15:8, Blue 7:0
     *  32bitBGRA8888          : Blue 31:24, Green 23:16, Red 15:8, Alpha 7:0
     *  32bitARGB8888          : Alpha 31:24, Red 23:16, Green 15:8, Blue 7:0
     *  YUV411Planar           : U,Y are subsampled by a factor of 4 horizontally
     *  YUV411PackedPlanar     : packed per payload in planar slices
     *  YUV420Planar           : Three arrays Y,U,V.
     *  YUV420PackedPlanar     : packed per payload in planar slices
     *  YUV420SemiPlanar       : Two arrays, one is all Y, the other is U and V
     *  YUV422Planar           : Three arrays Y,U,V.
     *  YUV422PackedPlanar     : packed per payload in planar slices
     *  YUV422SemiPlanar       : Two arrays, one is all Y, the other is U and V
     *  YCbYCr                 : Organized as 16bit YUYV (i.e. YCbYCr)
     *  YCrYCb                 : Organized as 16bit YVYU (i.e. YCrYCb)
     *  CbYCrY                 : Organized as 16bit UYVY (i.e. CbYCrY)
     *  CrYCbY                 : Organized as 16bit VYUY (i.e. CrYCbY)
     *  YUV444Interleaved      : Each pixel contains equal parts YUV
     *  RawBayer8bit           : SMIA camera output format
     *  RawBayer10bit          : SMIA camera output format
     *  RawBayer8bitcompressed : SMIA camera output format
     */
//	public static final int OMX_COLOR_FORMATTYPE = 0;
	public static final int OMX_COLOR_FormatUnused = 0;
	public static final int OMX_COLOR_FormatMonochrome = 1;
	public static final int OMX_COLOR_Format8bitRGB332 = 2;
	public static final int OMX_COLOR_Format12bitRGB444 = 3;
	public static final int OMX_COLOR_Format16bitARGB4444 = 4;
	public static final int OMX_COLOR_Format16bitARGB1555 = 5;
	public static final int OMX_COLOR_Format16bitRGB565 = 6;
	public static final int OMX_COLOR_Format16bitBGR565 = 7;
	public static final int OMX_COLOR_Format18bitRGB666 = 8;
	public static final int OMX_COLOR_Format18bitARGB1665 = 9;
	public static final int OMX_COLOR_Format19bitARGB1666 = 10;
	public static final int OMX_COLOR_Format24bitRGB888 = 11;
	public static final int OMX_COLOR_Format24bitBGR888 = 12;
	public static final int OMX_COLOR_Format24bitARGB1887 = 13;
	public static final int OMX_COLOR_Format25bitARGB1888 = 14;
	public static final int OMX_COLOR_Format32bitBGRA8888 = 15;
	public static final int OMX_COLOR_Format32bitARGB8888 = 16;
	public static final int OMX_COLOR_FormatYUV411Planar = 17;
	public static final int OMX_COLOR_FormatYUV411PackedPlanar = 18;
	public static final int OMX_COLOR_FormatYUV420Planar = 19;
	public static final int OMX_COLOR_FormatYUV420PackedPlanar = 20;
	public static final int OMX_COLOR_FormatYUV420SemiPlanar = 21;
	public static final int OMX_COLOR_FormatYUV422Planar = 22;
	public static final int OMX_COLOR_FormatYUV422PackedPlanar = 23;
	public static final int OMX_COLOR_FormatYUV422SemiPlanar = 24;
	public static final int OMX_COLOR_FormatYCbYCr = 25;
	public static final int OMX_COLOR_FormatYCrYCb = 26;
	public static final int OMX_COLOR_FormatCbYCrY = 27;
	public static final int OMX_COLOR_FormatCrYCbY = 28;
	public static final int OMX_COLOR_FormatYUV444Interleaved = 29;
	public static final int OMX_COLOR_FormatRawBayer8bit = 30;
	public static final int OMX_COLOR_FormatRawBayer10bit = 31;
	public static final int OMX_COLOR_FormatRawBayer8bitcompressed = 32;
	public static final int OMX_COLOR_FormatL2 = 33;
	public static final int OMX_COLOR_FormatL4 = 34;
	public static final int OMX_COLOR_FormatL8 = 35;
	public static final int OMX_COLOR_FormatL16 = 36;
	public static final int OMX_COLOR_FormatL24 = 37;
	public static final int OMX_COLOR_FormatL32 = 38;
	public static final int OMX_COLOR_FormatYUV420PackedSemiPlanar = 39;
	public static final int OMX_COLOR_FormatYUV422PackedSemiPlanar = 40;
	public static final int OMX_COLOR_Format18BitBGR666 = 41;
	public static final int OMX_COLOR_Format24BitARGB6666 = 42;
	public static final int OMX_COLOR_Format24BitABGR6666 = 43;
	public static final int OMX_COLOR_FormatKhronosExtensions = 0x6F000000; //< Reserved region for introducing Khronos Standard Extensions
	public static final int OMX_COLOR_FormatVendorStartUnused = 0x7F000000; //< Reserved region for introducing Vendor Extensions
	// Reserved android opaque colorformat. Tells the encoder that
	// the actual colorformat will be relayed by the Gralloc Buffers.
	// FIXME: In the process of reserving some enum values for
	// Android-specific OMX IL colorformats. Change this enum to
	// an acceptable range once that is done.
	public static final int OMX_COLOR_FormatAndroidOpaque = 0x7F000789;
	public static final int OMX_TI_COLOR_FormatYUV420PackedSemiPlanar = 0x7F000100;
	public static final int OMX_QCOM_COLOR_FormatYVU420SemiPlanar = 0x7FA30C00;
	public static final int OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka = 0x7FA30C03;
	public static final int OMX_SEC_COLOR_FormatNV12Tiled = 0x7FC00002;
	public static final int OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m = 0x7FA30C04;
	public static final int OMX_COLOR_FormatMax = 0x7FFFFFFF;

    protected int mColorFormat;
    protected int mWidth, mHeight;
    protected int mBitRate = -1;
	protected int mFramerate = -1;
    protected int mIFrameIntervals = -1;

    public AbstractVideoEncoder(final String mime, final IRecorder recorder, final EncoderListener listener) {
		super(mime, recorder, listener);
    }

	/**
	 * 動画サイズをセット
	 * ビットレートもサイズとVideoConfigの設定値に合わせて変更される
	 * @param width
	 * @param height
	 */
    public void setVideoSize(final int width, final int height) {
//    	Log.d(TAG, String.format("setVideoSize(%d,%d)", width, height));
    	mWidth = width;
    	mHeight = height;
		mBitRate = VideoConfig.getBitrate(width, height);
    }

	public void setVideoConfig(final int bitRate, final int frameRate, final int iFrameIntervals) {
		mBitRate = bitRate;
		mFramerate = frameRate;
		mIFrameIntervals = iFrameIntervals;
	}

    public int getWidth() {
    	return mWidth;
    }

    public int getHeight() {
    	return mHeight;
    }

    public abstract int getCaptureFormat();
    public abstract Surface getInputSurface();


    /**
     * 指定したMIMEで使用可能がcodecの一覧の中から先頭のものを取得する
     * もし使用可能なのがなければnullを返す
     */
	public final MediaCodecInfo selectVideoCodec(final String mimeType) {
//    	if (DEBUG) Log.v(TAG, "selectCodec:");

    	// コーデックの一覧を取得
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
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
            		format = selectColorFormat(codecInfo, mimeType);
                	if (format > 0) {
                		mColorFormat = format;
                		return codecInfo;
                	}
                }
            }
        }
        return null;
    }

	/**
	 * 使用可能なカラーフォーマットを設定
	 */
    public static int[] recognizedFormats;
	static {
		recognizedFormats = new int[] {
        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
		};
	}

	@Override
	public final boolean isAudio() {
		return false;
	}

    public static boolean supportsAdaptiveStreaming = BuildCheck.isKitKat();

    @TargetApi(Build.VERSION_CODES.KITKAT)
	public void adjustBitrate(final int targetBitrate) {
        if (supportsAdaptiveStreaming && mMediaCodec != null) {
            final Bundle bitrate = new Bundle();
            bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, targetBitrate);
            mMediaCodec.setParameters(bitrate);
        } else if (!supportsAdaptiveStreaming) {
//			Log.w(TAG, "Ignoring adjustVideoBitrate call. This functionality is only available on Android API 19+");
        }
    }

	@Override
	protected MediaFormat createOutputFormat(final byte[] csd, final int size, final int ix0, final int ix1) {
		final MediaFormat outFormat;
        if (ix0 >= 0) {
            outFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        	final ByteBuffer csd0 = ByteBuffer.allocateDirect(ix1 - ix0).order(ByteOrder.nativeOrder());
        	csd0.put(csd, ix0, ix1 - ix0);
        	csd0.flip();
            outFormat.setByteBuffer("csd-0", csd0);
            if (ix1 > ix0) {
				// FIXME ここのサイズはsize-ix1、今はたまたまix0=0だから大丈夫なのかも
            	final ByteBuffer csd1 = ByteBuffer.allocateDirect(size - ix1 + ix0).order(ByteOrder.nativeOrder());
            	csd1.put(csd, ix1, size - ix1 + ix0);
            	csd1.flip();
                outFormat.setByteBuffer("csd-1", csd1);
            }
        } else {
        	throw new RuntimeException("unexpected csd data came.");
        }
        return outFormat;
	}

//	@Override
//	public void prepare() throws Exception {
//		// TODO Auto-generated method stub
//
//	}

	/**
	 * 指定したカラーフォーマットをこのEncoderで使用可能かどうかを返す
	 * @param colorFormat
	 * @return
	 */
    private static final boolean isRecognizedVideoFormat(final int colorFormat) {
    	final int n = recognizedFormats != null ? recognizedFormats.length : 0;
    	for (int i = 0; i < n; i++) {
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
    private static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
    	int result = 0;
		final MediaCodecInfo.CodecCapabilities capabilities = getCodecCapabilities(codecInfo, mimeType);
        final int[] colorFormats = capabilities.colorFormats;
        final int n = colorFormats.length;
        int colorFormat;
//    	if (DEBUG) Log.i(TAG, "selectColorFormat:使用可能なカラーフォーマットを検索する");
        for (int i = 0; i < n; i++) {
        	colorFormat = colorFormats[i];
//        	if (DEBUG) Log.i(TAG,
//        		String.format("codec:%s,MIME:%s,colorFormat:%s,%d",
//        			codecInfo.getName(), mimeType, getColorFormatName(colorFormat), colorFormat));
            if (isRecognizedVideoFormat(colorFormat)) {
				result = colorFormat;
				break;	// if (!DEBUG) break;
            }
        }
//		if (result == 0)
//        	Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

	public static final void dumpVideoCodecEncoders() {
//    	if (DEBUG) Log.v(TAG, "dumpMediaCodecEncoders:");
    	// コーデックの一覧を取得
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);	// API >= 16

            if (!codecInfo.isEncoder()) {	// エンコーダーでない(デコーダー)はとばす
                continue;
            }
            // エンコーダーの一覧からMIMEが一致するものを選択する
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
//            	Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME:" + types[j]);
            	// カラーフォーマットを出力する
            	selectColorFormat(codecInfo, types[j]);
            }
        }
    }

    /**
     * Returns true if the specified color format is semi-planar YUV.  Throws an exception
     * if the color format is not recognized (e.g. not YUV).
     */
    protected static final boolean isSemiPlanarYUV(final int colorFormat) {
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

}
