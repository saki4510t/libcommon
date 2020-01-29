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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.mediastore.MediaInfo;
import com.serenegiant.mediastore.MediaStoreRecyclerAdapter;
import com.serenegiant.widget.RecycleViewWithEmptyView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 端末内の静止画・動画一覧を表示するためのFragment
 * RecyclerViewを使用
 * FIXME レイアウトの設定がいまいちで表示がきれくない(´・ω・｀)
 */
public class GalleyFragment2 extends BaseFragment {
	private static final boolean DEBUG = true;	// FIXME 実働時はfalseにすること
	private static String TAG = GalleyFragment2.class.getSimpleName();

	public static GalleyFragment2 newInstance() {
		GalleyFragment2 fragment = new GalleyFragment2();
		return fragment;
	}

	private MediaStoreRecyclerAdapter mMediaStoreAdapter;

	public GalleyFragment2() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		final View rootView = inflater.inflate(R.layout.fragment_galley2, container, false);
		initView(rootView);
		return rootView;
	}

	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (mMediaStoreAdapter != null) {
			mMediaStoreAdapter.refresh();
		}
	}

	/**
	 * Viewを初期化
	 * @param rootView
	 */
	private void initView(final View rootView) {
		final RecycleViewWithEmptyView recyclerView
			= rootView.findViewById(R.id.media_recyclerview);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(
			new GridLayoutManager(requireContext(), 4));
//		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setEmptyView(rootView.findViewById(R.id.empty));
		mMediaStoreAdapter = new MediaStoreRecyclerAdapter(
			requireContext(), R.layout.grid_item_media);
		mMediaStoreAdapter.setListener(mListener);
		recyclerView.setAdapter(mMediaStoreAdapter);
	}

	private final MediaStoreRecyclerAdapter.MediaStoreRecyclerAdapterListener
		mListener = new MediaStoreRecyclerAdapter.MediaStoreRecyclerAdapterListener() {

		@Override
		public void onItemClick(@NonNull final RecyclerView.Adapter<?> parent,
			@NonNull final View view, @NonNull final MediaInfo item) {

			if (DEBUG) Log.v(TAG, "onItemClick:" + item);
			requireFragmentManager()
				.beginTransaction()
				.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.replace(R.id.container, ImageFragment.newInstance(item))
				.commit();
		}

		@Override
		public boolean onItemLongClick(@NonNull final RecyclerView.Adapter<?> parent,
			@NonNull final View view, @NonNull final MediaInfo item) {

			if (DEBUG) Log.v(TAG, "onItemLongClick:" + item);
			return false;
		}

	};

}
