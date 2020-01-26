package com.serenegiant.mediastore;

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

import java.io.IOException;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import static com.serenegiant.mediastore.MediaStoreUtils.*;

/**
 * MediaStoreの静止画・動画一覧をRecyclerViewで表示するためのRecyclerView.Adapter実装
 */
public class MediaStoreRecyclerAdapter
	extends RecyclerView.Adapter<MediaStoreRecyclerAdapter.ViewHolder> {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreRecyclerAdapter.class.getSimpleName();

	public interface MediaStoreRecyclerAdapterListener {
		public void onItemClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final MediaInfo item);
		public boolean onItemLongClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final MediaInfo item);
	}

	private final LayoutInflater mInflater;
	private final int mLayoutId;
	private final ContentResolver mCr;
	private final MyAsyncQueryHandler mQueryHandler;
	private final ThumbnailCache mThumbnailCache;
	private final int mHashCode = hashCode();
	private final MediaInfo info = new MediaInfo();
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	protected boolean mDataValid;
	protected int mRowIDColumn;
	protected ChangeObserver mChangeObserver;
	protected DataSetObserver mDataSetObserver;
	private Cursor mCursor;
	private String mSelection = SELECTIONS[MEDIA_IMAGE];	// 静止画のみ有効
	private String[] mSelectionArgs = null;
	@Nullable
	private RecyclerView mRecycleView;
	@Nullable
	private MediaStoreRecyclerAdapterListener mListener;
	private boolean mShowTitle = false;
	private int mMediaType = MEDIA_ALL;
	private int mThumbnailWidth = 200, mThumbnailHeight = 200;

	public MediaStoreRecyclerAdapter(@NonNull final Context context,
		@LayoutRes final int itemLayout) {

		super();
		mInflater = LayoutInflater.from(context);
		mLayoutId = itemLayout;
		mCr = context.getContentResolver();
		mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		mThumbnailCache = new ThumbnailCache(context);

		startQuery();
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
		mRecycleView = recyclerView;
	}

	@Override
	public void onDetachedFromRecyclerView(@NonNull final RecyclerView recyclerView) {
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
		getMediaInfo(position, info);
		setInfo(holder, info);
	}

	@Override
	public int getItemCount() {
		if (mDataValid && mCursor != null) {
			return mCursor.getCount();
		} else {
			return 0;
		}
	}

	public MediaInfo getItem(final int position) {
		return getMediaInfo(position, null);
	}

