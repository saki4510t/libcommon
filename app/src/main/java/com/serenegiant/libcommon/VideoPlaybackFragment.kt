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

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.core.os.BundleCompat
import com.serenegiant.media.IFrameCallback
import com.serenegiant.media.MediaPlayer
import com.serenegiant.mediastore.MediaInfo
import java.io.FileNotFoundException

/**
 * 動画再生用Fragment
 */
class VideoPlaybackFragment : BaseFragment() {
	//--------------------------------------------------------------------------------
	private var mInfo: MediaInfo? = null
	private var mPlayer: MediaPlayer? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {

		return inflater.inflate(R.layout.fragment_video_playback, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		var args = savedInstanceState
		if (args == null) {
			args = arguments
		}
		if (args != null) {
			mInfo = BundleCompat.getParcelable(args, ARG_MEDIA_INFO, MediaInfo::class.java)
		}
		initView(view)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (mInfo == null || mInfo!!.mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
			popBackStack()
		}
		if (mPlayer != null && mPlayer!!.isPaused) {
			mPlayer!!.resume()
		}
	}

	override fun internalOnPause() {
		if (mPlayer != null) {
			mPlayer!!.pause()
		}
		super.internalOnPause()
	}

	override fun internalRelease() {
		releasePlayer()
		super.internalRelease()
	}

	private fun initView(rootView: View) {
		val mVideoView = rootView.findViewById<TextureView>(R.id.video_view)
		mVideoView.surfaceTextureListener = mSurfaceTextureListener
	}

	private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
		override fun onSurfaceTextureAvailable(
			surface: SurfaceTexture,
			width: Int, height: Int) {
			if (DEBUG) Log.v(TAG, "onSurfaceTextureAvailable:$surface")
			if (mPlayer == null) {
				mPlayer = MediaPlayer(Surface(surface), object : IFrameCallback {
					override fun onPrepared() {
						if (DEBUG) Log.v(TAG, "onPrepared:")
						if (mPlayer != null) {
							mPlayer!!.setLoop(true)
							mPlayer!!.play()
						}
					}

					override fun onFinished() {
						if (DEBUG) Log.v(TAG, "onFinished:")
					}

					override fun onFrameAvailable(presentationTimeUs: Long): Boolean {
						return false
					}
				}, true)
				try {
					val fd = requireContext()
						.contentResolver
						.openAssetFileDescriptor(mInfo!!.uri!!, "r")
					if (fd != null) {
						mPlayer!!.prepare(fd)
					}
				} catch (e: FileNotFoundException) {
					Log.w(TAG, e)
					showToast(Toast.LENGTH_SHORT, e.localizedMessage)
				}
			}
		}

		override fun onSurfaceTextureSizeChanged(
			surface: SurfaceTexture,
			width: Int, height: Int) {
			if (DEBUG) Log.v(
				TAG, "onSurfaceTextureSizeChanged:" + surface
					+ ",width=" + width + ",height=" + height
			)
		}

		override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
			if (DEBUG) Log.v(TAG, "onSurfaceTextureDestroyed:$surface")
			releasePlayer()
			return false
		}

		override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//			if (DEBUG) Log.v(TAG, "onSurfaceTextureUpdated:" + surface);
		}
	}

	private fun releasePlayer() {
		if (mPlayer != null) {
			mPlayer!!.release()
			mPlayer = null
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = VideoPlaybackFragment::class.java.simpleName
		private const val ARG_MEDIA_INFO = "ARG_MEDIA_INFO"

		/**
		 * インスタンス生成用のヘルパーメソッド
		 * @param info
		 * @return
		 */
		fun newInstance(info: MediaInfo) = VideoPlaybackFragment().apply {
			arguments = Bundle().apply {
				putParcelable(ARG_MEDIA_INFO, info)
			}
		}
	}
}
