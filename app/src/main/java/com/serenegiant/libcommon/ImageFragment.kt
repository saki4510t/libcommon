package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.mediastore.ImageLoader
import com.serenegiant.mediastore.LoaderDrawable
import com.serenegiant.mediastore.MediaInfo
import com.serenegiant.view.MotionEventUtils
import com.serenegiant.view.ViewTransformDelegater
import com.serenegiant.widget.ZoomImageView
import java.io.IOException
import kotlin.math.sign

/**
 * 静止画表示用のFragment
 * FIXME MainFragmentで選択したのと違う映像が読み込まれる！！
 */
class ImageFragment: BaseFragment() {

	private var mInfo: MediaInfo? = null
	private var mRootView: View? = null
	private var mImageView: ZoomImageView? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_galley)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		var args = savedInstanceState
		if (args == null) {
			args = arguments
		}
		if (args != null) {
			mInfo = args.getParcelable(ARG_MEDIA_INFO)
		}
		mRootView = inflater.inflate(R.layout.fragment_image, container, false)
		initView(mRootView!!)
		return mRootView
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:")
		val args = arguments
		if (args != null) {
			outState.putAll(args)
		}
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

//--------------------------------------------------------------------------------
	private fun initView(rootView: View) {
		if (DEBUG) Log.v(TAG, "initView:" + mInfo!!.uri)
		mImageView = rootView.findViewById(R.id.image_view)
		mImageView!!.setOnClickListener {
			v -> if (DEBUG) Log.v(TAG, "onClick:$v")
		}
		mImageView!!.setEnableHandleTouchEvent(
			ViewTransformDelegater.TOUCH_ENABLED_MOVE
				or ViewTransformDelegater.TOUCH_ENABLED_ZOOM
				or ViewTransformDelegater.TOUCH_ENABLED_ROTATE
		)
		mImageView!!.setViewTransformListener(object: ViewTransformDelegater.ViewTransformListener {
			override fun onStateChanged(view: View, newState: Int) {
				if (newState == ViewTransformDelegater.STATE_NON) {
					if (DEBUG) Log.v(TAG, "onStateChanged:scale=${mImageView!!.scale}")
				}
			}
			override fun onTransformed(view: View, transform: Matrix) {
				if (DEBUG) Log.v(TAG, "onTransformed:${transform}")
			}
		})
		mImageView!!.setOnGenericMotionListener(object : View.OnGenericMotionListener {
			override fun onGenericMotion(v: View?, event: MotionEvent?): Boolean {
				if (MotionEventUtils.isFromSource(event!!, InputDevice.SOURCE_CLASS_POINTER)) {
					when (event.getAction()) {
					MotionEvent.ACTION_HOVER_MOVE -> {
						if (DEBUG) Log.v(TAG, "onGenericMotion:ACTION_HOVER_MOVE")
						// process the mouse hover movement...
						return true;
					}
					MotionEvent.ACTION_SCROLL -> {
						if (DEBUG) Log.v(TAG, "onGenericMotion:ACTION_SCROLL")
						// process the scroll wheel movement...
						val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
						if (vScroll != 0.0f) {
							mImageView!!.setScaleRelative(1.0f + vScroll.sign / 10.0f)	// 1.1か0.9
						}
						return true;
					}
					}
				}
				return false;
			}
		})
		if (USU_LOADER_DRAWABLE) {
			var drawable = mImageView!!.getDrawable()
			if (drawable !is LoaderDrawable) {
				drawable = ImageLoaderDrawable(
					requireContext(), -1, -1)
				mImageView!!.setImageDrawable(drawable)
			}
			(drawable as LoaderDrawable).startLoad(mInfo!!)
		} else {
			queueEvent(Runnable {
//				mImageView.setImageURI(mInfo.getUri());
				try {
					val bitmap = BitmapHelper.asBitmap(
						requireContext().contentResolver, mInfo!!.id)
					runOnUiThread (Runnable {
						mImageView!!.setImageBitmap(bitmap)
					})
				} catch (e: IOException) {
					Log.w(TAG, e)
					popBackStack()
				}
			})
		}
	}

//--------------------------------------------------------------------------------
	private class ImageLoaderDrawable(context: Context,
		width: Int, height: Int) : LoaderDrawable(context, width, height) {

		override fun createImageLoader(): ImageLoader {
			return MyImageLoader(this)
		}

		override fun checkCache(id: Long): Bitmap? {
			return null
		}

		public override fun onBoundsChange(bounds: Rect) {
			super.onBoundsChange(bounds)
		}
	}

	private class MyImageLoader(parent: ImageLoaderDrawable)
		: ImageLoader(parent) {

		override fun loadBitmap(context: Context,
			info: MediaInfo,
			requestWidth: Int, requestHeight: Int): Bitmap {

			var result: Bitmap? = null
			try {
				result = BitmapHelper.asBitmap(context.contentResolver, info.id)
				if (result != null) {
					val w = result.width
					val h = result.height
					val bounds = Rect()
					mParent.copyBounds(bounds)
					val cx = bounds.centerX()
					val cy = bounds.centerY()
					bounds[cx - w / 2, cy - h / w, cx + w / 2] = cy + h / 2
					(mParent as ImageLoaderDrawable).onBoundsChange(bounds)
				}
			} catch (e: IOException) {
				if (DEBUG) Log.w(TAG, e)
				result = loadDefaultBitmap(context, R.drawable.ic_error_outline_red_24dp);
			}
			return result!!
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = ImageFragment::class.java.simpleName
		private const val USU_LOADER_DRAWABLE = false
		private const val ARG_MEDIA_INFO = "ARG_MEDIA_INFO"

		@JvmStatic
		fun newInstance(info: MediaInfo): ImageFragment {
			val fragment = ImageFragment()
			val args = Bundle()
			args.putParcelable(ARG_MEDIA_INFO, info)
			fragment.arguments = args
			return fragment
		}
	}
}