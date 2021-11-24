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
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * MediaCodecのデコーダーのラッパー用基本クラス
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractDecoder implements Decoder {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = AbstractDecoder.class.getSimpleName();

    public static final int TIMEOUT_USEC = 10000;	// 10ミリ秒

	@NonNull
	protected final Object mSync = new Object();
	@NonNull
	private final String mMimeType;
	@NonNull
	protected final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	@NonNull
	private final MediaExtractor mExtractor;
	@NonNull
	private final MediaMetadataRetriever mMetadata = new MediaMetadataRetriever();	// API>=10
	@Nullable
	private MediaCodec mDecoder;
	private long mStartTimeNs;
	private volatile int mTrackIndex;
	private volatile boolean mIsRunning;
	protected boolean mInputDone;
	protected boolean mOutputDone;
	protected boolean mRequestSeek;
	protected long mRequestTimeUs;

	/**
	 * コンストラクタ
	 * @param mimeType
	 * @param listener
	 */
    protected AbstractDecoder(
		@NonNull final String mimeType,
		@NonNull final DecoderListener listener) {
		mMimeType = mimeType;
		mExtractor = new MediaExtractor();
    }

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	@Override
	public void release() {
		stop();
		// FIXME 未実装
	}

	/**
	 * デコード実行中かどうかを取得
	 * @return
	 */
	public boolean isRunning() {
		return mIsRunning;
	}

	/**
	 * デコードの準備
	 * @param source String, AssetFileDescriptor, FileDescriptorのいずれか
	 * XXX 映像と音声の両方に同じFileDescriptorを渡すと途中でMPEG4Extractor: Video is malformedとlogCatへ出力されて再生が止まってしまうことがある
	 * @return トラックインデックス
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	public int prepare(@NonNull final Object source) throws IOException, IllegalArgumentException {
		if (source instanceof String) {
			final String srcString = (String)source;
			final File src = new File(srcString);
			if (TextUtils.isEmpty(srcString) || !src.canRead()) {
				throw new FileNotFoundException("Unable to read " + source);
			}
			mMetadata.setDataSource((String)source);	// API>=10
			mExtractor.setDataSource((String)source);	// API>=16
		} else if (source instanceof AssetFileDescriptor) {
			final FileDescriptor fd = ((AssetFileDescriptor)source).getFileDescriptor();
			mMetadata.setDataSource(fd);
			if (BuildCheck.isAndroid7()) {
				mExtractor.setDataSource((AssetFileDescriptor)source);	// API>=24
			} else {
				mExtractor.setDataSource(fd);			// API>=16
			}
		} else if (source instanceof FileDescriptor) {
			mMetadata.setDataSource((FileDescriptor)source);	// API>=10
			mExtractor.setDataSource((FileDescriptor)source);	// API>=16
		} else {
			// ここには来ないけど
			throw new IllegalArgumentException("unknown source type:source=" + source);
		}
		updateInfo(mMetadata);
		mTrackIndex = findTrack(mExtractor, mMimeType);
		if (mTrackIndex >= 0) {
			mExtractor.selectTrack(mTrackIndex);
			final MediaFormat format = mExtractor.getTrackFormat(mTrackIndex);
			internalPrepare(mTrackIndex, format);
		} else {
			throw new IllegalArgumentException("Track not found for " + mMimeType);
		}
		return mTrackIndex;
	}

	/**
	 * 動画ファイルの情報を取得する
	 * @param metaData
	 */
	protected abstract void updateInfo(@NonNull final MediaMetadataRetriever metaData);

	/**
	 * デコードの準備処理
	 * @param trackIndex
	 * @param format
	 */
	protected abstract void internalPrepare(final int trackIndex, @NonNull final MediaFormat format);

	/**
	 * デコーダー生成とデコード開始処理
	 */
	public void start() {
		if (DEBUG) Log.v(TAG, "start:trackIx=" + mTrackIndex);
		if (mTrackIndex >= 0) {
	        final MediaFormat format = mExtractor.getTrackFormat(mTrackIndex);
	        final String mime = format.getString(MediaFormat.KEY_MIME);
			try {
				mDecoder = createDecoder(mTrackIndex, format);
				mInputDone = mOutputDone = mRequestSeek = false;
				mIsRunning = true;
				final Thread decoderThread = new Thread(createOutputTask(), TAG + "-" + this.hashCode());
				synchronized (mSync) {
					decoderThread.start();
					try {
						// 出力スレッドの開始町
						mSync.wait(1000);
					} catch (final InterruptedException e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
				if (DEBUG) Log.v(TAG, "start:codec started");
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * MediaCodecのデコーダーを生成
	 * @param trackIndex
	 * @param format
	 * @return
	 * @throws IOException
	 */
	protected abstract MediaCodec createDecoder(final int trackIndex, @NonNull final MediaFormat format) throws IOException;

	/**
	 * 出力用スレッドの実行部を生成
	 * @return
	 */
	protected abstract DecodeTask createOutputTask();

	public void stop() {
		mIsRunning = false;
		synchronized (mSync) {
			mSync.notifyAll();
		}
	}

	/**
  * request to seek to specifc timed frame<br>
  * if the frame is not a key frame, frame image will be broken
  * @param newTimeUs seek to new time[usec]
  */
 public final void seek(final long newTimeUs) {
 	if (DEBUG) Log.v(TAG, "seek");
 	synchronized (mSync) {
		mRequestSeek = true;
 		mRequestTimeUs = newTimeUs;
 		mSync.notifyAll();
 	}
 }

	/**
	 * called every frame before time adjusting
	 * return true if you don't want to use internal time adjustment
	 */
	protected boolean onFrameAvailable(final long presentationTimeUs) {
		return false;
	}

	/**
	 * デコードスレッドの実行部の基本クラス
	 */
	protected abstract class DecodeTask implements Runnable {
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "DecodeTask:start");
			synchronized (mSync) {
				// 出力スレッドが起床したことを通知
				mExtractor.advance();
				mSync.notify();
			}
			while ( mIsRunning && !mInputDone && !mOutputDone ) {
				try {
					final boolean requestSeek;
					final long requestTimeUs;
					synchronized (mSync) {
						requestTimeUs = mRequestTimeUs;
						requestSeek = mRequestSeek;
						mRequestSeek = false;
					}
					if (requestSeek) {
						handleSeek(requestTimeUs);
					}
			        if (!mInputDone) {
						handleInput(mExtractor, mTrackIndex, mDecoder);
			        }
			        if (!mOutputDone) {
						handleOutput(mDecoder);
			        }
				} catch (final Exception e) {
					Log.e(TAG, "DecodeTask:", e);
					break;
				}
			} // end of while
			if (DEBUG) Log.v(TAG, "DecodeTask:finished");
			synchronized (mSync) {
				mInputDone = mOutputDone = true;
				mSync.notifyAll();
			}
		}

		/**
		 * 再生位置の変更処理
		 * @param requestTimeUs
		 */
		protected void handleSeek(final long requestTimeUs) {
			if (mTrackIndex >= 0) {
				mExtractor.seekTo(requestTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
				mExtractor.advance();
			}
		}

		/**
		 * デコーダーへの入力処理
		 * @param extractor
		 * @param decoder
		 */
		protected abstract void handleInput(
			@NonNull final MediaExtractor extractor, final int targetTrackIndex,
			@NonNull final MediaCodec decoder);

		/**
		 * デコーダーからの出力を受け取る処理
		 * @param decoder
		 */
		protected abstract void handleOutput(@NonNull final MediaCodec decoder);

		/**
		 * 音声用と映像用の映像入力処理は同じなのでヘルパーメソッドを作っておく, API>=16&API<21用
		 * @param extractor
		 * @param decoder
		 * @param inputBuffers
		 */
		protected void handleInputAPI16(
			@NonNull final MediaExtractor extractor,
			final int targetTrackIndex,
			@NonNull final MediaCodec decoder,
			@NonNull final ByteBuffer[] inputBuffers) {

			boolean b = true;
			while (isRunning()) {
				final int trackIx = extractor.getSampleTrackIndex();
				final long presentationTimeUs = extractor.getSampleTime();	// return -1 if no data is available
				if ((presentationTimeUs > 0) && (targetTrackIndex == trackIx)) {
					final int inputBufIndex = decoder.dequeueInputBuffer(0);
					if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						break;
					} else if (inputBufIndex >= 0) {
						final int size = extractor.readSampleData(inputBuffers[inputBufIndex], 0);
						if (size > 0) {
							decoder.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
						}
						b = extractor.advance();    // return false if no data is available
						break;
					}
				} else {
					b = extractor.advance();    // return false if no data is available
					break;
				}
			}
			if (!b) {
				if (DEBUG) Log.i(TAG, "input reached EOS");
				while (isRunning()) {
					final int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
					if (inputBufIndex >= 0) {
						decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
							MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						if (DEBUG) Log.v(TAG, "sent input EOS:" + decoder);
						break;
					}
				}
				synchronized (mSync) {
					mInputDone = true;
					mSync.notifyAll();
				}
			}
		}

		/**
		 * 音声用と映像用の映像入力処理は同じなのでヘルパーメソッドを作っておく, API>=21用
		 * @param extractor
		 * @param decoder
		 */
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		protected void handleInputAPI21(
			@NonNull final MediaExtractor extractor,
			final int targetTrackIndex,
			@NonNull final MediaCodec decoder) {
			boolean b = true;
			while (isRunning()) {
				final int trackIx = extractor.getSampleTrackIndex();
				final long presentationTimeUs = extractor.getSampleTime();	// return -1 if no data is available
				if ((presentationTimeUs > 0) && (targetTrackIndex == trackIx)) {
					final int inputBufIndex = decoder.dequeueInputBuffer(0);
					if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						break;
					} else if (inputBufIndex >= 0) {
						final ByteBuffer in = decoder.getInputBuffer(inputBufIndex);
						in.clear();
						final int size = extractor.readSampleData(in, 0);
						if (size > 0) {
							decoder.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
						}
						b = extractor.advance();    // return false if no data is available
						break;
					}
				} else {
					b = extractor.advance();    // return false if no data is available
					break;
				}
			}
			if (!b) {
				if (DEBUG) Log.i(TAG, "input reached EOS");
				while (isRunning()) {
					final int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
					if (inputBufIndex >= 0) {
						decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
							MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						if (DEBUG) Log.v(TAG, "sent input EOS:" + decoder);
						break;
					}
				}
				synchronized (mSync) {
					mInputDone = true;
					mSync.notifyAll();
				}
			}
		}

		private static final long VSYNC2 = 33330000;		// 33.33ミリ秒, approx. 2 frames @ 60fps
		private long mOffsetPtsNs = -1L;
		private long mOffsetSysTimeNs = -1L;

		/**
		 * 最初のフレームのpresentationTimeUsとシステム時間を保存しておいて
		 * 現在のフレームのpresentationTimeUsと現在時刻から描画予定時刻を計算して
		 * そのフレームの描画予定時刻まで待機する
		 * @param presentationTimeUs
		 */
		protected long adjustPresentationTime(
			final long presentationTimeUs) {

			final long presentationTimeNs = presentationTimeUs * 1000L;
			if (mOffsetSysTimeNs <= 0) {
				// 初回
				mOffsetSysTimeNs = System.nanoTime();
				mOffsetPtsNs = mOffsetSysTimeNs - presentationTimeNs;
				return mOffsetSysTimeNs + VSYNC2;
			} else {
				// 2回目以降
				// 現在のptsから最初のptsを引いたのが再生位置、そこから最初のシステム時間と現在システム時間の差を引いたのが待ち時間
				final long base = mOffsetPtsNs + presentationTimeNs - VSYNC2;
				for (long t = base - System.nanoTime();
					isRunning() && (t > 0); t = base - System.nanoTime()) {

					if (t > 20000000) t >>= 1;	// 20ミリ以上なら1/2にする
					synchronized (mSync) {
						try {
							mSync.wait(t / 1000000, (int)(t % 1000000));
						} catch (final InterruptedException e) {
							// ignore
						}
					}
				}
				return System.nanoTime() + VSYNC2;
			}
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * search first track index matched specific MIME
	 *
	 * @param extractor
	 * @param mimeType  "video/" or "audio/"
	 * @return track index, -1 if not found
	 */
	private static final int findTrack(@NonNull final MediaExtractor extractor, @NonNull final String mimeType) {
		final int numTracks = extractor.getTrackCount();
		for (int i = 0; i < numTracks; i++) {
			final MediaFormat format = extractor.getTrackFormat(i);
			final String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith(mimeType)) {
				if (DEBUG) Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
				return i;
			}
		}
		return -1;
	}
}
