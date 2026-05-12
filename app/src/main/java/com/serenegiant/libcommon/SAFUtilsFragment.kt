package com.serenegiant.libcommon

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenegiant.dialog.ConfirmDialogV4
import com.serenegiant.dialog.ConfirmDialogV4.ConfirmDialogListener
import com.serenegiant.documentfile.SAFPermission
import com.serenegiant.documentfile.SAFUtils
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.ArrayListRecyclerViewAdapter.ArrayListRecyclerViewListener
import com.serenegiant.widget.RecyclerViewWithEmptyView
import com.serenegiant.widget.StringsRecyclerViewAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

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
*/ /**
 * SAFUtilクラスのテスト用Fragment
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SAFUtilsFragment: BaseFragment(), ConfirmDialogListener {

	private lateinit var mList: RecyclerViewWithEmptyView
	private lateinit var mEditText: EditText
	private var mAdapter: StringsRecyclerViewAdapter? = null
	private lateinit var mSAFPermission: SAFPermission

	private val _requestCode = MutableStateFlow(Const.REQUEST_ACCESS_SD)
	private val requestCodeString = _requestCode.map {
		it.toString()
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		// XXX Fragmentの場合SAFPermissionを#onAttachまたは#onCreateで初期化しないといけない
		mSAFPermission = SAFPermission(
			this,
			object : SAFPermission.SAFCallback {
				override fun onResult(treeId: Int, uri: Uri) {
					updateSAFPermissions()
				}

				override fun onFailed(treeId: Int) {
					updateSAFPermissions()
				}
			}
		)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater = ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_Usb)
		return customInflater.inflate(R.layout.fragment_safutils, container, false)
		.apply {
			mList = findViewById(R.id.list)
			mList.setEmptyView(findViewById(R.id.empty))
			val button = findViewById<ImageButton>(R.id.add_btn)
			button.setOnClickListener { v ->
				if (DEBUG) Log.v(TAG, "onClick:$v")
				if (v.id == R.id.add_btn) {
					val requestCode = _requestCode.value
					if (requestCode != 0 && requestCode and 0xffff == requestCode) {
						if (DEBUG) Log.v(TAG, "onClick:request SAF permission,requestCode=$requestCode")
						if (!SAFUtils.hasPermission(requireContext(), requestCode)) {
							requestPermission(requestCode)
						} else {
							ConfirmDialogV4.showDialog(
								this@SAFUtilsFragment,
								requestCode, R.string.title_request_saf_permission,
								"Already has permission for requestCode($requestCode)",
								true
							)
						}
					} else {
						Toast.makeText(
							requireContext(),
							"Fragmentからは下位16ビットしかリクエストコードとして使えない,$requestCode",
							Toast.LENGTH_SHORT
						).show()
					}
				}
			}
			mEditText = findViewById(R.id.editText)
			mEditText.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(
					s: CharSequence?, start: Int, count: Int, after: Int) {
					// テキストが変更される前に呼ばれる
				}

				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					// テキストが変更された直後に呼ばれる（入力反映後）
				}

				override fun afterTextChanged(s: Editable?) {
					// テキストが変更された直後（確定後）に呼ばれる
					_requestCode.value = parseIntWithoutException(s?.toString() ?: "", _requestCode.value)
				}
			})
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		initView()
	}

	override fun internalOnResume() {
		super.internalOnResume()
		if (DEBUG) Log.v(TAG, "internalOnResume:")
		updateSAFPermissions()
	}

	override fun internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:")
		super.internalOnPause()
	}

	/**
	 * 指定したリクエストコードに対するパーミッションをすでに保持しているときに
	 * 再設定するかどうかの問い合わせに対するユーザーレスポンスの処理
	 * @param dialog
	 * @param requestCode
	 * @param result DialogInterface#BUTTONxxx
	 * @param userArgs
	 */
	override fun onConfirmResult(
		dialog: ConfirmDialogV4,
		requestCode: Int, result: Int, userArgs: Bundle?
	) {
		if (result == DialogInterface.BUTTON_POSITIVE) {
			// ユーザーがOKを押したときの処理, パーミッションを要求する
			requestPermission(requestCode)
		}
	}
	//--------------------------------------------------------------------------------
	/**
	 * 表示を初期化
	 */
	private fun initView() {
		mAdapter = StringsRecyclerViewAdapter(
			R.layout.list_item_title, R.id.content,
			ArrayList()
		)
		mAdapter!!.setOnItemClickListener(
			object : ArrayListRecyclerViewListener<String?> {
				override fun onItemClick(
					parent: RecyclerView.Adapter<*>,
					view: View,
					position: Int, item: String?
				) {
				}

				override fun onItemLongClick(
					parent: RecyclerView.Adapter<*>,
					view: View,
					position: Int, item: String?
				): Boolean {
					if (!TextUtils.isEmpty(item)) {
						val v = item!!.split("@").toTypedArray()
						if (v.size == 2) {
							try {
								val requestCode = v[0].toInt()
								if (requestCode != 0) {
									Toast.makeText(
										requireContext(),
										"release permission,requestCode=$requestCode",
										Toast.LENGTH_SHORT
									).show()
									SAFUtils.releasePersistableUriPermission(
										requireContext(),
										requestCode
									)
									updateSAFPermissions()
									return true
								}
							} catch (e: NumberFormatException) {
								Log.w(TAG, e)
							}
						}
					}
					return false
				}

				override fun onItemSelected(
					parent: RecyclerView.Adapter<*>,
					view: View, position: Int,
					item: String?
				) {
					if (DEBUG) Log.v(TAG, "onItemSelected:position=$position,item=$item")
				}

				override fun onNothingSelected(parent: RecyclerView.Adapter<*>) {
					if (DEBUG) Log.v(TAG, "onNothingSelected:")
				}
			})
		mList.layoutManager = LinearLayoutManager(requireContext())
		mList.adapter = mAdapter
	}

	/**
	 * 指定したリクエストコードに対するパーミッションを要求
	 * @param requestCode
	 */
	private fun requestPermission(requestCode: Int) {
		SAFUtils.releasePersistableUriPermission(requireContext(), requestCode)
		mSAFPermission.requestPermission(requestCode)
	}

	/**
	 * 保持しているパーミッション一覧表示を更新
	 */
	private fun updateSAFPermissions() {
		if (DEBUG) Log.v(TAG, "updateSAFPermissions:")
		val map = SAFUtils.getStorageUriAll(requireContext())
		val list: MutableList<String> = ArrayList()
		for ((key, value) in map) {
			list.add("$key@$value")
		}
		val adapter = mAdapter
		adapter?.replaceAll(list)
	}

	/**
	 * 例外生成せずに10進整数文字列を整数に変換、変換できないときはデフォルト値を返す
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	private fun parseIntWithoutException(value: String, defaultValue: Int): Int {
		var result = defaultValue
		if (!TextUtils.isEmpty(value)) {
			try {
				result = value.toInt()
			} catch (e: NumberFormatException) {
				if (DEBUG) Log.d(TAG, "setRequestCodeString:", e)
			}
		}

		return result
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SAFUtilsFragment::class.java.simpleName

		fun newInstance() = SAFUtilsFragment().apply {
			arguments = Bundle().apply {
				// 今は何もない
			}
		}
	}
}
