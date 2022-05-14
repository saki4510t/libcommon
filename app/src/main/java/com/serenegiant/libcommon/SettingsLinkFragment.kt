package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.serenegiant.libcommon.databinding.FragmentSettingsLinkBinding

/**
 * ユーザーがアプリに権限付与をしなかったために機能を実行できない時に
 * 端末のアプリ設定画面への繊維ボタンを表示するFragment
 */
class SettingsLinkFragment : Fragment() {
	private var mBinding: FragmentSettingsLinkBinding? = null
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mBinding = DataBindingUtil.inflate(
			inflater,
			R.layout.fragment_settings_link, container, false
		)
		return mBinding!!.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		init()
	}

	private fun init() {
		mBinding!!.onClickListener = View.OnClickListener { v -> // 端末のアプリ設定を開く
			val context = v.context
			val uriString = "package:" + context.packageName
			val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
			context.startActivity(intent)
		}
	}

	companion object {
		fun newInstance(): SettingsLinkFragment {
			val fragment = SettingsLinkFragment()
			val args = Bundle()
			fragment.arguments = args
			return fragment
		}
	}
}