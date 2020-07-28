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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.serenegiant.dialog.PermissionDescriptionDialogV4
import com.serenegiant.dialog.PermissionDescriptionDialogV4.DialogResultListener
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener
import com.serenegiant.libcommon.list.DummyContent
import com.serenegiant.libcommon.list.DummyContent.DummyItem
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionCheck

class MainActivity
	: AppCompatActivity(),
		OnListFragmentInteractionListener, DialogResultListener {

	private var mIsResumed = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		DummyContent.createItems(this, R.array.list_items)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.container, TitleFragment.newInstance(1))
				.commit()
		}
	}

	override fun onStart() {
		super.onStart()
		if (BuildCheck.isAndroid7()) {
			mIsResumed = true
			internalOnResume()
		}
	}

	override fun onResume() {
		super.onResume()
		if (!BuildCheck.isAndroid7()) {
			mIsResumed = true
			internalOnResume()
		}
	}

	override fun onPause() {
		if (!BuildCheck.isAndroid7()) {
			internalOnPause()
			mIsResumed = false
		}
		super.onPause()
	}

	override fun onStop() {
		if (BuildCheck.isAndroid7()) {
			internalOnPause()
			mIsResumed = false
		}
		super.onStop()
	}

	protected fun internalOnResume() {
	}

	protected fun internalOnPause() {
		clearToast()
	}

	/**
	 * helper method to get this Activity is paused or not
	 * @return true: this Activity is paused, false: resumed
	 */
	val isPaused: Boolean
		get() = !mIsResumed

	override fun onBackPressed() {
		if (DEBUG) Log.v(TAG, "onBackPressed:")
		// Fragment内の子Fragmentを切り替えた時にbackキーを押すと
		// Fragment自体がpopBackされてしまうのに対するworkaround
		val fm = supportFragmentManager
		val fragment = fm.findFragmentById(R.id.container)
		if (fragment is BaseFragment) {
			val child_fm = fragment.getChildFragmentManager()
			if (child_fm.backStackEntryCount > 0) { // HomeFragmentの子Fragmentがバックスタックに有る時はそれをpopBackする
				child_fm.popBackStack()
				return
			}
			if (fragment.onBackPressed()) {
				return
			}
		}
		super.onBackPressed()
	}

	override fun onListFragmentInteraction(item: DummyItem) {
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:$item")
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:enableVSync=${BuildConfig.ENABLE_VSYNC}")
		var fragment: Fragment? = null
		when (item.id) {
			0 -> {	// SAF
				fragment = SAFUtilsFragment()
			}
			1 -> {	// SAFContentProvider
				if (BuildCheck.isLollipop()) {
					fragment = SAFFilerFragment()
				}
			}
			2 -> {	// NetworkConnection
				fragment = NetworkConnectionFragment.newInstance()
			}
			3 -> {	// UsbMonitor
				if (BuildCheck.isAndroid9()
					&& !checkPermissionCamera()) {
					return
				}
				fragment = UsbFragment.newInstance()
			}
			4 -> {	// Camera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera)
			}
			5 -> {	// Camera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraRecFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera_rec)
			}
			6 -> {	// EffectCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = EffectCameraFragment.newInstance()
			}
			7 -> {	// MixCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_mix, R.string.title_mix_camera)
			}
			8 -> {	// OverlayCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_overlay, R.string.title_overlay_camera)
			}
			9 -> {	// VideoSourceCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_video_source, R.string.title_video_source_camera)
			}
			10 -> {	// VideoSourceDistributionCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_distributor, R.string.title_video_source_dist_camera)
			}
			11 -> {	// ImageViewCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_image_view, R.string.title_image_view_camera)
			}
			12 -> {	// TextureViewCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_texture_view, R.string.title_texture_view_camera)
			}
			13 -> {	// SimpleCameraGL
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_simple_camera_gl, R.string.title_simple_gl_camera)
			}
			14 -> {	// CameraSurface
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraSurfaceFragment()
			}
			15 -> {	// Galley
				if (!checkPermissionWriteExternalStorage()) {
					return
				}
				fragment = GalleyFragment()
			}
			16 -> {	// Galley(RecyclerView)
				if (!checkPermissionWriteExternalStorage()) {
					return
				}
				fragment = GalleyFragment2()
			}
			17 -> {	// NumberKeyboard
				fragment = NumberKeyboardFragment()
			}
			18 -> {	// ViewSlider
				fragment = ViewSliderFragment()
			}
			19 -> {	// ProgressView
				fragment = ProgressFragment()
			}
			else -> {
			}
		}
		if (fragment != null) {
			supportFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, fragment)
				.commit()
		}
	}

