package com.serenegiant.db;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ArrayListで複数のCursorを保持して1つのCursorとして扱うためのオレオレMergeCursor実装
 */
public class MergeCursor extends AbstractCursor {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = MergeCursor.class.getSimpleName();

	@NonNull
	private final ArrayList<Cursor> mCursors;
	private Cursor mCursor; // updated in onMove
	private int mIndex;
	private DataSetObserver mObserver;

	/**
	 * コンストラクタ
	 * @param cursors
	 */
	public MergeCursor(@Nullable final ArrayList<Cursor> cursors) {
		mCursors = cursors != null ? cursors : new ArrayList<>();
		mCursor = !mCursors.isEmpty() ? mCursors.get(0) : null;
		mObserver = mDefaultObserver;
		registerDataSetObserver();
	}

	@Override
	public int getCount() {
		int count = 0;
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					count += cursor.getCount();
				}
			}
		}
		return count;
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		synchronized (mCursors) {
			/* Find the right cursor */
			mCursor = null;
			mIndex = -1;
			int cursorStartPos = 0;
			int index = -1;
			for (final Cursor cursor: mCursors) {
				index++;
				if (cursor == null) {
					continue;
				}
				if (newPosition < (cursorStartPos + cursor.getCount())) {
					mCursor = cursor;
					mIndex = index;
					break;
				}

				cursorStartPos += cursor.getCount();
			}
			/* Move it to the right position */
			if (mCursor != null) {
				boolean ret = mCursor.moveToPosition(newPosition - cursorStartPos);
				return ret;
			}
		}
		return false;
	}

	@Override
	public String getString(int column) {
		synchronized (mCursors) {
			return mCursor.getString(column);
		}
	}

	@Override
	public short getShort(int column) {
		synchronized (mCursors) {
			return mCursor.getShort(column);
		}
	}

	@Override
	public int getInt(int column) {
		synchronized (mCursors) {
			return mCursor.getInt(column);
		}
	}

	@Override
	public long getLong(int column) {
		synchronized (mCursors) {
			return mCursor.getLong(column);
		}
	}

	@Override
	public float getFloat(int column) {
		synchronized (mCursors) {
			return mCursor.getFloat(column);
		}
	}

	@Override
	public double getDouble(int column) {
		synchronized (mCursors) {
			return mCursor.getDouble(column);
		}
	}

	@Override
	public int getType(int column) {
		synchronized (mCursors) {
			return mCursor.getType(column);
		}
	}

	@Override
	public boolean isNull(int column) {
		synchronized (mCursors) {
			return mCursor.isNull(column);
		}
	}

	@Override
	public byte[] getBlob(int column) {
		synchronized (mCursors) {
			return mCursor.getBlob(column);
		}
	}

	@Override
	public String[] getColumnNames() {
		synchronized (mCursors) {
			if (mCursor != null) {
				return mCursor.getColumnNames();
			} else {
				return new String[0];
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void deactivate() {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					cursor.deactivate();
				}
			}
		}
		super.deactivate();
	}

	@Override
	public void close() {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if ((cursor != null) && !cursor.isClosed()) {
					cursor.close();
				}
			}
			mCursors.clear();
		}
		super.close();
	}

	@Override
	public void registerContentObserver(final ContentObserver observer) {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					cursor.registerContentObserver(observer);
				}
			}
		}
	}

	@Override
	public void unregisterContentObserver(final ContentObserver observer) {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					cursor.unregisterContentObserver(observer);
				}
			}
		}
	}

	@Override
	public void registerDataSetObserver(final DataSetObserver observer) {
		mObserver = observer;
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					cursor.registerDataSetObserver(observer);
				}
			}
		}
	}

	@Override
	public void unregisterDataSetObserver(final DataSetObserver observer) {
		mObserver = mDefaultObserver;
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor != null) {
					cursor.unregisterDataSetObserver(observer);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean requery() {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor == null) {
					continue;
				}
				if (!cursor.requery()) {
					return false;
				}

			}
		}
		return true;
	}

//--------------------------------------------------------------------------------
	/**
	 * 現在の選択されているCursorのインデックスを返す
	 * Cursorが選択されていなければ-1
	 * @return
	 */
	public int getCurrentIndex() {
		synchronized (mCursors) {
			return mIndex;
		}
	}

	/**
	 * 現在選択されているCursorを取得する
	 * 選択されていなければnullを返す
	 * @return
	 */
	@Nullable
	public Cursor getCurrentCursor() {
		synchronized (mCursors) {
			return mCursor;
		}
	}

	/**
	 * 保持しているCursorの数を取得
	 * @return
	 */
	public int getCursorCount() {
		synchronized (mCursors) {
			return mCursors.size();
		}
	}

	/**
	 * 指定したインデックスに体操するCursorを取得
	 * インデックスが範囲外のときはnullを返す
	 * @param index
	 * @return
	 */
	@Nullable
	public Cursor getCursor(final int index) {
		synchronized (mCursors) {
			return ((index >= 0) && (index < mCursors.size()))
				? mCursors.get(index) : null;
		}
	}

	/**
	 * 保持しているCursorリストの最後に指定したCursorを追加する
	 * @param cursor
	 */
	public void add(@NonNull final Cursor cursor) {
		synchronized (mCursors) {
			mCursors.add(cursor);
			cursor.registerDataSetObserver(mObserver);
		}
	}

	/**
	 * 指定したインデックスが示す位置に指定したCursorをセットする
	 * 保持しているCursorの数がインデックスより少ない場合は空いている箇所にnullがセットされる
	 * @param index
	 * @param cursor
	 * @return
	 */
	@Nullable
	public Cursor add(final int index, @NonNull final Cursor cursor) {
		synchronized (mCursors) {
			if (mCursors.size() <= index) {
				mCursors.ensureCapacity(index + 1);
				for (int i = mCursors.size(); i <= index; i++) {
					mCursors.add(null);
				}
			}
			final Cursor oldCursor = getCursor(index);
			mCursors.add(index, cursor);
			cursor.registerDataSetObserver(mObserver);
			if (oldCursor != null) {
				oldCursor.unregisterDataSetObserver(mObserver);
			}
			return oldCursor;
		}
	}

	/**
	 * 指定したCursorを削除する
	 * @param cursor
	 */
	public void remove(@NonNull final Cursor cursor) {
		synchronized (mCursors) {
			cursor.unregisterDataSetObserver(mObserver);
			mCursors.remove(cursor);
		}
	}

	/**
	 * 指定したインデックスの位置にセットされているCursorを削除する
	 * @param index
	 * @return
	 */
	@Nullable
	public Cursor remove(final int index) {
		synchronized (mCursors) {
			final Cursor cursor = mCursors.remove(index);
			if (cursor != null) {
				cursor.unregisterDataSetObserver(mObserver);
			}
			return cursor;
		}
	}

	private void registerDataSetObserver() {
		synchronized (mCursors) {
			for (final Cursor cursor: mCursors) {
				if (cursor == null) continue;

				cursor.registerDataSetObserver(mObserver);
			}
		}
	}

	private final DataSetObserver mDefaultObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			// Reset our position so the optimizations in move-related code
			// don't screw us over
			mPos = -1;
		}

		@Override
		public void onInvalidated() {
			mPos = -1;
		}
	};

}
