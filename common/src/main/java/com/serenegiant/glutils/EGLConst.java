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

public interface EGLConst {
	public static final int EGL_RECORDABLE_ANDROID = 0x3142;
	public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	public static final int EGL_OPENGL_ES2_BIT = 4;
	public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
//	public static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;

	public static final int EGL_FLAG_DEPTH_BUFFER = 0x01;
	public static final int EGL_FLAG_RECORDABLE = 0x02;
	public static final int EGL_FLAG_STENCIL_1BIT = 0x04;
	public static final int EGL_FLAG_STENCIL_2BIT = 0x08;
	public static final int EGL_FLAG_STENCIL_4BIT = 0x10;
	public static final int EGL_FLAG_STENCIL_8BIT = 0x20;
}
