package com.serenegiant.libcommon;
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.ProxyPipeline;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.graphics.BitmapHelper;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.gl.GLConst.GL_TEXTURE_2D;
import static com.serenegiant.gl.GLConst.GL_TEXTURE_EXTERNAL_OES;

public class TestUtils {
	private static final String TAG = TestUtils.class.getSimpleName();

	private TestUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean bitmapEquals(@NonNull final Bitmap a, @NonNull final Bitmap b) {
		return bitmapEquals(a, b, false, false);
	}

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @param dumpOnError 一致しないピクセルが見つかったときにlogcatへビットマップデータを出力するかどうか
	 * @return
	 */
	public static boolean bitmapEquals(
		@NonNull final Bitmap a, @NonNull final Bitmap b,
		final boolean dumpOnError) {
		return bitmapEquals(a, b, dumpOnError, false);
	}

	/**
	 * 指定したビットマップがピクセル単位で一致するかどうかを確認
	 * @param a
	 * @param b
	 * @param dumpOnError 一致しないピクセルが見つかったときにlogcatへビットマップデータを出力するかどうか
	 * @param checkAll 一致しないピクセルが見つかった場合でも全てのピクセルを確認するかどうか
	 * @return
	 */
	public static boolean bitmapEquals(
		@NonNull final Bitmap a, @NonNull final Bitmap b,
		final boolean dumpOnError,
		final boolean checkAll) {

		boolean result = false;
		int errCnt = 0;
		final int width = a.getWidth();
		final int height = a.getHeight();
		if ((width == b.getWidth())
			&& (height == b.getHeight()
			&& (a.getConfig() == b.getConfig()))) {
			result = true;
LOOP:		for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (a.getPixel(x, y) != b.getPixel(x, y)) {
						Log.w(TAG, String.format("ピクセルが違う@(%d,%d)sz(%dx%d),a=0x%08x,b=0x%08x",
							x, y, width, height,
							a.getPixel(x, y), b.getPixel(x, y)));
						errCnt++;
						result = false;
						if (!checkAll) {
							break LOOP;
						}
					}
				}
			}
			if (!result && checkAll) {
				Log.i(TAG, "errCnt=" + errCnt + "/" + (width * height));
			}
			if (!result && dumpOnError) {
				dump(TAG, "a=", a);
				dump(TAG, "b=", b);
			}
		} else {
			Log.w(TAG, String.format("サイズが違うa(%dx%d),b=(%dx%d))",
				width, height, b.getWidth(), b.getHeight()));
		}
		return result;
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param bitmap
	 */
	public static void dump(@NonNull final Bitmap bitmap) {
		dump(TAG, null, bitmap);
	}

	/**
	 * 指定したビットマップのピクセルのうち0以外を16進文字列としてlogCatへ出力する
	 * @param tag
	 * @param bitmap
	 */
	public static void dump(@Nullable final String tag, @Nullable final String prefix, @NonNull final Bitmap bitmap) {
		final StringBuilder sb = new StringBuilder();
		final int w = bitmap.getWidth();
		final int h = bitmap.getHeight();
		final String t = (TextUtils.isEmpty(tag) ? TAG : tag);
		final String header = (TextUtils.isEmpty(prefix) ? "dump:" : prefix);
		Log.i(t, String.format("%s(%dx%d)", header, w, h));
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				final int cl = bitmap.getPixel(x, y);
				if (cl != 0) {
					sb.append(String.format("%08x", cl));
				}
			}
		}
		Log.i(t, header + sb);
	}

	/**
	 * ビットマップを上下反転
	 * @param bitmap
	 * @return
	 */
	public static Bitmap flipVertical(@NonNull final Bitmap bitmap) {
		return BitmapHelper.applyMirror(bitmap, IMirror.MIRROR_VERTICAL);
	}

	/**
	 * 非同期で指定したSurfaceへCanvasを使って指定した枚数指定したBitmapを書き込む
	 * @param bitmap
	 * @param surface
	 * @param num_images
	 */
	public static void inputImagesAsync(@NonNull final Bitmap bitmap, @NonNull final Surface surface, final int num_images) {
		new Thread(() -> {
			final Rect inOutDirty = new Rect();
			for (int i = 0; i < num_images; i++) {
				if (surface.isValid()) {
					final Canvas canvas = surface.lockCanvas(inOutDirty);
					try {
						if (canvas != null) {
							try {
								canvas.drawBitmap(bitmap, 0, 0, null);
								Thread.sleep(30);
							} finally {
								surface.unlockCanvasAndPost(canvas);
							}
						}
					} catch (Exception e) {
						break;
					}
				} else {
					break;
				}
			}
		}).start();
	}

	/**
	 * パイプラインチェーン内の順番を検証する
	 * @param head
	 * @param args
	 * @return
	 */
	public static boolean validatePipelineOrder(@NonNull final GLPipeline head, @NonNull GLPipeline... args) {
		boolean result = true;
		final int n = args.length;
		int cnt = 0;
		GLPipeline p = GLPipeline.findFirst(head);
		for (int i = 0; i < n; i++) {
			if (p != args[i]) {
				Log.w(TAG, "パイプラインチェーン内の順番が違う");
				result = false;
				break;
			}
			if (++cnt < n) {
				p = p.getPipeline();
			}
		}
		if (p.getPipeline() != null) {
			Log.w(TAG, "パイプラインチェーン内のパイプラインの数が違う");
			result = false;
		}
		return result;
	}

	/**
	 * glReadPixelsでフレームバッファを読み取ってビットマップを生成するときに使うバッファを割り当てる
	 * Bitmap.Config.ARGB_8888とする
	 * @param width
	 * @param height
	 * @return
	 */
	public static ByteBuffer allocateBuffer(final int width, final int height) {
		final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
		return ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * GLImageReceiverで映像をテクスチャとして受け取ってGLBitmapImageReaderで
	 * Bitmapを受け取るためのSurfaceを生成する
	 * Surface → GLImageReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * @param manager
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	public static Surface createGLImageReceiverSurface(
		@NonNull final GLManager manager,
		final int width, final int height,
		final int numFrames,
		@NonNull final Semaphore sem,
		@NonNull final AtomicReference<Bitmap> result) throws IllegalArgumentException {
		return createGLImageReceiverSurface(manager, width, height, numFrames, sem, result, new AtomicInteger());
	}

	/**
	 * GLImageReceiverで映像をテクスチャとして受け取ってGLBitmapImageReaderで
	 * Bitmapを受け取るためのSurfaceを生成する
	 * Surface → GLImageReceiver → GLSurface.wrap → glReadPixels → Bitmap
	 * @param manager
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @param cnt
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	public static Surface createGLImageReceiverSurface(
		@NonNull final GLManager manager,
		final int width, final int height,
		final int numFrames,
		@NonNull final Semaphore sem,
		@NonNull final AtomicReference<Bitmap> result,
		@NonNull final AtomicInteger cnt) throws IllegalArgumentException {

		if (numFrames < 1) {
			throw new IllegalArgumentException("numFrames is too small, must be more than 0");
		}
		if ((width < 2) || (height < 2)) {
			throw new IllegalArgumentException("width and or height is too small, must be more than or equals 2");
		}
		final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
		final ByteBuffer buffer = allocateBuffer(width, height);
		final GLSurfaceReceiver receiver = new GLSurfaceReceiver(
			manager,
			width, height,
			new GLSurfaceReceiver.Callback() {
				@Override
				public void onInitialize() {
				}

				@Override
				public void onRelease() {
					if (sem.availablePermits() == 0) {
						sem.release();
					}
				}

				@Override
				public void onCreateInputSurface(@NonNull final Surface surface, final int width, final int height) {
				}

				@Override
				public void onReleaseInputSurface(@NonNull final Surface surface) {
				}

				@Override
				public void onResize(final int width, final int height) {
				}

				@Override
				public void onFrameAvailable(
					final boolean isGLES3, final boolean isOES,
					final int width, final int height,
					final int texId, @NonNull final float[] texMatrix) {

					if (cnt.incrementAndGet() == numFrames) {
						// GLSurfaceを経由してテクスチャを読み取る
						// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
						final GLSurface surface = GLSurface.wrap(isGLES3,
							isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
							GLES20.GL_TEXTURE4, texId, width, height, false);
						surface.makeCurrent();
						final ByteBuffer buf = GLUtils.glReadPixels(buffer, width, height);
						final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
						bitmap.copyPixelsFromBuffer(buf);
						result.set(bitmap);
						sem.release();
					}
				}
			}
		);

		return receiver.getSurface();
	}

	/**
	 * 映像をテクスチャとして受け取ってBitmapを受け取るためのProxyPipelineを生成する
	 * ProxyPipeline -> GLImageReceiver -> GLBitmapImageReader -> Bitmap
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	public static GLPipeline createImageReceivePipeline(
		final int width, final int height,
		final int numFrames,
		@NonNull final Semaphore sem,
		@NonNull final AtomicReference<Bitmap> result) throws IllegalArgumentException {
		return createImageReceivePipeline(width, height, numFrames, sem, result, new AtomicInteger());
	}

	/**
	 * 映像をテクスチャとして受け取ってBitmapを受け取るためのProxyPipelineを生成する
	 * ProxyPipeline -> GLImageReceiver -> GLBitmapImageReader -> Bitmap
	 * @param width
	 * @param height
	 * @param numFrames
	 * @param sem
	 * @param result
	 * @param cnt
	 * @throws IllegalArgumentException numFramesが1未満・width/heightが2未満
	 * @return
	 */
	public static GLPipeline createImageReceivePipeline(
		final int width, final int height,
		final int numFrames,
		@NonNull final Semaphore sem,
		@NonNull final AtomicReference<Bitmap> result,
		@NonNull final AtomicInteger cnt) throws IllegalArgumentException {

		if (numFrames < 1) {
			throw new IllegalArgumentException("numFrames is too small, must be more than 0");
		}
		if ((width < 2) || (height < 2)) {
			throw new IllegalArgumentException("width and or height is too small, must be more than or equals 2");
		}
		final int bytes = width * height * BitmapHelper.getPixelBytes(Bitmap.Config.ARGB_8888);
		final ByteBuffer buffer = allocateBuffer(width, height);
		return new ProxyPipeline(width, height) {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3,
				final boolean isOES, final int texId,
				@NonNull final float[] texMatrix) {
				super.onFrameAvailable(isGLES3, isOES, texId, texMatrix);
				if (cnt.incrementAndGet() == numFrames) {
					// GLSurfaceを経由してテクスチャを読み取る
					// ここに来るのはDrawerPipelineからのテクスチャなのでisOES=falseのはず
					final GLSurface surface = GLSurface.wrap(isGLES3,
						isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D,
						GLES20.GL_TEXTURE4, texId, width, height, false);
					surface.makeCurrent();
					final ByteBuffer buf = GLUtils.glReadPixels(buffer, width, height);
					final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
					bitmap.copyPixelsFromBuffer(buf);
					result.set(bitmap);
					sem.release();
				}
			}
		};
	}