//--------------------------------------------------------------------------------
	public void setListener(final MediaStoreRecyclerAdapterListener listener) {
		mListener = listener;
	}

	public void notifyDataSetInvalidated() {
//		mDataSetObservable.notifyInvalidated();
	}

	public void startQuery() {
		mQueryHandler.requery();
	}

	/**
	 * set thumbnail size, if you set size to zero, the size is 96x96(MediaStore.Images.Thumbnails.MICRO_KIND)
	 * @param size
	 */
	public void setThumbnailSize(final int size) {
		if ((mThumbnailWidth != size) || (mThumbnailHeight != size)) {
			mThumbnailWidth = mThumbnailHeight = size;
			mThumbnailCache.clearThumbnailCache();
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
			mThumbnailCache.clearThumbnailCache();
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

//--------------------------------------------------------------------------------
	public synchronized MediaInfo getMediaInfo(final int position, final MediaInfo info) {
		final MediaInfo _info = info != null ? info : new MediaInfo();

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
		if (mCursor == null) {
			mCursor = mCr.query(
				QUERY_URI, PROJ_MEDIA,
				mSelection, mSelectionArgs, null);
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
		final Cursor old = swapCursor(cursor);
		if ((old != null) && !old.isClosed()) {
			old.close();
		}
	}

	protected Cursor getCursor(final int position) {
		if (mDataValid && mCursor != null) {
			mCursor.moveToPosition(position);
			return mCursor;
		} else {
			return null;
			}
	}

	protected Cursor swapCursor(final Cursor newCursor) {
		if (newCursor == mCursor) {
			return null;
		}
		Cursor oldCursor = mCursor;
		if (oldCursor != null) {
			if (mChangeObserver != null) oldCursor.unregisterContentObserver(mChangeObserver);
			if (mDataSetObserver != null) oldCursor.unregisterDataSetObserver(mDataSetObserver);
		}
		mCursor = newCursor;
		if (newCursor != null) {
			if (mChangeObserver != null) newCursor.registerContentObserver(mChangeObserver);
			if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);
			mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
			mDataValid = true;
			// notify the observers about the new cursor
			notifyDataSetChanged();
		} else {
			mRowIDColumn = -1;
			mDataValid = false;
			// notify the observers about the lack of a data set
			notifyDataSetInvalidated();
		}
		return oldCursor;
	}

	private static final class MyAsyncQueryHandler extends AsyncQueryHandler {
		private final MediaStoreRecyclerAdapter mAdapter;
		public MyAsyncQueryHandler(final ContentResolver cr,
			final MediaStoreRecyclerAdapter adapter) {

			super(cr);
			mAdapter = adapter;
		}

		public void requery() {
			synchronized (mAdapter) {
				startQuery(0, mAdapter, QUERY_URI, PROJ_MEDIA,
					mAdapter.mSelection, mAdapter.mSelectionArgs, null);
			}
		}

		@Override
		protected void onQueryComplete(final int token,
			final Object cookie, final Cursor cursor) {

//			super.onQueryComplete(token, cookie, cursor);	// this is empty method
			final Cursor oldCursor = mAdapter.swapCursor(cursor);
			if ((oldCursor != null) && !oldCursor.isClosed())
				oldCursor.close();
		}

	}

	private class ChangeObserver extends ContentObserver {
		public ChangeObserver() {
			super(new Handler());
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			startQuery();
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
					if (info != null) {
						try {
							mListener.onItemClick(
								MediaStoreRecyclerAdapter.this, v, info);
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					} else if (DEBUG) {
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
				}
			}
			return false;
		}
	};

//--------------------------------------------------------------------------------
	private void setInfo(@NonNull final ViewHolder holder,
		final MediaInfo info) {

		holder.info = info;
		holder.itemView.setTag(R.id.info, info);
		if (holder.mImageView != null) {
			Drawable drawable = holder.mImageView.getDrawable();
			if (!(drawable instanceof LoaderDrawable)) {
				drawable = new ThumbnailLoaderDrawable(mCr, mThumbnailWidth, mThumbnailHeight);
				holder.mImageView.setImageDrawable(drawable);
			}
			((LoaderDrawable)drawable).startLoad(
				mCursor.getInt(PROJ_INDEX_MEDIA_TYPE), mHashCode, mCursor.getLong(PROJ_INDEX_ID));
		}
		if (holder.mTitleView != null) {
			holder.mTitleView.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
			if (mShowTitle) {
				holder.mTitleView.setText(info.title);
			}
		}
	}

	private class ThumbnailLoaderDrawable extends LoaderDrawable {
		public ThumbnailLoaderDrawable(final ContentResolver cr,
			final int width, final int height) {

			super(cr, width, height);
		}

		@Override
		protected ImageLoader createThumbnailLoader() {
			return new ThumbnailLoader(this);
		}

		@Override
		protected Bitmap checkBitmapCache(final int hashCode, final long id) {
			return mThumbnailCache.get(hashCode, id);
		}
	}

	private class ThumbnailLoader extends ImageLoader {
		public ThumbnailLoader(final ThumbnailLoaderDrawable parent) {
			super(parent);
		}

		@Override
		protected Bitmap loadBitmap(@NonNull final ContentResolver cr,
			final int mediaType, final int hashCode, final long id,
			final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				switch (mediaType) {
				case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
					result = mThumbnailCache.getImageThumbnail(cr, hashCode, id, requestWidth, requestHeight);
					break;
				case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
					result = mThumbnailCache.getVideoThumbnail(cr, hashCode, id, requestWidth, requestHeight);
					break;
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			return result;
		}
	}

//--------------------------------------------------------------------------------
	public static class ViewHolder extends RecyclerView.ViewHolder {
		private TextView mTitleView;
		private ImageView mImageView;
		private MediaInfo info;

		public ViewHolder(@NonNull final View v) {
			super(v);
			if (v instanceof TextView) {
				mTitleView = (TextView)v;
				mImageView = null;
			} else if (v instanceof ImageView) {
				mTitleView = null;
				mImageView = (ImageView)v;
			} else {
				mTitleView = v.findViewById(R.id.title);
				mImageView = v.findViewById(R.id.thumbnail);
			}
		}

	}
}
