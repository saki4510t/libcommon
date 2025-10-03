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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

	/**
	 * EXTERNAL_STORAGEからメディアファイル(AUDIO/IMAGE?VIDEO)を読み込むのに必要な権限配列
	 * API>=33ならREAD_MEDIA_AUDIO/READ_MEDIA_IMAGES/READ_MEDIA_VIDEO
	 * API>=29ならREAD_EXTERNAL_STORAGE
	 * API<=28>ならREAD_EXTERNAL_STORAGE/WRITE_EXTERNAL_STORAGE
	 */
	public static final String[] READ_MEDIA_PERMISSIONS;
	static {
		final ArrayList<String> result = new ArrayList<>();
		if (BuildCheck.isAPI33()) {
			result.add(Manifest.permission.READ_MEDIA_AUDIO);
			result.add(Manifest.permission.READ_MEDIA_IMAGES);
			result.add(Manifest.permission.READ_MEDIA_VIDEO);
		} else if (BuildCheck.isAPI29()) {
			result.add(Manifest.permission.READ_EXTERNAL_STORAGE);
		} else {
			result.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			result.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		READ_MEDIA_PERMISSIONS = result.toArray(new String[0]);
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
	 * AndroidManifest.xmlで要求しているパーミッションの配列
	 */
	@NonNull
	private final String[] mRequestedPermissions;
	/**
	 * パーミッション要求中に他のパーミッションを要求すると先に要求したパーミッションが拒絶されるので
	 * パーミッション要求中かどうかを保持するためのフラグ
	 */
	private boolean mIsRequesting;

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
		mRequestedPermissions = requestedPermissions(activity);
		mLaunchers.putAll(prepare(this, activity, callback));
		prepare(activity, mRequestedPermissions);
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
		mRequestedPermissions = requestedPermissions(activity);
		mLaunchers.putAll(prepare(this, fragment, callback));
		prepare(fragment, mRequestedPermissions);
	}

	/**
	 * パーミッション要求中かどうかを取得
	 * @return
	 */
	public boolean isRequesting() {
		return mIsRequesting;
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
		if (permissions.length > 0) {
			mMultiLaunchers.put(permissions, prepare(this, activity, permissions, mCallback));
		}
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
		if (permissions.length > 0) {
			mMultiLaunchers.put(permissions, prepare(this, fragment, permissions, mCallback));
		}
		return this;
	}

	/**
	 * AndroidManifest.xmlで要求しているパーミッションを全て要求する
	 * @param canShowRational
	 * @return
	 */
	public boolean requestPermissionAll(final boolean canShowRational) {
		return requestPermission(mRequestedPermissions, canShowRational);
	}

	/**
	 * パーミッションを要求する
	 * 結果はコールバックで受け取る。
	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
	 * 他のパーミッション要求中はfalseを返す
	 * @param permission
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @throws IllegalArgumentException
	 */
	public synchronized boolean requestPermission(
		@NonNull final String permission,
		final boolean canShowRational) throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
		final ComponentActivity activity = mWeakActivity.get();
		if ((activity == null) || activity.isFinishing() || mIsRequesting) {
			return false;
		}
		final boolean hasPermission = PermissionUtils.hasPermission(activity, permission);
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
					mIsRequesting = true;
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
	 * 他のパーミッション要求中はfalseを返す
	 * XXX ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATIONとACCESS_BACKGROUND_LOCATIONを同時に要求するとどちらも許可されないので注意
	 * @param permissions #prepareを呼び出したのと同じString配列(中身が同じでも他の配列はだめ)
	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
	 * @throws IllegalArgumentException
	 */
	public synchronized boolean requestPermission(
		@NonNull final String[] permissions,
		final boolean canShowRational) throws IllegalArgumentException {

		if (permissions.length == 1) {
			// 1つだけのときは単体のパーミッション要求メソッドへ移譲する
			return requestPermission(permissions[0], canShowRational);
		}
		if (DEBUG) Log.v(TAG, "requestPermission:" + Arrays.toString(permissions));
		final ComponentActivity activity = mWeakActivity.get();
		if ((activity == null) || activity.isFinishing() || mIsRequesting) {
			return false;
		}
		final boolean hasPermission = PermissionUtils.hasPermissionAll(activity, permissions);
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
					mIsRequesting = true;
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
	 * アプリのAndroidManifest.xmlで要求しているパーミッションの全てに対して1つずつActivityResultLauncherを生成して
	 * パーミッション文字列をキーとするMapとして返す
	 * @param activity
	 * @param callback
	 * @return
	 */
	@NonNull
	private static Map<String, ActivityResultLauncher<String>> prepare(
		@NonNull final PermissionUtils parent,
		@NonNull final ComponentActivity activity,
		@NonNull final PermissionCallback callback) {

		final Map<String, ActivityResultLauncher<String>> result = new HashMap<>();
		final Context context = activity;
		final String[] requestedPermissions = PermissionUtils.requestedPermissions(context);
		for (final String permission: requestedPermissions) {
			if (DEBUG) Log.v(TAG, "prepare:" + permission + "=" + PermissionUtils.hasPermission(activity, permission));
			final ActivityResultLauncher<String> requestPermissionLauncher
				= activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
					isGranted -> {
						try {
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
						} finally {
							parent.mIsRequesting = false;
						}
					});
			result.put(permission, requestPermissionLauncher);
		}
		return result;
	}

	/**
	 * アプリのAndroidManifest.xmlで要求しているパーミッションの全てに対して1つずつActivityResultLauncherを生成して
	 * パーミッション文字列をキーとするMapとして返す
	 * @param parent
	 * @param fragment
	 * @param callback
	 * @return
	 */
	@NonNull
	private static Map<String, ActivityResultLauncher<String>> prepare(
		@NonNull final PermissionUtils parent,
		@NonNull final Fragment fragment,
		@NonNull final PermissionCallback callback) {

		final Map<String, ActivityResultLauncher<String>> result = new HashMap<>();
		final ComponentActivity activity = fragment.requireActivity();
		final String[] requestedPermissions = PermissionUtils.requestedPermissions(activity);
		for (final String permission: requestedPermissions) {
			if (DEBUG) Log.v(TAG, "prepare:" + permission + "=" + PermissionUtils.hasPermission(activity, permission));
			final ActivityResultLauncher<String> requestPermissionLauncher
				= fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
					isGranted -> {
						try {
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
						} finally {
							parent.mIsRequesting = false;
						}
					});
			result.put(permission, requestPermissionLauncher);
		}
		return result;
	}

	/**
	 * 指定したパーミッション文字列配列を一度にパーミッション要求するためのActivityResultLauncherインスタンスを生成する
	 * XXX ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATIONとACCESS_BACKGROUND_LOCATIONを同時に要求するとどちらも許可されないので注意
	 * @param parent
	 * @param activity
	 * @param permissions
	 * @param callback
	 * @return
	 */
	@NonNull
	private static ActivityResultLauncher<String[]> prepare(
		@NonNull final PermissionUtils parent,
		@NonNull final ComponentActivity activity,
		@NonNull final String[] permissions,
		@NonNull final PermissionCallback callback) {

		return activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
			result -> {
				try {
					for (final Map.Entry<String, Boolean> entry: result.entrySet()) {
						final String permission = entry.getKey();
						if (entry.getValue()) {
							if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
							// ユーザーがパーミッションを許可した場合
							setNeverAskAgain(activity, permission, false);
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
				} finally {
					parent.mIsRequesting = false;
				}
			});
	}

	/**
	 * 指定したパーミッション文字列配列を一度にパーミッション要求するためのActivityResultLauncherインスタンスを生成する
	 * XXX ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATIONとACCESS_BACKGROUND_LOCATIONを同時に要求するとどちらも許可されないので注意
	 * @param parent
	 * @param fragment
	 * @param permissions
	 * @param callback
	 * @return
	 */
	@NonNull
	private static ActivityResultLauncher<String[]> prepare(
		@NonNull final PermissionUtils parent,
		@NonNull final Fragment fragment,
		@NonNull final String[] permissions,
		@NonNull final PermissionCallback callback) {

		final ComponentActivity activity = fragment.requireActivity();
		return fragment.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
			result -> {
				try {
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
				} finally {
					parent.mIsRequesting = false;
				}
			});
	}

