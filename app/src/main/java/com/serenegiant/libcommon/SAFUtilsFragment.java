package com.serenegiant.libcommon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.libcommon.databinding.FragmentSafutilsBinding;
import com.serenegiant.libcommon.viewmodel.SAFUtilsViewModel;
import com.serenegiant.utils.SAFUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

public class SAFUtilsFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SAFUtilsFragment.class.getSimpleName();

	private FragmentSafutilsBinding mBinding;

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
	}

	@Override
	protected void internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:");
		super.internalOnPause();
	}

	private void initView() {
		// FIXME 未実装
	}

	private final SAFUtilsViewModel mViewModel
		= new SAFUtilsViewModel() {
		@Override
		public void onClick(final View v) {
			if (DEBUG) Log.v(TAG, "onClick:" + v);
			if (v.getId() == R.id.add_btn) {
				final int requestCode = mViewModel.getRequestCode();
				SAFUtils.releaseStorageAccessPermission(requireContext(), requestCode);
				SAFUtils.requestPermission(SAFUtilsFragment.this, requestCode);
			}
		}
	};
}
