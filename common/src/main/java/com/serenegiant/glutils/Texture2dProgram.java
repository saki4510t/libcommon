package com.serenegiant.glutils;
/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.FloatBuffer;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
    private static final String TAG = "Texture2dProgram";

	public enum ProgramType {
		// ここはGL_TEXTURE_2D
        TEXTURE_2D,
//		TEXTURE_SOBEL,		// フラグメントシェーダーがうまく走らなくて止まってしまう
//		TEXTURE_SOBEL2,		// フラグメントシェーダーがうまく走らなくて止まってしまう
        TEXTURE_FILT3x3,
        TEXTURE_CUSTOM,
        // ここから下はGL_TEXTURE_EXTERNAL_OES
		TEXTURE_EXT,
		TEXTURE_EXT_BW,
		TEXTURE_EXT_NIGHT,
		TEXTURE_EXT_CHROMA_KEY,
        TEXTURE_EXT_SQUEEZE,
        TEXTURE_EXT_TWIRL,
        TEXTURE_EXT_TUNNEL,
        TEXTURE_EXT_BULGE,
        TEXTURE_EXT_DENT,
        TEXTURE_EXT_FISHEYE,
        TEXTURE_EXT_STRETCH,
        TEXTURE_EXT_MIRROR,
//		TEXTURE_EXT_SOBEL,		// フラグメントシェーダーがうまく走らなくて止まってしまう
//		TEXTURE_EXT_SOBEL2,		// フラグメントシェーダーがうまく走らなくて止まってしまう
		TEXTURE_EXT_FILT3x3,
    }

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
    	"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER_2D
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_BW_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
		"    gl_FragColor = vec4(color, color, color, 1.0);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER_BW
		= String.format(FRAGMENT_SHADER_BW_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_BW
		= String.format(FRAGMENT_SHADER_BW_BASE, HEADER_OES, SAMPLER_OES);

    // Fragment shader that attempts to produce a high contrast image
    private static final String FRAGMENT_SHADER_NIGHT_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
		"    gl_FragColor = vec4(color, color + 0.15, color, 1.0);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER_NIGHT
		= String.format(FRAGMENT_SHADER_NIGHT_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_NIGHT
		= String.format(FRAGMENT_SHADER_NIGHT_BASE, HEADER_OES, SAMPLER_OES);

    // Fragment shader that applies a Chroma Key effect, making green pixels transparent
    private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
		"    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){ \n" +
		"        gl_FragColor = vec4(0, 0, 0, 0.0);\n" +
		"    }else{ \n" +
		"        gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"    }\n" +
		"}\n";
	private static final String FRAGMENT_SHADER_CHROMA_KEY
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_CHROMA_KEY
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE, HEADER_OES, SAMPLER_OES);

    private static final String FRAGMENT_SHADER_SQUEEZE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = pow(r, 1.0/1.8) * 0.8;\n"+  // Squeeze it
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";
	private static final String FRAGMENT_SHADER_SQUEEZE
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_SQUEEZE
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE, HEADER_OES, SAMPLER_OES);

    private static final String FRAGMENT_SHADER_EXT_TWIRL =
		"#version 100\n" +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;\n"+ // Twirl it
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_TUNNEL = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    if (r > 0.5) r = 0.5;\n"+ // Tunnel
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_BULGE = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = r * smoothstep(-0.1, 0.5, r);\n"+ // Bulge
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_DENT = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);\n"+ // Dent
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_FISHEYE = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = r * r / sqrt(2.0);\n"+ // Fisheye
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_STRETCH = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    vec2 s = sign(normCoord + uPosition);\n"+
		"    normCoord = abs(normCoord);\n"+
		"    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;\n"+
		"    normCoord = s * normCoord;\n"+
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

    private static final String FRAGMENT_SHADER_EXT_MIRROR = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);\n"+
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	private static final String FRAGMENT_SHADER_SOBEL_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    vec3 t0 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;\n" +
		"    vec3 t1 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;\n" +
		"    vec3 t2 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;\n" +
		"    vec3 t3 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;\n" +
		"    vec3 t4 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;\n" +
		"    vec3 t5 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;\n" +
		"    vec3 t6 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;\n" +
		"    vec3 t7 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;\n" +
		"    vec3 t8 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;\n" +
		"    vec3 sumH = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]\n" +
		"              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]\n" +
		"              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];\n" +
