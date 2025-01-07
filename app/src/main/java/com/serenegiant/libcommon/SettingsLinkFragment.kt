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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.serenegiant.libcommon.databinding.FragmentSettingsLinkBinding
import com.serenegiant.system.SettingsUtils

/**
 * ユーザーがアプリに権限付与をしなかったために機能を実行できない時に
 * 端末のアプリ設定画面への繊維ボタンを表示するFragment
 */
class SettingsLinkFragment : Fragment() {
	private lateinit var mBinding: FragmentSettingsLinkBinding
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View {

		return FragmentSettingsLinkBinding.inflate(inflater,
			container, false)
		.apply {
			mBinding = this
			onClickListener = View.OnClickListener { v -> // 端末のアプリ設定を開く
				SettingsUtils.openSettingsAppDetails(v.context)
			}
		}.run {
			root
		}
	}

	companion object {
		fun newInstance() = SettingsLinkFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
