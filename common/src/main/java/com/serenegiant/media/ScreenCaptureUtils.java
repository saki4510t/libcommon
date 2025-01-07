package com.serenegiant.media;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import com.serenegiant.system.ContextUtils;

import java.lang.ref.WeakReference;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

/**
 * MediaProjectionManagerでスクリーンキャプチャーするためのヘルパークラス
 * MediaProjectionManager#getMediaProjectionへ引き渡すIntentを取得する
 * 本当はoutの型をMediaProjectionにしてMediaProjectionの取得まで実行したい
 * ところだけど、それだとバックグラウンドで録画や映像伝送する際にバックグラウンド
 * 動作のServiceへ引き渡すのが難しくなるので、
 * MediaProjectionManager#getMediaProjectionへ引き渡すIntentを取得する
 * ところまでの処理とする
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenCaptureUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = ScreenCaptureUtils.class.getSimpleName();

	public interface ScreenCaptureCallback {
		/**
		 * ScreenCaptureUtils#requestScreenCaptureが成功した時のコールバック
		 * MediaProjectionManager#getMediaProjectionへ引き渡すIntent
		 * このコールバックメソッドが呼ばれたときはActivity.RESULT_OKなので
		 * getMediaProjectionの第1引数はActivity.RESULT_OKにすること
		 * @param data
		 */
		public void onResult(@NonNull final Intent data);

		/**
		 * ScreenCaptureUtils#requestMediaProjectionが成功してスクリーンキャプチャー用の
		 * MediaProjectionを取得できたときのコールバック
		 * @param projection
		 */
		public void onResult(@NonNull final MediaProjection projection);

		/**
		 * スクリーンキャプチャーの許可要求をユーザーが拒否したとき
		 */
		public void onFailed();
	}

	@NonNull
	private final ActivityResultLauncher<Void> mLauncher;
	private final ActivityResultLauncher<Void> mProjectionLauncher;
	private boolean mRequestProjection;

	/**
	 * コンストラクタ
	 * @param activity
	 * @param callback
	 */
	public ScreenCaptureUtils(
		@NonNull final ComponentActivity activity,
		@NonNull final ScreenCaptureCallback callback) {

		mLauncher = activity.registerForActivityResult(
			new ScreenCapture(),
			result -> {
				if (DEBUG) Log.v(TAG, "onActivityResult:" + result);
				if (result != null) {
					callback.onResult(result);
				} else {
					callback.onFailed();
				}
			});
		mProjectionLauncher = activity.registerForActivityResult(
			new MediaProjectionContract(activity),
			result -> {
				if (DEBUG) Log.v(TAG, "onActivityResult:" + result);
				if (result != null) {
					callback.onResult(result);
				} else {
					callback.onFailed();
				}
			});
	}

	/**
	 * コンストラクタ
	 * @param fragment
	 * @param callback
	 */
	public ScreenCaptureUtils(
		@NonNull final Fragment fragment,
		@NonNull final ScreenCaptureCallback callback) {

		mLauncher = fragment.registerForActivityResult(
			new ScreenCapture(),
			result -> {
				if (result != null) {
					callback.onResult(result);
				} else {
					callback.onFailed();
				}
			});
		mProjectionLauncher = fragment.registerForActivityResult(
			new MediaProjectionContract(fragment.requireContext()),
			result -> {
				if (DEBUG) Log.v(TAG, "onActivityResult:" + result);
				if (result != null) {
					callback.onResult(result);
				} else {
					callback.onFailed();
				}
			});
	}

	/**
	 * スクリーンキャプチャーの許可を求める
	 * 成功すればScreenCaptureCallback#onResult(@NonNull final Intent)が呼ばれる
	 */
	public void requestScreenCapture() {
		if (!mRequestProjection) {
			mRequestProjection = true;
			mLauncher.launch(null);
		}
	}

	/**
	 * スクリーンキャプチャーの許可を求める
	 * 成功すればScreenCaptureCallback#onResult(@NonNull final MediaProjection)が呼ばれる
	 */
	public void requestMediaProjection() {
		if (!mRequestProjection) {
			mRequestProjection = true;
			mLauncher.launch(null);
		}
	}

	/**
	 * MediaProjectionManager/MediaProjectionを利用したスクリーンキャプチャー
	 * 開始要求に使うためのActivityResultContract実装
	 * 本当はoutの型をMediaProjectionにしてparseResultでMediaProjectionの
	 * 取得まで実行したいところだけど、それだとバックグラウンドで録画や映像伝送する際に
	 * バックグラウンド動作のServiceへ引き渡すのが難しくなるので、
	 * MediaProjectionManager#getMediaProjectionへ引き渡すIntentを取得するところまでの
	 * 処理とする
	 */
	private static class ScreenCapture extends ActivityResultContract<Void, Intent> {
		private static final String TAG = ScreenCapture.class.getSimpleName();

		@CallSuper
		@NonNull
		@Override
		public Intent createIntent(@NonNull Context context, final Void unsued) {
			if (DEBUG) Log.v(TAG, "createIntent:");
			final MediaProjectionManager manager
				= ContextUtils.requireSystemService(context, MediaProjectionManager.class);
			return manager.createScreenCaptureIntent();
		}

		@Nullable
		@Override
		public final SynchronousResult<Intent> getSynchronousResult(
			@NonNull Context context,
			@Nullable Void unused) {
			if (DEBUG) Log.v(TAG, "getSynchronousResult:");
			return null;
		}

		@Nullable
		@Override
		public final Intent parseResult(final int resultCode, @Nullable Intent intent) {
			if (DEBUG) Log.v(TAG, "parseResult:resultCode=" + resultCode + "," + intent);
			return ((intent!= null)
				&& (resultCode == Activity.RESULT_OK))
				? intent : null;
		}
	}

	/**
	 * MediaProjectionManager/MediaProjectionを利用したスクリーンキャプチャー
	 * 開始要求に使うためのActivityResultContract実装
	 * こっちのoutの型はMediaProjectionでMediaProjectionManager#getMediaProjectionを使って
	 * MediaProjectionを取得する処理までを一括で行う
	 */
	private static class MediaProjectionContract extends ActivityResultContract<Void, MediaProjection> {
		private static final String TAG = MediaProjection.class.getSimpleName();

		@NonNull
		private final WeakReference<Context> mWeakContext;

		private MediaProjectionContract(@NonNull final Context context) {
			mWeakContext = new WeakReference<>(context);
		}

		@CallSuper
		@NonNull
		@Override
		public Intent createIntent(@NonNull final Context context, final Void unused) {
			final MediaProjectionManager manager
				= ContextUtils.requireSystemService(context, MediaProjectionManager.class);
			return manager.createScreenCaptureIntent();
		}

		@Nullable
		@Override
		public final SynchronousResult<MediaProjection> getSynchronousResult(
			@NonNull Context context, @Nullable Void unused) {
			if (DEBUG) Log.v(TAG, "getSynchronousResult:");
			return null;
		}

		@Override
		public MediaProjection parseResult(final int resultCode, @Nullable Intent intent) {
			if (DEBUG) Log.v(TAG, "parseResult:resultCode=" + resultCode + "," + intent);
			if ((intent!= null)
				&& (resultCode == Activity.RESULT_OK)) {

				try {
					final Context context = mWeakContext.get();
					if (context != null) {
						final MediaProjectionManager manager
							= ContextUtils.requireSystemService(context, MediaProjectionManager.class);
						return manager.getMediaProjection(Activity.RESULT_OK, intent);
					}
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			return null;
		}
	}
}
