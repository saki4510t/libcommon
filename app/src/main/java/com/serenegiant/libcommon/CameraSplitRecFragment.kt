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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import com.serenegiant.glpipeline.EncodePipeline
import com.serenegiant.glpipeline.GLPipeline
import com.serenegiant.gl.GLEffect
import com.serenegiant.media.*
import com.serenegiant.media.IRecorder.RecorderCallback
import com.serenegiant.system.BuildCheck
import com.serenegiant.widget.EffectCameraGLSurfaceView
import com.serenegiant.widget.GLPipelineView
import com.serenegiant.widget.SimpleVideoSourceCameraTextureView
import java.io.IOException

/**
 * MediaAVRecorderを使ったカメラ映像の録画テスト用Fragment
 * このテストFragmentではアプリ内で直接録画しているがCameraFragmentや
 * EffectCameraFragmentの様に録画処理部分はサービスとして実行するように
 * した方がアプリ終了時に再生できない動画ファイルができてしまうのを防止できる。
 */
class CameraSplitRecFragment : AbstractCameraFragment() {

	private var mEncoderSurface: Surface? = null
	private var mAudioSampler: IAudioSampler? = null
	private var mRecorder: IRecorder? = null
	@Volatile
	private var mVideoEncoder: IVideoEncoder? = null
	@Volatile
	private var mAudioEncoder: IAudioEncoder? = null

	override fun onLongClick(view: View): Boolean {
		super.onLongClick(view)
		when (view.id) {
			R.id.cameraView -> {
				// カメラ映像表示Viewを長押ししたとき
				if (mCameraView is EffectCameraGLSurfaceView) {
					val v = view as EffectCameraGLSurfaceView
					v.effect = (v.effect + 1) % GLEffect.EFFECT_NUM
					return true
				} else if (mCameraView is SimpleVideoSourceCameraTextureView) {
					val v = view as SimpleVideoSourceCameraTextureView
					if (v.isEffectSupported()) {
						v.effect = (v.effect + 1) % GLEffect.EFFECT_NUM
						return true
					}
				}
			}
			R.id.record_button -> {
				if (triggerStillCapture()) {
					return true
				}
			}
		}
		return false
	}

	override fun isRecording(): Boolean {
		return mRecorder != null
	}

	@Throws(IOException::class)
	override fun internalStartRecording() {
		if (DEBUG) Log.v(TAG, "internalStartRecording:")
		val context: Context = requireContext()
		// 出力先のディレクトリを示すDocumentFileを取得
		// API>=29の場合は通常のディレクトリへはアクセスできないのでnull(またはSAFで取得したツリードキュメントでも可)をセットする
		val outputDir: DocumentFile?
		= if (BuildCheck.isAPI29()) { // API29以降は対象範囲別ストレージなのでディレクトリアクセスできないのでnullをセット
			null
		} else {
			// ここの#getRecordingRoot呼び出しと#getRecordingFile呼び出しは等価
			MediaFileUtils.getRecordingRoot(
				context, Environment.DIRECTORY_MOVIES, Const.REQUEST_ACCESS_SD)
		}
		if (DEBUG) Log.v(TAG, "internalStartRecording:output=$outputDir," + outputDir?.uri)
		startEncoder(outputDir, 2, CHANNEL_COUNT, false)
		if (DEBUG) Log.v(TAG, "internalStartRecording:finished")
	}

	override fun internalStopRecording() {
		if (DEBUG) Log.v(TAG, "internalStopRecording:")
		stopEncoder()
		val recorder = mRecorder
		recorder?.stopRecording()
		// you should not wait and should not clear mRecorder here
		if (DEBUG) Log.v(TAG, "internalStopRecording:finished")
	}

	override fun onFrameAvailable() {
		val recorder = mRecorder
		recorder?.frameAvailableSoon()
	}

