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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.system.BuildCheck;

import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

/**
 * Notification生成用のファクトリークラス
 * NotificationFactoryをandroidxのNotification関係のcompat系を使って実装しなおした
 */
public class NotificationFactoryCompat {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = NotificationFactoryCompat.class.getSimpleName();

	@NonNull
	protected final Context context;
	@NonNull
	protected final String channelId;
	@Nullable
	protected final String channelTitle;
	protected final int importance;
	@Nullable
	protected final String groupId;
	@Nullable
	protected final String groupName;
	@DrawableRes
	protected final int smallIconId;	// API21未満ではVectorDrawableを指定してはだめ
	@DrawableRes
	protected final int largeIconId;
	protected final int foregroundServiceType;
	/**
	 * コンストラクタ
	 * @param channelId
	 * @param channelTitle
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ(IllegalArgumentExceptionを投げる)
	 */
	public NotificationFactoryCompat(
		@NonNull final Context context,
		@NonNull final String channelId, @Nullable final String channelTitle,
		@DrawableRes final int smallIconId, final int foregroundServiceType) {

		this(context, channelId, channelId,
			NotificationManagerCompat.IMPORTANCE_NONE,
			null, null, smallIconId, R.drawable.ic_notification,
			foregroundServiceType);
	}

	/**
	 * コンストラクタ
	 * @param channelId
	 * @param channelTitle
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ(IllegalArgumentExceptionを投げる)
	 * @param largeIconId
	 */
	public NotificationFactoryCompat(
		@NonNull final Context context,
		@NonNull final String channelId, @Nullable final String channelTitle,
		@DrawableRes final int smallIconId, @DrawableRes final int largeIconId,
		final int foregroundServiceType) {

		this(context, channelId, channelId,
			NotificationManagerCompat.IMPORTANCE_NONE,
			null, null, smallIconId, largeIconId,
			foregroundServiceType);
	}

