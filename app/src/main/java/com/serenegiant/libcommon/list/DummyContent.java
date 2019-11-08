package com.serenegiant.libcommon.list;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import android.content.Context;
import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {
	
	/**
	 * An array of sample (dummy) items.
	 */
	public static final List<DummyItem> ITEMS = new ArrayList<DummyItem>();
	
	public static void createItems(@NonNull final Context context, @ArrayRes final int idItems) {
		final String[] items = context.getResources().getStringArray(idItems);
		
		ITEMS.clear();
		int i = 0;
		for (final String item: items) {
			addItem(new DummyItem(i++, item, null));
		}
	}

	public static void addItem(final DummyItem item) {
		ITEMS.add(item);
	}
	
	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public final int id;
		public final String content;
		public final String details;
		
		public DummyItem(final int id, final String content, final String details) {
			this.id = id;
			this.content = content;
			this.details = details;
		}
		
		@Override
		public String toString() {
			return content;
		}
	}
}
