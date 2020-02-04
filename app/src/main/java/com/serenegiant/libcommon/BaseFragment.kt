package com.serenegiant.libcommon
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.serenegiant.system.BuildCheck
import com.serenegiant.utils.HandlerThreadHandler

open class BaseFragment : Fragment() {

	protected val mSync = Any()

	override fun onAttach(context: Context) {
		super.onAttach(context)
		mAsyncHandler = HandlerThreadHandler.createHandler("$TAG:async")
	}

	override fun onStart() {
		super.onStart()
		if (BuildCheck.isAndroid7()) {
			internalOnResume()
		}
	}

	override fun onResume() {
		super.onResume()
		if (!BuildCheck.isAndroid7()) {
			internalOnResume()
		}
	}

	override fun onPause() {
		if (!BuildCheck.isAndroid7()) {
			internalOnPause()
		}
		super.onPause()
	}

	override fun onStop() {
		if (BuildCheck.isAndroid7()) {
			internalOnPause()
		}
		super.onStop()
	}

	override fun onDestroy() {
		internalRelease()
		super.onDestroy()
	}

	/**
	 * actual methods of #onResume (for before Android 7)
	 * #onStart(for Android 7 and later)
	 * never call #onResume, #onStart in this method otherwise it raise infinite loop.
	 * if you need, pls override this instead of #onResume/#onStart
	 */
	protected open fun internalOnResume() {}

	/**
	 * actual methods of #onPause (for before Android 7)
	 * #onStop(for Android 7 and later)
	 * never call #onResume, #onStart in this method otherwise it raise infinite loop.
	 * if you need, pls override this instead of #onPause/#onStop
	 */
	protected open fun internalOnPause() {
		clearToast()
	}

	protected open fun internalRelease() {
		mUIHandler.removeCallbacksAndMessages(null)
		if (mAsyncHandler != null) {
			mAsyncHandler!!.removeCallbacksAndMessages(null)
			try {
				mAsyncHandler!!.looper.quit()
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
			}
			mAsyncHandler = null
		}
	}

	/**
	 * helper method to handle back key for Fragment
	 * because originally there are no back key event on Fragment.
	 * @return true: handled
	 */
	fun onBackPressed(): Boolean {
		return false
	}

	protected fun popBackStack() {
		val activity: Activity? = activity
		if ((activity == null) || activity.isFinishing) return
		try {
			parentFragmentManager.popBackStack()
		} catch (e: Exception) {
			Log.w(TAG, e)
		}
	}

//================================================================================
	/** Handler for UI operation   */
	private val mUIHandler = Handler(Looper.getMainLooper())
	private val mUiThread = mUIHandler.looper.thread
	private var mAsyncHandler: Handler? = null
	/**
	 * helper method to run specific task on UI thread
	 * @param task
	 * @param duration
	 */
	@JvmOverloads
	fun runOnUiThread(task: Runnable, duration: Long = 0L) {
		mUIHandler.removeCallbacks(task)
		if ((duration > 0) || (Thread.currentThread() !== mUiThread)) {
			mUIHandler.postDelayed(task, duration)
		} else {
			try {
				task.run()
			} catch (e: Exception) {
				Log.w(TAG, e)
			}
		}
	}

	/**
	 * remove specific task from UI thread if it is not run yet.
	 * @param task
	 */
	fun removeFromUiThread(task: Runnable?) {
		if (task == null) return
		mUIHandler.removeCallbacks(task)
	}

	/**
	 * remove all pending task from UI thread
	 */
	fun removeFromUiThreadAll() {
		mUIHandler.removeCallbacksAndMessages(null)
	}
	/**
	 * helper method to run specific Runnable on worker thread
	 * if same Runnable is waiting to execute, it will removed.
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	/**
	 * helper method to run specific Runnable on worker thread
	 * if same Runnable is waiting to execute, it will removed.
	 * @param task
	 */
	@Throws(IllegalStateException::class)
	protected fun queueEvent(task: Runnable?, delay: Long = 0) {
		if (task == null) return
		synchronized(mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler!!.removeCallbacks(task)
				if (delay > 0) {
					mAsyncHandler!!.postDelayed(task, delay)
				} else {
					mAsyncHandler!!.post(task)
				}
			} else {
				throw IllegalStateException("worker thread is not ready")
			}
		}
	}

	/**
	 * helper method to remove specific Runnable from worker thread if it does not run yet.
	 * @param task
	 */
	protected fun removeEvent(task: Runnable?) {
		synchronized(mSync) {
			if ((mAsyncHandler != null) && (task != null)) {
				mAsyncHandler!!.removeCallbacks(task)
			}
		}
	}

	/**
	 * helper method to remove all pending Runnable/message from worker thread
	 */
	protected fun removeEventAll() {
		synchronized(mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler!!.removeCallbacksAndMessages(null)
			}
		}
	}

//================================================================================
	private var mToast: Toast? = null

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 * @param args
	 */
	protected fun showToast(duration: Int, msg: String?, vararg args: Any?) {
		runOnUiThread(Runnable {
			try {
				if (mToast != null) {
					mToast!!.cancel()
					mToast = null
				}
				val _msg = String.format(msg!!, *args)
				mToast = Toast.makeText(activity, _msg, duration)
				mToast!!.show()
			} catch (e: Exception) { // ignore
			}
		}, 0)
	}

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 */
	protected fun showToast(duration: Int, @StringRes msg: Int, vararg args: Any?) {
		runOnUiThread(Runnable {
			try {
				if (mToast != null) {
					mToast!!.cancel()
					mToast = null
				}
				val _msg = args.let { getString(msg, it) }
				mToast = Toast.makeText(activity, _msg, duration)
				mToast!!.show()
			} catch (e: Exception) { // ignore
			}
		}, 0)
	}

	/**
	 * Toastが表示されていればキャンセルする
	 */
	protected fun clearToast() {
		try {
			if (mToast != null) {
				mToast!!.cancel()
				mToast = null
			}
		} catch (e: Exception) { // ignore
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = BaseFragment::class.java.simpleName
	}
}