	/**
	 * コンストラクタ
	 * @param channelId
	 * @param channelTitle
	 * @param importance
	 * @param groupId
	 * @param groupName
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ(IllegalArgumentExceptionを投げる)
	 * @param largeIconId
	 */
	public NotificationFactoryCompat(
		@NonNull final Context context,
		@NonNull final String channelId,
		@Nullable final String channelTitle,
		final int importance,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId, @DrawableRes final int largeIconId,
		final int foregroundServiceType)
			throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "コンストラクタ");
		this.context = context;
		this.channelId = channelId;
		this.channelTitle = TextUtils.isEmpty(channelTitle) ? channelId : channelTitle;
		this.importance = importance;
		this.groupId = groupId;
		this.groupName = TextUtils.isEmpty(groupName) ? groupId : groupName;
		this.smallIconId = smallIconId;
		this.largeIconId = largeIconId;
		this.foregroundServiceType = foregroundServiceType;
		if (!BuildCheck.isAPI21()) {
			// API21未満だとVectorDrawableをsmall iconに割り当てれないのでチェックを追加
			// Builder#setSmallIconをする前にチェックしてAPI21未満&vector drawableの時には
			// #setSmallIconを呼ばなくてもサービス自体は動作はできるけど通知アイコンをセットしないと
			// 通知領域に表示されない＆API21未満でvector drawableを通知アイコンにセットするのは
			// プログラム上のバグなのでNotificationFactoryのコンストラクタで例外生成する
			Drawable drawable;
			try {
				// ContextCompat.getDrawableはVectorDrawableを読み込もうとすると例外生成する
				drawable = ContextCompat.getDrawable(context, smallIconId);
			} catch (final Exception e) {
//				if (DEBUG) Log.d(TAG, "createNotificationBuilder:failed to load small icon, try load as VectorDrawableCompat", e);
				drawable = VectorDrawableCompat.create(context.getResources(), smallIconId, null);
			}
//			if (DEBUG) Log.v(TAG, "createNotificationBuilder:smallIcon=" + drawable);
			if (drawable instanceof VectorDrawableCompat) {
				throw new IllegalArgumentException("Can't use vector drawable as small icon before API21!");
			}
		}
	}

	/**
	 * フォアグラウンドサービスにするかどうか
	 * @return
	 */
	public boolean isForegroundService() {
		return true;
	}

	public int getForegroundServiceType() {
		return foregroundServiceType;
	}

	/**
	 * Notification生成用のファクトリーメソッド
	 * @param title
	 * @param content
	 * @return
	 */
	@NonNull
	public Notification createNotification(
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		if (DEBUG) Log.v(TAG, "createNotification:");
		createNotificationChannel(context);
		final NotificationCompat.Builder builder
			= createNotificationBuilder(context, title, content);

		if (DEBUG) Log.v(TAG, "createNotification:build");
		return builder.build();
	}

	/**
	 * NotificationChannelを生成する処理
	 * NotificationManager#getNotificationChannelがnullを
	 * 返したときのみ新規に作成する
	 * #createNotificationrから呼ばれる
	 * showNotification
	 * 	-> createNotificationBuilder
	 * 		-> (createNotificationChannel
	 * 			-> (createNotificationChannelGroup)
	 * 			-> setupNotificationChannel)
	 * 	-> createNotification
	 * 	-> startForeground -> NotificationManager#notify
	 * @param context
	 * @return
	 */
	protected void createNotificationChannel(
		@NonNull final Context context) {

		if (DEBUG) Log.v(TAG, "createNotificationChannel:");
		final NotificationManagerCompat manager = NotificationManagerCompat.from(context);
		if (manager.getNotificationChannel(channelId) == null) {
			final NotificationChannelCompat.Builder builder
				= new NotificationChannelCompat.Builder(channelId, importance);
			builder.setName(channelTitle);
			if (!TextUtils.isEmpty(groupId)) {
				createNotificationChannelGroup(context, groupId, groupName);
				builder.setGroup(groupId);
			}
			final NotificationChannelCompat channel = builder.build();
			manager.createNotificationChannel(setupNotificationChannel(channel));
		}
	}

	/**
	 * Android 8以降用にNotificationChannelを生成する処理
	 * NotificationManager#getNotificationChannelがnullを
	 * 返したときのみ新規に作成する
	 * NotificationManager#createNotificationChannelが呼ばれる直前に
	 * #createNotificationChannelrから呼ばれる
	 * @param channel
	 * @return
	 */
	@NonNull
	protected NotificationChannelCompat setupNotificationChannel(
		@NonNull final NotificationChannelCompat channel) {

		if (DEBUG) Log.v(TAG, "setupNotificationChannel:");
		return channel;
	}

	/**
	 * Android 8以降用にNotificationChannelGroupを生成する処理
	 * NotificationManager#getNotificationChannelGroupsに同じグループidの
	 * ものが存在しない時のみ新規に作成する
	 * #createNotificationBuilderから呼ばれる
	 * showNotification
	 * 	-> createNotificationBuilder
	 * 		-> (createNotificationChannel
	 * 			-> (createNotificationChannelGroup)
	 * 			-> setupNotificationChannel)
	 * 	-> createNotification
	 * 	-> startForeground -> NotificationManager#notify
	 * @param groupId
	 * @param groupName
	 * @return
	 */
	protected void createNotificationChannelGroup(
		@NonNull final Context context,
		@Nullable final String groupId, @Nullable final String groupName) {

		if (DEBUG) Log.v(TAG, "createNotificationChannelGroup:groupId=" + groupId);
		if (!TextUtils.isEmpty(groupId)) {
			final NotificationManagerCompat manager = NotificationManagerCompat.from(context);
			final List<NotificationChannelGroupCompat> groups
				= manager.getNotificationChannelGroupsCompat();

			NotificationChannelGroupCompat found = null;
			for (final NotificationChannelGroupCompat group: groups) {
				if (groupId.equals(group.getId())) {
					found = group;
					break;
				}
			}
			if (found == null) {
				final NotificationChannelGroupCompat.Builder builder
					= new NotificationChannelGroupCompat.Builder(groupId);
				builder.setName(TextUtils.isEmpty(groupName) ? groupId : groupName);
				found = builder.build();
				manager.createNotificationChannelGroup(
					setupNotificationChannelGroup(found));
			}
		}
	}

	/**
	 * NotificationChannelGroupを生成する処理
	 * NotificationManager#getNotificationChannelGroupsに同じグループidの
	 * ものが存在しない時のみ新規に作成する
	 * NotificationManager#createNotificationChannelGroupが呼ばれる直前に
	 * #createNotificationChannelGroupから呼ばれる
	 * @param group
	 * @return
	 */
	@NonNull
	protected NotificationChannelGroupCompat setupNotificationChannelGroup(
		@NonNull final NotificationChannelGroupCompat group) {

		return group;
	}

	@NonNull
	protected NotificationCompat.Builder createNotificationBuilder(
		@NonNull final Context context,
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		if (DEBUG) Log.v(TAG, "createNotificationBuilder:");
		final NotificationCompat.Builder builder
			= new NotificationCompat.Builder(context, channelId);
		builder.setContentTitle(title)
			.setContentText(content)
			.setContentIntent(createContentIntent())
			.setDeleteIntent(createDeleteIntent())
			.setSmallIcon(smallIconId)  // the status icon
			.setStyle(new NotificationCompat.BigTextStyle()
				.setBigContentTitle(title)
				.bigText(content)
				.setSummaryText(content));
		if (!TextUtils.isEmpty(groupId)) {
			builder.setGroup(groupId);
			// XXX 最初だけbuilder.setGroupSummaryが必要かも?
		}
		if (largeIconId != 0) {
			builder.setLargeIcon(
				BitmapFactory.decodeResource(context.getResources(), largeIconId));
		}
		return builder;
	}

	@Nullable
	protected PendingIntent createContentIntent() {
		return null;
	}

	@Nullable
	protected PendingIntent createDeleteIntent() {
		return null;
	}
}
