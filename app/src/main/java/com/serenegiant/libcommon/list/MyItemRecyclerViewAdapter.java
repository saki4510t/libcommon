package com.serenegiant.libcommon.list;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.serenegiant.libcommon.R;
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener;
import com.serenegiant.libcommon.list.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {
	
	private final List<DummyItem> mValues;
	private final OnListFragmentInteractionListener mListener;
	
	public MyItemRecyclerViewAdapter(List<DummyItem> items, OnListFragmentInteractionListener listener) {
		mValues = items;
		mListener = listener;
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		View view = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.list_item_title, parent, false);
		return new ViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		holder.mItem = mValues.get(position);
		holder.mContentView.setText(mValues.get(position).content);
		
		holder.mView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if the
					// fragment is attached to one) that an item has been selected.
					mListener.onListFragmentInteraction(holder.mItem);
				}
			}
		});
	}
	
	@Override
	public int getItemCount() {
		return mValues.size();
	}
	
	public class ViewHolder extends RecyclerView.ViewHolder {
		public final View mView;
		public final TextView mContentView;
		public DummyItem mItem;
		
		public ViewHolder(View view) {
			super(view);
			mView = view;
			mContentView = view.findViewById(R.id.content);
		}
		
		@Override
		public String toString() {
			return super.toString() + " '" + mContentView.getText() + "'";
		}
	}
}