//		"    vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]\n" +
//		"              + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]\n" +
//		"              + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];\n" +
//		"    float mag = length(abs(sumH) + abs(sumV));\n" +
		"    float mag = length(sumH);\n" +
		"    gl_FragColor = vec4(vec3(mag), 1.0);\n" +
		"}\n";

	private static final String FRAGMENT_SHADER_SOBEL
		= String.format(FRAGMENT_SHADER_SOBEL_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_SOBEL
		= String.format(FRAGMENT_SHADER_SOBEL_BASE, HEADER_OES, SAMPLER_OES);

	public static final float[] KERNEL_NULL = { 0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f};
	public static final float[] KERNEL_SOBEL_H = { 1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f, };	// ソーベル(1次微分)
	public static final float[] KERNEL_SOBEL_V = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f, };
	public static final float[] KERNEL_SOBEL2_H = { 3f, 0f, -3f, 10f, 0f, -10f, 3f, 0f, -3f, };
	public static final float[] KERNEL_SOBEL2_V = { 3f, 10f, 3f, 0f, 0f, 0f, -3f, -10f, -3f, };
	public static final float[] KERNEL_SHARPNESS = { 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f,};	// シャープネス
	public static final float[] KERNEL_EDGE_DETECT = { -1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f, }; // エッジ検出
	public static final float[] KERNEL_EMBOSS = { 2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f };	// エンボス, オフセット0.5f
	public static final float[] KERNEL_SMOOTH = { 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, };	// 移動平均
	public static final float[] KERNEL_GAUSSIAN = { 1/16f, 2/16f, 1/16f, 2/16f, 4/16f, 2/16f, 1/16f, 2/16f, 1/16f, };	// ガウシアン(ノイズ除去/)
	public static final float[] KERNEL_BRIGHTEN = { 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, };
	public static final float[] KERNEL_LAPLACIAN = { 1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f, };	// ラプラシアン(2次微分)

	private static final String FRAGMENT_SHADER_FILT3x3_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    vec4 sum = vec4(0.0);\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[0]) * uKernel[0];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[1]) * uKernel[1];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[2]) * uKernel[2];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[3]) * uKernel[3];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[4]) * uKernel[4];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[5]) * uKernel[5];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[6]) * uKernel[6];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[7]) * uKernel[7];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[8]) * uKernel[8];\n" +
		"    gl_FragColor = sum + uColorAdjust;\n" +
		"}\n";
	private static final String FRAGMENT_SHADER_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_OES, SAMPLER_OES);

	private final Object mSync = new Object();
	private final ProgramType mProgramType;

    private float mTexWidth;
    private float mTexHeight;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private final int muMVPMatrixLoc;	// モデルビュー変換行列
    private final int muTexMatrixLoc;	// テクスチャ行列
	private final int maPositionLoc;	//
	private final int maTextureCoordLoc;//
    private int muKernelLoc;			// カーネル行列(float配列)
    private int muTexOffsetLoc;			// テクスチャオフセット(カーネル行列用)
    private int muColorAdjustLoc;		// 色調整
    private int muTouchPositionLoc;
	private int muFlagsLoc;

    private int mTextureTarget;

	protected boolean mHasKernel2;
    private final float[] mKernel = new float[KERNEL_SIZE3x3 * 2];	// Inputs for convolution filter based shaders
    private final float[] mSummedTouchPosition = new float[2];	// Summed touch event delta
    private final float[] mLastTouchPosition = new float[2];	// Raw location of last touch event
    private float[] mTexOffset;
    private float mColorAdjust;
    private final int[] mFlags = new int[4];

	public Texture2dProgram(final int target, final String fss) {
		this(ProgramType.TEXTURE_CUSTOM, target, VERTEX_SHADER, fss);
	}

	public Texture2dProgram(final int target, final String vss, final String fss) {
		this(ProgramType.TEXTURE_CUSTOM, target, vss, fss);
	}

	public Texture2dProgram(final ProgramType programType) {
		this(programType, 0, null, null);
	}

    /**
     * Prepares the program in the current EGL context.
     */
    protected Texture2dProgram(final ProgramType programType, final int target, final String vss, final String fss) {
		mProgramType = programType;

		float[] kernel = null, kernel2 = null;
		switch (programType) {
			case TEXTURE_2D:
				mTextureTarget = GLES20.GL_TEXTURE_2D;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
//			case TEXTURE_SOBEL:
//				mTextureTarget = GLES20.GL_TEXTURE_2D;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SOBEL);
//				kernel = KERNEL_SOBEL_H;
//				kernel2 = KERNEL_SOBEL_V;
//				break;
//			case TEXTURE_SOBEL2:
//				mTextureTarget = GLES20.GL_TEXTURE_2D;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SOBEL);
//				kernel = KERNEL_SOBEL2_H;
//				kernel2 = KERNEL_SOBEL2_V;
//				break;
			case TEXTURE_FILT3x3:
				mTextureTarget = GLES20.GL_TEXTURE_2D;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_FILT3x3);
				break;
			case TEXTURE_EXT:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
				break;
			case TEXTURE_EXT_BW:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
				break;
			case TEXTURE_EXT_NIGHT:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_NIGHT);
				break;
			case TEXTURE_EXT_CHROMA_KEY:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_CHROMA_KEY);
				break;
			case TEXTURE_EXT_SQUEEZE:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SQUEEZE);
				break;
			case TEXTURE_EXT_TWIRL:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_TWIRL);
				break;
			case TEXTURE_EXT_TUNNEL:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_TUNNEL);
				break;
			case TEXTURE_EXT_BULGE:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BULGE);
				break;
			case TEXTURE_EXT_FISHEYE:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FISHEYE);
				break;
			case TEXTURE_EXT_DENT:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_DENT);
				break;
			case TEXTURE_EXT_MIRROR:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_MIRROR);
				break;
			case TEXTURE_EXT_STRETCH:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_STRETCH);
				break;
