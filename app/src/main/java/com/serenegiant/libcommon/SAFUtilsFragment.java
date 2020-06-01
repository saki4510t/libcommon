package com.serenegiant.libcommon;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.serenegiant.dialog.ConfirmDialogV4;
import com.serenegiant.libcommon.databinding.FragmentSafutilsBinding;
import com.serenegiant.libcommon.viewmodel.SAFUtilsViewModel;
import com.serenegiant.system.SAFUtils;
import com.serenegiant.widget.ArrayListRecyclerViewAdapter;
import com.serenegiant.widget.StringsRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * SAFUtilクラスのテスト用Fragment
 */
public class SAFUtilsFragment extends BaseFragment
	implements ConfirmDialogV4.ConfirmDialogListener {

	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = SAFUtilsFragment.class.getSimpleName();

	private FragmentSafutilsBinding mBinding;
	private StringsRecyclerViewAdapter mAdapter;
	private int mLastRequestCode;

	/**
	 * デフォルトコンストラクタ
	 */
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

	/**
	 * パーミッション要求結果の処理
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
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

	/**
	 * 指定したリクエストコードに対するパーミッションをすでに保持しているときに
	 * 再設定するかどうかの問い合わせに対するユーザーレスポンスの処理
	 * @param dialog
	 * @param requestCode
	 * @param result DialogInterface#BUTTONxxx
	 * @param userArgs
	 */
	@Override
	public void onConfirmResult(
		@NonNull final ConfirmDialogV4 dialog,
		final int requestCode, final int result, @Nullable final Bundle userArgs) {

		if (result == DialogInterface.BUTTON_POSITIVE) {
			// ユーザーがOKを押したときの処理, パーミッションを要求する
			requestPermission(requestCode);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 表示を初期化
	 */
	private void initView() {
		mAdapter = new StringsRecyclerViewAdapter(
			R.layout.list_item_title, new ArrayList<String>());
		mAdapter.setOnItemClickListener(
			new ArrayListRecyclerViewAdapter.ArrayListRecyclerViewListener<String>() {

			@Override
			public void onItemClick(
				@NonNull final RecyclerView.Adapter<?> parent,
				@NonNull final View view,
				final int position, @Nullable final String item) {

			}

			@Override
			public boolean onItemLongClick(
				@NonNull final RecyclerView.Adapter<?> parent,
				@NonNull final View view,
				final int position, @Nullable final String item) {

				if (!TextUtils.isEmpty(item)) {
					final String[] v = item.split("@");
					if (v.length == 2) {
						try {
							final int requestCode = Integer.parseInt(v[0]);
							if (requestCode != 0) {
								Toast.makeText(requireContext(),
									"release permission,requestCode=" + requestCode,
									Toast.LENGTH_SHORT).show();
								SAFUtils.releasePersistableUriPermission(requireContext(), requestCode);
								updateSAFPermissions();
								return true;
							}
						} catch (final NumberFormatException e) {
							Log.w(TAG, e);
						}
					}
				}
				return false;
			}
		});
		mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
		mBinding.list.setEmptyView(mBinding.empty);
		mBinding.list.setAdapter(mAdapter);
	}

	/**
	 * 表示内容の処理用ViewModelオブジェクト
	 */
	private final SAFUtilsViewModel mViewModel
		= new SAFUtilsViewModel() {
		@Override
		public void onClick(final View v) {
			if (DEBUG) Log.v(TAG, "onClick:" + v);
			if (v.getId() == R.id.add_btn) {
				final int requestCode = mViewModel.getRequestCode();
				if ((requestCode != 0) && ((requestCode & 0xffff) == requestCode)) {
					if (DEBUG) Log.v(TAG, "onClick:request SAF permission,requestCode=" + requestCode);
					if (!SAFUtils.hasPermission(requireContext(), requestCode)) {
						requestPermission(requestCode);
					} else {
						ConfirmDialogV4.showDialog(SAFUtilsFragment.this,
							requestCode, R.string.title_request_saf_permission,
								"Already has permission for requestCode(" + requestCode + ")",
								true);
					}
				} else {
					Toast.makeText(requireContext(),
					"Fragmentからは下位16ビットしかリクエストコードとして使えない," + requestCode,
						Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	/**
	 * 指定したリクエストコードに対するパーミッションを要求
	 * @param requestCode
	 */
	private void requestPermission(final int requestCode) {
		mLastRequestCode = requestCode;
		SAFUtils.releasePersistableUriPermission(requireContext(), requestCode);
		SAFUtils.requestPermission(SAFUtilsFragment.this, requestCode);
	}

	/**
	 * 保持しているパーミッション一覧表示を更新
	 */
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
