package com.serenegiant.libcommon

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import com.serenegiant.libcommon.databinding.FragmentScreenCaptureBinding
import com.serenegiant.libcommon.viewmodel.ScreenCaptureViewModel
import com.serenegiant.media.ScreenCaptureUtils
import com.serenegiant.media.ScreenCaptureUtils.ScreenCaptureCallback

/**
 * MediaProjectionManager/MediaProjectionを使った画面録画サンプル
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCaptureFragment : BaseFragment() {
	private val mViewModel: ScreenCaptureViewModel by viewModels()
	/**
	 * スクリーンキャプチャー要求用のヘルパーオブジェクト
	 */
	private lateinit var mScreenCapture: ScreenCaptureUtils

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		mScreenCapture = ScreenCaptureUtils(this, object : ScreenCaptureCallback {
			override fun onResult(data: Intent) {
				if (DEBUG) Log.v(TAG, "onResult:$data")
				// スクリーンキャプチャーサービス開始要求
				mViewModel.startScreenCapture(data)
			}

			override fun onFailed() {
				if (DEBUG) Log.v(TAG, "onFailed:")
				mViewModel.isChecked.value = false
				showToast(Toast.LENGTH_LONG, "User denied to start screen capture!")
			}
		})
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		return FragmentScreenCaptureBinding.inflate(
			inflater, container, false)
			.apply {
				viewModel = mViewModel
				lifecycleOwner = this@ScreenCaptureFragment
			}
			.run {
				root
			}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		mViewModel.run {
			isChecked.observe(viewLifecycleOwner, {
				if (DEBUG) Log.v(TAG, "check changed:isCheck=${it}/${isChecked.value}," +
					"isReceived=${isReceived},isRecording=${isRecording}")
				// 最初のステータス更新要求が終了するまではなにもしない
				if (isReceived) {
					if (it && (it != isRecording)) {
						mScreenCapture.requestScreenCapture()
					} else if (!it) {
						stopScreenCapture()
					}
				}
			})
		}
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}

	override fun internalOnResume() {
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		super.internalOnResume()
		mViewModel.queryRecordingStatus()
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = ScreenCaptureFragment::class.java.simpleName

		fun newInstance(): ScreenCaptureFragment {
			val fragment = ScreenCaptureFragment()
			val args = Bundle()
			fragment.arguments = args
			return fragment
		}
	}
}