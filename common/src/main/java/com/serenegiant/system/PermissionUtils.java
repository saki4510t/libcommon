package com.serenegiant.system;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

/**
 * パーミッション要求用のヘルパークラス
 */
public class PermissionUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = PermissionUtils.class.getSimpleName();

	/**
	 * ユーザーがパーミッション要求に対して「 次回から表示しない」を選択したかどうかを保持するための
	 * 共有プレファレンス名, キー名はパーミッション文字列
	 */
	private static final String PREF_NAME_NOT_ASK_AGAIN = TAG + "_NOT_ASK_AGAIN";

	/**
	 * パーミッション要求時のコールバックリスナー
	 */
	public interface PermissionCallback {
		/**
		 * パーミッション要求の説明表示が必要な場合
		 * @param permission
		 */
		public void onPermissionShowRational(@NonNull final String permission);
		/**
		 * パーミッション要求の説明表示が必要な場合(複数一括要求時)
		 * @param permissions
		 */
		public void onPermissionShowRational(@NonNull final String[] permissions);
		/**
		 * パーミッション要求をユーザーが拒否した時
		 * @param permission
		 */
		public void onPermissionDenied(@NonNull final String permission);
		/**
		 * パーミッション要求をユーザーが許可した場合
		 * @param permission
		 */
		public void onPermission(@NonNull final String permission);
		/**
		 * ユーザーがパーミッション要求を拒否した際に次回から表示しないを選択した後再度パーミッション要求を行った場合
		 * @param permission
		 */
		public void onPermissionNeverAskAgain(@NonNull final String permission);
		/**
		 * ユーザーがパーミッション要求を拒否した際に次回から表示しないを選択した後再度パーミッション要求を行った場合(複数一括要求時)
		 * @param permissions
		 */
		public void onPermissionNeverAskAgain(@NonNull final String[] permissions);
	}

	/**
	 * 複数のパーミッションを同時に要求しないときのPermissionCallback実装
	 */
	public static abstract class SinglePermissionCallback implements PermissionCallback {
		public void onPermissionShowRational(@NonNull final String[] permissions) {
		}
		public void onPermissionNeverAskAgain(@NonNull final String[] permissions) {
		}
	}

	@NonNull
	private final WeakReference<ComponentActivity> mWeakActivity;
	@NonNull
	private final Map<String, ActivityResultLauncher<String>> mLaunchers = new HashMap<>();
	@NonNull
	private final Map<String[], ActivityResultLauncher<String[]>> mMultiLaunchers = new HashMap<>();
	@NonNull
	private final PermissionCallback mCallback;

	/**
	 * コンストラクタ
	 * ActivityのonCreateから呼ばないといけない
	 * AndroidManifest.xmlで要求しているパーミッションはすべて個別にprepareした状態になる
	 * @param activity
	 * @param callback
	 */
	public PermissionUtils(
		@NonNull final ComponentActivity activity,
		@NonNull final PermissionCallback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:" + activity);
		mWeakActivity = new WeakReference<>(activity);
		mCallback = callback;
		mLaunchers.putAll(prepare(activity, callback));
	}

	/**
	 * コンストラクタ
	 * FragmentのonAttachまたはonCreateから呼ばないといけない
	 * AndroidManifest.xmlで要求しているパーミッションはすべて個別にprepareした状態になる
	 * @param fragment
	 * @param callback
	 */
	public PermissionUtils(
		@NonNull final Fragment fragment,
		@NonNull final PermissionCallback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:" + fragment);
		final ComponentActivity activity = fragment.requireActivity();
		mWeakActivity = new WeakReference<>(activity);
		mCallback = callback;
		mLaunchers.putAll(prepare(fragment, callback));
	}

	/**
	 * 複数同時にパーミッションを要求するための準備をする
	 * @param activity
	 * @param permissions
	 * @return
	 */
	public PermissionUtils prepare(@NonNull final ComponentActivity activity,
		@NonNull final String[] permissions) {
		if(DEBUG) Log.v(TAG, "prepare:" + Arrays.toString(permissions));
		mMultiLaunchers.put(permissions, prepare(activity, permissions, mCallback));
		return this;
	}

	/**
	 * 複数同時にパーミッションを要求するための準備をする
	 * @param fragment
	 * @param permissions
	 * @return
	 */
	public PermissionUtils prepare(@NonNull final Fragment fragment,
		@NonNull final String[] permissions) {
		if(DEBUG) Log.v(TAG, "prepare:" + Arrays.toString(permissions));
		mMultiLaunchers.put(permissions, prepare(fragment, permissions, mCallback));
		return this;
	}

	/**
	 * パーミッションを要求する
	 * 結果はコールバックで受け取る。
	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
	 * @param permission
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @throws IllegalArgumentException
	 */
	public boolean requestPermission(
		@NonNull final String permission,
		final boolean canShowRational) throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
		final ComponentActivity activity = mWeakActivity.get();
		if ((activity == null) || activity.isFinishing()) {
			return false;
		}
		final boolean hasPermission = PermissionCheck.hasPermission(activity, permission);
		if (hasPermission) {
			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + permission);
			// すでにパーミッションを保持している場合...コールバックを呼ばない
			setNeverAskAgain(activity, permission, false);
			return true;
		} else if (canShowRational && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + permission);
			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
			// パーミッションが必要な理由の説明が必要な時
			mCallback.onPermissionShowRational(permission);
		} else {
			// ここにくるのは
			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
			// ・一度拒否されて「次回から表示しない」が選択された場合
			if (!isNeverAskAgain(activity, permission)) {
				// 直接パーミッションを要求する
				@Nullable
				final ActivityResultLauncher<String> launcher
					= mLaunchers.containsKey(permission) ? mLaunchers.get(permission) : null;
				if (launcher != null) {
					launcher.launch(permission);
				} else {
					// ここに来るのはプログラミングのミスのはず
					// 対象のパーミッション文字列に対してprepareが呼ばれていない
					throw new IllegalArgumentException("has no ActivityResultLauncher for " + permission);
				}
			} else {
				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + permission);
				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
				mCallback.onPermissionNeverAskAgain(permission);
			}
		}
		return hasPermission;
	}

	/**
	 * パーミッションを要求する
	 * 結果はコールバックで受け取る。
	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
	 * @param permissions
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @throws IllegalArgumentException
	 */
	public boolean requestPermission(
		@NonNull final String[] permissions,
		final boolean canShowRational) throws IllegalArgumentException {

		if (permissions.length == 1) {
			// 1つだけのときは単体のパーミッション要求メソッドへ移譲する
			return requestPermission(permissions[0], canShowRational);
		}
		if (DEBUG) Log.v(TAG, "requestPermission:" + Arrays.toString(permissions));
		final ComponentActivity activity = mWeakActivity.get();
		if ((activity == null) || activity.isFinishing()) {
			return false;
		}
		final boolean hasPermission = PermissionCheck.hasPermissionAll(activity, permissions);
		if (hasPermission) {
			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + Arrays.toString(permissions));
			// すでにパーミッションを保持している場合...コールバックを呼ばない
			for (final String permission: permissions) {
				setNeverAskAgain(activity, permission, false);
			}
			return true;
		} else if (canShowRational && shouldShowRequestPermissionRationale(activity, permissions)) {
			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + Arrays.toString(permissions));
			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
			// パーミッションが必要な理由の説明が必要な時
			mCallback.onPermissionShowRational(permissions);
		} else {
			// ここにくるのは
			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
			// ・一度拒否されて「次回から表示しない」が選択された場合
			if (!isNeverAskAgain(activity, permissions)) {
				// 直接パーミッションを要求する
				@Nullable
				final ActivityResultLauncher<String[]> launcher
					= mMultiLaunchers.containsKey(permissions) ? mMultiLaunchers.get(permissions) : null;
				if (launcher != null) {
					launcher.launch(permissions);
				} else {
					// ここに来るのはプログラミングのミスのはず
					// 対象のパーミッション文字列に対してprepareが呼ばれていない
					throw new IllegalArgumentException("has no ActivityResultLauncher for " + Arrays.toString(permissions));
				}
			} else {
				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + Arrays.toString(permissions));
				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
				mCallback.onPermissionNeverAskAgain(permissions);
			}
		}
		return hasPermission;
	}

