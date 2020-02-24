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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener
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
			mColumnCount = arguments!!.getInt(ARG_COLUMN_COUNT)
		}
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		val view = inflater.inflate(R.layout.fragment_item_list, container, false)
		// Set the adapter
		if (view is RecyclerView) {
			val context = view.getContext()
			val recyclerView = view
			if (mColumnCount <= 1) {
				recyclerView.layoutManager = LinearLayoutManager(context)
			} else {
				recyclerView.layoutManager = GridLayoutManager(context, mColumnCount)
			}
			recyclerView.adapter = MyItemRecyclerViewAdapter(DummyContent.ITEMS, mListener)
		}
		return view
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