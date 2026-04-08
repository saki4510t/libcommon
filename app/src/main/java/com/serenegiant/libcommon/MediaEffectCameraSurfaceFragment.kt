package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.content.Context
import android.graphics.Color
import android.media.effect.EffectContext
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.serenegiant.gl.GLEffect
import com.serenegiant.gl.ShaderConst
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.mediaeffect.EffectsBuilder
import com.serenegiant.mediaeffect.IMediaEffect
import com.serenegiant.mediaeffect.MediaEffectAutoFix
import com.serenegiant.mediaeffect.MediaEffectBitmapOverlay
import com.serenegiant.mediaeffect.MediaEffectBlackWhite
import com.serenegiant.mediaeffect.MediaEffectContrast
import com.serenegiant.mediaeffect.MediaEffectCrop
import com.serenegiant.mediaeffect.MediaEffectCrossProcess
import com.serenegiant.mediaeffect.MediaEffectDocumentary
import com.serenegiant.mediaeffect.MediaEffectDuoTone
import com.serenegiant.mediaeffect.MediaEffectFillLight
import com.serenegiant.mediaeffect.MediaEffectFishEye
import com.serenegiant.mediaeffect.MediaEffectFlip
import com.serenegiant.mediaeffect.MediaEffectFlipHorizontal
import com.serenegiant.mediaeffect.MediaEffectFlipVertical
import com.serenegiant.mediaeffect.MediaEffectGLAlphaBlend
import com.serenegiant.mediaeffect.MediaEffectGLBrightness
import com.serenegiant.mediaeffect.MediaEffectGLCanny
import com.serenegiant.mediaeffect.MediaEffectGLColorCorrection
import com.serenegiant.mediaeffect.MediaEffectGLColorMatrix
import com.serenegiant.mediaeffect.MediaEffectGLCrossProcess
import com.serenegiant.mediaeffect.MediaEffectGLDilation
import com.serenegiant.mediaeffect.MediaEffectGLDocumentary
import com.serenegiant.mediaeffect.MediaEffectGLEffect
import com.serenegiant.mediaeffect.MediaEffectGLEmboss
import com.serenegiant.mediaeffect.MediaEffectGLErosion
import com.serenegiant.mediaeffect.MediaEffectGLExposure
import com.serenegiant.mediaeffect.MediaEffectGLExtraction
import com.serenegiant.mediaeffect.MediaEffectGLHistogram
import com.serenegiant.mediaeffect.MediaEffectGLMaskedAlphaBlend
import com.serenegiant.mediaeffect.MediaEffectGLMedian3x3
import com.serenegiant.mediaeffect.MediaEffectGLNegative
import com.serenegiant.mediaeffect.MediaEffectGLNull
import com.serenegiant.mediaeffect.MediaEffectGLPosterize
import com.serenegiant.mediaeffect.MediaEffectGLSaturate
import com.serenegiant.mediaeffect.MediaEffectGLSphere
import com.serenegiant.mediaeffect.MediaEffectGLTexProjection
import com.serenegiant.mediaeffect.MediaEffectGLVignette
import com.serenegiant.mediaeffect.MediaEffectGrain
import com.serenegiant.mediaeffect.MediaEffectGrayScale
import com.serenegiant.mediaeffect.MediaEffectLomoish
import com.serenegiant.mediaeffect.MediaEffectNegative
import com.serenegiant.mediaeffect.MediaEffectNull
import com.serenegiant.mediaeffect.MediaEffectRedEye
import com.serenegiant.mediaeffect.MediaEffectRotate
import com.serenegiant.mediaeffect.MediaEffectSaturate
import com.serenegiant.mediaeffect.MediaEffectSepia
import com.serenegiant.mediaeffect.MediaEffectSharpen
import com.serenegiant.mediaeffect.MediaEffectStraighten
import com.serenegiant.mediaeffect.MediaEffectTemperature
import com.serenegiant.mediaeffect.MediaEffectTint
import com.serenegiant.mediaeffect.MediaEffectVignette
import com.serenegiant.widget.CameraDelegator
import com.serenegiant.widget.MediaEffectCameraSurfaceView