//--------------------------------------------------------------------------------
	/**
	 * アプリのAndroidManifest.xmlで要求しているパーミッションの全てに対してActivityResultLauncherを生成して
	 * パーミッション文字列をキーとするMapとして返す
	 * @param activity
	 * @param callback
	 * @return
	 */
	public static Map<String, ActivityResultLauncher<String>> prepare(
		@NonNull final ComponentActivity activity,
		@NonNull final PermissionCallback callback) {

		final Map<String, ActivityResultLauncher<String>> result = new HashMap<>();
		final Context context = activity;
		final String[] requestedPermissions = PermissionCheck.requestedPermissions(context);
		for (final String permission: requestedPermissions) {
			final ActivityResultLauncher<String> requestPermissionLauncher
				= activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
					isGranted -> {
						if (isGranted) {
							if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
							// ユーザーがパーミッションを許可した場合
							setNeverAskAgain(context, permission, false);
							callback.onPermission(permission);
						} else {
							if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
							// ユーザーがパーミッションを許可しなかった場合
							if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
								// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
								setNeverAskAgain(context, permission, true);
							}
							callback.onPermissionDenied(permission);
						}
					});
			result.put(permission, requestPermissionLauncher);
		}
		return result;
	}

	/**
	 * アプリのAndroidManifest.xmlで要求しているパーミッションの全てに対してActivityResultLauncherを生成して
	 * パーミッション文字列をキーとするMapとして返す
	 * @param fragment
	 * @param callback
	 * @return
	 */
	public static Map<String, ActivityResultLauncher<String>> prepare(
		@NonNull final Fragment fragment,
		@NonNull final PermissionCallback callback) {

		final Map<String, ActivityResultLauncher<String>> result = new HashMap<>();
		final ComponentActivity activity = fragment.requireActivity();
		final String[] requestedPermissions = PermissionCheck.requestedPermissions(activity);
		for (final String permission: requestedPermissions) {
			final ActivityResultLauncher<String> requestPermissionLauncher
				= fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
					isGranted -> {
						if (isGranted) {
							if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
							// ユーザーがパーミッションを許可した場合
							setNeverAskAgain(activity, permission, false);
							callback.onPermission(permission);
						} else {
							if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
							// ユーザーがパーミッションを許可しなかった場合
							if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
								// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
								setNeverAskAgain(activity, permission, true);
							}
							callback.onPermissionDenied(permission);
						}
					});
			result.put(permission, requestPermissionLauncher);
		}
		return result;
	}

	/**
	 * 指定したパーミッション文字列配列を一度にパーミッション要求するためのActivityResultLauncherインスタンスを生成する
	 * @param activity
	 * @param permissions
	 * @param callback
	 * @return
	 */
	public static ActivityResultLauncher<String[]> prepare(
		@NonNull final ComponentActivity activity,
		@NonNull final String[] permissions,
		@NonNull final PermissionCallback callback) {

		return activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
			result -> {
				for (final Map.Entry<String, Boolean> entry: result.entrySet()) {
					final String permission = entry.getKey();
					if (entry.getValue()) {
						if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
						// ユーザーがパーミッションを許可した場合
						setNeverAskAgain(activity, permission, false);
						callback.onPermission(permission);
					} else {
						if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
						// ユーザーがパーミッションを許可しなかった場合
						if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
							// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
							setNeverAskAgain(activity, permission, true);
						}
						callback.onPermissionDenied(permission);
					}
				}
			});
	}

	/**
	 * 指定したパーミッション文字列配列を一度にパーミッション要求するためのActivityResultLauncherインスタンスを生成する
	 * @param fragment
	 * @param permissions
	 * @param callback
	 * @return
	 */
	public static ActivityResultLauncher<String[]> prepare(
		@NonNull final Fragment fragment,
		@NonNull final String[] permissions,
		@NonNull final PermissionCallback callback) {

		final ComponentActivity activity = fragment.requireActivity();
		return fragment.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
			result -> {
				for (final Map.Entry<String, Boolean> entry: result.entrySet()) {
					final String permission = entry.getKey();
					if (entry.getValue()) {
						if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
						// ユーザーがパーミッションを許可した場合
						setNeverAskAgain(activity, permission, false);
						callback.onPermission(permission);
					} else {
						if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
						// ユーザーがパーミッションを許可しなかった場合
						if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
							// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
							setNeverAskAgain(activity, permission, true);
						}
						callback.onPermissionDenied(permission);
					}
				}
			});
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したパーミッション文字列配列で1つでもActivityCompat.shouldShowRequestPermissionRationaleがtrueになればtrueを返す
	 * @param activity
	 * @param permissions
	 * @return
	 */
	public static boolean shouldShowRequestPermissionRationale(
		@NonNull final Activity activity, @NonNull final String[] permissions) {

		for (final String permission: permissions) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定したパーミッション文字列配列で１つでもneverAskAgainフラグがセットされていればtrueを返す
	 * @param context
	 * @param permissions
	 * @return
	 */
	public static boolean isNeverAskAgain(
		@NonNull final Context context, @NonNull final String[] permissions) {
		final SharedPreferences preferences
			= context.getSharedPreferences(PREF_NAME_NOT_ASK_AGAIN, 0);
		for (final String permission: permissions) {
			if (preferences.getBoolean(permission, false)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定したパーミッション文字列配列で１つでもneverAskAgainフラグがセットされていればtrueを返す
	 * @param context
	 * @param permission
	 * @return
	 */
	public static boolean isNeverAskAgain(
		@NonNull final Context context, @NonNull final String permission) {
		final SharedPreferences preferences
			= context.getSharedPreferences(PREF_NAME_NOT_ASK_AGAIN, 0);
		return preferences.getBoolean(permission, false);
	}

	/**
	 * 指定したパーミッション文字列対応するneverAskAgainフラグをセット/クリアする
	 * @param context
	 * @param permission
	 * @param neverAskAgain
	 */
	private static void setNeverAskAgain(
		@NonNull final Context context,
		@NonNull final String permission,
		final boolean neverAskAgain) {
		final SharedPreferences.Editor editor
			= context.getSharedPreferences(PREF_NAME_NOT_ASK_AGAIN, 0).edit();
		if (neverAskAgain) {
			editor.putBoolean(permission, true).apply();
		} else {
			editor.remove(permission).apply();
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * パーミッションを要求する
	 * 結果はコールバックで受け取る。
	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
	 * XXX このメソッドはFragmentの#onAttachまたは#onCreateで呼び出さないといけない(でないとクラッシュする)
	 * @param fragment
	 * @param permission
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @param callback
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @deprecated 動作確認のためだけに作ったので通常は使わないこと
	 */
	@Deprecated
	public static boolean requestPermission(
		@NonNull final Fragment fragment,
		@NonNull final String permission,
		final boolean canShowRational,
		@NonNull final PermissionCallback callback) {

		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
		final Context context = fragment.requireContext();
		final boolean hasPermission = PermissionCheck.hasPermission(context, permission);
		if (hasPermission) {
			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + permission);
			// すでにパーミッションを保持している場合...コールバックを呼ばない
			setNeverAskAgain(context, permission, false);
			return true;
		} else if (canShowRational && fragment.shouldShowRequestPermissionRationale(permission)) {
			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + permission);
			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
			// パーミッションが必要な理由の説明が必要な時
			callback.onPermissionShowRational(permission);
		} else {
			// ここにくるのは
			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
			// ・一度拒否されて「次回から表示しない」が選択された場合
			if (!isNeverAskAgain(context, permission)) {
				// 直接パーミッションを要求する
				// Fragment#registerForActivityResultはonAttacheまたはonCreateで呼び出さないといけない
				final ActivityResultLauncher<String> launcher
					= fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
						isGranted -> {
							if (isGranted) {
								if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
								// ユーザーがパーミッションを許可した場合
								setNeverAskAgain(context, permission, false);
								callback.onPermission(permission);
							} else {
								if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
								// ユーザーがパーミッションを許可しなかった場合
								if (!fragment.shouldShowRequestPermissionRationale(permission)) {
									// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
									setNeverAskAgain(context, permission, true);
								}
								callback.onPermissionDenied(permission);
							}
						});
				launcher.launch(permission);
			} else {
				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + permission);
				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
				callback.onPermissionNeverAskAgain(permission);
			}
		}
		return hasPermission;
	}

	/**
	 * パーミッションを要求する
	 * 結果はコールバックで受け取る。
	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
	 * XXX このメソッドはActivityのonCreateで呼び出さないといけない(でないとクラッシュする)
	 * @param activity
	 * @param permission
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @param callback
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @deprecated 動作確認のためだけに作ったので通常は使わないこと
	 */
	@Deprecated
	public static boolean requestPermission(
		@NonNull final AppCompatActivity activity,
		@NonNull final String permission,
		final boolean canShowRational,
		@NonNull final PermissionCallback callback) {

		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
		final boolean hasPermission = PermissionCheck.hasPermission(activity, permission);
		if (hasPermission) {
			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + permission);
			// すでにパーミッションを保持している場合...コールバックを呼ばない
			setNeverAskAgain(activity, permission, false);
			return true;
		} else if (canShowRational && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + permission);
			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
			// パーミッションが必要な理由の説明が必要な時
			callback.onPermissionShowRational(permission);
		} else {
			// ここにくるのは
			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
			// ・一度拒否されて「次回から表示しない」が選択された場合
			if (!isNeverAskAgain(activity, permission)) {
				// 直接パーミッションを要求する
				final ActivityResultLauncher<String> launcher
					= activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
						isGranted -> {
							if (isGranted) {
								if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
								// ユーザーがパーミッションを許可した場合
								setNeverAskAgain(activity, permission, false);
								callback.onPermission(permission);
							} else {
								if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
								// ユーザーがパーミッションを許可しなかった場合
								if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
									// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが次回から表示市内を選択した時
									setNeverAskAgain(activity, permission, true);
								}
								callback.onPermissionDenied(permission);
							}
						});
				launcher.launch(permission);
			} else {
				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + permission);
				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
				callback.onPermissionNeverAskAgain(permission);
			}
		}
		return hasPermission;
	}
}
