package com.serenegiant.system;
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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.serenegiant.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;

public final class CrashExceptionHandler implements UncaughtExceptionHandler {
	private static final String TAG = CrashExceptionHandler.class.getSimpleName();

	/* package */static final String LOG_NAME = "crashrepo.txt";
	/* package */static final String MAIL_TO = "t_saki@serenegiant.com";

	private static final int REQUEST_RESTART_ACTIVITY = 2039;

	public static void registerCrashHandler(@NonNull final Context app_context) {
		Thread.setDefaultUncaughtExceptionHandler(new CrashExceptionHandler(app_context));
	}

	/**
	 * アプリがクラッシュした際に指定したPendingIntentを実行するように設定
	 * @param context
	 * @param restartIntent
	 */
	@Deprecated
	public static void setAutoRestart(@NonNull final Context context,
		@NonNull final PendingIntent restartIntent,
		final long delayMs) {

		final UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
		final UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(@NonNull final Thread thread, @NonNull final Throwable throwable) {
				try {
					final AlarmManager am = ContextUtils.requireSystemService(context, AlarmManager.class);
					am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, restartIntent);
				} finally {
					original.uncaughtException(thread, throwable);
				}
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(handler);
	}

	/**
	 * アプリがクラッシュしたときに指定したActivityを起動するように設定
	 * @param context
	 * @param activityClass
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public static void setAutoRestart(@NonNull final Context context,
		@NonNull final Class<? extends Activity> activityClass,
		final long delayMs) {

		final PendingIntent intent = PendingIntent.getActivity(
			context.getApplicationContext(),
			REQUEST_RESTART_ACTIVITY,
			Intent.makeMainActivity(new ComponentName(context, activityClass)),
			PendingIntent.FLAG_CANCEL_CURRENT);
		setAutoRestart(context, intent, delayMs);
	}

/*	public static final void sendReport(final Context context) {
		final File file = getFileStreamPath(this, LOG_NAME);
		if (file.exists() && checkSdCardStatus()) {
		    final String attachmentFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + getString(R.string.appName) + File.separator + CrashExceptionHandler.FILE_NAME;
		    final File attachmentFile = new File(attachmentFilePath);
		    if(!attachmentFile.getParentFile().exists()){
		        attachmentFile.getParentFile().mkdirs();
		    }
		    file.renameTo(attachmentFile);
		    Intent intent = createSendMailIntent(MAIL_TO, "crash report", "********** crash report **********");
		    intent = addFile(intent, attachmentFile);
		    final Intent gmailIntent = createGmailIntent(intent);
		    if (canIntent(this, gmailIntent)){
		        startActivity(gmailIntent);
		    } else if (canIntent(this, intent)) {
		        startActivity(Intent.createChooser(intent, getString(R.string.sendCrashReport)));
		    } else {
		        showToast(context, R.string.mailerNotFound);
		    }
		    file.delete();
		}
	} */

	private final WeakReference<Context> mWeakContext;
	private final UncaughtExceptionHandler mHandler;

	private CrashExceptionHandler(@NonNull final Context context) {
		mWeakContext = new WeakReference<Context>(context);
		mHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	/**
	 * キャッチされなかった例外発生時に各種情報をJSONでテキストファイルに書き出す
	 */
	@Override
	public void uncaughtException(
		@NonNull final Thread thread, final @NonNull Throwable throwable) {

		final Context context = mWeakContext.get();
		if (context != null) {
			PrintWriter writer = null;
			try {
				final FileOutputStream out
					= context.openFileOutput(LOG_NAME, Context.MODE_PRIVATE);
				writer = new PrintWriter(out);
				final JSONObject json = new JSONObject();
				json.put("Build", getBuildInfo());
				json.put("PackageInfo", getPackageInfo(context));
				json.put("Exception", getExceptionInfo(throwable));
				json.put("SharedPreferences", getPreferencesInfo(context));
				writer.print(json.toString());
				writer.flush();
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final JSONException e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
		try {
			if (mHandler != null) {
				mHandler.uncaughtException(thread, throwable);
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * ビルド情報をJSONで返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject getBuildInfo() throws JSONException {
		final JSONObject buildJson = new JSONObject();
		buildJson.put("BRAND", Build.BRAND);	// キャリア、メーカー名など
		buildJson.put("MODEL", Build.MODEL);	// ユーザーに表示するモデル名
		buildJson.put("DEVICE", Build.DEVICE);	// デバイス名
		buildJson.put("MANUFACTURER", Build.MANUFACTURER);		// 製造者名
		buildJson.put("VERSION.SDK_INT", Build.VERSION.SDK_INT);	// フレームワークのバージョン情報
		buildJson.put("VERSION.RELEASE", Build.VERSION.RELEASE);	// ユーザーに表示するバージョン番号
		return buildJson;
	}

	/**
	 * パッケージ情報を返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject getPackageInfo(@NonNull final Context context)
		throws JSONException {

		final JSONObject packageInfoJson = new JSONObject();
		try {
			final PackageInfo info = context.getPackageManager()
				.getPackageInfo(context.getPackageName(), 0);
			packageInfoJson.put("packageName", info.packageName);
			packageInfoJson.put("versionCode", info.versionCode);
			packageInfoJson.put("versionName", info.versionName);
		} catch (final NameNotFoundException e) {
			packageInfoJson.put("error", e);
		}
		return packageInfoJson;
	}

	/**
	 * 例外情報を返す
	 *
	 * @param throwable
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject getExceptionInfo(final Throwable throwable)
		throws JSONException {

		final JSONObject exceptionJson = new JSONObject();
		exceptionJson.put("name", throwable.getClass().getName());
		exceptionJson.put("cause", throwable.getCause());
		exceptionJson.put("message", throwable.getMessage());
		// StackTrace
		final JSONArray stackTrace = new JSONArray();
		for (final StackTraceElement element : throwable.getStackTrace()) {
			stackTrace.put("at " + LogUtils.getMetaInfo(element));
		}
		exceptionJson.put("stacktrace", stackTrace);
		return exceptionJson;
	}

	/**
	 * Preferencesを返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject getPreferencesInfo(@NonNull final Context context)
		throws JSONException {

		final SharedPreferences preferences
			= PreferenceManager.getDefaultSharedPreferences(context);

		final JSONObject preferencesJson = new JSONObject();
		final Map<String, ?> map = preferences.getAll();
		for (final Entry<String, ?> entry : map.entrySet()) {
			preferencesJson.put(entry.getKey(), entry.getValue());
		}
		return preferencesJson;
	}
}
