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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import com.serenegiant.graphics.BitmapHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BitmapFragment : BaseFragment() {
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
		return inflater.inflate(R.layout.fragment_bitmap, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (DEBUG) Log.v(TAG, "onViewCreated:")
		lifecycleScope.launch(Dispatchers.Default) {
			view.findViewById<ImageButton>(R.id.imageButton1).apply {
				setImageBitmap(BitmapHelper.makeCheckBitmap())
			}
			view.findViewById<ImageButton>(R.id.imageButton2).apply {
				setImageBitmap(BitmapHelper.makeCheckBitmap(
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton3).apply {
				setImageBitmap(BitmapHelper.makeCheckBitmap(
					100, 100,
					15, 19,
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton4).apply {
				setImageBitmap(BitmapHelper.makeFilledBitmap(
					100, 100,
					0x7f123456,
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton5).apply {
				// 左上(0,0)から右下(99,99)の黒→白のグラデーション
				setImageBitmap(BitmapHelper.makeGradientBitmap(
					100, 100,
					0xff000000.toInt(), 0xffffffff.toInt(),
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton6).apply {
				// 上から下の黒→白のグラデーション
				setImageBitmap(BitmapHelper.makeGradientBitmap(
					100, 100,
					0xff000000.toInt(), Point(0, 0),
					0xffffffff.toInt(), Point(0, 100),
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton7).apply {
				// 右から左の黒→白のグラデーション
				setImageBitmap(BitmapHelper.makeGradientBitmap(
					100, 100,
					0xff000000.toInt(), Point(0, 0),
					0xffffffff.toInt(), Point(100, 0),
					Bitmap.Config.ARGB_8888))
			}
			view.findViewById<ImageButton>(R.id.imageButton8).apply {
				setImageBitmap(BitmapHelper.getMaskImage0(
					100, 100,
					60, Color.BLUE,
					127, 255))
			}
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = BitmapFragment::class.java.simpleName

		@JvmStatic
		fun newInstance() =
			BitmapFragment().apply {
				arguments = Bundle().apply {
				}
			}
	}
}