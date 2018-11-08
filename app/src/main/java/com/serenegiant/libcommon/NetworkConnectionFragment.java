package com.serenegiant.libcommon;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link BaseFragment} subclass.
 * Use the {@link NetworkConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NetworkConnectionFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = NetworkConnectionFragment.class.getSimpleName();

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
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_network_connection, container, false);
	}
	
}
