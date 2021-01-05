package com.serenegiant.widget;
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.system.SAFUtils;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.view.ViewUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

/**
 * DocumentFileで指定したディレクトリ以下を一覧表示するためのRecyclerView.Adapter実装
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DocumentTreeRecyclerAdapter
	extends RecyclerView.Adapter<DocumentTreeRecyclerAdapter.ViewHolder> {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = DocumentTreeRecyclerAdapter.class.getSimpleName();

	private static final long DELAY_MILLIS = 100L;

	/**
	 * リストの項目をクリック・ロングクリックしたときのコールバックリスナー
	 */
	public interface DocumentTreeRecyclerAdapterListener {
		public void onItemClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final DocumentFile item);
		public boolean onItemLongClick(@NonNull RecyclerView.Adapter<?> parent,
			@NonNull View view, @NonNull final DocumentFile item);
	}

//--------------------------------------------------------------------------------
@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final Context mContext;
	private final int mLayoutRes;
	@NonNull
	private final DocumentFile mRoot;
	@NonNull
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	/**
	 * 現在のディレクトリ内のファイルリスト
	 */
	@NonNull
	private final List<FileInfo> mItems = new ArrayList<>();
	/**
	 * ディレクトリ切替時の作業用FileInfoリスト
	 */
	@NonNull
	private final List<FileInfo> mWork = new ArrayList<>();

	@Nullable
	private LayoutInflater mLayoutInflater;
	/**
	 * 親となるRecyclerView
	 * onAttachedToRecyclerViewが呼ばれてからonDetachedFromRecyclerViewが
	 * 呼ばれるまではNonNull
	 */
	@Nullable
	private RecyclerView mRecycleView;
	/**
	 * リスト項目をクリック・ロングクリックしたときの用コールバックリスナー
	 */
	@Nullable
	private DocumentTreeRecyclerAdapterListener mListener;

	@Nullable
	private Handler mAsyncHandler;

	@Nullable
	private DocumentFile mCurrentDir;
	@Nullable
	private String[] mExtFilter = null;							// 拡張子選択フィルター文字列, '.'なしの拡張子のみの配列

	/**
	 * コンストラクタ
	 * @param context
	 * @param layoutResId
	 * @param root ディレクトリを示すDocumentFile
	 */
	public DocumentTreeRecyclerAdapter(
		@NonNull final Context context,
		@LayoutRes final int layoutResId,
		@NonNull final DocumentFile root) {

		super();
		if (root == null || !root.isDirectory()) {
			throw new IllegalArgumentException("root should be a directory!");
		}
		mContext = context;
		mLayoutRes = layoutResId;
		mRoot = root;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		internalChangeDir(root);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
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
	public final ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		if (mLayoutInflater == null) {
			mLayoutInflater = LayoutInflater.from(mContext);
		}
		final View view = mLayoutInflater.inflate(mLayoutRes, parent, false);
		view.setOnClickListener(mOnClickListener);
		view.setOnLongClickListener(mOnLongClickListener);
		return onCreateViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		holder.setItem(position, getFileInfo(position));
	}

	@Override
	public int getItemCount() {
		return mItems.size();
	}

	/**
	 * 指定した位置に対応するDocumentFileを取得
	 * @param position
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public DocumentFile getItem(final int position)
		throws IndexOutOfBoundsException {

		return getFileInfo(position).getFile();
	}

	/**
	 * リスト項目をクリック・ロングクリックしたときの用コールバックリスナーを登録
	 * @param listener
	 */
	public void setListener(@Nullable final DocumentTreeRecyclerAdapterListener listener) {
		if (DEBUG) Log.v(TAG, "setListener:" + listener);
		mListener = listener;
	}

	/**
	 * 指定したディレクトリへ切り替え
	 * コンストラクタのroot引数で指定したディレクトリorその下位ディレクトリを指定すること
	 * @param newDir
	 * @throws IOException
	 */
	public void changeDir(@NonNull final DocumentFile newDir) throws IOException {
		if (DEBUG) Log.v(TAG, "changeDir:newDir=" + newDir.getUri());
		if (newDir.isDirectory()) {
			internalChangeDir(newDir);
			notifyDataSetChanged();
		} else {
			throw new IOException(String.format(Locale.US, "%s is not a directory/could not read",
				newDir.getUri()));
		}
	}

	/**
	 * 拡張子でフィルタリング
	 * セミコロンまたは空白で区切った拡張子文字列(.無し)
	 * @param extString should separate with semicolon or space
	 */
	public void setExtFilter(@Nullable final String extString) {
		if (!TextUtils.isEmpty(extString)) {
			final Pattern p = Pattern.compile("[;\\s]+");	// セミコロンor空白で区切る
			mExtFilter = p.split(extString.toLowerCase(Locale.getDefault()));
		} else {
			mExtFilter = null;
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 関係するリソースを破棄する
	 * 再利用はできない
	 */
	protected void release() {
		synchronized (mItems) {
			mItems.clear();
		}
		mWork.clear();
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(null);
				try {
					mAsyncHandler.getLooper().quit();
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
				mAsyncHandler = null;
			}
		}
	}

	/**
	 * itemViewからViewHolderを生成する
	 * @param itemView
	 * @return
	 */
	protected ViewHolder onCreateViewHolder(final View itemView) {
		return new ViewHolder(itemView);
	}

	@NonNull
	protected FileInfo getFileInfo(final int position)
			throws IndexOutOfBoundsException {

		return mItems.get(position);
	}

	/**
	 * ディレクトリ切り替え実行
	 * @param newDir
	 */
	private void internalChangeDir(@NonNull final DocumentFile newDir) {
		if (DEBUG) Log.v(TAG, "internalChangeDir:" + newDir);
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.post(new ChangeDirTask(newDir));
			} else {
				throw new IllegalStateException("already released");
			}
		}
	}

	/**
	 * 非同期でディレクトリ切り替えを行うためのRunnable実装
	 */
	private class ChangeDirTask implements Runnable {
		private final DocumentFile newDir;
		public ChangeDirTask(@NonNull final DocumentFile newDir) {
			this.newDir = newDir;
		}

		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "ChangeDirTask#run:");
			mWork.clear();
			// ファイルリストを更新
			final Collection<DocumentFile> fileList
				= SAFUtils.listFiles(newDir, mFileFilter);
			if (DEBUG) Log.v(TAG, "ChangeDirTask#run:" + fileList);
			for (final DocumentFile file : fileList) {
				mWork.add(new FileInfo(file, false));
			}
			Collections.sort(mWork);
			// 親フォルダに戻るパスの追加
			if (!mRoot.equals(newDir)) {
				if (DEBUG) Log.v(TAG, "ChangeDirTask#run:check up navigation");
				final DocumentFile parent = newDir.getParentFile();
				if (parent != null) {
					// add at top
					if (DEBUG) Log.v(TAG, "ChangeDirTask#run:add up navigation at top");
					mWork.add(0, new FileInfo(parent, true));
				}
			}
			mUIHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					synchronized (mItems) {
						mCurrentDir = newDir;
						mItems.clear();
						mItems.addAll(mWork);
					}
					mWork.clear();
					notifyDataSetChanged();
				}
			}, DELAY_MILLIS);
		}
	}

	/**
	 * FileFilter object to apply extension filter
	 */
	private final SAFUtils.FileFilter mFileFilter = new SAFUtils.FileFilter() {
		@Override
		public boolean accept(@NonNull final DocumentFile file) {
			if (mExtFilter == null) {
				return true;	// フィルタ無しのときはtrue
			}
			if (file.isDirectory()) {
				return true;	// ディレクトリのときは、true
			}
			final String name = file.getName().toLowerCase(Locale.getDefault());
			for (final String ext : mExtFilter) {
				if (name.endsWith("." + ext)) {
					return true;	// 拡張子が一致すればtrue
				}
			}
			return false;
		}
	};

	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (DEBUG) Log.v(TAG, "onClick:" + v);
			if ((mRecycleView != null) && mRecycleView.isEnabled()) {
				if (v instanceof Checkable) {
					((Checkable)v).setChecked(true);
					mRecycleView.postDelayed(new Runnable() {
						@Override
						public void run() {
							((Checkable)v).setChecked(false);
						}
					}, 100);
				}
				if (mListener != null) {
					final Object pos = v.getTag(R.id.position);
					if (DEBUG) Log.v(TAG, "onClick:pos=" + pos);
					if (pos instanceof Integer) {
						try {
							mListener.onItemClick(
								DocumentTreeRecyclerAdapter.this, v, getItem((int)pos));
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					} else if (DEBUG) {
						// ここにくるのはおかしい
						Log.d(TAG, "FileInfo not attached!");
					}
				}
			} else {
				Log.w(TAG, "onClick:mRecycleView=" + mRecycleView);
			}
		}
	};

	private final View.OnLongClickListener mOnLongClickListener
		= new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(final View v) {
			if (DEBUG) Log.v(TAG, "onLongClick:" + v);
			if (((mRecycleView != null) && mRecycleView.isEnabled())
				&& (mListener != null)) {

				final Object pos = v.getTag(R.id.position);
				if (pos instanceof Integer) {
					try {
						return mListener.onItemLongClick(
							DocumentTreeRecyclerAdapter.this, v, getItem((int)pos));
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				} else if (DEBUG) {
					// ここにくるのはおかしい
					Log.d(TAG, "FileInfo not attached!");
				}
			}
			return false;
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * ファイル情報を保持するためのDocumentFileのラッパークラス
	 */
	protected static class FileInfo implements Comparable<FileInfo>  {
		private final Locale mLocale = Locale.getDefault();
		@NonNull
		private final DocumentFile mFile;			// ファイルオブジェクト
		private final boolean mIsUpNavigation;
		private boolean isSelected = false;	// 選択状態

		/**
		 * コンストラクタ
		 * @param file
		 * @param upNavigation　親ディレクトリへ戻るための仮想ファイルかどうか
		 */
		public FileInfo(
			@NonNull final DocumentFile file,
			final boolean upNavigation) {

			mFile = file;
			mIsUpNavigation = upNavigation;
		}

		/**
		 * 保持しているDocumentFileを取得
		 * @return
		 */
		@NonNull
		public DocumentFile getFile() {
			return mFile;
		}

		/**
		 * 保持しているDocumentFileのuriを取得
		 * 保持しているDocumentFileの#getUriを呼び出す
		 * @return
		 */
		@NonNull
		public Uri getUri() {
			return mFile.getUri();
		}

		/**
		 * 保持しているDocumentFileの表示名を取得
		 * @return
		 */
		@NonNull
		public String getName() {
			final String name = mFile.getName();
			return mIsUpNavigation ? ".." : (TextUtils.isEmpty(name) ? "" : name);
		}

		/**
		 * ディレクトリかどうかを取得
		 * 保持しているDocumentFileの#isDirectoryを呼び出す
		 * @return
		 */
		public boolean isDirectory() {
			return mFile.isDirectory();
		}

		/**
		 * 読み込み可能かどうかを取得
		 * 保持しているDocumentFileの#canReadを呼び出す
		 * @return
		 */
		public boolean canRead() {
			return mFile.canRead();
		}

		/**
		 * 書き込み可能かどうかを取得
		 * 保持しているDocumentFileの#canWriteを呼び出す
		 * @return
		 */
		public boolean canWrite() {
			return mFile.canWrite();
		}

		/**
		 * 親ディレクトリへ戻るための仮想ファイルかどうかを取得
		 * @return
		 */
		public boolean isUpNavigation() {
			return mIsUpNavigation;
		}

		/**
		 * ファイルを選択中かどうかを取得
		 * @return
		 */
		public boolean isSelected() {
			return isSelected;
		}

		/**
		 * ファイルを選択中かどうかをセット
		 * @param select
		 */
		public void select(final boolean select) {
			isSelected = select;
		}

		/**
		 * Comparable interface
		 * @param other
		 * @return
		 */
		@Override
		public int compareTo(@NonNull final FileInfo other) {
			// ディレクトリ < ファイル の順
			if (mFile.isDirectory() && !other.getFile().isDirectory()) {
				return -1;
			}
			if (!mFile.isDirectory() && other.getFile().isDirectory()) {
				return 1;
			}

			// ファイル同士、ディレクトリ同士の場合は、ファイル名（ディレクトリ名）の大文字小文字区別しない辞書順
			return mFile.getName().toLowerCase(mLocale).compareTo(
				other.getName().toLowerCase(mLocale)
			);
		}

		@NonNull
		@Override
		public String toString() {
			return "FileInfo{" +
				"file=" + mFile.getUri() +
				",mIsUpNavigation=" + mIsUpNavigation +
				",isSelected=" + isSelected +
				'}';
		}

	} // FileInfo

//--------------------------------------------------------------------------------
	public static class ViewHolder extends RecyclerView.ViewHolder
		implements Dividable {

		/*package*/ TextView mTitleTv;
		private FileInfo mItem;

		public ViewHolder(final View view) {
			super(view);
			mTitleTv = ViewUtils.findTitleView(view);
			if (mTitleTv == null) {
				throw new IllegalArgumentException("TextView not found");
			}
		}

		@SuppressLint("SetTextI18n")
		private void setItem(
			final int position,
			final FileInfo item) {

			mItem = item;
			itemView.setTag(R.id.position, position);
			if (mTitleTv != null) {
				if (item.isDirectory() && !item.isUpNavigation()) {
					// ディレクトリの場合は、名前の後ろに「/」を付ける
					mTitleTv.setText(item.getName() + "/");
				} else {
					mTitleTv.setText(item.getName());
				}
			}
		}

		@Nullable
		public FileInfo getItem() {
			return mItem;
		}

		public void setEnable(final boolean enable) {
			itemView.setEnabled(enable);
		}

		@Override
		public void hasDivider(final boolean hasDivider) {
			if (itemView instanceof Dividable) {
				((Dividable) itemView).hasDivider(hasDivider);
			} else {
				itemView.setTag(R.id.has_divider, hasDivider);
			}
		}

		@Override
		public boolean hasDivider() {
			if (itemView instanceof Dividable) {
				return ((Dividable) itemView).hasDivider();
			} else {
				final Boolean b = (Boolean) itemView.getTag(R.id.has_divider);
				return ((b != null) && b);
			}
		}

	} // ViewHolder

}
