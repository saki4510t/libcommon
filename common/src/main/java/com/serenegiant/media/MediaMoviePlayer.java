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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.Time;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

/**
 * 動画再生用のヘルパークラス
 * FIXME API>=21と未満で処理を分けるようにする
 */
@SuppressLint("InlinedApi")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaMoviePlayer {
    private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
    private static final String TAG_STATIC = "MediaMoviePlayer:";
    private final String TAG = TAG_STATIC + getClass().getSimpleName();

	private final IFrameCallback mCallback;
	private final boolean mAudioEnabled;

	public MediaMoviePlayer(final Surface outputSurface,
		final IFrameCallback callback, final boolean audio_enable)
			throws NullPointerException {

    	if (DEBUG) Log.v(TAG, "Constructor:");
    	if ((outputSurface == null) || (callback == null)) {
    		throw new NullPointerException("outputSurface and callback should not be null");
		}

		mOutputSurface = outputSurface;
		mCallback = callback;
		mAudioEnabled = audio_enable;
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
	public final void setLoop(final boolean loopEnabled) {
		synchronized (mSync) {
			mLoopEnabled = loopEnabled;
		}
	}

    public final int getWidth() {
        return mVideoWidth;
    }

    public final int getHeight() {
        return mVideoHeight;
    }

    public final int getBitRate() {
    	return mBitrate;
    }

    public final float getFramerate() {
    	return mFrameRate;
    }

    /**
     * @return 0, 90, 180, 270
     */
    public final int getRotation() {
    	return mRotation;
    }

    /**
     * get duration time as micro seconds
     * @return
     */
    public final long getDurationUs() {
    	return mDuration;
    }

    /**
     * get audio sampling rate[Hz]
     * @return
     */
    public final int getSampleRate() {
    	return mAudioSampleRate;
    }

    public final boolean hasAudio() {
    	return mHasAudio;
    }

	/**
	 * get currently playing or not
	 * @return
	 */
	public final boolean isPlaying() {
		synchronized (mSync) {
			return mState == STATE_PLAYING;
		}
	}

	/**
	 * get currently pausing or not
	 * @return
	 */
	public final boolean isPaused() {
		synchronized (mSync) {
			return mState == STATE_PAUSED;
		}
	}

    /**
     * request to prepare movie playing
     * @param src
     */
    public final void prepare(@NonNull final String src) {
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
	public final void prepare(@NonNull final AssetFileDescriptor src) {
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
	public final void prepare(@NonNull final FileDescriptor src) {
		if (DEBUG) Log.v(TAG, "prepare:");
		synchronized (mSync) {
			mSource = src;
			mRequest = REQ_PREPARE;
			mSync.notifyAll();
		}
	}

    /**
     * request to start playing movie
     * this method can be called after prepare
     */
    public final void play() {
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
    public final void seek(final long newTime) {
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
    public final void stop() {
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
    public final void pause() {
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
    public final void resume() {
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
    public final void release() {
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

//	private static final long EPS = (long)(1 / 240.0f * 1000000);	// 1/240 seconds[micro seconds]

	protected MediaMetadataRetriever mMetadata;
	private final Object mSync = new Object();
	private volatile boolean mIsRunning;
	private int mState;
	private Object mSource;
	private long mDuration;
	private int mRequest;
	private long mRequestTime;
	/**
	 * ループ再生が有効かどうか
	 */
	private volatile boolean mLoopEnabled;
    // for video playback
	private final Object mVideoSync = new Object();
	private final Surface mOutputSurface;
	protected MediaExtractor mVideoMediaExtractor;
	private MediaCodec mVideoMediaCodec;
	private MediaCodec.BufferInfo mVideoBufferInfo;
	private ByteBuffer[] mVideoInputBuffers;
	private ByteBuffer[] mVideoOutputBuffers;
	private long mVideoStartTime;
	private volatile int mVideoTrackIndex;
	private boolean mVideoInputDone;
	private boolean mVideoOutputDone;
	private int mVideoWidth, mVideoHeight;
	private int mBitrate;
	private float mFrameRate;
	private int mRotation;
	// for audio playback
	private final Object mAudioSync = new Object();
	protected MediaExtractor mAudioMediaExtractor;
	private MediaCodec mAudioMediaCodec;
	private MediaCodec.BufferInfo mAudioBufferInfo;
	private ByteBuffer[] mAudioInputBuffers;
	private ByteBuffer[] mAudioOutputBuffers;
	private long mAudioStartTime;
	private volatile int mAudioTrackIndex;
	private boolean mAudioInputDone;
	private boolean mAudioOutputDone;
	private int mAudioChannels;
	private int mAudioSampleRate;
	private int mAudioInputBufSize;
	private boolean mHasAudio;
	private byte[] mAudioOutTempBuf;
	private AudioTrack mAudioTrack;

//--------------------------------------------------------------------------------
	/**
	 * playback control task
	 */
	private final Runnable mMoviePlayerTask = new Runnable() {
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
				for ( ; isRunning ; ) {
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
				} // for (;isRunning;)
			} finally {
				if (DEBUG) Log.v(TAG, "player task finished:isRunning=" + isRunning);
				handleStop();
			}
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * video playback task
	 */
	private final Runnable mVideoTask = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "VideoTask:start");
			for (; mIsRunning && !mVideoInputDone && !mVideoOutputDone ;) {
				try {
			        if (!mVideoInputDone) {
			        	handleInputVideo();
			        }
			        if (!mVideoOutputDone) {
						handleOutputVideo(mCallback);
			        }
				} catch (final Exception e) {
					Log.e(TAG, "VideoTask:", e);
					break;
				}
			} // end of for
			if (DEBUG) Log.v(TAG, "VideoTask:finished");
			synchronized (mSync) {
				mVideoInputDone = mVideoOutputDone = true;
				mSync.notifyAll();
			}
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * audio playback task
	 */
	private final Runnable mAudioTask = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "AudioTask:start");
			for (; mIsRunning && !mAudioInputDone && !mAudioOutputDone ;) {
				try {
			        if (!mAudioInputDone) {
			        	handleInputAudio();
			        }
					if (!mAudioOutputDone) {
						handleOutputAudio(mCallback);
					}
				} catch (final Exception e) {
					Log.e(TAG, "VideoTask:", e);
					break;
				}
			} // end of for
			if (DEBUG) Log.v(TAG, "AudioTask:finished");
			synchronized (mSync) {
				mAudioInputDone = mAudioOutputDone = true;
				mSync.notifyAll();
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
	private final boolean processStop(final int req) throws InterruptedException, IOException {
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
	private final boolean processPrepared(final int req) throws InterruptedException {
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
	private final boolean processPlaying(final int req) {
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
	private final boolean processPaused(final int req) throws InterruptedException {
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

//--------------------------------------------------------------------------------
//
//--------------------------------------------------------------------------------
	/**
	 * @param source
	 * @throws IOException
	 */
	private final void handlePrepare(final Object source) throws IOException {
		if (DEBUG) Log.v(TAG, "handlePrepare:" + source);
        synchronized (mSync) {
			if (mState != STATE_STOP) {
				throw new RuntimeException("invalid state:" + mState);
			}
		}
		mVideoTrackIndex = mAudioTrackIndex = -1;
		if (source instanceof String) {
			final String srcString = (String)source;
			final File src = new File(srcString);
			if (TextUtils.isEmpty(srcString) || !src.canRead()) {
				throw new FileNotFoundException("Unable to read " + source);
			}
			mMetadata = new MediaMetadataRetriever();
			mMetadata.setDataSource((String)source);
		} else if (source instanceof AssetFileDescriptor) {
			mMetadata = new MediaMetadataRetriever();
			mMetadata.setDataSource(((AssetFileDescriptor)source).getFileDescriptor());
		} else if (source instanceof FileDescriptor) {
			mMetadata = new MediaMetadataRetriever();
			mMetadata.setDataSource((FileDescriptor)source);
		} else {
			throw new IllegalArgumentException("unknown source type:source=" + source);
		}
		updateMovieInfo();
		// preparation for video playback
		mVideoTrackIndex = internalPrepareVideo(source);
		// preparation for audio playback
		if (mAudioEnabled) {
			mAudioTrackIndex = internalPrepareAudio(source);
		}
		mHasAudio = mAudioTrackIndex >= 0;
		if ((mVideoTrackIndex < 0) && (mAudioTrackIndex < 0)) {
			throw new RuntimeException("No video and audio track found in " + source);
		}
		synchronized (mSync) {
			mState = STATE_PREPARED;
		}
		mCallback.onPrepared();
	}

	/**
	 * @param source
	 * @return first video track index, -1 if not found
	 */
	@SuppressLint("NewApi")
	protected int internalPrepareVideo(final Object source) throws IOException {
		mVideoMediaExtractor = new MediaExtractor();
		if (source instanceof String) {
			mVideoMediaExtractor.setDataSource((String)source);
		} else if (source instanceof AssetFileDescriptor) {
			if (BuildCheck.isAndroid7()) {
				mVideoMediaExtractor.setDataSource((AssetFileDescriptor)source);
			} else {
				mVideoMediaExtractor.setDataSource(((AssetFileDescriptor)source).getFileDescriptor());
			}
		} else if (source instanceof FileDescriptor) {
			mVideoMediaExtractor.setDataSource((FileDescriptor)source);
		} else {
			// ここには来ないけど
			throw new IllegalArgumentException("unknown source type:source=" + source);
		}
		final int trackIndex = selectTrack(mVideoMediaExtractor, "video/");
		if (trackIndex >= 0) {
			mVideoMediaExtractor.selectTrack(trackIndex);
			final MediaFormat format = mVideoMediaExtractor.getTrackFormat(trackIndex);
			mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
			mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
			mDuration = format.getLong(MediaFormat.KEY_DURATION);

			if (DEBUG) Log.v(TAG, String.format("format:size(%d,%d),duration=%d,bps=%d,framerate=%f,rotation=%d",
				mVideoWidth, mVideoHeight, mDuration, mBitrate, mFrameRate, mRotation));
		}
		return trackIndex;
	}

	/**
	 * @param source
	 * @return first audio track index, -1 if not found
	 */
	@SuppressLint("NewApi")
	protected int internalPrepareAudio(final Object source) throws IOException {
		mAudioMediaExtractor = new MediaExtractor();
		if (source instanceof String) {
			mAudioMediaExtractor.setDataSource((String)source);
		} else if (source instanceof AssetFileDescriptor) {
			if (BuildCheck.isAndroid7()) {
				mAudioMediaExtractor.setDataSource((AssetFileDescriptor)source);
			} else {
				mAudioMediaExtractor.setDataSource(((AssetFileDescriptor)source).getFileDescriptor());
			}
		} else if (source instanceof FileDescriptor) {
			mAudioMediaExtractor.setDataSource((FileDescriptor)source);
		} else {
			// ここには来ないけど
			throw new IllegalArgumentException("unknown source type:source=" + source);
		}
		final int trackIndex = selectTrack(mAudioMediaExtractor, "audio/");
		if (trackIndex >= 0) {
			mAudioMediaExtractor.selectTrack(trackIndex);
			final MediaFormat format = mAudioMediaExtractor.getTrackFormat(trackIndex);
			mAudioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
			mAudioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			final int min_buf_size = AudioTrack.getMinBufferSize(mAudioSampleRate,
				(mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
				AudioFormat.ENCODING_PCM_16BIT);
			final int max_input_size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
			mAudioInputBufSize =  min_buf_size > 0 ? min_buf_size * 4 : max_input_size;
			if (mAudioInputBufSize > max_input_size) mAudioInputBufSize = max_input_size;
			final int frameSizeInBytes = mAudioChannels * 2;
			mAudioInputBufSize = (mAudioInputBufSize / frameSizeInBytes) * frameSizeInBytes;
			if (DEBUG) Log.v(TAG, String.format("getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d",min_buf_size, max_input_size, mAudioInputBufSize));
			//
			mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				mAudioSampleRate,
				(mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
				AudioFormat.ENCODING_PCM_16BIT,
				mAudioInputBufSize,
				AudioTrack.MODE_STREAM);
			try {
				mAudioTrack.play();
			} catch (final Exception e) {
				Log.e(TAG, "failed to start audio track playing", e);
				mAudioTrack.release();
				mAudioTrack = null;
			}
		}
		return trackIndex;
	}

	protected void updateMovieInfo() {
		mVideoWidth = mVideoHeight = mRotation = mBitrate = 0;
		mDuration = 0;
		mFrameRate = 0;
		String value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
		if (!TextUtils.isEmpty(value)) {
			mVideoWidth = Integer.parseInt(value);
		}
		value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
		if (!TextUtils.isEmpty(value)) {
			mVideoHeight = Integer.parseInt(value);
		}
		value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
		if (!TextUtils.isEmpty(value)) {
			mRotation = Integer.parseInt(value);
		}
		value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
		if (!TextUtils.isEmpty(value)) {
			mBitrate = Integer.parseInt(value);
		}
		value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if (!TextUtils.isEmpty(value)) {
			mDuration = Long.parseLong(value) * 1000;
		}
	}

	private final void handleStart() {
    	if (DEBUG) Log.v(TAG, "handleStart:");
		synchronized (mSync) {
			if (mState != STATE_PREPARED)
				throw new RuntimeException("invalid state:" + mState);
			mState = STATE_PLAYING;
		}
        if (mRequestTime > 0) {
        	handleSeek(mRequestTime);
        }
		mVideoInputDone = mVideoOutputDone = true;
		Thread videoThread = null, audioThread = null;
		if (mVideoTrackIndex >= 0) {
			final MediaCodec codec = internalStartVideo(mVideoMediaExtractor, mVideoTrackIndex);
			if (codec != null) {
		        mVideoMediaCodec = codec;
		        mVideoBufferInfo = new MediaCodec.BufferInfo();
		        mVideoInputBuffers = codec.getInputBuffers();	// API>=16, deprecated API21
		        mVideoOutputBuffers = codec.getOutputBuffers();	// API>=16, deprecated API21
			}
			mVideoInputDone = mVideoOutputDone = false;
			videoThread = new Thread(mVideoTask, "VideoTask");
		}
		mAudioInputDone = mAudioOutputDone = true;
		if (mAudioTrackIndex >= 0) {
			final MediaCodec codec = internalStartAudio(mAudioMediaExtractor, mAudioTrackIndex);
			if (codec != null) {
		        mAudioMediaCodec = codec;
		        mAudioBufferInfo = new MediaCodec.BufferInfo();
		        mAudioInputBuffers = codec.getInputBuffers();	// API>=16, deprecated API21
		        mAudioOutputBuffers = codec.getOutputBuffers();	// API>=16, deprecated API21
			}
			mAudioInputDone = mAudioOutputDone = false;
	        audioThread = new Thread(mAudioTask, "AudioTask");
		}
		if (videoThread != null) videoThread.start();
		if (audioThread != null) audioThread.start();
	}

	/**
	 * @param media_extractor
	 * @param trackIndex
	 * @return
	 */
	protected MediaCodec internalStartVideo(final MediaExtractor media_extractor, final int trackIndex) {
		if (DEBUG) Log.v(TAG, "internalStartVideo:");
		MediaCodec codec = null;
		if (trackIndex >= 0) {
	        final MediaFormat format = media_extractor.getTrackFormat(trackIndex);
	        final String mime = format.getString(MediaFormat.KEY_MIME);
			try {
				codec = MediaCodec.createDecoderByType(mime);
				codec.configure(format, mOutputSurface, null, 0);
		        codec.start();
				if (DEBUG) Log.v(TAG, "internalStartVideo:codec started");
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		return codec;
	}

	/**
	 * @param media_extractor
	 * @param trackIndex
	 * @return
	 */
	protected MediaCodec internalStartAudio(final MediaExtractor media_extractor, final int trackIndex) {
		if (DEBUG) Log.v(TAG, "internalStartAudio:");
		MediaCodec codec = null;
		if (trackIndex >= 0) {
	        final MediaFormat format = media_extractor.getTrackFormat(trackIndex);
	        final String mime = format.getString(MediaFormat.KEY_MIME);
			try {
				codec = MediaCodec.createDecoderByType(mime);
				codec.configure(format, null, null, 0);
		        codec.start();
		    	if (DEBUG) Log.v(TAG, "internalStartAudio:codec started");
		    	//
		        final ByteBuffer[] buffers = codec.getOutputBuffers();
		        int sz = buffers[0].capacity();
		        if (sz <= 0) {
		        	sz = mAudioInputBufSize;
				}
		        if (DEBUG) Log.v(TAG, "AudioOutputBufSize:" + sz);
		        mAudioOutTempBuf = new byte[sz];
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		return codec;
	}

	private final void handleSeek(final long newTime) {
        if (DEBUG) Log.d(TAG, "handleSeek");
		if (newTime < 0) return;

		if (mVideoTrackIndex >= 0) {
			mVideoMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
	        mVideoMediaExtractor.advance();
		}
		if (mAudioTrackIndex >= 0) {
			mAudioMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
	        mAudioMediaExtractor.advance();
		}
        mRequestTime = -1;
	}

	private final void handleLoop(final IFrameCallback frameCallback) {
//		if (DEBUG) Log.d(TAG, "handleLoop");

		synchronized (mSync) {
			try {
				mSync.wait();
			} catch (final InterruptedException e) {
				// ignore
			}
		}
        if (mVideoInputDone && mVideoOutputDone && mAudioInputDone && mAudioOutputDone) {
            if (DEBUG) Log.d(TAG, "Reached EOS, looping check");
            // FIXME 未実装 ループ再生のチェック
        	handleStop();
        }
	}

	/**
	 * @param codec
	 * @param extractor
	 * @param inputBuffers
	 * @param presentationTimeUs
	 * @param isAudio
	 */
	protected boolean internalProcessInput(final MediaCodec codec, final MediaExtractor extractor, final ByteBuffer[] inputBuffers, final long presentationTimeUs, final boolean isAudio) {
//		if (DEBUG) Log.v(TAG, "internalProcessInput:presentationTimeUs=" + presentationTimeUs);
		boolean result = true;
		while (mIsRunning) {
            final int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
            	break;
            if (inputBufIndex >= 0) {
                final int size = extractor.readSampleData(inputBuffers[inputBufIndex], 0);
                if (size > 0) {
                	codec.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
                }
            	result = extractor.advance();	// return false if no data is available
                break;
            }
		}
		return result;
	}

	private final void handleInputVideo() {
    	final long presentationTimeUs = mVideoMediaExtractor.getSampleTime();
/*		if (presentationTimeUs < previousVideoPresentationTimeUs) {
    		presentationTimeUs += previousVideoPresentationTimeUs - presentationTimeUs; // + EPS;
    	}
    	previousVideoPresentationTimeUs = presentationTimeUs; */
        final boolean b = internalProcessInput(mVideoMediaCodec, mVideoMediaExtractor, mVideoInputBuffers,
        		presentationTimeUs, false);
        if (!b) {
        	if (DEBUG) Log.i(TAG, "video track input reached EOS");
    		while (mIsRunning) {
                final int inputBufIndex = mVideoMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                	mVideoMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                		MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                	if (DEBUG) Log.v(TAG, "sent input EOS:" + mVideoMediaCodec);
                	break;
                }
        	}
    		synchronized (mSync) {
    			mVideoInputDone = true;
    			mSync.notifyAll();
    		}
        }
	}
	/**
	 * @param frameCallback
	 */
	private final void handleOutputVideo(final IFrameCallback frameCallback) {
//    	if (DEBUG) Log.v(TAG, "handleOutputVideo:");
		while (mIsRunning && !mVideoOutputDone) {
			final int decoderStatus = mVideoMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
			if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				return;
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				mVideoOutputBuffers = mVideoMediaCodec.getOutputBuffers();
				if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				final MediaFormat newFormat = mVideoMediaCodec.getOutputFormat();
				if (DEBUG) Log.d(TAG, "video decoder output format changed: " + newFormat);
			} else if (decoderStatus < 0) {
				throw new RuntimeException(
					"unexpected result from video decoder.dequeueOutputBuffer: " + decoderStatus);
			} else { // decoderStatus >= 0
				boolean doRender = false;
				if (mVideoBufferInfo.size > 0) {
					doRender = (mVideoBufferInfo.size != 0)
						&& !internalWriteVideo(mVideoOutputBuffers[decoderStatus],
							0, mVideoBufferInfo.size, mVideoBufferInfo.presentationTimeUs);
					if (doRender) {
						if (!frameCallback.onFrameAvailable(mVideoBufferInfo.presentationTimeUs))
							mVideoStartTime = adjustPresentationTime(mVideoSync, mVideoStartTime, mVideoBufferInfo.presentationTimeUs);
					}
				}
				mVideoMediaCodec.releaseOutputBuffer(decoderStatus, doRender);
				if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					if (DEBUG) Log.d(TAG, "video:output EOS");
					synchronized (mSync) {
						mVideoOutputDone = true;
						mSync.notifyAll();
					}
				}
			}
		}
	}

	/**
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 */
	protected boolean internalWriteVideo(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
//		if (DEBUG) Log.v(TAG, "internalWriteVideo");
		// 映像の場合はSurfaceへ出力しているので特にここですることはない
		return false;
	}

	private final void handleInputAudio() {
		final long presentationTimeUs = mAudioMediaExtractor.getSampleTime();
/*		if (presentationTimeUs < previousAudioPresentationTimeUs) {
    		presentationTimeUs += previousAudioPresentationTimeUs - presentationTimeUs; //  + EPS;
    	}
    	previousAudioPresentationTimeUs = presentationTimeUs; */
        final boolean b = internalProcessInput(mAudioMediaCodec, mAudioMediaExtractor, mAudioInputBuffers,
        		presentationTimeUs, true);
        if (!b) {
        	if (DEBUG) Log.i(TAG, "audio track input reached EOS");
    		while (mIsRunning) {
                final int inputBufIndex = mAudioMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                	mAudioMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                		MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                	if (DEBUG) Log.v(TAG, "sent input EOS:" + mAudioMediaCodec);
                	break;
                }
        	}
    		synchronized (mSync) {
    			mAudioInputDone = true;
    			mSync.notifyAll();
    		}
       }
	}

	private final void handleOutputAudio(final IFrameCallback frameCallback) {
//		if (DEBUG) Log.v(TAG, "handleOutputAudio:");
		while (mIsRunning && !mAudioOutputDone) {
			final int decoderStatus = mAudioMediaCodec.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
			if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				return;
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				mAudioOutputBuffers = mAudioMediaCodec.getOutputBuffers();
				if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				final MediaFormat newFormat = mAudioMediaCodec.getOutputFormat();
				if (DEBUG) Log.d(TAG, "audio decoder output format changed: " + newFormat);
			} else if (decoderStatus < 0) {
				throw new RuntimeException(
					"unexpected result from audio decoder.dequeueOutputBuffer: " + decoderStatus);
			} else { // decoderStatus >= 0
				if (mAudioBufferInfo.size > 0) {
					internalWriteAudio(mAudioOutputBuffers[decoderStatus],
						0, mAudioBufferInfo.size, mAudioBufferInfo.presentationTimeUs);
					if (!frameCallback.onFrameAvailable(mAudioBufferInfo.presentationTimeUs))
						mAudioStartTime = adjustPresentationTime(mAudioSync, mAudioStartTime, mAudioBufferInfo.presentationTimeUs);
				}
				mAudioMediaCodec.releaseOutputBuffer(decoderStatus, false);
				if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					if (DEBUG) Log.d(TAG, "audio:output EOS");
					synchronized (mSync) {
						mAudioOutputDone = true;
						mSync.notifyAll();
					}
				}
			}
		}
	}

	/**
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 * @return ignored
	 */
	protected boolean internalWriteAudio(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
//		if (DEBUG) Log.d(TAG, "internalWriteAudio");
        if (mAudioOutTempBuf.length < size) {
        	mAudioOutTempBuf = new byte[size];
        }
        buffer.position(offset);
        buffer.get(mAudioOutTempBuf, 0, size);
        buffer.clear();
        if (mAudioTrack != null)
        	mAudioTrack.write(mAudioOutTempBuf, 0, size);
        return true;
	}

	/**
	 * adjusting frame rate
	 * @param sync
	 * @param startTime
	 * @param presentationTimeUs
	 * @return startTime
	 */
	protected long adjustPresentationTime(final Object sync, final long startTime, final long presentationTimeUs) {
		if (startTime > 0) {
			for (long t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime);
				 t > 0; t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime)) {
				synchronized (sync) {
					try {
						sync.wait(t / 1000, (int)((t % 1000) * 1000));
					} catch (final InterruptedException e) {
						break;
					}
					if ((mState == REQ_STOP) || (mState == REQ_QUIT))
						break;
				}
			}
			return startTime;
		} else {
			return Time.nanoTime() / 1000;
		}
	}

	private final void handleStop() {
    	if (DEBUG) Log.v(TAG, "handleStop:");
    	synchronized (mVideoTask) {
    		internalStopVideo();
    		mVideoTrackIndex = -1;
    	}
    	synchronized (mAudioTask) {
    		internalStopAudio();
    		mAudioTrackIndex = -1;
    	}
    	if (mVideoMediaCodec != null) {
    		mVideoMediaCodec.stop();
    		mVideoMediaCodec.release();
    		mVideoMediaCodec = null;
    	}
    	if (mAudioMediaCodec != null) {
    		mAudioMediaCodec.stop();
    		mAudioMediaCodec.release();
    		mAudioMediaCodec = null;
    	}
		if (mVideoMediaExtractor != null) {
			mVideoMediaExtractor.release();
			mVideoMediaExtractor = null;
		}
		if (mAudioMediaExtractor != null) {
			mAudioMediaExtractor.release();
			mAudioMediaExtractor = null;
		}
        mVideoBufferInfo = mAudioBufferInfo = null;
        mVideoInputBuffers = mVideoOutputBuffers = null;
        mAudioInputBuffers = mAudioOutputBuffers = null;
		if (mMetadata != null) {
			mMetadata.release();
			mMetadata = null;
		}
		synchronized (mSync) {
			mVideoOutputDone = mVideoInputDone = mAudioOutputDone = mAudioInputDone = true;
			mState = STATE_STOP;
		}
		mCallback.onFinished();
	}

	protected void internalStopVideo() {
		if (DEBUG) Log.v(TAG, "internalStopVideo:");
	}

	protected void internalStopAudio() {
		if (DEBUG) Log.v(TAG, "internalStopAudio:");
    	if (mAudioTrack != null) {
    		if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED)
    			mAudioTrack.stop();
    		mAudioTrack.release();
    		mAudioTrack = null;
    	}
		mAudioOutTempBuf = null;
	}

	private final void handlePause() {
    	if (DEBUG) Log.v(TAG, "handlePause:");
    	// FIXME unimplemented yet
	}

	private final void handleResume() {
    	if (DEBUG) Log.v(TAG, "handleResume:");
    	// FIXME unimplemented yet
	}

    /**
     * search first track index matched specific MIME
     * @param extractor
     * @param mimeType "video/" or "audio/"
     * @return track index, -1 if not found
     */
    protected static final int selectTrack(final MediaExtractor extractor, final String mimeType) {
        final int numTracks = extractor.getTrackCount();
        MediaFormat format;
        String mime;
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                if (DEBUG) {
                    Log.d(TAG_STATIC, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }
}
