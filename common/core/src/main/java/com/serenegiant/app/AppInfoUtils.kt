package com.serenegiant.app
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.StringBuilderPrinter
import com.serenegiant.content.ContextUtils
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ApplicationInfo用のヘルパーメソッド
 */
object AppInfoUtils {
	private const val DEBUG = false // set false on production
	private val TAG: String = AppInfoUtils::class.java.simpleName

	/**
	 * システムアプリまたは更新可能なシステムアプリかどうか
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun isSystemAppOrUpdatedSystemApp(info: ApplicationInfo): Boolean {
		return ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
			|| ((info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
	}

	/**
	 * システムアプリかどうか
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun isSystemApp(info: ApplicationInfo): Boolean {
		return (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
	}

	/**
	 * 更新可能なシステムアプリかどうか
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun isUpdatedSystemApp(info: ApplicationInfo): Boolean {
		return (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
	}

	/**
	 * 指定したfeatureをmanifestで要求しているかどうか
	 * @param context
	 * @param info
	 * @param feature
	 * @return
	 */
	@JvmStatic
	fun hasFeature(
		context: Context,
		info: ApplicationInfo,
		feature: String
	): Boolean {
		val pm = context.packageManager
		try {
			val pi = pm.getPackageInfo(info.packageName, PackageManager.GET_CONFIGURATIONS)
			if ((pi.reqFeatures != null) && (pi.reqFeatures!!.size > 0)) {
				for (fi in pi.reqFeatures!!) {
					if (feature == fi.name) {
						if (DEBUG) Log.v(
							TAG,
							"hasFeature:has $feature"
						)
						return true
					}
				}
			} else if (DEBUG) {
				if (DEBUG) Log.v(TAG, "hasFeature:has no features.")
			}
//			if ((pi.featureGroups != null) && (pi.featureGroups.length > 0)) {
//				for (final FeatureGroupInfo fi: pi.featureGroups) {
//					if (DEBUG) Log.v(TAG, "hasFeature:" + fi);
//				}
//			} else if (DEBUG) {
//				if (DEBUG) Log.v(TAG, "hasFeature:has no feature groups.");
//			}
		} catch (e: PackageManager.NameNotFoundException) {
			if (DEBUG) Log.d(TAG, "hasFeature:", e)
		}
		return false
	}

	/**
	 * 指定したパッケージ名のアプリがインストールされているかどうかを取得する
	 */
	@JvmStatic
	fun isInstalled(context: Context, packageName: String): Boolean {
		try {
			return context.packageManager.getPackageInfo(packageName, 0) != null
		} catch (e: PackageManager.NameNotFoundException) {
			// ignore
		}

		return false
	}

	/**
	 * フィルター処理をしたApplicationInfoリストを取得する
	 * @param context
	 * @param filter
	 * @return
	 */
	@JvmStatic
	fun getInstalledApplications(
		context: Context,
		filter: AppInfoFilterCallback?
	): List<ApplicationInfo> {
		val result: MutableList<ApplicationInfo> = ArrayList()
		val pm = context.packageManager
		val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
		for (app in apps) {
			if ((filter == null) || filter.onFilter(app)) {
				result.add(app)
			}
		}
		if (DEBUG) Log.v(TAG, "getInstalledApplications:n=" + result.size)
		return result
	}

	/**
	 * 指定したApplicationInfoに対応するアイコン用のDrawableを取得する
	 * @param context
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun getApplicationIcon(
		context: Context,
		info: ApplicationInfo
	): Drawable {
		//		return pm.getApplicationIcon(info);

		return info.loadIcon(context.packageManager)
	}

	/**
	 * アプリのタイトル表示用CharSequenceを取得する
	 * @param context
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun getDisplayName(
		context: Context,
		info: ApplicationInfo
	): CharSequence {
		return info.loadLabel(context.packageManager)
	}

	/**
	 * 指定したApplicationInfoの内容をApplicationInfo#dumpを
	 * 使って内容全部を文字列化するためのヘルパーメソッド
	 * (ApplicationInfo#toStringだと一部分した出力されない)
	 * @param info
	 * @return
	 */
	@JvmStatic
	fun toString(info: ApplicationInfo): String {
		val sb = StringBuilder()
		val printer = StringBuilderPrinter(sb)
		info.dump(printer, "")
		return sb.toString()
	}