//			case TEXTURE_EXT_SOBEL:
//				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SOBEL);
//				kernel = KERNEL_SOBEL_H;
//				kernel2 = KERNEL_SOBEL_V;
//				break;
//			case TEXTURE_EXT_SOBEL2:
//				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SOBEL);
//				kernel = KERNEL_SOBEL2_H;
//				kernel2 = KERNEL_SOBEL2_V;
//				break;
			case TEXTURE_EXT_FILT3x3:
				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT3x3);
				break;
			case TEXTURE_CUSTOM:
				switch (target) {
				case GLES20.GL_TEXTURE_2D:
				case GLES11Ext.GL_TEXTURE_EXTERNAL_OES:
					break;
				default:
					throw new IllegalArgumentException("target should be GL_TEXTURE_2D or GL_TEXTURE_EXTERNAL_OES");
				}
				mTextureTarget = target;
				mProgramHandle = GLHelper.loadShader(vss, fss);
				break;
			default:
				throw new RuntimeException("Unhandled type " + programType);
		}
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        if (DEBUG) Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms
		maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
		GLHelper.checkLocation(maPositionLoc, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
		GLHelper.checkLocation(maTextureCoordLoc, "aTextureCoord");
		muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
		GLHelper.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
		muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
//		GLHelper.checkLocation(muTexMatrixLoc, "uTexMatrix");
        initLocation(kernel, kernel2);

    }

    /**
     * Releases the program.
     */
    public void release() {
        if (DEBUG) Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

	public int getProgramHandle() {
		return mProgramHandle;
	}

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject() {
        final int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
		GLHelper.checkGlError("glGenTextures");

        final int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
		GLHelper.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLHelper.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the effect offset
     *
     * This only has an effect for programs that
     * use positional effects like SQUEEZE and MIRROR
     */
    public void handleTouchEvent(final MotionEvent ev){
		synchronized (mSync) {
			if (ev.getAction() == MotionEvent.ACTION_MOVE){
				// A finger is dragging about
				if (mTexHeight != 0 && mTexWidth != 0){
					mSummedTouchPosition[0] += (2 * (ev.getX() - mLastTouchPosition[0])) / mTexWidth;
					mSummedTouchPosition[1] += (2 * (ev.getY() - mLastTouchPosition[1])) / -mTexHeight;
					mLastTouchPosition[0] = ev.getX();
					mLastTouchPosition[1] = ev.getY();
				}
			} else if (ev.getAction() == MotionEvent.ACTION_DOWN){
				// The primary finger has landed
				mLastTouchPosition[0] = ev.getX();
				mLastTouchPosition[1] = ev.getY();
			}
		}
    }

    /**
     * Configures the convolution filter values.
     * This only has an effect for programs that use the
     * FRAGMENT_SHADER_EXT_FILT3x3 Fragment shader.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE3x3 elements.
     */
    public void setKernel(final float[] values, final float colorAdj) {
        if (values.length < KERNEL_SIZE3x3) {
            throw new IllegalArgumentException(
            	"Kernel size is " + values.length + " vs. " + KERNEL_SIZE3x3);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE3x3);
        mColorAdjust = colorAdj;
    }

	public void setKernel2(final float[] values) {
		synchronized (mSync) {
			mHasKernel2 = values != null && (values.length == KERNEL_SIZE3x3);
			if (mHasKernel2) {
				System.arraycopy(values, 0, mKernel, KERNEL_SIZE3x3, KERNEL_SIZE3x3);
			}
		}
	}

	public void setColorAdjust(final float adjust) {
		synchronized (mSync) {
			mColorAdjust = adjust;
		}
	}

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(final int width, final int height) {
        mTexHeight = height;
        mTexWidth = width;
        final float rw = 1.0f / width;
        final float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
		synchronized (mSync) {
			mTexOffset = new float[] {
					-rw, -rh,   0f, -rh,    rw, -rh,
					-rw, 0f,    0f, 0f,     rw, 0f,
					-rw, rh,    0f, rh,     rw, rh
			};
		}
    }

	public void setFlags(final int[] flags) {
		final int n = Math.min(4, flags != null ? flags.length : 0);
		if (n > 0) {
			synchronized (mSync) {
				System.arraycopy(flags, 0, mFlags, 0, n);
			}
		}
	}

	public void setFlag(final int index, final int value) {
		if ((index >= 0) && (index < mFlags.length)) {
			synchronized (mSync) {
				mFlags[index] = value;
			}
		}
	}

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
	 * @param mvpMatrixOffset offset of mvpMatrix
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.
	 * @param texMatrixOffset offset of texMatrix
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    public void draw(final float[] mvpMatrix, final int mvpMatrixOffset, final FloatBuffer vertexBuffer, final int firstVertex,
                     final int vertexCount, final int coordsPerVertex, final int vertexStride,
                     final float[] texMatrix, final int texMatrixOffset, final FloatBuffer texBuffer, final int textureId, final int texStride) {
		GLHelper.checkGlError("draw start");

        // シェーダープログラムを選択
        GLES20.glUseProgram(mProgramHandle);
		GLHelper.checkGlError("glUseProgram");

        // テクスチャを選択
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);
		GLHelper.checkGlError("glBindTexture");

		synchronized (mSync) {
			// モデルビュー変換行列をセット
			GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, mvpMatrixOffset);
			GLHelper.checkGlError("glUniformMatrix4fv");

			// テクスチャ変換行列をセット
			if (muTexMatrixLoc >= 0) {
				GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texMatrixOffset);
				GLHelper.checkGlError("glUniformMatrix4fv");
			}

			// 頂点座標バッファを有効にする("aPosition" vertex attribute)
			GLES20.glEnableVertexAttribArray(maPositionLoc);
			GLHelper.checkGlError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLHelper.checkGlError("glVertexAttribPointer");

			// テクスチャ座標バッファを有効にする("aTextureCoord" vertex attribute)
			GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
			GLHelper.checkGlError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
					GLES20.GL_FLOAT, false, texStride, texBuffer);
			GLHelper.checkGlError("glVertexAttribPointer");

			// カーネル関数(行列)
			if (muKernelLoc >= 0) {
				if (!mHasKernel2) {
					GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE3x3, mKernel, 0);
				} else {
					GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE3x3 * 2, mKernel, 0);
				}
				GLHelper.checkGlError("set kernel");
			}
			// テクセルオフセット
			if ((muTexOffsetLoc >= 0) && (mTexOffset != null)) {
				GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE3x3, mTexOffset, 0);
			}
			// 色調整オフセット
			if (muColorAdjustLoc >= 0) {
				GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
			}
			// タッチ座標
			if (muTouchPositionLoc >= 0){
				GLES20.glUniform2fv(muTouchPositionLoc, 1, mSummedTouchPosition, 0);
			}
			// フラグ
			if (muFlagsLoc >= 0) {
				GLES20.glUniform1iv(muFlagsLoc, 4, mFlags, 0);
			}
		}

		internal_draw(firstVertex, vertexCount);

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

	protected void initLocation(float[] kernel, float[] kernel2) {
		muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
		if (muKernelLoc < 0) {
			// no kernel in this one
			muKernelLoc = -1;
			muTexOffsetLoc = -1;
		} else {
			// has kernel, must also have tex offset and color adj
			muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
			if (muTexOffsetLoc < 0) {
				muTexOffsetLoc = -1;
			}
//			GLHelper.checkLocation(muTexOffsetLoc, "uTexOffset");	// 未使用だと削除されてしまうのでチェックしない

			// initialize default values
			if (kernel == null) {
				kernel = KERNEL_NULL;
			}
			setKernel(kernel, 0f);
			setTexSize(256, 256);
			}
			if (kernel2 != null) {
				setKernel2(kernel2);
			}

			muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
			if (muColorAdjustLoc < 0) {
				muColorAdjustLoc = -1;
			}
//			GLHelper.checkLocation(muColorAdjustLoc, "uColorAdjust");	// 未使用だと削除されてしまうのでチェックしない

			muTouchPositionLoc = GLES20.glGetUniformLocation(mProgramHandle, "uPosition");
			if (muTouchPositionLoc < 0) {
				// Shader doesn't use position
				muTouchPositionLoc = -1;
			} else {
				// initialize default values
				//handleTouchEvent(new float[]{0f, 0f});
			}
			muFlagsLoc = GLES20.glGetUniformLocation(mProgramHandle, "uFlags");
			if (muFlagsLoc < 0) {
				muFlagsLoc = -1;
			} else {
		}
	}

    protected void internal_draw(final int firstVertex, final int vertexCount) {
		// Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
		GLHelper.checkGlError("glDrawArrays");
	}
}