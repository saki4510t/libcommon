package com.serenegiant.libcommon;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.widget.CameraSurfaceView;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SurfaceViewへカメラ映像を流し込んで表示するだけのFragment
 */
public class CameraSurfaceFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = CameraSurfaceFragment.class.getSimpleName();

	private CameraSurfaceView mCameraView;

	public CameraSurfaceFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public void onAttach(@NotNull final Context context) {
		super.onAttach(context);
		requireActivity().setTitle(TAG);
	}

	@Nullable
	@Override
	public View onCreateView(
		@NonNull final LayoutInflater inflater,
		@Nullable final ViewGroup container,
		@Nullable final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		final View rootView = inflater.inflate(
			R.layout.fragment_camera_surfaceview,
			container, false);
		mCameraView = rootView.findViewById(R.id.cameraView);
		return rootView;
	}

	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (DEBUG) Log.v(TAG, "internalOnResume:");
		mCameraView.onResume();
	}

	@Override
	protected void internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:");
		mCameraView.onPause();
		super.internalOnPause();
	}
}