	override fun isRecordingSupported(): Boolean {
		return super.isRecordingSupported()
			|| (enablePipelineEncode && (mCameraView is GLPipelineView))
	}

//--------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------
	/**
	 * create IRecorder instance for recording and prepare, start
	 * @param outputDir
	 * @param audioSource
	 * @param audioChannels
	 * @param align16
	 */
	@Throws(IOException::class)
	private fun startEncoder(outputDir: DocumentFile?,
		audioSource: Int, audioChannels: Int, align16: Boolean) {

		var recorder = mRecorder
		if (DEBUG) Log.d(TAG, "startEncoder:recorder=$recorder")
		if (recorder == null) {
			try {
				recorder = createRecorder(outputDir,
					audioSource, audioChannels, align16)
				recorder.prepare()
				recorder.startRecording()
				mRecorder = recorder
			} catch (e: Exception) {
				Log.w(TAG, "startEncoder:", e)
				stopEncoder()
				recorder?.stopRecording()
				mRecorder = null
				throw e
			}
		}
		if (DEBUG) Log.v(TAG, "startEncoder:finished")
	}

	private fun stopEncoder() {
		if (DEBUG) Log.v(TAG, "stopEncoder:surface=${mEncoderSurface}")
		val surface = mEncoderSurface
		mEncoderSurface = null
		if (surface != null) {
			removeSurface(surface)
		}
		if (mVideoEncoder is GLPipeline) {
			if (DEBUG) Log.v(TAG, "stopEncoder:remove Encoder from pipeline chains,${mVideoEncoder}")
			val pipeline = mVideoEncoder as GLPipeline
			val first = GLPipeline.findFirst(pipeline)
			if (DEBUG) Log.v(TAG, "stopEncoder:before=${GLPipeline.pipelineString(first)}")
			pipeline.remove()
//			GLPipeline.validatePipelineChain(first)
			if (DEBUG) Log.v(TAG, "stopEncoder:after=${GLPipeline.pipelineString(first)}")
		}
		mVideoEncoder = null
		mAudioEncoder = null
		val sampler = mAudioSampler
		mAudioSampler = null
		sampler?.release()
		if (DEBUG) Log.v(TAG, "stopEncoder:finished")
	}

