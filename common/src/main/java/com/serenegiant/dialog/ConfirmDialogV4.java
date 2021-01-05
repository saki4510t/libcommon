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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * ユーザー確認用ダイアログ
 * 表示要求するActivity/FragmentはConfirmDialogListenerを実装していないといけない
 */
public class ConfirmDialogV4 extends DialogFragmentEx {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ConfirmDialogV4.class.getSimpleName();

	private static final String ARGS_KEY_CANCELED_ON_TOUCH_OUTSIDE
		= "ARGS_KEY_CANCELED_ON_TOUCH_OUTSIDE";
	private static final String ARGS_KEY_MESSAGE_STRING
		= "ARGS_KEY_MESSAGE_STRING";
	private static final String ARGS_KEY_ARGS
		= "ARGS_KEY_ARGS";

	/**
	 * ダイアログの表示結果を受け取るためのコールバックリスナー
	 */
	public static interface ConfirmDialogListener {
		/**
		 * 確認結果を引き渡すためのコールバックメソッド
		 * @param dialog
		 * @param requestCode
		 * @param result DialogInterface#BUTTONxxx
		 */
		public void onConfirmResult(
			@NonNull final ConfirmDialogV4 dialog,
			final int requestCode, final int result,
			@Nullable final Bundle userArgs);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param canceledOnTouchOutside
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		final boolean canceledOnTouchOutside) throws IllegalStateException {

		return showDialog(parent, requestCode, id_title, id_message, canceledOnTouchOutside, null);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param canceledOnTouchOutside
	 * @param userArgs
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		final boolean canceledOnTouchOutside,
		@Nullable final Bundle userArgs) throws IllegalStateException {

		final ConfirmDialogV4 dialog
			= newInstance(requestCode,
				id_title, id_message, null,
				canceledOnTouchOutside,
				userArgs);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * こっちはCharSequenceとしてメッセージ内容を指定
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param message
	 * @param canceledOnTouchOutside
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @NonNull final CharSequence message,
		final boolean canceledOnTouchOutside) throws IllegalStateException {

		return showDialog(parent, requestCode, id_title, message, canceledOnTouchOutside, null);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * こっちはCharSequenceとしてメッセージ内容を指定
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param message
	 * @param canceledOnTouchOutside
	 * @param userArgs
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @NonNull final CharSequence message,
		final boolean canceledOnTouchOutside,
		@Nullable final Bundle userArgs) throws IllegalStateException {

		final ConfirmDialogV4 dialog
			= newInstance(requestCode,
				id_title, 0, message,
				canceledOnTouchOutside,
				userArgs);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param canceledOnTouchOutside
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		final boolean canceledOnTouchOutside) throws IllegalStateException {

		return showDialog(parent, requestCode, id_title, id_message, canceledOnTouchOutside, null);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param canceledOnTouchOutside
	 * @param userArgs
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		final boolean canceledOnTouchOutside,
		@Nullable final Bundle userArgs) throws IllegalStateException {

		final ConfirmDialogV4 dialog
			= newInstance(requestCode,
				id_title, id_message, null,
				canceledOnTouchOutside,
				userArgs);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getParentFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * こっちはCharSequenceとしてメッセージ内容を指定
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param message
	 * @param canceledOnTouchOutside
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @NonNull final CharSequence message,
		final boolean canceledOnTouchOutside) throws IllegalStateException {

		return showDialog(parent, requestCode, id_title, message, canceledOnTouchOutside, null);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * こっちはCharSequenceとしてメッセージ内容を指定
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param message
	 * @param canceledOnTouchOutside
	 * @param userArgs
	 * @return
	 * @throws IllegalStateException
	 */
	public static ConfirmDialogV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @NonNull final CharSequence message,
		final boolean canceledOnTouchOutside,
		@Nullable final Bundle userArgs) throws IllegalStateException {

		final ConfirmDialogV4 dialog
			= newInstance(requestCode,
				id_title, 0, message,
				canceledOnTouchOutside,
				userArgs);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getParentFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ生成のためのヘルパーメソッド
	 * ダイアログ自体を直接生成せずにこのメソッドを呼び出すこと
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param message
	 * @param canceledOnTouchOutside
	 * @param userArgs
	 * @return
	 */
	public static ConfirmDialogV4 newInstance(
		final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@Nullable final CharSequence message,
		final boolean canceledOnTouchOutside,
		@Nullable final Bundle userArgs) {

		final ConfirmDialogV4 fragment = new ConfirmDialogV4();
		final Bundle args = new Bundle();
		// ここでパラメータをセットする
		args.putInt(ARGS_KEY_REQUEST_CODE, requestCode);
		args.putInt(ARGS_KEY_ID_TITLE, id_title);
		args.putInt(ARGS_KEY_ID_MESSAGE, id_message);
		args.putCharSequence(ARGS_KEY_MESSAGE_STRING, message);
		args.putBoolean(ARGS_KEY_CANCELED_ON_TOUCH_OUTSIDE, canceledOnTouchOutside);
		args.putBundle(ARGS_KEY_ARGS, userArgs);
		fragment.setArguments(args);
		return fragment;
	}

	private ConfirmDialogListener mListener;

	/**
	 * コンストラクタ, 直接生成せずに#newInstanceを使うこと
	 */
	public ConfirmDialogV4() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public void onAttach(@NonNull final Context context) {
		super.onAttach(context);
		// コールバックインターフェースを取得
		if (context instanceof ConfirmDialogListener) {
			mListener = (ConfirmDialogListener)context;
		}
		if (mListener == null) {
			final Fragment fragment = getTargetFragment();
			if (fragment instanceof ConfirmDialogListener) {
				mListener = (ConfirmDialogListener)fragment;
			}
		}
		if (mListener == null) {
			if (BuildCheck.isAndroid4_2()) {
				final Fragment target = getParentFragment();
				if (target instanceof ConfirmDialogListener) {
					mListener = (ConfirmDialogListener)target;
				}
			}
		}
		if (mListener == null) {
//			Log.w(TAG, "caller activity/fragment must implement ConfirmDialogV4#ConfirmDialogListener");
        	throw new ClassCastException(context.toString());
		}
	}

	@NonNull
	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = savedInstanceState != null ? savedInstanceState : requireArguments();
		final int id_title = args.getInt(ARGS_KEY_ID_TITLE);
		final int id_message = args.getInt(ARGS_KEY_ID_MESSAGE);
		final CharSequence message = args.getCharSequence(ARGS_KEY_MESSAGE_STRING);
		final boolean canceledOnTouchOutside = args.getBoolean(ARGS_KEY_CANCELED_ON_TOUCH_OUTSIDE);

		final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), getTheme())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(id_title)
			.setPositiveButton(android.R.string.ok, mOnClickListener)
			.setNegativeButton(android.R.string.cancel, mOnClickListener);
		if (id_message != 0) {
			builder.setMessage(id_message);
		} else {
			builder.setMessage(message);
		}
		final AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);
		return dialog;
	}

	private final DialogInterface.OnClickListener mOnClickListener
		= new DialogInterface.OnClickListener() {
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			// 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
			// 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
			callOnMessageDialogResult(which);
		}
	};

	@Override
	public void onCancel(@NonNull final DialogInterface dialog) {
		super.onCancel(dialog);
		callOnMessageDialogResult(DialogInterface.BUTTON_NEGATIVE);
	}
	
	/**
	 * コールバックリスナー呼び出しのためのヘルパーメソッド
	 * @param result DialogInterface#BUTTONxxx
	 */
	private void callOnMessageDialogResult(final int result)
		throws IllegalStateException {

		final Bundle args = requireArguments();
		final Bundle userArgs = args.getBundle(ARGS_KEY_ARGS);
		final int requestCode = args.getInt(ARGS_KEY_REQUEST_CODE);
		try {
			mListener.onConfirmResult(
				ConfirmDialogV4.this, requestCode, result, userArgs);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}
}
