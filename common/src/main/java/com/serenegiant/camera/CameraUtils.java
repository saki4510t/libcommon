package com.serenegiant.camera;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.serenegiant.glutils.WrappedSurfaceHolder;
import com.serenegiant.view.ViewUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Camera API用のヘルパークラス
 */
public class CameraUtils implements CameraConst {
	private static final boolean DEBUG = false;
	private static final String TAG = CameraUtils.class.getSimpleName();

	@TargetApi(Build.VERSION_CODES.N)
	@IntDef({
		ImageFormat.DEPTH16,
		ImageFormat.DEPTH_POINT_CLOUD,
		ImageFormat.FLEX_RGBA_8888,
		ImageFormat.FLEX_RGB_888,
		ImageFormat.JPEG,
		ImageFormat.NV16,
		ImageFormat.NV21,
		ImageFormat.PRIVATE,
		ImageFormat.RAW10,
		ImageFormat.RAW12,
		ImageFormat.RAW_PRIVATE,
		ImageFormat.RAW_SENSOR,
		ImageFormat.RGB_565,
		ImageFormat.UNKNOWN,
		ImageFormat.YUV_420_888,
		ImageFormat.YUV_422_888,
		ImageFormat.YUV_444_888,
		ImageFormat.YUY2,
		ImageFormat.YV12,})
	@Retention(RetentionPolicy.SOURCE)
	public @interface PreviewFormat {}

	/**
	 * カメラを初期化する
	 *
	 * @param context
	 * @param face
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static Camera setupCamera(@NonNull final Context context,
		 final int face, final int width, final int height) throws IOException {

		final int cameraId = CameraUtils.findCamera(CameraConst.FACING_BACK);
		final Camera camera = Camera.open(cameraId);
		final Camera.Parameters params = camera.getParameters();
		if (params != null) {
			final List<String> focusModes = params.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				if (DEBUG) Log.i(TAG, "handleStartPreview:FOCUS_MODE_CONTINUOUS_VIDEO");
			} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				if (DEBUG) Log.i(TAG, "handleStartPreview:FOCUS_MODE_AUTO");
			} else {
				if (DEBUG) Log.i(TAG, "handleStartPreview:Camera does not support autofocus");
			}
			params.setRecordingHint(true);
			CameraUtils.chooseVideoSize(params, width, height);
			final int[] fps = CameraUtils.chooseFps(params, 1.0f, 120.0f);
			CameraUtils.setupRotation(context, cameraId, camera, params);
			camera.setParameters(params);
			// get the actual preview size
			final Camera.Size previewSize = camera.getParameters().getPreviewSize();
			Log.d(TAG, String.format("handleStartPreview(%d, %d),fps(%d-%d)",
				previewSize.width, previewSize.height, fps[0], fps[1]));
		}
		return camera;
	}

	/**
	 * 指定したfaceに対応するカメラIDを取得する
	 * ただし指定したfaceが無い場合などには違うfaceのカメラが選択されるかもしれない
	 * @param face
	 * @return
	 */
	@SuppressWarnings("WrongConstant")
	@CameraConst.FaceType
	public static final int findCamera(final int face) {
		final Camera.CameraInfo info = new Camera.CameraInfo();
		final int numCameras = Camera.getNumberOfCameras();
		@CameraConst.FaceType
		int targetFace = face;
		boolean triedAllCameras = false;
		int result = FACING_UNSPECIFIED;
cameraLoop:
		for (; !triedAllCameras ;) {
			for (int i = 0; i < numCameras; i++) {
				Camera.getCameraInfo(i, info);
				if (info.facing == targetFace) {
					result = i;
					break cameraLoop;
				}
			}
			if (result < 0) {
				if (targetFace == face) {
					targetFace = (
						face == FACING_BACK ? FACING_FRONT : FACING_BACK);
				} else {
					triedAllCameras = true;
				}
			}
		}
		return result;
	}

