package com.serenegiant.camera;
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

import android.hardware.Camera;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

/**
 * Camera/Camera2関係の定数
 */
public interface CameraConst {
	public static final int FACING_UNSPECIFIED = -1;
	@SuppressWarnings("deprecation")
	public static final int FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
	@SuppressWarnings("deprecation")
	public static final int FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;

	@IntDef({
		FACING_UNSPECIFIED,
		FACING_BACK,
		FACING_FRONT,})
	@Retention(RetentionPolicy.SOURCE)
	public @interface FaceType {}

	public static final int DEFAULT_WIDTH = 640;
	public static final int DEFAULT_HEIGHT = 480;

	/**
	 * FaceTypeを文字列表記に変換する
	 * @param face
	 * @return
	 */
	static String faceString(@FaceType final int face) {
		switch (face) {
		case FACING_BACK: return "BACK";
		case FACING_FRONT: return "FRONT";
		case FACING_UNSPECIFIED:
		default: return "UNSPECIFIED";
		}
	}
}
