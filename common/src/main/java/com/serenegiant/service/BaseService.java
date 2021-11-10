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

package com.serenegiant.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.notification.NotificationFactory;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.HandlerThreadHandler;

/**
 * サービスに各種ユーティリティーメソッドを追加
 * FIXME NotificationCompat.Builderを直接使う代わりにNotificationBuilderを使うように変更する
 */
public abstract class BaseService extends LifecycleService {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = BaseService.class.getSimpleName();

	private static final int NOTIFICATION_ID = R.string.service_name;

	protected final Object mSync = new Object();
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private Handler mAsyncHandler;
	private LocalBroadcastManager mLocalBroadcastManager;
	private volatile boolean mDestroyed;

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.v(TAG, "onCreate:");
		final Context app_context = getApplicationContext();
		synchronized (mSync) {
			mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
			final IntentFilter filter = createIntentFilter();
			if ((filter != null) && filter.countActions() > 0) {
				mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, filter);
			}
			if (mAsyncHandler == null) {
				mAsyncHandler = HandlerThreadHandler.createHandler(getClass().getSimpleName());
			}
		}
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		mDestroyed = true;
		synchronized (mSync) {
			mUIHandler.removeCallbacksAndMessages(null);
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(null);
				try {
					mAsyncHandler.getLooper().quit();
				} catch (final Exception e) {
					// ignore
				}
				mAsyncHandler = null;
			}
			if (mLocalBroadcastManager != null) {
				try {
					mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
				} catch (final Exception e) {
					// ignore
				}
				mLocalBroadcastManager = null;
			}
		}
		super.onDestroy();
	}

	protected boolean isDestroyed() {
		return mDestroyed;
	}

	/**
	 * create IntentFilter to receive local broadcast
	 * @return null if you don't want to receive local broadcast
	 */
	protected abstract IntentFilter createIntentFilter();

	/** BroadcastReceiver to receive local broadcast */
	private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (DEBUG) Log.v(TAG, "onReceive:" + intent);
			try {
				onReceiveLocalBroadcast(context, intent);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	};

	protected abstract void onReceiveLocalBroadcast(final Context context, final Intent intent);

	/**
	 * local broadcast asynchronously
	 * @param intent
	 */
	protected void sendLocalBroadcast(final Intent intent) {
		if (DEBUG) Log.v(TAG, "sendLocalBroadcast:" + intent);
		synchronized (mSync) {
			if (mLocalBroadcastManager != null) {
				mLocalBroadcastManager.sendBroadcast(intent);
			}
		}
	}