	/**
	 * create recorder and related encoder
	 * @param outputDir
	 * @param audioSource
	 * @param audioChannels
	 * @param align16
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("MissingPermission")
	@Suppress("DEPRECATION")
	@Throws(IOException::class)
	private fun createRecorder(
		outputDir: DocumentFile?,
		audioSource: Int, audioChannels: Int,
		align16: Boolean): Recorder {

		if (DEBUG) Log.v(TAG, "createRecorder:basePath=" + outputDir?.uri)
		val recorder = MediaAVSplitRecorderV2(
			requireContext(), mRecorderCallback, null, MAX_FILE_SIZE)
		// create encoder for video recording
		mVideoEncoder = if (enablePipelineEncode && (mCameraView is GLPipelineView)) {
			if (DEBUG) Log.v(TAG, "createRecorder:create EncodePipeline")
			val view = mCameraView as GLPipelineView
			val pipeline = EncodePipeline(view.getGLManager(), recorder, mEncoderListener) // API>=18
			view.addPipeline(pipeline)
			pipeline
		} else {
			if (DEBUG) Log.v(TAG, "createRecorder:create SurfaceEncoder")
			SurfaceEncoder(recorder, mEncoderListener) // API>=18
		}
		mVideoEncoder!!.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
		mVideoEncoder!!.setVideoConfig(-1, VIDEO_FPS, 10)
		if (audioSource >= 0) {
			mAudioSampler = AudioSampler(audioSource,
				audioChannels, SAMPLE_RATE)
			mAudioSampler!!.start()
			mAudioEncoder = AudioSamplerEncoder(recorder, mEncoderListener, mAudioSampler)
		}
		if (DEBUG) Log.v(TAG, "createRecorder:finished")
		return recorder
	}

	private val mRecorderCallback = object : RecorderCallback {
		override fun onPrepared(recorder: IRecorder) {
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onPrepared:" + recorder
				+ ",mEncoderSurface=" + mEncoderSurface)
			val encoder = recorder.videoEncoder
			try {
				if (encoder is SurfaceEncoder) {
					if (mEncoderSurface == null) {
						val surface = recorder.inputSurface
						if (surface != null) {
							if (DEBUG) Log.v(TAG, "use SurfaceEncoder")
							mEncoderSurface = surface
							try {
								addSurface(surface)
							} catch (e: Exception) {
								mEncoderSurface = null
								throw e
							}
						}
					}
				} else if (encoder is EncodePipeline) {
					if (DEBUG) Log.v(TAG, "use EncodePipeline")
					mEncoderSurface = null
				} else if (encoder is IVideoEncoder) {
					mEncoderSurface = null
					throw RuntimeException("unknown video encoder $encoder")
				}
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
				try {
					stopRecording()
				} catch (e1: Exception) {
					// unrecoverable exception
					Log.w(TAG, e1)
				}
			}
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onPrepared:finished")
		}

		override fun onStarted(recorder: IRecorder) {
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onStarted:$recorder")
		}

		override fun onStopped(unused: IRecorder) {
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onStopped:$unused")
			stopEncoder()
			val recorder = mRecorder
			mRecorder = null
			try {
				queueEvent({
					if (recorder != null) {
						try {
							recorder.release()
						} catch (e: Exception) {
							Log.w(TAG, e)
						}
					}
				}, 1000)
			} catch (e: IllegalStateException) {
				// ignore, will be already released
				Log.w(TAG, e)
			}
			clearRecordingState()
			if (DEBUG) Log.v(TAG, "mRecorderCallback#onStopped:finished")
		}

		override fun onError(e: Exception) {
			Log.w(TAG, e)
			val recorder = mRecorder
			mRecorder = null
			if (recorder != null && (recorder.isReady || recorder.isStarted)) {
				recorder.stopRecording()
			}
		}
	}

	private val mEncoderListener = object : EncoderListener2 {
		override fun onStartEncode(
			encoder: Encoder, source: Surface?, mayFail: Boolean) {
			if (DEBUG) Log.v(TAG, "mEncoderListener#onStartEncode:$encoder")
		}

		override fun onStopEncode(encoder: Encoder) {
			if (DEBUG) Log.v(TAG, "mEncoderListener#onStopEncode:$encoder")
		}

		override fun onDestroy(encoder: Encoder) {
			if (DEBUG) Log.v(TAG, "mEncoderListener#onDestroy:"
				+ encoder + ",mRecorder=" + mRecorder)
			if (DEBUG) Log.v(TAG, "mEncoderListener#onDestroy:finished")
		}

		override fun onError(e: Throwable) {
			Log.w(TAG, e)
			val recorder = mRecorder
			mRecorder = null
			if (recorder != null && (recorder.isReady || recorder.isStarted)) {
				recorder.stopRecording()
			}
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = CameraSplitRecFragment::class.java.simpleName

		/**
		 * 分割録画する際のおおよそのファイルサイズ
		 */
		private const val MAX_FILE_SIZE = (1024 * 1024 * 10L) // 10MB

		fun newInstance(
			@LayoutRes layoutRes: Int, @StringRes titleRes: Int,
			pipelineMode: Int = GLPipelineView.PREVIEW_ONLY,
			enablePipelineEncode: Boolean = false,
			enableFaceDetect: Boolean = false)
			= CameraSplitRecFragment().apply {
				arguments = Bundle().apply {
					putInt(ARGS_KEY_LAYOUT_ID, layoutRes)
					putInt(ARGS_KEY_TITLE_ID, titleRes)
					putInt(ARGS_KEY_PIPELINE_MODE, pipelineMode)
					putBoolean(ARGS_KEY_ENABLE_PIPELINE_RECORD, enablePipelineEncode)
					putBoolean(ARGS_KEY_ENABLE_FACE_DETECT, enableFaceDetect)
				}
		}
	}
}
