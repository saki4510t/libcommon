package com.serenegiant.libcommon
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
*/

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import com.serenegiant.gl.GLManager
import com.serenegiant.gl.GLUtils
import com.serenegiant.glpipeline.GLPipeline
import com.serenegiant.glpipeline.ProxyPipeline
import com.serenegiant.glutils.GLSurfaceReceiver
import com.serenegiant.glutils.GLSurfaceReceiver.DefaultCallback
import com.serenegiant.glutils.IMirror
import com.serenegiant.glutils.IRendererHolder
import com.serenegiant.graphics.BitmapHelper
import com.serenegiant.graphics.MatrixUtils
import com.serenegiant.utils.ThreadPool
import com.serenegiant.utils.ThreadUtils
import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

object TestUtils {
	private val TAG: String = TestUtils::class.java.simpleName

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @param dumpOnError 一致しないピクセルが見つかったときにlogcatへビットマップデータを出力するかどうか
	 * @param checkAll 一致しないピクセルが見つかった場合でも全てのピクセルを確認するかどうか
	 * @return
	 */
	@JvmStatic
	@JvmOverloads
	fun bitmapEquals(
		a: Bitmap, b: Bitmap,
		dumpOnError: Boolean = false,
		checkAll: Boolean = false
	): Boolean {
		var result = false
		var errCnt = 0
		val width = a.width
		val height = a.height
		if ((width == b.width) && (height == b.height
			&& (a.config == b.config))) {
			result = true
LOOP@		for (y in 0..< height) {
				for (x in 0..< width) {
					if (a.getPixel(x, y) != b.getPixel(x, y)) {
						Log.w(TAG, "ピクセルが違う@($x,$y)sz(${width}x$height),a=0x${a.getPixel(x, y).toString(16).padStart(8, '0')}%08x,b=0x${b.getPixel(x, y).toString(16).padStart(8, '0')}")
						errCnt++
						result = false
						if (!checkAll) {
							break@LOOP
						}
					}
				}
			}
			if (!result && checkAll) {
				Log.i(TAG, "errCnt=$errCnt/${(width * height)}")
			}
			if (!result && dumpOnError) {
				dump(TAG, "a=", a)
				dump(TAG, "b=", b)
			}
		} else {
			Log.w(TAG, "サイズが違うa(${width}x$height),b(${a.width}x${b.height})")
		}
		return result
	}

