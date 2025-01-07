package com.serenegiant.dialog;
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.system.BuildCheck;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * パーミッション要求前に説明を表示するためのDialogFragment実装
 * PermissionDescriptionDialogV4と違ってこちらは要求コードを使わない(permissionsの文字列で判別)
 */
public class RationalDialogV4 extends DialogFragmentEx {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = RationalDialogV4.class.getSimpleName();

	private static final String ARGS_KEY_PERMISSIONS = "permissions";

	/**
	 * ダイアログの表示結果を受け取るためのコールバックリスナー
	 */
	public interface DialogResultListener {
		public void onDialogResult(
			@NonNull final RationalDialogV4 dialog,
			@NonNull final String[] permissions, final boolean result);
	}

	/**
	 * ダイアログ表示用の文字列リソースのホルダークラス
	 * FIXME 個別の文字列/文字列リソースをダイアログフラグメントへ渡すよりも
	 *       RationalResourceをParcelableにして引き渡す方が良いかもしれない
	 */
	private static class RationalResource {
		@NonNull
		private final String permission;
		@StringRes
		private final int titleRes;
		@StringRes
		private final int messageRes;

		public RationalResource(
			@NonNull final String permission,
			@StringRes final int titleRes, @StringRes final int messageRes) {
			this.permission = permission;
			this.titleRes = titleRes;
			this.messageRes = messageRes;
		}

		public CharSequence getTitle(@NonNull final Context context) {
			return context.getText(titleRes);
		}

