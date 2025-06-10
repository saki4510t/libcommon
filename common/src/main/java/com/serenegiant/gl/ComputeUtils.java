package com.serenegiant.gl;
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

import android.opengl.GLES31;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import static com.serenegiant.gl.GLUtils.checkGlError;

/**
 * API21/GLES31以降のコンピュートシェーダー関係のヘルパー
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ComputeUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ComputeUtils.class.getSimpleName();

	private ComputeUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * GLES31以降のコンピュートシェーダーをコンパイル＆リンク
	 * @param css
	 * @return
	 */
	public static int loadShader(@NonNull final String css) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		final int[] compiled = new int[1];
		// コンピュートシェーダーをコンパイル
		final int vs = GLUtils.loadShader(GLES31.GL_COMPUTE_SHADER, css);
		if (vs == 0) {
			Log.d(TAG, "loadShader:failed to compile compute shader,\n" + css);
			return 0;
		}
		// リンク
		final int program = GLES31.glCreateProgram();
		checkGlError("glCreateProgram");
		if (program == 0) {
			Log.e(TAG, "loadShader:Failed to create program");
		}
		GLES31.glAttachShader(program, vs);
		checkGlError("glAttachShader");
		GLES31.glLinkProgram(program);
		final int[] linkStatus = new int[1];
		GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] != GLES31.GL_TRUE) {
			Log.e(TAG, "loadShader:Failed to link program");
			Log.e(TAG, GLES31.glGetProgramInfoLog(program));
			GLES31.glDeleteProgram(program);
			return 0;
		}
		return program;
	}
}
