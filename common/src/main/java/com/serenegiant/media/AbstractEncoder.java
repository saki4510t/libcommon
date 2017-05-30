package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import java.nio.ByteBuffer;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.utils.MediaInfo;
import com.serenegiant.utils.Time;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractEncoder implements Encoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AbstractEncoder.class.getSimpleName();

    public static final int TIMEOUT_USEC = 10000;	// 10ミリ秒

    private volatile int mRequestDrain;
    /**
     * エンコード実行中フラグ
     */
    protected volatile boolean mIsCapturing;
    /**
     * 終了要求フラグ(新規エンコード禁止フラグ)
     */
    protected boolean mRequestStop;
    /**
     * ファイルへの出力中フラグ
     */
    protected volatile boolean mRecorderStarted;
    /**
     * 終了フラグ
     */
    protected boolean mIsEOS;
    /**
     * トラックインデックス
     */
    protected int mTrackIndex;
    /**
     * エンコーダーの本体MediaCodecインスタンス
     */
    protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
    /**
     * エンコード用バッファ情報
     */
    private MediaCodec.BufferInfo mBufferInfo;		// API >= 16(Android4.1.2)

    /**
     * Recorderオブジェクトへの参照
     */
    private IRecorder mRecorder;
    /**
     * フラグの排他制御用
     */
    protected final Object mSync = new Object();
    /**
     * エンコーダーイベントコールバックリスナー
     */
    private final EncoderListener mListener;
    /**
     * MIME
     */
    protected final String MIME_TYPE;

	public static final int getCodecCount() {
		return MediaInfo.getCodecCount();
	}

	public static final List<MediaCodecInfo> getCodecs() {
		return MediaInfo.getCodecs();
	}

	public static final MediaCodecInfo getCodecInfoAt(final int ix) {
		return MediaInfo.getCodecInfoAt(ix);
	}

	public static MediaCodecInfo.CodecCapabilities getCodecCapabilities(final MediaCodecInfo codecInfo, final String mimeType) {
		return MediaInfo.getCodecCapabilities(codecInfo, mimeType);
	}

//********************************************************************************
    public AbstractEncoder(final String mime_type, final IRecorder recorder, final EncoderListener listener) {
    	if (listener == null) throw new NullPointerException("EncodeListener is null");
    	if (recorder == null) throw new NullPointerException("IMuxer is null");
    	MIME_TYPE = mime_type;
    	mRecorder = recorder;
    	mListener = listener;
		recorder.addEncoder(this);
        // 効率化のために先に生成しておく(drain内で毎回生成するとGCの影響が大きくなる)
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 出力用のMuxerWrapperを返す
     * @return
     */
	public IRecorder getRecorder() {
    	return mRecorder;
    }

    /**
     * 出力ファイルのパスを返す
     * @return
     */
    @Override
	public String getOutputPath() {
    	return mRecorder != null ? mRecorder.getOutputPath() : null;
    }

	public int getCaptureFormat() {
		return -1;
	}

    @Override
	protected void finalize() throws Throwable {
//    	if (DEBUG) Log.v(TAG, "finalize:");
    	mRecorder = null;
    	release();
        super.finalize();
	}

	@Override
	public final void prepare() throws Exception {
		final boolean mayFail = internalPrepare();
		final Surface surface = (this instanceof ISurfaceEncoder) ?
			((ISurfaceEncoder)this).getInputSurface() : null;
		try {
			mListener.onStartEncode(this, surface, getCaptureFormat(), mayFail);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		synchronized (mSync) {
			// エンコーダースレッドを生成
			new Thread(mDrainTask, getClass().getSimpleName()).start();
			try {
				mSync.wait(1000);	// エンコーダースレッド起床待ち
			} catch (final InterruptedException e) {
				// ignore
			}
		}
	}
	
	protected abstract boolean internalPrepare() throws Exception;

	/**
	 * エラー発生時に呼び出す
	 * @param e
	 */
	protected void callOnError(final Exception e) {
		try {
			mListener.onError(e);
       	} catch (final Exception e2) {
			Log.w(TAG, e2);
        }
	}
//********************************************************************************
	/**
	 * エンコード開始要求(Recorderから呼び出される)
	 */
	@Override
	public  void start() {
//    	if (DEBUG) Log.v(TAG, "start");
		synchronized (mSync) {
			mIsCapturing = true;
			mRequestStop = false;
			mRequestDrain = 0;
		}
	}
    /**
     * エンコーダ終了要求(Recorderから呼び出される)
     */
	@Override
	public  void stop() {
//    	if (DEBUG) Log.v(TAG, "stop");
    	synchronized (mSync) {
            if (/*!mIsCapturing ||*/ mRequestStop) {
                return;
            }
	        // 終了要求
            mRequestStop = true;	// 新規のフレームを受けないようにする
            mSync.notifyAll();
        }
        // 本当のところいつ終了するのかはわからないので、呼び出し元スレッドを遅延させないために終了待ちせずに直ぐに返る
    }

    /**
     * フレームデータの読込み準備要求
     * native側からも呼び出されるので名前を変えちゃダメ
     */
    @Override
	public void frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "AbstractEncoder#frameAvailableSoon");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
    }