		public CharSequence getMessage(@NonNull final Context context) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				// Android11/API30以上
				if (Manifest.permission.ACCESS_BACKGROUND_LOCATION.equals(permission)) {
					// Android11/API30以上でバッググラウンドでの位置情報取得パーミッションの時
					// Android10/API29の時はgetBackgroundPermissionOptionLabelが使えないので
					// 置換無しのリソース文字列なのでここでは処理しない
					return context.getString(messageRes,
						context.getPackageManager().getBackgroundPermissionOptionLabel());
				}
			}
			return context.getText(messageRes);
		}
	}

	/**
	 * デフォルトのパーミッション要求時の理由表示文字列リソース定義
	 */
	private static final Map<String, RationalResource> mRationalResources = new HashMap<>();
	static {
		mRationalResources.put(Manifest.permission.CAMERA,
			new RationalResource(Manifest.permission.CAMERA,
				R.string.permission_title, R.string.permission_camera_reason));
		mRationalResources.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
			new RationalResource(Manifest.permission.WRITE_EXTERNAL_STORAGE,
				R.string.permission_title, R.string.permission_ext_storage_reason));
		mRationalResources.put(Manifest.permission.READ_EXTERNAL_STORAGE,
			new RationalResource(Manifest.permission.READ_EXTERNAL_STORAGE,
				R.string.permission_title, R.string.permission_read_ext_storage_reason));
		mRationalResources.put(Manifest.permission.RECORD_AUDIO,
			new RationalResource(Manifest.permission.RECORD_AUDIO,
				R.string.permission_title, R.string.permission_audio_recording_reason));
		mRationalResources.put(Manifest.permission.ACCESS_COARSE_LOCATION,
			new RationalResource(Manifest.permission.ACCESS_COARSE_LOCATION,
				R.string.permission_title, R.string.permission_location_reason));
		mRationalResources.put(Manifest.permission.ACCESS_FINE_LOCATION,
			new RationalResource(Manifest.permission.ACCESS_FINE_LOCATION,
				R.string.permission_title, R.string.permission_location_reason));
		mRationalResources.put(Manifest.permission.INTERNET,
			new RationalResource(Manifest.permission.INTERNET,
				R.string.permission_title, R.string.permission_network_reason));
		mRationalResources.put(Manifest.permission.ACCESS_NETWORK_STATE,
			new RationalResource(Manifest.permission.ACCESS_NETWORK_STATE,
				R.string.permission_title, R.string.permission_network_state_reason));
		mRationalResources.put(Manifest.permission.CHANGE_NETWORK_STATE,
			new RationalResource(Manifest.permission.CHANGE_NETWORK_STATE,
				R.string.permission_title, R.string.permission_change_network_state_reason));
		mRationalResources.put(Manifest.permission.ACCESS_WIFI_STATE,
			new RationalResource(Manifest.permission.ACCESS_WIFI_STATE,
				R.string.permission_title, R.string.permission_access_wifi_state_reason));
		mRationalResources.put(Manifest.permission.READ_PHONE_STATE,
			new RationalResource(Manifest.permission.READ_PHONE_STATE,
				R.string.permission_title, R.string.permission_read_phone_state_reason));
		mRationalResources.put(Manifest.permission.READ_SMS,
			new RationalResource(Manifest.permission.READ_SMS,
				R.string.permission_title, R.string.permission_read_sms_reason));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android11/API30以降
			mRationalResources.put(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
				new RationalResource(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
					R.string.permission_title, R.string.permission_access_background_location_api30));
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// Android10/API29以降
			mRationalResources.put(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
				new RationalResource(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
					R.string.permission_title, R.string.permission_access_background_location));
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mRationalResources.put(Manifest.permission.READ_PHONE_NUMBERS,
				new RationalResource(Manifest.permission.READ_PHONE_NUMBERS,
					R.string.permission_title, R.string.permission_read_phone_number));
		}
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * 表示するタイトル・メッセージはデフォルトのリソースを使う
	 * デフォルトのリソース設定がない場合はダイアログを表示せずにnullを返す
	 * @param parent
	 * @param permission
	 * @return
	 */
	@Nullable
	public static RationalDialogV4 showDialog(
		@NonNull final FragmentActivity parent,
		@NonNull final String permission) {

		final RationalResource res = mRationalResources.containsKey(permission)
			? mRationalResources.get(permission) : null;
		if (res != null) {
			return showDialog(parent,
				res.getTitle(parent), res.getMessage(parent),
				new String[] {permission});
		}
		return null;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * 表示するタイトル・メッセージはデフォルトのリソースを使う
	 * デフォルトのリソース設定がない場合はダイアログを表示せずにnullを返す
	 * @param parent
	 * @param permission
	 * @return
	 */
	@Nullable
	public static RationalDialogV4 showDialog(
		@NonNull final Fragment parent,
		@NonNull final String permission) {

		final RationalResource res = mRationalResources.containsKey(permission)
			? mRationalResources.get(permission) : null;
		if (res != null) {
			final Context context = parent.requireContext();
			return showDialog(parent,
				res.getTitle(context), res.getMessage(context),
				new String[] {permission});
		}
		return null;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param titleRes
	 * @param messageRes
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static RationalDialogV4 showDialog(
		@NonNull final FragmentActivity parent,
		@StringRes final int titleRes, @StringRes final int messageRes,
		@NonNull final String[] permissions) throws IllegalStateException {

		final RationalDialogV4 dialog
			= newInstance(titleRes, messageRes, permissions);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param titleRes
	 * @param messageRes
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static RationalDialogV4 showDialog(
		@NonNull final Fragment parent,
		@StringRes final int titleRes, @StringRes final int messageRes,
		@NonNull final String[] permissions) throws IllegalStateException {

		final RationalDialogV4 dialog
			= newInstance(titleRes, messageRes, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getParentFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param title
	 * @param message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static RationalDialogV4 showDialog(
		@NonNull final FragmentActivity parent,
		@NonNull final CharSequence title, @NonNull final CharSequence message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final RationalDialogV4 dialog
			= newInstance(title, message, permissions);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param title
	 * @param message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static RationalDialogV4 showDialog(
		@NonNull final Fragment parent,
		@NonNull final CharSequence title, @NonNull final CharSequence message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final RationalDialogV4 dialog
			= newInstance(title, message, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getParentFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ生成のためのヘルパーメソッド
	 * ダイアログ自体を直接生成せずにこのメソッドを呼び出すこと
	 * @param titleRes
	 * @param messageRes
	 * @param permissions
	 * @return
	 */
	public static RationalDialogV4 newInstance(
		@StringRes final int titleRes, @StringRes final int messageRes,
		@NonNull final String[] permissions) {

		final RationalDialogV4 fragment = new RationalDialogV4();
		final Bundle args = new Bundle();
		if ((titleRes == 0) || (messageRes == 0) || (permissions.length == 0)) {
			throw new IllegalArgumentException("wrong resource id or permissions has no element!");
		}
		// ここでパラメータをセットする
		args.putInt(ARGS_KEY_TITLE_ID, titleRes);
		args.putInt(ARGS_KEY_MESSAGE_ID, messageRes);
		args.putStringArray(ARGS_KEY_PERMISSIONS, permissions);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * ダイアログ生成のためのヘルパーメソッド
	 * ダイアログ自体を直接生成せずにこのメソッドを呼び出すこと
	 * @param title
	 * @param message
	 * @param permissions
	 * @return
	 */
	public static RationalDialogV4 newInstance(
		@NonNull final CharSequence title, @NonNull final CharSequence message,
		@NonNull final String[] permissions) {

		final RationalDialogV4 fragment = new RationalDialogV4();
		final Bundle args = new Bundle();
		if (permissions.length == 0) {
			throw new IllegalArgumentException("permissions has no element!");
		}
		// ここでパラメータをセットする
		args.putCharSequence(ARGS_KEY_TITLE_STRING, title);
		args.putCharSequence(ARGS_KEY_MESSAGE_STRING, message);
		args.putStringArray(ARGS_KEY_PERMISSIONS, permissions);
		fragment.setArguments(args);
		return fragment;
	}

	@Nullable
	private DialogResultListener mDialogListener;

	/**
	 * コンストラクタ, 直接生成せずに#newInstanceを使うこと
	 */
	public RationalDialogV4() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public void onAttach(@NonNull final Context context) {
		super.onAttach(context);
		// コールバックインターフェースを取得
		if (context instanceof DialogResultListener) {
			mDialogListener = (DialogResultListener)context;
		}
		if (mDialogListener == null) {
			final Fragment fragment = getTargetFragment();
			if (fragment instanceof DialogResultListener) {
				mDialogListener = (DialogResultListener)fragment;
			}
		}
		if (mDialogListener == null) {
			if (BuildCheck.isAndroid4_2()) {
				final Fragment target = getParentFragment();
				if (target instanceof DialogResultListener) {
					mDialogListener = (DialogResultListener)target;
				}
			}
		}
		if (mDialogListener == null) {
//			Log.w(TAG, "caller activity/fragment must implement PermissionDetailDialogFragmentListener");
        	throw new ClassCastException(context.toString());
		}
	}

//	@Override
//	public void onCreate(final Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
//	}

	@NonNull
	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Activity activity = requireActivity();
		final Bundle args = savedInstanceState != null ? savedInstanceState : requireArguments();
		final int titleId = args.getInt(ARGS_KEY_TITLE_ID);
		final int messageId = args.getInt(ARGS_KEY_MESSAGE_ID);
		final CharSequence titleText
			= args.getCharSequence(ARGS_KEY_TITLE_STRING, titleId != 0 ? activity.getText(titleId) : "");
		final CharSequence messageText
			= args.getCharSequence(ARGS_KEY_MESSAGE_STRING, messageId != 0 ? activity.getText(messageId) : "");

		return new AlertDialog.Builder(activity, getTheme())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(titleText)
			.setMessage(messageText)
			.setPositiveButton(android.R.string.ok, mOnClickListener)
			.setNegativeButton(android.R.string.cancel, mOnClickListener)
			.create();
	}

	private final DialogInterface.OnClickListener mOnClickListener
		= new DialogInterface.OnClickListener() {
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			// 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
			// 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
			callOnMessageDialogResult(which == DialogInterface.BUTTON_POSITIVE);
		}
	};

	@Override
	public void onCancel(@NonNull final DialogInterface dialog) {
		super.onCancel(dialog);
		callOnMessageDialogResult(false);
	}
	
	/**
	 * コールバックリスナー呼び出しのためのヘルパーメソッド
	 * @param result
	 */
	private void callOnMessageDialogResult(final boolean result)
		throws IllegalStateException {

		final Bundle args = requireArguments();
		final String[] permissions = args.getStringArray(ARGS_KEY_PERMISSIONS);
		try {
			mDialogListener.onDialogResult(RationalDialogV4.this,
				permissions, result);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}
}
