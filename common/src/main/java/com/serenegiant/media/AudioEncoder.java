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
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;

/**
 * AudioRecordから音声データを取得してMediaCodecエンコーダーでエンコードするためのクラス
 * FIXME 今はAbstractEncoderから直接継承しているのをAbstractAudioEncoderを継承するように変更する
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioEncoder extends AbstractEncoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "AudioEncoder";

	public static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    private AudioThread mAudioThread = null;
    protected final int mAudioSource;
    protected final int mChannelCount;
	protected final int mSampleRate;

	public AudioEncoder(final IRecorder recorder, final EncoderListener listener,
						final int audio_source, final int audio_channels) {

		super(AUDIO_MIME_TYPE, recorder, listener);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mAudioSource = audio_source;
		mSampleRate = AbstractAudioEncoder.DEFAULT_SAMPLE_RATE;
		mChannelCount = audio_channels;
		if (audio_source < MediaRecorder.AudioSource.DEFAULT
			|| audio_source > MediaRecorder.AudioSource.VOICE_COMMUNICATION)
			throw new IllegalArgumentException("invalid audio source:" + audio_source);
	}

	@Override
	public void prepare() throws Exception {
//		if (DEBUG) Log.v(TAG, "prepare:");
        mTrackIndex = -1;
        mRecorderStarted = mIsEOS = false;

// 内蔵マイクから音声を取り込んでAACにエンコードするためのMediaCodecの準備
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
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AbstractAudioEncoder.DEFAULT_BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
//		if (DEBUG) Log.i(TAG, "format: " + audioFormat);

		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
//		if (DEBUG) Log.i(TAG, "prepare finished");
        callOnStartEncode(null, -1, false);
	}

	@Override
	public void start() {
		super.start();
		if (mAudioThread == null) {
			// 内蔵マイクからの音声取り込みスレッド生成＆実行
	        mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	@Override
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		mAudioThread = null;
		super.release();
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音を取得してエンコーダーへ書き込むためのスレッド
	 */
    private final class AudioThread extends Thread {
    	public AudioThread() {
    		super("AudioThread");
    	}

    	@Override
    	public final void run() {
//    		if (DEBUG) Log.v(TAG, getName() + " started");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
			final int buffer_size = AudioSampler.getAudioBufferSize(mChannelCount, mSampleRate,
				AbstractAudioEncoder.SAMPLES_PER_FRAME, AbstractAudioEncoder.FRAMES_PER_BUFFER);
/*
			final Class audioSystemClass = Class.forName("android.media.AudioSystem");
			// will disable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_UNAVAILABLE, new String(""));
			// will enable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_AVAILABLE, new Lang.String(""));
*/
    		final AudioRecord audioRecord = AudioSampler.createAudioRecord(
    			mAudioSource, mSampleRate, mChannelCount, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
            int frame_count = 0, err_count = 0;
            final ByteBuffer buf = ByteBuffer.allocateDirect(buffer_size).order(ByteOrder.nativeOrder());
            if (audioRecord != null) {
	            try {
	            	if (mIsCapturing) {
	//    				if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
		                int readBytes;
		                audioRecord.startRecording();
		                try {
		                	for ( ; ;) {
		                		synchronized (mSync) {
			                		if (!mIsCapturing || mRequestStop || mIsEOS) break;
			                	}
		                		buf.clear();
		                		try {
		                			readBytes = audioRecord.read(buf, AbstractAudioEncoder.SAMPLES_PER_FRAME * mChannelCount);
		                		} catch (final Exception e) {
//		    		        		Log.w(TAG, "AudioRecord#read failed:", e);
		                			break;
		                		}
				    			if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
//				    				Log.e(TAG, "Read error ERROR_BAD_VALUE");
				    				err_count++;
				    			} else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
//				    				Log.e(TAG, "Read error ERROR_INVALID_OPERATION");
				    				err_count++;
		                		} else if (readBytes > 0) {
		                			err_count = 0;
		                			frame_count++;
				    			    // 内蔵マイクからの音声入力をエンコーダーにセット
		                			buf.position(readBytes);
		                			buf.flip();
				    				encode(buf, readBytes, getInputPTSUs());
				    				frameAvailableSoon();
				    			}
				    			if (err_count > 10) break;
		                	}
		                	if (frame_count > 0)
		                		frameAvailableSoon();
		                } finally {
		                	audioRecord.stop();
		                }
	            	}
	            } catch (final Exception e) {
//					Log.e(TAG, "exception on AudioRecord", e);
	            } finally {
	            	audioRecord.release();
	            }
//	    	} else {
//        		Log.w(TAG, "AudioRecord failed to initialize");
	    	}
            if (frame_count == 0) {
            	// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
            	// ダミーデータを書き込む
            	for (int i = 0; mIsCapturing && (i < 5); i++) {
	    			buf.position(AbstractAudioEncoder.SAMPLES_PER_FRAME);
	    			buf.flip();
					encode(buf, AbstractAudioEncoder.SAMPLES_PER_FRAME, getInputPTSUs());
					frameAvailableSoon();
					synchronized(this) {
						try {
							wait(50);
						} catch (final InterruptedException e) {
						}
					}
            	}
            }
//			if (DEBUG) Log.v(TAG, "AudioThread:finished");
    	}
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
	public boolean isAudio() {
		return true;
	}

	@Override
	protected MediaFormat createOutputFormat(final byte[] csd, final int size, final int ix0, final int ix1) {
		MediaFormat outFormat;
//		if (ix0 >= 0) {
//			Log.w(TAG, "csd may be wrong, it may be for video");
//		}
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