//================================================================================

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(@DrawableRes final int smallIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {

		showNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			null, null,
			smallIconId, R.drawable.ic_notification,
			title, content,
			true, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param isForegroundService フォアグラウンドサービスとして動作させるかどうか
	 * @param intent
	 */
	protected void showNotification(@DrawableRes final int smallIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final boolean isForegroundService,
		final PendingIntent intent) {

		showNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			null, null,
			smallIconId, R.drawable.ic_notification,
			title, content,
			isForegroundService, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param notificationId
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {
		
		showNotification(notificationId, channelId, null, null,
			smallIconId, largeIconId,
			title, content,
			true, intent);
	}
	
	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * こっちはAndroid 8以降でのグループid/グループ名の指定可能
	 * @param notificationId
	 * @param groupId
	 * @param groupName
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {
		
		showNotification(notificationId, channelId,
			groupId, groupName,
			smallIconId, largeIconId,
			title, content, true, intent);
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
	 * @param isForegroundService フォアグラウンドサービスとして動作させるかどうか
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final boolean isForegroundService,
		final PendingIntent intent) {

		showNotification(notificationId, title, content,
			new NotificationFactory(this, channelId, channelId, 0,
				groupId, groupName, smallIconId, largeIconId) {

				@Override
				public boolean isForegroundService() {
					if (DEBUG) Log.v(TAG, "showNotification:isForegroundService=" + isForegroundService);
					return isForegroundService;
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
	protected void showNotification(final int notificationId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		@NonNull final NotificationFactory factory) {
	
		if (DEBUG) Log.v(TAG, "showNotification:");
		final NotificationManager manager
			= ContextUtils.requireSystemService(this, NotificationManager.class);
		final Notification notification = factory.createNotification(content, title);
		if (factory.isForegroundService()) {
			startForeground(notificationId, notification);
		}
		if (DEBUG) Log.v(TAG, "showNotification:notify");
		manager.notify(notificationId, notification);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	protected void releaseNotification() {
		releaseNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			R.drawable.ic_notification, R.drawable.ic_notification,
			getString(R.string.service_name), getString(R.string.service_stop));
	}
	
	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 * @param notificationId
	 * @param channelId
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ
	 * @param largeIconId
	 * @param title
	 * @param content
	 */
	@SuppressLint("NewApi")
	protected void releaseNotification(final int notificationId,
		@NonNull final String channelId,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		showNotification(notificationId, channelId, smallIconId, largeIconId, title, content, null);
		releaseNotification(notificationId, channelId);
	}
	
	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	@SuppressLint("NewApi")
	protected void releaseNotification(final int notificationId,
		@NonNull final String channelId) {

		if (DEBUG) Log.v(TAG, "releaseNotification:");
		stopForeground(true);
		cancelNotification(notificationId, channelId);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスの状態は変化しない
	 * @param notificationId
	 * @param channelId Android 8以降でnull以外なら対応するNotificationChannelを削除する.
	 * 			nullまたはAndroid 8未満の場合は何もしない
	 */
	@SuppressLint("NewApi")
	protected void cancelNotification(final int notificationId,
		@Nullable final String channelId) {

		if (DEBUG) Log.v(TAG, "cancelNotification:");
		final NotificationManager manager
			= ContextUtils.requireSystemService(this, NotificationManager.class);
		manager.cancel(notificationId);
		releaseNotificationChannel(channelId);
	}

	/**
	 * 通知領域を開放する。
	 * フォアグラウンドサービスの状態は変化しない。
	 * Android 8以降のNotificationChannelを削除しない
	 * @param notificationId
	 */
	@SuppressLint("NewApi")
	protected void cancelNotification(final int notificationId) {

		cancelNotification(notificationId, null);
	}
	
	/**
	 * 指定したNotificationChannelを破棄する
	 * @param channelId
	 */
	@SuppressLint("NewApi")
	protected void releaseNotificationChannel(@Nullable final String channelId) {
		if (DEBUG) Log.v(TAG, "releaseNotificationChannel:");
		if (!TextUtils.isEmpty(channelId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= ContextUtils.requireSystemService(this, NotificationManager.class);
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
	protected void releaseNotificationGroup(@NonNull final String groupId) {
		if (!TextUtils.isEmpty(groupId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= ContextUtils.requireSystemService(this, NotificationManager.class);
			try {
				manager.deleteNotificationChannelGroup(groupId);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * サービスノティフィケーションを選択した時に実行されるPendingIntentの生成
	 * 普通はMainActivityを起動させる。
	 * デフォルトはnullを返すだけでノティフィケーションを選択しても何も実行されない。
	 * @return
	 */
	protected abstract PendingIntent contextIntent();

//================================================================================
	/**
	 * メインスレッド/UIスレッド上で処理を実行する
	 * @param task
	 * @throws IllegalStateException
	 */
	protected void runOnUiThread(final Runnable task) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		mUIHandler.removeCallbacks(task);
		mUIHandler.post(task);
	}

	/**
	 * メインスレッド/UIスレッド上で処理を実行する
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	protected void runOnUiThread(@Nullable final Runnable task, final long delay)
		throws IllegalStateException {

		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		mUIHandler.removeCallbacks(task);
		if (delay > 0) {
			mUIHandler.postDelayed(task, delay);
		} else {
			mUIHandler.post(task);
		}
	}

	/**
	 * メインスレッド/UIスレッドの実行予定の処理をキャンセルする
	 * @param task
	 */
	protected void removeFromUiThread(@Nullable final Runnable task) {
		mUIHandler.removeCallbacks(task);
	}
	
	/**
	 * メインスレッド/UIスレッドの未実行処理をキャンセルする
	 * @param token
	 */
	protected void removeFromUiThreadAll(@Nullable final Object token) {
		mUIHandler.removeCallbacksAndMessages(token);
	}

	/**
	 * ワーカースレッド上で処理を実行する
	 * @param task
	 * @throws IllegalStateException
	 */
	protected void queueEvent(@Nullable final Runnable task) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		queueEvent(task, 0);
	}

	/**
	 * ワーカースレッド上で処理を実行する
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	protected void queueEvent(@Nullable final Runnable task, final long delay) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
				if (delay > 0) {
					mAsyncHandler.postDelayed(task, delay);
				} else {
					mAsyncHandler.post(task);
				}
			} else {
				throw new IllegalStateException("worker thread is not ready");
			}
		}
	}

	/**
	 * ワーカースレッドの上の処理をキャンセルする
	 * @param task
	 */
	protected void removeEvent(@Nullable final Runnable task) {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
			}
		}
	}

	/**
	 * ワーカースレッド上の待機中の処理をキャンセルする
	 * @param token
	 */
	protected void removeEventAll(@Nullable final Object token) {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(token);
			}
		}
	}
	
	protected Handler getAsyncHandler() throws IllegalStateException {
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		synchronized (mSync) {
			return mAsyncHandler;
		}
	}
}
