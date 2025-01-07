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
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.serenegiant.libcommon.databinding.FragmentRpgMessageViewBinding
import com.serenegiant.widget.RPGMessageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RPGMessageFragment : BaseFragment() {
	private lateinit var mBinding: FragmentRpgMessageViewBinding

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return FragmentRpgMessageViewBinding.inflate(inflater, container, false)
		.apply {
			mBinding = this
			lifecycleOwner = this@RPGMessageFragment.viewLifecycleOwner
		}
		.run {
			root
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		requireActivity().window.addFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_FULLSCREEN)
		requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		mBinding.message.apply {
			setTypeface(Typeface.SERIF)
		}
		.eventListener = object : RPGMessageView.MessageEventListener {
			/**
			 * メッセージがすべて表示されたときのイベントコールバック
			 * @param view
			 */
			override fun onMessageEnd(view: RPGMessageView): Int {
				// 負ならメッセージが消えるまで行送りする, 0以上なら行送りはせずにその時間後にクリアする
				// 2000ミリ秒後に消去
				return 2000
			}

			/**
			 * メッセージがクリアされたときのイベントコールバック
			 * @param view
			 */
			override fun onCleared(view: RPGMessageView) {
				if (DEBUG) Log.v(TAG, "onCleared:")
				if (!APPEND_MESSAGE) {
					lifecycleScope.launch {
						delay(APPEND_INTERVAL_MS)
						view.setText(MESSAGE)
					}
				}
			}
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_rpg_message_view)
	}

	override fun onResume() {
		super.onResume()
		lifecycleScope.launch {
			delay(1000)
			mBinding.message.setText(MESSAGE)
			var cnt = 0
			if (APPEND_MESSAGE) {
				while (isActive) {
					delay(APPEND_INTERVAL_MS)
					mBinding.message.addLine("Message${cnt++}\n")
					delay(1000)
					mBinding.message.addLine("Message${cnt++}\n")
					delay(1000)
					mBinding.message.addLine("Message${cnt++}\n")
					delay(1000)
					mBinding.message.addLine("Message${cnt++}\n")
					delay(1000)
					mBinding.message.addLine("Message${cnt++}\n")
				}
			}
		}
	}

	override fun onStop() {
		requireActivity().window.clearFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_FULLSCREEN)
		requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
		super.onStop()
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

//--------------------------------------------------------------------------------
	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = RPGMessageFragment::class.java.simpleName

		private const val APPEND_MESSAGE = true
		private const val MESSAGE = "012345678901234567890123456789\n012345678901234567890123456789abcd\nABCDEFGHIJKLMNOPQRSTUVWXYZ\nＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ\nあいうえお漢字カタカナ\n日本語を表示\nこの行を表示してから一定時間後にクリアする"
		private const val APPEND_INTERVAL_MS = 10000L

		fun newInstance() = RPGMessageFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
