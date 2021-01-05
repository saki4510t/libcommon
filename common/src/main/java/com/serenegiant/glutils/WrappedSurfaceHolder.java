package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * Android4.1.2だとSurfaceを使えない。
 * SurfaceTexture/SurfaceHolderの場合は内部でSurfaceを生成して使っているにもかかわらず。
 * SurfaceHolderはインターフェースなのでSurfaceHolderを継承したダミークラスを生成して食わす
 * Camera#
 */
public class WrappedSurfaceHolder implements SurfaceHolder {
	private final Surface surface;

	public WrappedSurfaceHolder(final Surface surface) {
		this.surface = surface;
	}
	@Override
	public Surface getSurface() {
		return surface;
	}
	// ここより下はどないでもええ
	@Override
	public void addCallback(final Callback callback) {
	}
	@Override
	public void removeCallback(final Callback callback) {
	}
	@Override
	public boolean isCreating() {
		return false;
	}
	@Override
	public void setType(final int type) {
	}
	@Override
	public void setFixedSize(final int width, final int height) {
	}
	@Override
	public void setSizeFromLayout() {
	}
	@Override
	public void setFormat(final int format) {
	}
	@Override
	public void setKeepScreenOn(final boolean screenOn) {
	}
	@Override
	public Canvas lockCanvas() {
		return null;
	}
	@Override
	public Canvas lockCanvas(final Rect dirty) {
		return null;
	}
	@Override
	public void unlockCanvasAndPost(final Canvas canvas) {
	}
	@Override
	public Rect getSurfaceFrame() {
		return null;
	}
}
