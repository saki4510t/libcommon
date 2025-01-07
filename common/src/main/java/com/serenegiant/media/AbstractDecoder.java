package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * MediaCodecのデコーダーのラッパー用基本クラス
 */
public abstract class AbstractDecoder implements Decoder {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AbstractDecoder.class.getSimpleName();

    public static final int TIMEOUT_USEC = 10000;	// 10ミリ秒

	@NonNull
	protected final Object mSync = new Object();
	@NonNull
	private final String mMimeType;
	@NonNull
	protected final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	@Nullable
	private MediaFormat mFormat;
	@Nullable
	protected MediaCodec mDecoder;
	private long mStartTimeNs;
	private volatile int mTrackIndex;
	private volatile boolean mIsRunning;
	protected boolean mOutputDone;

	/**
	 * コンストラクタ
	 * @param mimeType
	 * @param listener
	 */
    protected AbstractDecoder(
		@NonNull final String mimeType,
		@NonNull final DecoderListener listener) {
		mMimeType = mimeType;
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
	 * @param extractor String, AssetFileDescriptor, FileDescriptorのいずれか
	 * XXX 映像と音声の両方に同じFileDescriptorを渡すと途中でMPEG4Extractor: Video is malformedとlogCatへ出力されて再生が止まってしまうことがある
	 * @return トラックインデックス
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	public int prepare(@NonNull final MediaExtractor extractor) throws IllegalArgumentException {
		mTrackIndex = findTrack(extractor, mMimeType);
		if (mTrackIndex >= 0) {
			extractor.selectTrack(mTrackIndex);
			mFormat = extractor.getTrackFormat(mTrackIndex);
			prepare(mTrackIndex, mFormat);
		} else {
			throw new IllegalArgumentException("Track not found for " + mMimeType);
		}
		return mTrackIndex;
	}

	/**
	 * デコードの準備処理
	 * @param trackIndex
	 * @param format
	 */
	public abstract void prepare(final int trackIndex, @NonNull final MediaFormat format);

	/**
	 * デコーダー生成とデコード開始処理
	 */
	public void start() {
		if (DEBUG) Log.v(TAG, "start:trackIx=" + mTrackIndex);
		if (mTrackIndex >= 0) {
	        final String mime = mFormat.getString(MediaFormat.KEY_MIME);
			try {
				mDecoder = createDecoder(mTrackIndex, mFormat);
				mOutputDone = false;
				mIsRunning = true;
				final Thread outputThread = new Thread(createOutputTask(mTrackIndex), TAG + "-" + this.hashCode());
				synchronized (mSync) {
					outputThread.start();
					try {
						// 出力スレッドの開始待ち
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
	protected abstract OutputTask createOutputTask(final int trackIndex);

	public abstract void decode(@NonNull final MediaExtractor extractor);

	public void stop() {
		mIsRunning = false;
		synchronized (mSync) {
			mSync.notifyAll();
		}
	}

	public void signalEndOfStream() {
		if (DEBUG) Log.i(TAG, "signalEndOfStream:");
		while (isRunning()) {
			final int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufIndex >= 0) {
				mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
					MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				if (DEBUG) Log.v(TAG, "signalEndOfStream:sent input EOS:" + mDecoder);
				break;
			}
		}
		synchronized (mSync) {
			mSync.notifyAll();
		}
	}

	/**
	 * MediaCodecのデコーダーへの入力処理, API>=16&API<21用
	 * @param extractor
	 * @param decoder
	 */
	protected void decodeAPI16(
		@NonNull final MediaExtractor extractor,
		@NonNull final MediaCodec decoder,
		final ByteBuffer[] inputBuffers) {

		while (isRunning()) {
			final int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufIndex >= 0) {
				final int size = extractor.readSampleData(inputBuffers[inputBufIndex], 0);
				if (size > 0) {
					final long presentationTimeUs = extractor.getSampleTime();
					decoder.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
				} else {
					// 念のためにsize<=0ならEOSを送る
					mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
						MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				}
				break;
			}
		}
	}

	/**
	 * MediaCodecのデコーダーへの入力処理, API>=21用
	 * @param extractor
	 * @param decoder
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	protected void decodeAPI21(
		@NonNull final MediaExtractor extractor,
		@NonNull final MediaCodec decoder) {

		while (isRunning()) {
			final int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufIndex >= 0) {
				final ByteBuffer in = decoder.getInputBuffer(inputBufIndex);
				final int size = extractor.readSampleData(in, 0);
				if (size > 0) {
					final long presentationTimeUs = extractor.getSampleTime();
					decoder.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
				} else {
					// 念のためにsize<=0ならEOSを送る
					mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
						MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				}
				break;
			}
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
	protected abstract class OutputTask implements Runnable {
		protected final int trackIndex;

		protected OutputTask(final int trackIndex) {
			this.trackIndex = trackIndex;
		}

		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "DecodeTask:start");
			synchronized (mSync) {
				// 出力スレッドが起床したことを通知
				mSync.notify();
			}
			while ( mIsRunning && !mOutputDone ) {
				try {
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
				mOutputDone = true;
				mSync.notifyAll();
			}
		}

		/**
		 * デコーダーからの出力を受け取る処理
		 * @param decoder
		 */
		protected abstract void handleOutput(@NonNull final MediaCodec decoder);

		/*
		 * API21以降で使用可能なMediaCodec#releaseOutputBuffer(int,long)は再生したいシステム時刻から
		 * vsync x 2早く(通常の60fpsディスプレーであれば約33ミリ秒早く)#releaseOutputBufferを呼び出すと
		 * 最適なパフォーマンスと品質が得られるらしいのでptsを調整する。
		 * #releaseOutputBufferを呼ぶときのptsがシステム時刻と大きく離れている時は無視されて、一番早く表示可能な
		 * タイミング表示される、この場合にはフレームがドロップすることはないらしい。
		 * でも#releaseOutputBuffer(int,long)へ調整したptsを渡すだけではだめで自前でウエイトを入れないとだめっぽい
		 */
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
