package com.serenegiant.libcommon
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
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.os.BundleCompat
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.mediastore.ImageLoader
import com.serenegiant.mediastore.LoaderDrawable
import com.serenegiant.mediastore.MediaInfo
import com.serenegiant.view.ITouchViewTransformer
import com.serenegiant.view.MotionEventUtils
import com.serenegiant.view.TouchViewTransformer
import com.serenegiant.widget.TouchTransformImageView
import java.io.IOException
import kotlin.math.sign

/**
 * 静止画表示用のFragment
 * FIXME MainFragmentで選択したのと違う映像が読み込まれる！！
 */
class ImageFragment: BaseFragment() {

	private lateinit var mInfo: MediaInfo
	private lateinit var mImageView: TouchTransformImageView

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_galley)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		return inflater.inflate(R.layout.fragment_image, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		var args = savedInstanceState
		if (args == null) {
			args = arguments
		}
		if (args != null) {
			mInfo = BundleCompat.getParcelable(args, ARG_MEDIA_INFO, MediaInfo::class.java)!!
		}
		initView(view)
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
		if (DEBUG) Log.v(TAG, "initView:" + mInfo.uri)
		mImageView = rootView.findViewById(R.id.image_view)
		mImageView.setOnClickListener {
			v -> if (DEBUG) Log.v(TAG, "onClick:$v")
		}
		mImageView.enableHandleTouchEvent = (
			TouchViewTransformer.TOUCH_ENABLED_MOVE
			or TouchViewTransformer.TOUCH_ENABLED_ZOOM
			or TouchViewTransformer.TOUCH_ENABLED_ROTATE)
		mImageView.viewTransformListener = object: ITouchViewTransformer.ViewTransformListener<Matrix> {
			override fun onStateChanged(view: View, newState: Int) {
				if (newState == TouchViewTransformer.STATE_NON) {
					if (DEBUG) Log.v(TAG, "onStateChanged:scale=${mImageView.scale}")
				}
			}

			override fun onTransformed(view: View, transform: Matrix) {
				if (DEBUG) Log.v(TAG, "onTransformed:${transform}")
			}
		}
		mImageView.setOnGenericMotionListener(object : View.OnGenericMotionListener {
			override fun onGenericMotion(v: View?, event: MotionEvent?): Boolean {
				if (MotionEventUtils.isFromSource(event!!, InputDevice.SOURCE_CLASS_POINTER)) {
					when (event.action) {
					MotionEvent.ACTION_HOVER_MOVE -> {
						if (DEBUG) Log.v(TAG, "onGenericMotion:ACTION_HOVER_MOVE")
						// process the mouse hover movement...
						return true
					}
					MotionEvent.ACTION_SCROLL -> {
						if (DEBUG) Log.v(TAG, "onGenericMotion:ACTION_SCROLL")
						// process the scroll wheel movement...
						val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
						if (vScroll != 0.0f) {
							mImageView.setScaleRelative(1.0f + vScroll.sign / 10.0f)	// 1.1か0.9
						}
						return true
					}
					}
				}
				return false
			}
		})
		if (USU_LOADER_DRAWABLE) {
			var drawable = mImageView.drawable
			if (drawable !is LoaderDrawable) {
				drawable = ImageLoaderDrawable(
					requireContext(), -1, -1)
				mImageView.setImageDrawable(drawable)
			}
			(drawable as LoaderDrawable).startLoad(mInfo)
		} else {
			queueEvent({
//				mImageView.setImageURI(mInfo.getUri());
				try {
					val bitmap = BitmapHelper.asBitmap(
						requireContext().contentResolver, mInfo.id)
					runOnUiThread ({
						mImageView.setImageBitmap(bitmap)
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

			var result: Bitmap?
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
				result = loadDefaultBitmap(context, com.serenegiant.common.R.drawable.ic_error_outline_red_24dp)
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
		fun newInstance(info: MediaInfo) = ImageFragment().apply {
			arguments = Bundle().apply {
				putParcelable(ARG_MEDIA_INFO, info)
			}
		}
	}
}
