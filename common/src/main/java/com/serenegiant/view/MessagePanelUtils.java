package com.serenegiant.view;
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.system.ContextHolder;
import com.serenegiant.view.animation.ResizeAnimation;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.IntentCompat;

/**
 * スナックバー類似のメッセージ表示処理のヘルパークラス
 */
public abstract class MessagePanelUtils extends ContextHolder<Context> {
	private static final boolean DEBUG = false;	// XXX set false on production
	private static final String TAG = MessagePanelUtils.class.getSimpleName();

	public static final long MESSAGE_DURATION_INFINITY = 0;
	public static final long MESSAGE_DURATION_FLASH = 1500;
	public static final long MESSAGE_DURATION_SHORT = 2500;
	public static final long MESSAGE_DURATION_LONG = 5000;

	/**
	 * クリックした時用のIntentをstartActivityするかどうか
	 * true: startActivityする, false: ローカルブロードキャストする
	 */
	public static final String APP_EXTRA_KEY_CLICK_START_ACTIVITY = "APP_EXTRA_KEY_CLICK_START_ACTIVITY";
	// 表示中のメッセージパネルをクリックしたときのアクション
	public static final String APP_ACTION_CLICK_MESSAGE = "APP_ACTION_CLICK_MESSAGE";

	private static final String APP_ACTION_SHOW_MESSAGE = "APP_ACTION_SHOW_MESSAGE";
	private static final String APP_EXTRA_KEY_MESSAGE_AUTO_HIDE = "APP_EXTRA_KEY_MESSAGE_AUTO_HIDE";
	private static final String APP_EXTRA_KEY_EXCEPTION = "APP_EXTRA_KEY_EXCEPTION";
	private static final String APP_EXTRA_KEY_MESSAGE_ID = "APP_EXTRA_KEY_MESSAGE_ID";
	private static final String APP_EXTRA_KEY_MESSAGE = "APP_EXTRA_KEY_MESSAGE";
	private static final String APP_EXTRA_KEY_CLICK_INTENT = "APP_EXTRA_KEY_CLICK_INTENT";
	private static final String APP_EXTRA_KEY_COLOR = "APP_EXTRA_KEY_COLOR";

	// メッセージパネル非表示要求
	private static final String APP_ACTION_HIDE_MESSAGE = "APP_ACTION_HIDE_MESSAGE";
	private static final String APP_EXTRA_KEY_MESSAGE_HIDE_DELAY_MS = "APP_EXTRA_KEY_MESSAGE_HIDE_DELAY_MS";

	/**
	 * APP_ACTION_SHOW_MESSAGEインテントをローカルブロードキャストするためのヘルパークラス
	 */
	public static class MessageSender extends MessagePanelUtils {

		public MessageSender(@NonNull final Context context) {
			super(context);
		}

		@Override
		protected void internalRelease() {
			// do nothing now
		}

		public void showMessage(@NonNull final Intent intent) {
			sendLocalBroadcast(intent);
		}

		/**
		 * show message on message panel on main activity
		 * @param msgId
		 * @param autoHideDurationMs
		 */
		public void showMessage(@StringRes final int msgId,
			final long autoHideDurationMs) {
			showMessage(msgId, autoHideDurationMs, null);
		}

		/**
		 * show message on message panel on main activity
		 * @param msgId
		 * @param autoHideDurationMs
		 * @param clickIntent
		 */
		public void showMessage(@StringRes final int msgId,
			final long autoHideDurationMs,
			@Nullable final Intent clickIntent) {

			final Intent intent = new Intent(APP_ACTION_SHOW_MESSAGE)
				.putExtra(APP_EXTRA_KEY_MESSAGE_AUTO_HIDE, autoHideDurationMs)
				.putExtra(APP_EXTRA_KEY_MESSAGE_ID, msgId)
				.putExtra(APP_EXTRA_KEY_CLICK_INTENT, clickIntent);
			sendLocalBroadcast(intent);
		}

