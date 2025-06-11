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

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGL10;

import org.json.JSONException;
import org.json.JSONObject;

import android.opengl.GLES20;

import com.serenegiant.egl.EGLBase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * OpenGL|ESとEGLの情報取得用ヘルパークラス
 */
public class GLInfo {
	private GLInfo() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	private static final int EGL_CLIENT_APIS                    = 0x308D;

	/**
	 * OpenGL|ESとEGLの情報取得をJSONObjectとして取得する
	 * @return
	 * @throws JSONException
	 */
	@NonNull
	public static JSONObject get() throws JSONException {
		try {
			final GLContext context = new GLContext(3, null, 0);
			final Semaphore sem = new Semaphore(0);
			final AtomicReference<JSONObject> info = new AtomicReference<>();
			new Thread(() -> {
				context.initialize();
				JSONObject result = new JSONObject();
				try {
					result = getOnGL(context.getEgl());
				} catch (final JSONException e) {
					try {
						result.put("EXCEPTION", e.getMessage());
					} catch (JSONException ex) {
						// ignore
					}
				} finally {
					context.release();
					info.set(result);
					sem.release();
				}
			}).start();
			if (!sem.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Failed to get GL info");
			}
			final JSONObject result = info.get();
			if (result != null) {
				return  result;
			} else {
				return new JSONObject();
			}
		} catch (Exception e) {
			final JSONObject result = new JSONObject();
			result.put("EXCEPTION", e.getMessage());
			return result;
		}
	}

	/**
	 * EGLとGLESの情報をJSONObjectとして取得する
	 * EGL/GL|ESコンテキスト上で実行すること
	 * @param egl
	 * @return
	 * @throws JSONException
	 */
	@WorkerThread
	@NonNull
	public static JSONObject getOnGL(final EGLBase egl) throws JSONException {
		final JSONObject result = new JSONObject();
		final IntBuffer val = IntBuffer.allocate(2);
		// GLコンテキストの情報を取得
		final JSONObject glInfo = new JSONObject();
		try {
			glInfo.put("GL_VENDOR", GLES20.glGetString(GLES20.GL_VENDOR));
		} catch (Exception e) {
			glInfo.put("GL_VENDOR", e.getMessage());
		}
		try {
			glInfo.put("GL_VERSION", GLES20.glGetString(GLES20.GL_VERSION));
		} catch (Exception e) {
			glInfo.put("GL_VERSION", e.getMessage());
		}
		try {
			glInfo.put("GL_RENDERER", GLES20.glGetString(GLES20.GL_RENDERER));
		} catch (Exception e) {
			glInfo.put("GL_RENDERER", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, val);
			glInfo.put("GL_MAX_VERTEX_ATTRIBS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_VERTEX_ATTRIBS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, val);
			glInfo.put("GL_MAX_VERTEX_UNIFORM_VECTORS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_VERTEX_UNIFORM_VECTORS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, val);
			glInfo.put("GL_MAX_VARYING_VECTORS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_VARYING_VECTORS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, val);
			glInfo.put("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, val);
			glInfo.put("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, val);
			glInfo.put("GL_MAX_TEXTURE_IMAGE_UNITS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_TEXTURE_IMAGE_UNITS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, val);
			glInfo.put("GL_MAX_FRAGMENT_UNIFORM_VECTORS", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_FRAGMENT_UNIFORM_VECTORS", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, val);
			glInfo.put("GL_MAX_CUBE_MAP_TEXTURE_SIZE", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_CUBE_MAP_TEXTURE_SIZE", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, val);
			glInfo.put("GL_MAX_RENDERBUFFER_SIZE", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_RENDERBUFFER_SIZE", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, val);
			glInfo.put("GL_MAX_TEXTURE_SIZE", val.get(0));
		} catch (Exception e) {
			glInfo.put("GL_MAX_TEXTURE_SIZE", e.getMessage());
		}
		try {
			GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, val);
			glInfo.put("GL_MAX_VIEWPORT_DIMS", String.format(Locale.US, "%d x %d", val.get(0), val.get(1)));
		} catch (Exception e) {
			glInfo.put("GL_MAX_VIEWPORT_DIMS", e.getMessage());
		}
		try {
			glInfo.put("GL_EXTENSIONS", formatExtensions(GLES20.glGetString(GLES20.GL_EXTENSIONS)));
		} catch (Exception e) {
			glInfo.put("GL_EXTENSIONS", e.getMessage());
		}
		result.put("GL_INFO", glInfo);
		// EGLの情報取得
		final JSONObject eglInfo = new JSONObject();
		try {
			eglInfo.put("EGL_VENDOR", egl.queryString(EGL10.EGL_VENDOR));
		} catch (Exception e) {
			glInfo.put("EGL_VENDOR", e.getMessage());
		}
		try {
			eglInfo.put("EGL_VERSION", egl.queryString(EGL10.EGL_VERSION));
		} catch (Exception e) {
			glInfo.put("EGL_VERSION", e.getMessage());
		}
		try {
			eglInfo.put("EGL_CLIENT_APIS", egl.queryString(EGL_CLIENT_APIS));
		} catch (Exception e) {
			glInfo.put("EGL_CLIENT_APIS", e.getMessage());
		}
		try {
			eglInfo.put("EGL_EXTENSIONS:", formatExtensions(egl.queryString(EGL10.EGL_EXTENSIONS)));
		} catch (Exception e) {
			glInfo.put("EGL_EXTENSIONS", e.getMessage());
		}
		result.put("EGL_INFO", eglInfo);

		return result;
	}

    /**
     * Formats the extensions string, which is a space-separated list, into a series of indented
     * values followed by newlines.  The list is sorted.
     */
    private static final JSONObject formatExtensions(@NonNull final String ext) throws JSONException {
    	JSONObject result = new JSONObject();
        final String[] values = ext.split(" ");
        Arrays.sort(values);
        for (int i = 0; i < values.length; i++) {
			result.put(Integer.toString(i), values[i]);
        }
        return result;
    }

}