	/**
	 * ビットマップが指定した色で塗りつぶされているかどうかを確認する
	 * @param bitmap
	 * @param color
	 * @param dumpOnError 一致しないピクセルが見つかったときにlogcatへビットマップデータを出力するかどうか
	 * @param checkAll 一致しないピクセルが見つかった場合でも全てのピクセルを確認するかどうか
	 * @return
	 */
	@JvmStatic
	fun bitmapFilledColor(
		bitmap: Bitmap, color: Int,
		dumpOnError: Boolean,
		checkAll: Boolean
	): Boolean {
		var errCnt = 0
		val width = bitmap.width
		val height = bitmap.height
		var result = true
LOOP@	for (y in 0..< height) {
			for (x in 0..< width) {
				if (bitmap.getPixel(x, y) != color) {
					Log.w(TAG, "ピクセルが違う@($x,$y)sz(${width}x$height),a=0x${bitmap.getPixel(x, y).toString(16).padStart(8, '0')}%08x,b=0x${color.toString(16).padStart(8, '0')}")
					errCnt++
					result = false
					if (!checkAll) {
						break@LOOP
					}
				}
			}
		}
		if (!result && checkAll) {
			Log.i(TAG, "errCnt=$errCnt/${width * height}")
		}
		if (!result && dumpOnError) {
			dump(TAG, "bitmap=", bitmap)
		}
//		Log.i(TAG, String.format("ARGB=%08x/%08x/%08x/%08x",
//			((color & 0xff000000) >>> 24),	// A
//			((color & 0x00ff0000) >>> 16),	// R
//			((color & 0x0000ff00) >>>  8),	// G
//			((color & 0x000000ff))			// B
//		));
		return result
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param bitmap
	 */
	@JvmStatic
	fun dump(bitmap: Bitmap) {
		dump(TAG, null, bitmap)
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param tag
	 * @param bitmap
	 */
	@JvmStatic
	fun dump(tag: String?, prefix: String?, bitmap: Bitmap) {
		val sb = StringBuilder()
		val w = bitmap.width
		val h = bitmap.height
		val t = (if (TextUtils.isEmpty(tag)) TAG else tag!!)
		val header = (if (TextUtils.isEmpty(prefix)) "dump:" else prefix!!)
		Log.i(t, "$header(${w}x$h)")
		for (y in 0..< h) {
			for (x in 0..< w) {
				val cl = bitmap.getPixel(x, y)
				if (cl != 0) {
					sb.append(String.format("%08x", cl))
				}
			}
		}
		Log.i(t, header + sb)
	}

	/**
	 * ビットマップを上下反転
	 * @param bitmap
	 * @return
	 */
	@JvmStatic
	fun flipVertical(bitmap: Bitmap): Bitmap {
		return BitmapHelper.applyMirror(bitmap, IMirror.MIRROR_VERTICAL)
	}

	/**
	 * 非同期で指定したSurfaceへCanvasを使って指定した枚数指定したBitmapを書き込む
	 * @param bitmap
	 * @param surface
	 * @param numImages
	 * @param requestStop
	 */
	@JvmStatic
	fun inputImagesAsync(
		bitmap: Bitmap,
		surface: Surface,
		numImages: Int,
		requestStop: AtomicBoolean
	) {
		ThreadPool.queueEvent {
			Log.v(TAG, "inputImagesAsync:start,$bitmap")
			val inOutDirty = Rect()
			var cnt = 0
			for (i in 0..< numImages) {
				if (!requestStop.get() && surface.isValid) {
					val canvas = surface.lockCanvas(inOutDirty)
					try {
						if (canvas != null) {
							cnt++
							try {
								canvas.drawBitmap(bitmap, 0f, 0f, null)
							} finally {
								surface.unlockCanvasAndPost(canvas)
							}
						}
					} catch (e: Exception) {
						break
					}
					ThreadUtils.NoThrowSleep(30L)
				} else {
					break
				}
			}
			Log.v(TAG, "inputImagesAsync:finished,$cnt/$numImages")
		}
	}

	/**
	 * 一定時間毎に指定回数IRendererHolder#clearSurfaceAllを非同期で呼び出す
	 * @param rendererHolder
	 * @param color
	 * @param numImages
	 * @param requestStop
	 */
	@JvmStatic
	fun clearRendererAsync(
		rendererHolder: IRendererHolder,
		color: Int,
		numImages: Int,
		requestStop: AtomicBoolean
	) {
		ThreadPool.queueEvent {
			Log.v(TAG, "clearRendererAsync:start")
			val cnt = 0
			for (i in 0..< numImages) {
				if (!requestStop.get() && rendererHolder.isRunning) {
					try {
						rendererHolder.clearSurfaceAll(color)
					} catch (e: Exception) {
						break
					}
					ThreadUtils.NoThrowSleep(30L)
				} else {
					break
				}
			}
			Log.v(TAG, "clearRendererAsync:finished,$cnt/$numImages")
		}
	}

	/**
	 * パイプラインチェーン内の順番を検証する
	 * @param head
	 * @param args
	 * @return
	 */
	@JvmStatic
	fun validatePipelineOrder(head: GLPipeline, vararg args: GLPipeline): Boolean {
		var result = true
		val n = args.size
		var cnt = 0
		var p: GLPipeline? = GLPipeline.findFirst(head)
		for (i in 0..<n) {
			if (p !== args[i]) {
				Log.w(TAG, "パイプラインチェーン内の順番が違う")
				result = false
				break
			}
			if (++cnt < n) {
				p = p.pipeline
			}
		}
		if (p!!.pipeline != null) {
			Log.w(TAG, "パイプラインチェーン内のパイプラインの数が違う")
			result = false
		}
		return result
	}

	/**
	 * glReadPixelsでフレームバッファを読み取ってビットマップを生成するときに使うバッファを割り当てる
	 * Bitmap.Config.ARGB_8888とする
	 * @param width
	 * @param height
	 * @return
	 */
	@JvmStatic
	fun allocateBuffer(width: Int, height: Int): ByteBuffer {
		val bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888)
		return ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN)
	}

