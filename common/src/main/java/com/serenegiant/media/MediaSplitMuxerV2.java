package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2016-2025 saki t_saki@serenegiant.com
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
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.serenegiant.mediastore.MediaStoreUtils;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.PermissionUtils;
import com.serenegiant.system.StorageInfo;
import com.serenegiant.system.StorageUtils;
import com.serenegiant.system.Time;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.utils.UriHelper;

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
 * 出力ファイル切替時にIフレームの検出処理を行うため動画ファイル間で一部フレームが
 * スキップされてしまう可能性がある
 */
public class MediaSplitMuxerV2 implements IMuxer {
	private static final boolean DEBUG = false; // FIXME set false on production
	private static final String TAG = MediaSplitMuxerV2.class.getSimpleName();

	/**
	 * セグメント名のプレフィックス文字列のデフォルト
	 */
	private static final String DEFAULT_PREFIX_SEGMENT_NAME = "";
	/**
	 * セグメント名のプレフィックス文字列, コンストラクタで読み込む
	 */
	public static String PREFIX_SEGMENT_NAME = DEFAULT_PREFIX_SEGMENT_NAME;

	private static final int INI_POOL_NUM = 4;
	private static final int MAX_POOL_NUM = 1000;
	private static final long DEFAULT_SPLIT_SIZE = 4000000000L;
	private static final String EXT_MP4 = "mp4";

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final WeakReference<Context> mWeakContext;
	/**
	 * MediaCodecのエンコーダーの設定
	 * 最終のmp4ファイル出力時に必要なため保持しておく
	 */
	@NonNull
	private final MediaFormat[] mMediaFormats = new MediaFormat[2];
	@NonNull
	private final String mSegmentPrefix;
	@NonNull
	private final VideoConfig mVideoConfig;
	@NonNull
	private final IMuxerFactory mMuxerFactory;
	@NonNull
	private final IMediaQueue<RecycleMediaData> mQueue;
	private final long mSplitSize;
	@NonNull
	private final String mOutputDirName;
	@Nullable
	private final DocumentFile mOutputDir;

	private int mVideoTrackIx = -1;
	private int mAudioTrackIx = -1;
	/**
	 * 現在の出力先DocumentFile
	 */
	private DocumentFile mCurrent;
	/** 実行中フラグ */
	private volatile boolean mIsRunning;
	private volatile boolean mRequestStop;
	private boolean mReleased;
	private int mLastTrackIndex = -1;
	/**
	 * コンストラクタで生成したセグメント0のmuxerをワーカースレッドへ引き継ぐための一時変数
	 */
	@Nullable
	private IMuxer mMuxer;
	@Nullable
	private MuxTask mMuxTask;

