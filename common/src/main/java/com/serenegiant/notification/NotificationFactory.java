package com.serenegiant.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

/**
 * Notification生成用のファクトリークラス
 */
public class NotificationFactory {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = NotificationFactory.class.getSimpleName();

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

	/**
	 * コンストラクタ
	 * @param channelId
	 * @param channelTitle
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ(IllegalArgumentExceptionを投げる)
	 */
	@SuppressLint("InlinedApi")
	public NotificationFactory(
		@NonNull final Context context,
		@NonNull final String channelId, @Nullable final String channelTitle,
		@DrawableRes final int smallIconId) {

		this(context, channelId, channelId,
			BuildCheck.isAndroid7() ? NotificationManager.IMPORTANCE_NONE : 0,
			null, null, smallIconId, R.drawable.ic_notification);
	}

	/**
	 * コンストラクタ
	 * @param channelId
	 * @param channelTitle
	 * @param smallIconId	API21未満ではVectorDrawableを指定してはだめ(IllegalArgumentExceptionを投げる)
	 * @param largeIconId
	 */
	@SuppressLint("InlinedApi")
	public NotificationFactory(
		@NonNull final Context context,
		@NonNull final String channelId, @Nullable final String channelTitle,
		@DrawableRes final int smallIconId, @DrawableRes final int largeIconId) {

		this(context, channelId, channelId,
			BuildCheck.isAndroid7() ? NotificationManager.IMPORTANCE_NONE : 0,
			null, null, smallIconId, largeIconId);
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
	public NotificationFactory(
		@NonNull final Context context,
		@NonNull final String channelId,
		@Nullable final String channelTitle,
		final int importance,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId, @DrawableRes final int largeIconId)
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
//				if (DEBUG) Log.v(TAG, "createNotificationBuilder:smallIcon=" + drawable);
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

	/**
	 * Notification生成用のファクトリーメソッド
	 * @param title
	 * @param content
	 * @return
	 */
	@SuppressLint("NewApi")
	@NonNull
	public Notification createNotification(
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		if (DEBUG) Log.v(TAG, "createNotification:");
		if (BuildCheck.isOreo()) {
			createNotificationChannel(context);
		}

		final NotificationCompat.Builder builder
			= createNotificationBuilder(context, title, content);

		if (DEBUG) Log.v(TAG, "createNotification:build");
		return builder.build();
	}

	/**
	 * Android 8以降用にNotificationChannelを生成する処理
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
	@RequiresApi(Build.VERSION_CODES.O)
	protected void createNotificationChannel(
		@NonNull final Context context) {

		if (DEBUG) Log.v(TAG, "createNotificationChannel:");
		final NotificationManager manager
			= ContextUtils.requireSystemService(context, NotificationManager.class);
		if (manager.getNotificationChannel(channelId) == null) {
			final NotificationChannel channel
				= new NotificationChannel(channelId, channelTitle, importance);
			if (!TextUtils.isEmpty(groupId)) {
				createNotificationChannelGroup(context, groupId, groupName);
				channel.setGroup(groupId);
			}
			channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
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
	@RequiresApi(Build.VERSION_CODES.O)
	@NonNull
	protected NotificationChannel setupNotificationChannel(
		@NonNull final NotificationChannel channel) {

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
	@RequiresApi(Build.VERSION_CODES.O)
	protected void createNotificationChannelGroup(
		@NonNull final Context context,
		@Nullable final String groupId, @Nullable final String groupName) {

		if (DEBUG) Log.v(TAG, "createNotificationChannelGroup:groupId=" + groupId);
		if (!TextUtils.isEmpty(groupId)) {
			final NotificationManager manager
				= ContextUtils.requireSystemService(context, NotificationManager.class);
			final List<NotificationChannelGroup> groups
				= manager.getNotificationChannelGroups();

			NotificationChannelGroup found = null;
			for (final NotificationChannelGroup group: groups) {
				if (groupId.equals(group.getId())) {
					found = group;
					break;
				}
			}
			if (found == null) {
				found = new NotificationChannelGroup(groupId,
					TextUtils.isEmpty(groupName) ? groupId : groupName);
				manager.createNotificationChannelGroup(
					setupNotificationChannelGroup(found));
			}
		}
	}

	/**
	 * Android 8以降用にNotificationChannelGroupを生成する処理
	 * NotificationManager#getNotificationChannelGroupsに同じグループidの
	 * ものが存在しない時のみ新規に作成する
	 * NotificationManager#createNotificationChannelGroupが呼ばれる直前に
	 * #createNotificationChannelGroupから呼ばれる
	 * @param group
	 * @return
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	@NonNull
	protected NotificationChannelGroup setupNotificationChannelGroup(
		@NonNull final NotificationChannelGroup group) {

		return group;
	}

	@SuppressLint("InlinedApi")
	@NonNull
	protected NotificationBuilder createNotificationBuilder(
		@NonNull final Context context,
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		if (DEBUG) Log.v(TAG, "createNotificationBuilder:");
		final NotificationBuilder builder
			= new NotificationBuilder(context, channelId, smallIconId) {
			@Override
			protected PendingIntent createContentIntent() {
				return NotificationFactory.this.createContentIntent();
			}
		};
		builder.setContentTitle(title)
			.setContentText(content)
			.setSmallIcon(smallIconId)  // the status icon
			.setStyle(new NotificationCompat.BigTextStyle()
				.setBigContentTitle(title)
				.bigText(content)
				.setSummaryText(content));
		final PendingIntent contentIntent = createContentIntent();
		if (contentIntent != null) {
			builder.setContentIntent(contentIntent);
		}
		final PendingIntent deleteIntent = createDeleteIntent();
		if (deleteIntent != null) {
			builder.setDeleteIntent(deleteIntent);
		}
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