		/**
		 * show message on message panel on main activity
	 	 * @param msg
		 * @param autoHideDurationMs
		 */
		public void showMessage(final String msg,
			final long autoHideDurationMs) {
			showMessage(msg, autoHideDurationMs, null);
		}

		/**
		 * show message on message panel on main activity
	 	 * @param msg
		 * @param autoHideDurationMs
		 * @param clickIntent
		 */
		public void showMessage(final String msg,
			final long autoHideDurationMs,
			@Nullable final Intent clickIntent) {

			final Intent intent = new Intent(APP_ACTION_SHOW_MESSAGE)
				.putExtra(APP_EXTRA_KEY_MESSAGE_AUTO_HIDE, autoHideDurationMs)
				.putExtra(APP_EXTRA_KEY_MESSAGE, msg)
				.putExtra(APP_EXTRA_KEY_CLICK_INTENT, clickIntent);
			sendLocalBroadcast(intent);
		}

		/**
		 * show message on message panel on main activity
		 * @param e
		 * @param autoHideDurationMs
		 */
		public void showMessage(final Exception e,
			final long autoHideDurationMs) {
			showMessage(e, autoHideDurationMs, null);
		}

		/**
		 * show message on message panel on main activity
		 * @param e
		 * @param autoHideDurationMs
		 * @param clickIntent
		 */
		public void showMessage(final Exception e,
			final long autoHideDurationMs,
			@Nullable final Intent clickIntent) {

			final Intent intent = new Intent(APP_ACTION_SHOW_MESSAGE)
				.putExtra(APP_EXTRA_KEY_MESSAGE_AUTO_HIDE, autoHideDurationMs)
				.putExtra(APP_EXTRA_KEY_EXCEPTION, e)
				.putExtra(APP_EXTRA_KEY_CLICK_INTENT, clickIntent);
			sendLocalBroadcast(intent);
		}

		/**
		 * hide message on message panel on main activity
		 * @param delayMs
		 */
		public void hideMessage(final long delayMs) {
			final Intent intent = new Intent(APP_ACTION_HIDE_MESSAGE)
				.putExtra(APP_EXTRA_KEY_MESSAGE_HIDE_DELAY_MS, delayMs);
			sendLocalBroadcast(intent);
		}
	}

//================================================================================
	/**
	 * ローカルブロードキャストでAPP_ACTION_SHOW_MESSAGE受信時に指定したViewの操作を行うヘルパークラス
	 */
	public static class MessageReceiver extends MessagePanelUtils {

		private static final int DURATION_RESIZE_MS = 300;

		@NonNull private final View mParentView;
		@NonNull final View mPanelView;
		@NonNull final TextView mMessageTv;
		private final int mPanelWidthPx;
		private final int mPanelHeightPx;

