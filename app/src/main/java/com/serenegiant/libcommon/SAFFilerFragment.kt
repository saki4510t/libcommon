package com.serenegiant.libcommon

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.SAFRootTreeDocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.libcommon.databinding.FragmentSafFilerBinding
import com.serenegiant.widget.DocumentTreeRecyclerAdapter
import com.serenegiant.widget.DocumentTreeRecyclerAdapter.DocumentTreeRecyclerAdapterListener
import java.io.IOException

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
*/ /**
 * DocumentTreeRecyclerAdapterとSAFRootTreeDocumentFileのテスト用Fragment
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SAFFilerFragment
/**
 * デフォルトコンストラクタ
 */
	: BaseFragment() {
	private var mBinding: FragmentSafFilerBinding? = null
	private var mAdapter: DocumentTreeRecyclerAdapter? = null
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		mBinding = DataBindingUtil.inflate(
			inflater,
			R.layout.fragment_saf_filer, container, false
		)
		return mBinding!!.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		initView()
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}
	//--------------------------------------------------------------------------------
	/**
	 * 表示を初期化
	 */
	private fun initView() {
		mAdapter = DocumentTreeRecyclerAdapter(
			requireContext(),
			R.layout.list_item_title,
			SAFRootTreeDocumentFile.fromContext(requireContext())
		)
		mAdapter!!.setListener(
			object : DocumentTreeRecyclerAdapterListener {
				override fun onItemClick(
					parent: RecyclerView.Adapter<*>,
					view: View,
					item: DocumentFile
				) {
					if (DEBUG) Log.v(TAG, "onItemClick:$item")
					if (item.isDirectory) {
						try {
							mAdapter!!.changeDir(item)
						} catch (e: IOException) {
							Log.w(TAG, e)
						}
//					} else {
//						// open specific file
					}
				}

				override fun onItemLongClick(
					parent: RecyclerView.Adapter<*>,
					view: View,
					item: DocumentFile
				): Boolean {
					if (DEBUG) Log.v(TAG, "onItemLongClick:$item")
					// show popup menu?
					return false
				}
			})
		mBinding!!.list.layoutManager = LinearLayoutManager(requireContext())
		mBinding!!.list.adapter = mAdapter
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SAFFilerFragment::class.java.simpleName

		fun newInstance() = SAFFilerFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
