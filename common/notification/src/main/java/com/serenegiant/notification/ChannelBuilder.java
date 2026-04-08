package com.serenegiant.notification;
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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.XmlRes;
import androidx.core.app.NotificationManagerCompat;

import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.content.ContextUtils;
import com.serenegiant.utils.ObjectHelper;
import com.serenegiant.content.XmlHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Android 8 (API26)以降でのNotificationChannel作成用のヘルパークラス
 * Android 8未満でも実行はできるが何もしない
 */
@SuppressLint("InlinedApi")
public class ChannelBuilder {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = ChannelBuilder.class.getSimpleName();
	
	public static final String DEFAULT_CHANNEL_ID = NotificationChannel.DEFAULT_CHANNEL_ID;

	/**
	 * フレームワークのアノテーションがhide指定💩なので
	 * しかたなく自前で再定義
	 */
	@IntDef({
		NotificationManagerCompat.IMPORTANCE_UNSPECIFIED,
		NotificationManagerCompat.IMPORTANCE_NONE,
		NotificationManagerCompat.IMPORTANCE_MIN,
		NotificationManagerCompat.IMPORTANCE_LOW,
		NotificationManagerCompat.IMPORTANCE_DEFAULT,
		NotificationManagerCompat.IMPORTANCE_HIGH,
		NotificationManagerCompat.IMPORTANCE_MAX})	// API>=24
	@Retention(RetentionPolicy.SOURCE)
	public @interface Importance {}
	
	public static final Set<Integer> IMPORTANCE = new HashSet<Integer>();
	static {
		Collections.addAll(IMPORTANCE,
			NotificationManagerCompat.IMPORTANCE_UNSPECIFIED,
			NotificationManagerCompat.IMPORTANCE_NONE,
			NotificationManagerCompat.IMPORTANCE_MIN,
			NotificationManagerCompat.IMPORTANCE_LOW,
			NotificationManagerCompat.IMPORTANCE_DEFAULT,
			NotificationManagerCompat.IMPORTANCE_HIGH,
			NotificationManagerCompat.IMPORTANCE_MAX	// API>=24
		);
	}

	/**
	 * フレームワークのアノテーションがhide指定💩なので
	 * しかたなく自前で再定義
	 */
	@IntDef({
		Notification.VISIBILITY_PUBLIC,
		Notification.VISIBILITY_PRIVATE,
		Notification.VISIBILITY_SECRET})
	@Retention(RetentionPolicy.SOURCE)
	public @interface NotificationVisibility {}

	public static final Set<Integer> NOTIFICATION_VISIBILITY = new HashSet<Integer>();
	static {
		Collections.addAll(NOTIFICATION_VISIBILITY,
			Notification.VISIBILITY_PUBLIC,
			Notification.VISIBILITY_PRIVATE,
			Notification.VISIBILITY_SECRET);
	}

	/**
	 * 既存のNotificationChannelが存在すればその設定をコピーしてChannelBuilderを生成する。
	 * 既存のNotificationChannelがなければ新規生成する。
	 * @param context
	 * @param channelId 通知チャネルid
	 * @return
	 */
	@NonNull
	public static ChannelBuilder getBuilder(@NonNull final Context context,
		@NonNull final String channelId) {
		
		if (DEBUG) Log.v(TAG, "getBuilder:" + channelId);
		final NotificationChannel channel
			= NotificationManagerCompat.from(context)
				.getNotificationChannel(channelId);
		if (channel != null) {
			// 既にNotificationChannelが存在する場合はその設定を取得して生成
			final ChannelBuilder builder = new ChannelBuilder(context,
				channelId, channel.getName(), channel.getImportance());
			builder.setLockscreenVisibility(channel.getLockscreenVisibility())
				.setBypassDnd(channel.canBypassDnd())
				.setShowBadge(channel.canShowBadge())
				.setDescription(channel.getDescription())
				.setLightColor(channel.getLightColor())
				.setVibrationPattern(channel.getVibrationPattern())
				.enableLights(channel.shouldShowLights())
				.enableVibration(channel.shouldVibrate())
				.setSound(channel.getSound(), channel.getAudioAttributes())
				.setGroup(channel.getGroup(), null)
				.setCreateIfExists(true);
			return builder;
		} else {
			// 存在しない場合は新規に生成
			return new ChannelBuilder(context,
				channelId, null, NotificationManagerCompat.IMPORTANCE_NONE);
		}
	}
	