		/**
		 * コンストラクタ
		 * コンストラクタ呼び出し時のpanelViewを記録して使うのでVisibilityとしてView.GONE以外を設定しておくこと
		 * メッセージ表示時の高さとしてR.dimen.bottom_message_panel_height(84dp)とpanelViewのheightの大きい方を使う
		 * @param context
		 * @param panelView
		 * @param messageTv
		 */
		public MessageReceiver(
			@NonNull final Context context,
			@NonNull final View panelView,
			@NonNull final TextView messageTv) {
			this(context, panelView, messageTv, 0);
		}
		/**
		 * コンストラクタ
		 * コンストラクタ呼び出し時のpanelViewを記録して使うのでVisibilityとしてView.GONE以外を設定しておくこと
		 * @param context
		 * @param panelView
		 * @param messageTv
		 * @param panelHeightPx 0以下ならR.dimen.bottom_message_panel_height(84dp)とpanelViewのheightの大きい方を使う
		 */
		public MessageReceiver(@NonNull final Context context,
			@NonNull final View panelView,
			@NonNull final TextView messageTv,
			final int panelHeightPx) {

			super(context);
			final ViewParent parent = panelView.getParent();
			if (parent instanceof View) {
				mParentView = (View)parent;
			} else {
				Log.w(TAG, "parent is not a instance of View," + parent);
				mParentView = panelView;
			}
			mPanelWidthPx = panelView.getWidth();
			mPanelHeightPx = (panelHeightPx <= 0)
				? Math.max(panelView.getHeight(), getResources().getDimensionPixelSize(R.dimen.bottom_message_panel_height))
				: panelHeightPx;
			mPanelView = panelView;
			mPanelView.setVisibility(View.GONE);
			mPanelView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					final Object obj = v.getTag(R.id.intent);
					if (DEBUG) Log.v(TAG, "onClick:" + obj);
					if (obj instanceof Intent) {
						final Intent clickIntent = (Intent)obj;
						final boolean startActivity = clickIntent.getBooleanExtra(
							APP_EXTRA_KEY_CLICK_START_ACTIVITY, false);
						if (startActivity) {
							v.getContext().startActivity(clickIntent);
						} else {
							sendLocalBroadcast(clickIntent);
						}
					} else {
						hideMessage(DURATION_RESIZE_MS);
					}
				}
			});
			mMessageTv = messageTv;
			final IntentFilter filter = new IntentFilter(APP_ACTION_SHOW_MESSAGE);
			filter.addAction(APP_ACTION_HIDE_MESSAGE);
			requireLocalBroadcastManager().registerReceiver(mReceiver, filter);
		}

		@Override
		protected void internalRelease() {
			try {
				requireLocalBroadcastManager().unregisterReceiver(mReceiver);
			} catch (final Exception e) {
				// ignore
			}
		}

		/**
		 * show message panel, autoHideDurationMs == infinity
		 * @param intent
		 */
		private void showMessage(@NonNull final Context context, @NonNull final Intent intent) {
			if (DEBUG) Log.v(TAG, "showMessage:" + intent);
			final long autoHideDurationMs = intent.getLongExtra(APP_EXTRA_KEY_MESSAGE_AUTO_HIDE, MESSAGE_DURATION_INFINITY);
			final int cl = intent.getIntExtra(APP_EXTRA_KEY_COLOR, mMessageTv.getCurrentTextColor());
			final int msgId = intent.getIntExtra(APP_EXTRA_KEY_MESSAGE_ID, 0);
			final String msg = intent.getStringExtra(APP_EXTRA_KEY_MESSAGE);
			final Intent clickIntent = IntentCompat.getParcelableExtra(intent, APP_EXTRA_KEY_CLICK_INTENT, Intent.class);
			if ((msgId != 0) && !TextUtils.isEmpty(msg)) {
				// フォーマット用文字列リソース
				showMessage(cl, getString(msgId, msg), autoHideDurationMs, clickIntent);
			} else if (msgId != 0) {
				// フォーマットなしの文字列リソース
				showMessage(cl, getString(msgId), autoHideDurationMs, clickIntent);
			} else if (!TextUtils.isEmpty(msg)) {
				// 文字列の即値指定
				showMessage(cl, msg, autoHideDurationMs, clickIntent);
			} else {
				// 例外オブジェクトからのメッセージ表示
				try {
					final Exception e
						= (Exception)intent.getSerializableExtra(APP_EXTRA_KEY_EXCEPTION);
					if (e != null) {
						final String error = e.getMessage();
						if (!TextUtils.isEmpty(error)) {
							showMessage(cl, error, autoHideDurationMs, clickIntent);
						}
					}
				} catch (final Exception e1) {
					Log.w(TAG, e1);
				}
			}
		}

		/**
		 * show message panel
		 * @param message
		 * @param autoHideDurationMs if greater than zero,
		 * 			automatically hide message panel after specific duration
		 * @param clickIntent メッセージパネルをクリックした時にローカルブルードキャストするIntent、
		 * 			nullならローカルブロードキャストする代わりにメッセージパネルを非表示(#hideMessage)する
		 */
		private void showMessage(final int cl, final String message, final long autoHideDurationMs, @Nullable final Intent clickIntent) {
			if (DEBUG) Log.v(TAG, "showMessage:" + message);
			mPanelView.post(new Runnable() {
				@Override
				public void run() {
					if (DEBUG) Log.i(TAG, String.format(Locale.US, "showMessage:size(%d,%d)(%d,%d)",
						mPanelWidthPx, mPanelHeightPx, mPanelView.getWidth(), mPanelView.getHeight()));
					mMessageTv.setText(message);
					mMessageTv.setTextColor(cl);
					mPanelView.clearAnimation();
					final ResizeAnimation expandAnimation
						= new ResizeAnimation(mPanelView,
						mPanelWidthPx, 0,
						mPanelWidthPx, mPanelHeightPx);
					expandAnimation.setDuration(DURATION_RESIZE_MS);
					expandAnimation.setAnimationListener(mAnimationListener);
					mPanelView.setVisibility(View.VISIBLE);
					mPanelView.setTag(R.id.visibility, 1);
					mPanelView.setTag(R.id.auto_hide_duration, autoHideDurationMs);
					mPanelView.setTag(R.id.intent, clickIntent);
					mPanelView.startAnimation(expandAnimation);
				}
			});
		}

		/**
		 * hide message panel
		 * @param durationMs
		 */
		private void hideMessage(final long durationMs) {
			if (DEBUG) Log.v(TAG, "hideMessage:");
			mPanelView.post(new Runnable() {
				@Override
				public void run() {
					if (mPanelView.getVisibility() == View.VISIBLE) {
						if (DEBUG) Log.v(TAG,
							String.format(Locale.US, "hideMessage:size(%d,%d)",
								mPanelView.getWidth(), mPanelView.getHeight()));
						mPanelView.clearAnimation();
						final ResizeAnimation collapseAnimation
							= new ResizeAnimation(mPanelView,
							mPanelWidthPx, mPanelHeightPx,
							mPanelWidthPx, 0);
						collapseAnimation.setDuration(durationMs);
						collapseAnimation.setAnimationListener(mAnimationListener);
						mPanelView.setTag(R.id.visibility, 0);
						mPanelView.startAnimation(collapseAnimation);
					}
				}
			});
		}

		/**
		 * implementation of AnimationListener to show/hide message panel with animation
		 */
		private final Animation.AnimationListener mAnimationListener
			= new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(final Animation animation) {
				// ignore
			}

			@Override
			public void onAnimationEnd(final Animation animation) {
				final Object visibility = mPanelView.getTag(R.id.visibility);
				if ((visibility instanceof Integer) && ((Integer)visibility == 1)) {
					final Object durationObj = mPanelView.getTag(R.id.auto_hide_duration);
					final long duration = (durationObj instanceof Long) ? (Long)durationObj : 0;
					if (duration > 0) {
						mPanelView.postDelayed(new Runnable() {
							@Override
							public void run() {
								hideMessage(DURATION_RESIZE_MS);
							}
						}, duration);
					}
				} else {
					mPanelView.setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationRepeat(final Animation animation) {
				// ignore
			}
		};

		private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				final String action = intent != null ? intent.getAction() : null;
				if (!TextUtils.isEmpty(action)) {
					switch (action) {
					case APP_ACTION_SHOW_MESSAGE ->
						showMessage(context, intent);
					case APP_ACTION_HIDE_MESSAGE ->
						hideMessage(intent.getLongExtra(APP_EXTRA_KEY_MESSAGE_HIDE_DELAY_MS, 0));
					}
				}
			}
		};
	}

//================================================================================
	private MessagePanelUtils(@NonNull final Context context) {
		super(context);
	}
}
