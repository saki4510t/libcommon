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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.serenegiant.view.ViewSlider
import com.serenegiant.view.ViewUtils

/**
 * ViewSliderテスト用フラグメント
 */
class ViewSliderFragment : BaseFragment() {

	private var mRootView: View? = null
	private var mViewSliderLeft: ViewSlider? = null
	private var mViewSliderTop: ViewSlider? = null
	private var mViewSliderRight: ViewSlider? = null
	private var mViewSliderBottom: ViewSlider? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_view_slider)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_ViewSlider)
		return customInflater.inflate(
			if (USE_CONSTRAINT_LAYOUT) R.layout.fragment_viewslider_constraint
				else R.layout.fragment_viewslider,
			container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		initView(view)
	}

	private fun initView(rootView: View) {
		mRootView = rootView
		rootView.setOnTouchListener(mOnTouchListener)
		mViewSliderLeft = ViewSlider(rootView, R.id.slide_view_left, ViewSlider.HORIZONTAL)
		.apply {
			hide(0)
			targetView.setOnTouchListener(mOnTouchListener)
			setViewSliderListener(mViewSliderListener)
		}

		mViewSliderTop = ViewSlider(rootView, R.id.slide_view_top, ViewSlider.VERTICAL)
		.apply {
			hide(0)
			targetView.setOnTouchListener(mOnTouchListener)
			setViewSliderListener(mViewSliderListener)
		}

		mViewSliderRight = ViewSlider(rootView, R.id.slide_view_right, ViewSlider.HORIZONTAL)
		.apply {
			hide(0)
			targetView.setOnTouchListener(mOnTouchListener)
			setViewSliderListener(mViewSliderListener)
		}

		mViewSliderBottom = ViewSlider(rootView, R.id.slide_view_bottom)
		.apply {
			hide(0)
			targetView.setOnTouchListener(mOnTouchListener)
			setViewSliderListener(mViewSliderListener)
		}
	}

	private val mOnTouchListener = View.OnTouchListener { view, event ->
		if (DEBUG) Log.v(TAG, "onTouch:${view.javaClass.simpleName}(${view.id}),${event}")
		if (event.actionMasked == MotionEvent.ACTION_UP) {
			if (view == mRootView) {
				view.performClick()
				val width = view.width
				val height = view.height
				val left = width / 4
				val top = height / 4
				val right = width * 3 / 4
				val bottom = height * 3 / 4
				if (event.y < top) {
					if (mViewSliderTop!!.isVisible) {
						mViewSliderTop!!.hide()
					} else {
						mViewSliderTop!!.show(0)
					}
				} else if (event.y > bottom) {
					if (mViewSliderBottom!!.isVisible) {
						mViewSliderBottom!!.hide()
					} else {
						mViewSliderBottom!!.show(0)
					}
				} else if (event.x < left) {
					if (mViewSliderLeft!!.isVisible) {
						mViewSliderLeft!!.hide()
					} else {
						mViewSliderLeft!!.show(0)
					}
				} else if (event.x > right) {
					if (mViewSliderRight!!.isVisible) {
						mViewSliderRight!!.hide()
					} else {
						mViewSliderRight!!.show(0)
					}
				}
			} else if (view == mViewSliderLeft!!.targetView) {
				view.performClick()
				mViewSliderLeft!!.hide()
			} else if (view == mViewSliderTop!!.targetView) {
				view.performClick()
				mViewSliderTop!!.hide()
			} else if (view == mViewSliderRight!!.targetView) {
				view.performClick()
				mViewSliderRight!!.hide()
			} else if (view == mViewSliderBottom!!.targetView) {
				view.performClick()
				mViewSliderBottom!!.hide()
			}
		}
		return@OnTouchListener true
	}

	private val mViewSliderListener = object : ViewSlider.ViewSliderListener {
		override fun onOpened(targetView: View) {
			if (DEBUG) Log.v(TAG, "onOpened:$targetView")
		}

		override fun onClosed(targetView: View) {
			if (DEBUG) Log.v(TAG, "onClosed:$targetView")
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private const val USE_CONSTRAINT_LAYOUT = false
		private val TAG = ViewSliderFragment::class.java.simpleName

		fun newInstance() = ViewSliderFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
