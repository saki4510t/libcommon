package com.serenegiant.dialog;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.serenegiant.utils.BuildCheck;

/**
 * パーミッション要求前に説明用のダイアログを表示するためのDialogFragment
 */
public class MessageDialogFragment extends DialogFragment {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MessageDialogFragment.class.getSimpleName();

	public static interface MessageDialogListener {
		public void onMessageDialogResult(final MessageDialogFragment dialog, final int requestCode, final String[] permissions, final boolean result);
	}

	public static MessageDialogFragment showDialog(final Activity parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment dialog = newInstance(requestCode, id_title, id_message, permissions);
		dialog.show(parent.getFragmentManager(), TAG);
		return dialog;
	}

	public static MessageDialogFragment showDialog(final Fragment parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment dialog = newInstance(requestCode, id_title, id_message, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getFragmentManager(), TAG);
		return dialog;
	}

	public static MessageDialogFragment newInstance(final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment fragment = new MessageDialogFragment();
		final Bundle args = new Bundle();
		// ここでパラメータをセットする
		args.putInt("requestCode", requestCode);
		args.putInt("title", id_title);
		args.putInt("message", id_message);
		args.putStringArray("permissions", permissions != null ? permissions : new String[]{});
		fragment.setArguments(args);
		return fragment;
	}

	private MessageDialogListener mDialogListener;

	public MessageDialogFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@SuppressLint("NewApi")
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		// コールバックインターフェースを取得
		if (activity instanceof MessageDialogListener) {
			mDialogListener = (MessageDialogListener)activity;
		}
		if (mDialogListener == null) {
			final Fragment fragment = getTargetFragment();
			if (fragment instanceof MessageDialogListener) {
				mDialogListener = (MessageDialogListener)fragment;
			}
		}
		if (mDialogListener == null) {
			if (BuildCheck.isAndroid4_2()) {
				final Fragment target = getParentFragment();
				if (target instanceof MessageDialogListener) {
					mDialogListener = (MessageDialogListener)target;
				}
			}
		}
		if (mDialogListener == null) {
//			Log.w(TAG, "caller activity/fragment must implement PermissionDetailDialogFragmentListener");
        	throw new ClassCastException(activity.toString());
		}
	}

	@Override
    public void onCreate(final Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
	}

	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
		final int requestCode = getArguments().getInt("requestCode");
		final int id_title = getArguments().getInt("title");
		final int id_message = getArguments().getInt("message");
		final String[] permissions = args.getStringArray("permissions");


		return new AlertDialog.Builder(getActivity())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(id_title)
			.setMessage(id_message)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int whichButton) {
						// 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
						// 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
						mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode, permissions, true);
					}
				}
			)
			.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode, permissions, false);
					}
				}
			)
			.create();
	}

}
