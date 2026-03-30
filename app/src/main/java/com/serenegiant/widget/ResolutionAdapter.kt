package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.serenegiant.camera.CameraSize
import com.serenegiant.libcommon.R
import java.lang.Exception

/**
 * 解像度選択のためのArrayAdapter実装
 */
class ResolutionAdapter(context: Context) : ArrayAdapter<CameraSize>(context, 0) {
	private class ViewHolder {
		var titleTv: TextView? = null
	}

	private val mInflater = LayoutInflater.from(context)

	override fun getView(
		position: Int, convertView: View?,
		parent: ViewGroup): View {
		var result = convertView
		if (result == null) {
			result = mInflater.inflate(R.layout.list_item_resolution, parent, false)
		}
		if (DEBUG) Log.i(TAG, "result=$result,tag=${result?.tag}")
		var holder = result!!.getTag(R.id.view_holder) as ViewHolder?
		if (holder == null) {
			holder = ViewHolder()
			holder.titleTv = result.findViewById(R.id.title)
			result.setTag(R.id.view_holder, holder)
		}
		try {
			val item = getItem(position)
			if ((item != null) && (holder.titleTv != null)) {
				holder.titleTv!!.text = item.toShortString()
			}
		} catch (e: Exception) {
			if (DEBUG) Log.w(TAG, e)
		}
		return result
	}

	override fun getDropDownView(
		position: Int, convertView: View?,
		parent: ViewGroup): View {
		return getView(position, convertView, parent)
	}

	/**
	 * 既存のデータをクリアして指定した新しい要素に置き換える
	 * @param collection
	 */
	fun replaceAll(collection: Collection<CameraSize>) {
		if (DEBUG) Log.v(TAG, "replaceAll:")
		setNotifyOnChange(false)
		try {
			clear()
			addAll(collection)
		} finally {
			notifyDataSetChanged()
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = ResolutionAdapter::class.java.simpleName
	}
}
