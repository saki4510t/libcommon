package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.view.ViewUtils

class ForegroundServiceFragment : BaseFragment() {
	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_service_foreground)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return customInflater.inflate(R.layout.fragment_foreground_service, container, false)
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = ForegroundServiceFragment::class.java.simpleName

		fun newInstance() = ForegroundServiceFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}