//********************************************************************************
	private final Runnable mDrainTask = new Runnable() {
		// ドレインスレッドの実体
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY); // THREAD_PRIORITY_URGENT_AUDIO
			synchronized (mSync) {
				mRequestStop = false;
				mRequestDrain = 0;
				mSync.notify();	// スレッド起床通知
			}
			boolean localRequestStop = false;
			boolean localRequestDrain;
			// #startが呼ばれるまで待機
			for ( ; !mIsCapturing && !localRequestStop ; ) {
				synchronized (mSync) {
				try {
					mSync.wait(10);
				} catch (final InterruptedException e) {
					break;
				}
				localRequestStop = mRequestStop;
				mRequestDrain = 0;
			}
		}
		// エンコード済みデータの処理ループ
		for ( ; ; ) {
			synchronized (mSync) {
				localRequestStop = mRequestStop;
				localRequestDrain = (mRequestDrain > 0);
				if (localRequestDrain)
					mRequestDrain--;
			}
			if (localRequestStop) {
				try {
					drain();
				} catch (final Exception e) {
					// ignore
				}
				try {
					signalEndOfInputStream();
				} catch (final Exception e) {
					// ignore
				}
				try {
					drain();
				} catch (final Exception e) {
					// ignore
				}
				release();
				break;
			}
			if (localRequestDrain) {
				try {
					drain();
				} catch (final Exception e) {
					mRequestStop = true;
				}
			} else {
				synchronized (mSync) {
					try {
						mSync.wait(30);
					} catch (final InterruptedException e) {
						break;
					}
				}
			}
		} // end of for
		// 終了
		synchronized (mSync) {
			mRequestStop = true;
			mIsCapturing = false;
			mSync.notifyAll();
		}
	}
};

