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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.serenegiant.utils.BuildCheck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * OpenGL|ES用のヘルパークラス
 */
public final class GL1Helper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = "GLHelper";

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param op
	 */
    public static void checkGlError(final String op) {
        final int error = GLES10.glGetError();
        if (error != GLES10.GL_NO_ERROR) {
            final String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg);
            new Throwable(msg).printStackTrace();
//         	if (DEBUG) {
//	            throw new RuntimeException(msg);
//       	}
        }
    }

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param gl
	 * @param op
	 */
	public static void checkGlError(final GL10 gl, final String op) {
		final int error = gl.glGetError();
		if (error != GL10.GL_NO_ERROR) {
			final String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg);
			new Throwable(msg).printStackTrace();
//         	if (DEBUG) {
//	            throw new RuntimeException(msg);
//       	}
		}
	}

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param op
	 */
	public static void checkGlErrorV1(final String op) {
		final int error = GLES10.glGetError();
		if (error != GLES10.GL_NO_ERROR) {
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
		return initTex(texTarget, GLES10.GL_TEXTURE0, filter_param, filter_param, GLES10.GL_CLAMP_TO_EDGE);
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
	public static int initTex(final int texTarget, final int texUnit, final int min_filter, final int mag_filter, final int wrap) {
//		if (DEBUG) Log.v(TAG, "initTex:target=" + texTarget);
		final int[] tex = new int[1];
		GLES10.glActiveTexture(texUnit);
		GLES10.glGenTextures(1, tex, 0);
		GLES10.glBindTexture(texTarget, tex[0]);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_S, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_T, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MIN_FILTER, min_filter);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MAG_FILTER, mag_filter);
		return tex[0];
	}

	/**
	 * テクスチャ名を生成(GL_TEXTURE0のみ)
	 * @param gl
	 * @param texTarget
	 * @param filter_param テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 * @return
	 */
	public static int initTex(final GL10 gl, final int texTarget, final int filter_param) {
//		if (DEBUG) Log.v(TAG, "initTex:target=" + texTarget);
		final int[] tex = new int[1];
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glGenTextures(1, tex, 0);
		gl.glBindTexture(texTarget, tex[0]);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_MIN_FILTER, filter_param);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_MAG_FILTER, filter_param);
		return tex[0];
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		GLES10.glDeleteTextures(1, tex, 0);
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final GL10 gl, final int hTex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		gl.glDeleteTextures(1, tex, 0);
	}

	public static int loadTextureFromResource(final Context context, final int resId) {
		// Create an empty, mutable bitmap
		final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		final Canvas canvas = new Canvas(bitmap);
		canvas.drawARGB(0,0,255,0);

		// get a background image from resources
		// note the image format must match the bitmap format
		final Drawable background = context.getResources().getDrawable(resId);
		background.setBounds(0, 0, 256, 256);
		background.draw(canvas); // draw the background to our bitmap

		final int[] textures = new int[1];

		//Generate one texture pointer...
		GLES10.glGenTextures(1, textures, 0);
		//...and bind it to our array
		GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textures[0]);

		//Create Nearest Filtered Texture
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);

		//Different possible texture parameters, e.g. GLES10.GL_CLAMP_TO_EDGE
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_REPEAT);
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_REPEAT);

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
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

		final int texture = initTex(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE0, GLES10.GL_NEAREST, GLES10.GL_LINEAR, GLES10.GL_REPEAT);

		// Alpha blending
		// GLES10.glEnable(GLES10.GL_BLEND);
		// GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);

		// Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
		// Clean up
		bitmap.recycle();

		return texture;
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
	public static void logVersionInfo() {
		Log.i(TAG, "vendor  : " + GLES10.glGetString(GLES10.GL_VENDOR));
		Log.i(TAG, "renderer: " + GLES10.glGetString(GLES10.GL_RENDERER));
		Log.i(TAG, "version : " + GLES10.glGetString(GLES10.GL_VERSION));

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
