package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2026 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.serenegiant.camera.Camera2Utils
import com.serenegiant.camera.CameraConst
import com.serenegiant.camera.CameraUtils
import com.serenegiant.graphics.ImageFormatUtils
import com.serenegiant.libcommon.databinding.FragmentCameraInfoBinding
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.ContextUtils
import com.serenegiant.system.PermissionUtils
import com.serenegiant.view.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class CameraInfoFragment  : BaseFragment() {

	private lateinit var mBinding: FragmentCameraInfoBinding

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_camera_info)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater = ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return DataBindingUtil.inflate<FragmentCameraInfoBinding?>(
			customInflater,
			R.layout.fragment_camera_info, container, false
		).apply {
			mBinding = this
		}.run {
			root
		}
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		// カメラパーミッションが無いなら終了して前画面へ戻る
		if (!PermissionUtils.hasCamera(activity)) {
			popBackStack()
			return
		}
		lifecycleScope.launch(Dispatchers.Default) {
			dumpCamera1Info()
			if (BuildCheck.isAPI21()) {
				dumpCamera2Info()
			}
		}
	}

	/**
	 * Camera1 APIで内蔵カメラ情報を出力する
	 */
	@Suppress("DEPRECATION")
	private suspend fun dumpCamera1Info() {
		addMessage("Camera1 API")
		facing.forEach { face ->
			val cameraId = CameraUtils.findCamera(face)
			// カメラIDを表示
			addMessage("  ${CameraConst.faceString(face)}=$cameraId")
			if (cameraId >= 0) {
				try {
					val camera = Camera.open(cameraId)
					try {
						val params = camera.parameters
						if (params != null) {
							// カメラの標準の解像度を取得する
							val ppsfv = params.preferredPreviewSizeForVideo
							addMessage("    ppsfv=(${ppsfv.width}x${ppsfv.height})")
							addMessage("")
							// カメラガ対応しているプレビュー解像度一覧を出力する
							for (size in params.supportedPreviewSizes) {
								addMessage("    preview:(${size.width}x${size.height})")
							}
							var formats = params.supportedPreviewFormats
							for (format in formats) {
								addMessage("    preview format:(${ImageFormatUtils.toImageFormatString(format)})")
							}
							addMessage("")
							// カメラガ対応している静止画解像度一覧を出力する
							for (size in params.supportedPictureSizes) {
								addMessage("    picture:(${size.width}x${size.height})")
							}
							formats = params.supportedPictureFormats
							for (format in formats) {
								addMessage("    picture format:(${ImageFormatUtils.toImageFormatString(format)})")
							}
							addMessage("")
							// カメラガ対応している動画解像度一覧を出力する
							for (size in params.supportedVideoSizes) {
								addMessage("    video:(${size.width}x${size.height})")
							}
						}
					} finally {
						camera.release()
					}
				} catch (e: Exception) {
					addMessage("  $e")
				}
			}
			addMessage("")
		}
	}

	/**
	 * Camera2 APIで内蔵カメラ情報を出力する
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private suspend fun dumpCamera2Info() {
		addMessage("Camera2 API")
		val cameraManager = ContextUtils.requireSystemService(requireContext(), CameraManager::class.java)
		facing.forEach { face ->
			val info = Camera2Utils.findCamera(cameraManager, face)
			if (info?.isValid == true) {
				// カメラIDを表示
				addMessage("  ${CameraConst.faceString(face)}=${info.id}/orientation=${info.orientation}")
				try {
					val characteristics = cameraManager.getCameraCharacteristics(info.id)
					val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
					addMessage("    activeArraySize=(${activeArraySize?.width()}x${activeArraySize?.height()})")
					val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
					if (configMap != null) {
						addMessage("")
						// MediaCodec用の解像度一覧を取得
						var sizes = configMap.getOutputSizes(MediaCodec::class.java)
						if (sizes != null) {
							for (sz in sizes) {
								addMessage("    MediaCodec:(${sz.width}x${sz.height})")
							}
						} else {
							addMessage("    MediaCodec:not found")
						}
						addMessage("")
						// MediaRecorder用の解像度一覧を取得
						sizes = configMap.getOutputSizes(MediaRecorder::class.java)
						if (sizes != null) {
							for (sz in sizes) {
								addMessage("    MediaRecorder:(${sz.width}x${sz.height})")
							}
						} else {
							addMessage("    MediaRecorder:not found")
						}
						addMessage("")
						// SurfaceTexture用の解像度一覧を取得
						sizes = configMap.getOutputSizes(SurfaceTexture::class.java)
						if (sizes != null) {
							for (sz in sizes) {
								addMessage("    SurfaceTexture:(${sz.width}x${sz.height})")
							}
						} else {
							addMessage("    SurfaceTexture:not found")
						}
					} else {
						addMessage("    SCALER_STREAM_CONFIGURATION_MAP failed")
					}
				} catch (e: Exception) {
					addMessage("  $e")
				}
			}
			addMessage("")
		}
	}

	@SuppressLint("SetTextI18n")
	private suspend fun addMessage(msg: String) {
		withContext(Dispatchers.Main) {
			mBinding.message.text = "${mBinding.message.text}\n$msg"
		}
		yield()
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraInfoFragment::class.java.simpleName

		private val facing = arrayOf(CameraConst.FACING_BACK, CameraConst.FACING_FRONT)

		fun newInstance() = CameraInfoFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}