//********************************************************************************
	/**
	 * 子クラスでOverrideした時でもEncoder#releaseを呼び出すこと
	 */
	@Override
	public  void release() {
//		if (DEBUG) Log.d(TAG, "release:");
		if (mIsCapturing) {
			try {
				mListener.onStopEncode(this);
			} catch (final Exception e) {
//				Log.e(TAG, "failed onStopped", e);
			}
		}
		mIsCapturing = false;
        if (mMediaCodec != null) {
			try {
//				if (DEBUG) Log.v(TAG, "call MediaCodec#stop");
	            mMediaCodec.stop();
	            mMediaCodec.release();
	            mMediaCodec = null;
			} catch (final Exception e) {
//				Log.e(TAG, "failed releasing MediaCodec", e);
			}
        }
        if (mRecorderStarted) {
        	mRecorderStarted = false;
        	if (mRecorder != null) {
       			try {
//    				if (DEBUG) Log.v(TAG, "call MuxerWrapper#stop");
       				mRecorder.stop(this);
    			} catch (final Exception e) {
//    				Log.e(TAG, "failed stopping muxer", e);
    			}
//				mRecorder = null;
        	}
        }
		try {
			mListener.onDestroy(this);
		} catch (final Exception e) {
//			Log.e(TAG, "destroy:", e);
		}
        mBufferInfo = null;
		mRecorder = null;
	}

	/**
	 * ストリーミング終了指示を送る
	 */
	@Override
	public  void signalEndOfInputStream() {
//		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
        // MediaCodec#signalEndOfInputStreamはBUFFER_FLAG_END_OF_STREAMフラグを付けて
        // 空のバッファをセットするのと等価である
    	// ・・・らしいので空バッファを送る。encode内でBUFFER_FLAG_END_OF_STREAMを付けてセットする
        encode(null, 0, getInputPTSUs());
	}

    // encodeはnative側からアクセスするので変更時は注意
	@Override
	public void encode(final ByteBuffer buffer){/* VideoEncoder以外は特に何もしない */}

	@Override
	public boolean isCapturing() {
        synchronized (mSync) {
            return mIsCapturing;
        }
	}

    /**
     * バイト配列をエンコードする場合
     * @param buffer
     * @param length　書き込むバイト配列の長さ。0ならBUFFER_FLAG_END_OF_STREAMフラグをセットする
     * @param presentationTimeUs [マイクロ秒]
     */
	@Override
	public  void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) return;
			if (mMediaCodec == null) return;
		}
        @SuppressWarnings("deprecation")
		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
	        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
	        if (inputBufferIndex >= 0) {
	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            if (buffer != null) {
	            	inputBuffer.put(buffer);
	            }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
	            if (length <= 0) {
	            	// エンコード要求サイズが0の時はEOSを送信
	            	mIsEOS = true;
//	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
	            } else {
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
	            		presentationTimeUs, 0);
	            }
	            break;
	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//	        	// 送れるようになるまでループする
