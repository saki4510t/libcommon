package com.serenegiant.notification;
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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

public class NotificationCompat {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = NotificationCompat.class.getSimpleName();

	private static final int NOTIFICATION_ID = R.string.service_name;

	private NotificationCompat() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param foregroundServiceType
	 * @param intent
	 */
	public static void showNotification(
		@NonNull final Service service,
		@DrawableRes final int smallIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final int foregroundServiceType,
		@Nullable final PendingIntent intent) {

		showNotification(
			service,
			NOTIFICATION_ID,
			service.getString(R.string.service_name),
			null, null,
			smallIconId, R.drawable.ic_notification,
			title, content,
			foregroundServiceType, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param notificationId
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param foregroundServiceType
	 * @param intent
	 */
	public static void showNotification(
		@NonNull final Service service,
		final int notificationId,
		@NonNull final String channelId,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final int foregroundServiceType,
		@Nullable final PendingIntent intent) {

		showNotification(service, notificationId, channelId, null, null,
			smallIconId, largeIconId,
			title, content,
			foregroundServiceType, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。
	 * こっちはAndroid 8以降でのグループid/グループ名の指定可能
	 * @param notificationId
	 * @param channelId
	 * @param groupId
	 * @param groupName
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param largeIconId
	 * @param title
	 * @param content
	 * @param foregroundServiceType フォアグラウンドサービスとして動作させるかどうか
	 * @param intent
	 */
	public static void showNotification(
		@NonNull final Service service,
		final int notificationId,
		@NonNull final String channelId,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final int foregroundServiceType,
		@Nullable final PendingIntent intent) {

		showNotification(
			service,
			notificationId, title, content,
			new NotificationFactoryCompat(service, channelId, channelId, 0,
				groupId, groupName, smallIconId, largeIconId, foregroundServiceType) {

				@Override
				public boolean isForegroundService() {
					if (DEBUG) Log.v(TAG, "showNotification:foregroundServiceType=" + foregroundServiceType);
					return foregroundServiceType != 0;
				}

				@Nullable
				@Override
				protected PendingIntent createContentIntent() {
					return intent;
				}
			}
		);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。
	 * @param notificationId
	 * @param title
	 * @param content
	 * @param factory
	 */
	public static void showNotification(
		@NonNull final Service service,
		final int notificationId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		@NonNull final NotificationFactoryCompat factory) {

		if (DEBUG) Log.v(TAG, "showNotification:");
		final NotificationManager manager
			= ContextUtils.requireSystemService(service, NotificationManager.class);
		final Notification notification = factory.createNotification(content, title);
		if (factory.isForegroundService()) {
			ServiceCompat.startForeground(service, notificationId, notification, factory.getForegroundServiceType());
		}
		if (DEBUG) Log.v(TAG, "showNotification:notify");
		manager.notify(notificationId, notification);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	public static void releaseNotification(
		@NonNull final Service service
	) {
		releaseNotification(service, NOTIFICATION_ID,
			service.getString(R.string.service_name));
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	@SuppressLint("NewApi")
	public static void releaseNotification(
		@NonNull final Service service,
		final int notificationId,
		@NonNull final String channelId) {

		if (DEBUG) Log.v(TAG, "releaseNotification:");
		service.stopForeground(true);
		cancelNotification(service, notificationId, channelId);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスの状態は変化しない
	 * @param notificationId
	 * @param channelId Android 8以降でnull以外なら対応するNotificationChannelを削除する.
	 * 			nullまたはAndroid 8未満の場合は何もしない
	 */
	@SuppressLint("NewApi")
	public static void cancelNotification(
		@NonNull final Service service,
		final int notificationId,
		@Nullable final String channelId) {

		if (DEBUG) Log.v(TAG, "cancelNotification:");
		final NotificationManager manager
			= ContextUtils.requireSystemService(service, NotificationManager.class);
		manager.cancel(notificationId);
		releaseNotificationChannel(service, channelId);
	}

	/**
	 * 通知領域を開放する。
	 * フォアグラウンドサービスの状態は変化しない。
	 * Android 8以降のNotificationChannelを削除しない
	 * @param notificationId
	 */
	@SuppressLint("NewApi")
	public static void cancelNotification(
		@NonNull final Service service,
		final int notificationId) {

		cancelNotification(service, notificationId, null);
	}

	/**
	 * 指定したNotificationChannelを破棄する
	 * @param channelId
	 */
	@SuppressLint("NewApi")
	public static void releaseNotificationChannel(
		@NonNull final Service service,
		@Nullable final String channelId) {
		if (DEBUG) Log.v(TAG, "releaseNotificationChannel:");
		if (!TextUtils.isEmpty(channelId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= ContextUtils.requireSystemService(service, NotificationManager.class);
			try {
				manager.deleteNotificationChannel(channelId);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * 指定したNotificationGroupを削除する
	 * @param groupId
	 */
	@SuppressLint("NewApi")
	public static void releaseNotificationGroup(
		@NonNull final Service service,
		@NonNull final String groupId) {
		if (!TextUtils.isEmpty(groupId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= ContextUtils.requireSystemService(service, NotificationManager.class);
			try {
				manager.deleteNotificationChannelGroup(groupId);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

}
