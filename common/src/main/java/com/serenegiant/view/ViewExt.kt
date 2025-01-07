package com.serenegiant.view
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

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.serenegiant.common.R

/*
 * データバインディングでLiveDataに合わせてスナックバーを表示するためのヘルパー用View拡張
 */

/**
 * Snackbar表示の準備
 * @param lifecycleOwner
 * @param snackbarEvent
 * @param duration Snackbarの表示時間, Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONGまたはミリ秒
 * @param callback Snackbar.Callback, null可, デフォルトnull
 */
fun View.setupSnackbar(
	lifecycleOwner: LifecycleOwner,
	snackbarEvent: LiveData<Event<CharSequence>>, duration: LiveData<Event<Int>>,
	callback: Snackbar.Callback? = null) {
    snackbarEvent.observeEvent(lifecycleOwner) {
		showSnackbar(it, duration.value?.value ?: Snackbar.LENGTH_LONG, callback)
	}
}

/**
 * Snackbar表示の準備
 * @param lifecycleOwner
 * @param snackbarEvent
 * @param duration Snackbarの表示時間, Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONGまたはミリ秒
 * @param callback Snackbar.Callback, null可, デフォルトnull
 */
fun View.setupSnackbar2(
	lifecycleOwner: LifecycleOwner,
	snackbarEvent: LiveData<CharSequence>, duration: LiveData<Int>,
	callback: Snackbar.Callback? = null) {
    snackbarEvent.observe(lifecycleOwner) {
		showSnackbar(it, duration.value ?: Snackbar.LENGTH_LONG, callback)
	}
}

/**
 * Snackbarの表示処理
 * @param message
 * @param duration Snackbarの表示時間, Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONGまたはミリ秒
 * @param callback Snackbar.Callback, null可, デフォルトnull
 */
private fun View.showSnackbar(
	message: CharSequence, duration: Int,
	callback: Snackbar.Callback? = null) {

	val snackbar = getTag(R.id.snackbar)
	if ((snackbar is Snackbar) && snackbar.isShown) {
		snackbar.dismiss()
	}
	if (message.isNotEmpty()) {
		Snackbar.make(this, message, duration)
		.apply {
			if (callback != null) {
				addCallback(callback)
			}
			this@showSnackbar.setTag(R.id.snackbar, this)
		}.show()
	}
}
