package com.serenegiant.mediastore;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.utils.ThreadPool;
import com.serenegiant.view.ViewUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

/**
 * MediaStoreの静止画・動画一覧をRecyclerViewで表示するためのRecyclerView.Adapter実装
 * 実データではなくサムネイルを表示する
 * MediaStoreAdapterのRecyclerView.Adapter版
 * XXX サムネイルを取得できなかった項目は表示されなくなる
 * XXX サムネイルを取得できない壊れたファイル等があっても正常に動作するが対象ファイル数が多いと表示されるまでに時間がかかるのでdeprecatedにする
 */
@Deprecated
public class MediaStoreRecyclerAdapter
	extends RecyclerView.Adapter<MediaStoreRecyclerAdapter.ViewHolder> {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreRecyclerAdapter.class.getSimpleName();

	@NonNull
	private final Context mContext;
	@NonNull
	private final LayoutInflater mInflater;
	private final int mLayoutId;
	@NonNull
	private final ContentResolver mCr;
	@NonNull
	private final MyAsyncQueryHandler mQueryHandler;
	@NonNull
	private final ThumbnailCache mThumbnailCache;
	@NonNull
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	@NonNull
	private final MediaInfo info = new MediaInfo();
	/**
	 * 読み込み可能なレコードの位置を保持するList
	 */
	@NonNull
	private final List<Integer> mValues = new ArrayList<>();
	private boolean mNeedValidate;

	private boolean mDataValid;
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
	 * すぐにデータ取得要求する
	 * @param context
	 * @param itemLayout
	 */
	public MediaStoreRecyclerAdapter(@NonNull final Context context,
		@LayoutRes final int itemLayout) {

		this(context, itemLayout, true);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param itemLayout
	 * @param refreshNow true: すぐにデータ取得要求する, false: refreshを呼ぶまでデータ取得しない
	 */
	public MediaStoreRecyclerAdapter(@NonNull final Context context,
		@LayoutRes final int itemLayout, final boolean refreshNow) {

		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mLayoutId = itemLayout;
		mCr = context.getContentResolver();
		mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		mThumbnailCache = new ThumbnailCache(context);
		mNeedValidate = true;

		if (refreshNow) {
			refresh();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			releaseCursor();
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
		setInfo(holder, position, getMediaInfo(position, info));
	}

	@Override
	public int getItemCount() {
		synchronized (mValues) {
			return mValues.size();
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
		ThreadPool.preStartAllCoreThreads();
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
	private MediaInfo getMediaInfo(
		final int position, @Nullable final MediaInfo info) {

		final MediaInfo _info = info != null ? info : new MediaInfo();
		final int pos;
		synchronized (mValues) {
			pos = mValues.get(position);
			if (mCursor == null) {
				throw new IllegalStateException("Cursor is not ready!");
//				mCursor = mCr.query(
//					QUERY_URI, PROJ_MEDIA,
//					mSelection, mSelectionArgs, mSortOrder);
			}
			if (mCursor.moveToPosition(pos)) {
				_info.loadFromCursor(mCursor);
			}
		}
		return _info;
	}

	protected void onContentChanged() {
		mQueryHandler.requery();
	}

	protected void releaseCursor() {
		if (DEBUG) Log.v(TAG, "releaseCursor:");
		final Cursor old = swapCursor(null);
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
	@Nullable
	protected Cursor swapCursor(@Nullable final Cursor newCursor) {
		if (DEBUG) Log.v(TAG, "swapCursor:" + newCursor);
		Cursor oldCursor;
		synchronized (mValues) {
			if (newCursor == mCursor) {
				return null;
			}
			oldCursor = mCursor;
		}
		if (oldCursor != null) {
			if (mChangeObserver != null) {
				oldCursor.unregisterContentObserver(mChangeObserver);
			}
			if (mDataSetObserver != null) {
				oldCursor.unregisterDataSetObserver(mDataSetObserver);
			}
		}
		if (newCursor != null) {
			if (mChangeObserver != null) {
				newCursor.registerContentObserver(mChangeObserver);
			}
			if (mDataSetObserver != null) {
				newCursor.registerDataSetObserver(mDataSetObserver);
			}

			final List<MediaInfo> removes = new ArrayList<>();
			final List<Integer> adds = new ArrayList<>();
			if (newCursor.moveToFirst()) {
				int pos = 0;
				do {
					info.loadFromCursor(newCursor);
					if (DEBUG) Log.v(TAG, "swapCursor:" + info);
					if (!mNeedValidate || info.canRead(mCr)) {
						adds.add(pos);
					} else {
						removes.add(new MediaInfo(info));
					}
					pos++;
				} while (newCursor.moveToNext());
			}
			synchronized (mValues) {
				mValues.clear();
				mValues.addAll(adds);
				mCursor = newCursor;
			}
			mDataValid = true;
			// notify the observers about the new cursor
			notifyDataSetChanged();
			if (false) {
				// 読み込めないのがあれば削除を試みる
				if (!removes.isEmpty()) {
					for (final MediaInfo item: removes) {
						try {
							if(DEBUG) Log.v(TAG, "try to delete " + info);
							mCr.delete(item.getUri(), null, null);
						} catch (final Exception e) {
							Log.w(TAG, "failed to delete " + item, e);
						}
					}
				}
			}
		} else {
			mDataValid = false;
			// notify the observers about the lack of a data set
			synchronized (mValues) {
				mValues.clear();
				mCursor = null;
			}
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
			synchronized (mAdapter.mValues) {
				if (mAdapter.mCursor != null) {
					mAdapter.mCursor.close();
					mAdapter.mCursor = null;
				}
				mAdapter.mSelection = SELECTIONS[mAdapter.mMediaType % MEDIA_TYPE_NUM];
				mAdapter.mSelectionArgs = null;
				startQuery(0, mAdapter, QUERY_URI_FILES, PROJ_MEDIA,
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
	private class MyThumbnailLoaderDrawable extends ThumbnailLoaderDrawable {
		private int mPosition;

		public MyThumbnailLoaderDrawable(@NonNull final Context context,
			final int width, final int height) {

			super(context, width, height);
		}

		public void startLoad(@NonNull final MediaInfo info, final int position) {
			mPosition = position;
			super.startLoad(info);
		}

		@NonNull
		@Override
		protected ThumbnailLoader createLoader() {
			return new MyThumbnailLoader(this);
		}

		@Override
		protected Bitmap checkCache(final long id) {
			return mThumbnailCache.get(id);
		}

		@Override
		protected void setBitmap(@Nullable final Bitmap bitmap) {
			Bitmap _bitmap = bitmap;
			if (_bitmap == null) {
				_bitmap = BitmapHelper.fromDrawable(getContext(), R.drawable.ic_error_outline_red_24dp);
				synchronized (mValues) {
					mValues.remove(mPosition);
					notifyDataSetChanged();
				}
			}
			super.setBitmap(_bitmap);
		}
	}

	/**
	 * ThumbnailLoaderDrawableのための非同期読み込みヘルパークラス
	 */
	private class MyThumbnailLoader extends ThumbnailLoader {
		public MyThumbnailLoader(final MyThumbnailLoaderDrawable parent) {
			super(parent);
		}

		@Nullable
		@Override
		protected Bitmap loadThumbnail(@NonNull final Context context,
			@NonNull final MediaInfo info,
			final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				result = mThumbnailCache.getThumbnail(
					context.getContentResolver(),
					info,
					requestWidth, requestHeight);
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
			}
			return result;
		}
	}

//--------------------------------------------------------------------------------
	private void setInfo(
		@NonNull final ViewHolder holder,
		final int position,
		@NonNull final MediaInfo info) {

//		if (DEBUG) Log.v(TAG, "setInfo:" + info);
		holder.info.set(info);
		// ローカルキャッシュ
		final ImageView iv = holder.mImageView;
		final TextView tv = holder.mTitleView;

		if (iv != null) {
			Drawable drawable = iv.getDrawable();
			if (!(drawable instanceof MyThumbnailLoaderDrawable)) {
				drawable = new MyThumbnailLoaderDrawable(mContext, mThumbnailWidth, mThumbnailHeight);
				iv.setImageDrawable(drawable);
			}
			((MyThumbnailLoaderDrawable)drawable).startLoad(info, position);
		}
		if (tv != null) {
			tv.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
			if (mShowTitle) {
				tv.setText(info.title);
			}
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView mTitleView;
		private final ImageView mImageView;
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