class MediaEffectCameraSurfaceFragment : BaseFragment() {

	private interface MyEffectsBuilder :
		EffectsBuilder {
		fun name(): String {
			return "No name"
		}
	}

	private lateinit var mCameraView: MediaEffectCameraSurfaceView
	private lateinit var mEffectNameTv: TextView

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = TAG
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val rootView = inflater.inflate(
			R.layout.fragment_camera_media_effect,
			container, false)
		mCameraView = rootView.findViewById(R.id.cameraView)
		mEffectNameTv = rootView.findViewById(R.id.effect_name_textview)
		return rootView
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		val builder = mEffectsBuilders[mEffectIx]
		mEffectNameTv.text = builder.name()
		mCameraView.changeEffect(builder)
		mCameraView.setOnLongClickListener { v ->
			onLongClickListener(v)
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCameraView.onResume()
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		mCameraView.onPause()
		super.internalOnPause()
	}

	private fun onLongClickListener(view: View): Boolean {
		when (view.id) {
			R.id.cameraView -> {
				mEffectIx = (mEffectIx + 1) % mEffectsBuilders.size
				val builder = mEffectsBuilders[mEffectIx]
				mEffectNameTv.text = builder.name()
				mCameraView.changeEffect(builder)
			}
		}
		return false
	}

	private var mEffectIx = 0
	private val mEffectsBuilders = listOf(
		object : MyEffectsBuilder{
			override fun name(): String { return "Non" }
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectAutoFix" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectAutoFix(
						effectContext,
						0.5f
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "EFFECT_ADAPTIVE_BIN" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLEffect(GLEffect.EFFECT_ADAPTIVE_BIN))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "EFFECT_KERNEL_SOBEL_HV" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLEffect(GLEffect.EFFECT_KERNEL_SOBEL_HV))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "EFFECT_BIN" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLEffect(GLEffect.EFFECT_BIN))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "EFFECT_KERNEL_CANNY_ENHANCE" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLEffect(GLEffect.EFFECT_KERNEL_CANNY_ENHANCE))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "AutoFix+Crop" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectAutoFix(
						effectContext,
						0.5f
					),
					MediaEffectCrop(effectContext,
						CameraDelegator.DEFAULT_PREVIEW_WIDTH / 4, CameraDelegator.DEFAULT_PREVIEW_HEIGHT / 4,
						CameraDelegator.DEFAULT_PREVIEW_WIDTH / 2, CameraDelegator.DEFAULT_PREVIEW_HEIGHT / 2),
				)
			}
		},
		// FIXME これはうまく動かない、urlから動画取得開始後しばらくしてから
		//       内部的にクラッシュする(android.media.effect.FilterGraphEffect.apply(FilterGraphEffect)
