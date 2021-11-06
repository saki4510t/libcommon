package com.serenegiant.system;

/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
 * StorageUtilsで取得するストレージ情報のホルダークラス
 */
public class StorageInfo implements Parcelable {
	public long totalBytes;
	public long freeBytes;
	
	public StorageInfo(final long total, final long free) {
		totalBytes = total;
		freeBytes = free;
	}

	protected StorageInfo(@NonNull final Parcel in) {
		totalBytes = in.readLong();
		freeBytes = in.readLong();
	}

	@NonNull
	@Override
	public String toString() {
		return "StorageInfo{" +
			"totalBytes=" + totalBytes +
			", freeBytes=" + freeBytes +
			'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeLong(totalBytes);
		dest.writeLong(freeBytes);
	}

	public static final Creator<StorageInfo> CREATOR = new Creator<StorageInfo>() {
		@Override
		public StorageInfo createFromParcel(@NonNull final Parcel in) {
			return new StorageInfo(in);
		}

		@Override
		public StorageInfo[] newArray(final int size) {
			return new StorageInfo[size];
		}
	};
}