	/**
	 * 指定したサイズと同じか近い解像度(±5%以内)を選択する
	 * もし近い解像度が無ければカメラのデフォルト解像度を選択する
	 * @param params
	 * @param width
	 * @param height
	 * @return 見つかった解像度
	 * @throws IllegalArgumentException 解像度を選択できなかった(起こらないはず)
	 */
	@NonNull
	public static Camera.Size chooseVideoSize(@NonNull final Camera.Parameters params,
		 final int width, final int height) throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, String.format("chooseVideoSize:request(%dx%dx)", width, height));
		// カメラの標準の解像度を取得する
		final Camera.Size ppsfv = params.getPreferredPreviewSizeForVideo();
		if (DEBUG) {
			if (ppsfv != null) {
				Log.d(TAG, String.format("Camera preferred preview size for video is %dx%d",
						ppsfv.width, ppsfv.height));
			}
			for (final Camera.Size size : params.getSupportedPreviewSizes()) {
				Log.v(TAG, String.format("getSupportedPreviewSizes:%dx%d", size.width, size.height));
			}
		}
		// もし指定したサイズがサポートされている解像度と一致すればそれを選択する
		for (final Camera.Size size : params.getSupportedPreviewSizes()) {
			if (((width <= 0) || (height <= 0))
				|| ((size.width == width) && (size.height == height))) {
				Log.d(TAG, String.format("match supported preview size:%dx%d", size.width, size.height));
				params.setPreviewSize(size.width, size.height);
				params.setPictureSize(size.width, size.height);
				return size;
			}
		}

		// 一致するのがなかったので近い解像度を選ぶ
		Camera.Size selectedSize = getClosestSupportedSize(
			params.getSupportedPreviewSizes(), width, height);

		// それでも見つからなければカメラの標準解像度を使用する
		if (ppsfv != null) {
			Log.d(TAG, String.format("use ppsfv: %dx%d", ppsfv.width, ppsfv.height));
			params.setPreviewSize(ppsfv.width, ppsfv.height);
			params.setPictureSize(ppsfv.width, ppsfv.height);
			return ppsfv;
		}
		throw new IllegalArgumentException(
			String.format("Unable to set preview size to %dx%d)", width, height));
	}

	/**
	 * 指定した解像度に近い解像度を選ぶ
	 * @param supportedSizes
	 * @param requestWidth
	 * @param requestHeight
	 * @return 選択した解像度
	 */
	@Nullable
	public static Camera.Size getClosestSupportedSize(
		@NonNull final List<Camera.Size> supportedSizes,
		final int requestWidth, final int requestHeight) {

		final double aspect = requestWidth / (double)requestHeight;
		double a, r, selectedDelta = Double.MAX_VALUE;
		Camera.Size selectedSize = null;
		// 幅が一致してアスペクト比が指定したサイズに近いものを探す
		for (final Camera.Size size : supportedSizes) {
			if (size.width == requestWidth) {
				a = size.width / (double)size.height;
				r = Math.abs(a - aspect) / aspect;
				if (r < selectedDelta) {
					selectedSize = size;
					selectedDelta = r;
				}
			}
		}
		if ((selectedSize == null) || (selectedDelta < 0.05)) {
			// それでも見つからなければ高さが同じでアスペクト比が指定したサイズに近いものを探す
			selectedDelta = Double.MAX_VALUE;
			selectedSize = null;
			for (final Camera.Size size : supportedSizes) {
				if (size.width == requestWidth) {
					a = size.width / (double) size.height;
					r = Math.abs(a - aspect) / aspect;
					if (r < selectedDelta) {
						selectedSize = size;
						selectedDelta = r;
					}
				}
			}
		}
		// アスペクト比の差が±5%ならそれを選択する
		if ((selectedSize != null) && (selectedDelta < 0.05)) {
			Log.w(TAG, String.format("Set preview size to (%dx%d) instead of (%d,%d)",
				selectedSize.width, selectedSize.height, requestWidth, requestHeight));
		}

		return selectedSize;
	}

	/**
	 * 指定した範囲内のフレームレートで最大のものを選択する
	 * 結果はCamera.Parameters(mParams)へ設定
	 * @param minFps
	 * @param maxFps
	 * @return 見つかったフレームレート範囲, nullなら見つからなかった
	 */
	@Nullable
	public static int[] chooseFps(@NonNull final Camera.Parameters params,
		final float minFps, final float maxFps) {
		// サポートするフレームレートの一覧を取得、フレームレートの昇順にならんでいる
		final List<int[]> fpsRanges = params.getSupportedPreviewFpsRange();
		int[] foundFpsRange = null;
		if ((fpsRanges != null) && !fpsRanges.isEmpty()) {
			// カメラがサポートするfpsの内[minFps,maxFps]内の最大のものを取得する
			for (int x = fpsRanges.size() - 1; x >= 0; x--) {
				final int[] range = fpsRanges.get(x);
				if ((range[0] / 1000.0f >= minFps)
					&& (range[1] / 1000.0f <= maxFps)) {
					foundFpsRange = range;
					break;
				}
			}
			if (foundFpsRange == null) {
				for (int x = fpsRanges.size() - 1; x >= 0; x--) {
					final int[] range = fpsRanges.get(x);
					if (range[1] / 1000.0f <= maxFps) {
						foundFpsRange = range;
						break;
					}
				}
			}
			if (foundFpsRange == null) {
				// 見つからなかったときは一番早いフレームレートを選択
				Log.w(TAG, String.format(
					"chooseFps:specific fps range(%f-%f) not found," +
				 	"use fastest one", minFps, maxFps));
			}
		}
		// FPSをセット
		if (foundFpsRange != null) {
			params.setPreviewFpsRange(foundFpsRange[0], foundFpsRange[1]);
			final Camera.Size sz = params.getPreviewSize();
			Log.d(TAG, String.format("chooseFps:(%dx%d),fps=%d-%d",
				sz.width, sz.height, foundFpsRange[0], foundFpsRange[1]));
		}
		return foundFpsRange;
	}

	/**
	 * 画面の回転状態に合わせてプレビュー映像の回転設定を行う
	 * @param cameraId
	 * @param view
	 * @param camera
	 * @param params
	 * @return
	 */
	@SuppressLint("NewApi")
	public static int setupRotation(
		final int cameraId,
		@NonNull View view,
		@NonNull Camera camera,
		@NonNull final Camera.Parameters params) {

		if (DEBUG) Log.v(TAG, "CameraThread#setRotation:");
		int degrees = ViewUtils.getRotationDegrees(view);
		// get whether the camera is front camera or back camera
		final Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // front camera
			degrees = (info.orientation + degrees) % 360;
			degrees = (360 - degrees) % 360; // reverse
		} else { // back camera
			degrees = (info.orientation - degrees + 360) % 360;
		}
		// apply rotation setting
		camera.setDisplayOrientation(degrees);
		// XXX This method fails to call and camera stops working on some devices.
