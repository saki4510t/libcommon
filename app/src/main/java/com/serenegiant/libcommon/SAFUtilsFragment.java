package com.serenegiant.libcommon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.serenegiant.libcommon.databinding.FragmentSafutilsBinding;
import com.serenegiant.libcommon.viewmodel.SAFUtilsViewModel;
import com.serenegiant.utils.SAFUtils;
import com.serenegiant.widget.StringsRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

public class SAFUtilsFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SAFUtilsFragment.class.getSimpleName();

	private FragmentSafutilsBinding mBinding;
	private StringsRecyclerViewAdapter mAdapter;
	private int mLastRequestCode;

	public SAFUtilsFragment() {
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
			R.layout.fragment_safutils, container, false);
		mBinding.setViewModel(mViewModel);
		initView();
		return mBinding.getRoot();
	}

	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (DEBUG) Log.v(TAG, "internalOnResume:");
		updateSAFPermissions();
	}

	@Override
	protected void internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:");
		super.internalOnPause();
	}

	@Override
	public void onActivityResult(final int requestCode,
		final int resultCode, @Nullable final Intent data) {

		if (DEBUG) Log.v(TAG, "onActivityResult:requestCode=" + requestCode);
		if (mLastRequestCode == requestCode) {
			mLastRequestCode = 0;
			SAFUtils.handleOnResult(requireContext(),
				requestCode, resultCode, data, new SAFUtils.handleOnResultDelegater() {

				@Override
				public boolean onResult(final int requestCode,
					@NonNull final Uri uri, @NonNull final Intent data) {

					if (DEBUG) Log.v(TAG, "onResult:call takePersistableUriPermission," + data);
					SAFUtils.takePersistableUriPermission(requireContext(), requestCode, uri);
					return true;
				}

				@Override
				public void onFailed(final int requestCode,
					@Nullable final Intent data) {

					if (DEBUG) Log.v(TAG, "onFailed:" + data);
				}
			});
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void initView() {
		mAdapter = new StringsRecyclerViewAdapter(
			R.layout.list_item_title, new ArrayList<String>());
		mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
		mBinding.list.setEmptyView(mBinding.empty);
		mBinding.list.setAdapter(mAdapter);
	}

	private final SAFUtilsViewModel mViewModel
		= new SAFUtilsViewModel() {
		@Override
		public void onClick(final View v) {
			if (DEBUG) Log.v(TAG, "onClick:" + v);
			if (v.getId() == R.id.add_btn) {
				final int requestCode = mViewModel.getRequestCode();
				if ((requestCode != 0) && ((requestCode & 0xffff) == requestCode)) {
					if (DEBUG) Log.v(TAG, "onClick:request SAF permission,requestCode=" + requestCode);
					mLastRequestCode = requestCode;
					SAFUtils.releaseStorageAccessPermission(requireContext(), requestCode);
					SAFUtils.requestPermission(SAFUtilsFragment.this, requestCode);
				} else {
					Toast.makeText(requireContext(),
					"Fragmentからは下位16ビットしかリクエストコードとして使えない," + requestCode,
						Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	private void updateSAFPermissions() {
		if (DEBUG) Log.v(TAG, "updateSAFPermissions:");
		@NonNull
		final Map<Integer, Uri> map = SAFUtils.getStorageUriAll(requireContext());
		final List<String> list = new ArrayList<>();
		for (final Map.Entry<Integer, Uri> entry: map.entrySet()) {
			list.add(entry.getKey() + "@" + entry.getValue().toString());
		}
		mAdapter.replaceAll(list);
		mAdapter.notifyDataSetChanged();
	}
}
