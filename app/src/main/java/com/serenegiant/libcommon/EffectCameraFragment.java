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
import android.view.View;

import com.serenegiant.glutils.EffectRendererHolder;
import com.serenegiant.widget.EffectCameraGLView;

public class EffectCameraFragment extends AbstractCameraFragment {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = EffectCameraFragment.class.getSimpleName();

	public static EffectCameraFragment newInstance() {
		final EffectCameraFragment fragment = new EffectCameraFragment();
		final Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public EffectCameraFragment() {
		super();
		// need default constructor
	}

	@Override
	protected int getLayoutXml() {
		return R.layout.fragment_camera_effect;
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

	protected boolean onLongClick(final View view) {
		if (mCameraView instanceof EffectCameraGLView) {
			final EffectCameraGLView v = (EffectCameraGLView)view;
			v.setEffect((v.getEffect() + 1) % EffectRendererHolder.EFFECT_NUM);
			return true;
		}
		return false;
	}

}
