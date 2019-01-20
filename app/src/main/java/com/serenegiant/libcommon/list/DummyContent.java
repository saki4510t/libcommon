package com.serenegiant.libcommon.list;

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