	/**
	 * コンストラクタ
	 * @param context
	 * @param outputDir 出力先ディレクトリを示すDocumentFile
	 * 					API>=29の場合はSAFのツリードキュメントかnullでないとだめ
	 * 					nullを指定した場合はFileUtils.getCaptureDir(API>=29の場合は対象範囲別ストレージを使ってEnvironment.DIRECTORY_MOVIES)の下に
	 * 					MediaAVSplitRecorderV2生成時刻文字列をフォルダ名として追加した上で各セグメント毎の録画ファイルが生成される
	 * @param config
	 * @param factory
	 * @param queue バッファリング用IMediaQueue
	 * @param splitSize 出力ファイルサイズの目安, 0以下ならデフォルト値
	 * @throws IOException
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public MediaSplitMuxerV2(
		@NonNull final Context context,
		@Nullable final DocumentFile outputDir,
		@Nullable final VideoConfig config,
		@Nullable final IMuxerFactory factory,
		@Nullable final IMediaQueue<RecycleMediaData> queue,
		final long splitSize) throws IOException {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<Context>(context);
		mVideoConfig = config != null ? config : new VideoConfig();
		mMuxerFactory = factory != null ? factory : new DefaultFactory();
		mQueue = queue != null
			? queue : new MemMediaQueue(INI_POOL_NUM, MAX_POOL_NUM);
		mSplitSize = splitSize <= 0 ? DEFAULT_SPLIT_SIZE : splitSize;
		mSegmentPrefix = PREFIX_SEGMENT_NAME != null
			? PREFIX_SEGMENT_NAME : DEFAULT_PREFIX_SEGMENT_NAME;
		// 出力先の相対パスに挿入するディレクトリ名
		mOutputDirName = FileUtils.getDateTimeString();
		// 出力先ディレクトリのDocumentFileを生成
		if (outputDir != null) {
			// 上位からDocumentFileが指定されたときはそれを使う
			mOutputDir = outputDir;
		} else if (!BuildCheck.isAPI29()
			&& PermissionUtils.hasWriteExternalStorage(context)) {
			// API29未満で外部ストレージアクセスのパーミッションがある時
			if (DEBUG) Log.v(TAG, "コンストラクタ:出力先ディレクトリのツリードキュメントの生成を試みる");
			// FileUtils#getCaptureDirはFileUtils.getDirNameを含んだパスを返す
			File dir = FileUtils.getCaptureDir(context, Environment.DIRECTORY_MOVIES);
			if (dir != null) {
				dir = new File(dir, mOutputDirName);	// 出力先ディレクトリ名を追加
				dir.mkdirs();	// パス中のディレクトリをすべて作成
				// 出力先のディレクトリを指すツリードキュメントを生成
				mOutputDir = DocumentFile.fromFile(dir);
			} else {
				mOutputDir = null;
			}
		} else {
			mOutputDir = null;
		}
		// 録画ファイルを出力できるかどうかを確認するためにセグメント0のmuxerを生成する
		// (出力できなければIOExceptionを投げる)
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
		case 0, 1 -> {
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
		}
		default -> throw new IllegalArgumentException();
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
	 * 動画出力ファイルサイズを確認する最大間隔[ナノ秒]
	 * 前回のチェックからこの値を超えるか1000フレームを超えるとファイルサイズチェックを行う
	 */
	private static final long MAX_CHECK_INTERVALS_NS = 3 * 1000000000L;	// 3 seconds