	//--------------------------------------------------------------------------------
	@JvmStatic
	fun getDefaultActivity(
		context: Context,
		packageName: String
	): String? {
		var activityFQCN: String? = null
		val pm = context.packageManager
		val intent = pm.getLaunchIntentForPackage(packageName)
		val componentName = intent?.component
		if (componentName != null) {
			activityFQCN = componentName.className
			//		} else {
//			// ここには来ないはずだけどデフォルトのランチャーアクティビティが無い時
		}
		return activityFQCN
	}
	//--------------------------------------------------------------------------------
	/**
	 * ランチャーActivityを取得する。
	 * filterが最初に一致したものを返す。
	 * filterがnullなら最初に見つかったものを返す。
	 * @param context
	 * @param filter
	 * @return 一致するものがなければnull
	 */
	@JvmStatic
	fun findLauncherActivity(
		context: Context,
		filter: AppInfoFilterCallback?
	): ActivityInfo? {
		return findActivity(
			context,
			Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER),
			filter
		)
	}

	/**
	 * 指定したIntentで起動可能なActivityを取得する。
	 * filterが最初に一致したものを返す。
	 * filterがnullなら最初に見つかったものを返す。
	 * @param context
	 * @param intent
	 * @param filter
	 * @return 一致するものがなければnull
	 */
	@JvmStatic
	fun findActivity(
		context: Context,
		intent: Intent,
		filter: AppInfoFilterCallback?
	): ActivityInfo? {
		val pm = context.packageManager
		val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			pm.queryIntentActivities(
				intent,
				PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
			)
		} else {
			pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
		}
		var found: ActivityInfo? = null
		for (item in list) {
			val info = item.activityInfo
			if ((filter == null) || filter.onFilter(info.applicationInfo)) {
				if (TextUtils.isEmpty(info.packageName)) {
					try {
						info.packageName = info.applicationInfo.packageName
					} catch (e: Exception) {
						if (DEBUG) Log.w(TAG, e)
					}
				}
				if (TextUtils.isEmpty(info.targetActivity)) {
					if (info.name.startsWith(".")) {
						info.targetActivity = info.packageName + info.name
					} else {
						info.targetActivity = info.name
					}
				}
				found = info
				break
			}
		}
		if (DEBUG) Log.v(TAG, "findActivity:result=$found")
		return found
	}

	/**
	 * ランチャーActivity一覧を取得
	 * @param context
	 * @return
	 */
	@JvmStatic
	fun getLauncherActivities(
		context: Context,
		filter: AppInfoFilterCallback?
	): List<ActivityInfo> {
		return getActivities(
			context,
			Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER),
			filter
		)
	}

	/**
	 * 指定したIntentで起動可能なActivity一覧を取得
	 * @param context
	 * @return
	 */
	@JvmStatic
	fun getActivities(
		context: Context,
		intent: Intent,
		filter: AppInfoFilterCallback?
	): List<ActivityInfo> {
		val result: MutableList<ActivityInfo> = ArrayList()
		val pm = context.packageManager
		val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			pm.queryIntentActivities(
				intent,
				PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
			)
		} else {
			pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
		}
		for (item in list) {
			val info = item.activityInfo
			if ((filter == null) || filter.onFilter(info.applicationInfo)) {
				if (TextUtils.isEmpty(info.packageName)) {
					try {
						info.packageName = info.applicationInfo.packageName
					} catch (e: Exception) {
						if (DEBUG) Log.w(TAG, e)
					}
				}
				if (TextUtils.isEmpty(info.targetActivity)) {
					if (info.name.startsWith(".")) {
						info.targetActivity = info.packageName + info.name
					} else {
						info.targetActivity = info.name
					}
				}
				result.add(info)
			}
		}
		if (DEBUG) Log.v(TAG, "getLauncherActivities:n=" + result.size)
		return result
	}

	//--------------------------------------------------------------------------------
	/**
	 * 無効にされているのも含めてインストールされているパッケージを全て取得する
	 * PackageManagerのメソッドだと無効にされているアプリは取得できないので
	 * コマンドラインツールのpmを使ってコンソールに出力したインストール済み
	 * パッケージ一覧を解析してリストとして返す
	 * @param context
	 * @return
	 */
	@JvmStatic
	fun getInstalledPackages(context: Context): List<String> {
		val result: MutableList<String> = ArrayList()

		try {
			val p = Runtime.getRuntime().exec("pm list packages -u")
			p.waitFor()
			val reader = BufferedReader(InputStreamReader(p.inputStream))

			var line = ""
			while ((reader.readLine().also { line = it }) != null) {
				line = line.replace("package:", "")
				result.add(line)
			}
		} catch (e: Exception) {
			if (DEBUG) Log.w(TAG, "getInstalledPackages", e)
		}
		return result
	}

	/**
	 * 起動可能なアクティビティを保持するアプリのパッケージ名一覧を取得する
	 * @param context
	 * @return
	 */
	@JvmStatic
	fun getLauncherPackages(context: Context): List<String> {
		val result: MutableList<String> = ArrayList()
		val pm = context.packageManager
		val intent = Intent(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_LAUNCHER)
		val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
		} else {
			pm.queryIntentActivities(intent, 0)
		}
		for (item in list) {
			val app = item.activityInfo.applicationInfo
			result.add(app.packageName)
		}
		return result
	}

	/**
	 * ApplicationInfoのフィルター処理用インターフェース
	 */
	interface AppInfoFilterCallback {
		/**
		 * ApplicationInfoのフィルター処理用コールバックメソッド
		 * @param info
		 * @return true: 引数のApplicationInfoは有効、false:　引数のApplicationInfoは無効
		 */
		fun onFilter(info: ApplicationInfo): Boolean
	}

	/**
	 * 指定したアプリが起動しているかどうか
	 * @param packageName
	 * @return true: 起動している、false: 起動していない
	 */
	fun Context.isRunning(packageName: String): Boolean {
		val am = ContextUtils.requireSystemService(this, ActivityManager::class.java)
		val runningApps = am.runningAppProcesses
		for (app in runningApps) {
			if (packageName == app.processName) {
				return app.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
			}
		}
		return false
	}
}
