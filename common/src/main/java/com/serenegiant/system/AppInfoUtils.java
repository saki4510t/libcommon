package com.serenegiant.system;
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ApplicationInfo用のヘルパーメソッド
 */
public class AppInfoUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AppInfoUtils.class.getSimpleName();

	private AppInfoUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * システムアプリまたは更新可能なシステムアプリかどうか
	 * @param info
	 * @return
	 */
	public static boolean isSystemAppOrUpdatedSystemApp(@NonNull final ApplicationInfo info) {
		return ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
			|| ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
	}

	/**
	 * システムアプリかどうか
	 * @param info
	 * @return
	 */
	public static boolean isSystemApp(@NonNull final ApplicationInfo info) {
		return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
	}

	/**
	 * 更新可能なシステムアプリかどうか
	 * @param info
	 * @return
	 */
	public static boolean isUpdatedSystemApp(@NonNull final ApplicationInfo info) {
		return (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
	}

//--------------------------------------------------------------------------------
	/**
	 * AndroidManifest.xmlで要求している機能フラグ一覧を取得する
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<String> requestedFeatures(@NonNull final Context context) {
		final List<String> result = new ArrayList<>();

		final PackageManager pm = context.getPackageManager();
		final ApplicationInfo info = context.getApplicationInfo();
		try {
			final PackageInfo pi = pm.getPackageInfo(info.packageName, PackageManager.GET_CONFIGURATIONS);
			if ((pi.reqFeatures != null) && (pi.reqFeatures.length > 0)) {
				for (final FeatureInfo fi: pi.reqFeatures) {
					if (fi.name != null) {
						result.add(fi.name);
					}
				}
			} else if (DEBUG) {
				if (DEBUG) Log.v(TAG, "hasFeature:has no features.");
			}
//			if ((pi.featureGroups != null) && (pi.featureGroups.length > 0)) {
//				for (final FeatureGroupInfo fi: pi.featureGroups) {
//					if (DEBUG) Log.v(TAG, "hasFeature:" + fi);
//				}
//			} else if (DEBUG) {
//				if (DEBUG) Log.v(TAG, "hasFeature:has no feature groups.");
//			}
		} catch (final PackageManager.NameNotFoundException e) {
			if (DEBUG) Log.d(TAG, "hasFeature:", e);
		}

		return result;
	}

	/**
	 * 指定したfeatureをAndroidManifest.xmlで要求しているかどうか
	 * @param context
	 * @param feature
	 * @return
	 */
	public static boolean hasFeature(
		@NonNull final Context context,
		@NonNull final String feature) {

		return hasFeature(context, context.getApplicationInfo(), feature);
	}

	/**
	 * 指定したfeatureをAndroidManifest.xmlで要求しているかどうか
	 * @param context
	 * @param info
	 * @param feature
	 * @return
	 */
	public static boolean hasFeature(
		@NonNull final Context context,
		@NonNull final ApplicationInfo info,
		@NonNull final String feature) {

		final PackageManager pm = context.getPackageManager();
		try {
			final PackageInfo pi = pm.getPackageInfo(info.packageName, PackageManager.GET_CONFIGURATIONS);
			if ((pi.reqFeatures != null) && (pi.reqFeatures.length > 0)) {
				for (final FeatureInfo fi: pi.reqFeatures) {
					if (feature.equals(fi.name)) {
						if (DEBUG) Log.v(TAG, "hasFeature:has " + feature);
						return true;
					}
				}
			} else if (DEBUG) {
				if (DEBUG) Log.v(TAG, "hasFeature:has no features.");
			}
//			if ((pi.featureGroups != null) && (pi.featureGroups.length > 0)) {
//				for (final FeatureGroupInfo fi: pi.featureGroups) {
//					if (DEBUG) Log.v(TAG, "hasFeature:" + fi);
//				}
//			} else if (DEBUG) {
//				if (DEBUG) Log.v(TAG, "hasFeature:has no feature groups.");
//			}
		} catch (final PackageManager.NameNotFoundException e) {
			if (DEBUG) Log.d(TAG, "hasFeature:", e);
		}
		return false;
	}

	/**
	 * 指定したfeature全てをAndroidManifest.xmlで要求しているかどうか
	 * @param context
	 * @param features
	 * @return
	 */
	public static boolean hasFeaturesAll(
		@NonNull final Context context,
		@NonNull final String[] features) {

		return hasFeaturesAll(context, context.getApplicationInfo(), features);
	}

	/**
	 * 指定したfeature全てをAndroidManifest.xmlで要求しているかどうか
	 * @param context
	 * @param info
	 * @param features
	 * @return
	 */
	public static boolean hasFeaturesAll(
		@NonNull final Context context,
		@NonNull final ApplicationInfo info,
		@NonNull final String[] features) {

		boolean result = true;
		final PackageManager pm = context.getPackageManager();
		try {
			final PackageInfo pi = pm.getPackageInfo(info.packageName, PackageManager.GET_CONFIGURATIONS);
			if ((pi.reqFeatures != null) && (pi.reqFeatures.length > 0)) {
				for (final String feature: features) {
					boolean found = false;
					for (final FeatureInfo fi: pi.reqFeatures) {
						if (feature.equals(fi.name)) {
							if (DEBUG) Log.v(TAG, "hasFeature:has " + feature);
							found =  true;
							break;
						}
					}
					result |= found;
					if (!result) {
						break;
					}
				}
			} else if (DEBUG) {
				if (DEBUG) Log.v(TAG, "hasFeature:has no features.");
			}
		} catch (final PackageManager.NameNotFoundException e) {
			if (DEBUG) Log.d(TAG, "hasFeature:", e);
		}

		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * ApplicationInfoのフィルター処理用インターフェース
	 */
	public interface AppInfoFilterCallback {
		/**
		 * ApplicationInfoのフィルター処理用コールバックメソッド
		 * @param info
		 * @return true: 引数のApplicationInfoは有効、false:　引数のApplicationInfoは無効
		 */
		public boolean onFilter(@NonNull final ApplicationInfo info);
	}

	/**
	 * フィルター処理をしたApplicationInfoリストを取得する
	 * @param context
	 * @param filter
	 * @return
	 */
	public static List<ApplicationInfo> getInstalledApplications(
		@NonNull final Context context,
		@Nullable final AppInfoFilterCallback filter) {

		final List<ApplicationInfo> result = new ArrayList<>();
		final PackageManager pm = context.getPackageManager();
		final List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		for (final ApplicationInfo app: apps) {
			if ((filter == null) || filter.onFilter(app)) {
				result.add(app);
			}
		}
		if (DEBUG) Log.v(TAG, "getInstalledApplications:n=" + result.size());
		return result;
	}

	/**
	 * 指定したApplicationInfoに対応するアイコン用のDrawableを取得する
	 * @param context
	 * @param info
	 * @return
	 */
	@NonNull
	public static Drawable getApplicationIcon(
		@NonNull final Context context,
		@NonNull final ApplicationInfo info) {

//		return pm.getApplicationIcon(info);
		return info.loadIcon(context.getPackageManager());
	}

	/**
	 * アプリのタイトル表示用CharSequenceを取得する
	 * @param context
	 * @param info
	 * @return
	 */
	@NonNull
	public static CharSequence getDisplayName(
		@NonNull final Context context,
		@NonNull final ApplicationInfo info) {

		return info.loadLabel(context.getPackageManager());
	}

	/**
	 * 指定したApplicationInfoの内容をApplicationInfo#dumpを
	 * 使って内容全部を文字列化するためのヘルパーメソッド
	 * (ApplicationInfo#toStringだと一部分しか出力されない)
	 * @param info
	 * @return
	 */
	@NonNull
	public static String toString(@NonNull final ApplicationInfo info) {
		final StringBuilder sb = new StringBuilder();
		final StringBuilderPrinter printer = new StringBuilderPrinter(sb);
		info.dump(printer, "");
		return sb.toString();
	}

//--------------------------------------------------------------------------------
	/**
	 * デフォルトActivityのComponentNameを取得する
	 * @param context
	 * @param packageName
	 * @return
	 */
	@Nullable
	public static ComponentName getDefaultActivityName(
		@NonNull final Context context,
		@NonNull final String packageName) {

		String activityFQCN = null;
		final PackageManager pm = context.getPackageManager();
		@Nullable
		final Intent intent = pm.getLaunchIntentForPackage(packageName);
		return intent != null ? intent.getComponent() : null;
	}

	/**
	 * デフォルトActivityのFQCN文字列を取得する
	 * @param context
	 * @param packageName
	 * @return
	 */
	@Nullable
	public static String getDefaultActivity(
		@NonNull final Context context,
		@NonNull final String packageName) {

		@Nullable
		final ComponentName componentName = getDefaultActivityName(context, packageName);
		return componentName != null ? componentName.getClassName() : null;
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
	@Nullable
	public static ActivityInfo findLauncherActivity(
		@NonNull final Context context,
		@Nullable final AppInfoFilterCallback filter) {

		return findActivity(context,
			new Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER),
			filter);
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
	@Nullable
	public static ActivityInfo findActivity(
		@NonNull final Context context,
		@NonNull final Intent intent,
		@Nullable final AppInfoFilterCallback filter) {

		final PackageManager pm = context.getPackageManager();
		@NonNull
		final List<ResolveInfo> list;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL | PackageManager.GET_META_DATA);
		} else {
			list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
		}
		ActivityInfo found = null;
		for (final ResolveInfo item: list) {
			final ActivityInfo info = item.activityInfo;
			if ((filter == null) || filter.onFilter(info.applicationInfo)) {
				if (TextUtils.isEmpty(info.packageName)) {
					try {
						info.packageName = info.applicationInfo.packageName;
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
				if (TextUtils.isEmpty(info.targetActivity)) {
					if (info.name.startsWith(".")) {
						info.targetActivity = info.packageName + info.name;
					} else {
						info.targetActivity = info.name;
					}
				}
				found = info;
				break;
			}
		}
		if (DEBUG) Log.v(TAG, "findActivity:result=" + found);
		return found;
	}

	/**
	 * ランチャーActivity一覧を取得
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<ActivityInfo> getLauncherActivities(
		@NonNull final Context context,
		@Nullable final AppInfoFilterCallback filter) {

		return getActivities(context,
			new Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER),
			filter);
	}

	/**
	 * ランチャーActivity一覧を取得
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<ComponentName> getLauncherActivityNames(
		@NonNull final Context context,
		@Nullable final AppInfoFilterCallback filter) {

		return getActivityNames(context,
			new Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER),
			filter);
	}

	/**
	 * 指定したIntentで起動可能なActivity一覧を取得
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<ActivityInfo> getActivities(
		@NonNull final Context context,
		@NonNull final Intent intent,
		@Nullable final AppInfoFilterCallback filter) {

		final List<ActivityInfo> result = new ArrayList<>();
		final PackageManager pm = context.getPackageManager();
		@NonNull
		final List<ResolveInfo> list;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL | PackageManager.GET_META_DATA);
		} else {
			list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
		}
		for (final ResolveInfo item: list) {
			final ActivityInfo info = item.activityInfo;
			if ((filter == null) || filter.onFilter(info.applicationInfo)) {
				if (TextUtils.isEmpty(info.packageName)) {
					try {
						info.packageName = info.applicationInfo.packageName;
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
				if (TextUtils.isEmpty(info.targetActivity)) {
					if (info.name.startsWith(".")) {
						info.targetActivity = info.packageName + info.name;
					} else {
						info.targetActivity = info.name;
					}
				}
				result.add(info);
			}
		}
		if (DEBUG) Log.v(TAG, "getLauncherActivities:n=" + result.size());
		return result;
	}

	/**
	 * 指定したIntentで起動可能なActivity一覧を取得
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<ComponentName> getActivityNames(
		@NonNull final Context context,
		@NonNull final Intent intent,
		@Nullable final AppInfoFilterCallback filter) {

		final List<ComponentName> result = new ArrayList<>();
		final PackageManager pm = context.getPackageManager();
		@NonNull
		final List<ResolveInfo> list;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL | PackageManager.GET_META_DATA);
		} else {
			list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
		}
		for (final ResolveInfo item: list) {
			final ActivityInfo info = item.activityInfo;
			if ((filter == null) || filter.onFilter(info.applicationInfo)) {
				if (TextUtils.isEmpty(info.packageName)) {
					try {
						info.packageName = info.applicationInfo.packageName;
					} catch (final Exception e) {
						if (DEBUG) Log.w(TAG, e);
					}
				}
				if (TextUtils.isEmpty(info.targetActivity)) {
					if (info.name.startsWith(".")) {
						info.targetActivity = info.packageName + info.name;
					} else {
						info.targetActivity = info.name;
					}
				}
				result.add(new ComponentName(info.packageName, info.name));
			}
		}
		if (DEBUG) Log.v(TAG, "getLauncherActivities:n=" + result.size());
		return result;
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
	@NonNull
	public static List<String> getInstalledPackages(@NonNull final Context context) {
		final List<String> result = new ArrayList<>();

		try {
			final Process p = Runtime.getRuntime().exec("pm list packages -u");
			p.waitFor();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				line = line.replace("package:", "");
				result.add(line);
			}
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, "getInstalledPackages", e);
		}
		return result;
	}

	/**
	 * 起動可能なアクティビティを保持するアプリのパッケージ名一覧を取得する
	 * @param context
	 * @return
	 */
	@NonNull
	public static List<String> getLauncherPackages(@NonNull final Context context) {
		final List<String> result = new ArrayList<>();
		final PackageManager pm = context.getPackageManager();
		final Intent intent = new Intent(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_LAUNCHER);
		@NonNull
		final List<ResolveInfo> list;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
		} else {
			list = pm.queryIntentActivities(intent, 0);
		}
		for (final ResolveInfo item: list) {
			final ApplicationInfo app = item.activityInfo.applicationInfo;
			result.add(app.packageName);
		}
		return result;
	}
}
