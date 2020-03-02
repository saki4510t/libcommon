package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.serenegiant.system.KeyboardDelegater
import com.serenegiant.view.ViewSlider
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.KeyboardView

/**
 * ViewSliderテスト用フラグメント
 */
class ViewSliderFragment : BaseFragment() {

	private var mViewSlider: ViewSlider? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_view_slider)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_ViewSlider)
		val rootView = customInflater.inflate(R.layout.fragment_viewslider, container, false)
		initView(rootView)
		return rootView;
	}

	override fun internalOnResume() {
		super.internalOnResume()
	}

	override fun internalOnPause() {
		super.internalOnPause()
	}

	fun initView(rootView: View) {
		mViewSlider = ViewSlider(rootView, R.id.slide_view)
		rootView.setOnClickListener {
			var visibility = mViewSlider!!.visibility;
			if (visibility == View.VISIBLE) {
				mViewSlider!!.hide(1000)
			} else {
				mViewSlider!!.show(300)
			}
		}
		mViewSlider!!.hide(300)
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = ViewSliderFragment::class.java.simpleName
	}
}