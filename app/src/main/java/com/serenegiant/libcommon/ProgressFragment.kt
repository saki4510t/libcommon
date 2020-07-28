package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.widget.ProgressView

class ProgressFragment : BaseFragment() {

	private var mRootView: View? = null
	private var mProgress1: ProgressView? = null;
	private var mProgress2: ProgressView? = null;

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		mRootView = inflater.inflate(R.layout.fragment_progress_view, container, false)
		initView(mRootView!!)
		return mRootView
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_progress_view)
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

//--------------------------------------------------------------------------------
	private fun initView(rootView: View) {
		if (DEBUG) Log.v(TAG, "initView:")
		mProgress1 = rootView.findViewById(R.id.progressView1)
		mProgress2 = rootView.findViewById(R.id.progressView2)
	}

//--------------------------------------------------------------------------------
	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = ProgressFragment::class.java.simpleName
	}
}
