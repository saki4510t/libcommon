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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public abstract class DialogFragmentEx extends DialogFragment {
	private static final String TAG = DialogFragmentEx.class.getSimpleName();

	protected static final String ARGS_KEY_REQUEST_CODE = "requestCode";
	protected static final String ARGS_KEY_ID_TITLE = "title";
	protected static final String ARGS_KEY_ID_MESSAGE = "message";
	protected static final String ARGS_KEY_TAG = "tag";

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
		final Bundle args = getArguments();
		if (args != null) {
			outState.putAll(args);
		}
	}

// androidxのFragmentでいつのまにか実装されててしかもfinalになってるからoverrideもできん
//	@NonNull
//	protected Bundle requireArguments() throws IllegalStateException {
//		final Bundle args = getArguments();
//		if (args == null) {
//			throw new IllegalStateException();
//		}
//		return args;
//	}
//
	@Override
	public final void onStart() {
		super.onStart();
		if (BuildCheck.isAndroid7()) {
			internalOnResume();
		}
	}

	@Override
	public final void onResume() {
		super.onResume();
		if (!BuildCheck.isAndroid7()) {
			internalOnResume();
		}
	}

	@Override
	public final void onPause() {
		if (!BuildCheck.isAndroid7()) {
			internalOnPause();
		}
		super.onPause();
	}

	@Override
	public final void onStop() {
		if (BuildCheck.isAndroid7()) {
			internalOnPause();
		}
		super.onStop();
	}
	
	/**
	 * Android6未満でのonResume, Android7以上でのonStartの処理
	 * この中からonResumeやonStartを呼んでは行けない(無限ループになる)
	 * onResumeとonStartはこのクラスでfinalにしてあるので子クラスで
	 * 追加処理が必要な場合にはこのメソッドをoverrideすること
	 */
	protected void internalOnResume() {
	}

	/**
	 * Android6未満でのonPause, Android7以上でのonStopの処理
	 * この中からonPauseやonTopを呼んでは行けない(無限ループになる)
	 * onPauseとonStopはこのクラスでfinalにしてあるので子クラスで
	 * 追加処理が必要な場合にはこのメソッドをoverrideすること
	 */
	protected void internalOnPause() {
	}

	protected void popBackStack() {
		final Activity activity = getActivity();
		if ((activity == null) || activity.isFinishing()) return;
		try {
			getParentFragmentManager().popBackStack();
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}
}
