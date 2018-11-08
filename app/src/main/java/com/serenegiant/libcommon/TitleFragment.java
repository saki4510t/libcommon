package com.serenegiant.libcommon;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.libcommon.list.DummyContent;
import com.serenegiant.libcommon.list.DummyContent.DummyItem;
import com.serenegiant.libcommon.list.MyItemRecyclerViewAdapter;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class TitleFragment extends Fragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = TitleFragment.class.getSimpleName();
	
	// TODO: Customize parameter argument names
	private static final String ARG_COLUMN_COUNT = "column-count";
	// TODO: Customize parameters
	private int mColumnCount = 1;
	private OnListFragmentInteractionListener mListener;
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TitleFragment() {
		super();
	}
	
	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static TitleFragment newInstance(int columnCount) {
		TitleFragment fragment = new TitleFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_COLUMN_COUNT, columnCount);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getArguments() != null) {
			mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
		}
	}
	
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_item_list, container, false);
		
		// Set the adapter
		if (view instanceof RecyclerView) {
			Context context = view.getContext();
			RecyclerView recyclerView = (RecyclerView) view;
			if (mColumnCount <= 1) {
				recyclerView.setLayoutManager(new LinearLayoutManager(context));
			} else {
				recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
			}
			recyclerView.setAdapter(new MyItemRecyclerViewAdapter(DummyContent.ITEMS, mListener));
		}
		return view;
	}
	
	
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnListFragmentInteractionListener) {
			mListener = (OnListFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
				+ " must implement OnListFragmentInteractionListener");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnListFragmentInteractionListener {
		// TODO: Update argument type and name
		void onListFragmentInteraction(DummyItem item);
	}
}
