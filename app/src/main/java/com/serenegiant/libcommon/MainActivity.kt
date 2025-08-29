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

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.serenegiant.dialog.RationalDialogV4
import com.serenegiant.libcommon.TitleFragment.OnListFragmentInteractionListener
import com.serenegiant.libcommon.list.DummyContent
import com.serenegiant.libcommon.list.DummyContent.DummyItem
import com.serenegiant.media.VideoConfig
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils
import com.serenegiant.system.PermissionUtils.PermissionCallback
import com.serenegiant.usb.UsbPermission
import com.serenegiant.widget.GLPipelineView

class MainActivity
	: AppCompatActivity(),
		OnListFragmentInteractionListener,
		RationalDialogV4.DialogResultListener {

	private lateinit var READ_MEDIA_PERMISSIONS: Array<String>
	private lateinit var mPermissions: PermissionUtils
	private var mIsResumed = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		// パーミッション要求の準備
		READ_MEDIA_PERMISSIONS = PermissionUtils.requestedPermissions(this, PermissionUtils.READ_MEDIA_PERMISSIONS)
		mPermissions = PermissionUtils(this, mCallback)
			.prepare(this@MainActivity, LOCATION_PERMISSIONS)
			.prepare(this@MainActivity, READ_MEDIA_PERMISSIONS)
		DummyContent.createItems(this, R.array.list_items)
		if (savedInstanceState == null) {
			// IRecorderで使う最大録画時間を無制限(-1)にする
			VideoConfig.DEFAULT_CONFIG.setMaxDuration(-1)
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.container, TitleFragment.newInstance(1))
				.commit()
			LocalBroadcastManager.getInstance(this)
				.registerReceiver(object: BroadcastReceiver() {
					override fun onReceive(context: Context?, intent: Intent?) {
						if (DEBUG) Log.v(TAG, "onReceive:$intent")
						when (intent?.action) {
							Const.ACTION_REQUEST_USB_PERMISSION -> {
								val device = IntentCompat.getParcelableExtra(intent, Const.EXTRA_REQUEST_USB_PERMISSION, UsbDevice::class.java)
								if (device != null) {
									UsbPermission.requestPermission(this@MainActivity, device)
								}
							}
						}
					}
				}, IntentFilter().apply {
					addAction(Const.ACTION_REQUEST_USB_PERMISSION)
				})
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

	private fun internalOnResume() {
	}

	private fun internalOnPause() {
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
			val childFm = fragment.getChildFragmentManager()
			if (childFm.backStackEntryCount > 0) { // HomeFragmentの子Fragmentがバックスタックに有る時はそれをpopBackする
				childFm.popBackStack()
				return
			}
			if (fragment.onBackPressed()) {
				return
			}
		}
		super.onBackPressed()
	}

	@SuppressLint("NewApi")
	override fun onListFragmentInteraction(item: DummyItem) {
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:$item")
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:enableVSync=${BuildConfig.ENABLE_VSYNC}")
		var fragment: Fragment? = null
		when (item.id) {
			R.string.title_request_saf_permission -> {	// SAF
				if (BuildCheck.isLollipop()) {
					fragment = SAFUtilsFragment.newInstance()
				} else {
					showToast(Toast.LENGTH_SHORT, "This feature is only available on API>=21")
				}
			}
			R.string.title_saf_filer -> {	// SAFContentProvider
				if (BuildCheck.isLollipop()) {
					fragment = SAFFilerFragment.newInstance()
				} else {
					showToast(Toast.LENGTH_SHORT, "This feature is only available on API>=21")
				}
			}
			R.string.title_network_connection -> {	// NetworkConnection
				if (!checkPermissionNetwork() || !checkPermissionWiFiState() || !checkPermissionLocation()) {
					return
				}
				fragment = NetworkConnectionFragment.newInstance()
			}
			R.string.title_telephony -> {	// Telephony
				if (!checkPermissionPhoneState() || !checkPermissionSMS() || !checkPermissionPhoneNumber()) {
					return
				}
				fragment = TelephonyFragment.newInstance()
			}
			R.string.title_usb_monitor -> {	// UsbMonitor
				if (BuildCheck.isAndroid9()
					&& !checkPermissionCamera()) {
					return
				}
				fragment = UsbMonitorFragment.newInstance()
			}
			R.string.title_usb_permission -> {	// UsbDetector+UsbPermission
				if (BuildCheck.isAndroid9()
					&& !checkPermissionCamera()) {
					return
				}
				fragment = UsbPermissionFragment.newInstance()
			}
			R.string.title_window_insets -> {
				fragment = WindowInsetsFragment.newInstance()
			}
			R.string.title_audio_record -> {
				if (!checkPermissionAudio()) {
					return
				}
				fragment = AudioRecordFragment.newInstance()
			}
			R.string.title_color_picker -> {
				fragment = ColorPickerFragment.newInstance()
			}
			R.string.title_camera -> {	// Camera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera)
			}
			R.string.title_camera_rec -> {	// Camera(MediaAVRecorder)
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraRecFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera_rec)
			}
			R.string.title_camera_rec_split -> {	// Camera(MediaAVSplitRecorder/MediaAVSplitRecorderV2)
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraSplitRecFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera_rec_split)
			}
			R.string.title_camera_rec_timelapse -> {	// Camera(MediaAVTimelapseRecorder)
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraTimelapseRecFragment.newInstance(
					R.layout.fragment_camera, R.string.title_camera_rec_timelapse)
			}
			R.string.title_camera_rec_pipeline -> {	// Camera(MediaAVRecorder+EncoderPipeline)
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraRecFragment.newInstance(
					R.layout.fragment_simple_video_source_camera, R.string.title_camera_rec_pipeline,
					GLPipelineView.EFFECT_PLUS_SURFACE,
					true // trueならEncoderPipelineを使った録画, falseならSurfaceEncoderを使った録画
				)
			}
			R.string.title_camera_face_detect -> {	// Camera(FaceDetectPipeline)
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraRecFragment.newInstance(
					R.layout.fragment_simple_video_source_camera, R.string.title_camera_face_detect,
					GLPipelineView.PREVIEW_ONLY,
					enablePipelineEncode = false,
					enableFaceDetect = true
				)
			}
			R.string.title_effect_camera -> {	// EffectCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = EffectCameraFragment.newInstance()
			}
			R.string.title_mix_camera -> {	// MixCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_mix, R.string.title_mix_camera)
			}
			R.string.title_overlay_camera -> {	// OverlayCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_overlay, R.string.title_overlay_camera)
			}
			R.string.title_mask_camera -> {	// MaskCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = MaskCameraSurfaceFragment.newInstance()
			}
			R.string.title_media_effect_camera -> { // MediaEffectCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = MediaEffectCameraSurfaceFragment.newInstance()
			}
			R.string.title_video_source_camera -> {	// VideoSourceCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_video_source, R.string.title_video_source_camera)
			}
			R.string.title_video_source_dist_camera -> {	// VideoSourceDistributionCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_distributor, R.string.title_video_source_dist_camera)
			}
			R.string.title_image_view_camera -> {	// ImageViewCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_image_view, R.string.title_image_view_camera)
			}
			R.string.title_texture_view_camera -> {	// TextureViewCamera
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_texture_view, R.string.title_texture_view_camera)
			}
			R.string.title_simple_gl_camera -> {	// SimpleCameraGL
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_simple_camera_gl, R.string.title_simple_gl_camera)
			}
			R.string.title_camera_surface -> {	// CameraSurface
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraSurfaceFragment.newInstance()
			}
			R.string.title_simple_camera_source -> {	// SimpleVideoSourceCameraGLView
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_simple_video_source_camera, R.string.title_simple_camera_source)
			}
			R.string.title_dummy_camera_source -> {	// DummyCameraGLView
				if (!checkPermissionCamera()
					|| !checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = CameraFragment.newInstance(
					R.layout.fragment_camera_dummy_source, R.string.title_dummy_camera_source)
			}
			R.string.title_galley -> {	// Galley
				if (!checkPermissionReadExternalStorage()) {
					return
				}
				fragment = GalleyFragment()
			}
			R.string.title_galley_recycler_cursor -> {	// Galley(RecyclerView,Cursor)
				if (!checkPermissionReadExternalStorage()) {
					return
				}
				fragment = GalleyFragment2()
			}
			R.string.title_number_keyboard -> {	// NumberKeyboard
				fragment = NumberKeyboardFragment.newInstance()
			}
			R.string.title_view_slider -> {	// ViewSlider
				fragment = ViewSliderFragment.newInstance()
			}
			R.string.title_progress_view -> {	// ProgressView
				fragment = ProgressFragment.newInstance()
			}
			R.string.title_permissions -> {	// PermissionUtils
				fragment = PermissionFragment.newInstance()
			}
			R.string.title_screen_capture -> {	// ScreenCapture
				if (!checkPermissionWriteExternalStorage()
					|| !checkPermissionAudio()) {
					return
				}
				fragment = ScreenCaptureFragment.newInstance()
			}
			R.string.title_checkable_ex -> {	// CheckableEx
				fragment = CheckableExFragment.newInstance()
			}
			R.string.title_rpg_message_view -> {
				fragment = RPGMessageFragment.newInstance()
			}
			R.string.title_bitmap_helper -> {
				fragment = BitmapFragment.newInstance()
			}
			R.string.title_cpu_monitor -> {
				fragment = CpuMonitorFragment.newInstance()
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
	 * callback listener from RationalDialogV4
	 *
	 * @param dialog
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	override fun onDialogResult(
		dialog: RationalDialogV4,
		permissions: Array<String>, result: Boolean) {
		if (DEBUG) Log.v(TAG, "onDialogResult:result=${result}," + permissions.contentToString())
		if (result) { // メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				mPermissions.requestPermission(permissions, false)
				return
			}
		}
		// メッセージダイアログでキャンセルされた時とAndroid6でない時は自前でチェックして#checkPermissionResultを呼び出す
		for (permission in permissions) {
			checkPermissionResult(permission,
				PermissionUtils.hasPermission(this, permission))
		}
	}

	/**
	 * actual handling of requesting permission
	 * this method just show Toast if permission request failed
	 *
	 * @param permission
	 * @param result
	 */
	private fun checkPermissionResult(
		permission: String?, result: Boolean) {

		// パーミッションがないときにはメッセージを表示する
		if (!result && permission != null) {
			if (Manifest.permission.RECORD_AUDIO == permission) {
				showToast(Toast.LENGTH_SHORT, com.serenegiant.common.R.string.permission_audio)
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
				showToast(Toast.LENGTH_SHORT, com.serenegiant.common.R.string.permission_ext_storage)
			}
			if (Manifest.permission.CAMERA == permission) {
				showToast(Toast.LENGTH_SHORT, com.serenegiant.common.R.string.permission_camera)
			}
			if (Manifest.permission.INTERNET == permission) {
				showToast(Toast.LENGTH_SHORT, com.serenegiant.common.R.string.permission_network)
			}
			if (Manifest.permission.ACCESS_FINE_LOCATION == permission) {
				showToast(Toast.LENGTH_SHORT, com.serenegiant.common.R.string.permission_location)
			}
		}
	}

	/**
	 * check permission to access external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	private fun checkPermissionWriteExternalStorage(): Boolean {
		// API29以降は対象範囲別ストレージ＆MediaStoreを使うのでWRITE_EXTERNAL_STORAGEパーミッションは不要
		return (BuildCheck.isAPI29()
			|| mPermissions.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, true))
	}

	/**
	 * check permission to read external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	private fun checkPermissionReadExternalStorage(): Boolean {
		if (!PermissionUtils.hasReadExternalStorage(this)) {
			if (DEBUG) Log.v(TAG, "checkPermissionReadExternalStorage:request=$READ_MEDIA_PERMISSIONS")
			return mPermissions.requestPermission(READ_MEDIA_PERMISSIONS, true)
		}
		return true
	}

	/**
	 * check permission to record audio
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to record audio
	 */
	private fun checkPermissionAudio(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.RECORD_AUDIO, true)
	}

	/**
	 * check permission to access internal camera
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access internal camera
	 */
	private fun checkPermissionCamera(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.CAMERA, true)
	}

	/**
	 * check permission to access network
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	private fun checkPermissionNetwork(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.INTERNET, true)
	}

	/**
	 * check permission to access wifi state
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	private fun checkPermissionWiFiState(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.ACCESS_WIFI_STATE, true)
	}

	/**
	 * check permission to access gps
	 * and request to show detail dialog to request permission
	 * @return true already have permission to access gps
	 */
	private fun checkPermissionLocation(): Boolean {
		return mPermissions.requestPermission(LOCATION_PERMISSIONS, true)
	}

	/**
	 * check permission to read phone state
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	private fun checkPermissionPhoneState(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.READ_PHONE_STATE, true)
	}

	/**
	 * check permission to read phone state
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	private fun checkPermissionPhoneNumber(): Boolean {
		return !BuildCheck.isAPI26()
			|| mPermissions.requestPermission(Manifest.permission.READ_PHONE_NUMBERS, true)
	}

	/**
	 * check permission to read phone state
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	private fun checkPermissionSMS(): Boolean {
		return mPermissions.requestPermission(Manifest.permission.READ_SMS, true)
	}

	/**
	 * PermissionUtilsからのコールバックリスナー実装
	 */
	private val mCallback: PermissionCallback = object : PermissionCallback {
		override fun onPermissionShowRational(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:$permission")
			// パーミッション要求理由の表示が必要な時
			val dialog: RationalDialogV4? =
				RationalDialogV4.showDialog(this@MainActivity, permission)
			if (dialog == null) {
				if (DEBUG) Log.v(TAG, "onPermissionShowRational:" +
					"デフォルトのダイアログ表示ができなかったので自前で表示しないといけない," + permission)
				if (Manifest.permission.INTERNET == permission) {
					RationalDialogV4.showDialog(this@MainActivity,
						com.serenegiant.common.R.string.permission_title,
						com.serenegiant.common.R.string.permission_network_request,
						arrayOf(Manifest.permission.INTERNET))
				} else if ((Manifest.permission.ACCESS_FINE_LOCATION == permission)
					|| (Manifest.permission.ACCESS_COARSE_LOCATION == permission)) {
					RationalDialogV4.showDialog(this@MainActivity,
						com.serenegiant.common.R.string.permission_title,
						com.serenegiant.common.R.string.permission_location_request,
						LOCATION_PERMISSIONS)
				}
			}
		}

		override fun onPermissionShowRational(permissions: Array<out String>) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:" + permissions.contentToString())
			// 複数パーミッションの一括要求時はデフォルトのダイアログ表示がないので自前で実装する
			if (LOCATION_PERMISSIONS.contentEquals(permissions)) {
				RationalDialogV4.showDialog(this@MainActivity,
					com.serenegiant.common.R.string.permission_title,
					com.serenegiant.common.R.string.permission_location_request,
					LOCATION_PERMISSIONS)
			}
		}

		override fun onPermissionDenied(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionDenied:$permission")
			// ユーザーがパーミッション要求を拒否したときの処理
		}

		override fun onPermission(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermission:$permission")
			// ユーザーがパーミッション要求を承認したときの処理
		}

		override fun onPermissionNeverAskAgain(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:$permission")
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			supportFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, SettingsLinkFragment.newInstance())
				.commit()
		}

		override fun onPermissionNeverAskAgain(permissions: Array<out String>) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:" + permissions.contentToString())
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			supportFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, SettingsLinkFragment.newInstance())
				.commit()
		}
	}

	//================================================================================
	private var mToast: Toast? = null

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 * @param args
	 */
	private fun showToast(duration: Int, msg: String?, vararg args: Any?) {
		runOnUiThread {
			try {
				mToast?.cancel()
				mToast = null
				val text = String.format(msg!!, *args)
				mToast = Toast.makeText(this@MainActivity, text, duration)
				mToast!!.show()
			} catch (e: Exception) { // ignore
			}
		}
	}

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 */
	private fun showToast(duration: Int, @StringRes msg: Int, vararg args: Any?) {
		runOnUiThread {
			try {
				mToast?.cancel()
				mToast = null
				val text = getString(msg, args) ?: getString(msg)
				mToast = Toast.makeText(this@MainActivity, text, duration)
				mToast!!.show()
			} catch (e: Exception) {
				if (DEBUG) Log.d(TAG, "clearToast", e)
			}
		}
	}

	/**
	 * Toastが表示されていればキャンセルする
	 */
	private fun clearToast() {
		try {
			mToast?.cancel()
			mToast = null
		} catch (e: Exception) {
			if (DEBUG) Log.d(TAG, "clearToast", e)
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = MainActivity::class.java.simpleName
		private val LOCATION_PERMISSIONS
			= arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
					  Manifest.permission.ACCESS_COARSE_LOCATION)
	}
}
