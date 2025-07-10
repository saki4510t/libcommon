package com.serenegiant.mediaeffect
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

import android.opengl.GLES20
import android.util.Log
import com.serenegiant.gl.GLEffectDrawer2D
import com.serenegiant.gl.GLEffectDrawer2D.EffectListener
import com.serenegiant.gl.IEffect
import com.serenegiant.glutils.IMirror
import com.serenegiant.glutils.IMirror.MirrorMode
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile

/**
 * GLEffectDrawer2Dを使って映像効果付与を行うIMediaEffect実装
 * @param effect
 * @param effectListener nullならGLEffectDrawer2Dのデフォルト設定を使う
 */
class MediaEffectGLEffect @JvmOverloads constructor(
	effect: Int,
	effectListener: EffectListener? = null
) : IMediaEffect, IMirror, IEffect {

	private val mLock = ReentrantLock()
	private val mDrawer: GLEffectDrawer2D
	@Volatile
	private var mEnabled = true
	/**
	 * 映像効果番号
	 */
	private var mEffect: Int

	/**
	 * ソーステクスチャがisOES=falseなので上下反転させるのがデフォルト
	 */
	@MirrorMode
	private var mMirror = IMirror.MIRROR_VERTICAL

	/**
	 * コンストラクタ
	 */
	init {
		if (DEBUG) Log.v(TAG, "コンストラクタ:effect=$effect,$effectListener")
		mDrawer = GLEffectDrawer2D(false, false, effectListener)
		mEffect = effect
		mDrawer.effect = mEffect
		mDrawer.mirror = mMirror
	}

	override fun release() {
		if (DEBUG) Log.v(TAG, "release:")
		mDrawer.release()
	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	val mvpMatrix: FloatArray
		get() {
			if (DEBUG) Log.v(TAG, "getMvpMatrix:")
			return mDrawer.mvpMatrix
		}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	fun setMvpMatrix(matrix: FloatArray, offset: Int): MediaEffectGLEffect {
		if (DEBUG) Log.v(TAG, "setMvpMatrix:")
		mDrawer.setMvpMatrix(matrix, offset)
		return this
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	fun getMvpMatrix(matrix: FloatArray, offset: Int) {
		if (DEBUG) Log.v(TAG, "getMvpMatrix:")
		mDrawer.copyMvpMatrix(matrix, offset)
	}

	override fun resize(width: Int, height: Int): MediaEffectGLEffect {
		if (DEBUG) Log.v(TAG, "resize:(" + width + "x" + height + ")")
		mDrawer.setTexSize(width, height)
		return this
	}

	override fun enabled(): Boolean {
		return mEnabled
	}

	override fun setEnable(enable: Boolean): IMediaEffect {
		if (DEBUG) Log.v(TAG, "setEnable:$enable")
		mEnabled = enable
		return this
	}

	/**
	 * if your source texture comes from ISource,
	 * please use this method instead of #apply(final int [], int, int, int)
	 * @param src
	 */
	override fun apply(src: ISource) {
//		if (DEBUG) Log.v(TAG, "apply:enabled=" + mEnabled + ",src=" + src);
		if (!mEnabled) return
		val mirror: Int
		val effect: Int
		mLock.lock()
		try {
			mirror = mMirror
			effect = mEffect
		} finally {
			mLock.unlock()
		}
		if ((effect != mDrawer.effect) || (mirror != mDrawer.mirror)) {
			if (DEBUG) Log.v(TAG, "apply:update effect and mirror")
			mDrawer.effect = effect
			mDrawer.mirror = mirror
		}

		val output = src.outputTargetTexture
		val srcTexIds = src.sourceTexId
		output?.let {
			it.makeCurrent()
			try {
				mDrawer.draw(GLES20.GL_TEXTURE0, srcTexIds[0], src.texMatrix, 0)
			} finally {
				it.swap()
			}
		}
	}

	protected val program: Int
		get() = mDrawer.program

	/**
	 * IMirrorの実装
	 * @param mirror 0:通常, 1:左右反転, 2:上下反転, 3:上下左右反転
	 */
	override fun setMirror(@MirrorMode mirror: Int) {
		if (DEBUG) Log.v(TAG, "setMirror:$mMirror=>$mirror")
		mLock.lock()
		try {
			mMirror = mirror
		} finally {
			mLock.unlock()
		}
	}

	/**
	 * IMirrorの実装
	 * @return
	 */
	@MirrorMode
	override fun getMirror(): Int {
		mLock.lock()
		try {
			return mMirror
		} finally {
			mLock.unlock()
		}
	}

	/**
	 * IEffectの実装
	 * @param effect
	 */
	override fun setEffect(effect: Int) {
		if (DEBUG) Log.v(TAG, "setEffect:$mEffect=>$effect")
		mLock.lock()
		try {
			mEffect = effect
		} finally {
			mLock.unlock()
		}
	}

	/**
	 * IEffectの実装
	 * @return
	 */
	override fun getEffect(): Int {
		mLock.lock()
		try {
			return mEffect
		} finally {
			mLock.unlock()
		}
	}

	/**
	 * IEffectの実装
	 * @param params
	 */
	override fun setParams(params: FloatArray) {
		if (DEBUG) Log.v(TAG, "setParams:" + params.contentToString())
		mDrawer.setParams(mEffect, params)
	}

	/**
	 * IEffectの実装
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException
	 */
	@Throws(IllegalArgumentException::class)
	override fun setParams(effect: Int, params: FloatArray) {
		if (DEBUG) Log.v(TAG, "setParams:effect=" + effect + "," + params.contentToString())
		mDrawer.setParams(effect, params)
	}

	companion object {
		private const val DEBUG = false
		private const val TAG = "MediaEffectGLEffect"
	}
}
