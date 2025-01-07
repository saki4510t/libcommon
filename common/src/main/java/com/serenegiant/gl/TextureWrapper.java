package com.serenegiant.gl;
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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * OpenGL|ESのテクスチャ情報のホルダークラス
 */
public class TextureWrapper implements GLConst, Parcelable {
	@TexTarget
	public final int texTarget;
	@TexUnit
	public final int texUnit;
	public final int texId;
	public final int width;
	public final int height;

	/**
	 * コンストラクタ
	 * @param texTarget
	 * @param texUnit
	 * @param texId
	 * @param width
	 * @param height
	 */
	public TextureWrapper(
		@TexTarget final int texTarget,
		@TexUnit final int texUnit, final int texId,
		final int width, final int height) {

		this.texTarget = texTarget;
		this.texUnit = texUnit;
		this.texId = texId;
		this.width = width;
		this.height = height;
	}

	/**
	 * Parcelable実装用のコンストラクタ
	 * @param in
	 */
	protected TextureWrapper(@NonNull final Parcel in) {
		texTarget = in.readInt();
		texUnit = in.readInt();
		texId = in.readInt();
		width = in.readInt();
		height = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(texTarget);
		dest.writeInt(texUnit);
		dest.writeInt(texId);
		dest.writeInt(width);
		dest.writeInt(height);
	}

	public static final Creator<TextureWrapper> CREATOR
		= new Creator<TextureWrapper>() {

		@Override
		public TextureWrapper createFromParcel(Parcel in) {
			return new TextureWrapper(in);
		}

		@Override
		public TextureWrapper[] newArray(int size) {
			return new TextureWrapper[size];
		}
	};

}
