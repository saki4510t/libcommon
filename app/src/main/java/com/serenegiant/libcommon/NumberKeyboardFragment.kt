package com.serenegiant.libcommon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.serenegiant.system.KeyboardDelegater
import com.serenegiant.view.ViewUtils
import com.serenegiant.widget.KeyboardView

/**
 * NumberKeyboardテスト用フラグメント
 */
class NumberKeyboardFragment : BaseFragment() {

	private var mInputEditText: EditText? = null
	private var mKeyboardView: KeyboardView? = null
	private var mDelegater: KeyboardDelegater? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		requireActivity().title = getString(R.string.title_number_keyboard)
	}

	override fun onCreateView(inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View? {

		if (DEBUG) Log.v(TAG, "onCreateView:")
		val customInflater
			= ViewUtils.createCustomLayoutInflater(requireContext(), inflater, R.style.AppTheme_NumberKeyboard)
		val rootView = customInflater.inflate(R.layout.fragment_numberkeyboard, container, false)
		initView(rootView)
		return rootView;
	}

	override fun internalOnResume() {
		super.internalOnResume()
		mDelegater!!.showKeyboard()
	}

	override fun internalOnPause() {
		mDelegater!!.hideKeyboard()
		super.internalOnPause()
	}

	fun initView(rootView: View) {
		mInputEditText = rootView.findViewById(R.id.edittext)
		mKeyboardView = rootView.findViewById(R.id.number_keyboardview)
		mDelegater = object : KeyboardDelegater(
			mInputEditText!!, mKeyboardView!!, R.xml.keyboard_number) {

			override fun onCancelClick() {
				if (DEBUG) Log.v(TAG, "onCancelClick:")
			}

			override fun onOkClick() {
				if (DEBUG) Log.v(TAG, "onOkClick:")
			}
		}
	}

	companion object {
		private const val DEBUG = true // TODO set false on release
		private val TAG = NumberKeyboardFragment::class.java.simpleName
	}
}