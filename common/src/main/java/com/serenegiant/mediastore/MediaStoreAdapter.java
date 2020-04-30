package com.serenegiant.mediastore;
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

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.ThreadPool;
import com.serenegiant.view.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

/**
 * MediaStoreの静止画・動画一覧を取得するためのCursorAdapter実装
 * 実データではなくサムネイルを表示する
 * XXX CursorAdapterの代わりにArrayAdapterから継承したほうがいいかも
 */
public class MediaStoreAdapter extends CursorAdapter {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreAdapter.class.getSimpleName();

	private int mThumbnailWidth = 200, mThumbnailHeight = 200;

	@NonNull
	private final Context mContext;
	@NonNull
	private final LayoutInflater mInflater;
	@NonNull
	private final ContentResolver mCr;
	private final int mLayoutId;
	@NonNull
	private final MyAsyncQueryHandler mQueryHandler;
	@NonNull
	private final ThumbnailCache mThumbnailCache;

	private Cursor mMediaInfoCursor;
	private String mSelection;
	private String[] mSelectionArgs;
	private boolean mShowTitle = false;
	private int mMediaType = MEDIA_ALL;
	@NonNull
	private final MediaInfo info = new MediaInfo();
	/**
	 * 読み込み可能なレコードの位置を保持するList
	 */
	@NonNull
	private final List<Integer> mValues = new ArrayList<>();
	private boolean mNeedValidate;

	/**
	 * コンストラクタ
	 * すぐにデータ取得要求する
	 * @param context
	 * @param id_layout
	 */
	public MediaStoreAdapter(@NonNull final Context context,
		@LayoutRes final int id_layout) {

		this(context, id_layout, true);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param id_layout
	 * @param refreshNow true: すぐにデータ取得要求する, false: refreshを呼ぶまでデータ取得しない
	 */
	public MediaStoreAdapter(@NonNull final Context context,
		@LayoutRes final int id_layout, final boolean refreshNow) {

		super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mContext = context;
	    mInflater = LayoutInflater.from(context);
	    mCr = context.getContentResolver();
	    mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		// getMemoryClass return the available memory amounts for app as mega bytes(API >= 5)
		mLayoutId = id_layout;
		mThumbnailCache = new ThumbnailCache(context);
		mNeedValidate = true;
		if (refreshNow) {
			refresh();
		}
	}

	@Override
	public View newView(final Context context,
		final Cursor cursor, final ViewGroup parent) {

		// this method is called within UI thread and should return as soon as possible
		final View view = mInflater.inflate(mLayoutId, parent, false);
		getViewHolder(view);
		return view;
	}

	protected LoaderDrawable createLoaderDrawable(@NonNull final Context context) {
		return new ThumbnailLoaderDrawable(context, mThumbnailWidth, mThumbnailHeight);
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		// this method is called within UI thread and should return as soon as possible
		final ViewHolder holder = getViewHolder(view);
		// ローカルキャッシュ
		final ImageView iv = holder.mImageView;
		final TextView tv = holder.mTitleView;
		Drawable drawable = iv.getDrawable();
		if (!(drawable instanceof LoaderDrawable)) {
			drawable = createLoaderDrawable(mContext);
			iv.setImageDrawable(drawable);
		}
		((LoaderDrawable)drawable).startLoad(
			cursor.getInt(PROJ_INDEX_MEDIA_TYPE), cursor.getLong(PROJ_INDEX_ID));
		if (tv != null) {
			tv.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
			if (mShowTitle) {
				tv.setText(cursor.getString(PROJ_INDEX_TITLE));
			}
		}
	}

	private ViewHolder getViewHolder(final View view) {
		ViewHolder holder;
		// you can use View#getTag()/setTag() instead of using View#getTag(int)/setTag(int)
		// but we assume that using getTag(int)/setTag(int) and keeping getTag()/setTag() left for user is better.
		holder = (ViewHolder)view.getTag(R.id.mediastorephotoadapter);
		if (holder == null) {
			holder = new ViewHolder();
			holder.mImageView = ViewUtils.findIconView(view);
			holder.mTitleView = ViewUtils.findTitleView(view);
			view.setTag(R.id.mediastorephotoadapter, holder);
		}
		return holder;
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			changeCursor(null);
			if (mMediaInfoCursor != null) {
				mMediaInfoCursor.close();
				mMediaInfoCursor = null;
			}
		} finally {
			super.finalize();
		}
	}

