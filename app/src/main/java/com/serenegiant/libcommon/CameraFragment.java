package com.serenegiant.libcommon;
/*
 *
 * Copyright (c) 2016-2019 saki t_saki@serenegiant.com
 *
 * File name: PostMuxRecFragment.java
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
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.os.Bundle;
import android.util.Log;

public class CameraFragment extends AbstractCameraFragment {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = CameraFragment.class.getSimpleName();

	public static CameraFragment newInstance() {
		final CameraFragment fragment = new CameraFragment();
		final Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public CameraFragment() {
		super();
		// need default constructor
	}

	@Override
	protected boolean isRecording() {
		// FIXME 未実装
		return false;
	}

	@Override
	protected void internalStartRecording() {
		if (DEBUG) Log.v(TAG, "internalStartRecording:");
		// FIXME 未実装
	}

	@Override
	protected void internalStopRecording() {
		if (DEBUG) Log.v(TAG, "internalStopRecording:");
		// FIXME 未実装
	}

	@Override
	protected void onFrameAvailable() {
		// FIXME 未実装
	}

}
