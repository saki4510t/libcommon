package com.serenegiant.widget
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

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.media.FaceDetector
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.View
import com.serenegiant.glpipeline.*
import com.serenegiant.gl.GLContext
import com.serenegiant.gl.GLDrawer2D
import com.serenegiant.gl.GLEffect
import com.serenegiant.gl.GLManager
import com.serenegiant.glpipeline.GLPipelineSurfaceSource.PipelineSourceCallback
import com.serenegiant.glutils.GLFrameAvailableCallback
import com.serenegiant.math.Fraction
import com.serenegiant.media.OnFrameAvailableListener
import com.serenegiant.view.TouchViewTransformer
import com.serenegiant.widget.SurfaceSourceCameraGLView.Companion

/**
 * VideoSourceを使ってカメラ映像を受け取りSurfacePipelineで描画処理を行うZoomAspectScaledTextureView/ICameraView実装
 */
class SimpleVideoSourceCameraTextureView @JvmOverloads constructor(
	context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
		: TouchTransformTextureView2(context, attrs, defStyleAttr), ICameraView, GLPipelineView {

	private val mGLManager: GLManager
	private val mGLContext: GLContext
	private val mGLHandler: Handler
	private val mCameraDelegator: CameraDelegator
	private var mSourcePipeline: GLPipelineSurfaceSource? = null
	var pipelineMode = GLPipelineView.PREVIEW_ONLY
	var enableFaceDetect = false

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")

		mGLManager = GLManager()
		mGLContext = mGLManager.glContext
		mGLHandler = mGLManager.glHandler
		setEnableHandleTouchEvent(TouchViewTransformer.TOUCH_ENABLED_ALL)
		mCameraDelegator = CameraDelegator(this@SimpleVideoSourceCameraTextureView,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			object : CameraDelegator.ICameraRenderer {
				override fun hasSurface(): Boolean {
					if (DEBUG) Log.v(TAG, "hasSurface:")
					return mSourcePipeline != null
				}

				override fun getInputSurface(): SurfaceTexture {
					if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:")
					checkNotNull(mSourcePipeline)
					return mSourcePipeline!!.inputSurfaceTexture
				}

				override fun onPreviewSizeChanged(width: Int, height: Int) {
					if (DEBUG) Log.v(TAG, "onPreviewSizeChanged:(${width}x${height})")
					mSourcePipeline!!.resize(width, height)
					setAspectRatio(width, height)
				}
			}
		)

		surfaceTextureListener = object : SurfaceTextureListener {
			override fun onSurfaceTextureAvailable(
				surface: SurfaceTexture, width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, "onSurfaceTextureAvailable:(${width}x${height})")
				val source = mSourcePipeline
				if (source != null) {
					addSurface(surface.hashCode(), surface, false)
					source.resize(width, height)
					mCameraDelegator.startPreview(
						CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
				} else {
					throw IllegalStateException("already released?")
				}
			}

			override fun onSurfaceTextureSizeChanged(
				surface: SurfaceTexture,
				width: Int, height: Int) {

				if (DEBUG) Log.v(TAG, "onSurfaceTextureSizeChanged:(${width}x${height})")
			}

			override fun onSurfaceTextureDestroyed(
				surface: SurfaceTexture): Boolean {

				if (DEBUG) Log.v(TAG, "onSurfaceTextureDestroyed:")
				removeSurface(surface.hashCode())
				return true
			}

			override fun onSurfaceTextureUpdated(
				surface: SurfaceTexture) {

//				if (DEBUG) Log.v(TAG, "onSurfaceTextureUpdated:")
			}

		}
	}

	override fun onDetachedFromWindow() {
		val source = mSourcePipeline
		mSourcePipeline = null
		source?.release()
		mGLManager.release()
		super.onDetachedFromWindow()
	}

	override fun getView() : View {
		return this
	}

	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mSourcePipeline = createSurfaceSource()
		if (USE_DRAWER_PIPELINE) {
			if (DEBUG) Log.v(TAG, "onResume:add DrawerPipeline")
			mSourcePipeline!!.append(DrawerPipeline(mGLManager, GLDrawer2D.DEFAULT_FACTORY))
		}
		mCameraDelegator.onResume()
	}

	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
		mSourcePipeline?.release()
		mSourcePipeline = null
	}

	override fun setVideoSize(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, "setVideoSize:(${width}x${height})")
		mCameraDelegator.setVideoSize(width, height)
	}

	override fun addListener(listener: OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "addListener:")
		mCameraDelegator.addListener(listener)
	}

	override fun removeListener(listener: OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "removeListener:")
		mCameraDelegator.removeListener(listener)
	}

	override fun setScaleMode(mode: Int) {
		if (DEBUG) Log.v(TAG, "setScaleMode:")
		super.setScaleMode(mode)
		mCameraDelegator.scaleMode = mode
	}

	override fun getScaleMode(): Int {
		if (DEBUG) Log.v(TAG, "getScaleMode:")
		return mCameraDelegator.scaleMode
	}

	override fun getVideoWidth(): Int {
		if (DEBUG) Log.v(TAG, "getVideoWidth:${mCameraDelegator.previewWidth}")
		return mCameraDelegator.previewWidth
	}

	override fun getVideoHeight(): Int {
		if (DEBUG) Log.v(TAG, "getVideoHeight:${mCameraDelegator.previewHeight}")
		return mCameraDelegator.previewHeight
	}

	override fun addSurface(
		id: Int, surface: Any,
		isRecordable: Boolean,
		maxFps: Fraction?) {

		// XXX AndroidのView座標系とOpenGL|ESの座標系ではY軸の方向が逆になっている。
		//     GL系のクラスはOpenGL|ES座標系で処理するのでViewへ表示させたり録画する場合には
		//     明示的にIMirror.MIRROR_VERTICALを指定して上下を反転させる必要がある。
		if (DEBUG) Log.v(TAG, "addSurface:id=${id},${surface}")
		val source = mSourcePipeline
		if (source != null) {
			val last = GLPipeline.findLast(source)
			if (DEBUG) Log.v(TAG, "addSurface:last=$last")
			when (last) {
				mSourcePipeline -> {
					source.pipeline = createPipeline(surface, maxFps)
					if (SUPPORT_RECORDING) {
						// 通常の録画(#addSurfaceでエンコーダーへの映像入力用surfaceを受け取る)場合は
						// 毎フレームCameraDelegator#callOnFrameAvailableを呼び出さないといけないので
						// OnFramePipelineを追加する
						if (DEBUG) Log.v(TAG, "addSurface:create OnFramePipeline")
						val p = GLPipeline.findLast(source)
						p.pipeline = OnFramePipeline(object: GLFrameAvailableCallback {
							var cnt: Int = 0

							override fun onFrameAvailable(
								isGLES3: Boolean,
								isOES: Boolean,
								width: Int, height: Int,
								texId: Int,
								texMatrix: FloatArray
							) {
								if (DEBUG && ((++cnt) % 100 == 0)) {
									Log.v(TAG, "onFrameAvailable:${cnt}")
								}
								mCameraDelegator.callOnFrameAvailable()
							}
						})
					}
					if (enableFaceDetect) {
						if (DEBUG) Log.v(TAG, "addSurface:create FaceDetectPipeline")
						val p = GLPipeline.findLast(source)
						p.pipeline = FaceDetectPipeline(mGLManager, Fraction.FIVE, 1
						) { /*bitmap,*/ num, faces, width, height ->
							if (DEBUG) {
								Log.v(TAG, "onDetected:n=${num}")
								for (i in 0 until num) {
									val face: FaceDetector.Face = faces[i]
									val point = PointF()
									face.getMidPoint(point)
									Log.v(TAG, "onDetected:sz((${width}x${height}),num=$i/${num}")
									Log.v(TAG, "onDetected:Confidence=" + face.confidence())
									Log.v(TAG, "onDetected:MidPoint.X=" + point.x)
									Log.v(TAG, "onDetected:MidPoint.Y=" + point.y)
									Log.v(TAG, "onDetected:EyesDistance=" + face.eyesDistance())
								}
							}
//							val context: Context = context
//							val outputFile: DocumentFile = if (BuildCheck.isAPI29()) {
//								// API29以降は対象範囲別ストレージ
//								MediaStoreUtils.getContentDocument(
//									context, "image/jpeg",
//									Environment.DIRECTORY_DCIM + "/" + Const.APP_DIR,
//									FileUtils.getDateTimeString() + ".jpg", null
//								)
//							} else {
//								MediaFileUtils.getRecordingFile(
//									context,
//									Const.REQUEST_ACCESS_SD,
//									Environment.DIRECTORY_DCIM,
//									"image/jpeg",
//									".jpg"
//								)
//							}
//							var bos: BufferedOutputStream? = null
//							try {
//								bos = BufferedOutputStream (context.contentResolver.openOutputStream(outputFile.uri))
//								bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
//								MediaStoreUtils.updateContentUri(context, outputFile.uri)
//							} catch (e: FileNotFoundException) {
//								Log.w(TAG, e);
//							} finally {
//								if (bos != null) {
//									try {
//										bos.close();
//									} catch (e: IOException) {
//										Log.w(TAG, e);
//									}
//								}
//							}
						}
					}
				}
				is GLSurfacePipeline -> {
					last.pipeline = SurfaceRendererPipeline(mGLManager, surface, maxFps)
				}
				else -> {
					last.pipeline = SurfaceRendererPipeline(mGLManager, surface, maxFps)
				}
			}
			if (DEBUG) Log.v(TAG, "addSurface:" + GLPipeline.pipelineString(source))
		} else {
			throw IllegalStateException("already released?")
		}
	}

	override fun removeSurface(id: Int) {
		if (DEBUG) Log.v(TAG, "removeSurface:id=${id}")
		val source = mSourcePipeline
		if (source != null) {
			val found = GLSurfacePipeline.findById(source, id)
			if (found != null) {
				found.remove()
				found.release()
			}
		}
	}

	override fun isRecordingSupported(): Boolean {
		// XXX ここでtrueを返すなら録画中にCameraDelegatorのOnFrameAvailableListener#onFrameAvailableが呼び出されるようにしないといけない
		return SUPPORT_RECORDING
	}

	/**
	 * GLPipelineViewの実装
	 * @param pipeline
	 */
	override fun addPipeline(pipeline: GLPipeline)  {
		val source = mSourcePipeline
		if (source != null) {
			GLPipeline.append(source, pipeline)
			if (DEBUG) Log.v(TAG, "addPipeline:" + GLPipeline.pipelineString(source))
		} else {
			throw IllegalStateException()
		}
	}

	/**
	 * GLPipelineViewの実装
	 */
	override fun getGLManager(): GLManager {
		return mGLManager
	}

	fun isEffectSupported(): Boolean {
		return (pipelineMode == GLPipelineView.EFFECT_ONLY)
			|| (pipelineMode == GLPipelineView.EFFECT_PLUS_SURFACE)
	}

	var effect: Int
	get() {
		val source = mSourcePipeline
		return if (source != null) {
			val pipeline = GLPipeline.find(source, EffectPipeline::class.java)
			if (DEBUG) Log.v(TAG, "getEffect:$pipeline")
			pipeline?.effect ?: 0
		} else {
			0
		}
	}

	set(effect) {
		if (DEBUG) Log.v(TAG, "setEffect:$effect")
		if ((effect >= 0) && (effect < GLEffect.EFFECT_NUM)) {
			post {
				val source = mSourcePipeline
				if (source != null) {
					val pipeline = GLPipeline.find(source, EffectPipeline::class.java)
					pipeline?.effect = effect
				}
			}
		}
	}


	override fun getContentBounds(): RectF {
		if (DEBUG) Log.v(TAG, "getContentBounds:")
		return RectF(0.0f, 0.0f, getVideoWidth().toFloat(), getVideoHeight().toFloat())
	}

	/**
	 * GLPipelineSurfaceSourceインスタンスを生成
	 * @return
	 */
	private fun createSurfaceSource(): GLPipelineSurfaceSource {
		val callback = object: PipelineSourceCallback {
			override fun onCreate(surface: Surface) {
				if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onCreate:$surface")
			}
			override fun onDestroy() {
				if (DEBUG) Log.v(TAG, "PipelineSourceCallback#onDestroy:")
			}
		}
		return if (USE_EFFECT) {
			if (DEBUG) Log.v(TAG, "createSurfaceSource:create SurfaceEffectSourcePipeline")
			SurfaceEffectSourcePipeline(getGLManager(),
				CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
				callback).apply {
				effect = GLEffect.EFFECT_GRAY_REVERSE
			}
		} else {
			if (DEBUG) Log.v(TAG, "createSurfaceSource:create SurfaceSourcePipeline")
			SurfaceSourcePipeline(getGLManager(),
				CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
				callback, USE_SHARED_CONTEXT
			)
		}
	}

	/**
	 * GLPipelineインスタンスを生成
	 * @param surface
	 */
	private fun createPipeline(surface: Any?, maxFps: Fraction?): GLPipeline {
		if (DEBUG) Log.v(TAG, "createPipeline:surface=${surface}")
		return when (pipelineMode) {
			GLPipelineView.EFFECT_PLUS_SURFACE -> {
				if (DEBUG) Log.v(TAG, "createPipeline:create EffectPipeline & SurfaceRendererPipeline")
				val effect = EffectPipeline(mGLManager)
				effect.pipeline = SurfaceRendererPipeline(mGLManager, surface, maxFps)
				effect
			}
			GLPipelineView.EFFECT_ONLY -> {
				if (DEBUG) Log.v(TAG, "createPipeline:create EffectPipeline")
				EffectPipeline(mGLManager, surface, maxFps)
			}
			else -> {
				if (DEBUG) Log.v(TAG, "createPipeline:create SurfaceRendererPipeline")
				SurfaceRendererPipeline(mGLManager, surface, maxFps)
			}
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SimpleVideoSourceCameraTextureView::class.java.simpleName
		/**
		 * 共有GLコンテキストコンテキストを使ったマルチスレッド処理を行うかどうか
		 */
		private const val USE_SHARED_CONTEXT = false
		private const val SUPPORT_RECORDING = true
		/**
		 * テスト用にDrawerPipelineを接続するかどうか
		 */
		private const val USE_DRAWER_PIPELINE = true
		/**
		 * GLPipelineSurfaceSourceとしてSurfaceEffectSourcePipelineを使うかどうか
		 * true: SurfaceEffectSourcePipelineを使う
		 * false: SurfaceSourcePipelineを使う
		 */
		private const val USE_EFFECT = true
	}
}