//--------------------------------------------------------------------------------
// テストに使うダミーのクラス
//--------------------------------------------------------------------------------

	/*package*/static class ParcelableValue implements Parcelable {
		public final int value;

		ParcelableValue(final int value) {
			this.value = value;
		}

		ParcelableValue(@NonNull final Parcel src) {
			value = src.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeInt(value);
		}

		public static final Parcelable.Creator<?> CREATOR
			= new Parcelable.Creator<ParcelableValue>() {
			@Override
			public ParcelableValue createFromParcel(@NonNull final Parcel src) {
				return new ParcelableValue(src);
			}

			@Override
			public ParcelableValue[] newArray(final int size) {
				return new ParcelableValue[0];
			}
		};
	}

	/*package*/static class BothValue implements Parcelable, Serializable {
		public final int value;

		BothValue(final int value) {
			this.value = value;
		}

		BothValue(@NonNull final Parcel src) {
			value = src.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeInt(value);
		}

		public static final Parcelable.Creator<?> CREATOR
			= new Parcelable.Creator<BothValue>() {
			@Override
			public BothValue createFromParcel(@NonNull final Parcel src) {
				return new BothValue(src);
			}

			@Override
			public BothValue[] newArray(final int size) {
				return new BothValue[0];
			}
		};
	}

	/*package*/record SerializableValue(int value) implements Serializable {
	}

	/*package*/record Value(int value) {
	}
}
