package com.serenegiant.libcommon;
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

import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.serenegiant.mediastore.MediaInfo;
import com.serenegiant.mediastore.MediaStoreAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;

/** 端末内の静止画・動画一覧を表示するためのFragment */
public class GalleyFragment extends BaseFragment {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static String TAG = GalleyFragment.class.getSimpleName();

	public static GalleyFragment newInstance() {
		GalleyFragment fragment = new GalleyFragment();
		return fragment;
	}

	private MediaStoreAdapter mMediaStoreAdapter;

	public GalleyFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		final View rootView = inflater.inflate(R.layout.fragment_galley, container, false);
		initView(rootView);
		return rootView;
	}

	/**
	 * Viewを初期化
	 * @param rootView
	 */
	private void initView(final View rootView) {
		final GridView gridView = rootView.findViewById(R.id.media_gridview);
		mMediaStoreAdapter = new MediaStoreAdapter(requireContext(), R.layout.grid_item_media);
		gridView.setAdapter(mMediaStoreAdapter);
		gridView.setOnItemClickListener(mOnItemClickListener);
	}

	private final AdapterView.OnItemClickListener mOnItemClickListener
		= new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {

			switch (parent.getId()) {
			case R.id.media_gridview:
				doPlay(position, id);
				break;
			}
		}
	};

	private void doPlay(final int position, final long id) {
		final MediaInfo info = mMediaStoreAdapter.getMediaInfo(position);
		if (DEBUG) Log.v(TAG, "" + info);
		switch (info.mediaType) {
		case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
			// 静止画を選択した時
			requireFragmentManager()
				.beginTransaction()
				.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.replace(R.id.container, ImageFragment.newInstance(info))
				.commit();
			break;
		case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
			// 動画を選択した時
			break;
		default:
			break;
		}
	}
}
