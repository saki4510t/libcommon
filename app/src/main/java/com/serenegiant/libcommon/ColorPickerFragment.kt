package com.serenegiant.libcommon

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.serenegiant.dialog.ColorPickerDialogV4

class ColorPickerFragment
	: BaseFragment(), ColorPickerDialogV4.OnColorChangedListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		if (DEBUG) Log.v(TAG, "onCreateView:")
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_color_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		view.findViewById<Button>(R.id.button).apply {
			setOnClickListener {
				ColorPickerDialogV4.show(this@ColorPickerFragment, 1234, R.string.title_dialog_color_picker, 0xffffffff.toInt())
			}
		}

	}

	override fun onColorChanged(dialog: ColorPickerDialogV4, requestCode: Int, color: Int) {
		if (DEBUG) Log.v(TAG, "onColorChanged:$color")
	}

	override fun onCancel(dialog: ColorPickerDialogV4, requestCode: Int) {
		if (DEBUG) Log.v(TAG, "onCancel:")
	}

	override fun onDismiss(dialog: ColorPickerDialogV4, requestCode: Int, color: Int) {
		if (DEBUG) Log.v(TAG, "onDismiss:$color")
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = ColorPickerFragment::class.java.simpleName

		@JvmStatic
		fun newInstance() =
			ColorPickerFragment().apply {
				arguments = Bundle().apply {
				}
			}
	}
}