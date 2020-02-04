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

import android.content.Context
import androidx.annotation.ArrayRes
import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object DummyContent {
	/**
	 * An array of sample (dummy) items.
	 */
	@JvmField
	val ITEMS: MutableList<DummyItem> = ArrayList()

	fun createItems(context: Context, @ArrayRes idItems: Int) {
		val items = context.resources.getStringArray(idItems)
		ITEMS.clear()
		var i = 0
		for (item in items) {
			addItem(DummyItem(i++, item, null))
		}
	}

	fun addItem(item: DummyItem) {
		ITEMS.add(item)
	}

	/**
	 * A dummy item representing a piece of content.
	 */
	class DummyItem(val id: Int, val content: String, val details: String?) {

		override fun toString(): String {
			return content
		}

	}
}