package com.serenegiant.preference;
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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;

public class OnDisplayPreferenceDialogDelegater {
	private static final boolean DEBUG = false; //set false on production
	private static final String TAG = OnDisplayPreferenceDialogDelegater.class.getSimpleName();

	private static final String DIALOG_FRAGMENT_TAG =
         "com.serenegiant.preference.OnDisplayPreferenceDialogDelegater.DIALOG";

	/**
	 * PreferenceFragmentCompat#onDisplayPreferenceDialogのヘルパーメソッド
	 * @param fragment
	 * @param preference
	 * @return true: 処理済み, false: 未処理
	 */
	public static boolean onDisplayPreferenceDialog(
		@NonNull final PreferenceFragmentCompat fragment,
		final Preference preference) {

		if (DEBUG) Log.v(TAG, "onDisplayPreferenceDialog:" + preference);
		final androidx.fragment.app.FragmentManager fm = fragment.getParentFragmentManager();
		if (preference instanceof NumberPickerPreferenceV7) {
			// check if dialog is already showing
			if (fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
				return true;	// handled
			}

			final androidx.fragment.app.DialogFragment f
				= NumberPickerPreferenceFragmentCompat.newInstance(preference.getKey());
			f.setTargetFragment(fragment, 0);
			f.show(fm, DIALOG_FRAGMENT_TAG);
			return true;	// handled
		}
		return false;
	}

	/**
	 * PreferenceFragment#onDisplayPreferenceDialogのヘルパーメソッド
	 * @param fragment
	 * @param preference
	 * @return true: 処理済み, false: 未処理
	 */
	@Deprecated
	public static boolean onDisplayPreferenceDialog(
		@NonNull final PreferenceFragment fragment,
		final Preference preference) {

		if (DEBUG) Log.v(TAG, "onDisplayPreferenceDialog:" + preference);
		final android.app.FragmentManager fm = fragment.getFragmentManager();
		if (preference instanceof NumberPickerPreferenceV7) {
			// check if dialog is already showing
			if (fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
				return true;	// handled
			}

			final android.app.DialogFragment f
				= NumberPickerPreferenceFragment.newInstance(preference.getKey());
			f.setTargetFragment(fragment, 0);
			f.show(fm, DIALOG_FRAGMENT_TAG);
			return true;	// handled
		}
		return false;
	}

}
