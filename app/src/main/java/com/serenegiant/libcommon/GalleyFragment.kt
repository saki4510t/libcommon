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
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import androidx.fragment.app.FragmentTransaction
import com.serenegiant.libcommon.ImageFragment.Companion.newInstance
import com.serenegiant.mediastore.MediaStoreAdapter

/**
 * 端末内の静止画・動画一覧を表示するためのFragment
 */
class GalleyFragment : BaseFragment() {

	private var mMediaStoreAdapter: MediaStoreAdapter? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_galley)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val rootView = inflater.inflate(R.layout.fragment_galley, container, false)
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
		val gridView = rootView.findViewById<GridView>(R.id.media_gridview)
		mMediaStoreAdapter = MediaStoreAdapter(requireContext(), R.layout.grid_item_media, false)
		gridView.adapter = mMediaStoreAdapter
		gridView.onItemClickListener = mOnItemClickListener
	}

	private val mOnItemClickListener = AdapterView.OnItemClickListener {
		parent, view, position, id ->
			when (parent.id) {
				R.id.media_gridview -> doPlay(position, id)
			}
	}

	private fun doPlay(position: Int, id: Long) {
		val info = mMediaStoreAdapter!!.getMediaInfo(position)
		if (DEBUG) Log.v(TAG, "" + info)
		when (info.mediaType) {
			MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->  // 静止画を選択した時
				requireFragmentManager()
					.beginTransaction()
					.addToBackStack(null)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.replace(R.id.container, newInstance(info))
					.commit()
			MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
			}
			else -> {
			}
		}
	}

	companion object {
		private const val DEBUG = false // FIXME 実働時はfalseにすること
		private val TAG = GalleyFragment::class.java.simpleName

		fun newInstance(): GalleyFragment {
			return GalleyFragment()
		}
	}
}