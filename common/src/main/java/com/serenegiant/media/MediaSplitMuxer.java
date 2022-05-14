package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2016-2022 saki t_saki@serenegiant.com
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
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.serenegiant.system.StorageInfo;
import com.serenegiant.system.StorageUtils;
import com.serenegiant.system.Time;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * 指定したファイルサイズになるように自動分割してMP4へ出力するためのIMuxer実装
 * Iフレームが来たときにしか出力ファイルを切り替えることができないため
 * 確実に指定ファイルサイズ以下になるわけではないので、多少の余裕をもって
 * 出力ファイルサイズをセットすること
 *
 * FIXME これは今のところAPI29/Android10以降の対象範囲別ストレージでは動かない(SAF経由なら動く)
 */
@Deprecated
public class MediaSplitMuxer implements IMuxer {
	private static final boolean DEBUG = false; // FIXME set false on production
	@SuppressWarnings("deprecation")
	private static final String TAG = MediaSplitMuxer.class.getSimpleName();
	
	/**
	 * セグメント名のプレフィックス文字列のデフォルト
	 */
	public static final String DEFAULT_PREFIX_SEGMENT_NAME = "-";
	/**
	 * セグメント名のプレフィックス文字列, コンストラクタで読み込む
	 */
	public static String PREFIX_SEGMENT_NAME = DEFAULT_PREFIX_SEGMENT_NAME;

	private static final int INI_POOL_NUM = 4;
	private static final int MAX_POOL_NUM = 1000;
	private static final long DEFAULT_SPLIT_SIZE = 4000000000L;
	private static final String EXT_MP4 = "mp4";
	
	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	/**
	 * MediaCodecのエンコーダーの設定
	 * 最終のmp4ファイル出力時に必要なため保持しておく
	 */
	private final MediaFormat[] mMediaFormats = new MediaFormat[2];
	private int mVideoTrackIx = -1;
	private int mAudioTrackIx = -1;
	/**
	 * mp4ファイルの出力ディレクトリ(絶対パス文字列)
	 */
	@Nullable
	private final String mOutputDir;
	/**
	 * mp4ファイルの出力ディレクトリ(DocumentFile)
	 */
	@Nullable
	private final DocumentFile mOutputDoc;
	/**
	 * 最終出力ファイル名 = 一時ファイルを保存するディレクトリ名
	 * 	= インスタンス生成時の日時文字列
	 */
	@NonNull
	private final String mOutputName;
	@NonNull
	private final String mSegmentPrefix;
	@NonNull
	private final IMediaQueue<RecycleMediaData> mQueue;
	private final long mSplitSize;
	@NonNull
	private final IMuxerFactory mMuxerFactory;
	@NonNull
	private final VideoConfig mVideoConfig;
	private DocumentFile mCurrent;
	/** 実行中フラグ */
	private volatile boolean mIsRunning;
	private volatile boolean mRequestStop;
	private boolean mReleased;
	private int mLastTrackIndex = -1;
	@Nullable
	private IMuxer mMuxer;
	@Nullable
	private MuxTask mMuxTask;
	
