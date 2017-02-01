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
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

/**
 * 音声データをMediaCodecでエンコードするためのクラス
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractAudioEncoder extends AbstractEncoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "AbstractAudioEncoder";

	public static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

	public static final int DEFAULT_SAMPLE_RATE = 44100;	// 44.1[KHz]	8-48[kHz] 全機種で保証されているのは44100だけ
	public static final int DEFAULT_BIT_RATE = 64000;		// 64[kbps]		5-320[kbps]
    public static final int SAMPLES_PER_FRAME = 1024;		// AAC, bytes/frame/channel
	public static final int FRAMES_PER_BUFFER = 25; 		// AAC, frame/mBuffer/sec

    protected int mAudioSource;
    protected int mChannelCount;
	protected int mSampleRate;
    protected int mBitRate;

	public AbstractAudioEncoder(final IRecorder recorder, final EncoderListener listener,
								final int audio_source, final int audio_channels) {
		this(recorder, listener, audio_source, audio_channels, DEFAULT_SAMPLE_RATE, DEFAULT_BIT_RATE);
	}

	public AbstractAudioEncoder(final IRecorder recorder, final EncoderListener listener,
		final int audio_source, final int audio_channels, final int sample_rate, final int bit_rate) {

		super(AUDIO_MIME_TYPE, recorder, listener);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mAudioSource = audio_source;
		mChannelCount = audio_channels;
		mSampleRate = sample_rate;
		mBitRate = bit_rate;
	}

	@Override
	public void prepare() throws Exception {
//		if (DEBUG) Log.v(TAG, "prepare:");
        mTrackIndex = -1;
        mRecorderStarted = mIsEOS = false;

// 音声を取り込んでAACにエンコードするためのMediaCodecの準備
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
//			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
//		if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelCount);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK,
			mChannelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
//		if (DEBUG) Log.i(TAG, "format: " + audioFormat);

		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
//		if (DEBUG) Log.i(TAG, "prepare finished");
        callOnStartEncode(null, -1, false);
	}

    /**
     * 指定したMIMEに一致する最初のコーデックを選択する
     * @param mimeType
     * @return
     */
	private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
//    	if (DEBUG) Log.v(TAG, "selectAudioCodec:");

    	MediaCodecInfo result = null;
    	// コーデックの一覧を取得
        final int numCodecs = getCodecCount();
LOOP:	for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {	// エンコーダーでない(=デコーダー)はスキップする
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
//            	if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
               		result = codecInfo;
               		break LOOP;
                }
            }
        }
   		return result;
    }

	@Override
	public final boolean isAudio() {
		return true;
	}

	@Override
	protected MediaFormat createOutputFormat(final byte[] csd, final int size, final int ix0, final int ix1) {
		MediaFormat outFormat;
        if (ix0 >= 0) {
//        	Log.w(TAG, "csd may be wrong, it may be for video");
        }
        // audioの時はSTART_MARKが無いので全体をコピーして渡す
        outFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelCount);
        // encodedDataをそのまま渡しても再生できないファイルが出来上がるので一旦コピーしないと駄目
        final ByteBuffer csd0 = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        csd0.put(csd, 0, size);
        csd0.flip();
        outFormat.setByteBuffer("csd-0", csd0);
        return outFormat;
	}

}
