package com.serenegiant.libcommon;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.serenegiant.system.PermissionCheck;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.utils.SAFUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class RecordingHelper {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = RecordingHelper.class.getSimpleName();

	public static final int REQUEST_ACCESS_SD = 34567;
	public static final String APP_DIR = "libcommon";
	/*package*/ static String PREF_NAME = "com.serenegiant.libcommon";

	/**
	 * キャプチャ用のディレクトリを取得、取得できなければnull
	 * こっちはSAF経由でアクセス可能な場合のみ指定場所を示すDocumentFileを返す
	 * @param context
	 * @return
	 */
	@Nullable
	public static synchronized DocumentFile getSAFRecordingRoot(
		@NonNull final Context context) {

		DocumentFile root = null;
		final SharedPreferences pref
			= context.getSharedPreferences(PREF_NAME, 0);
		if (SAFUtils.hasStorageAccess(context, REQUEST_ACCESS_SD)) {
			try {
				root = SAFUtils.getStorage(context, REQUEST_ACCESS_SD, FileUtils.DIR_NAME);
				if ((root == null) || !root.exists() || !root.canWrite()) {
					Log.d(TAG, "path will be wrong, will already be removed,"
						+ (root != null ? root.getUri() : null));
					root = null;
				}
			} catch (final IOException | IllegalStateException e) {
				root = null;
				Log.d(TAG, "path is wrong, will already be removed.", e);
			}
		}
		if (root == null) {
			if (DEBUG) Log.d(TAG, "getRecordingRoot:保存先を取得できなかったので念のためにセカンダリーストレージアクセスのパーミッションも落としておく");
			SAFUtils.releaseStorageAccessPermission(context, REQUEST_ACCESS_SD);
		}
		return root;
	}

	/**
	 * キャプチャ用のディレクトリを取得、取得できなければnull
	 * SAF経由での録画ディレクトリ、またはSAF経由でアクセス出来ないとき＆外部ストレージへの書き込みパーミッションがあれば
	 * 外部ストレージの${type}ディレクトリを返す
	 * "${DIRECTORY_MOVIES}/${FileUtils.DIR_NAME}" directory on primary storage as default value
	 * if user change
	 * @param context
	 * @param type SAFではなく外部ストレージへ保存する場合のディレクトリタイプ, Environment.DIRECTORY_MOVIES / DIRECTORY_DCIM
	 * @return
	 */
	@Nullable
	public static synchronized DocumentFile getRecordingRoot(
		@NonNull final Context context,
		@NonNull final String type) {

		// SAF経由での録画用ディレクトリ取得を試みる
		DocumentFile root = getSAFRecordingRoot(context);
		if ((root == null) && PermissionCheck.hasWriteExternalStorage(context)) {
			// SAF経由で録画用ディレクトリを取得できなかったが外部ストレージのアクセスパーミッションがある時
			if (DEBUG) Log.d(TAG, "getRecordingRoot:アプリが外部ストレージへのアクセスパーミッションを保持していればパスの取得を試みる");
			final File captureDir
				= FileUtils.getCaptureDir(context, type, 0);
			if ((captureDir != null) && captureDir.canWrite()) {
				root = DocumentFile.fromFile(captureDir);
				// こっちの場合は既にディレクトリ名としてFileUtils.DIR_NAMEが付加されてくるはずなのでここでは追加しない
			}
		}
		return root;
	}
	
	/**
	 * 静止画/動画保存用のDocumentFileを取得
	 * @param context
	 * @param mime
	 * @param type SAFではなく外部ストレージへ保存する場合のディレクトリタイプ, Environment.DIRECTORY_MOVIES / DIRECTORY_DCIM
	 * @param ext 拡張子、ピリオドを含む(例：".jpeg", ".png", ".mp4")
	 * @return
	 * @throws IOException
	 */
	public static DocumentFile getRecordingFile(
		@NonNull final Context context,
		@NonNull final String type, @Nullable final String mime,
		@NonNull final String ext) throws IOException {

		final DocumentFile root = getRecordingRoot(context, type);
		return getRecordingFile(context, root, null,
			mime, getDateTimeString() + ext);
	}

	/**
	 * 静止画/動画保存用のDocumentFileを取得
	 * @param context
	 * @param root
	 * @param mime
	 * @param fileNameWithExt
	 * @return
	 * @throws IOException
	 */
	public static synchronized DocumentFile getRecordingFile(
		@NonNull final Context context,
		@NonNull final DocumentFile root, @Nullable final String dirs,
		@Nullable final String mime,
		@NonNull final String fileNameWithExt) throws IOException {

		DocumentFile result = SAFUtils.getStorageFile(context, root, dirs, mime, fileNameWithExt);
		if (result == null) {
			throw new IOException("has no permission or can't write to storage" +
				"(storage will be full?) or something files already exist");
		}
		return result;
	}

	/**
	 * 現在の日時を表す文字列を取得する
	 * @return
	 */
	public static final String getDateTimeString() {
		return getDateTimeString(System.currentTimeMillis());
	}

	/**
	 * 日時を表す文字列を取得する
	 * @return
	 */
	public static final String getDateTimeString(final long timeMillis) {
		final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);
		return format.format(timeMillis);
	}
}
