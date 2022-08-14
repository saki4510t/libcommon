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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener
import com.serenegiant.libcommon.databinding.FragmentItemListBinding
import com.serenegiant.libcommon.list.DummyContent
import com.serenegiant.libcommon.list.DummyContent.DummyItem
import com.serenegiant.libcommon.list.MyItemRecyclerViewAdapter

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnListFragmentInteractionListener]
 * interface.
 */
class TitleFragment: BaseFragment() {

	private lateinit var list: RecyclerView
	private var mColumnCount = 1
	private var mListener: OnListFragmentInteractionListener? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		mListener = if (context is OnListFragmentInteractionListener) {
			context
		} else {
			throw RuntimeException(context.toString()
				+ " must implement OnListFragmentInteractionListener")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (arguments != null) {
			mColumnCount = requireArguments().getInt(ARG_COLUMN_COUNT)
		}
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return FragmentItemListBinding.inflate(inflater, container, false)
		.apply {
			swipeRefresh.setOnRefreshListener {
				// 何もしてないけどしたつもりになってちょっと待ってからプログレス表示を非表示にする
				runOnUiThread({
					swipeRefresh.isRefreshing = false
				}, 1000)
			}
			this@TitleFragment.list = list
			list.run {
				// Set the adapter
				layoutManager = if (mColumnCount <= 1) {
					LinearLayoutManager(context)
				} else {
					GridLayoutManager(context, mColumnCount)
				}
				adapter = MyItemRecyclerViewAdapter(DummyContent.ITEMS, mListener)
			}
		}
		.run {
			root
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		requireActivity().title = getString(R.string.app_name)
	}

	override fun onDetach() {
		mListener = null
		super.onDetach()
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 *
	 *
	 * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
	 */
	interface OnListFragmentInteractionListener {
		fun onListFragmentInteraction(item: DummyItem)
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = TitleFragment::class.java.simpleName
		private const val ARG_COLUMN_COUNT = "column-count"

		fun newInstance(columnCount: Int): TitleFragment {
			val fragment = TitleFragment()
			val args = Bundle()
			args.putInt(ARG_COLUMN_COUNT, columnCount)
			fragment.arguments = args
			return fragment
		}
	}
}
