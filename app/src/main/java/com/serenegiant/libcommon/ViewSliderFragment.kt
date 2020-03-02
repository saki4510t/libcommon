package com.serenegiant.libcommon

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
		mRootView = customInflater.inflate(
			if (USE_CONSTRAINT_LAYOUT) R.layout.fragment_viewslider_constraint
				else R.layout.fragment_viewslider,
			container, false)
		initView(mRootView!!)
		return mRootView;
	}

	override fun internalOnResume() {
		super.internalOnResume()
	}

	override fun internalOnPause() {
		super.internalOnPause()
	}

	private fun initView(rootView: View) {
		rootView.setOnTouchListener(mOnTouchListener)
		mViewSliderLeft = ViewSlider(rootView, R.id.slide_view_left, ViewSlider.HORIZONTAL)
		mViewSliderLeft!!.hide(0)
		mViewSliderLeft!!.targetView.setOnTouchListener(mOnTouchListener)

		mViewSliderTop = ViewSlider(rootView, R.id.slide_view_top, ViewSlider.VERTICAL)
		mViewSliderTop!!.hide(0)
		mViewSliderTop!!.targetView.setOnTouchListener(mOnTouchListener)

		mViewSliderRight = ViewSlider(rootView, R.id.slide_view_right, ViewSlider.HORIZONTAL)
		mViewSliderRight!!.hide(0)
		mViewSliderRight!!.targetView.setOnTouchListener(mOnTouchListener)

		mViewSliderBottom = ViewSlider(rootView, R.id.slide_view_bottom)
		mViewSliderBottom!!.hide(0)
	}

	private val mOnTouchListener = View.OnTouchListener { view, event ->
		if (DEBUG) Log.v(TAG, "onTouch:${view.javaClass.simpleName}(${view.id}),${event}")
		if (event.actionMasked == MotionEvent.ACTION_UP) {
			if (view == mRootView) {
				val width = view.width
				val height = view.height
				val left = width / 4;
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
				mViewSliderLeft!!.hide()
			} else if (view == mViewSliderTop!!.targetView) {
				mViewSliderTop!!.hide()
			} else if (view == mViewSliderRight!!.targetView) {
				mViewSliderRight!!.hide()
			} else if (view == mViewSliderBottom!!.targetView) {
				mViewSliderBottom!!.hide()
			}
		}
		return@OnTouchListener true
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private const val USE_CONSTRAINT_LAYOUT = false
		private val TAG = ViewSliderFragment::class.java.simpleName
	}
}