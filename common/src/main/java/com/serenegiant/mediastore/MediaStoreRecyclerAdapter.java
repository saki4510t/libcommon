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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.utils.ThreadPool;
import com.serenegiant.view.ViewUtils;

import java.io.IOException;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

/**
 * MediaStoreの静止画・動画一覧をRecyclerViewで表示するためのRecyclerView.Adapter実装
 * 実データではなくサムネイルを表示する
 * MediaStoreAdapterのRecyclerView.Adapter版
 */
public class MediaStoreRecyclerAdapter
	extends RecyclerView.Adapter<MediaStoreRecyclerAdapter.ViewHolder> {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreRecyclerAdapter.class.getSimpleName();

	/**
	 * MediaStoreRecyclerAdapterでアイテムを選択したときのコールバックリスナー
	 */
	public interface MediaStoreRecyclerAdapterListener {
		/**
		 * アイテムをクリックした
		 * @param parent
		 * @param view
		 * @param item
		 */
		public void onItemClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final MediaInfo item);

		/**
		 * アイテムを長押しした
		 * @param parent
		 * @param view
		 * @param item
		 * @return
		 */
		public boolean onItemLongClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final MediaInfo item);
	}

	private final Context mContext;
	private final LayoutInflater mInflater;
	private final int mLayoutId;
	private final ContentResolver mCr;
	private final MyAsyncQueryHandler mQueryHandler;
	private final ThumbnailCache mThumbnailCache;
	private final MediaInfo info = new MediaInfo();
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());

	private boolean mDataValid;
//	private int mRowIDColumn;
	private ChangeObserver mChangeObserver;
	private DataSetObserver mDataSetObserver;
	private Cursor mCursor;
	private String mSelection;
	private String[] mSelectionArgs = null;
	private String mSortOrder = null;
	@Nullable
	private RecyclerView mRecycleView;
	@Nullable
	private MediaStoreRecyclerAdapterListener mListener;
	private boolean mShowTitle = false;
	private int mMediaType = MEDIA_ALL;
	private int mThumbnailWidth = 200, mThumbnailHeight = 200;

	/**
	 * コンストラクタ
	 * @param context
	 * @param itemLayout
	 */
	public MediaStoreRecyclerAdapter(@NonNull final Context context,
		@LayoutRes final int itemLayout) {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mLayoutId = itemLayout;
		mCr = context.getContentResolver();
		mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		mThumbnailCache = new ThumbnailCache(context);

		ThreadPool.preStartAllCoreThreads();
		refresh();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			changeCursor(null);
		} finally {
			super.finalize();
		}
	}

	@Override
	public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
		if (DEBUG) Log.v(TAG, "onAttachedToRecyclerView:");
		mRecycleView = recyclerView;
	}

	@Override
	public void onDetachedFromRecyclerView(@NonNull final RecyclerView recyclerView) {
		if (DEBUG) Log.v(TAG, "onDetachedFromRecyclerView:");
		mRecycleView = null;
		super.onDetachedFromRecyclerView(recyclerView);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final View view = mInflater.inflate(mLayoutId, parent, false);
		view.setOnClickListener(mOnClickListener);
		view.setOnLongClickListener(mOnLongClickListener);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		setInfo(holder, getMediaInfo(position, info));
	}

	@Override
	public int getItemCount() {
		if (mDataValid && mCursor != null) {
			return mCursor.getCount();
		} else {
			return 0;
		}
	}

	/**
	 * 指定したpositionにあるデータを保持したMediaInfoを取得する
	 * @param position
	 * @return
	 */
	@NonNull
	public MediaInfo getItem(final int position) {
		return getMediaInfo(position, null);
	}

