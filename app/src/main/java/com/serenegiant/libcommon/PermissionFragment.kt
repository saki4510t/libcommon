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
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.serenegiant.dialog.RationalDialogV4
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils
import com.serenegiant.system.PermissionUtils.PermissionCallback

class PermissionFragment : BaseFragment(), RationalDialogV4.DialogResultListener {
	private var mPermissions: PermissionUtils? = null
	override fun onAttach(context: Context) {
		super.onAttach(context)
		// パーミッション要求の準備
		mPermissions = PermissionUtils(this, mCallback)
			.prepare(this, LOCATION_PERMISSIONS)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_permission, container, false)
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		requestPermissionsAll()
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	private fun requestPermissionsAll() {
		if (DEBUG) Log.v(TAG, "requestPermissionsAll:")
		runOnUiThread({
			if (!checkPermissionWriteExternalStorage()) {
				return@runOnUiThread
			}
			if (!checkPermissionCamera()) {
				return@runOnUiThread
			}
			if (!checkPermissionAudio()) {
				return@runOnUiThread
			}
			if (DEBUG) Log.v(TAG, "requestPermissionsAll:has all permissions")
		})
	}

	/**
	 * check permission to access external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	private fun checkPermissionWriteExternalStorage(): Boolean {
		if (DEBUG) Log.v(TAG, "checkPermissionWriteExternalStorage:")
		// API29以降は対象範囲別ストレージ＆MediaStoreを使うのでWRITE_EXTERNAL_STORAGEパーミッションは不要
		return (BuildCheck.isAPI29()
			|| ((mPermissions != null)
				&& mPermissions!!.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, true)))
	}

	/**
	 * check permission to access external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	private fun checkPermissionCamera(): Boolean {
		if (DEBUG) Log.v(TAG, "checkPermissionCamera:")
		return ((mPermissions != null)
			&& mPermissions!!.requestPermission(Manifest.permission.CAMERA, true))
	}

	/**
	 * check permission to record audio
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to record audio
	 */
	private fun checkPermissionAudio(): Boolean {
		if (DEBUG) Log.v(TAG, "checkPermissionAudio:")
		return ((mPermissions != null)
			&& mPermissions!!.requestPermission(Manifest.permission.RECORD_AUDIO, true))
	}

	/**
	 * check permission to access location info
	 * and request to show detail dialog to request permission
	 * @return true already have permission to access location
	 */
	private fun checkPermissionLocation(): Boolean {
		return ((mPermissions != null)
			&& mPermissions!!.requestPermission(LOCATION_PERMISSIONS, true))
	}

	private val mCallback: PermissionCallback = object : PermissionCallback {
		override fun onPermissionShowRational(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:$permission")
			val dialog = RationalDialogV4.showDialog(this@PermissionFragment, permission)
			if (dialog == null) {
				if (DEBUG) Log.v(TAG, "onPermissionShowRational:" +
					"デフォルトのダイアログ表示ができなかったので自前で表示しないといけない," + permission)
				if (Manifest.permission.INTERNET == permission) {
					RationalDialogV4.showDialog(this@PermissionFragment,
						com.serenegiant.common.R.string.permission_title,
						com.serenegiant.common.R.string.permission_network_request,
						arrayOf(Manifest.permission.INTERNET))
				} else if ((Manifest.permission.ACCESS_FINE_LOCATION == permission)
					|| (Manifest.permission.ACCESS_COARSE_LOCATION == permission)) {
					RationalDialogV4.showDialog(this@PermissionFragment,
						com.serenegiant.common.R.string.permission_title,
						com.serenegiant.common.R.string.permission_location_request,
						LOCATION_PERMISSIONS
					)
				}
			}
		}

		override fun onPermissionShowRational(permissions: Array<String>) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:" + permissions.contentToString())
			// 複数パーミッションの一括要求時はデフォルトのダイアログ表示がないので自前で実装する
			if (LOCATION_PERMISSIONS.contentEquals(permissions)) {
				RationalDialogV4.showDialog(
					this@PermissionFragment,
					com.serenegiant.common.R.string.permission_title,
					com.serenegiant.common.R.string.permission_location_request,
					LOCATION_PERMISSIONS
				)
			}
		}

		override fun onPermissionDenied(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionDenied:$permission")
			// ユーザーがパーミッション要求を拒否したときの処理
			requestPermissionsAll()
		}

		override fun onPermission(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermission:$permission")
			// ユーザーがパーミッション要求を承認したときの処理
			requestPermissionsAll()
		}

		override fun onPermissionNeverAskAgain(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:$permission")
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			parentFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, SettingsLinkFragment.newInstance())
				.commit()
		}

		override fun onPermissionNeverAskAgain(permissions: Array<String>) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:" + permissions.contentToString())
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			parentFragmentManager
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, SettingsLinkFragment.newInstance())
				.commit()
		}
	}

	override fun onDialogResult(
		dialog: RationalDialogV4,
		permissions: Array<String>, result: Boolean) {
		if (DEBUG) Log.v(TAG, "onDialogResult:${result}," + permissions.contentToString())
		if (result) { // メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				if (mPermissions != null) {
					mPermissions!!.requestPermission(permissions, false)
					return
				}
			}
		}
		requestPermissionsAll()
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = PermissionFragment::class.java.simpleName
		private val LOCATION_PERMISSIONS = arrayOf(
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION
		)

		fun newInstance() = PermissionFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