//	        	// MediaCodec#dequeueInputBufferにタイムアウト(10ミリ秒)をセットしているのでここでは待機しない
	        	frameAvailableSoon();	// drainが詰まってると予想されるのでdrain要求をする
	        }
        }
    }

    /**
     * エンコードしたデータをmuxerへ書き込む
     */
	@SuppressWarnings("deprecation")
	private final void drain() {
//    	if (DEBUG) Log.v(TAG, "drain:encoder=" + this);
    	if (mMediaCodec == null) return;
    	ByteBuffer[] encoderOutputBuffers;
    	try {
    		encoderOutputBuffers = mMediaCodec.getOutputBuffers();
    	} catch (final IllegalStateException e) {
//    		Log.w(TAG, "drain:", e);
    		return;
    	}
        int encoderStatus, count = 0;
        final IRecorder recorder = mRecorder;
        if (recorder == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
//        	Log.w(TAG, "muxer is unexpectedly null");
        	return;
        }
LOOP:	while (mIsCapturing) {
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);	// wait for max TIMEOUT_USEC(=10msec)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 出力するデータが無い時は最大でTIMEOUT_USEC x 5 = 50msec経過するかEOSが来るまでループする
                if (!mIsEOS) {
                	if (++count > 5)
                		break LOOP;		// out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                // エンコード時にはこれは来ないはず
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            	// コーデックからの出力フォーマットが変更された時
                // エンコード済みバッファの受け取る前にだけ１回来るはず。
            	// ただし、Android4.3未満だとINFO_OUTPUT_FORMAT_CHANGEDは来ないので
            	// 代わりにflags & MediaCodec.BUFFER_FLAG_CODEC_CONFIGの時に処理しないとだめ
                if (mRecorderStarted) {	// ２回目が来た時はエラー
                    throw new RuntimeException("format changed twice");
                }
				// コーデックからの出力フォーマットを取得してnative側へ引き渡す
				// getOutputFormatはINFO_OUTPUT_FORMAT_CHANGEDが来た後でないと呼んじゃダメ(クラッシュする)
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                if (!startRecorder(recorder, format))
                	break LOOP;
            } else if (encoderStatus < 0) {
            	// 想定外の結果が返って来た時は無視する
//            	if (DEBUG) Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                	// 出力バッファインデックスが来てるのに出力バッファを取得できない・・・無いはずやねんけど
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//					if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                	// Android4.3未満をターゲットにするならここで処理しないと駄目
                    if (!mRecorderStarted) {	// 1回目に来た時だけ処理する
                    	// csd-0とcsd-1が同時に来ているはずなので分離してセットする
                        final byte[] tmp = new byte[mBufferInfo.size];
                        encodedData.position(0);
                        encodedData.get(tmp, mBufferInfo.offset, mBufferInfo.size);
                        encodedData.position(0);
                        final int ix0 = byteComp(tmp, 0, START_MARK, START_MARK.length);
                        final int ix1 = byteComp(tmp, ix0 + 2, START_MARK, START_MARK.length);
						final int ix2 = byteComp(tmp, ix1 + 2, START_MARK, START_MARK.length);
//						if (DEBUG) Log.i(TAG, "ix0=" + ix0 + ",ix1=" + ix1);
                        final MediaFormat outFormat = createOutputFormat(tmp, mBufferInfo.size, ix0, ix1, ix2);
                        if (!startRecorder(recorder, outFormat))
                        	break LOOP;
                    }
					mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                	// エンコード済みバッファにデータが入っている時・・・待機カウンタをクリア
            		count = 0;
                    if (!mRecorderStarted) {
                    	// でも出力可能になっていない時
                    	// =INFO_OUTPUT_FORMAT_CHANGED/BUFFER_FLAG_CODEC_CONFIGをまだ受け取ってない時
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // ファイルに出力(presentationTimeUsを調整)
                    try {
	                   	mBufferInfo.presentationTimeUs = getNextOutputPTSUs(mBufferInfo.presentationTimeUs);
	                   	recorder.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
//						prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    } catch (final TimeoutException e) {
//						if (DEBUG) Log.v(TAG, "最大録画時間を超えた", e);
                    	recorder.stopRecording();
                    } catch (final Exception e) {
//						if (DEBUG) Log.w(TAG, e);
                    	recorder.stopRecording();
                    }
                }
                // 出力済みのバッファをエンコーダーに返す
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                	// ストリーム終了指示が来た時
//					if (!mIsEOS) {
//						if (DEBUG) Log.w(TAG, "drain:reached end of stream unexpectedly");
//					} else {
//						if (DEBUG) Log.d(TAG, "drain:end of stream reached");
//					}
                	stopRecorder(recorder);
					break LOOP;      // out of while
                }
            }
        }	// while (mIsCapturing)
