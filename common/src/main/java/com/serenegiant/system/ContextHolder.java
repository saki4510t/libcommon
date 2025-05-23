package com.serenegiant.system;
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Looper;

import java.lang.ref.WeakReference;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Contextを保持するクラスのベースクラス
 * @param <T>
 */
public abstract class ContextHolder<T extends Context> {

	@NonNull
	private final WeakReference<T> mWeakContext;
	private volatile boolean mReleased;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ContextHolder(@NonNull final T context) {
		mWeakContext = new WeakReference<>(context);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public final void release() {
		if (!mReleased) {
			mReleased = true;
			internalRelease();
			mWeakContext.clear();
		}
	}

	protected abstract void internalRelease();

	@Nullable
	public T getContext() {
		return mWeakContext.get();
	}

	@NonNull
	public T requireContext() throws IllegalStateException {
		final T context = getContext();
		if (context == null) {
			throw new IllegalStateException();
		}
		return context;
	}

	@Nullable
	public AssetManager getAssets() {
		final Context context = getContext();
		return context != null ? context.getAssets() : null;
	}

	@NonNull
	public AssetManager requireAssets() throws IllegalStateException {
		final AssetManager result = requireContext().getAssets();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	public ContentResolver getContentResolver() {
		final Context context = getContext();
		return context != null ? context.getContentResolver() : null;
	}

	@NonNull
	public ContentResolver requireContentResolver() throws IllegalStateException {
		final ContentResolver result = requireContext().getContentResolver();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	@NonNull
	public LocalBroadcastManager requireLocalBroadcastManager() throws IllegalStateException {
		return LocalBroadcastManager.getInstance(requireContext());
	}

	@Nullable
	public Looper getMainLooper() {
		final Context context = getContext();
		return context != null ? context.getMainLooper() : null;
	}

	@NonNull
	public Looper requireMainLooper() throws IllegalStateException {
		final Looper result = requireContext().getMainLooper();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	@NonNull
	public PackageManager getPackageManager() {
		final Context context = getContext();
		return context != null ? context.getPackageManager() : null;
	}

	@NonNull
	public PackageManager requirePackageManager() {
		final PackageManager result = requireContext().getPackageManager();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	public String getPackageName() {
		final Context context = getContext();
		return context != null ? context.getPackageName() : null;
	}

	@NonNull
	public String requirePackageName() throws IllegalStateException {
		final String result = requireContext().getPackageName();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	@Nullable
	public Resources getResources() {
		final Context context = getContext();
		return context != null ? context.getResources() : null;
	}

	@NonNull
	public Resources requireResources() throws IllegalStateException {
		final Resources result = requireContext().getResources();
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	@Nullable
	public SharedPreferences getSharedPreferences(final String name, final int mode) {
		final Context context = getContext();
		return context != null ? context.getSharedPreferences(name, mode) : null;
	}

	@NonNull
	public SharedPreferences requireSharedPreferences(final String name, final int mode) throws IllegalStateException {
		final SharedPreferences result = requireContext().getSharedPreferences(name, mode);
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	@NonNull
	public String getString(final int resId) throws IllegalStateException {
		return requireContext().getString(resId);
	}

	@NonNull
	public String getString(final int resId, final Object... formatArgs) throws IllegalStateException {
		return requireContext().getString(resId, formatArgs);
	}

	@NonNull
	public CharSequence getText(final int resId) throws IllegalStateException {
		return requireContext().getText(resId);
	}

	public int getColor(@ColorRes final int resId) throws IllegalStateException {
		return ResourcesCompat.getColor(requireResources(), resId, null);
	}

	public void sendLocalBroadcast(@NonNull final Intent intent) throws IllegalStateException {
		requireLocalBroadcastManager().sendBroadcast(intent);
	}
}
