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

import android.animation.Animator
import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.libcommon.databinding.FragmentCameraSourcePipelineBinding
import com.serenegiant.libcommon.viewmodel.CameraSourcePipelineViewModel
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.UriHelper
import com.serenegiant.view.ViewAnimationHelper
import com.serenegiant.view.ViewAnimationHelper.ViewAnimationListener
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.ResolutionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CameraSourcePipelineFragment : Fragment() {
	private val mViewModel: CameraSourcePipelineViewModel by viewModels()
	private lateinit var mBinding : FragmentCameraSourcePipelineBinding
	/**
	 * 解像度切り替え用
	 */
	private lateinit var mAdapter: ResolutionAdapter


	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
		requireActivity().title = getString(R.string.title_camera_source_pipeline)
		lifecycle.addObserver(mViewModel)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater = ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Camera)
		return DataBindingUtil.inflate<FragmentCameraSourcePipelineBinding>(
			customInflater,
			R.layout.fragment_camera_source_pipeline, container, false
		).apply {
			mBinding = this
			viewModel = mViewModel
			cameraView.apply {
				holder.addCallback(mViewModel)
			}
			mAdapter = ResolutionAdapter(requireActivity())
			spinner.adapter = mAdapter
			spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View, position: Int, id: Long) {
					val item = mAdapter.getItem(position)
					val current = mViewModel.currentViewSize.value
					if (DEBUG) Log.v(TAG, "onItemSelected:${current}=>$item")
					if ((item != null) && (item != current)) {
						mViewModel.setVideoSize(item)
					}
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}
			recordButton.setOnClickListener {
				// 静止画撮影要求
				setThumbnailVisibility(false)
				mViewModel.triggerStillCapture()
			}
			// 静止画撮影後のサムネイル表示用
			thumbnail.setMaskDrawable(
				ContextCompat.getDrawable(
					requireContext(),
					com.serenegiant.common.R.drawable.mask_circle
				)
			)
			thumbnail.visibility = View.INVISIBLE
		}.run {
			root
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		mViewModel.apply {
			lifecycleScope.launch {
				viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
					supportedSizeList.collect { supported ->
						mAdapter.clear()
						if (supported.isNotEmpty()) {
							val current = mViewModel.currentViewSize.value
							var found = -1
							// 現在選択されている解像度のインデックスを探す
							for ((index, sz) in supported.withIndex()) {
								if (sz == current) {
									found = index
									break
								}
							}
							if (DEBUG) Log.v(TAG, "found=$found/$current")
							val f = found
							mAdapter.replaceAll(supported)
							mBinding.spinner.isEnabled = true
							if (f > 0) {
								mBinding.spinner.setSelection(f)
							}
						}
					}
				}
			}
			lifecycleScope.launch {
				viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
					currentViewSize.collect { sz ->
						if (DEBUG) Log.v(TAG, "currentViewSize=$sz")
						mBinding.cameraView.apply {
//							holder.setFixedSize(sz.width, sz.height)
//							aspectRatio = sz.width / sz.height.toDouble()
							setAspectRatio(sz.width, sz.height)
						}
					}
				}
			}
			lifecycleScope.launch {
				viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
					thumbnailDocument.collect { doc ->
						if (doc != null) {
							// サムネイル表示を更新
							updateThumbnail(doc)
						}
					}
				}
			}
		}
	}

	/**
	 * サムネイル表示の切り替え
	 * @param visible サムネイル表示するかどうか
	 */
	private fun setThumbnailVisibility(visible: Boolean) {
		if (DEBUG) Log.v(TAG, "setThumbnailVisibility:$visible")
		lifecycleScope.launch {
			if (visible) {
				zoomIn(mBinding.thumbnail)
			} else {
				mBinding.thumbnail.visibility = View.INVISIBLE
			}
		}
	}

	/**
	 * 非同期でサムネイルを更新する際に、処理中に次の更新要求が来た場合古い処理は
	 * キャンセルして最新のものだけを処理するためにJobを保持する
	 */
	private var mUpdateThumbnailJob: Job? = null

	/**
	 * 静止画撮影後の処理, 非同期でサムネイル更新する
	 * @param path 静止画の保存先
	 */
	private fun updateThumbnail(path: DocumentFile) {
		if (DEBUG) Log.v(TAG, "updateThumbnail:" + path.uri)
		// 未実行が残っていれば削除して最後のみを有効にする
		mUpdateThumbnailJob?.cancel()
		mUpdateThumbnailJob = lifecycleScope.launch(Dispatchers.Default) {
			delay(if (BuildCheck.isKitKat()) 300L else 500L)
			if (DEBUG) Log.v(TAG, "UpdateThumbnailTask#run:" + path.uri)
			val w = mBinding.thumbnail.width
			val h = mBinding.thumbnail.height
			try {
				val thumbnail = if (BuildCheck.isKitKat()) {
					BitmapHelper.asBitmap(requireContext().contentResolver,
						path.uri, w, h)
				} else {
					val filePath = UriHelper.getPath(
						requireContext(), path.uri)
					if (DEBUG) Log.v(TAG, "UpdateThumbnailTask#run:$filePath")
					BitmapHelper.asBitmap(filePath, w, h)
				}
				if (DEBUG) Log.v(TAG, "UpdateThumbnailTask#run:thumbnail=$thumbnail")
				if (thumbnail != null) {
					lifecycleScope.launch {
						if (DEBUG) Log.v(TAG, "UpdateThumbnailTask#run:サムネイルを表示する")
						mBinding.thumbnail.setImageBitmap(thumbnail)
						setThumbnailVisibility(true)
					}
				}
			} catch (e: Exception) {
				Log.w(TAG, e)
			}
		}
	}

	/**
	 * スケールを0→1まで変化(Viewをズームイン)させる
	 * @param target スケールアニメーションの対象となるView
	 */
	private fun zoomIn(target: View) {
		if (DEBUG) Log.v(TAG, "zoomIn:target=$target");
		ViewAnimationHelper.zoomIn(target, 200L, 0L, mViewAnimationListener)
	}

	/**
	 * アルファ値を1→0まで変化(Viewをフェードアウト)させる
	 * @param target アルファアニメーションの対象となるView
	 * @param startDelay アルファアニメーション開始までの遅延時間
	 */
	private fun fadeOut(target: View, startDelay: Long = 5000L) {
		if (DEBUG) Log.v(TAG, "fadeOut:target=$target");
		ViewAnimationHelper.fadeOut(target, -1L, startDelay, mViewAnimationListener)
	}

	private val mViewAnimationListener = object : ViewAnimationListener {
		override fun onAnimationStart(
			animator: Animator,
			target: View, animationType: Int) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}

		override fun onAnimationEnd(
			animator: Animator,
			target: View, animationType: Int) {
			when (animationType) {
				ViewAnimationHelper.ANIMATION_ZOOM_IN -> {
//					// 静止画モードならTHUMBNAIL_HIDE_DELAY経過後にフェードアウトさせる
//					// 既に動画モードに切り替えられていればすぐにフェードアウトさせる.
//					lifecycleScope.launch {
//						delay(100L)
//						fadeOut(target)
//					}
				}
				ViewAnimationHelper.ANIMATION_ZOOM_OUT -> {
					lifecycleScope.launch {
						delay(100L)
						target.visibility = View.GONE
					}
				}
			}
		}

		override fun onAnimationCancel(
			animator: Animator,
			target: View, animationType: Int) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}
	}

	companion object {
		private const val DEBUG = true // set false on release
		private val TAG = CameraSourcePipelineFragment::class.java.simpleName

		fun newInstance() = CameraSourcePipelineFragment()
	}
}