	/**
	 * コンストラクタ
	 * キューはMemMediaQueueを使う
	 * @param context
	 * @param outputDir 最終出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @param splitSize 出力ファイルサイズの目安, 0以下ならデフォルト値
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public MediaSplitMuxer(@NonNull final Context context,
		@NonNull final String outputDir, @NonNull final String name,
		final long splitSize) throws IOException {
		
		this(context, null, null,null,
			outputDir, name, splitSize);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param queue バッファリング用IMediaQueue
	 * @param outputDir 最終出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @param splitSize 出力ファイルサイズの目安, 0以下ならデフォルト値
	 */
	@Deprecated
	public MediaSplitMuxer(@NonNull final Context context,
		@Nullable final VideoConfig config,
		@Nullable final IMuxerFactory factory,
		@Nullable final IMediaQueue<RecycleMediaData> queue,
		@NonNull final String outputDir, @NonNull final String name,
		final long splitSize) throws IOException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<Context>(context);
		mVideoConfig = config != null ? config : new VideoConfig();
		mMuxerFactory = factory != null ? factory : new DefaultFactory();
		mQueue = queue != null
			? queue : new MemMediaQueue(INI_POOL_NUM, MAX_POOL_NUM);
		mOutputDir = outputDir;
		mOutputDoc = null;
		mOutputName = name;
		mSplitSize = splitSize <= 0 ? DEFAULT_SPLIT_SIZE : splitSize;
		mSegmentPrefix = PREFIX_SEGMENT_NAME != null
			? PREFIX_SEGMENT_NAME : DEFAULT_PREFIX_SEGMENT_NAME;
		mMuxer = createMuxer(0);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param outputDir 最終出力ディレクトリ
	 * @param name 出つ力ファイル名(拡張子なし)
	 * @param splitSize 出力ファイルサイズの目安, 0以下ならデフォルト値
	 */
	public MediaSplitMuxer(@NonNull final Context context,
		@NonNull final DocumentFile outputDir, @NonNull final String name,
		final long splitSize) throws IOException {

		this(context, null, null, null,
			outputDir, name, splitSize);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param queue バッファリング用IMediaQueue
	 * @param outputDir 最終出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @param splitSize 出力ファイルサイズの目安, 0以下ならデフォルト値
	 */
	public MediaSplitMuxer(@NonNull final Context context,
		@Nullable final VideoConfig config,
		@Nullable final IMuxerFactory factory,
		@Nullable final IMediaQueue<RecycleMediaData> queue,
		@NonNull final DocumentFile outputDir, @NonNull final String name,
		final long splitSize) throws IOException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<Context>(context);
		mVideoConfig = config != null ? config : new VideoConfig();
		mMuxerFactory = factory != null ? factory : new DefaultFactory();
		mQueue = queue != null
			? queue : new MemMediaQueue(INI_POOL_NUM, MAX_POOL_NUM);
		mOutputDir = null;
		mOutputDoc = outputDir;
		mOutputName = name;
		mSplitSize = splitSize <= 0 ? DEFAULT_SPLIT_SIZE : splitSize;
		mSegmentPrefix = PREFIX_SEGMENT_NAME != null
			? PREFIX_SEGMENT_NAME : DEFAULT_PREFIX_SEGMENT_NAME;
		mMuxer = createMuxer(0);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * 関連するリソースを破棄する
	 */
	@Override
	public void release() {
		synchronized (mSync) {
			if (!mReleased) {
				mReleased = true;
				if (DEBUG) Log.v(TAG, "release:");
				if (mIsRunning && !mRequestStop) {
					stop();
				}
				mIsRunning = false;
				mQueue.clear();
				if (DEBUG) Log.v(TAG, "release:finished");
			}
		}
	}

	/**
	 * 実行中かどうかを取得
	 * @return
	 */
	@Override
	public boolean isStarted() {
		synchronized (mSync) {
			return !mReleased && mIsRunning;
		}
	}

	@Override
	public synchronized void start() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "start:");
		if (!mReleased && !mIsRunning) {
			if ((mMediaFormats[0] != null)
				|| (mMediaFormats[1] != null)) {

				mIsRunning = true;
				mRequestStop = false;
				mMuxTask = new MuxTask();
				new Thread(mMuxTask, "MuxTask").start();
			} else {
				throw new IllegalStateException("no added track");
			}
		} else {
			throw new IllegalStateException("already released or started");
		}
		if (DEBUG) Log.v(TAG, "start:finished");
	}
	
	/**
	 * 終了指示を送る
	 */
	@Override
	public synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
		synchronized (mSync) {
			mRequestStop = true;
			mMuxTask = null;
			mLastTrackIndex = mVideoTrackIx = mAudioTrackIx = -1;
			mMediaFormats[0] = mMediaFormats[1] = null;
		}
		if (DEBUG) Log.v(TAG, "stop:finished");
	}
	
	/**
	 * 映像/音声トラックを追加
	 * それぞれ最大で１つずつしか追加できない
 	 * @param format
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	@Override
	public int addTrack(@NonNull final MediaFormat format)
		throws IllegalArgumentException, IllegalStateException {

		if (DEBUG) Log.v(TAG, "addTrack:" + format);
		int result = mLastTrackIndex + 1;
		switch (result) {
		case 0:
		case 1:
			if (format.containsKey(MediaFormat.KEY_MIME)) {
				final String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video/")) {
					result = mVideoTrackIx = mMuxer.addTrack(format);
					mMediaFormats[result] = format;
				} else if (mime.startsWith("audio/")) {
					result = mAudioTrackIx = mMuxer.addTrack(format);
					mMediaFormats[result] = format;
				} else {
					throw new IllegalArgumentException("un-expected mime type");
				}
			} else {
				throw new IllegalArgumentException("has no mime type");
			}
			mLastTrackIndex++;
			break;
		default:
			throw new IllegalArgumentException();
		}
		if (DEBUG) Log.v(TAG, "addTrack:finished,result=" + result);
		return result;
	}
	
	/**
	 * こっちはキューに追加するだけ
	 * 実際のファイルへ出力は#internalWriteSampleData
	 * @param trackIx
	 * @param buffer
	 * @param info
	 */
	@Override
	public void writeSampleData(final int trackIx,
		@NonNull final ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {
	
		if (!mRequestStop && (trackIx <= mLastTrackIndex)) {
			final RecycleMediaData buf = mQueue.obtain();
			if (buf != null) {
				buffer.clear();	// limit==positionになってる変なByteBufferが来る端末があるのでclearする
				buf.set(trackIx, buffer, info);
				mQueue.queueFrame(buf);
			} else if (DEBUG) {
				Log.w(TAG, "frame skipped, failed to get buffer from pool.");
			}
		} else {
			if (DEBUG) Log.w(TAG, "not ready!");
		}
	}

	/**
	 * 実際のファイル出力用にIMuxerへ書き込む
	 * @param muxer
	 * @param trackIx
	 * @param buffer
	 * @param info
	 */
	protected void internalWriteSampleData(@NonNull final IMuxer muxer,
		final int trackIx,
		@NonNull final ByteBuffer buffer,
		@NonNull final MediaCodec.BufferInfo info) {

		muxer.writeSampleData(trackIx, buffer, info);
	}
	
	@Nullable
	protected Context getContext() {
		return mWeakContext.get();
	}
	
	@NonNull
	protected Context requireContext() throws IllegalStateException {
		final Context context = mWeakContext.get();
		if (context == null) {
			throw new IllegalStateException();
		}
		return context;
	}

	@NonNull
	public VideoConfig getConfig() {
		return mVideoConfig;
	}

	/**
	 * 出力ファイル名(拡張子なし)を取得
 	 * @return
	 */
	@NonNull
	protected String getOutputName() {
		return mOutputName;
	}
	
	/**
	 * 出力ディレクトリを取得,
	 * #getOutputDirPathと#getOutputDirDocはどちらかが必ずnull
 	 * @return
	 */
	@Nullable
	protected String getOutputDirPath() {
		return mOutputDir;
	}
	
	/**
	 * 出力ディレクトリを取得,
	 * #getOutputDirPathと#getOutputDirDocはどちらかが必ずnull
	 * @return
	 */
	@Nullable
	protected DocumentFile getOutputDirDoc() {
		return mOutputDoc;
	}
	
	private static final long MAX_CHECK_INTERVALS_NS = 3 * 1000000000L;	// 3 seconds

	private final class MuxTask implements Runnable {

		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "MuxTask#run:");
			final Context context = getContext();
			if (context != null) {
				IMuxer muxer = mMuxer;
				mMuxer = null;
				try {
					if (muxer == null) {
						try {
							muxer = setupMuxer(0);
						} catch (final IOException e) {
							Log.w(TAG, e);
							return;
						}
					} else {
						muxer.start();
					}
					final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
					final boolean shouldCheckIFrame = mVideoTrackIx >= 0;
					long prevCheckTime = Time.nanoTime();
					boolean mRequestChangeFile = false;
					int segment = 1, cnt = 0;
					if (DEBUG) Log.v(TAG, "MuxTask#run:muxing");
					while (mIsRunning) {
						// バッファキューからエンコード済みデータを取得する
						final RecycleMediaData buf;
						try {
							buf = mQueue.poll(10, TimeUnit.MILLISECONDS);
						} catch (final InterruptedException e) {
							if (DEBUG) Log.v(TAG, "interrupted");
							mIsRunning = false;
							break;
						}
						if (buf != null) {
							buf.get(info);
							if (mRequestChangeFile
								&& (!shouldCheckIFrame
									|| (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME)) ) {
	
								// ファイルサイズが超えていて、音声トラックのみかIフレームが来たときに
								// 出力ファイルを変更する
								mRequestChangeFile = false;
								try {
									muxer = restartMuxer(muxer, segment++);
								} catch (final IOException e) {
									break;
								}
							}
							// 出力ファイルへの書き込み処理
							internalWriteSampleData(muxer,
								buf.trackIx(),
								buf.get(), info);
							// 再利用のためにバッファを返す
							mQueue.recycle(buf);
						} else if (mRequestStop) {
							mIsRunning = false;
							break;
						}
						if (!mRequestChangeFile
							&& ( (((++cnt) % 1000) == 0)
								|| ((Time.nanoTime() - prevCheckTime)
										> MAX_CHECK_INTERVALS_NS) )) {
								
							prevCheckTime = Time.nanoTime();

							if (mCurrent.length() >= mSplitSize) {
								// ファイルサイズが指定値を超えた
								// ファイルサイズのチェック時はフラグを立てるだけにして
								// 次のIフレームが来たときに切り替えないと次のファイルの先頭が
								// 正しく再生できなくなる
								if (DEBUG) Log.v(TAG, "exceeds file size limit");
								mRequestChangeFile = true;
							}
							if (checkFreespace()) {
								mRequestStop = true;
								mIsRunning = false;
								break;
							}
						}
					} // end of while
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				if (muxer != null) {
					try {
						muxer.stop();
						muxer.release();
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
			mIsRunning = false;
			if (DEBUG) Log.v(TAG, "MuxTask#run:finished");
		}
		
	}
	
	private static final long STORAGE_SIZE_LIMIT = 1024L * 1024L * 1024L * 4L;	// 4GB
	private static final long MIN_FREE_SPACE = 1024L * 1024L * 16L;	// 16MB
	
	/**
	 * ストレージの空き容量を確認する
	 * @return 空き容量が少なければtrueを返す
	 */
	@SuppressWarnings("deprecation")
	protected boolean checkFreespace() {
		StorageInfo info = null;
		if (mOutputDoc != null) {
			try {
				info = StorageUtils.getStorageInfo(requireContext(), mOutputDoc);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		if (info == null) {
			try {
				info = StorageUtils.getStorageInfo(getContext(),
					Environment.DIRECTORY_MOVIES, 0);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		final float rate;
		final long totalBytes, freeBytes;
		if ((info != null) && (info.totalBytes > 0)) {
			rate = info.freeBytes / (float)info.totalBytes;
			totalBytes = info.totalBytes;
			freeBytes = info.freeBytes;
		} else {
			rate = 0;
			totalBytes = freeBytes = 0;
		}
		
		return (
			((rate < FileUtils.FREE_RATIO) && (totalBytes < STORAGE_SIZE_LIMIT))
			|| (freeBytes < MIN_FREE_SPACE)
		);
	}
	
	/**
	 * IMuxerの切り替え処理
	 * 内部で#setupMuxerを呼び出す
	 * @param muxer 今まで使っていたIMuxer
	 * @param segment 次のセグメント番号
	 * @return 次のファイル出力用のIMuxer
	 * @throws IOException
	 */
	protected IMuxer restartMuxer(@NonNull final IMuxer muxer, final int segment)
		throws IOException {

		if (DEBUG) Log.v(TAG, "restartMuxer:");
		try {
			muxer.stop();
			muxer.release();
		} catch (final Exception e) {
			throw new IOException(e);
		}
		// 次のIMuxerに切り替える
		return setupMuxer(segment);
	}
	
	/**
	 * createMuxerを呼び出してIMuxerを生成してから
	 * addTrack, startを呼び出す
	 * @param segment
	 * @return
	 * @throws IOException
	 */
	protected IMuxer setupMuxer(final int segment) throws IOException {
		final IMuxer result = createMuxer(segment);
		int n = 0;
		synchronized (mSync) {
			if (mMediaFormats[0] != null) {
				final int trackIx = result.addTrack(mMediaFormats[0]);
				if (DEBUG) Log.v(TAG, "add track," + trackIx
					+ ",video=" + mVideoTrackIx + ",audio=" + mAudioTrackIx);
				n++;
			}
			if (mMediaFormats[1] != null) {
				final int trackIx = result.addTrack(mMediaFormats[1]);
				if (DEBUG) Log.v(TAG, "add track," + trackIx
					+ ",video=" + mVideoTrackIx + ",audio=" + mAudioTrackIx);
				n++;
			}
		}
		if (n > 0) {
			result.start();
		} else {
			throw new IOException("already released?");
		}
		return result;
	}
	
	/**
	 * IMuxerを生成する, addTrack, startは呼ばない
	 * @param segment 次のセグメント番号
	 * @return
	 * @throws IOException
	 */
	protected IMuxer createMuxer(final int segment) throws IOException {
		if (DEBUG) Log.v(TAG, "MuxTask#run:create muxer");
		IMuxer result;
		mCurrent = createOutputDoc(EXT_MP4, segment);
		result = createMuxer(requireContext(), mCurrent);
		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * 出力ファイルを示すDocumentFileを生成
	 * @param ext 拡張子, ドット無し
	 * @param segment
	 * @return
	 * @throws IOException
	 */
	protected DocumentFile createOutputDoc(
		@NonNull final String ext, final int segment) throws IOException {
		
		return createOutputDoc(mOutputName, ext, segment);
	}

	/**
	 * 出力ファイルを示すDocumentFileを生成
	 * @param name
	 * @param ext 拡張子, ドット無し
	 * @param segment
	 * @return
	 */
	protected DocumentFile createOutputDoc(
		@NonNull final String name,
		@NonNull final String ext, final int segment) throws IOException {

		final String fileName = String.format(Locale.US, "%s%s%03d.%s",
			mOutputName, mSegmentPrefix, segment + 1, ext);
		if (mOutputDoc != null) {
			final DocumentFile dir = mOutputDoc.isDirectory()
				? mOutputDoc : mOutputDoc.getParentFile();
			return dir.createFile(null, fileName);
		} else if (mOutputDir != null) {
			final File _dir = new File(mOutputDir);
			final File dir = _dir.isDirectory() ? _dir : _dir.getParentFile();
			return DocumentFile.fromFile(
				new File(dir, fileName));
		} else {
			throw new IOException("output dir not set");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * IMuxer生成処理
	 * @param context
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	protected IMuxer createMuxer(@NonNull final Context context,
		@NonNull final DocumentFile file) throws IOException {

		if (DEBUG) Log.v(TAG, "createMuxer:file=" + file.getUri());
		final boolean useMediaMuxer = getConfig().useMediaMuxer();
		IMuxer result = mMuxerFactory.createMuxer(context, useMediaMuxer, file);
		if (result == null) {
			throw new IOException("Failed to create muxer");
//			result = mMuxerFactory.createMuxer(useMediaMuxer,
//				context.getContentResolver().openFileDescriptor(file.getUri(), "rw").getFd());
		}
		if (DEBUG) Log.v(TAG, "createMuxer:finished," + result);
		return result;
	}
}
