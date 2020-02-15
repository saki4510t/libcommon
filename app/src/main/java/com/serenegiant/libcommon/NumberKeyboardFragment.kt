package com.serenegiant.libcommon

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.view.ViewUtils

/**
 * NumberKeyboardテスト用フラグメント
 */
class NumberKeyboardFragment : BaseFragment() {

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return customInflater.inflate(R.layout.fragment_numberkeyboard, container, false)
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = NumberKeyboardFragment::class.java.simpleName
	}
}