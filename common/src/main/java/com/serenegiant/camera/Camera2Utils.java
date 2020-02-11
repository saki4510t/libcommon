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

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Camera 2 API用のヘルパークラス
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Utils implements CameraConst {
	private static final boolean DEBUG = false;
	private static final String TAG = Camera2Utils.class.getSimpleName();

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({
		CameraDevice.TEMPLATE_PREVIEW,
		CameraDevice.TEMPLATE_STILL_CAPTURE,
		CameraDevice.TEMPLATE_RECORD,
		CameraDevice.TEMPLATE_VIDEO_SNAPSHOT,
		CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG,
		CameraDevice.TEMPLATE_MANUAL,
	})
	public @interface RequestTemplate {}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	protected static class CompareSizesByArea implements Comparator<Size> {
		@Override
		public int compare(Size lhs, Size rhs) {
			// We cast here to ensure the multiplications won't overflow
			return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
					(long) rhs.getWidth() * rhs.getHeight());
		}
	}

	/**
	 * 指定した条件に合うカメラを探す
	 * @param manager
	 * @param preferedFace カメラの方向、対応するカメラがなければ異なるfaceのカメラが選択される
	 * @return
	 * @throws CameraAccessException
	 */
	public static CameraConst.CameraInfo findCamera(
		@NonNull final CameraManager manager,
		@CameraConst.FaceType final int preferedFace)
			throws CameraAccessException {

		if (DEBUG) Log.v(TAG, "findCamera:preferedFace=" + preferedFace);
		CameraConst.CameraInfo info = null;
		int targetFace;
		final String[] cameraIds = manager.getCameraIdList();
		if ((cameraIds != null) && (cameraIds.length > 0)) {
			final int face = (preferedFace == FACING_BACK
				? CameraCharacteristics.LENS_FACING_BACK
				: CameraCharacteristics.LENS_FACING_FRONT);
			boolean triedAllCameras = false;
			targetFace = face;
			String cameraId = null;
			int orientation = 0;
cameraLoop:
			for (; !triedAllCameras ;) {
				for (final String id: cameraIds) {
					final CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
					if (characteristics.get(CameraCharacteristics.LENS_FACING) == targetFace) {
						final StreamConfigurationMap map = characteristics.get(
								CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						cameraId = id;
						orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
						break cameraLoop;
					}
				}
				if ((cameraId == null) && (targetFace == face)) {
					targetFace = (face == CameraCharacteristics.LENS_FACING_BACK
						? CameraCharacteristics.LENS_FACING_FRONT
						: CameraCharacteristics.LENS_FACING_BACK);
				} else {
					triedAllCameras = true;
				}
			}
			if (!TextUtils.isEmpty(cameraId)) {
				info = new CameraConst.CameraInfo(cameraId, face, orientation,
					CameraConst.DEFAULT_WIDTH, CameraConst.DEFAULT_HEIGHT);
			}
		}
		return info;
	}

	/**
	 * 指定した条件に合うカメラを探す
	 * @param manager
	 * @param width
	 * @param height
	 * @param preferedFace
	 * @param degrees
	 * @return
	 * @throws CameraAccessException
	 */
	@Nullable
	public static CameraConst.CameraInfo findCamera(
		@NonNull final CameraManager manager,
		final int width, final int height,
		final int preferedFace, final int degrees)
			throws CameraAccessException {

		if (DEBUG) Log.v(TAG, String.format("findCamera:Size(%dx%d),preferedFace=%d,degrees=%d",
			width, height, preferedFace, degrees));

		String cameraId = null;
		Size previewSize = null;
		int targetFace = -1;
		int orientation = 0;
		final String[] cameraIds = manager.getCameraIdList();
		if ((cameraIds != null) && (cameraIds.length > 0)) {
			final int face = (preferedFace == FACING_BACK
				? CameraCharacteristics.LENS_FACING_BACK
				: CameraCharacteristics.LENS_FACING_FRONT);
			boolean triedAllCameras = false;
			targetFace = face;
cameraLoop:
			for (; !triedAllCameras ;) {
				for (final String id: cameraIds) {
					final CameraCharacteristics characteristics
						= manager.getCameraCharacteristics(id);
					if (characteristics.get(CameraCharacteristics.LENS_FACING) == targetFace) {
						final StreamConfigurationMap map = characteristics.get(
								CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						previewSize = chooseOptimalSize(characteristics, map, width, height, degrees);
						orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
						cameraId = id;
						break cameraLoop;
					}
				}
				if ((cameraId == null) && (targetFace == face)) {
					targetFace = (face == CameraCharacteristics.LENS_FACING_BACK
						? CameraCharacteristics.LENS_FACING_FRONT
						: CameraCharacteristics.LENS_FACING_BACK);
				} else {
					triedAllCameras = true;
				}
			}
		}
		if (!TextUtils.isEmpty(cameraId) && (previewSize != null)) {
			return new CameraConst.CameraInfo(cameraId, targetFace, orientation,
				previewSize.getWidth(), previewSize.getHeight());
		}
		if (DEBUG) Log.w(TAG, "findCamera: not found");
		return null;
	}

	/**
	 * 指定した条件に合う解像度を選択する
	 * @param manager
	 * @param cameraId
	 * @param targetFace
	 * @param width
	 * @param height
	 * @param degrees
	 * @return
	 * @throws CameraAccessException
	 */
	public static CameraConst.CameraInfo chooseOptimalSize(
		@NonNull final CameraManager manager,
		final String cameraId, @CameraConst.FaceType final int targetFace,
		final int width, final int height, final int degrees)
			throws CameraAccessException {

		if (DEBUG) Log.v(TAG,
			String.format("chooseOptimalSize:Size(%dx%d),targetFace=%d,degrees=%d",
				width, height, targetFace, degrees));

		final CameraCharacteristics characteristics
			= manager.getCameraCharacteristics(cameraId);
		final StreamConfigurationMap map = characteristics.get(
				CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		final Size previewSize
			= chooseOptimalSize(characteristics, map, width, height, degrees);
		final int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
		if (!TextUtils.isEmpty(cameraId) && (previewSize != null)) {
			return new CameraConst.CameraInfo(cameraId, targetFace, orientation,
				previewSize.getWidth(), previewSize.getHeight());
		}
		return null;
	}

	/**
	 * 指定した条件に合う解像度を選択する
	 * @param characteristics
	 * @param map
	 * @param _width
	 * @param _height
	 * @param degrees
	 * @return
	 */
	public static Size chooseOptimalSize(
		final CameraCharacteristics characteristics,
		final StreamConfigurationMap map,
		final int _width, final int _height, final int degrees) {

		if (DEBUG) Log.v(TAG,
			String.format("chooseOptimalSize:size(%d,%d),degrees=%d",
				_width, _height, degrees));

		// カメラの物理的な有効解像度を取得する
		final Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		if (DEBUG) Log.v(TAG, "activeArraySize=" + activeArraySize);
//		if (DEBUG) dump_available_scene_modes(characteristics);

		// MediaCodec用の解像度一覧を取得
		Size[] sizes = map.getOutputSizes(MediaCodec.class);	// MediaRecorder.class
		if (DEBUG) { for (final Size sz : sizes) { Log.v(TAG, "chooseOptimalSize:getOutputSizes(MediaCodec)=" + sz); } }
		// MediaCodec用の最大解像度を探す
		final Size ppsfv =  Collections.max(Arrays.asList(sizes), new CompareSizesByArea());
		// widthまたはheightが未指定名の時は最大解像度を使う・・・ここは1080pに制限しといた方が良いかもしれない
		if ((_width <= 0) || (_height <= 0)) {
			Log.d(TAG, "chooseOptimalSize:select" + ppsfv);
			return ppsfv;
		}

		// 縦画面か横画面かに合わせてアスペクト比を計算する(カメラ側は常に横長)
		final int width = Math.max(_width, _height);
		final int height = Math.min(_width, _height);
		final double aspect = (width > 0) && (height > 0)
			? (width / (double)height)
			: (ppsfv.getWidth() / (double)ppsfv.getHeight());
		final long max_areas = (width > 0) && (height > 0) ? width * (long)height : ppsfv.getWidth() * (long)ppsfv.getHeight();
		double a, r, selectedDelta = Double.MAX_VALUE;
		Size selectedSize = null;
		long areas;

		// SurfaceTextureへの描画用の解像度を一覧を取得
		sizes = map.getOutputSizes(SurfaceTexture.class);
		if (DEBUG) { for (final Size sz : sizes) { Log.v(TAG, "chooseOptimalSize:getOutputSizes(SurfaceTexture)=" + sz); } }

		// サイズが一致するものを探す
		for (Size sz : sizes) {
			if ((sz.getWidth() == width) && (sz.getHeight() == height)) {
				selectedSize = sz;
				Log.v(TAG, "chooseOptimalSize:found(" + selectedSize + ")");
				return selectedSize;
			}
		}

		final List<Size> possible = new ArrayList<Size>();
		if (selectedSize == null) {
			// 指定幅と同じでアスペクト比の小さいものを探す
			for (Size sz : sizes) {
				a = sz.getWidth() / (double)sz.getHeight();
				areas = sz.getWidth() * (long)sz.getHeight();
				r = Math.abs(a - aspect) / aspect;
//				if (DEBUG) Log.v(TAG, String.format("getOutputSizes(SurfaceTexture):%dx%d,a=%6.4f,r=%6.4f",
//					sz.getWidth(), sz.getHeight(), a, r));
				// 画素数が指定値以下でアスペクト比の差が0.2未満のを保存しておく
				if ((r < 0.2) && (areas <= max_areas)) {
					possible.add(sz);
				}
				// 指定幅と同じであればよりアスペクト比の差が小さいものを保持する
				if (sz.getWidth() == width) {
					if (r < selectedDelta) {
						selectedSize = sz;
						selectedDelta = r;
					}
				}
			}
		}

		// 指定幅と同じでアスペクト比の差が5%未満のものがなければ高さ基準で再度探す
		if ((selectedSize == null) || (selectedDelta >= 0.05)) {
			// heightが同じでアスペクト比が+/-5%未満のを探す
			selectedDelta = Double.MAX_VALUE;
			selectedSize = null;
			for (Size sz : sizes) {
				if (sz.getWidth() == width) {
					a = sz.getWidth() / (double)sz.getHeight();
					r = Math.abs(a - aspect) / aspect;
					if (r < selectedDelta) {
						selectedSize = sz;
						selectedDelta = r;
					}
				}
			}
		}
		// アスペクト比の差が+/-5%未満のがあればそれを選択する
		if ((selectedSize != null) && (selectedDelta < 0.05)) {
			Log.d(TAG, String.format("chooseOptimalSize:select(%dx%d), request(%d,%d)",
				selectedSize.getWidth(), selectedSize.getHeight(), width, height));
			return selectedSize;
		}
		// アスペクト比の差が0.2未満の中で最大解像度を取得する
		try {
			selectedSize = Collections.max(possible, new CompareSizesByArea());
		} catch (Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		// ここまで見つからなければMediaCodec用の最大解像度を使う・・・1080p以下に制限した方が良いかもしれない
		if (selectedSize == null)
			selectedSize = ppsfv;
		Log.d(TAG, "chooseOptimalSize:select(" + selectedSize + ")");
		return selectedSize;
	}
}
