/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.utils.HandlerThreadHandler;

public abstract class BaseService extends Service {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = BaseService.class.getSimpleName();

	private static final int NOTIFICATION_ID = R.string.service_name;

	protected final Object mSync = new Object();
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private Handler mAsyncHandler;
	private LocalBroadcastManager mLocalBroadcastManager;
	private NotificationManager mNotificationManager;
	private volatile boolean mDestroyed;

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.v(TAG, "onCreate:");
		final Context app_context = getApplicationContext();
		synchronized (mSync) {
			mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
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
			if (mAsyncHandler != null) {
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
			mNotificationManager = null;
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
		synchronized (mSync) {
			if (mLocalBroadcastManager != null) {
				mLocalBroadcastManager.sendBroadcast(intent);
			}
		}
	}

//================================================================================
	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param title_id
	 * @param content_id
	 * @param intent
	 */
	protected void showNotification(final int icon_id, final int title_id, final int content_id, final PendingIntent intent) {
		synchronized (mSync) {
			if (mNotificationManager != null) {
				try {
					final String title = getString(title_id);
					final String content = getString(content_id);
					final Notification notification = new NotificationCompat.Builder(this, null)
						.setContentTitle(title)
						.setContentText(content)
						.setSmallIcon(R.drawable.ic_notification)  // the status icon
						.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification))
						.setStyle(new NotificationCompat.BigTextStyle()
							.setBigContentTitle(title)
							.bigText(content)
							.setSummaryText(content))
						.setContentIntent(intent)
						.build();
					startForeground(NOTIFICATION_ID, notification);
					mNotificationManager.notify(NOTIFICATION_ID, notification);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	protected void releaseNotification() {
		showNotification(R.drawable.ic_notification, R.string.service_name, R.string.service_stop, null);
		stopForeground(true);
		synchronized (mSync) {
			if (mNotificationManager != null) {
				mNotificationManager.cancel(NOTIFICATION_ID);
				mNotificationManager = null;
			}
		}
	}

	/**
	 * 通知領域からアクティビティを起動するためのインテントを生成する
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

	protected Handler getAsyncHandler() throws IllegalStateException {
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		synchronized (mSync) {
			return mAsyncHandler;
		}
	}
}
