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
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.serenegiant.libcommon.databinding.FragmentWindowInsetsBinding
import com.serenegiant.view.SysUiUtils

class WindowInsetsFragment : BaseFragment() {

	private var mIsFullScreen = false
	private var mSysUiUtils: SysUiUtils? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_window_insets)
		mSysUiUtils = SysUiUtils(requireActivity())
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return FragmentWindowInsetsBinding.inflate(inflater, container, false)
		.apply {
			onLongClickListener = View.OnLongClickListener {
				toggleFullScreen(root)
				textView.text = "$mIsFullScreen"
				return@OnLongClickListener true
			}
		}
		.run {
			root
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		mSysUiUtils = null
		super.onDetach()
	}

	private fun toggleFullScreen(rootView: View) {
		if (DEBUG) Log.v(TAG, "toggleFullScreen:$mIsFullScreen")
		mIsFullScreen = !mIsFullScreen
		// XXX WindowInsetsControllerCompatを使うときと従来のdecorView.setSystemUiVisibilityを
		//     使うときで微妙に挙動が違う気がする
		if (USE_WINDOW_INSETS) {
			WindowInsetsControllerCompat(window, rootView).also { controller ->
				val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
				if (mIsFullScreen) {
					WindowCompat.setDecorFitsSystemWindows(window, false)
					controller.hide(INSET_TYPES)
					controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
					actionBar?.hide()
				} else {
					WindowCompat.setDecorFitsSystemWindows(window, true)
					controller.show(INSET_TYPES)
					actionBar?.show()
				}
			}
		} else {
			mSysUiUtils?.setSystemUIVisibility(!mIsFullScreen)
		}
	}

	@get:Throws(IllegalStateException::class)
	private val window: Window
		get() = requireActivity().window

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = WindowInsetsFragment::class.java.simpleName

		private const val USE_WINDOW_INSETS = true

		private val INSET_TYPES = (WindowInsetsCompat.Type.statusBars()
			or WindowInsetsCompat.Type.navigationBars()
			or WindowInsetsCompat.Type.captionBar()
			or WindowInsetsCompat.Type.systemBars())

		fun newInstance() = WindowInsetsFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
