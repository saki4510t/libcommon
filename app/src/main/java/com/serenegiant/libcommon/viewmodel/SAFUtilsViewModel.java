package com.serenegiant.libcommon.viewmodel;
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

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.serenegiant.libcommon.BR;
import com.serenegiant.libcommon.Const;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

public abstract class SAFUtilsViewModel extends BaseObservable
	implements View.OnClickListener {

	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SAFUtilsViewModel.class.getSimpleName();

	private int mRequestCode = Const.REQUEST_ACCESS_SD;

	@Bindable
	public int getRequestCode() {
		if (DEBUG) Log.v(TAG, "getRequestCode:" + mRequestCode);
		return mRequestCode;
	}

	public void setRequestCode(final int value) {
		if (DEBUG) Log.v(TAG, "setRequestCode:" + value);
		if (mRequestCode != value) {
			if ((value & 0xffff) == value) {
				// Fragmentからのリクエストコードは下位16ビットしか使えないので制限
				mRequestCode = value;
			}
			// Notify observers of a new value.
			notifyPropertyChanged(BR.requestCode);
		}
	}

	/**
	 * Converter#requestCodeToString/stringToRequestCodeを使うと
	 * kotline関係の処理でエラーになってコード生成できないので
	 * 文字列用のバインディングを追加
	 * @return
	 */
	@Bindable
	public String getRequestCodeString() {
		return Integer.toString(getRequestCode());
	}

	/**
	 * Converter#requestCodeToString/stringToRequestCodeを使うと
	 * kotline関係の処理でエラーになってコード生成できないので
	 * 文字列用のバインディングを追加
	 * @param value
	 */
	public void setRequestCodeString(final String value) {
		final int v = parseIntWithoutException(value, mRequestCode);
		if (v != mRequestCode) {
			setRequestCode(parseIntWithoutException(value, mRequestCode));
			// Notify observers of a new value.
			notifyPropertyChanged(BR.requestCodeString);
		}
	}

	/**
	 * 例外生成せずに10進整数文字列を整数に変換、変換できないときはデフォルト値を返す
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	private static int parseIntWithoutException(final String value, final int defaultValue) {
		int result = defaultValue;
		if (!TextUtils.isEmpty(value)) {
			try {
				result = Integer.parseInt(value);
			} catch (final NumberFormatException e) {
				if (DEBUG) Log.d(TAG, "setRequestCodeString:", e);
			}
		}
		return result;
	}
}
