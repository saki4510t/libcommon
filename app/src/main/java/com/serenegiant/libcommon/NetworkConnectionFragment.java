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
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.net.ConnectivityHelper;

/**
 * A simple {@link BaseFragment} subclass.
 * Use the {@link NetworkConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NetworkConnectionFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = NetworkConnectionFragment.class.getSimpleName();

	private ConnectivityHelper mHelper;

	public NetworkConnectionFragment() {
		super();
	}
	
	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment NetworkConnectionFragment.
	 */
	public static NetworkConnectionFragment newInstance() {
		NetworkConnectionFragment fragment = new NetworkConnectionFragment();
		final Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mHelper == null) {
			mHelper = new ConnectivityHelper(requireContext(), mConnectivityCallback);
		}
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_network_connection, container, false);
	}
	
	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (DEBUG) Log.v(TAG, "isWifiNetworkReachable:"
			+ ConnectivityHelper.isWifiNetworkReachable(requireContext()));
		if (DEBUG) Log.v(TAG, "isMobileNetworkReachable:"
			+ ConnectivityHelper.isMobileNetworkReachable(requireContext()));
		if (DEBUG) Log.v(TAG, "isNetworkReachable:"
			+ ConnectivityHelper.isNetworkReachable(requireContext()));
	}
	
	@Override
	protected void internalOnPause() {
		super.internalOnPause();
	}
	
	@Override
	protected void internalRelease() {
		if (mHelper != null) {
			mHelper.release();
		}
		super.internalRelease();
	}

	private final ConnectivityHelper.ConnectivityCallback
		mConnectivityCallback = new ConnectivityHelper.ConnectivityCallback() {

		@Override
		public void onNetworkChanged(final int activeNetworkType) {
			if (DEBUG) Log.v(TAG, "onNetworkChanged:"
				+ ConnectivityHelper.getNetworkTypeString(activeNetworkType));
		}
		
		@Override
		public void onError(final Throwable t) {
			Log.w(TAG, t);
		}
	};
}