//--------------------------------------------------------------------------------
	/**
	 * ActivityCompat.shouldShowRequestPermissionRationaleのラッパー
	 * @param activity
	 * @param permission
	 * @return
	 */
	public static boolean shouldShowRequestPermissionRationale(
		@NonNull final Activity activity, @NonNull final String permission) {
		return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
	}

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
// XXX staticメソッドとして実装した場合の参考としてコメントにして残す
//     Fragmentの#onAttachまたは#onCreateまたはActivity#onCreateで呼び出さないとクラッシュするので
//     使い勝手が悪いのとアプリ固有の追加処理を実装できない
//	/**
//	 * パーミッションを要求する
//	 * 結果はコールバックで受け取る。
//	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
//	 * XXX このメソッドはFragmentの#onAttachまたは#onCreateで呼び出さないといけない(でないとクラッシュする)
//	 * @param fragment
//	 * @param permission
//	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
//	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
//	 * @param callback
//	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
//	 * @deprecated 動作確認のためだけに作ったので通常は使わないこと
//	 */
//	@Deprecated
//	public static boolean requestPermission(
//		@NonNull final Fragment fragment,
//		@NonNull final String permission,
//		final boolean canShowRational,
//		@NonNull final PermissionCallback callback) {
//
//		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
//		final Context context = fragment.requireContext();
//		final boolean hasPermission = PermissionUtils.hasPermission(context, permission);
//		if (hasPermission) {
//			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + permission);
//			// すでにパーミッションを保持している場合...コールバックを呼ばない
//			setNeverAskAgain(context, permission, false);
//			return true;
//		} else if (canShowRational && fragment.shouldShowRequestPermissionRationale(permission)) {
//			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + permission);
//			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
//			// パーミッションが必要な理由の説明が必要な時
//			callback.onPermissionShowRational(permission);
//		} else {
//			// ここにくるのは
//			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
//			// ・一度拒否されて「次回から表示しない」が選択された場合
//			if (!isNeverAskAgain(context, permission)) {
//				// 直接パーミッションを要求する
//				// Fragment#registerForActivityResultはonAttacheまたはonCreateで呼び出さないといけない
//				final ActivityResultLauncher<String> launcher
//					= fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
//						isGranted -> {
//							if (isGranted) {
//								if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
//								// ユーザーがパーミッションを許可した場合
//								setNeverAskAgain(context, permission, false);
//								callback.onPermission(permission);
//							} else {
//								if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
//								// ユーザーがパーミッションを許可しなかった場合
//								if (!fragment.shouldShowRequestPermissionRationale(permission)) {
//									// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが今後表示しないを選択してパーミッション要求を拒否した時
//									setNeverAskAgain(context, permission, true);
//								}
//								callback.onPermissionDenied(permission);
//							}
//						});
//				launcher.launch(permission);
//			} else {
//				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + permission);
//				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
//				callback.onPermissionNeverAskAgain(permission);
//			}
//		}
//		return hasPermission;
//	}