//--------------------------------------------------------------------------------
	public void setListener(final MediaStoreRecyclerAdapterListener listener) {
		if (DEBUG) Log.v(TAG, "setListener:" + listener);
		mListener = listener;
	}

	public void notifyDataSetInvalidated() {
		if (DEBUG) Log.v(TAG, "notifyDataSetInvalidated:");
//		mDataSetObservable.notifyInvalidated();
	}

	public void refresh() {
		if (DEBUG) Log.v(TAG, "refresh:");
		mQueryHandler.requery();
	}

	/**
	 * サムネイルのサイズを設定
	 * 0を指定したときは96x96(MediaStore.Images.Thumbnails.MICRO_KIND)になる
	 * @param size
	 */
	public void setThumbnailSize(final int size) {
		setThumbnailSize(size, size);
	}

	/**
	 * サムネイルのサイズを設定
	 * 0を指定したときは96x96(MediaStore.Images.Thumbnails.MICRO_KIND)になる
	 * @param width
	 * @param height
	 */
	public void setThumbnailSize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("setThumbnailSize:(%dx%d)", width, height));
		if ((mThumbnailWidth != width) || (mThumbnailHeight != height)) {
			mThumbnailWidth = width;
			mThumbnailHeight = height;
			mThumbnailCache.clear();
			onContentChanged();
		}
	}

	/**
	 * タイトルを表示するかどうかを設定
	 * @param showTitle
	 */
	public void setShowTitle(final boolean showTitle) {
		if (DEBUG) Log.v(TAG, "setShowTitle:" + showTitle);
		if (mShowTitle != showTitle) {
			mShowTitle = showTitle;
			onContentChanged();
		}
	}

	/**
	 * タイトルを表示するかどうかを取得
	 * @return
	 */
	public boolean getShowTitle() {
		return mShowTitle;
	}

	/**
	 * 表示するメディ他の種類を取得
	 * @return
	 */
	public int getMediaType() {
		return mMediaType % MEDIA_TYPE_NUM;
	}

	/**
	 * 表示するメディアの種類を設定
	 * @param media_type
	 */
	public void setMediaType(final int media_type) {
		if (mMediaType != (media_type % MEDIA_TYPE_NUM)) {
			mMediaType = media_type % MEDIA_TYPE_NUM;
			onContentChanged();
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * get MediaInfo at specified position
	 * @param position
	 * @param info
	 * @return
	 */
	@NonNull
	private synchronized MediaInfo getMediaInfo(
		final int position, @Nullable final MediaInfo info) {

		final MediaInfo _info = info != null ? info : new MediaInfo();
		if (mCursor == null) {
			mCursor = mCr.query(
				QUERY_URI, PROJ_MEDIA,
				mSelection, mSelectionArgs, mSortOrder);
		}
		if (mCursor.moveToPosition(position)) {
			_info.loadFromCursor(mCursor);
		}
		return _info;
	}

	protected void onContentChanged() {
		mQueryHandler.requery();
	}

	protected void changeCursor(@Nullable final Cursor cursor) {
		if (DEBUG) Log.v(TAG, "changeCursor:" + cursor);
		final Cursor old = swapCursor(cursor);
		if ((old != null) && !old.isClosed()) {
			old.close();
		}
	}

	/**
	 * 指定したpositionを示すCursorら返す
	 * @param position
	 * @return
	 */
	@Nullable
	protected Cursor getCursor(final int position) {
		if (mDataValid && mCursor != null) {
			mCursor.moveToPosition(position);
			return mCursor;
		} else {
			return null;
		}
	}

	/**
	 * カーソルを交換
	 * @param newCursor
	 * @return
	 */
	protected Cursor swapCursor(final Cursor newCursor) {
		if (DEBUG) Log.v(TAG, "swapCursor:" + newCursor);
		if (newCursor == mCursor) {
			return null;
		}
		Cursor oldCursor = mCursor;
		if (oldCursor != null) {
			if (mChangeObserver != null) {
				oldCursor.unregisterContentObserver(mChangeObserver);
			}
			if (mDataSetObserver != null) {
				oldCursor.unregisterDataSetObserver(mDataSetObserver);
			}
		}
		mCursor = newCursor;
		if (newCursor != null) {
			if (mChangeObserver != null) {
				newCursor.registerContentObserver(mChangeObserver);
			}
			if (mDataSetObserver != null) {
				newCursor.registerDataSetObserver(mDataSetObserver);
			}
//			mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
			mDataValid = true;
			// notify the observers about the new cursor
			notifyDataSetChanged();
		} else {
//			mRowIDColumn = -1;
			mDataValid = false;
			// notify the observers about the lack of a data set
			notifyDataSetInvalidated();
		}
		return oldCursor;
	}

	/**
	 * ContentResolverへ非同期で問い合わせを行うためのAsyncQueryHandler実装
	 */
	private static final class MyAsyncQueryHandler extends AsyncQueryHandler {
		@NonNull
		private final MediaStoreRecyclerAdapter mAdapter;

		/**
		 * コンストラクタ
		 * @param cr
		 * @param adapter
		 */
		public MyAsyncQueryHandler(
			@NonNull final ContentResolver cr,
			@NonNull final MediaStoreRecyclerAdapter adapter) {

			super(cr);
			if (DEBUG) Log.v(TAG, "MyAsyncQueryHandler:");
			mAdapter = adapter;
		}

		public void requery() {
			synchronized (mAdapter) {
				if (mAdapter.mCursor != null) {
					mAdapter.mCursor.close();
					mAdapter.mCursor = null;
				}
				mAdapter.mSelection = SELECTIONS[mAdapter.mMediaType % MEDIA_TYPE_NUM];
				mAdapter.mSelectionArgs = null;
				startQuery(0, mAdapter, QUERY_URI, PROJ_MEDIA,
					mAdapter.mSelection, mAdapter.mSelectionArgs, mAdapter.mSortOrder);
			}
		}

		@Override
		protected void onQueryComplete(final int token,
			final Object cookie, final Cursor cursor) {

			super.onQueryComplete(token, cookie, cursor);	// this is empty method
			if (DEBUG) Log.v(TAG, "MyAsyncQueryHandler#onQueryComplete:");
			final Cursor oldCursor = mAdapter.swapCursor(cursor);
			if ((oldCursor != null) && !oldCursor.isClosed())
				oldCursor.close();
		}

	}	// MyAsyncQueryHandler

	private class ChangeObserver extends ContentObserver {
		public ChangeObserver() {
			super(mUIHandler);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			refresh();
		}
	}

	private class MyDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			mDataValid = true;
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			mDataValid = false;
			notifyDataSetInvalidated();
		}
	}

	protected final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (DEBUG) Log.v(TAG, "onClick:" + v);
			if ((mRecycleView != null) && mRecycleView.isEnabled()) {
				if (v instanceof Checkable) {
					((Checkable)v).setChecked(true);
					mUIHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							((Checkable)v).setChecked(false);
						}
					}, 100);
				}
				if (mListener != null) {
					final MediaInfo info = (MediaInfo) v.getTag(R.id.info);
					if (DEBUG) Log.v(TAG, "onClick:info=" + info);
					if (info != null) {
						try {
							mListener.onItemClick(
								MediaStoreRecyclerAdapter.this, v, info);
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					} else if (DEBUG) {
						// ここにくるのはおかしい
						Log.d(TAG, "MediaInfo not attached!");
					}
				}
			} else {
				Log.w(TAG, "onClick:mRecycleView=" + mRecycleView);
			}
		}
	};

	protected final View.OnLongClickListener mOnLongClickListener
		= new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(final View v) {
			if (DEBUG) Log.v(TAG, "onLongClick:" + v);
			if (((mRecycleView != null) && mRecycleView.isEnabled())
				&& (mListener != null)) {

				final MediaInfo info = (MediaInfo) v.getTag(R.id.info);
				if (info != null) {
					try {
						return mListener.onItemLongClick(
							MediaStoreRecyclerAdapter.this, v, info);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				} else if (DEBUG) {
					// ここにくるのはおかしい
					Log.d(TAG, "MediaInfo not attached!");
				}
			}
			return false;
		}
	};

