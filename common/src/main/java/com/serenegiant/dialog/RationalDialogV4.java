package com.serenegiant.dialog;
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
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
	 */
	private static class RationalResource {
		@StringRes
		public final int titleRes;
		@StringRes
		public final int messageRes;

		public RationalResource(@StringRes final int titleRes, @StringRes final int messageRes) {
			this.titleRes = titleRes;
			this.messageRes = messageRes;
		}
	}

	/**
	 * デフォルトのパーミッション要求時の理由表示文字列リソース定義
	 */
	private static final Map<String, RationalResource> mRationalResources = new HashMap<>();
	static {
		mRationalResources.put(Manifest.permission.CAMERA,
			new RationalResource(R.string.permission_title, R.string.permission_camera_reason));
		mRationalResources.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
			new RationalResource(R.string.permission_title, R.string.permission_ext_storage_reason));
		mRationalResources.put(Manifest.permission.READ_EXTERNAL_STORAGE,
			new RationalResource(R.string.permission_title, R.string.permission_read_ext_storage_reason));
		mRationalResources.put(Manifest.permission.RECORD_AUDIO,
			new RationalResource(R.string.permission_title, R.string.permission_audio_recording_reason));
		mRationalResources.put(Manifest.permission.ACCESS_COARSE_LOCATION,
			new RationalResource(R.string.permission_title, R.string.permission_location_reason));
		mRationalResources.put(Manifest.permission.ACCESS_FINE_LOCATION,
			new RationalResource(R.string.permission_title, R.string.permission_location_reason));
		mRationalResources.put(Manifest.permission.ACCESS_NETWORK_STATE,
			new RationalResource(R.string.permission_title, R.string.permission_network_state_reason));
		mRationalResources.put(Manifest.permission.CHANGE_NETWORK_STATE,
			new RationalResource(R.string.permission_title, R.string.permission_change_network_state_reason));
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
			return showDialog(parent, res.titleRes, res.messageRes, new String[] {permission});
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
			return showDialog(parent, res.titleRes, res.messageRes, new String[] {permission});
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
		// ここでパラメータをセットする
		args.putInt(ARGS_KEY_ID_TITLE, titleRes);
		args.putInt(ARGS_KEY_ID_MESSAGE, messageRes);
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
		final Bundle args = savedInstanceState != null ? savedInstanceState : requireArguments();
		final int id_title = args.getInt(ARGS_KEY_ID_TITLE);
		final int id_message = args.getInt(ARGS_KEY_ID_MESSAGE);

		final Activity activity = requireActivity();
		return new AlertDialog.Builder(activity, getTheme())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(id_title)
			.setMessage(id_message)
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