	@Override
	protected void onContentChanged() {
		mQueryHandler.requery();
	}

	public void refresh() {
		ThreadPool.preStartAllCoreThreads();
		onContentChanged();
	}

	/**
	 * @see android.widget.ListAdapter#getCount()
	 */
	@Override
	public int getCount() {
		synchronized (mValues) {
			return mValues.size();
		}
	}

	/**
	 * return thumbnail image at specific position.
	 * this method is synchronously executed and may take time
	 * @return null if the position value is out of range etc. 
	 */
	@Override
	public Bitmap getItem(final int position) {
		Bitmap result = null;

		getMediaInfo(position, info);
		if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
			// 静止画の場合のサムネイル取得
			try {
				result = mThumbnailCache.getImageThumbnail(
					mCr, getItemId(position),
					mThumbnailWidth, mThumbnailHeight);
			} catch (final FileNotFoundException e) {
				Log.w(TAG, e);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		} else if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
			// 動画の場合のサムネイル取得
			try {
				result = mThumbnailCache.getVideoThumbnail(
					mCr, getItemId(position),
					mThumbnailWidth, mThumbnailHeight);
			} catch (final FileNotFoundException e) {
				Log.w(TAG, e);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		if (DEBUG && (result == null)) {
			Log.w(TAG, "failed to getItem(" + info.title + ") at position=" + position);
		}
		return result;
	}

	/**
	 * 読み込みできないデータをオミットするかどうか
	 * 次回のrefresh呼び出しから有効
	 * @param needValidate
	 */
	public void setValidateItems(final boolean needValidate) {
		if (mNeedValidate != needValidate) {
			mNeedValidate = needValidate;
		}
	}

	public int getPositionFromId(final long id) {
		int result = -1;
		final int n = getCount();
		final MediaInfo info = new MediaInfo();
		for (int i = 0; i < n; i++) {
			getMediaInfo(i, info);
			if (info.id == id) {
				result = i;
				break;
			}
		}
		return result;
	}

	/**
	 * return image with specific size(only scale-down or original size are available now)
	 * if width=0 and height=0, return image with original size.
	 * this method is synchronously executed and may take time
	 * @return null if the position value is out of range etc. 
	 * @param position
	 * @param width
	 * @param height
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap getImage(final int position, final int width, final int height)
		throws FileNotFoundException, IOException {

		return BitmapHelper.asBitmap(mCr, getItemId(position), width, height);
	}
	
	/**
	 * get MediaInfo at specified position
	 * @param position
	 * @return
	 */
	@NonNull
	public MediaInfo getMediaInfo(final int position) {
		return getMediaInfo(position, null);
	}

	/**
	 * get MediaInfo at specified position
	 * @param position
	 * @param info
	 * @return
	 */
	@NonNull
	public synchronized MediaInfo getMediaInfo(
		final int position, @Nullable final MediaInfo info) {

		final MediaInfo _info = info != null ? info : new MediaInfo();
		final int pos;
		synchronized (mValues) {
			pos = mValues.get(position);
		}

/*		// if you don't need to frequently call this method, temporary query may be better to reduce memory usage.
		// but it will take more time.
		final Cursor cursor = mCr.query(
			ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, getItemId(position)),
			PROJ_IMAGE, mSelection, mSelectionArgs, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					info = readMediaInfo(cursor, new MediaInfo());
				}
			} finally {
				cursor.close();
			}
		} */
		if (mMediaInfoCursor == null) {
			throw new IllegalStateException("Cursor is not ready!");
//			mMediaInfoCursor = mCr.query(
//				QUERY_URI, PROJ_MEDIA,
//				mSelection, mSelectionArgs, null);
		}
		if (mMediaInfoCursor.moveToPosition(pos)) {
			_info.loadFromCursor(mMediaInfoCursor);
		}
		return _info;
	}

	/**
	 * set thumbnail size, if you set size to zero, the size is 96x96(MediaStore.Images.Thumbnails.MICRO_KIND)
	 * @param size
	 */
	public void setThumbnailSize(final int size) {
		if ((mThumbnailWidth != size) || (mThumbnailHeight != size)) {
			mThumbnailWidth = mThumbnailHeight = size;
			mThumbnailCache.clear();
			onContentChanged();
		}
	}

	/**
	 * set thumbnail size, if you set both width and height to zero, the size is 96x96(MediaStore.Images.Thumbnails.MICRO_KIND)
	 * @param width
	 * @param height
	 */
	public void setThumbnailSize(final int width, final int height) {
		if ((mThumbnailWidth != width) || (mThumbnailHeight != height)) {
			mThumbnailWidth = width;
			mThumbnailHeight = height;
			mThumbnailCache.clear();
			onContentChanged();
		}
	}

	public void setShowTitle(final boolean showTitle) {
		if (mShowTitle != showTitle) {
			mShowTitle = showTitle;
			onContentChanged();
		}
	}
	
	public boolean getShowTitle() {
		return mShowTitle;
	}

	public int getMediaType() {
		return mMediaType % MEDIA_TYPE_NUM;
	}
	
	public void setMediaType(final int media_type) {
		if (mMediaType != (media_type % MEDIA_TYPE_NUM)) {
			mMediaType = media_type % MEDIA_TYPE_NUM;
			onContentChanged();
		}
	}

	@Nullable
	@Override
 	public Cursor swapCursor(@Nullable final Cursor newCursor) {
 		if (newCursor != null) {
			synchronized (mValues) {
				mValues.clear();
				if (newCursor.moveToFirst()) {
					int pos = 0;
					while (newCursor.moveToNext()) {
						info.loadFromCursor(newCursor);
						if (!mNeedValidate || info.canRead(mCr)) {
							mValues.add(pos);
						}
						pos++;
					}
				}
			}
		} else {
			synchronized (mValues) {
				mValues.clear();
			}
		}
		return super.swapCursor(newCursor);
	}

	private static final class MyAsyncQueryHandler extends AsyncQueryHandler {
		private final MediaStoreAdapter mAdapter;
		public MyAsyncQueryHandler(final ContentResolver cr, final MediaStoreAdapter adapter) {
			super(cr);
			mAdapter = adapter;
		}
		
		public void requery() {
			synchronized (mAdapter) {
				if (mAdapter.mMediaInfoCursor != null) {
					mAdapter.mMediaInfoCursor.close();
					mAdapter.mMediaInfoCursor = null;
				}
				mAdapter.mSelection = SELECTIONS[mAdapter.mMediaType % MEDIA_TYPE_NUM];
				mAdapter.mSelectionArgs = null;
				startQuery(0, mAdapter, QUERY_URI, PROJ_MEDIA,
					mAdapter.mSelection, mAdapter.mSelectionArgs, null);
			}
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
//			super.onQueryComplete(token, cookie, cursor);	// this is empty method
			final Cursor oldCursor = mAdapter.swapCursor(cursor);
			if ((oldCursor != null) && !oldCursor.isClosed())
				oldCursor.close();
		}

	}
	
	private static final class ViewHolder {
		TextView mTitleView;
		ImageView mImageView;
	}

	private class ThumbnailLoaderDrawable extends LoaderDrawable {
		public ThumbnailLoaderDrawable(@NonNull final Context context,
			final int width, final int height) {

			super(context, width, height);
		}

		@NonNull
		@Override
		protected ImageLoader createImageLoader() {
			return new ThumbnailLoader(this);
		}

		@Override
		protected Bitmap checkCache(final long id) {
			return mThumbnailCache.get(id);
		}
	}

	private class ThumbnailLoader extends ImageLoader {
		public ThumbnailLoader(final ThumbnailLoaderDrawable parent) {
			super(parent);
		}

		@Override
		protected Bitmap loadBitmap(@NonNull final ContentResolver cr,
			final int mediaType, final long id,
			final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				switch (mediaType) {
				case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
					result = mThumbnailCache.getImageThumbnail(cr, id, requestWidth, requestHeight);
					break;
				case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
					result = mThumbnailCache.getVideoThumbnail(cr, id, requestWidth, requestHeight);
					break;
				}
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
				result = loadDefaultBitmap(R.drawable.ic_error_outline_red_24dp);
			}
			return result;
		}
	}
}
