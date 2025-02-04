package com.serenegiant.graphics;
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

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.exifinterface.media.ExifInterface;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.glutils.IMirror;
import com.serenegiant.utils.BitsHelper;
import com.serenegiant.utils.UriHelper;

public final class BitmapHelper {
	private static final boolean DEBUG = false;	// 実働時はfalseにすること
	private static final String TAG = BitmapHelper.class.getSimpleName();

	private BitmapHelper() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

//--------------------------------------------------------------------------------
	/**
	 * Bitmapをpng形式のbyte[]に変換して返す
	 * @param bitmap
	 * @return
	 */
	public static byte[] BitmapToByteArray(@NonNull final Bitmap bitmap) {
		return BitmapToByteArray(bitmap, Bitmap.CompressFormat.PNG, 100);
	}

	/**
	 * Bitmapをjpeg/png/webp形式のbyte[]に変換して返す
	 * @param bitmap
	 * @param format Bitmap.CompressFormat.JPEGまたはBitmap.CompressFormat.PNGまたはBitmap.CompressFormat.WEBP
	 * @param quality JPEGのときのみ有効(0,100]
	 * @return
	 */
	public static byte[] BitmapToByteArray(@NonNull final Bitmap bitmap,
		final Bitmap.CompressFormat format, final int quality) {

		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] bytes = null;
        if (bitmap.compress(format, quality, byteArrayOutputStream)) {
            bytes = byteArrayOutputStream.toByteArray();
        }
        return bytes;
	}

//--------------------------------------------------------------------------------
// byte配列を使うとき

	/**
	 * byte[]をBitmapに変換して返す
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(@Nullable final byte[] bytes) {
		Bitmap bitmap = null;
		if (bytes != null) {
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		}
		return bitmap;
	}

	/**
	 * byte[]を指定した幅・高さに最も近い大きさのBitmapに変換して返す
	 * @param bytes
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(
		@Nullable final byte[] bytes,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (bytes != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
		}
		return bitmap;
	}

	/**
	 * byte[]を指定した幅・高さのBitmapに変換して返す
	 * @param bytes
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmapStrictSize(
		@Nullable final byte[] bytes,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (bytes != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcSampleSize);
			options.inSampleSize = inSampleSize;
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			if ((inSampleSize != calcSampleSize)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleBitmap(bitmap, requestWidth, requestHeight);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

//--------------------------------------------------------------------------------
// ファイルパス文字列を使うとき
	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param filePath
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(final String filePath) {
		Bitmap bitmap= null;
		if (!TextUtils.isEmpty(filePath)) {
			bitmap = BitmapFactory.decodeFile(filePath);
			final int orientation = getOrientation(filePath);
			if (orientation != 0) {
				final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param filePath
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(
		final String filePath,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (!TextUtils.isEmpty(filePath)) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFile(filePath, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeFile(filePath, options);
			final int orientation = getOrientation(filePath);
			if (orientation != 0) {
				final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param filePath
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmapStrictSize(
		final String filePath,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (!TextUtils.isEmpty(filePath)) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFile(filePath, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
			options.inSampleSize = inSampleSize;
//			options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeFile(filePath, options);
			final int orientation = getOrientation(filePath);
			if ((inSampleSize != calcedSampleSize)
				|| (orientation != 0)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleRotateBitmap(bitmap, requestWidth, requestHeight, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

//--------------------------------------------------------------------------------
// FileDescriptorを使う場合

	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param fd
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(final FileDescriptor fd) {
		Bitmap bitmap= null;
		if (fd != null && fd.valid()) {
			bitmap = BitmapFactory.decodeFileDescriptor(fd);
			final int orientation = getOrientation(fd);
			if (orientation != 0) {
				final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param fd
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap asBitmap(
		final FileDescriptor fd,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (fd != null && fd.valid()) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
			final int orientation = getOrientation(fd);
			if (orientation != 0) {
				final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param fd
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmapStrictSize(
		final FileDescriptor fd,
		final int requestWidth, final int requestHeight) {

		Bitmap bitmap = null;
		if (fd != null && fd.valid()) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcSampleSize);
			options.inSampleSize = inSampleSize;
//			options.inMutable = (inSampleSize != calcSampleSize);	// サイズ変更する時はmutableにする
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
			final int orientation = getOrientation(fd);
			if ((inSampleSize != calcSampleSize)
				|| (orientation != 0)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleRotateBitmap(bitmap, requestWidth, requestHeight, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

//--------------------------------------------------------------------------------
// ContentResolverを使うとき

	/**
	 * 指定したIDの静止画をContentResolverから読み込む
	 * @param cr
	 * @param id
	 * @return
	 * @throws IOException
	 */
	@Nullable
	public static Bitmap asBitmap(@NonNull final ContentResolver cr,
		final long id) throws IOException {

		Bitmap result = null;
		final Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
		final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
		if (pfd != null) {
			result = asBitmap(pfd.getFileDescriptor());
		}
		return result;
	}

	/**
	 * 指定したIDの静止画をContentResolverから読み込む
	 * @param cr
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws IOException
	 */
	@Nullable
	public static Bitmap asBitmap(@NonNull final ContentResolver cr,
		final long id,
		final int requestWidth, final int requestHeight)
			throws IOException {

		Bitmap result = null;
		final Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
		final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
		if (pfd != null) {
			result = asBitmap(pfd.getFileDescriptor(), requestWidth, requestHeight);
		}
		return result;
	}


	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param cr
	 * @return
	 * @throws FileNotFoundException
	 */
	@Nullable
	public static Bitmap asBitmap(@NonNull final ContentResolver cr, final Uri uri)
		throws IOException {

		Bitmap bitmap= null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				bitmap = asBitmap(pfd.getFileDescriptor());
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param cr
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	@Nullable
	public static Bitmap asBitmap(@NonNull final ContentResolver cr,
		final Uri uri,
		final int requestWidth, final int requestHeight) throws IOException {

		Bitmap bitmap = null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				bitmap = asBitmap(pfd.getFileDescriptor(), requestWidth, requestHeight);
			}
		}