//		if (DEBUG) Log.v(TAG, "drain:finished");
    }

    protected abstract MediaFormat createOutputFormat(final byte[] csd, final int size,
    	final int ix0, final int ix1, final int ix2);

	/**
	 * コーデックからの出力フォーマットを取得してnative側へ引き渡してRecorderをスタートさせる
	 */
	public boolean startRecorder(final IRecorder recorder, final MediaFormat outFormat) {
//		if (DEBUG) Log.i(TAG, "startMuxer:outFormat=" + outFormat + ",state=" + recorder.getState());
		if (recorder.getState() != IRecorder.STATE_STARTING) {
			for (int i = 0; i < 10; i++) {
				if (recorder.getState() == IRecorder.STATE_STARTING) {
					break;
				}
//				Log.v(TAG, "sleep");
				try {
					Thread.sleep(10);
				} catch (final InterruptedException e) {
					break;
				}
			}
		}
		if (recorder.getState() == IRecorder.STATE_STARTING) {
			mTrackIndex = recorder.addTrack(this, outFormat);
			if (mTrackIndex >= 0) {
				mRecorderStarted = true;
				if (!recorder.start(this)) {
					// startは全てのエンコーダーがstartを呼ぶまで返ってこない
					// falseを返した時はmuxerをスタート出来てない。何らかの異常
	//	       		Log.e(TAG, "failed to start muxer mTrackIndex=" + mTrackIndex);
				}
			} else {
	//			Log.e(TAG, "failed to addTrack: mTrackIndex=" + mTrackIndex);
				recorder.removeEncoder(this);
			}
		}
       	return recorder.isStarted();
	}

	public void stopRecorder(final IRecorder recorder) {
   		mRecorderStarted = mIsCapturing = false;
	}

    /**
     * 前回Recorderに書き込んだ際のpresentationTimeUs
     */
	private long prevOutputPTSUs = -1;
//	private long firstOutputPTSUs = -1;

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
    @SuppressLint("NewApi")
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
/*		if (result <= prevOutputPTSUs) {
			Log.w(TAG, "input pts smaller than previous output PTS");
			result = (prevOutputPTSUs - result) + result;
		} */
		if (result <= prevInputPTSUs) {
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
    }

    /**
     * Muxerの今回の書き込み用のpresentationTimeUs値を取得
     * @return
     */
    protected long getNextOutputPTSUs(long presentationTimeUs) {
//		long result = Time.nanoTime() / 1000L;
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
/*		if (result < prevOutputPTSUs)
			result = (prevOutputPTSUs - result) + result; */
		if (presentationTimeUs <= prevOutputPTSUs) {
			presentationTimeUs = prevOutputPTSUs + 9643;
		}
		prevOutputPTSUs = presentationTimeUs;
		return presentationTimeUs;
    }

//********************************************************************************
//********************************************************************************
    /**
     * プロファイル・レベルが低ければtrueを返す
     * @param mimeType
     * @param info
     * @return
     */
    public static boolean checkProfileLevel(final String mimeType, final MediaCodecInfo info) {
        if (info != null) {
        	if (mimeType.equalsIgnoreCase("video/avc")) {
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

    public static void dumpProfileLevel(final String mimeType, final MediaCodecInfo info) {
        if (info != null) {
//        	Log.i(TAG, "dumpProfileLevel:codec=" + info.getName());
/*            final MediaCodecInfo.CodecCapabilities caps = getCodecCapabilities(info, mimeType);
	        final MediaCodecInfo.CodecProfileLevel[] profileLevel = caps.profileLevels;
	        for (int j = 0; j < profileLevel.length; j++) {
	        	Log.i(TAG, getProfileLevelString(mimeType, profileLevel[j]));
	        } */
        }
    }

    public static String getProfileLevelString(final String mimeType, final MediaCodecInfo.CodecProfileLevel profileLevel) {
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
    	} else if (mimeType.equalsIgnoreCase("ausio/aac")) {
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
     * codec specific dataの先頭マーカー
     */
	protected static final byte[] START_MARK = { 0, 0, 0, 1, };
	/**
	 * byte[]を検索して一致する先頭インデックスを返す
	 * @param array 検索されるbyte[]
	 * @param search 検索するbyte[]
	 * @param len 検索するバイト数
	 * @return 一致した先頭位置、一致しなければ-1
	 */
	protected final int byteComp(@NonNull final byte[] array, final int offset, @NonNull final byte[] search, final int len) {
		int index = -1;
		final int n0 = array.length;
		final int ns = search.length;
		if ((n0 >= offset + len) && (ns >= len)) {
			for (int i = offset; i < n0 - len; i++) {
				int j = len - 1;
				while (j >= 0) {
					if (array[i + j] != search[j]) break;
					j--;
				}
				if (j < 0) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

}
