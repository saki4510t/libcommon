package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Hold shared texture that has camera frame and draw them to registered surface if needs<br>
 */
public class RendererHolder extends AbstractRendererHolder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = RendererHolder.class.getSimpleName();

	public RendererHolder(final int width, final int height, @Nullable final RenderHolderCallback callback) {
//		if (DEBUG) Log.v(TAG, "Constructor");
		super(width, height, callback);
//		if (DEBUG) Log.v(TAG, "Constructor:finished");
	}

	@NonNull
	protected RendererTask createRendererTask(final int width, final int height) {
		return new MyRendererTask(this, width, height);
	}
	
//================================================================================
// 実装
//================================================================================
	/**
	 * ワーカースレッド上でOpenGL|ESを用いてマスター映像を分配描画するためのインナークラス
	 */
	private static final class MyRendererTask extends RendererTask {

		public MyRendererTask(final RendererHolder parent, final int width, final int height) {
			super(parent, width, height);
		}
	}

}
