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

import android.text.TextUtils;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.camera.CameraConst.faceString;

/**
 * カメラ情報保持用のヘルパークラス
 */
public final class CameraInfo {
	public String id = null;
	@CameraConst.FaceType
	public int face = CameraConst.FACING_UNSPECIFIED;
	public int orientation;
	public int width, height;
	public boolean canDisableShutterSound;

	/**
	 * コンストラクタ
	 */
	public CameraInfo() {
	}

	/**
	 * コンストラクタ
	 * @param other
	 */
	public CameraInfo(@NonNull final CameraInfo other) {
		set(other);
	}

	/**
	 * コンストラクタ
	 * @param id
	 * @param face
	 * @param orientation
	 * @param width
	 * @param height
	 */
	public CameraInfo(
		final String id, final int face, final int orientation,
		final int width, final int height) {

		this.id = id;
		this.face = face;
		this.orientation = orientation;
		this.width = width;
		this.height = height;
	}

	/**
	 * コンストラクタ
	 * @param id
	 * @param face
	 * @param orientation
	 */
	public CameraInfo(final String id, final int face, final int orientation) {
		this.id = id;
		this.face = face;
		this.orientation = orientation;
	}

	/**
	 * セッター
	 * @param id
	 * @param face
	 * @param orientation
	 * @param width
	 * @param height
	 */
	public void set(
		final String id, final int face, final int orientation,
		final int width, final int height) {

		this.id = id;
		this.face = face;
		this.orientation = orientation;
		this.width = width;
		this.height = height;
	}

	/**
	 * セッター
	 * @param other
	 */
	public void set(@Nullable final CameraInfo other) {
		if (other == null) {
			id = null;
			width = height = 0;
			canDisableShutterSound = false;
		} else if (other != this) {
			id = other.id;
			face = other.face;
			orientation = other.orientation;
			width = other.width;
			height = other.height;
			canDisableShutterSound = other.canDisableShutterSound;
		}
	}

	/**
	 * セッター
	 * @param face
	 */
	public void set(@CameraConst.FaceType final int face) {
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
