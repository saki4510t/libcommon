package com.serenegiant.glutils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class TextureWrapper implements Parcelable {
	public final int texId;
	public final int width;
	public final int height;

	public TextureWrapper(final int texId, final int width, final int height) {
		this.texId = texId;
		this.width = width;
		this.height = height;
	}

	protected TextureWrapper(@NonNull final Parcel in) {
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
		dest.writeInt(texId);
		dest.writeInt(width);
		dest.writeInt(height);
	}

	public static final Creator<TextureWrapper> CREATOR = new Creator<TextureWrapper>() {
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
