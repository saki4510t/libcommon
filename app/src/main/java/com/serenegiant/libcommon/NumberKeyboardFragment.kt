package com.serenegiant.libcommon
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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.serenegiant.system.KeyboardDelegater
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.KeyboardView

/**
 * NumberKeyboardテスト用フラグメント
 */
class NumberKeyboardFragment : BaseFragment() {

	private var mInputEditText: EditText? = null
	private var mKeyboardView: KeyboardView? = null
	private var mDelegater: KeyboardDelegater? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_number_keyboard)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_NumberKeyboard)
		return customInflater.inflate(R.layout.fragment_numberkeyboard, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		initView(view)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		mDelegater!!.showKeyboard()
	}

	override fun internalOnPause() {
		mDelegater!!.hideKeyboard()
		super.internalOnPause()
	}

	private fun initView(rootView: View) {
		mInputEditText = rootView.findViewById(R.id.edittext)
		mKeyboardView = rootView.findViewById(R.id.number_keyboardview)
		mDelegater = object : KeyboardDelegater(
			mInputEditText!!, mKeyboardView!!, com.serenegiant.common.R.xml.keyboard_number) {

			override fun onCancelClick() {
				if (DEBUG) Log.v(TAG, "onCancelClick:")
			}

			override fun onOkClick() {
				if (DEBUG) Log.v(TAG, "onOkClick:")
			}
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = NumberKeyboardFragment::class.java.simpleName

		fun newInstance() = NumberKeyboardFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
