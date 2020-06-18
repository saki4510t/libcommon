package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.libcommon.ImageFragment.Companion.newInstance
import com.serenegiant.mediastore.MediaInfo
import com.serenegiant.mediastore.MediaStoreCursorRecyclerAdapter
import com.serenegiant.mediastore.MediaStoreRecyclerAdapter
import com.serenegiant.mediastore.MediaStoreRecyclerAdapterListener
import com.serenegiant.widget.RecyclerViewWithEmptyView

/**
 * 端末内の静止画・動画一覧を表示するためのFragment
 * RecyclerViewを使用
 * FIXME レイアウトの設定がいまいちで表示がきれくない(´・ω・｀)
 */
class GalleyFragment2 : BaseFragment() {

	private var mMediaStoreAdapter: MediaStoreCursorRecyclerAdapter? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_galley_recycler)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val rootView = inflater.inflate(R.layout.fragment_galley2, container, false)
		initView(rootView)
		return rootView
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (mMediaStoreAdapter != null) {
			mMediaStoreAdapter!!.refresh()
		}
	}

	/**
	 * Viewを初期化
	 * @param rootView
	 */
	private fun initView(rootView: View) {
		val recyclerView: RecyclerViewWithEmptyView = rootView.findViewById(R.id.media_recyclerview)
		recyclerView.setHasFixedSize(true)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
//		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setEmptyView(rootView.findViewById(R.id.empty))
		mMediaStoreAdapter = MediaStoreCursorRecyclerAdapter(
			requireContext(), R.layout.grid_item_media, false)
		mMediaStoreAdapter!!.setListener(mListener)
		recyclerView.adapter = mMediaStoreAdapter
	}

	private val mListener: MediaStoreRecyclerAdapterListener
		= object : MediaStoreRecyclerAdapterListener {

		override fun onItemClick(parent: RecyclerView.Adapter<*>,
		view: View, item: MediaInfo) {
			if (DEBUG) Log.v(TAG, "onItemClick:$item")
			parentFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.replace(R.id.container, newInstance(item))
				.commit()
		}

		override fun onItemLongClick(parent: RecyclerView.Adapter<*>,
			view: View, item: MediaInfo): Boolean {

			if (DEBUG) Log.v(TAG, "onItemLongClick:$item")
			return false
		}
	}

	companion object {
		private const val DEBUG = true // FIXME 実働時はfalseにすること
		private val TAG = GalleyFragment2::class.java.simpleName

		fun newInstance(): GalleyFragment2 {
			return GalleyFragment2()
		}
	}
}