//		if (DEBUG) Log.v(TAG, "asBitmap:" + bitmap);
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param cr
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	@Nullable
	public static Bitmap asBitmapStrictSize(@NonNull final ContentResolver cr,
		final Uri uri,
		final int requestWidth, final int requestHeight) throws IOException {

		Bitmap bitmap = null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				bitmap = asBitmapStrictSize(pfd.getFileDescriptor(), requestWidth, requestHeight);
			}
		}
		return bitmap;
	}

//--------------------------------------------------------------------------------
	/**
	 * DrawableをBitmapへ変換する
	 * @param drawable
	 * @return
	 * @throws IllegalArgumentException
	 */
	@NonNull
	public static Bitmap fromDrawable(
		@NonNull final Drawable drawable,
		final int width, final int height) {

		Bitmap result;
		Drawable _drawable = drawable;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			_drawable = (DrawableCompat.wrap(_drawable)).mutate();
		}
		_drawable.setBounds(0, 0, width, height);
		result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(result);
		_drawable.draw(canvas);
		return result;
	}

	/**
	 * 指定したDrawableリソースからビットマップとして画像を取得する
	 * @param drawableRes
	 * @return
	 */
	@NonNull
	public static Bitmap fromDrawable(
		@NonNull final Context context,
		@DrawableRes final int drawableRes) {

		Bitmap result = null;
		if (drawableRes != 0) {
			final Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
			if ((drawable.getIntrinsicWidth() <= 0) || (drawable.getIntrinsicHeight() <= 0)) {
				// ColorDrawable等でサイズ指定されていないときは
				// intrinsicWidth/intrinsicHeightが0なのでエラーにならないように
				// 適当な値を入れておく
				drawable.setBounds(0, 0, 72, 72);
			}
			result = fromDrawable(drawable,
				drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		}
		if (result == null) {
			throw new IllegalArgumentException("failed to load from resource " + drawableRes);
		}
		return result;
	}

//--------------------------------------------------------------------------------
// Bitmapの操作メソッド

	/**
	 * 指定したサイズになるように拡大縮小したBitmapを生成して返す(Bitmap#createScaledBitmapと一緒かな?)
	 * @param bitmap
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Nullable
	public static Bitmap scaleBitmap(
		@Nullable final Bitmap bitmap,
		final int requestWidth, final int requestHeight) {

		if (DEBUG) Log.v(TAG, String.format("scaleBitmap:(%dx%d)",
			requestWidth, requestHeight));
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postScale(width / (float)requestWidth, height / (float)requestHeight);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
		if (DEBUG) Log.v(TAG, "scaleBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

	/**
	 * 指定した角度回転させたBitmapを生成して返す
	 * @param bitmap
	 * @param rotation
	 * @return
	 */
	@Nullable
	public static Bitmap rotateBitmap(
		@Nullable final Bitmap bitmap, final int rotation) {

		if (DEBUG) Log.v(TAG, String.format("rotateBitmap:%d", rotation));
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
		if (DEBUG) Log.v(TAG, "rotateBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

	/**
	 * 指定した大きさにした後指定した角度回転させたBitmapを生成して返す
	 * @param bitmap
	 * @param requestWidth
	 * @param requestHeight
	 * @param rotation
	 * @return
	 */
	@Nullable
	public static Bitmap scaleRotateBitmap(
		@Nullable final Bitmap bitmap,
		final int requestWidth, final int requestHeight, final int rotation) {

		if (DEBUG) Log.v(TAG, String.format("scaleRotateBitmap:(%dx%d)/%d",
			requestWidth, requestHeight, rotation));
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postScale(width / (float)requestWidth, height / (float)requestHeight);
			matrix.postRotate(rotation);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
		if (DEBUG) Log.v(TAG, "scaleBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

    /* Options used internally. */
    private static final int OPTIONS_SCALE_UP = 0x1;
    public static final int OPTIONS_RECYCLE_INPUT = 0x2;
	/**
	 * 縦横の短い方に合わせて中央を切り出して指定した大きさにしたビットマップを生成して返す
	 * @param source
	 * @param width
	 * @param height
	 * @return
	 */
	@Nullable
	public static Bitmap extractBitmap(
		@Nullable final Bitmap source,
		final int width, final int height) {

		Bitmap newBitmap = null;
		if (source != null) {
	        float scale;
	        if (source.getWidth() < source.getHeight()) {
	            scale = width / (float) source.getWidth();
	        } else {
	            scale = height / (float) source.getHeight();
	        }
	        final Matrix matrix = new Matrix();
	        matrix.setScale(scale, scale);
	        newBitmap = transform(matrix, source, width, height,
	        	OPTIONS_RECYCLE_INPUT | OPTIONS_SCALE_UP);
		}
		return newBitmap;
	}

    /**
     * Transform source Bitmap to targeted width and height.
     */
    @NonNull
    private static Bitmap transform(
    	Matrix scaler,
		@NonNull final Bitmap source,
		final int targetWidth, final int targetHeight,
		final int options) {

        final boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
        final boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

        final int deltaX = source.getWidth() - targetWidth;
        final int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
            * In this case the bitmap is smaller, at least in one dimension,
            * than the target.  Transform it by placing as much of the image
            * as possible into the target and leaving the top/bottom or
            * left/right (or both) black.
            */
            final Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
            		Bitmap.Config.ARGB_8888);
            final Canvas c = new Canvas(b2);

            final int deltaXHalf = Math.max(0, deltaX / 2);
            final int deltaYHalf = Math.max(0, deltaY / 2);
            final Rect src = new Rect(
            	deltaXHalf,
            	deltaYHalf,
            	deltaXHalf + Math.min(targetWidth, source.getWidth()),
            	deltaYHalf + Math.min(targetHeight, source.getHeight()));
            final int dstX = (targetWidth  - src.width())  / 2;
            final int dstY = (targetHeight - src.height()) / 2;
            final Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            if (recycle) {
                source.recycle();
            }
            c.setBitmap(null);
            return b2;
        }
        final float bitmapWidthF = source.getWidth();
        final float bitmapHeightF = source.getHeight();

        final float bitmapAspect = bitmapWidthF / bitmapHeightF;
        final float viewAspect   = (float) targetWidth / targetHeight;

		final float scale;
		if (bitmapAspect > viewAspect) {
			scale = targetHeight / bitmapHeightF;
		} else {
			scale = targetWidth / bitmapWidthF;
		}
		if (scale < .9F || scale > 1F) {
			scaler.setScale(scale, scale);
		} else {
			scaler = null;
		}

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        if (recycle && b1 != source) {
            source.recycle();
        }

        final int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        final int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        final Bitmap b2 = Bitmap.createBitmap(b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

        if (b2 != b1) {
            if (recycle || b1 != source) {
                b1.recycle();
            }
        }

        return b2;
    }

	/**
	 * 指定したサイズよりも小さくならない最大のサブサンプリング数を取得
	 * @param options
	 * @param requestWidth　0以下でrequestHeightが指定されていればアスペクト比を保つ用にwidthを自動設定, requestHeightが設定されていなければ映像幅を使う
	 * @param requestHeight 0以下でrequestWidthが指定されていればアスペクト比を保つ用にheightを自動設定, requestWidthが設定されていなければ映像高さを使う
	 * @return
	 */
	public static int calcSampleSize(@NonNull final BitmapFactory.Options options,
		final int requestWidth, final int requestHeight) {

		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int reqWidth = requestWidth, reqHeight = requestHeight;
		if (requestWidth <= 0) {
			if (requestHeight > 0)
				reqWidth = (int)(imageWidth * requestHeight / (float)imageHeight);
			else
				reqWidth = imageWidth;
		}
		if (requestHeight <= 0) {
			if (requestWidth > 0)
				reqHeight = (int)(imageHeight * requestWidth / (float)imageHeight);
			else
				reqHeight = imageHeight;
		}
		int inSampleSize = 1;
		if ((imageHeight > reqHeight) || (imageWidth > reqWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = Math.round(imageHeight / (float)reqHeight);	// Math.floor
			} else {
				inSampleSize = Math.round(imageWidth / (float)reqWidth);	// Math.floor
			}
		}
		if (DEBUG) Log.v(TAG, String.format("calcSampleSize:(%dx%d)=>(%dx%d)/img(%dx%d),inSampleSize=%d",
			requestWidth, requestHeight, reqWidth, reqHeight, imageWidth, imageHeight, inSampleSize));
		return inSampleSize;
	}

	@NonNull
	public static Bitmap copyBitmap(@NonNull final Bitmap src, Bitmap dest) {
		if (dest == null) {
			dest = Bitmap.createBitmap(src);
		} else if (!src.equals(dest)) {
			final Canvas canvas = new Canvas(dest);
			canvas.drawBitmap(src, 0, 0, new Paint());
		}
		return dest;
	}

	/**
	 * ビットマップにミラー設定を適用
	 * @param bitmap
	 * @param mirror
	 * @return
	 */
	public static Bitmap applyMirror(
		@NonNull final Bitmap bitmap,
		@IMirror.MirrorMode final int mirror) {

		final int _mirror = mirror % IMirror.MIRROR_NUM;
		if (_mirror == IMirror.MIRROR_NORMAL) {
			return bitmap;
		} else {
			final Matrix m = new Matrix();
			switch (_mirror) {
			case IMirror.MIRROR_HORIZONTAL -> m.preScale(-1, 1);
			case IMirror.MIRROR_VERTICAL -> m.preScale(1, -1);
			case IMirror.MIRROR_BOTH -> m.preScale(-1, -1);
			default -> {}
			}
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 透過部分の背景用に白とグレーの市松模様の単位パターンのRGB565のビットマップを生成する
	 * 全体が40x40で20x20の四角を白とグレーで塗りつぶす
	 * @return
	 */
	@NonNull
	public static Bitmap makeCheckBitmap() {
		return makeCheckBitmap(Bitmap.Config.RGB_565);
    }

	/**
	 * 透過部分の背景用に白とグレーの市松模様の単位パターンの指定したBitmap.configのビットマップを生成する
	 * 全体が40x40で20x20の四角を白とグレーで塗りつぶす
	 * @param config
	 * @return
	 */
	@NonNull
	public static Bitmap makeCheckBitmap(@NonNull final Bitmap.Config config) {
		final Bitmap bm = Bitmap.createBitmap(40, 40, config);
		final Canvas c = new Canvas(bm);
		c.drawColor(Color.WHITE);
		final Paint p = new Paint();
		p.setColor(Color.LTGRAY);
		c.drawRect(0, 0, 20, 20, p);
		c.drawRect(20, 20, 40, 40, p);
		return bm;
	}

	/**
	 * 透過部分の背景用に白とグレーの市松模様の単位パターンのビットマップを生成
	 * @param width  生成するビットマップの幅
	 * @param height 生成するビットマップの宝
	 * @param step_width 単位パターンの幅
	 * @param step_height 単位パターンの高さ
	 * @param config 生成するビットマップのタイプ
	 * @return
	 */
	@NonNull
	public static Bitmap makeCheckBitmap(
		final int width, final int height,
		final int step_width, final int step_height,
		@NonNull final Bitmap.Config config) {

        final Bitmap bm = Bitmap.createBitmap(width, height, config);
        final Canvas c = new Canvas(bm);
        c.drawColor(Color.WHITE);
        final Paint p = new Paint();
        p.setColor(Color.LTGRAY);
        for (int x = 0; x < width; x += step_width * 2) {
        	for (int y = 0; y < height; y += step_height * 2) {
				c.drawRect(x, y, step_width, step_height, p);
				c.drawRect(x + step_width, y + step_height, step_width, step_height, p);
			}
		}
        return bm;
    }

	/**
	 * 単色で塗りつぶしたBitmapを生成
	 * @param width
	 * @param height
	 * @param color
	 * @param config
	 * @return
	 */
	@NonNull
	public static Bitmap makeFilledBitmap(
		final int width, final int height,
		final int color,
		@NonNull final Bitmap.Config config) {

		final Bitmap bm = Bitmap.createBitmap(width, height, config);
		bm.eraseColor(color);

		return bm;
	}

	/**
	 * 指定した色でグラデーションしたビットマップを生成
	 * (0, 0)がcolor1、(width,height)がcolor2
	 * @param width
	 * @param height
	 * @param color1
	 * @param color2
	 * @param config
	 * @return
	 */
	@NonNull
	public static Bitmap makeGradientBitmap(
		final int width, final int height,
		final int color1, final int color2,
		@NonNull final Bitmap.Config config) {

		return makeGradientBitmap(
			width, height,
			color1, new Point(0, 0),
			color2, new Point(width, height),
			config);
	}

	/**
	 * 指定した色でグラデーションしたビットマップを生成
	 * p1がcolor1、p2がcolor2
	 * @param width
	 * @param height
	 * @param color1
	 * @param p1
	 * @param color2
	 * @param p2
	 * @param config
	 * @return
	 */
	@NonNull
	public static Bitmap makeGradientBitmap(
		final int width, final int height,
		final int color1, final Point p1,
		final int color2, final Point p2,
		@NonNull final Bitmap.Config config) {

		final Bitmap bm = Bitmap.createBitmap(width, height, config);
		final Canvas c = new Canvas(bm);
		c.drawColor(color1);
		final LinearGradient grad = new LinearGradient(p1.x, p1.y, p2.x, p2.y, color1, color2, Shader.TileMode.CLAMP);
		final Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setShader(grad);
		c.drawRect(0, 0, width, height, paint);

		return bm;
	}

	/**
	 * マスク用Bitmapを生成
	 * @return
	 */
	@Nullable
	public static Bitmap genMaskImage(
		@IntRange(from = 0, to = 0)final int type,
		final int width, final int height,
		@IntRange(from = 0, to = 100) final int size,
		final int color,
		@IntRange(from = 0, to = 255) final int alpha0,
		@IntRange(from = 0, to = 255) final int alphaMax) {

		if (DEBUG) Log.v(TAG, String.format("genMaskImage:(%dx%d)", width, height));

		return getMaskImage0(width, height, size, color, alpha0, alphaMax);
	}

	/**
	 * 円形マスク用Bitmap生成処理
	 * @param width
	 * @param height
	 * @param size
	 * @param color
	 * @param alpha0
	 * @param alphaMax
	 * @return
	 */
	@Nullable
	public static Bitmap getMaskImage0(
		final int width, final int height,
		@IntRange(from = 0, to = 100) final int size,
		final int color,
		@IntRange(from = 0, to = 255) final int alpha0,
		@IntRange(from = 0, to = 255) final int alphaMax) {

		if (DEBUG) Log.v(TAG, String.format("getMaskImage0:(%dx%d)", width, height));

		// 透過円の半径
		final float r = (Math.min(width, height) * size) / 200.0f;
		final int cx = width / 2;
		final int cy = height / 2;
		// 透過円描画用のオブジェクト
		final Paint paint = new Paint();
		paint.setDither(true);
		final int rr = Color.red(color);
		final int bb = Color.blue(color);
		final int gg = Color.green(color);
		final RadialGradient gradient = new RadialGradient(
			cx, cy, r,
			new int[] {
				Color.argb(alphaMax, rr, gg, bb),
				Color.argb(alphaMax, rr, gg, bb),
				Color.argb((int)(alphaMax * 0.6f), rr, gg, bb),
				Color.argb(alpha0, rr, gg, bb)},
			new float[] {
				0.00f,
				0.60f,
				0.80f,
				1.00f},
			Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		// Bitmapをバックバッファにしてオフスクリーン描画する
		final Bitmap offscreen = Bitmap.createBitmap(width, height,
			Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(offscreen);
//		// 全体のアルファ値, RadialGradientをCLAMPにしているから不要
//		canvas.drawColor(cl0);
		// 透過円
		canvas.drawCircle(cx, cy, r, paint);
		return offscreen;
	}

	/**
	 * アルファ値だけを反転(a'=1-a)させる色変換行列
	 * 元のRGBAの値は[0,255]
	 * RGBA' = RGBA x MAT
	 */
	private static final float[] COLOR_MATRIX_INVERT_ALPHA = {
		1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
		0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 0.0f, -1.0f, 255.0f,
	};

	/**
	 * 指定したBitmapのアルファ値を反転(a'=1-a)させたBitmapを生成する
	 * @param src
	 * @return
	 */
	public static Bitmap invertAlpha(@NonNull final Bitmap src) {
		final Bitmap offscreen = Bitmap.createBitmap(
			src.getWidth(), src.getHeight(), src.getConfig());
		final Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(COLOR_MATRIX_INVERT_ALPHA));
		final Canvas canvas = new Canvas(offscreen);
		canvas.drawBitmap(src, 0, 0, paint);
		return offscreen;
	}
//--------------------------------------------------------------------------------

	/**
	 * 画像の回転角度を取得
	 * @param path
	 * @return 0, 90, 180, 270
	 */
	public static int getOrientation(final String path) {
		final ExifInterface exif;
	    try {
	    	// これはファイルの位置によってはパーミッションが必要かも
	        exif = new ExifInterface(path);
		} catch (final Exception e) {	// IOException/IllegalArgumentException
//        	if (DEBUG) Log.w(TAG, e);
			return 0;
		}
		return getOrientation(exif);
	}

	/**
	 * 画像の回転角度を取得
	 * @param fd
	 * @return 0, 90, 180, 270
	 */
	public static int getOrientation(final FileDescriptor fd) {
		final ExifInterface exif;
	    try {
			// これはファイルの位置によってはパーミッションが必要かも
	        exif = new ExifInterface(fd);
		} catch (final Exception e) {	// IOException/IllegalArgumentException
//        	if (DEBUG) Log.w(TAG, e);
			return 0;
		}
		return getOrientation(exif);
	}

	private static int getOrientation(@NonNull final ExifInterface exif) {
		final int rotation = exif.getAttributeInt(
			ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

		int result = switch (rotation) {
			case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
			case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
			case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
			default -> 0;
		};
		if (DEBUG) Log.v(TAG, "getOrientation:" + result);
		return result;
	}

	/**
	 * 画像の回転角度を取得
	 * @param cr
	 * @param id
	 * @return 0, 90, 180, 270
	 */
	public static int getOrientation(
		@NonNull final ContentResolver cr, final long id) {

		return getOrientation(cr,
			ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
	}

	/**
	 * 画像の回転角度を取得
	 * @param cr
	 * @param imageUri
	 * @return 0, 90, 180, 270
	 */
	public static int getOrientation(
		@NonNull final ContentResolver cr, @NonNull final  Uri imageUri) {

		final ExifInterface exif;
	    try {
			// これはファイルの位置によってはパーミッションが必要かも
	        exif = new ExifInterface(UriHelper.getAbsolutePath(cr, imageUri));
		} catch (final Exception e) {	// IOException/IllegalArgumentException
//        	if (DEBUG) Log.w(TAG, e);
			return getOrientationFromMediaStore(cr, imageUri);
		}
        final int rotation = exif.getAttributeInt(
        	ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        if (rotation == ExifInterface.ORIENTATION_UNDEFINED) {
        	// Exifに値がセットされていないときはMediaStoreからの取得を試みる
            return getOrientationFromMediaStore(cr, imageUri);
		} else {
			return getOrientation(exif);
		}
	}

	/**
	 * MediaStoreから画像の回転角度を取得
	 * @param cr
	 * @param imageUri
	 * @return 0, 90, 180, 270
	 */
	public static int getOrientationFromMediaStore(
		@NonNull final ContentResolver cr, @NonNull final  Uri imageUri) {

	    final String[] columns = {MediaStore.Images.Media.DATA, "orientation"};
	    int result = 0;
	    final Cursor cursor
	    	= cr.query(imageUri, columns, null, null, null);
	    try {
	    	if (cursor != null) {
				cursor.moveToFirst();
				final int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
    			result = cursor.getInt(orientationColumnIndex);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		if (DEBUG) Log.v(TAG, "getOrientationFromMediaStore:" + result);
		return result;
	}

	/**
	 * ビットマップの1ピクセル当たりのバイト数を取得する
	 * @param config
	 * @return
	 */
	public static int getPixelBytes(@NonNull final Bitmap.Config config) {
		return switch (config) {
			case ALPHA_8 -> 1;
			case RGB_565, ARGB_4444 -> 2;
			case ARGB_8888 -> 4;
			case RGBA_F16 -> 8;
			default -> throw new IllegalArgumentException("Unexpected config type" + config);
		};
	}

//--------------------------------------------------------------------------------
// その他
	/**
	 * 飽和計算
	 * @param v
	 * @param min
	 * @param max
	 * @return
	 */
	private static int sat(final int v, final int min, final int max) {
		return v < min ? min : Math.min(v, max);
	}

	/**
	 * 飽和計算
	 * @param v
	 * @param min
	 * @param max
	 * @return
	 */
	private static int sat(final float v, final float min, final float max) {
		return (int)(v < min ? min : Math.min(v, max));
	}

}
