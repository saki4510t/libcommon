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
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

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
	 * カメラ情報保持用のヘルパークラス
	 */
	public static final class CameraInfo {
		public String id = null;
		@FaceType
		public int face = FACING_UNSPECIFIED;
		public int orientation;
		public int width, height;

		public CameraInfo() {
		}

		public CameraInfo(final String id, final int face, final int orientation,
			final int width, final int height) {

			this.id = id;
			this.face = face;
			this.orientation = orientation;
			this.width = width;
			this.height = height;
		}

		public CameraInfo(final String id, final int face, final int orientation) {
			this.id = id;
			this.face = face;
			this.orientation = orientation;
		}

		public void set(final String id, final int face, final int orientation,
			final int width, final int height) {

			this.id = id;
			this.face = face;
			this.orientation = orientation;
			this.width = width;
			this.height = height;
		}

		public void set(@NonNull final CameraInfo other) {
			id = other.id;
			face = other.face;
			orientation = other.orientation;
			width = other.width;
			height = other.height;
		}

		public void set(@FaceType final int face) {
			this.face = face;
			id = String.format(Locale.US, "FACE_%d", face);
		}

		/**
		 * 有効なカメラ設定を保持しているかどうか
		 * idがnull/空文字列ではない & width>0 & height>0
		 * @return
		 */
		public boolean isValid() {
			return !TextUtils.isEmpty(id) && (width > 0) && (height > 0);
		}

		@NonNull
		@Override
		public String toString() {
			return String.format(Locale.US,
				"CameraInfo(face=%s(%d),id=%s,Size(%dx%d),orientation=%d)",
				faceString(face), face, id, width, height, orientation);
		}
	}

	/**
	 * FaceTypeを文字列表記に変換する
	 * @param face
	 * @return
	 */
	private static String faceString(@FaceType final int face) {
		switch (face) {
		case FACING_BACK: return "BACK";
		case FACING_FRONT: return "FRONT";
		case FACING_UNSPECIFIED:
		default: return "UNSPECIFIED";
		}
	}
}
