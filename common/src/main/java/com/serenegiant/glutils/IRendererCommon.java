package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
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

public interface IRendererCommon {
	// IRendererHolder, CameraViewInterfaceと一致させること
	public static final int MIRROR_NORMAL = 0;
	public static final int MIRROR_HORIZONTAL = 1;
	public static final int MIRROR_VERTICAL = 2;
	public static final int MIRROR_BOTH = 3;
	public static final int MIRROR_NUM = 4;

	/**
	 * 映像を上下左右反転させるかどうかをセット
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 */
	public void setMirror(final int mirror);
}