//================================================================================
	/**
	 * callback listener from MessageDialogFragment
	 *
	 * @param dialog
	 * @param requestCode
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	override fun onDialogResult(
		dialog: PermissionDescriptionDialogV4, requestCode: Int,
		permissions: Array<String>, result: Boolean) {
		if (result) { // メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode)
				return
			}
		}
		// メッセージダイアログでキャンセルされた時とAndroid6でない時は自前でチェックして#checkPermissionResultを呼び出す
		for (permission in permissions) {
			checkPermissionResult(requestCode, permission,
				PermissionCheck.hasPermission(this, permission))
		}
	}

	/**
	 * override this method to handle result of permission request
	 * actual handling of requesting permission is delegated on #checkPermissionResult
	 *
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	override fun onRequestPermissionsResult(requestCode: Int,
		permissions: Array<String>, grantResults: IntArray) {

		super.onRequestPermissionsResult(requestCode, permissions, grantResults) // 何もしてないけど一応呼んどく
		val n = Math.min(permissions.size, grantResults.size)
		for (i in 0 until n) {
			checkPermissionResult(requestCode, permissions[i],
				grantResults[i] == PackageManager.PERMISSION_GRANTED)
		}
	}

	/**
	 * actual handling of requesting permission
	 * this method just show Toast if permission request failed
	 *
	 * @param requestCode
	 * @param permission
	 * @param result
	 */
	protected fun checkPermissionResult(equestCode: Int,
		permission: String?, result: Boolean) {

		// パーミッションがないときにはメッセージを表示する
		if (!result && permission != null) {
			if (Manifest.permission.RECORD_AUDIO == permission) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_audio)
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_ext_storage)
			}
			if (Manifest.permission.CAMERA == permission) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_camera)
			}
			if (Manifest.permission.INTERNET == permission) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_network)
			}
			if (Manifest.permission.ACCESS_FINE_LOCATION == permission) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_location)
			}
		}
	}

	/**
	 * check permission to access external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	protected fun checkPermissionWriteExternalStorage(): Boolean {
		if (!PermissionCheck.hasWriteExternalStorage(this)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_EXT_STORAGE,
				arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
			return false
		}
		return true
	}

	/**
	 * check permission to record audio
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to record audio
	 */
	protected fun checkPermissionAudio(): Boolean {
		if (!PermissionCheck.hasAudio(this)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_AUDIO,
				arrayOf(Manifest.permission.RECORD_AUDIO))
			return false
		}
		return true
	}

	/**
	 * check permission to access internal camera
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access internal camera
	 */
	protected fun checkPermissionCamera(): Boolean {
		if (!PermissionCheck.hasCamera(this)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_CAMERA,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_CAMERA,
				arrayOf(Manifest.permission.CAMERA))
			return false
		}
		return true
	}

	/**
	 * check permission to access network
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	protected fun checkPermissionNetwork(): Boolean {
		if (!PermissionCheck.hasNetwork(this)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_NETWORK,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_NETWORK,
				arrayOf(Manifest.permission.INTERNET))
			return false
		}
		return true
	}

	/**
	 * check permission to access gps
	 * and request to show detail dialog to request permission
	 * @return true already have permission to access gps
	 */
	protected fun checkPermissionLocation(): Boolean {
		if (!PermissionCheck.hasAccessLocation(this)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_LOCATION,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_LOCATION,
				arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
			return false
		}
		return true
	}

	/**
	 * check permission to of READ_PHONE_STATE
	 * and request to show detail dialog to request permission
	 * This permission is necessarily to get hardware ID on device like IMEI.
	 *
	 * @return true already have permission of READ_PHONE_STATE
	 */
	protected fun checkPermissionHardwareId(): Boolean {
		if (!PermissionCheck.hasPermission(this,
				Manifest.permission.READ_PHONE_STATE)) {
			PermissionDescriptionDialogV4.showDialog(this,
				REQUEST_PERMISSION_HARDWARE_ID,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_HARDWARE_ID,
				arrayOf(Manifest.permission.READ_PHONE_STATE))
			return false
		}
		return true
	}

	//================================================================================
	private var mToast: Toast? = null

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 * @param args
	 */
	protected fun showToast(duration: Int, msg: String?, vararg args: Any?) {
		runOnUiThread {
			try {
				if (mToast != null) {
					mToast!!.cancel()
					mToast = null
				}
				val _msg = if (args != null) String.format(msg!!, *args) else msg!!
				mToast = Toast.makeText(this@MainActivity, _msg, duration)
				mToast!!.show()
			} catch (e: Exception) { // ignore
			}
		}
	}

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 */
	protected fun showToast(duration: Int, @StringRes msg: Int, vararg args: Any?) {
		runOnUiThread {
			try {
				if (mToast != null) {
					mToast!!.cancel()
					mToast = null
				}
				val _msg = args?.let { getString(msg, it) } ?: getString(msg)
				mToast = Toast.makeText(this@MainActivity, _msg, duration)
				mToast!!.show()
			} catch (e: Exception) {
				if (DEBUG) Log.d(TAG, "clearToast", e)
			}
		}
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
		} catch (e: Exception) {
			if (DEBUG) Log.d(TAG, "clearToast", e)
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = MainActivity::class.java.simpleName
		private const val ID_PERMISSION_REASON_AUDIO = R.string.permission_audio_recording_reason
		private const val ID_PERMISSION_REQUEST_AUDIO = R.string.permission_audio_recording_request
		private const val ID_PERMISSION_REASON_NETWORK = R.string.permission_network_reason
		private const val ID_PERMISSION_REQUEST_NETWORK = R.string.permission_network_request
		private const val ID_PERMISSION_REASON_EXT_STORAGE = R.string.permission_ext_storage_reason
		private const val ID_PERMISSION_REQUEST_EXT_STORAGE = R.string.permission_ext_storage_request
		private const val ID_PERMISSION_REASON_CAMERA = R.string.permission_camera_reason
		private const val ID_PERMISSION_REQUEST_CAMERA = R.string.permission_camera_request
		private const val ID_PERMISSION_REQUEST_HARDWARE_ID = R.string.permission_hardware_id_request
		private const val ID_PERMISSION_REASON_LOCATION = R.string.permission_location_reason
		private const val ID_PERMISSION_REQUEST_LOCATION = R.string.permission_location_request
		/** request code for WRITE_EXTERNAL_STORAGE permission  */
		private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345
		/** request code for RECORD_AUDIO permission  */
		private const val REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567
		/** request code for CAMERA permission  */
		private const val REQUEST_PERMISSION_CAMERA = 0x345678
		/** request code for INTERNET permission  */
		private const val REQUEST_PERMISSION_NETWORK = 0x456789
		/** request code for READ_PHONE_STATE permission  */
		private const val REQUEST_PERMISSION_HARDWARE_ID = 0x567890
		/** request code for ACCESS_FINE_LOCATION permission  */
		private const val REQUEST_PERMISSION_LOCATION = 0x678901
	}
}