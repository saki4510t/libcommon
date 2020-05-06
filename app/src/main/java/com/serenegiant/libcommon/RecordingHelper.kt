package com.serenegiant.libcommon

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.serenegiant.system.PermissionCheck
import com.serenegiant.utils.FileUtils
import com.serenegiant.utils.SAFUtils
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object RecordingHelper {
	private const val DEBUG = false // set false on production
	private val TAG = RecordingHelper::class.java.simpleName
	const val REQUEST_ACCESS_SD = 34567
	const val APP_DIR = "libcommon"

	/*package*/
	var PREF_NAME = "com.serenegiant.libcommon"

	/**
	 * キャプチャ用のディレクトリを取得、取得できなければnull
	 * こっちはSAF経由でアクセス可能な場合のみ指定場所を示すDocumentFileを返す
	 * @param context
	 * @return
	 */
	@Synchronized
	fun getSAFRecordingRoot(
		context: Context): DocumentFile? {

		var root: DocumentFile? = null
		val pref = context.getSharedPreferences(PREF_NAME, 0)
		if (SAFUtils.hasPermission(context, REQUEST_ACCESS_SD)) {
			try {
				root = SAFUtils.getDir(context, REQUEST_ACCESS_SD, FileUtils.DIR_NAME)
				if (!root.exists() || !root.canWrite()) {
					Log.d(TAG, "path will be wrong, will already be removed,"
						+ root?.uri)
					root = null
				}
			} catch (e: IOException) {
				root = null
				Log.d(TAG, "path is wrong, will already be removed.", e)
			} catch (e: IllegalStateException) {
				root = null
				Log.d(TAG, "path is wrong, will already be removed.", e)
			}
		}
		if (root == null) {
			if (DEBUG) Log.d(TAG, "getSAFRecordingRoot:保存先を取得できなかったので念のためにセカンダリーストレージアクセスのパーミッションも落としておく")
			SAFUtils.releaseStorageAccessPermission(context, REQUEST_ACCESS_SD)
		}
		return root
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
	@Synchronized
	fun getRecordingRoot(
		context: Context,
		type: String): DocumentFile? {

		// SAF経由での録画用ディレクトリ取得を試みる
		var root = getSAFRecordingRoot(context)
		if (root == null && PermissionCheck.hasWriteExternalStorage(context)) {
			// SAF経由で録画用ディレクトリを取得できなかったが外部ストレージのアクセスパーミッションがある時
			if (DEBUG) Log.d(TAG, "getRecordingRoot:アプリが外部ストレージへのアクセスパーミッションを保持していればパスの取得を試みる")
			val captureDir = FileUtils.getCaptureDir(context, type, 0)
			if (captureDir != null && captureDir.canWrite()) {
				root = DocumentFile.fromFile(captureDir)
				// こっちの場合は既にディレクトリ名としてFileUtils.DIR_NAMEが付加されてくるはずなのでここでは追加しない
			}
		}
		return root
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
	@Throws(IOException::class)
	fun getRecordingFile(
		context: Context,
		type: String, mime: String?,
		ext: String): DocumentFile {
		val root = getRecordingRoot(context, type)
		return getRecordingFile(context, root!!, null,
			mime, dateTimeString + ext)
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
	@Synchronized
	@Throws(IOException::class)
	fun getRecordingFile(
		context: Context,
		root: DocumentFile, dirs: String?,
		mime: String?,
		fileNameWithExt: String): DocumentFile {
		return SAFUtils.getFile(root, dirs, mime!!, fileNameWithExt)
			?: throw IOException("has no permission or can't write to storage" +
				"(storage will be full?) or something files already exist")
	}

	/**
	 * 現在の日時を表す文字列を取得する
	 * @return
	 */
	val dateTimeString: String
		get() = getDateTimeString(System.currentTimeMillis())

	/**
	 * 日時を表す文字列を取得する
	 * @return
	 */
	fun getDateTimeString(timeMillis: Long): String {
		val format = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US)
		return format.format(timeMillis)
	}
}