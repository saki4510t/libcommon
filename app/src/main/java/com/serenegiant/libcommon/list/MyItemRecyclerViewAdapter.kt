package com.serenegiant.libcommon.list
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.libcommon.R
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener
import com.serenegiant.libcommon.list.DummyContent.DummyItem

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyItemRecyclerViewAdapter(
	private val mValues: List<DummyItem>,
	private val mListener: OnListFragmentInteractionListener?)
		: RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.list_item_title, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.mItem = mValues[position]
		holder.mContentView.text = mValues[position].content
		holder.mView.setOnClickListener {
			mListener?.onListFragmentInteraction(holder.mItem!!)
		}
	}

	override fun getItemCount(): Int {
		return mValues.size
	}

	inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
		val mContentView: TextView
		var mItem: DummyItem? = null

		init {
			mContentView = mView.findViewById(R.id.content)
		}

		override fun toString(): String {
			return super.toString() + " '" + mContentView.text + "'"
		}

	}

}