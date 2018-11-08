package com.serenegiant.libcommon;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.libcommon.list.DummyContent;

public class MainActivity extends AppCompatActivity
	implements TitleFragment.OnListFragmentInteractionListener {

	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = MainActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DummyContent.createItems(this, R.array.list_items);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, new TitleFragment())
				.commit();
		}
	}
	
	@Override
	public void onListFragmentInteraction(final DummyContent.DummyItem item) {
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:" + item);
		
		Fragment fragment = null;
		switch (item.id) {
		case 0:
			fragment = NetworkConnectionFragment.newInstance();
			break;
		case 1:
			break;
		default:
			break;
		}
		if (fragment != null) {
			getSupportFragmentManager()
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, fragment)
				.commit();
		}
	}
}