//		object : MyEffectsBuilder {
//			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
//				return mutableListOf(MediaEffectBackDropper(effectContext,
//					"https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
//				))
//			}
//		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectBitmapOverlay" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectBitmapOverlay(effectContext,
					BitmapHelper.genMaskImage(0,
						CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
						60, Color.BLUE,127, 255)
				)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectBlackWhite" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectBlackWhite(
						effectContext
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectContrast(1.2)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectContrast(effectContext, 1.2f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectCrop" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectCrop(effectContext,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH / 4, CameraDelegator.DEFAULT_PREVIEW_HEIGHT / 4,
					CameraDelegator.DEFAULT_PREVIEW_WIDTH / 2, CameraDelegator.DEFAULT_PREVIEW_HEIGHT / 2
				)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectCrossProcess" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectCrossProcess(effectContext))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectDocumentary" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectDocumentary(effectContext))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectDuoTone(0xffff0000,0xff00ff00)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectDuoTone(effectContext,
					0xffff0000.toInt(), 0xff00ff00.toInt()
				)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectFillLight" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				// FillLight tile size: 640とlogCatへ出力される
				return mutableListOf(MediaEffectFillLight(effectContext, 0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectFishEye" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectFishEye(effectContext, 0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectFlipHorizontal" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectFlipHorizontal(effectContext))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectFlipVertical" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectFlipVertical(
						effectContext
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectFlip(true,true)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectFlip(
						effectContext,
						true,
						true
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGrain(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGrain(effectContext, 0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGrayScale" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGrayScale(
						effectContext
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectLomoish" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectLomoish(effectContext))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectNegative" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectNegative(effectContext))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectNull" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectNull(
						effectContext
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectRedEye" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectRedEye(effectContext,
					floatArrayOf(0.33f, 0.33f, 0.66f, 0.33f)	// これは適当
				)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectRotate(90)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectRotate(effectContext, 90))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectSaturate(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectSaturate(
						effectContext,
						0.5f
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectSepia" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectSepia(
						effectContext
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectSharpen(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectSharpen(effectContext, 0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectStraighten(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectStraighten(effectContext, 0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectTemperature(0.75)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectTemperature(
						effectContext,
						0.75f
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectTint(0xff00ffff)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectTint(
						effectContext,
						0xff00ffff.toInt()
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectVignette(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectVignette(
						effectContext,
						0.5f
					)
				)
			}
		},
		// OpenGL|ESでの映像フィルタ処理
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLAlphaBlend(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLAlphaBlend(0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLBrightness(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLBrightness(0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLCanny(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLCanny(
						0.5f
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "COLOR_MATRIX_BLACK_COLOR" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLColorMatrix(ShaderConst.COLOR_MATRIX_BLACK_COLOR, 0))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "COLOR_MATRIX_GRAYSCALE" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLColorMatrix(ShaderConst.COLOR_MATRIX_GRAYSCALE, 0))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "COLOR_MATRIX_CONTRAST(0)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLColorMatrix(ShaderConst.COLOR_MATRIX_CONTRAST, 0))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "COLOR_MATRIX_CONTRAST(16)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLColorMatrix(ShaderConst.COLOR_MATRIX_CONTRAST, 16))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "COLOR_MATRIX_SEPIA" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLColorMatrix(ShaderConst.COLOR_MATRIX_SEPIA, 0))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(gamma=2.0)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setGamma(2.0f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(gamma=0.6)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setGamma(0.6f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(contrast=-0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setContrast(-0.5f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(contrast=0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setContrast(0.5f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(sigmoid=10.0,0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setSigmoid(10.0f, 0.5f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLColorCorrection(sigmoid=5.0,0.8)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLColorCorrection().apply {
					setSigmoid(5.0f, 0.8f)
				})
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLCrossProcess" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLCrossProcess())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLDilation(2)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLDilation(
						2
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLDocumentary(2)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLDocumentary())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLEmboss(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLEmboss(0.5f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLErosion(2)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLErosion(2))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLExposure(2.0)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLExposure(2.0f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLExtraction" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLExtraction())	// この設定だとなにもしない
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLHistogram(false)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					mutableListOf(MediaEffectGLHistogram(false))
				} else {
					super.buildEffects(effectContext)
				}
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLHistogram(false,true)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					mutableListOf(MediaEffectGLHistogram(false, true))
				} else {
					super.buildEffects(effectContext)
				}
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLMaskedAlphaBlend(no mask)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLMaskedAlphaBlend())	// マスク用テクスチャを指定していないので変化無し
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLMedian3x3" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLMedian3x3())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLNegative" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLNegative())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLNull" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLNull())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLPosterize(10.0)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLPosterize(10.0f))
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLSaturate(0.5)" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLSaturate(
						0.5f
					)
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLSphere" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(
					MediaEffectGLSphere(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH / CameraDelegator.DEFAULT_PREVIEW_HEIGHT.toFloat())
				)
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLTexProjection" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLTexProjection())
			}
		},
		object : MyEffectsBuilder {
			override fun name(): String { return "MediaEffectGLVignette" }
			override fun buildEffects(effectContext: EffectContext): MutableList<IMediaEffect> {
				return mutableListOf(MediaEffectGLVignette())
			}
		},
	)

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = MediaEffectCameraSurfaceFragment::class.java.simpleName

		fun newInstance() = MediaEffectCameraSurfaceFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}

}
