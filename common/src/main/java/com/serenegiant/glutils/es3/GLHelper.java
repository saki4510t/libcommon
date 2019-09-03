package com.serenegiant.glutils.es3;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.serenegiant.glutils.ShaderConst;
import com.serenegiant.utils.AssetsHelper;
import com.serenegiant.utils.BuildCheck;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * OpenGL|ES2/3用のヘルパークラス
 */
public final class GLHelper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = "GLHelper";

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param op
	 */
    public static void checkGlError(final String op) {
        final int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            final String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg);
            new Throwable(msg).printStackTrace();
//         	if (DEBUG) {
//	            throw new RuntimeException(msg);
//       	}
        }
    }

	/**
	 * テクスチャ名を生成, テクスチャユニットはGL_TEXTURE0, クランプ方法はGL_CLAMP_TO_EDGE
	 * @param texTarget
	 * @param filter_param テクスチャの補完方法を指定, min/mag共に同じ値になる, GL_LINEARとかGL_NEAREST
	 * @return
	 */
	public static int initTex(final int texTarget, final int filter_param) {
		return initTex(texTarget, GLES30.GL_TEXTURE0,
			filter_param, filter_param, GLES30.GL_CLAMP_TO_EDGE);
	}

	/**
	 * テクスチャ名を生成(GL_TEXTURE0のみ)
	 * @param texTarget
	 * @param texUnit テクスチャユニット, GL_TEXTURE0...GL_TEXTURE31
	 * @param min_filter テクスチャの補間方法を指定, GL_LINEARとかGL_NEAREST
	 * @param mag_filter テクスチャの補間方法を指定, GL_LINEARとかGL_NEAREST
	 * @param wrap テクスチャのクランプ方法, GL_CLAMP_TO_EDGE
	 * @return
	 */
	public static int initTex(final int texTarget, final int texUnit,
		final int min_filter, final int mag_filter, final int wrap) {

//		if (DEBUG) Log.v(TAG, "initTex:target=" + texTarget);
		final int[] tex = new int[1];
		GLES30.glActiveTexture(texUnit);
		GLES30.glGenTextures(1, tex, 0);
		GLES30.glBindTexture(texTarget, tex[0]);
		GLES30.glTexParameteri(texTarget, GLES30.GL_TEXTURE_WRAP_S, wrap);
		GLES30.glTexParameteri(texTarget, GLES30.GL_TEXTURE_WRAP_T, wrap);
		GLES30.glTexParameteri(texTarget, GLES30.GL_TEXTURE_MIN_FILTER, min_filter);
		GLES30.glTexParameteri(texTarget, GLES30.GL_TEXTURE_MAG_FILTER, mag_filter);
		return tex[0];
	}
	
	/**
	 * テクスチャ名配列を生成(前から順にGL_TEXTURE0, GL_TEXTURE1, ...)
	 * @param n 生成するテキスチャ名の数, 最大で32個(GL_MAX_TEXTURE_IMAGE_UNITS以下)
	 * @param texTarget
	 * @param filter_param
	 * @return
	 */
	public static int[] initTexes(final int n,
		final int texTarget, final int filter_param) {
		
		return initTexes(new int[n], texTarget,
			filter_param, filter_param, GLES30.GL_CLAMP_TO_EDGE);
	}

	/**
	 * テクスチャ名配列を生成(前から順にGL_TEXTURE0, GL_TEXTURE1, ...)
	 * @param texIds テクスチャ名配列, 最大で32個(GL_MAX_TEXTURE_IMAGE_UNITS以下)
	 * @param texTarget
	 * @param filter_param
	 * @return
	 */
	public static int[] initTexes(@NonNull final int[] texIds,
		final int texTarget, final int filter_param) {
		
		return initTexes(texIds, texTarget,
			filter_param, filter_param, GLES30.GL_CLAMP_TO_EDGE);
	}

	/**
	 * テクスチャ名配列を生成(前から順にGL_TEXTURE0, GL_TEXTURE1, ...)
 	 * @param n 生成するテキスチャ名の数, 最大32
	 * @param texTarget
	 * @param min_filter
	 * @param mag_filter
	 * @param wrap
	 * @return
	 */
	public static int[] initTexes(final int n,
		final int texTarget, final int min_filter, final int mag_filter, final int wrap) {
		
		return initTexes(new int[n], texTarget, min_filter, mag_filter, wrap);
	}

	/**
	 * テクスチャ名配列を生成(前から順にGL_TEXTURE0, GL_TEXTURE1, ...)
	 * @param texIds テクスチャ名配列, 最大で32個(GL_MAX_TEXTURE_IMAGE_UNITS以下)
	 * @param texTarget
	 * @param min_filter
	 * @param mag_filter
	 * @param wrap
	 * @return
	 */
	public static int[] initTexes(@NonNull final int[] texIds,
		final int texTarget, final int min_filter, final int mag_filter, final int wrap) {

		int[] textureUnits = new int[1];
		GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_IMAGE_UNITS, textureUnits, 0);
		Log.v(TAG, "GL_MAX_TEXTURE_IMAGE_UNITS=" + textureUnits[0]);
		final int n = texIds.length > textureUnits[0]
			? textureUnits[0] : texIds.length;
		for (int i = 0; i < n; i++) {
			texIds[i] = GLHelper.initTex(texTarget, ShaderConst.TEX_NUMBERS[i],
				min_filter, mag_filter, wrap);
		}
		return texIds;
	}
	
	/**
	 * テクスチャ名配列を生成(こっちは全部同じテクスチャユニット)
	 * @param n 最大で32個(GL_MAX_TEXTURE_IMAGE_UNITS以下)
	 * @param texTarget
	 * @param texUnit
	 * @param min_filter
	 * @param mag_filter
	 * @param wrap
	 * @return
	 */
	public static int[] initTexes(final int n,
		final int texTarget, final int texUnit,
			final int min_filter, final int mag_filter, final int wrap) {

		return initTexes(new int[n], texTarget, texUnit,
			min_filter, mag_filter, wrap);
	}
	
	/**
	 * テクスチャ名配列を生成(こっちは全部同じテクスチャユニット)
	 * @param texIds 最大で32個(GL_MAX_TEXTURE_IMAGE_UNITS以下)
	 * @param texTarget
	 * @param texUnit
	 * @param filter_param
	 * @return
	 */
	public static int[] initTexes(@NonNull final int[] texIds,
		final int texTarget, final int texUnit, final int filter_param) {
		
		return initTexes(texIds, texTarget, texUnit,
			filter_param, filter_param, GLES30.GL_CLAMP_TO_EDGE);
	}
	
	/**
	 * テクスチャ名配列を生成(こっちは全部同じテクスチャユニット)
	 * @param texIds
	 * @param texTarget
	 * @param texUnit
	 * @param min_filter
	 * @param mag_filter
	 * @param wrap
	 * @return
	 */
	public static int[] initTexes(@NonNull final int[] texIds,
		final int texTarget, final int texUnit,
		final int min_filter, final int mag_filter, final int wrap) {

		int[] textureUnits = new int[1];
		GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_IMAGE_UNITS, textureUnits, 0);
		final int n = texIds.length > textureUnits[0]
			? textureUnits[0] : texIds.length;
		for (int i = 0; i < n; i++) {
			texIds[i] = GLHelper.initTex(texTarget, texUnit,
				min_filter, mag_filter, wrap);
		}
		return texIds;
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		GLES30.glDeleteTextures(1, tex, 0);
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(@NonNull final int[] tex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		GLES30.glDeleteTextures(tex.length, tex, 0);
	}

	public static int loadTextureFromResource(final Context context, final int resId) {
		return loadTextureFromResource(context, resId, null);
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static int loadTextureFromResource(final Context context, final int resId, final Resources.Theme theme) {
		// Create an empty, mutable bitmap
		final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		final Canvas canvas = new Canvas(bitmap);
		canvas.drawARGB(0,0,255,0);

		// get a background image from resources
		// note the image format must match the bitmap format
		final Drawable background;
		if (BuildCheck.isAndroid5()) {
			background = context.getResources().getDrawable(resId, theme);
		} else {
			background = context.getResources().getDrawable(resId);
		}
		background.setBounds(0, 0, 256, 256);
		background.draw(canvas); // draw the background to our bitmap

		final int[] textures = new int[1];

		//Generate one texture pointer...
		GLES30.glGenTextures(1, textures, 0);
		//...and bind it to our array
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);

		//Create Nearest Filtered Texture
		GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
			GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
		GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
			GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

		//Different possible texture parameters, e.g. GLES30.GL_CLAMP_TO_EDGE
		GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
			GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
		GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
			GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
		//Clean up
		bitmap.recycle();

		return textures[0];
	}

	public static int createTextureWithTextContent (final String text) {
		// Create an empty, mutable bitmap
		final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		final Canvas canvas = new Canvas(bitmap);
		canvas.drawARGB(0,0,255,0);

		// Draw the text
		final Paint textPaint = new Paint();
		textPaint.setTextSize(32);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
		// draw the text centered
		canvas.drawText(text, 16, 112, textPaint);

		final int texture = initTex(GLES30.GL_TEXTURE_2D,
			GLES30.GL_TEXTURE0, GLES30.GL_NEAREST, GLES30.GL_LINEAR, GLES30.GL_REPEAT);

		// Alpha blending
		// GLES30.glEnable(GLES30.GL_BLEND);
		// GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

		// Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
		// Clean up
		bitmap.recycle();

		return texture;
	}

	/**
	 * load, compile and link shader from Assets files
	 * @param context
	 * @param vss_asset source file name in Assets of vertex shader
	 * @param fss_asset source file name in Assets of fragment shader
	 * @return
	 */
	public static int loadShader(@NonNull final Context context,
		final String vss_asset, final String fss_asset) {

		int program = 0;
		try {
			final String vss = AssetsHelper.loadString(context.getAssets(), vss_asset);
			final String fss = AssetsHelper.loadString(context.getAssets(), vss_asset);
			program = loadShader(vss, fss);
		} catch (IOException e) {
		}
		return program;
	}

	/**
	 * load, compile and link shader
	 * @param vss source of vertex shader
	 * @param fss source of fragment shader
	 * @return
	 */
	public static int loadShader(final String vss, final String fss) {
//		if (DEBUG) Log.v(TAG, "loadShader:");
		final int[] compiled = new int[1];
		// 頂点シェーダーをコンパイル
		final int vs = loadShader(GLES30.GL_VERTEX_SHADER, vss);
		if (vs == 0) {
			return 0;
		}
		// フラグメントシェーダーをコンパイル
		int fs = loadShader(GLES30.GL_FRAGMENT_SHADER, fss);
		if (fs == 0) {
			return 0;
		}
		// リンク
		final int program = GLES30.glCreateProgram();
		checkGlError("glCreateProgram");
		if (program == 0) {
			Log.e(TAG, "Could not create program");
		}
		GLES30.glAttachShader(program, vs);
		checkGlError("glAttachShader");
		GLES30.glAttachShader(program, fs);
		checkGlError("glAttachShader");
		GLES30.glLinkProgram(program);
		final int[] linkStatus = new int[1];
		GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] != GLES30.GL_TRUE) {
			Log.e(TAG, "Could not link program: ");
			Log.e(TAG, GLES30.glGetProgramInfoLog(program));
			GLES30.glDeleteProgram(program);
			return 0;
		}
		return program;
	}

	/**
	  * Compiles the provided shader source.
	  *
	  * @return A handle to the shader, or 0 on failure.
	  */
	public static int loadShader(final int shaderType, final String source) {
		int shader = GLES30.glCreateShader(shaderType);
		checkGlError("glCreateShader type=" + shaderType);
		GLES30.glShaderSource(shader, source);
		GLES30.glCompileShader(shader);
		final int[] compiled = new int[1];
		GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			Log.e(TAG, "Could not compile shader " + shaderType + ":");
			Log.e(TAG, " " + GLES30.glGetShaderInfoLog(shader));
			GLES30.glDeleteShader(shader);
			shader = 0;
		}
		return shader;
	 }

	/**
	 * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
	 * could not be found, but does not set the GL error.
	 * <p>
	 * Throws a RuntimeException if the location is invalid.
	 */
	public static void checkLocation(final int location, final String label) {
		if (location < 0) {
			throw new RuntimeException("Unable to locate '" + label + "' in program");
		}
	}

	/**
	 * Writes GL version info to the log.
	 */
	@SuppressLint("InlinedApi")
	public static void logVersionInfo() {
		Log.i(TAG, "vendor  : " + GLES30.glGetString(GLES30.GL_VENDOR));
		Log.i(TAG, "renderer: " + GLES30.glGetString(GLES30.GL_RENDERER));
		Log.i(TAG, "version : " + GLES30.glGetString(GLES30.GL_VERSION));

		if (BuildCheck.isAndroid4_3()) {
			final int[] values = new int[1];
			GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
			final int majorVersion = values[0];
			GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
			final int minorVersion = values[0];
			if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
				Log.i(TAG, "version: " + majorVersion + "." + minorVersion);
			}
		}
	}
}
