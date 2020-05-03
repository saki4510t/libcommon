package com.serenegiant.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.Size
import com.serenegiant.glutils.GLDrawer2D
import com.serenegiant.glutils.es3.GLHelper
import com.serenegiant.graphics.MatrixUtils
import com.serenegiant.widget.CameraDelegator.ICameraRenderer
import java.lang.UnsupportedOperationException

class SimpleCameraGLView @JvmOverloads constructor(context: Context?,
	attrs: AttributeSet? = null, defStyle: Int = 0)
		: AspectScaledGLView(context, attrs, defStyle), ICameraView {

	private val mSync = Any()
	private val mCameraDelegator: CameraDelegator
	private val mTexMatrix = FloatArray(16)
	private var mDrawer: GLDrawer2D? = null
	private var mTexId = 0
	private var mSurfaceTexture: SurfaceTexture? = null
	@Volatile
	private var mRequestUpdateTex = false

	/**
	 * コンストラクタ
	 */
	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		mCameraDelegator = CameraDelegator(this,
			CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT,
			object : ICameraRenderer {
				override fun hasSurface(): Boolean {
					if (DEBUG) Log.v(TAG, "hasSurface:$mSurfaceTexture")
					synchronized(mSync) { return mSurfaceTexture != null }
				}

				override fun onPreviewSizeChanged(width: Int, height: Int) {
					if (DEBUG) Log.v(TAG, String.format("onPreviewSizeChanged:(%dx%dx)", width, height))
					setAspectRatio(width, height)
					synchronized(mSync) {
						if (mSurfaceTexture != null) {
							mSurfaceTexture!!.setDefaultBufferSize(width, height)
						}
					}
				}

				override fun getInputSurface(): SurfaceTexture {
					if (DEBUG) Log.v(TAG, "getInputSurfaceTexture:$mSurfaceTexture")
					synchronized(mSync) { return mSurfaceTexture!! }
				}
			}
		)
		setRenderer(object : GLRenderer {
			private var cnt = 0
			override fun onSurfaceCreated() {
				if (DEBUG) Log.v(TAG, "onSurfaceCreated:")
				synchronized(mSync) {
					mDrawer = GLDrawer2D.create(isOES3(), true)
					mTexId = mDrawer!!.initTex()
					mSurfaceTexture = SurfaceTexture(mTexId)
					mSurfaceTexture!!.setDefaultBufferSize(
						CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
					mSurfaceTexture!!.setOnFrameAvailableListener {
//						if (DEBUG && ((++cnt % 100) == 0)) Log.v(TAG, "onFrameAvailable:" + cnt);
						mRequestUpdateTex = true
					}
				}
			}

			override fun onSurfaceChanged(format: Int, width: Int, height: Int) {
				if (DEBUG) Log.v(TAG, String.format("onSurfaceChanged:(%dx%d))", width, height))
				mCameraDelegator.startPreview(
					CameraDelegator.DEFAULT_PREVIEW_WIDTH, CameraDelegator.DEFAULT_PREVIEW_HEIGHT)
			}

			override fun drawFrame() {
				if (DEBUG && ++cnt % 1800 == 0) Log.v(TAG, "drawFrame:$cnt")
				if (mRequestUpdateTex) {
					mRequestUpdateTex = false
					mSurfaceTexture!!.updateTexImage()
					mSurfaceTexture!!.getTransformMatrix(mTexMatrix)
				}
				mDrawer!!.draw(mTexId, mTexMatrix, 0)
			}

			@SuppressLint("NewApi")
			override fun onSurfaceDestroyed() {
				if (DEBUG) Log.v(TAG, "onSurfaceDestroyed:")
				synchronized(mSync) {
					if (mSurfaceTexture != null) {
						mSurfaceTexture!!.release()
						mSurfaceTexture = null
					}
					if (mTexId != 0) {
						if (isGLES3()) {
							GLHelper.deleteTex(mTexId)
						} else {
							com.serenegiant.glutils.es2.GLHelper.deleteTex(mTexId)
						}
						mTexId = 0
					}
					if (mDrawer != null) {
						mDrawer!!.release()
						mDrawer = null
					}
				}
			}

			override fun applyTransformMatrix(@Size(min = 16) transform: FloatArray) {
				if (mDrawer != null) {
					if (DEBUG) Log.v(TAG, "applyTransformMatrix:"
						+ MatrixUtils.toGLMatrixString(transform))
					mDrawer!!.setMvpMatrix(transform, 0)
				}
			}
		})
	}

//--------------------------------------------------------------------------------
// ICameraViewの実装
	/**
	 * ICameraViewの実装
	 */
	override fun onResume() {
		if (DEBUG) Log.v(TAG, "onResume:")
		mCameraDelegator.onResume()
	}

	/**
	 * ICameraViewの実装
	 */
	override fun onPause() {
		if (DEBUG) Log.v(TAG, "onPause:")
		mCameraDelegator.onPause()
	}

	/**
	 * ICameraViewの実装
	 * @param width
	 * @param height
	 */
	override fun setVideoSize(width: Int, height: Int) {
		if (DEBUG) Log.v(TAG, String.format("setVideoSize:(%dx%d)", width, height))
		mCameraDelegator.setVideoSize(width, height)
	}

	/**
	 * ICameraViewの実装
	 * @param listener
	 */
	override fun addListener(listener: CameraDelegator.OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "addListener:")
	}

	/**
	 * ICameraViewの実装
	 * @param listener
	 */
	override fun removeListener(listener: CameraDelegator.OnFrameAvailableListener) {
		if (DEBUG) Log.v(TAG, "removeListener:")
	}

	/**
	 * ICameraViewの実装
	 * @return
	 */
	override fun getVideoWidth(): Int {
		if (DEBUG) Log.v(TAG, "getVideoWidth:")
		return mCameraDelegator.previewWidth
	}

	/**
	 * ICameraViewの実装
	 * @return
	 */
	override fun getVideoHeight(): Int {
		if (DEBUG) Log.v(TAG, "getVideoHeight:")
		return mCameraDelegator.previewHeight
	}

	override fun addSurface(id: Int, surface: Any, isRecordable: Boolean) {
		throw UnsupportedOperationException()
	}

	override fun removeSurface(id: Int) {
		throw UnsupportedOperationException()
	}

	private fun updateViewport() { //		final int viewWidth = getWidth();
//		final int viewHeight = getHeight();
//		if (viewWidth == 0 || viewHeight == 0) {
//			if (DEBUG) Log.v(TAG, String.format("updateViewport:view is not ready(%dx%d)", viewWidth, viewHeight));
//			return;
//		}
//		final double videoWidth = mCameraDelegator.getWidth();
//		final double videoHeight = mCameraDelegator.getHeight();
//		if (videoWidth == 0 || videoHeight == 0) {
//			if (DEBUG) Log.v(TAG, String.format("updateViewport:video is not ready(%dx%d)", viewWidth, viewHeight));
//			return;
//		}
//		final double viewAspect = viewWidth / (double)viewHeight;
//		Log.i(TAG, String.format("updateViewport:view(%d,%d)%f,video(%1.0f,%1.0f)",
//			viewWidth, viewHeight, viewAspect, videoWidth, videoHeight));
//
//		android.opengl.Matrix.setIdentityM(mMvpMatrix, 0);
//		final int scaleMode = mCameraDelegator.getScaleMode();
//		switch (scaleMode) {
//		case CameraDelegator.SCALE_STRETCH_FIT:
//			break;
//		case CameraDelegator.SCALE_KEEP_ASPECT_VIEWPORT:
//		{
//			final double req = videoWidth / videoHeight;
//			int x, y;
//			int width, height;
//			if (viewAspect > req) {
//				// if view is wider than camera image, calc width of drawing area based on view height
//				y = 0;
//				height = viewHeight;
//				width = (int)(req * viewHeight);
//				x = (viewWidth - width) / 2;
//			} else {
//				// if view is higher than camera image, calc height of drawing area based on view width
//				x = 0;
//				width = viewWidth;
//				height = (int)(viewWidth / req);
//				y = (viewHeight - height) / 2;
//			}
//			// set viewport to draw keeping aspect ration of camera image
//			Log.i(TAG, String.format("updateViewport;xy(%d,%d),size(%d,%d)", x, y, width, height));
////			mTarget.setViewPort(0, 0, width, height);
//			break;
//		}
//		case CameraDelegator.SCALE_KEEP_ASPECT:
//		case CameraDelegator.SCALE_CROP_CENTER:
//		{
//			final double scale_x = viewWidth / videoWidth;
//			final double scale_y = viewHeight / videoHeight;
//			final double scale = (scaleMode == CameraDelegator.SCALE_CROP_CENTER
//				? Math.max(scale_x,  scale_y) : Math.min(scale_x, scale_y));
//			final double width = scale * videoWidth;
//			final double height = scale * videoHeight;
//			Log.i(TAG, String.format("updateViewport:size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
//				width, height, scale_x, scale_y, width / viewWidth, height / viewHeight));
//			android.opengl.Matrix.scaleM(mMvpMatrix, 0, (float)(width / viewWidth), (float)(height / viewHeight), 1.0f);
//			break;
//		}
//		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SimpleCameraGLView::class.java.simpleName
	}
}