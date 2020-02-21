package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.widget.CameraSurfaceView

class CameraSurfaceFragment : BaseFragment() {

	private var mCameraView: CameraSurfaceView? = null

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
			R.layout.fragment_camera_surfaceview,
			container, false)
		mCameraView = rootView.findViewById(R.id.cameraView)
		return rootView
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		mCameraView!!.onResume()
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		mCameraView!!.onPause()
		super.internalOnPause()
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraSurfaceFragment::class.java.simpleName
	}

}