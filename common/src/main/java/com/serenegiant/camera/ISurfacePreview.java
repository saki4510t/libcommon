package com.serenegiant.camera;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
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

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;

public interface ISurfacePreview extends IPreview {
	/**
	 * set preview surface with SurfaceHolder</br>
	 * you can use SurfaceHolder came from SurfaceView/GLSurfaceView
	 * @param holder
	 */
	public void setPreviewSurface(final SurfaceHolder holder);

	/**
	 * set preview surface with SurfaceTexture.
	 * this method require API >= 14
	 * @param texture
	 */
	public void setPreviewSurface(final SurfaceTexture texture);

	/**
	 * set preview surface with Surface
	 * @param surface
	 */
	public void setPreviewSurface(final Surface surface);
}
