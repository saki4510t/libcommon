package com.serenegiant.widget
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.SparseBooleanArray
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * 通常はViewヒエラルキーの上から順にenableでfocusableなViewの
 * dispatchTouchEventを呼び出して最初にtrueを返した子Viewのみが
 * イベント処理を行なえるのに対して、重なり合った複数のViewの下側も
 * 含めてタッチした位置に存在するViewに対してdispatchTouchEventの
 * 呼び出し処理を行うViewGroup
 * (同じタッチイベントで複数の子Viewへタッチイベント生成＆操作できる)
 */
open class MultiDispatchTouchRelativeLayout
	@JvmOverloads
		constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
			: RelativeLayout(context, attrs, defStyleAttr) {

	private val mDispatched = SparseBooleanArray()
	private val mWorkRect = Rect()

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		return if (onFilterTouchEventForSecurity(ev)) {
			var result = false
			val children = childCount
			for (i in 0 until children) {
				val v = getChildAt(i)
				val id = v.hashCode()
				v.getHitRect(mWorkRect)
				if (v.isEnabled && isFocusable
					&& mWorkRect.contains(ev.x.toInt(), ev.y.toInt())
					&& mDispatched[id, true]) {

					// 子Viewが有効＆子ViewのhitRect内をタッチ&子Viewがイベントをハンドリングしているとき
					// 子Viewのローカル座標系への変換してから子ViewのdispatchTouchEventを呼び出す
					val offsetX: Float = scrollX - v.left.toFloat()
					val offsetY: Float = scrollY - v.top.toFloat()
					ev.offsetLocation(offsetX, offsetY)

					val dispatched = v.dispatchTouchEvent(ev)
					mDispatched.put(id, dispatched)
					result = result or dispatched
					// オフセットを元に戻す
					ev.offsetLocation(-offsetX, -offsetY)
				}
			}
			val action = ev.actionMasked
			if ((!result
					|| (action == MotionEvent.ACTION_UP)
					|| (action == MotionEvent.ACTION_CANCEL))
				&& ev.pointerCount == 1) {

				mDispatched.clear()
			}
//			if (!result) {
//				// 子Viewがどれもイベントをハンドリングしなかったときは上位に任せる
//				// もしかすると子ViewのdispatchTouchEventが2回呼び出されるかも
//				result = super.dispatchTouchEvent(ev)
//			}
			if (DEBUG) Log.v(TAG, "dispatchTouchEvent:result=$result")
			result
		} else {
			mDispatched.clear()
			super.dispatchTouchEvent(ev)
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = MultiDispatchTouchRelativeLayout::class.java.simpleName
	}

	init {
		// 子クラスへフォーカスを与えないようにする
		descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
		isFocusable = true
	}
}