//		params.setRotation(degrees);
		return degrees;
	}

	/**
	 * 画面の回転状態に合わせてプレビュー映像の回転設定を行う
	 * @param context
	 * @param cameraId
	 * @param camera
	 * @param params
	 * @return
	 */
	public static int setupRotation(
		@NonNull Context context,
		final int cameraId,
		@NonNull Camera camera,
		@NonNull final Camera.Parameters params) {

		if (DEBUG) Log.v(TAG, "CameraThread#setRotation:");
		int degrees = ViewUtils.getRotationDegrees(context);
		// get whether the camera is front camera or back camera
		final Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // front camera
			degrees = (info.orientation + degrees) % 360;
			degrees = (360 - degrees) % 360; // reverse
		} else { // back camera
			degrees = (info.orientation - degrees + 360) % 360;
		}
		// apply rotation setting
		camera.setDisplayOrientation(degrees);
		// XXX This method fails to call and camera stops working on some devices.
//		params.setRotation(degrees);
		return degrees;
	}

	/**
	 * Cameraにプレビュー用Surface/SurfaseTexture/SurfaceHolder/SurfaceViewをセットする
	 * @param camera
	 * @param surface
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static void setPreviewSurface(
		@NonNull final Camera camera,
		@NonNull final Object surface) throws IllegalArgumentException, IOException {

		if (surface instanceof SurfaceTexture) {
			camera.setPreviewTexture((SurfaceTexture) surface);
		} else if (surface instanceof SurfaceHolder) {
			camera.setPreviewDisplay((SurfaceHolder)surface);
		} else if (surface instanceof Surface) {
			camera.setPreviewDisplay(new WrappedSurfaceHolder((Surface) surface));
		} else if (surface instanceof SurfaceView) {
			camera.setPreviewDisplay(((SurfaceView) surface).getHolder());
		}
	}

	/**
	 * 対応する映像フォーマットをlogCatへ出力する
	 * @param params
	 */
	public static void dumpSupportedPictureFormats(@NonNull final Camera.Parameters params) {
		final List<Integer> formats = params.getSupportedPictureFormats();
		for (final int format: formats) {
			switch (format) {
			case ImageFormat.DEPTH16:			Log.i(TAG, "supported: DEPTH16"); break;
			case ImageFormat.DEPTH_POINT_CLOUD:	Log.i(TAG, "supported: DEPTH_POINT_CLOUD"); break;
			case ImageFormat.FLEX_RGBA_8888:	Log.i(TAG, "supported: FLEX_RGBA_8888"); break;
			case ImageFormat.FLEX_RGB_888:		Log.i(TAG, "supported: FLEX_RGB_888"); break;
			case ImageFormat.JPEG:				Log.i(TAG, "supported: JPEG"); break;
			case ImageFormat.NV16:				Log.i(TAG, "supported: NV16"); break;
			case ImageFormat.NV21:				Log.i(TAG, "supported: NV21"); break;
			case ImageFormat.PRIVATE:			Log.i(TAG, "supported: PRIVATE"); break;
			case ImageFormat.RAW10:				Log.i(TAG, "supported: RAW10"); break;
			case ImageFormat.RAW12:				Log.i(TAG, "supported: RAW12"); break;
			case ImageFormat.RAW_PRIVATE:		Log.i(TAG, "supported: RAW_PRIVATE"); break;
			case ImageFormat.RAW_SENSOR:		Log.i(TAG, "supported: RAW_SENSOR"); break;
			case ImageFormat.RGB_565:			Log.i(TAG, "supported: RGB_565"); break;
			case ImageFormat.UNKNOWN:			Log.i(TAG, "supported: UNKNOWN"); break;
			case ImageFormat.YUV_420_888:		Log.i(TAG, "supported: YUV_420_888"); break;
			case ImageFormat.YUV_422_888:		Log.i(TAG, "supported: YUV_422_888"); break;
			case ImageFormat.YUV_444_888:		Log.i(TAG, "supported: YUV_444_888"); break;
			case ImageFormat.YUY2:				Log.i(TAG, "supported: YUY2"); break;
			case ImageFormat.YV12:				Log.i(TAG, "supported: YV12"); break;
			default:
				Log.i(TAG, String.format("supported: unknown, %08x", format));
				break;
			}
		}
	}
}