	/**
	 * 映像をテクスチャとして受け取ってBitmapとして読み取るためのGLSurfaceReceiverを生成する
	 * Surface → GLSurfaceReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * @param manager
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @param cnt
	 * @param useOffscreenRendering true: オフスクリーンサーフェースへ描画してからキャプチャする、
	 * false: 受け取ったテクスチャをGLSurface#wrapでバックバッファとしてラップしてキャプチャする
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IllegalArgumentException::class)
	fun createGLSurfaceReceiver(
		manager: GLManager,
		width: Int, height: Int,
		numFrames: Int,
		sem: Semaphore,
		result: AtomicReference<Bitmap?>,
		cnt: AtomicInteger,
		useOffscreenRendering: Boolean = false
	): GLSurfaceReceiver {
		require(numFrames >= 1) { "numFrames is too small, must be more than 0" }
		require(!((width < 2) || (height < 2))) { "width and or height is too small, must be more than or equals 2" }

		val receiver = GLSurfaceReceiver(
			manager,
			width, height,
			DefaultCallback { isGLES3, isOES, w, h, texId, texMatrix ->
				if (cnt.incrementAndGet() == numFrames) {
					manager.makeDefault()
					Log.v(TAG, "createGLSurfaceReceiver:create Bitmap from texture,isOES=$isOES,texMatrix=${MatrixUtils.toGLMatrixString(texMatrix)}")
					if (useOffscreenRendering) {
						result.set(GLUtils.glDrawTextureToBitmap(isGLES3, isOES, w, h, texId, texMatrix, null))
					} else {
						result.set(GLUtils.glCopyTextureToBitmap(isOES, w, h, texId, texMatrix, null))
					}
					sem.release()
				}
			}
		)

		return receiver
	}

	/**
	 * 映像をテクスチャとして受け取ってBitmapを受け取るためのProxyPipelineを生成する
	 * ProxyPipeline -> GLSurfaceReceiver -> GLBitmapImageReader -> Bitmap
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @param cnt
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IllegalArgumentException::class)
	fun createImageReceivePipeline(
		manager: GLManager,
		width: Int, height: Int,
		numFrames: Int,
		sem: Semaphore,
		result: AtomicReference<Bitmap?>,
		cnt: AtomicInteger,
		useOffscreenRendering: Boolean = false
	): GLPipeline {
		require(numFrames >= 1) { "numFrames is too small, must be more than 0" }
		require(!((width < 2) || (height < 2))) { "width and or height is too small, must be more than or equals 2" }
		return object : ProxyPipeline(width, height) {
			override fun onFrameAvailable(
				isGLES3: Boolean, isOES: Boolean,
				width: Int, height: Int,
				texId: Int, texMatrix: FloatArray
			) {
				super.onFrameAvailable(isGLES3, isOES, width, height, texId, texMatrix)
				if (cnt.incrementAndGet() == numFrames) {
					manager.makeDefault()
					Log.v(TAG, "createImageReceivePipeline:create Bitmap from texture,isOES=$isOES,texMatrix=${MatrixUtils.toGLMatrixString(texMatrix)}")
					if (useOffscreenRendering) {
						result.set(GLUtils.glDrawTextureToBitmap(isGLES3, isOES, width, height, texId, texMatrix, null))
					} else {
						result.set(	GLUtils.glCopyTextureToBitmap(isOES, width, height, texId, texMatrix, null))
					}
					sem.release()
				}
			}
		}
	}

	//--------------------------------------------------------------------------------
	// テストに使うダミーのクラス
	//--------------------------------------------------------------------------------
	/*package*/
	internal class ParcelableValue : Parcelable {
		val value: Int

		constructor(value: Int) {
			this.value = value
		}

		constructor(src: Parcel) {
			value = src.readInt()
		}

		override fun describeContents(): Int {
			return 0
		}

		override fun writeToParcel(dst: Parcel, flags: Int) {
			dst.writeInt(value)
		}

		companion object {
			@JvmField
			val CREATOR = object : Parcelable.Creator<ParcelableValue> {
				override fun createFromParcel(src: Parcel): ParcelableValue {
					return ParcelableValue(src)
				}

				override fun newArray(size: Int): Array<ParcelableValue?> {
					return arrayOfNulls(0)
				}
			}
		}
	}

	/*package*/
	internal class BothValue : Parcelable, Serializable {
		val value: Int

		constructor(value: Int) {
			this.value = value
		}

		constructor(src: Parcel) {
			value = src.readInt()
		}

		override fun describeContents(): Int {
			return 0
		}

		override fun writeToParcel(dst: Parcel, flags: Int) {
			dst.writeInt(value)
		}

		companion object {
			@JvmField
			val CREATOR = object : Parcelable.Creator<BothValue> {
				override fun createFromParcel(src: Parcel): BothValue {
					return BothValue(src)
				}

				override fun newArray(size: Int): Array<BothValue?> {
					return arrayOfNulls(0)
				}
			}
		}
	}

	/*package*/
	internal class SerializableValue(val value: Int) : Serializable

	/*package*/
	internal class Value(val value: Int)
}
