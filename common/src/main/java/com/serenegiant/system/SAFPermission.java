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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Pair;

import java.lang.ref.WeakReference;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

/**
 * SAFでディレクトリアクセスの権限を取得するためのヘルパークラス
 */
@RequiresApi(21)
public class SAFPermission {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = SAFPermission.class.getSimpleName();

	/**
	 * registerForActivityResultとActivityResultLauncherを使ったSAFアクセスパーミッション要求時のコールバックリスナー
	 */
	public interface SAFCallback {
		/**
		 * DocumentTreeのuriを取得できた時
		 * SAFUtils.takePersistableUriPermissionはすでに呼び出し済み
		 * @param treeId
		 * @param uri
		 */
		public void onResult(final int treeId, @NonNull final Uri uri);

		/**
		 * DocumentTreeのuriを取得できなかった時
		 * SAFUtils.releasePersistableUriPermissionはすでに呼び出し済み
		 * @param treeId
		 */
		public void onFailed(final int treeId);
	}

	/**
	 * デフォルトのSAFCallback実装
	 */
	public static class DefaultSAFCallback implements SAFCallback {
		@Override
		public void onResult(final int treeId, @NonNull final Uri uri) {
		}

		@Override
		public void onFailed(final int treeId) {
		}
	}

	public static final SAFCallback DEFAULT_CALLBACK = new DefaultSAFCallback();

	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final ActivityResultLauncher<Pair<Integer, Uri>> mLauncher;

	/**
	 * コンストラクタ
	 * Activityの#onCreateで呼び出すこと、それ以降だとクラッシュする
	 * Fragmentからはこのコンストラクタではなく引数にFragmentをとるコンストラクタを呼び出すこと
	 * @param activity
	 * @param callback
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public SAFPermission(
		@NonNull final ComponentActivity activity,
		@NonNull final SAFCallback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<>(activity);
		mLauncher = activity.registerForActivityResult(new OpenDocumentTree(),
			new MyActivityResultCallback(mWeakContext, callback));
	}

	/**
	 * コンストラクタ
	 * Fragmentの#onAttachまたは#onCreateから呼び出すこと, それ以降だとクラッシュする
	 * @param fragment
	 * @param callback
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public SAFPermission(
		@NonNull final Fragment fragment,
		@NonNull final SAFCallback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<>(fragment.requireContext());
		mLauncher = fragment.registerForActivityResult(new OpenDocumentTree(),
			new MyActivityResultCallback(mWeakContext, callback));
	}

	/**
	 * ディレクトリアクセスのためのドキュメントツリーUriをSAFで取得要求する
	 * 結果はコンストラクタへ引き渡したコールバックリスナーで返す
	 * @param treeId
	 * @throws IllegalStateException
	 */
	public void requestPermission(final int treeId) throws IllegalStateException {
		requestPermission(treeId, null);
	}

	/**
	 * ディレクトリアクセスのためのドキュメントツリーUriをSAFで取得要求する
	 * 結果はコンストラクタへ引き渡したコールバックリスナーで返す
	 * initialUriの指定はAPI>=26のときのみ有効
	 * @param treeId
	 * @param initialUri
	 * @throws IllegalStateException
	 */
	@RequiresApi(26)
	public void requestPermission(
		final int treeId,
		@Nullable final Uri initialUri) throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "requestPermission:");
		final Context context = mWeakContext.get();
		if (context != null) {
			SAFUtils.releasePersistableUriPermission(context, treeId);
			mLauncher.launch(Pair.create(treeId, initialUri));
		} else {
			throw new IllegalStateException("context is already released!");
		}
	}

//--------------------------------------------------------------------------------
	private static class MyActivityResultCallback implements ActivityResultCallback<Pair<Integer, Uri>> {
		private static final String TAG = MyActivityResultCallback.class.getSimpleName();
		@NonNull
		private final WeakReference<Context> mWeakContext;
		@NonNull
		final SAFCallback callback;

		private MyActivityResultCallback(
			@NonNull final WeakReference<Context> weakContext,
			@NonNull final SAFCallback callback) {

			if (DEBUG) Log.v(TAG, "コンストラクタ:");
			mWeakContext = weakContext;
			this.callback = callback;
		}

		@Override
		public void onActivityResult(final Pair<Integer, Uri> result) {
			if (DEBUG) Log.v(TAG, "onActivityResult:" + result);
			if (result != null) {
				final int requestCode = result.first;
				final Uri uri = result.second;
				final Context context = mWeakContext.get();
				if (uri != null) {
					// DocumentTreeのuriを取得できた時
					if (context != null) {
						SAFUtils.takePersistableUriPermission(context, requestCode, uri);
					}
					callback.onResult(requestCode, uri);
				} else {
					// DocumentTreeのuriを取得できなかったとき
					if (context != null) {
						SAFUtils.releasePersistableUriPermission(context, requestCode);
					}
					callback.onFailed(requestCode);
				}
			} else {
				// ActivityResultContractの実装上実際にはresultはnullにならないけど
				callback.onFailed(0);
			}
		}
	}

	@RequiresApi(21)
	private static class OpenDocumentTree extends ActivityResultContract<Pair<Integer, Uri>, Pair<Integer, Uri>> {
		private static final String TAG = OpenDocumentTree.class.getSimpleName();
		@Nullable
		private Pair<Integer, Uri> input;

		@CallSuper
		@NonNull
		@Override
		public Intent createIntent(@NonNull Context context, @Nullable Pair<Integer, Uri> input) {
			if (DEBUG) Log.v(TAG, "createIntent:" + input);
			this.input = input;
			final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
				final Uri initialUri = input != null ? input.second : null;
				intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
			}
			return intent;
		}

		@Nullable
		@Override
		public final SynchronousResult<Pair<Integer, Uri>> getSynchronousResult(@NonNull Context context,
				@Nullable Pair<Integer, Uri> input) {
			if (DEBUG) Log.v(TAG, "getSynchronousResult:" + input);
			this.input = input;
			if (input != null) {
				final Uri uri = SAFUtils.getStorageUri(context, input.first);
				if (uri != null) {
					return new SynchronousResult<Pair<Integer, Uri>>(Pair.create(input.first, uri));
				}
			}
			return null;
		}

		@Nullable
		@Override
		public final Pair<Integer, Uri> parseResult(final int resultCode, @Nullable Intent intent) {
			final Uri uri = ((intent!= null) && (resultCode == Activity.RESULT_OK)) ? intent.getData() : null;
			final int treeId = input != null ? input.first : 0;
			return Pair.create(treeId, uri);
		}
	}
}
