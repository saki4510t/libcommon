package com.serenegiant.libcommon.viewmodel
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

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.serenegiant.service.ScreenRecorderService

class ScreenCaptureViewModel(application: Application) : AndroidViewModel(application) {

	var isRecording = false
	val isChecked by lazy { MutableLiveData(isRecording) }
	val recordingLabel = isChecked.map {
		if (it) "stop" else "start"
	}
	private val mBroadcastReceiver = MyBroadcastReceiver()
	var isReceived = false
		private set

	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:")
		val context = getApplication<Application>()
		val filter = IntentFilter(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT)
		LocalBroadcastManager.getInstance(context)
			.registerReceiver(mBroadcastReceiver, filter)
		queryRecordingStatus()
	}

	override fun onCleared() {
		if (DEBUG) Log.v(TAG, "onCleared:")
		val context = getApplication<Application>()
		LocalBroadcastManager.getInstance(context)
			.unregisterReceiver(mBroadcastReceiver)
	}

	/**
	 * スクリーンキャプチャー開始要求
	 * @param data
	 */
	fun startScreenCapture(data: Intent) {
		if (DEBUG) Log.v(TAG, "startScreenCapture:${isRecording},${data}")
		if (!isRecording) {
			isRecording = true
			val context = getApplication<Application>()
			val intent = Intent(context, ScreenRecorderService::class.java)
			intent.action = ScreenRecorderService.ACTION_START
			intent.putExtras(data)
			context.startService(intent)
		}
	}

	/**
	 * スクリーンキャプチャー終了要求
	 */
	fun stopScreenCapture() {
		if (isReceived && isRecording) {
			if (DEBUG) Log.v(TAG, "stopScreenCapture:")
			val context = getApplication<Application>()
			val intent = Intent(context, ScreenRecorderService::class.java)
			intent.action = ScreenRecorderService.ACTION_STOP
			context.startService(intent)
		}
	}

	/**
	 * スクリーンキャプチャの状態要求
	 */
	fun queryRecordingStatus() {
		if (DEBUG) Log.v(TAG, "queryRecording:")
		val context = getApplication<Application>()
		val intent = Intent(context, ScreenRecorderService::class.java)
		intent.action = ScreenRecorderService.ACTION_QUERY_STATUS
		context.startService(intent)
	}

	private inner class MyBroadcastReceiver: BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val action = intent?.action
			if (DEBUG) Log.v(TAG, "onReceive:${action}")
			if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT == action) {
				val recording = intent.getBooleanExtra(
					ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false)
//				val isPausing = intent.getBooleanExtra(
//					ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false)
				isReceived = true
				if (isRecording != recording) {
					isRecording = recording
					isChecked.value = recording
				}
			}
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = ScreenCaptureViewModel::class.java.simpleName
	}
}