	/**
	 * 動画出力ファイルサイズをモニターして必要に応じて出力ファイルを切り替えるためのRunnable実装
	 */
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
					// MediaStoreから取得したUriをDocumentFileデラップした時に
					// DocumentFile#lengthが常に0を返すのでファイルサイズチェック
					// できないのでワークアラウンドとして書き込んだデータバイト数を自前で
					// カウントする
					long bytesWrote = 0;
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
									bytesWrote = 0;
								} catch (final IOException e) {
									break;
								}
							}
							// 出力ファイルへの書き込み処理
							internalWriteSampleData(muxer,
								buf.trackIx(),
								buf.get(), info);
							bytesWrote += Math.max(info.size, 0);
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
							final long length = Math.max(bytesWrote, mCurrent.length());
							if (DEBUG) Log.v(TAG, "MuxTask#run:length=" + length);
							if (length >= mSplitSize) {
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
	protected boolean checkFreespace() {
		StorageInfo info = null;
		if (mCurrent != null) {
			try {
				info = StorageUtils.getStorageInfo(requireContext(), mCurrent);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		if (info == null) {
			try {
				info = StorageUtils.getStorageInfo(getContext(), Environment.DIRECTORY_MOVIES);
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
	private IMuxer restartMuxer(@NonNull final IMuxer muxer, final int segment)
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
	private IMuxer setupMuxer(final int segment) throws IOException {
		if (DEBUG) Log.v(TAG, "setupMuxer:");
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
	private IMuxer createMuxer(final int segment) throws IOException {
		if (DEBUG) Log.v(TAG, "createMuxer:");
		if (mCurrent != null) {
			final Context context = requireContext();
			if (BuildCheck.isAPI29()) {
				// API>=29でMediaStoreからIS_PENDING=1で取得したuriの後処理
				// デフォルトのIMuxerFactory実装であればMediaStoreOutputStreamで
				// ラップしているのでここでのupdateContentUri呼び出しは冗長だけど、
				// IMuxerFactoryを時前実装してる可能性があるので念のために呼んでおく
				MediaStoreUtils.updateContentUri(context, mCurrent);
			} else if (UriHelper.isFileUri(mCurrent)) {
				final String path = UriHelper.getPath(context, mCurrent.getUri());
				if (DEBUG) Log.v(TAG, "createMuxer:scanFile," + path);
				try {
					// 内部でexecutorを使ってワーカースレッド上で処理しているのでここで呼び出しても大丈夫なはず
					MediaScannerConnection.scanFile(context, new String[] {path}, null, null);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
		mCurrent = createOutputDoc(segment);
		return createMuxer(requireContext(), mCurrent);
	}

//--------------------------------------------------------------------------------
	/**
	 * 出力ファイルを示すDocumentFileを生成
	 * @param segment
	 * @return
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@NonNull
	protected DocumentFile createOutputDoc(final int segment) throws IOException {

		final String fileName = String.format(Locale.US, "%s%04d.%s",
			mSegmentPrefix, segment + 1, EXT_MP4);
		if (DEBUG) Log.v(TAG, "createOutputDoc:" + fileName);
		final Context context = requireContext();
		if (mOutputDir != null) {
			// SAF経由か外部ストレージアクセスのパーミションがあって出力先ディレクトリのDocumentFileが指定されている時
			final DocumentFile dir = mOutputDir.isDirectory()
				? mOutputDir : mOutputDir.getParentFile();
			return mOutputDir.createFile(null, fileName);
		} else if (BuildCheck.isAPI29()) {
			// 対象範囲別ストレージの場合
			return MediaStoreUtils.getContentDocument(
				context, "video/mp4",
				Environment.DIRECTORY_MOVIES
					+ "/" + FileUtils.getDirName()
					+ "/" + mOutputDirName,
				fileName, null);
		} else if (PermissionUtils.hasWriteExternalStorage(context)) {
			// フォールバック...コンストラクタで処理しているのでここへは来ないはず
			if (DEBUG) Log.v(TAG, "createOutputDoc:外部ストレージへの書き込みを試みる");
			// FileUtils#getCaptureDirはFileUtils.getDirNameを含んだパスを返す
			File dir = FileUtils.getCaptureDir(context, Environment.DIRECTORY_MOVIES);
			if (dir != null) {
				dir = new File(dir, mOutputDirName);	// 出力先ディレクトリ名を追加
				dir.mkdirs();	// パス中のディレクトリをすべて作成
				// 出力先のディレクトリを指すツリードキュメントを生成
				final DocumentFile output = DocumentFile.fromFile(dir);
				return output.createFile(null, fileName);
			}
		}
		throw new IOException("Failed to create output DocumentFile");
	}

//--------------------------------------------------------------------------------
	/**
	 * IMuxer生成処理
	 * @param context
	 * @param output
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	private IMuxer createMuxer(@NonNull final Context context,
		@NonNull final DocumentFile output) throws IOException {

		if (DEBUG) Log.v(TAG, "createMuxer:uri=" + output.getUri());
		final boolean useMediaMuxer = getConfig().useMediaMuxer();
		IMuxer result = mMuxerFactory.createMuxer(context, useMediaMuxer, output);
		if (result == null) {
			throw new IOException("Failed to create muxer");
//			result = mMuxerFactory.createMuxer(useMediaMuxer,
//				context.getContentResolver().openFileDescriptor(output.getUri(), "rw").getFd());
		}
		if (DEBUG) Log.v(TAG, "createMuxer:finished," + result);
		return result;
	}
}