//	/**
//	 * パーミッションを要求する
//	 * 結果はコールバックで受け取る。
//	 * ただし、このメソッド呼び出し時にすでにパーミッションを保持している場合は#onPermissionを呼ばない。
//	 * XXX このメソッドはActivityのonCreateで呼び出さないといけない(でないとクラッシュする)
//	 * @param activity
//	 * @param permission
//	 * @param canShowRational パーミッション要求の確認ダイアログ表示後に再度パーミッション要求した時に
//	 * 							再度shouldShowRequestPermissionRationaleがヒットしてループしてしまうのを防ぐため
//	 * @param callback
//	 * @return このメソッドを呼出した時点で指定したパーミッションを保持しているかどうか
//	 * @deprecated 動作確認のためだけに作ったので通常は使わないこと
//	 */
//	@Deprecated
//	public static boolean requestPermission(
//		@NonNull final AppCompatActivity activity,
//		@NonNull final String permission,
//		final boolean canShowRational,
//		@NonNull final PermissionCallback callback) {
//
//		if (DEBUG) Log.v(TAG, "requestPermission:" + permission);
//		final boolean hasPermission = PermissionUtils.hasPermission(activity, permission);
//		if (hasPermission) {
//			if (DEBUG) Log.v(TAG, "requestPermission:already has permission," + permission);
//			// すでにパーミッションを保持している場合...コールバックを呼ばない
//			setNeverAskAgain(activity, permission, false);
//			return true;
//		} else if (canShowRational && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
//			if (DEBUG) Log.v(TAG, "requestPermission:shouldShowRequestPermissionRationale," + permission);
//			// ここにくるのは1度拒否されて「今後表示しない」が選択されていない場合
//			// パーミッションが必要な理由の説明が必要な時
//			callback.onPermissionShowRational(permission);
//		} else {
//			// ここにくるのは
//			// ・一度も拒否されていない場合(まだパーミッションダイアログを出していない)
//			// ・一度拒否されて「次回から表示しない」が選択された場合
//			if (!isNeverAskAgain(activity, permission)) {
//				// 直接パーミッションを要求する
//				final ActivityResultLauncher<String> launcher
//					= activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
//						isGranted -> {
//							if (isGranted) {
//								if (DEBUG) Log.v(TAG, "requestPermission:granted," + permission);
//								// ユーザーがパーミッションを許可した場合
//								setNeverAskAgain(activity, permission, false);
//								callback.onPermission(permission);
//							} else {
//								if (DEBUG) Log.v(TAG, "requestPermission:denied," + permission);
//								// ユーザーがパーミッションを許可しなかった場合
//								if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
//									// この時点でshouldShowRequestPermissionRationale=falseになるのはユーザーが次回から表示市内を選択した時
//									setNeverAskAgain(activity, permission, true);
//								}
//								callback.onPermissionDenied(permission);
//							}
//						});
//				launcher.launch(permission);
//			} else {
//				if (DEBUG) Log.v(TAG, "requestPermission:don't ask again," + permission);
//				// ユーザーがパーミッション要求を拒否して次回から表示しないを選択している時
//				callback.onPermissionNeverAskAgain(permission);
//			}
//		}
//		return hasPermission;
//	}

	public static final void dumpPermissions(@Nullable final Context context) {
		if (context == null) return;
		try {
			final PackageManager pm = context.getPackageManager();
			final List<PermissionGroupInfo> list = pm.getAllPermissionGroups(PackageManager.GET_META_DATA);
			for (final PermissionGroupInfo info : list) {
				Log.d(TAG, "dumpPermissions:" + info.name + "=" + info);
			}
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
	}

	/**
	 * アプリのAndroidManifest.xmlで要求しているパーミッション一覧を取得する
	 *
	 * @param context
	 * @return
	 */
	@NonNull
	public static String[] requestedPermissions(@NonNull final Context context) {
		PackageInfo info = null;
		try {
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
		} catch (final PackageManager.NameNotFoundException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return (info != null) && (info.requestedPermissions != null)
			? info.requestedPermissions : new String[0];
	}

	/**
	 * アプリのAndroidManifestで要求している権限のうち指定した配列に含まれているものを取得する
	 * @param context
	 * @param permissions
	 * @return
	 */
	@NonNull
	public static String[] requestedPermissions(
		@NonNull final Context context,
		@NonNull final String[] permissions) {

		final ArrayList<String> result = new ArrayList<>();

		// AndroidManifestで要求している権限リストを取得
		final List<String> requested = Arrays.asList(requestedPermissions(context));
		for (final String permission: permissions) {
			if (requested.contains(permission)) {
				// 引数の配列に含まれている＆AndroidManifestでも要求しているものを追加
				result.add(permission);
			}
		}

		return result.toArray(new String[0]);
	}

	/**
	 * パーミッションを確認
	 *
	 * @param context
	 * @param permissionName
	 * @return
	 */
	public static int checkSelfPermission(@Nullable final Context context, final String permissionName) {
		if (context == null) return PackageManager.PERMISSION_DENIED;
		int result = PackageManager.PERMISSION_DENIED;
		try {
			result = ContextCompat.checkSelfPermission(context, permissionName);
			if (result == PackageManager.PERMISSION_GRANTED) {
				// パーミッションがあればNeverAskAgainフラグをクリアする
				setNeverAskAgain(context, permissionName, false);
			}
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}

	/**
	 * パーミッションを確認
	 *
	 * @param context
	 * @param permissionName
	 * @return 指定したパーミッションがあればtrue
	 */
	public static boolean hasPermission(@Nullable final Context context, final String permissionName) {
		if (context == null) return false;
		boolean result = false;
		try {
			result = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED;
			if (result) {
				// パーミッションがあればNeverAskAgainフラグをクリアする
				setNeverAskAgain(context, permissionName, false);
			}
		} catch (final Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}

	/**
	 * 指定した複数のパーミッションを確認して保持しているパーミッション配列を返す
	 *
	 * @param context
	 * @param permissions
	 * @return
	 */
	@NonNull
	public static String[] hasPermission(
		@Nullable final Context context,
		@NonNull final String[] permissions) {

		final ArrayList<String> result = new ArrayList<>();
		if (context != null) {
			for (final String permission : permissions) {
				if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
					result.add(permission);
					// パーミッションがあればNeverAskAgainフラグをクリアする
					setNeverAskAgain(context, permission, false);
				}
			}
		}
		return result.toArray(new String[0]);
	}

	/**
	 * 指定した複数のパーミッションを確認してすべてを保持している場合のみtrueを返す
	 *
	 * @param context
	 * @param permissions
	 * @return
	 */
	public static boolean hasPermissionAll(
		@Nullable final Context context,
		@NonNull final String[] permissions) {

		boolean result = true;
		if (context != null) {
			for (final String permission : permissions) {
				if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					// 1つでもパーミッションがなければfalse
					result = false;
				} else {
					// パーミッションがあればNeverAskAgainフラグをクリアする
					setNeverAskAgain(context, permission, false);
				}
			}
		}
		return result;
	}

	/**
	 * アプリのAndroidManifest.xmlで要求しているすべてのパーミッションを保持している場合のみtrueを返す
	 *
	 * @param context
	 * @return
	 */
	public static boolean hasPermissionAll(@Nullable final Context context) {
		boolean result = false;
		if (context != null) {
			result = hasPermissionAll(context, requestedPermissions(context));
		}
		return result;
	}

	/**
	 * 録音のミッションがあるかどうかを確認
	 *
	 * @param context
	 * @return 録音のパーミッションがあればtrue
	 */
	public static boolean hasAudio(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.RECORD_AUDIO);
	}

	/**
	 * ネットワークへのアクセスパーミッション(INTERNET)があるかどうかを確認
	 *
	 * @param context
	 * @return ネットワークへのアクセスパーミッションがあればtrue
	 */
	public static boolean hasNetwork(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.INTERNET);
	}

	/**
	 * ネットワーク接続状態へのアクセスパーミッション(ACCESS_NETWORK_STATE)があるかどうかを確認
	 *
	 * @param context
	 * @return ネットワーク接続状態へのアクセスパーミッションがあればtrue
	 */
	public static boolean hasNetworkState(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
	}

	/**
	 * 外部ストレージへの書き込みパーミッションがあるかどうかを確認
	 * API>=29では常にfalse
	 * API>=21なら基本的にSAF/DocumentFile+MediaStoreでアクセスする
	 *
	 * @param context
	 * @return 外部ストレージへの書き込みパーミッションがあればtrue
	 */
	public static boolean hasWriteExternalStorage(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	/**
	 * 外部ストレージからの読み込みパーミッションがあるかどうかを確認
	 * API>=33ならREAD_MEDIA_AUDIO/READ_MEDIA_IMAGES/READ_MEDIA_VIDEOのいずれか
	 * API>=29ならREAD_EXTERNAL_STORAGE
	 * API28以下ならREAD_EXTERNAL_STORAGE/WRITE_EXTERNAL_STORAGEのいずれか
	 * を保持していればtrue
	 * XXX API>=33の場合、どれか1つ保持していればtrueを返すけど権限を保持していない種類のメディアファイルへ
	 *     アクセスすると例外生成するので注意
	 * @param context
	 * @return 外部ストレージへの読み込みパーミッションがあればtrue
	 */
	public static boolean hasReadExternalStorage(@Nullable final Context context) {
		int numMissings = READ_MEDIA_PERMISSIONS.length;
		try {
			numMissings = missingPermissions(context, READ_MEDIA_PERMISSIONS).size();
		} catch (final Exception e) {
			// ignore
		}

		return numMissings < READ_MEDIA_PERMISSIONS.length;
	}

	/**
	 * 外部ストレージからの読み込みパーミッションを全て保持しているかどうかを確認
	 * API>=33ならREAD_MEDIA_AUDIO/READ_MEDIA_IMAGES/READ_MEDIA_VIDEOの全て
	 * API>=29ならREAD_EXTERNAL_STORAGE
	 * API28以下ならREAD_EXTERNAL_STORAGE/WRITE_EXTERNAL_STORAGEの全て
	 * を保持していればtrue
	 * @param context
	 * @return 外部ストレージへの読み込みパーミッションがあればtrue
	 */
	public static boolean hasReadExternalStorageAll(@Nullable final Context context) {
		int numMissings = READ_MEDIA_PERMISSIONS.length;
		try {
			numMissings = missingPermissions(context, READ_MEDIA_PERMISSIONS).size();
		} catch (final Exception e) {
			// ignore
		}
		return numMissings == 0;
	}

	/**
	 * 位置情報アクセスのパーミッションが有るかどうかを確認
	 *
	 * @param context
	 * @return
	 */
	public static boolean hasAccessLocation(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
			|| hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	/**
	 * 低精度位置情報アクセスのパーミッションが有るかどうかを確認
	 *
	 * @param context
	 * @return
	 */
	public static boolean hasAccessCoarseLocation(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
	}

	/**
	 * 高精度位置情報アクセスのパーミッションが有るかどうかを確認
	 *
	 * @param context
	 * @return
	 */
	public static boolean hasAccessFineLocation(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	/**
	 * バックグラウンドからの位置情報アクセスのパーミッションが有るかどうかを確認
	 * XXX ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATIONとACCESS_BACKGROUND_LOCATIONを同時に要求するとどちらも許可されないので注意
	 *
	 * @param context
	 * @return
	 */
	@SuppressLint("InlinedApi")
	public static boolean hasAccessBackgroundLocation(@Nullable final Context context) {
		return (!BuildCheck.isAndroid10() && hasAccessLocation(context))
			|| hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
	}

	/**
	 * カメラへアクセス可能かどうか
	 *
	 * @param context
	 * @return
	 */
	public static boolean hasCamera(@Nullable final Context context) {
		return hasPermission(context, Manifest.permission.CAMERA);
	}

	/**
	 * AndroidManifest.xmlに設定されているはずのパーミッションをチェックする
	 *
	 * @param context
	 * @return 空リストなら全てのパーミッションが入っていた,
	 * @throws IllegalArgumentException
	 * @throws PackageManager.NameNotFoundException
	 */
	@NonNull
	public static List<String> missingPermissions(
		@NonNull final Context context)
		throws IllegalArgumentException, PackageManager.NameNotFoundException {

		return missingPermissions(context, requestedPermissions(context));
	}

	/**
	 * AndroidManifest.xmlに設定されているはずのパーミッションをチェックする
	 *
	 * @param context
	 * @param requiredPermissions 確認するパーミッション文字列配列
	 * @return 空リストなら全てのパーミッションが入っていた,
	 * @throws IllegalArgumentException
	 * @throws PackageManager.NameNotFoundException
	 */
	@NonNull
	public static List<String> missingPermissions(
		@NonNull final Context context,
		@NonNull final String[] requiredPermissions)
		throws IllegalArgumentException, PackageManager.NameNotFoundException {

		return missingPermissions(context, Arrays.asList(requiredPermissions));
	}

	/**
	 * AndroidManifest.xmlに設定されているはずのパーミッションをチェックする
	 *
	 * @param context
	 * @param requiredPermissions 確認するパーミッション文字列配列
	 * @return 空リストなら全てのパーミッションが入っていた,
	 * @throws IllegalArgumentException
	 * @throws PackageManager.NameNotFoundException
	 */
	@NonNull
	public static List<String> missingPermissions(
		@NonNull final Context context,
		@NonNull final Collection<String> requiredPermissions)
		throws IllegalArgumentException, PackageManager.NameNotFoundException {

		final PackageManager pm = context.getPackageManager();
		final PackageInfo pi = pm.getPackageInfo(
			context.getPackageName(), PackageManager.GET_PERMISSIONS);
		final List<String> result = new ArrayList<>(requiredPermissions);
		final String[] permissions = pi.requestedPermissions;
		if (permissions != null) {
			for (String permission : permissions) {
				if (hasPermission(context, permission)) {
					result.remove(permission);
				}
			}
		}
		return result;
	}
}