//--------------------------------------------------------------------------------

	/**
	 * サムネイルを非同期で取得するためのDrawable
	 */
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

	/**
	 * ThumbnailLoaderDrawableのための非同期読み込みヘルパークラス
	 */
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
					result = mThumbnailCache.getImageThumbnail(cr, id,
						requestWidth, requestHeight);
					break;
				case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
					result = mThumbnailCache.getVideoThumbnail(cr, id,
						requestWidth, requestHeight);
					break;
				}
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
				result = loadDefaultBitmap(R.drawable.ic_error_outline_red_24dp);
			}
			return result;
		}
	}

//--------------------------------------------------------------------------------
	private void setInfo(
		@NonNull final ViewHolder holder,
		@NonNull final MediaInfo info) {

//		if (DEBUG) Log.v(TAG, "setInfo:" + info);
		holder.info.set(info);
		// ローカルキャッシュ
		final ImageView iv = holder.mImageView;
		final TextView tv = holder.mTitleView;

		if (iv != null) {
			Drawable drawable = iv.getDrawable();
			if (!(drawable instanceof LoaderDrawable)) {
				drawable = new ThumbnailLoaderDrawable(mContext, mThumbnailWidth, mThumbnailHeight);
				iv.setImageDrawable(drawable);
			}
			((LoaderDrawable)drawable).startLoad(
				mCursor.getInt(PROJ_INDEX_MEDIA_TYPE), mCursor.getLong(PROJ_INDEX_ID));
		}
		if (tv != null) {
			tv.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
			if (mShowTitle) {
				tv.setText(info.title);
			}
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		private TextView mTitleView;
		private ImageView mImageView;
		@NonNull
		private final MediaInfo info = new MediaInfo();

		public ViewHolder(@NonNull final View v) {
			super(v);
			v.setTag(R.id.info, info);
			mImageView = ViewUtils.findIconView(v);
			mTitleView = ViewUtils.findTitleView(v);
		}

	}
}
