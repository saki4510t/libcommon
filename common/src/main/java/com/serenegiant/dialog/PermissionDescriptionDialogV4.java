package com.serenegiant.dialog;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * パーミッション要求前に説明を表示するためのDialogFragment実装
 */
public class PermissionDescriptionDialogV4 extends DialogFragmentEx {
//	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = PermissionDescriptionDialogV4.class.getSimpleName();

	private static final String ARGS_KEY_PERMISSIONS = "permissions";

	/**
	 * ダイアログの表示結果を受け取るためのコールバックリスナー
	 */
	public interface DialogResultListener {
		public void onDialogResult(
			@NonNull final PermissionDescriptionDialogV4 dialog, final int requestCode,
			@NonNull final String[] permissions, final boolean result);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static PermissionDescriptionDialogV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final PermissionDescriptionDialogV4 dialog
			= newInstance(requestCode, id_title, id_message, permissions);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static PermissionDescriptionDialogV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final PermissionDescriptionDialogV4 dialog
			= newInstance(requestCode, id_title, id_message, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.requireFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ生成のためのヘルパーメソッド
	 * ダイアログ自体を直接生成せずにこのメソッドを呼び出すこと
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 */
	public static PermissionDescriptionDialogV4 newInstance(
		final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) {

		final PermissionDescriptionDialogV4 fragment = new PermissionDescriptionDialogV4();
		final Bundle args = new Bundle();
		// ここでパラメータをセットする
		args.putInt(ARGS_KEY_REQUEST_CODE, requestCode);
		args.putInt(ARGS_KEY_TITLE_ID, id_title);
		args.putInt(ARGS_KEY_MESSAGE_ID, id_message);
		args.putStringArray(ARGS_KEY_PERMISSIONS, permissions);
		fragment.setArguments(args);
		return fragment;
	}

	private DialogResultListener mDialogListener;

	/**
	 * コンストラクタ, 直接生成せずに#newInstanceを使うこと
	 */
	public PermissionDescriptionDialogV4() {
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
		final int id_title = args.getInt(ARGS_KEY_TITLE_ID);
		final int id_message = args.getInt(ARGS_KEY_MESSAGE_ID);

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
		final int requestCode = args.getInt(ARGS_KEY_REQUEST_CODE);
		final String[] permissions = args.getStringArray(ARGS_KEY_PERMISSIONS);
		try {
			mDialogListener.onDialogResult(
				PermissionDescriptionDialogV4.this,
				requestCode, permissions, result);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}
}
