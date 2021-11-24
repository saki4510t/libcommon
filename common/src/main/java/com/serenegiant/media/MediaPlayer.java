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
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * 動画再生用のヘルパークラス
 */
@SuppressLint("InlinedApi")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaPlayer {
    private static final boolean DEBUG = true;	// FIXME 実働時はfalseにすること
    private static final String TAG = MediaPlayer.class.getSimpleName();

	@Nullable
	private final Surface mOutputSurface;
	@NonNull
	private final IFrameCallback mCallback;
	private final boolean mAudioEnabled;

	/**
	 * コンストラクタ
	 * @param outputSurface
	 * @param callback
	 * @param audioEnabled
	 * @throws NullPointerException
	 * @throws IllegalArgumentException outputSurface==nullでaudioEnabled=falseなら再生できるものがなにもないのでIllegalArgumentExceptionを投げる
	 */
	public MediaPlayer(
		@Nullable final Surface outputSurface,
		@NonNull final IFrameCallback callback, final boolean audioEnabled)
			throws NullPointerException, IllegalArgumentException {

    	if (DEBUG) Log.v(TAG, "コンストラクタ:");
    	if (callback == null) {
    		throw new NullPointerException("callback should not be null");
		}
		if ((outputSurface == null) && !audioEnabled) {
			throw new IllegalArgumentException("Should playback at least either video or audio.");
		}
		mOutputSurface = outputSurface;
		mCallback = callback;
		mAudioEnabled = audioEnabled;
		new Thread(mMoviePlayerTask, TAG).start();
    	synchronized (mSync) {
    		// ステート処理スレッド起床待ち
    		try {
    			if (!mIsRunning) {
    				mSync.wait();
				}
			} catch (final InterruptedException e) {
				// ignore
			}
    	}
    }

	/**
	 * ループ再生を有効にするかどうかを設定
	 * FIXME ループ再生機能自体は未実装
	 * @param loopEnabled
	 */
	public void setLoop(final boolean loopEnabled) {
		synchronized (mSync) {
			mLoopEnabled = loopEnabled;
		}
	}

    public int getWidth() {
        return mVideoDecoder != null ? mVideoDecoder.getWidth() : 0;
    }

    public int getHeight() {
		return mVideoDecoder != null ? mVideoDecoder.getHeight() : 0;
    }

    public int getBitRate() {
		return mVideoDecoder != null ? mVideoDecoder.getBitRate() : 0;
    }

    public float getFramerate() {
		return mVideoDecoder != null ? mVideoDecoder.getFramerate() : 0;
    }

    /**
     * @return 0, 90, 180, 270
     */
    public int getRotation() {
		return mVideoDecoder != null ? mVideoDecoder.getRotation() : 0;
    }

    /**
     * get duration time as micro seconds
     * @return
     */
    public long getDurationUs() {
		return mVideoDecoder != null ? mVideoDecoder.getDurationUs() : 0;
    }

    /**
     * get audio sampling rate[Hz]
     * @return
     */
    public int getSampleRate() {
		return mAudioDecoder != null ? mAudioDecoder.getSampleRate() : 0;
    }

    public boolean hasAudio() {
    	return mAudioEnabled && (mAudioDecoder != null);
    }

	/**
	 * get currently playing or not
	 * @return
	 */
	public boolean isPlaying() {
		synchronized (mSync) {
			return mState == STATE_PLAYING;
		}
	}

	/**
	 * get currently pausing or not
	 * @return
	 */
	public boolean isPaused() {
		synchronized (mSync) {
			return mState == STATE_PAUSED;
		}
	}

    /**
     * request to prepare movie playing
     * @param src
     */
    public void prepare(@NonNull final String src) {
    	if (DEBUG) Log.v(TAG, "prepare:");
    	synchronized (mSync) {
    		mSource = src;
    		mRequest = REQ_PREPARE;
    		mSync.notifyAll();
    	}
    }

	/**
	 * request to prepare movie playing
	 * @param src
	 */
	public void prepare(@NonNull final AssetFileDescriptor src) {
		if (DEBUG) Log.v(TAG, "prepare:");
		synchronized (mSync) {
			mSource = src;
			mRequest = REQ_PREPARE;
			mSync.notifyAll();
		}
	}

//	/**
//	 * request to prepare movie playing
//	 *
//	 * FileDescriptorで引き渡すと競争条件になるのか途中でMPEG4Extractor: Video is malformedとlogCatへ出力されて
//	 * 再生できなくなってしまう時がある。
//	 * 映像と音声で別々のMediaExtractorを使っているからかもしれない
//	 * @param src
//	 */
//	public void prepare(@NonNull final FileDescriptor src) {
//		if (DEBUG) Log.v(TAG, "prepare:");
//		synchronized (mSync) {
//			mSource = src;
//			mRequest = REQ_PREPARE;
//			mSync.notifyAll();
//		}
//	}

    /**
     * request to start playing movie
     * this method can be called after prepare
     */
    public void play() {
    	if (DEBUG) Log.v(TAG, "play:");
    	synchronized (mSync) {
    		if (mState == STATE_PLAYING) return;
    		mRequest = REQ_START;
    		mSync.notifyAll();
    	}
	}

    /**
     * request to seek to specifc timed frame<br>
     * if the frame is not a key frame, frame image will be broken
     * @param newTime seek to new time[usec]
     */
    public void seek(final long newTime) {
    	if (DEBUG) Log.v(TAG, "seek");
    	synchronized (mSync) {
    		mRequest = REQ_SEEK;
    		mRequestTime = newTime;
    		mSync.notifyAll();
    	}
    }

    /**
     * request stop playing
     */
    public void stop() {
    	if (DEBUG) Log.v(TAG, "stop:");
    	synchronized (mSync) {
    		if (mState != STATE_STOP) {
	    		mRequest = REQ_STOP;
	    		mSync.notifyAll();
	        	try {
	    			mSync.wait(50);
	    		} catch (final InterruptedException e) {
	    			// ignore
	    		}
    		}
    	}
    }

    /**
     * request pause playing<br>
     * this function is not implemented yet
     */
    public void pause() {
    	if (DEBUG) Log.v(TAG, "pause:");
    	synchronized (mSync) {
			if (mState == STATE_PLAYING) {
				mRequest = REQ_PAUSE;
				mSync.notifyAll();
			}
    	}
    }

    /**
     * request resume from pausing<br>
     * this function is not implemented yet
     */
    public void resume() {
    	if (DEBUG) Log.v(TAG, "resume:");
    	synchronized (mSync) {
			if (mState == STATE_PAUSED) {
				mRequest = REQ_RESUME;
				mSync.notifyAll();
			}
    	}
    }

    /**
     * release related resources
     */
    public void release() {
    	if (DEBUG) Log.v(TAG, "release:");
    	stop();
    	synchronized (mSync) {
    		mRequest = REQ_QUIT;
    		mSync.notifyAll();
    	}
    }

//================================================================================
    private static final int TIMEOUT_USEC = 10000;	// 10msec

    /*
     * STATE_STOP => [prepare] => STATE_PREPARED [start]
     * 	=> STATE_PLAYING => [seek] => STATE_PLAYING
     * 		=> [pause] => STATE_PAUSED => [resume] => STATE_PLAYING
     * 		=> [stop] => STATE_STOP
     */
    private static final int STATE_STOP = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;

    // request code
    private static final int REQ_NON = 0;
    private static final int REQ_PREPARE = 1;
    private static final int REQ_START = 2;
    private static final int REQ_SEEK = 3;
    private static final int REQ_STOP = 4;
    private static final int REQ_PAUSE = 5;
    private static final int REQ_RESUME = 6;
    private static final int REQ_QUIT = 9;

	@NonNull
	private final Object mSync = new Object();
	private volatile boolean mIsRunning;
	private int mState;
	@Nullable
	private Object mSource;
	private int mRequest;
	private long mRequestTime;
	/**
	 * ループ再生が有効かどうか
	 */
	private volatile boolean mLoopEnabled;
    // for video playback
    @Nullable
    private VideoDecoder mVideoDecoder;
	// for audio playback
	@Nullable
	private AudioDecoder mAudioDecoder;

//--------------------------------------------------------------------------------
	/**
	 * playback control task
	 */
	private final Runnable mMoviePlayerTask = new Runnable() {
		@WorkerThread
		@Override
		public final void run() {
			boolean isRunning = false;
			int localReq;
			try {
		    	synchronized (mSync) {
					isRunning = mIsRunning = true;
					mState = STATE_STOP;
					mRequest = REQ_NON;
					mRequestTime = -1;
		    		mSync.notifyAll();
		    	}
				while (isRunning) {
					try {
						synchronized (mSync) {
							isRunning = mIsRunning;
							localReq = mRequest;
							mRequest = REQ_NON;
						}
						switch (mState) {
						case STATE_STOP:
							isRunning = processStop(localReq);
							break;
						case STATE_PREPARED:
							isRunning = processPrepared(localReq);
							break;
						case STATE_PLAYING:
							isRunning = processPlaying(localReq);
							break;
						case STATE_PAUSED:
							isRunning = processPaused(localReq);
							break;
						}
					} catch (final InterruptedException e) {
						break;
					} catch (final Exception e) {
						Log.e(TAG, "MoviePlayerTask:", e);
						break;
					}
				} // while (isRunning)
			} finally {
				if (DEBUG) Log.v(TAG, "player task finished:isRunning=" + isRunning);
				handleStop();
			}
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * @param req
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@WorkerThread
	private boolean processStop(final int req) throws InterruptedException, IOException {
		boolean isRunning = true;
		switch (req) {
		case REQ_PREPARE:
			handlePrepare(mSource);
			break;
		case REQ_START:
		case REQ_PAUSE:
		case REQ_RESUME:
			throw new IllegalStateException("invalid state:" + mState);
		case REQ_QUIT:
			isRunning = false;
			break;
//		case REQ_SEEK:
//		case REQ_STOP:
		default:
			synchronized (mSync) {
				mSync.wait();
			}
			break;
		}
		synchronized (mSync) {
			isRunning &= mIsRunning;
		}
		return isRunning;
	}

	/**
	 * @param req
	 * @return
	 * @throws InterruptedException
	 */
	@WorkerThread
	private boolean processPrepared(final int req) throws InterruptedException {
		boolean isRunning = true;
		switch (req) {
		case REQ_START:
			handleStart();
			break;
		case REQ_PAUSE:
		case REQ_RESUME:
			throw new IllegalStateException("invalid state:" + mState);
		case REQ_STOP:
			handleStop();
			break;
		case REQ_QUIT:
			isRunning = false;
			break;
//		case REQ_PREPARE:
//		case REQ_SEEK:
		default:
			synchronized (mSync) {
				mSync.wait();
			}
			break;
		} // end of switch (req)
		synchronized (mSync) {
			isRunning &= mIsRunning;
		}
		return isRunning;
	}

	/**
	 * @param req
	 * @return
	 */
	@WorkerThread
	private boolean processPlaying(final int req) {
		boolean isRunning = true;
		switch (req) {
		case REQ_PREPARE:
		case REQ_START:
		case REQ_RESUME:
			throw new IllegalStateException("invalid state:" + mState);
		case REQ_SEEK:
			handleSeek(mRequestTime);
			break;
		case REQ_STOP:
			handleStop();
			break;
		case REQ_PAUSE:
			handlePause();
			break;
		case REQ_QUIT:
			isRunning = false;
			break;
		default:
			handleLoop(mCallback);
			break;
		} // end of switch (req)
		synchronized (mSync) {
			isRunning &= mIsRunning;
		}
		return isRunning;
	}

	/**
	 * @param req
	 * @return
	 * @throws InterruptedException
	 */
	@WorkerThread
	private boolean processPaused(final int req) throws InterruptedException {
		boolean isRunning = true;
		switch (req) {
		case REQ_PREPARE:
		case REQ_START:
			throw new IllegalStateException("invalid state:" + mState);
		case REQ_SEEK:
			handleSeek(mRequestTime);
			break;
		case REQ_STOP:
			handleStop();
			break;
		case REQ_RESUME:
			handleResume();
			break;
		case REQ_QUIT:
			isRunning = false;
			break;
//		case REQ_PAUSE:
		default:
			synchronized (mSync) {
				mSync.wait();
			}
			break;
		} // end of switch (req)
		synchronized (mSync) {
			isRunning &= mIsRunning;
		}
		return isRunning;
	}

	/**
	 * @param source
	 * @throws IOException
	 */
	@WorkerThread
	private void handlePrepare(final Object source) throws IOException {
		if (DEBUG) Log.v(TAG, "handlePrepare:" + source);
        synchronized (mSync) {
			if (mState != STATE_STOP) {
				throw new RuntimeException("invalid state:" + mState);
			}
		}
		if ((mOutputSurface != null) && mVideoDecoder == null) {
			mVideoDecoder = VideoDecoder.createDecoder(mOutputSurface, mListener);
			mVideoDecoder.prepare(source);
		}
		if (mAudioEnabled && (mAudioDecoder == null)) {
			mAudioDecoder = AudioDecoder.createDecoder(mListener);
			mAudioDecoder.prepare(source);
		}
		synchronized (mSync) {
			mState = STATE_PREPARED;
		}
		mCallback.onPrepared();
	}

	/**
	 * 動画再生開始処理
	 */
	@WorkerThread
	private void handleStart() {
    	if (DEBUG) Log.v(TAG, "handleStart:");
		synchronized (mSync) {
			if (mState != STATE_PREPARED)
				throw new RuntimeException("invalid state:" + mState);
			mState = STATE_PLAYING;
		}
        if (mRequestTime > 0) {
        	handleSeek(mRequestTime);
        }
        if (mVideoDecoder != null) {
        	mVideoDecoder.start();
		}
		if (mAudioDecoder != null) {
			mAudioDecoder.start();
		}
	}

	/**
	 * シーク処理
	 * @param newTimeUs
	 */
	@WorkerThread
	private void handleSeek(final long newTimeUs) {
        if (DEBUG) Log.d(TAG, "handleSeek");
		if (newTimeUs < 0) return;

		if (mVideoDecoder != null) {
			mVideoDecoder.seek(newTimeUs);
		}
		if (mAudioDecoder != null) {
			mAudioDecoder.seek(newTimeUs);
		}
        mRequestTime = -1;
	}

	@WorkerThread
	private void handleLoop(final IFrameCallback frameCallback) {
//		if (DEBUG) Log.d(TAG, "handleLoop");

		synchronized (mSync) {
			try {
				mSync.wait();
			} catch (final InterruptedException e) {
				// ignore
			}
		}
        if (false) {
            if (DEBUG) Log.d(TAG, "Reached EOS, looping check");
            // FIXME 未実装 ループ再生のチェック
        	handleStop();
        }
	}

	@WorkerThread
	private void handleStop() {
    	if (DEBUG) Log.v(TAG, "handleStop:");
    	synchronized (mSync) {
    		if (mVideoDecoder != null) {
    			mVideoDecoder.release();
				mVideoDecoder = null;
			}
			if (mAudioDecoder != null) {
				mAudioDecoder.release();
				mAudioDecoder = null;
			}
    	}
    	final boolean needCallOnFinished;
		synchronized (mSync) {
			needCallOnFinished = (mState != STATE_STOP);
			mState = STATE_STOP;
		}
		if (needCallOnFinished) {
			mCallback.onFinished();
		}
	}

	@WorkerThread
	private void handlePause() {
    	if (DEBUG) Log.v(TAG, "handlePause:");
    	// FIXME unimplemented yet
	}

	@WorkerThread
	private void handleResume() {
    	if (DEBUG) Log.v(TAG, "handleResume:");
    	// FIXME unimplemented yet
	}

	private final DecoderListener mListener = new DecoderListener() {
		@Override
		public void onStartDecode(@NonNull final Decoder decoder) {
			if (DEBUG) Log.v(TAG, "onStartDecode:" + decoder);
		}

		@Override
		public void onStopDecode(@NonNull final Decoder decoder) {
			if (DEBUG) Log.v(TAG, "onStopDecode:" + decoder);
		}

		@Override
		public void onDestroy(@NonNull final Decoder decoder) {
			if (DEBUG) Log.v(TAG, "onDestroy:" + decoder);
		}

		@Override
		public void onError(@NonNull final Throwable t) {
			if (DEBUG) Log.v(TAG, "onError:" + t);
		}
	};

}
