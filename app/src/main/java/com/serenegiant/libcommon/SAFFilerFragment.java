package com.serenegiant.libcommon;
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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.libcommon.databinding.FragmentSafFilerBinding;
import com.serenegiant.widget.DocumentTreeRecyclerAdapter;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.documentfile.provider.DocumentFile;
import androidx.documentfile.provider.SAFRootTreeDocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * DocumentTreeRecyclerAdapterとSAFRootTreeDocumentFileのテスト用Fragment
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SAFFilerFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SAFFilerFragment.class.getSimpleName();

	private FragmentSafFilerBinding mBinding;
	private DocumentTreeRecyclerAdapter mAdapter;

	/**
	 * デフォルトコンストラクタ
	 */
	public SAFFilerFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		@Nullable final ViewGroup container,
		@Nullable final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		mBinding = DataBindingUtil.inflate(inflater,
			R.layout.fragment_saf_filer, container, false);
		initView();
		return mBinding.getRoot();
	}

	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (DEBUG) Log.v(TAG, "internalOnResume:");
	}

	@Override
	protected void internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:");
		super.internalOnPause();
	}

//--------------------------------------------------------------------------------
	/**
	 * 表示を初期化
	 */
	private void initView() {
		mAdapter = new DocumentTreeRecyclerAdapter(
			requireContext(),
			R.layout.list_item_title,
			SAFRootTreeDocumentFile.fromContext(requireContext()));
		mAdapter.setListener(
			new DocumentTreeRecyclerAdapter.DocumentTreeRecyclerAdapterListener() {

			@Override
			public void onItemClick(
				@NonNull final RecyclerView.Adapter<?> parent,
				@NonNull final View view,
				@NonNull final DocumentFile item) {

				if (DEBUG) Log.v(TAG, "onItemClick:" + item);
				if (item.isDirectory()) {
					try {
						mAdapter.changeDir(item);
					} catch (final IOException e) {
						Log.w(TAG, e);
					}
				} else {
					// open specific file
				}

			}

			@Override
			public boolean onItemLongClick(
				@NonNull final RecyclerView.Adapter<?> parent,
				@NonNull final View view,
				@NonNull final DocumentFile item) {

				if (DEBUG) Log.v(TAG, "onItemLongClick:" + item);
				// show popup menu?
				return false;
			}

		});
		mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
		mBinding.list.setEmptyView(mBinding.empty);
		mBinding.list.setAdapter(mAdapter);
	}

}