	/**
	 * xmlリソースファイルから設定を読み込んで、Android8以降ならNotificationChannelを生成/再設定する
	 * <resources>
	 *	<notificationChannel
	 *		channelId="@string/channel_id1"	// これはロケールに関係ない固定文字列/または翻訳しない文字列リソース
	 *		name="@string/channel_name1"	// 文字列または文字列リソース
	 *		lockscreenVisibility="0"
	 *		bypassDnd="false"
	 *		showBadge="true"
	 *		description="詳細"				// 文字列または文字列リソース
	 *		light="0"
	 *		enableLights="false"
	 *		vibrationPattern="0,500,0,500"
	 *		enableVibration="false"
	 *		sound=""
	 *		isCreateIfExists="false"		// xmlから読み込む場合のデフォルトはfalse
	 *	/>
	 *	<notificationChannel
	 *		...
	 *	/>
	 * </resources>
	 * @param context
	 * @param channelInfoXmlId
	 * @return list of channel id
	 */
	@NonNull
	public static List<String> updateNotificationChannel(@NonNull final Context context,
		@XmlRes final int channelInfoXmlId) {
		if (DEBUG) Log.v(TAG, "updateNotificationChannel:");

		final List<String> result = new ArrayList<>();
		final XmlPullParser parser = context.getResources().getXml(channelInfoXmlId);
		
		try {
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					readEntryOne(context, parser, result);
				}
				eventType = parser.next();
			}
		} catch (final XmlPullParserException e) {
			Log.d(TAG, "XmlPullParserException", e);
		} catch (final IOException e) {
			Log.d(TAG, "IOException", e);
		}
		
		return result;
	}
	
	/**
	 * #updateNotificationChannelの下請け
	 * @param context
	 * @param parser
	 * @param result
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	@SuppressLint("InlinedApi")
	private static void readEntryOne(@NonNull final Context context,
		@NonNull final XmlPullParser parser, @NonNull final List<String> result)
			throws XmlPullParserException, IOException {

		ChannelBuilder builder = null;
		String tag;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
        	tag = parser.getName();
			if (!TextUtils.isEmpty(tag)
				&& (tag.equalsIgnoreCase("notificationChannel"))) {
				
				if (eventType == XmlPullParser.START_TAG) {
					final String channelId = XmlHelper.getAttribute(context, parser,
						null, "channelId", "");
		   			if (!TextUtils.isEmpty(channelId)) {
		   				builder = ChannelBuilder.getBuilder(context, channelId)
		   					.setCreateIfExists(false);	// xmlから読み込む場合のデフォルトはfalse
		   				final int n = parser.getAttributeCount();
		   				for (int i = 0; i < n; i++) {
		   					final String attrName = parser.getAttributeName(i);
		   					if (!TextUtils.isEmpty(attrName)) {
		   						switch (attrName) {
								case "name":
									builder.setName(XmlHelper.getAttribute(context, parser,
										null, "name", builder.getName()));
									break;
								case "importance":
									@Importance
									final int importance = XmlHelper.getAttribute(context, parser,
										null, "importance", builder.getImportance());
									builder.setImportance(importance);
									break;
								case "lockscreenVisibility":
									final int lockscreenVisibility
										= XmlHelper.getAttribute(context, parser,
										null, "lockscreenVisibility",
										builder.getLockscreenVisibility());
									if (NOTIFICATION_VISIBILITY.contains(lockscreenVisibility)) {
										builder.setLockscreenVisibility(lockscreenVisibility);
									}
									break;
								case "bypassDnd":
									builder.setBypassDnd(XmlHelper.getAttribute(context, parser,
										null, "bypassDnd",
										builder.canBypassDnd()));
									break;
								case "showBadge":
									builder.setShowBadge(XmlHelper.getAttribute(context, parser,
										null, "showBadge",
										builder.canShowBadge()));
									break;
								case "description":
									builder.setDescription(XmlHelper.getAttribute(context, parser,
										null, "description", builder.getDescription()));
									break;
								case "light":
									builder.setLightColor(XmlHelper.getAttribute(context, parser,
										null, "light",
										builder.getLightColor()));
								case "enableLights":
									builder.enableLights(XmlHelper.getAttribute(context, parser,
										null, "enableLights",
										builder.shouldShowLights()));
									break;
								case "vibrationPattern":
									final String patternString = XmlHelper.getAttribute(context, parser,
										null, "vibrationPattern", "");
									if (!TextUtils.isEmpty(patternString)) {
										final String[] pats = patternString.trim().split(",");
										if (pats.length > 0) {
											final long[] pattern = new long[pats.length];
											int ix = -1;
											for (final String v: pats) {
												long val = ObjectHelper.asLong(v, 0);
												pattern[++ix] = val;
											}
											if (ix >= 0) {
												final long[] p = Arrays.copyOf(pattern, ix+1);
												builder.setVibrationPattern(p);
											}
										}
									}
									break;
								case "enableVibration":
									builder.enableVibration(XmlHelper.getAttribute(context, parser,
										null, "enableVibration",
										builder.shouldVibrate()));
									break;
								case "sound":
									final String uriString = XmlHelper.getAttribute(context, parser,
										null, "sound", "");
									if (!TextUtils.isEmpty(uriString)) {
										builder.setSound(Uri.parse(uriString), null);
									}
									break;
								case "createIfExists":
									builder.setCreateIfExists(XmlHelper.getAttribute(context, parser,
										null, "createIfExists",
										false));	// xmlから読み込む場合のデフォルトはfalse
									break;
								}
							}
						}
					}
				} else if (eventType == XmlPullParser.END_TAG) {
		  			if (builder != null) {
		  				if (DEBUG) Log.v(TAG, "readEntryOne:build" + builder);
		  				builder.build();
		  				result.add(builder.getId());
		  				return;
					}
				}
			}
			eventType = parser.next();
		}
	}

//================================================================================
	@NonNull
	private final Context mContext;
	@NonNull
	private String channelId;
	@Nullable
	private CharSequence name;
	@Importance
	private int importance;
	@NotificationVisibility
	private int lockscreenVisibility = Notification.VISIBILITY_PRIVATE;
	private boolean bypassDnd = false;
	private boolean showBadge = true;
	private String description;
	private int argb = 0;
	private boolean lights;
	private long[] vibrationPattern;
	private boolean vibration;
	private Uri sound;
	private AudioAttributes audioAttributes;

	@Nullable
	private String groupId;
	@Nullable
	private String groupName;
	private boolean createIfExists = true;
	
	/**
	 * コンストラクタ
	 * チャネルidはDEFAULT_CHANNEL_IDになる
	 * 新規に作成するとわかっている場合・上書きする場合を除いて#getBuilderを使うほうがいい。
	 */
	public ChannelBuilder(@NonNull final Context context) {

		this(context,
			DEFAULT_CHANNEL_ID,
			DEFAULT_CHANNEL_ID,
			NotificationManagerCompat.IMPORTANCE_NONE,
			null, null);
	}
	
	/**
	 * コンストラクタ
	 * 新規に作成するとわかっている場合・既存設定を上書きする場合を除いて#getBuilderを使うほうがいい。
	 * @param channelId nullならチャネルidはDEFAULT_CHANNEL_IDになる
	 * @param name
	 * @param importance
	 */
	public ChannelBuilder(
		@NonNull final Context context,
		@Nullable final String channelId,
		@Nullable final CharSequence name,
		@Importance final int importance) {

		this(context,
			channelId, name, importance,
			null, null);
	}
	
	/**
	 * コンストラクタ
	 * 新規に作成するとわかっている場合・既存設定を上書きする場合を除いて#getBuilderを使うほうがいい。
	 * @param channelId nullならチャネルidはDEFAULT_CHANNEL_IDになる
	 * @param name
	 * @param importance
	 * @param groupId
	 * @param groupName
	 */
	public ChannelBuilder(
		@NonNull final Context context,
		@Nullable final String channelId,
		@Nullable final CharSequence name,
		@Importance final int importance,
		@Nullable final String groupId, @Nullable final String groupName) {

		if (DEBUG) Log.v(TAG, "Constructor:");
		this.mContext = context;
		this.channelId = TextUtils.isEmpty(channelId)
			? DEFAULT_CHANNEL_ID : channelId;
		this.name = name;
		this.importance = importance;
		this.groupId = groupId;
		this.groupName = groupName;
	}

	@NonNull
	@Override
	public String toString() {
		return "ChannelBuilder{" +
			"channelId='" + channelId + '\'' +
			", name=" + name +
			", importance=" + importance +
			", lockscreenVisibility=" + lockscreenVisibility +
			", bypassDnd=" + bypassDnd +
			", showBadge=" + showBadge +
			", description='" + description + '\'' +
			", argb=" + argb +
			", lights=" + lights +
			", vibrationPattern=" + Arrays.toString(vibrationPattern) +
			", vibration=" + vibration +
			", sound=" + sound +
			", audioAttributes=" + audioAttributes +
			", groupId='" + groupId + '\'' +
			", groupName='" + groupName + '\'' +
			", createIfExists=" + createIfExists +
			'}';
	}
	
	/**
	 * NotificationChannelを生成/再設定
	 * Android 8未満では何もしない
	 */
	@SuppressLint("NewApi")
	@Nullable
	public NotificationChannel build() {
		if (BuildCheck.isOreo()) {
			if (DEBUG) Log.v(TAG, "build:");
			return createNotificationChannel(mContext);
		}
		return null;
	}
	
	/**
	 * チャネルidをセット
	 * @param channelId nullならチャネルidはDEFAULT_CHANNEL_IDになる
	 * @return
	 */
	public ChannelBuilder setId(@Nullable final String channelId) {
		// nullや空文字列にならなようにする
		this.channelId = TextUtils.isEmpty(channelId)
			? DEFAULT_CHANNEL_ID : channelId;
		return this;
	}
	
	/**
	 * チャネルidを取得
	 * @return
	 */
	@NonNull
	public String getId() {
		return channelId;
	}
	
	/**
	 * チャネル名(UI上の表示名)をセット
	 * @param name
	 * @return
	 */
	public ChannelBuilder setName(@Nullable final CharSequence name) {
		this.name = name;
		return this;
	}
	
	/**
	 * チャネル名(UI上の表示名)を取得
	 * @return
	 */
	@Nullable
	public CharSequence getName() {
		return name;
	}
	
	/**
	 * チャネルの重要度をセット
	 * NotificationManagerCompat.IMPORTANCE_UNSPECIFIED,
	 * NotificationManagerCompat.IMPORTANCE_NONE,
	 * NotificationManagerCompat.IMPORTANCE_MIN,
	 * NotificationManagerCompat.IMPORTANCE_LOW,
	 * NotificationManagerCompat.IMPORTANCE_DEFAULT,
	 * NotificationManagerCompat.IMPORTANCE_HIGH
	 * NotificationManagerCompat.IMPORTANCE_MAX
	 * のいずれか
	 * @param importance
	 * @return
	 */
	public ChannelBuilder setImportance(@Importance final int importance) {
		this.importance = importance;
		return this;
	}
	
	/**
	 * チャネルの重要度を取得
	 * @return NotificationManagerCompat.IMPORTANCE_UNSPECIFIED,
	 * 			NotificationManagerCompat.IMPORTANCE_NONE,
	 * 			NotificationManagerCompat.IMPORTANCE_MIN,
	 * 			NotificationManagerCompat.IMPORTANCE_LOW,
	 * 			NotificationManNotificationManagerCompatTANCE_DEFAULT,
	 * 			NotificationManageNotificationManagerCompatCE_HIGH
	 * 			NotificationManagerCompat.IMPORTANCE_MAX
	 * 			のいずれか
	 */
	@Importance
	public int getImportance() {
		return importance;
	}
	
	/**
	 * ロック画面上への表示モードをセット
	 * Notification.VISIBILITY_PUBLIC, Notification.VISIBILITY_PRIVATE,
	 * Notification.VISIBILITY_SECRETのいずれか
	 * @param lockscreenVisibility
	 * @return
	 */
	public ChannelBuilder setLockscreenVisibility(
		@NotificationVisibility final int lockscreenVisibility) {

		this.lockscreenVisibility = lockscreenVisibility;
		return this;
	}
	
	/**
	 * ロック画面上への表示モードを取得
	 * @return Notification.VISIBILITY_PUBLIC, Notification.VISIBILITY_PRIVATE,
 	 * 			Notification.VISIBILITY_SECRETのいずれか
	 */
	public int getLockscreenVisibility() {
		return lockscreenVisibility;
	}
	
	/**
	 * マナーモードの上書き(オーバーロード)するかどうかをセット
	 * @param bypassDnd
	 * @return
	 */
	public ChannelBuilder setBypassDnd(final boolean bypassDnd) {
		this.bypassDnd = bypassDnd;
		return this;
	}
	
	/**
	 * マナーモードの上書き(オーバーロード)するかどうかを取得
 	 * @return
	 */
	public boolean canBypassDnd() {
		return bypassDnd;
	}
	
	/**
	 * チャネル詳細情報をセット
	 * @param description
	 * @return
	 */
	public ChannelBuilder setDescription(final String description) {
		this.description = description;
		return this;
	}
	
	/**
	 * チャネル詳細情報を取得
 	 * @return
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * 通知でLED表示が有効な場合のLED色をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param argb
	 * @return
	 */
	public ChannelBuilder setLightColor(final int argb) {
		this.argb = argb;
		return this;
	}
	
	/**
	 * 通知でLED表示が有効な場合のLED色を取得
 	 * @return
	 */
	public int getLightColor() {
		return argb;
	}
	
	/**
	 * 通知でのLED表示の有効無効をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param lights
	 * @return
	 */
	public ChannelBuilder enableLights(final boolean lights) {
		this.lights = lights;
		return this;
	}
	
	/**
	 * 通知でのLED表示の有効無効をセット
 	 * @return
	 */
	public boolean shouldShowLights() {
		return lights;
	}
	
	/**
	 * ランチャーでバッジ表示をするかどうかをセット
	 * @param showBadge
	 * @return
	 */
	public ChannelBuilder setShowBadge(final boolean showBadge) {
		this.showBadge = showBadge;
		return this;
	}
	
	/**
	 * ランチャーでバッジ表示をするかどうかを取得
 	 * @return
	 */
	public boolean canShowBadge() {
		return showBadge;
	}
	
	/**
	 * バイブレーションのパターンをセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
 	 * @param vibrationPattern
	 * @return
	 */
	public ChannelBuilder setVibrationPattern(final long[] vibrationPattern) {
		vibration = vibrationPattern != null && vibrationPattern.length > 0;
		this.vibrationPattern = vibrationPattern;
		return this;
	}
	
	/**
	 * バイブレーションのパターンを取得
 	 * @return
	 */
	public long[] getVibrationPattern() {
		return vibrationPattern;
	}
	
	/**
	 * バイブレーションを有効にするかどうかをセット
	 * #setVibrationPatternでバイブレーションパターンをセットすると自動的に有効になる
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param vibration
	 * @return
	 */
	public ChannelBuilder enableVibration(final boolean vibration) {
		this.vibration = vibration;
		return this;
	}
	
	/**
	 * バイブレーションが有効かどうかを取得
	 * @return
	 */
	public boolean shouldVibrate() {
		return vibration && (vibrationPattern != null) && (vibrationPattern.length > 0);
	}
	
	/**
	 * 通知音をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param sound
	 * @param audioAttributes
	 * @return
	 */
	public ChannelBuilder setSound(final Uri sound, final AudioAttributes audioAttributes) {
		this.sound = sound;
		this.audioAttributes = audioAttributes;
		return this;
	}
	
	/**
	 * 通知音設定を取得
	 * @return
	 */
	public Uri getSound() {
		return sound;
	}
	
	/**
	 * 通知音設定を取得
 	 * @return
	 */
	public AudioAttributes getAudioAttributes() {
		return audioAttributes;
	}
	
	/**
	 * UI上のグループ分け設置をセット
	 * このグループ分けはNotificationのグループ分けとは関係なく通知設定のUI上でのグループ分けのみ
	 * @param groupId
	 * @param groupName
	 * @return
	 */
	public ChannelBuilder setGroup(
		@Nullable final String groupId, @Nullable final String groupName) {

		this.groupId = groupId;
		this.groupName = groupName;
		return this;
	}
	
	
	/**
	 * UI上のグループ分けidを取得
	 * このグループ分けはNotificationのグループ分けとは関係なく通知設定のUI上でのグループ分けのみ
	 * @return
	 */
	@Nullable
	public String getGroup() {
		return groupId;
	}
	
	/**
	 * グループの表示名を取得
	 * @return
	 */
	@Nullable
	public String getGroupName() {
		return groupName;
	}
	
	/**
	 * 既に同じのチャネルidのNotificationChannelが登録されている場合に
	 * 上書きして再設定するかどうかをセット。デフォルトでは上書きする。
	 * falseの場合でもチャネル名とグループは常時上書きする
	 * @param createIfExists
	 * @return
	 */
	public ChannelBuilder setCreateIfExists(final boolean createIfExists) {
		this.createIfExists = createIfExists;
		return this;
	}
	
	/**
	 * 既に同じのチャネルidのNotificationChannelが登録されている場合に
	 * 上書きして再設定するかどうかを取得。デフォルトではtrueで上書きする。
	 * falseの場合でもチャネル名とグループは常時上書きする
	 * @return
	 */
	public boolean isCreateIfExists() {
		return createIfExists;
	}
	
	/**
	 * Android 8以降用にNotificationChannelを生成する処理
	 * createNotificationChannel
	 * 		-> (createNotificationChannelGroup)
	 * 		-> (setupNotificationChannel)
	 * @param context
	 */
	@SuppressLint("WrongConstant")
	@RequiresApi(Build.VERSION_CODES.O)
	@Nullable
	protected NotificationChannel createNotificationChannel(@NonNull final Context context) {

		final NotificationManager manager
			= ContextUtils.requireSystemService(context, NotificationManager.class);
		NotificationChannel channel = manager.getNotificationChannel(channelId);
		if (createIfExists || (channel == null)) {
			if (importance == NotificationManagerCompat.IMPORTANCE_NONE) {
				importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
			}
			if (channel == null) {
				channel = new NotificationChannel(channelId, name, importance);
			}
			// ユーザーが変更可能な項目は新規作成時またはcreateIfExists=trueのときのみ変更
			channel.setImportance(importance);
			channel.setLockscreenVisibility(lockscreenVisibility);
			channel.setBypassDnd(bypassDnd);
			channel.setShowBadge(showBadge);
			channel.setLightColor(argb);
			channel.enableLights(lights);
			channel.setVibrationPattern(vibrationPattern);
			channel.enableVibration(vibration);
			channel.setSound(sound, audioAttributes);
		}
		if (channel != null) {
			// 表示文字列はユーザーが変更できないのと国際化対応のために常に上書きする
			if (!TextUtils.isEmpty(groupId)) {
				createNotificationChannelGroup(context, groupId, groupName);
			}
			channel.setName(name);
			channel.setDescription(description);
			channel.setGroup(groupId);
			manager.createNotificationChannel(setupNotificationChannel(channel));
		}
		return channel;
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
	protected NotificationChannel setupNotificationChannel(
		@NonNull final NotificationChannel channel) {
		
		return channel;
	}
	
	/**
	 * Android 8以降用にNotificationChannelGroupを生成する処理
	 * NotificationManager#getNotificationChannelGroupsに同じグループidの
	 * ものが存在しない時のみ新規に作成する
	 * createNotificationChannel
	 * 		-> (createNotificationChannelGroup)
	 * 		-> setupNotificationChannel
	 * @param groupId
	 * @param groupName
	 * @return
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	protected void createNotificationChannelGroup(
		@NonNull final Context context,
		@Nullable final String groupId, @Nullable final String groupName) {
		
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
	 * @param group
	 * @return
	 */
	@NonNull
	protected NotificationChannelGroup setupNotificationChannelGroup(
		@NonNull final NotificationChannelGroup group) {
		
		return group;
	}
}
