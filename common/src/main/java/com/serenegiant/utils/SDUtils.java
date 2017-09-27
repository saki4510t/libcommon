package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

public class SDUtils {
	private static final String TAG = SDUtils.class.getSimpleName();

//********************************************************************************
// Storage Access Framework関係
//********************************************************************************
	
	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理のうち
	 * Storage Access Framework関係の処理を行うためのdelegater
	 */
	public interface handleOnResultDelegater {
		public void onResult(final int requestCode, final Uri uri, final Intent data);
		public void onFailed(final int requestCode, final Intent data);
	}
	
	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理をdelegaterで
	 * 処理するためのヘルパーメソッド
	 * @param context
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @param delegater
	 */
	public static void handleOnResult(final Context context,
		final int requestCode, final int resultCode,
		final Intent data, final handleOnResultDelegater delegater) {

		if ((data != null) && (delegater != null)) {
			final String action = data.getAction();
			if (resultCode == Activity.RESULT_OK) {
				final Uri uri = data.getData();
				if (uri != null) {
					try {
						delegater.onResult(requestCode, uri, data);
						return;
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
			try {
				clearUri(context, getKey(requestCode));
				delegater.onFailed(requestCode, data);
			} catch (final Exception e) {
			}
		}
	}
	
	/**
	 * uriを保存する際に使用する共有プレファレンスのキー名を要求コードから生成する
	 * @param request_code
	 * @return
	 */
	private static String getKey(final int request_code) {
		return String.format(Locale.US, "SDUtils-%d", request_code);
	}
	
	/**
	 * uriを共有プレファレンスに保存する
	 * @param context
	 * @param key
	 * @param uri
	 */
	private static void saveUri(final Context context, final String key, final Uri uri) {
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if (pref != null) {
			pref.edit().putString(key, uri.toString()).apply();
		}
	}
	
	/**
	 * 共有プレファレンスの保存しているuriを取得する
	 * @param context
	 * @param key
	 * @return
	 */
	@Nullable
	private static Uri loadUri(final Context context, final String key) {
		Uri result = null;
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if ((pref != null) && pref.contains(key)) {
			try {
				result = Uri.parse(pref.getString(key, null));
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
		return result;
	}
	
	/**
	 * 共有プレファレンスに保存しているuriを消去する
	 * @param context
	 * @param key
	 */
	private static void clearUri(final Context context, final String key) {
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if ((pref != null) && pref.contains(key)) {
			try {
				pref.edit().remove(key).apply();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(final Activity activity,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime_type), request_code);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(final FragmentActivity activity,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime_type), request_code);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(final android.app.Fragment fragment,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime_type), request_code);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(final android.support.v4.app.Fragment fragment,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime_type), request_code);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime_type
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static Intent prepareOpenDocumentIntent(final String mime_type) {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime_type);
		return intent;
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final Activity activity,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime_type, null), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param default_name
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final Activity activity,
		final String mime_type, final String default_name, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime_type, default_name), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final FragmentActivity activity,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime_type, null), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime_type
	 * @param default_name
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final FragmentActivity activity,
		final String mime_type, final String default_name, final int request_code) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime_type, default_name), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final android.app.Fragment fragment,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime_type, null), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime_type
	 * @param default_name
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final android.app.Fragment fragment,
		final String mime_type, final String default_name, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime_type, default_name), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime_type
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final android.support.v4.app.Fragment fragment,
		final String mime_type, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime_type, null), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
 	 * @param fragment
	 * @param mime_type
	 * @param default_name
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(final android.support.v4.app.Fragment fragment,
		final String mime_type, final String default_name, final int request_code) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime_type, default_name), request_code);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime_type
	 * @param default_name
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static Intent prepareCreateDocument(final String mime_type, final String default_name) {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime_type);
		if (!TextUtils.isEmpty(default_name)) {
			intent.putExtra(Intent.EXTRA_TITLE, default_name);
		}
		return intent;
	}

	/**
	 * ファイル削除要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param context
	 * @param uri
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean requestDeleteDocument(final Context context, final Uri uri) {
		try {
			return BuildCheck.isKitKat() && DocumentsContract.deleteDocument(context.getContentResolver(), uri);
		} catch (final FileNotFoundException e) {
			return false;
		}
	}

	/**
	 * request_codeに対応するUriへアクセス可能かどうか
	 * @param context
	 * @param request_code
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean hasStorageAccess(final Context context, final int request_code) {
		boolean found = false;
		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(request_code));
			if (uri != null) {
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list = context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// request_codeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
			}
		}
		return found;
	}

	/**
	 * request_codeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param request_code
	 * @return 既にrequest_codeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(final Activity activity, final int request_code) {
		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, request_code);
			if (uri == null) {
				// request_codeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), request_code);
			}
			return uri;
		}
		return null;
	}

	/**
	 * request_codeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param request_code
	 * @return 既にrequest_codeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(final FragmentActivity activity, final int request_code) {
		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, request_code);
			if (uri == null) {
				// request_codeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), request_code);
			}
			return uri;
		}
		return null;
	}

	/**
	 * request_codeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param request_code
	 * @return 既にrequest_codeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(final android.app.Fragment fragment, final int request_code) {
		final Uri uri = getStorageUri(fragment.getActivity(), request_code);
		if (uri == null) {
			// request_codeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
			fragment.startActivityForResult(prepareStorageAccessPermission(), request_code);
		}
		return uri;
	}

	/**
	 * request_codeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param request_code
	 * @return 既にrequest_codeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(final android.support.v4.app.Fragment fragment, final int request_code) {
		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(fragment.getActivity(), request_code);
			if (uri == null) {
				// request_codeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				fragment.startActivityForResult(prepareStorageAccessPermission(), request_code);
			}
			return uri;
		}
		return null;
	}

	/**
	 * request_codeに対応するUriが存在していて恒常的パーミッションがあればそれを返す, なければnullを返す
	 * @param context
	 * @param request_code
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Nullable private static Uri getStorageUri(final Context context, final int request_code) {
		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(request_code));
			if (uri != null) {
				boolean found = false;
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list = context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// request_codeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
				if (found) {
					return uri;
				}
			}
		}
		return null;
	}

	/**
	 * requestStorageAccessの下請け
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static Intent prepareStorageAccessPermission() {
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	}

	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * @param context
	 * @param tree_uri
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccessPermission(final Context context,
		final int request_code, final Uri tree_uri) {

		if (BuildCheck.isLollipop()) {
			context.getContentResolver().takePersistableUriPermission(tree_uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			saveUri(context, getKey(request_code), tree_uri);
			return tree_uri;
		} else {
			return null;
		}
	}

	/**
	 * 恒常的にアクセスできるように取得したパーミッションを開放する
	 * @param context
	 * @param request_code
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void releaseStorageAccessPermission(final Context context,
		final int request_code) {

		if (BuildCheck.isLollipop()) {
			final String key = getKey(request_code);
			final Uri uri = loadUri(context, key);
			if (uri != null) {
				context.getContentResolver().releasePersistableUriPermission(uri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				clearUri(context, key);
			}
		}
	}

	/**
	 *
	 * @param context
	 * @param tree_id
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageDir(final Context context, final int tree_id) {
		Log.i(TAG, "createStorageDir:");
		if (BuildCheck.isLollipop()) {
			final Uri tree_uri = getStorageUri(context, tree_id);
			if (tree_uri != null) {
				final DocumentFile save_tree = DocumentFile.fromTreeUri(context, tree_uri);
				final String path = UriHelper.getPath(context, save_tree.getUri());
				if (!TextUtils.isEmpty(path)) {
					final File dir = new File(path);
					if (dir.canWrite()) {
						dir.mkdirs();
						return dir;
					} else if (dir.canRead()) {
						return dir;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param tree_id
	 * @param mime
	 * @param file_name
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageFile(final Context context,
		final int tree_id, final String mime, final String file_name) {

		Log.i(TAG, "createStorageFile:" + file_name);
		return createStorageFile(context, getStorageUri(context, tree_id), mime, file_name);
	}

	/**
	 * 指定したUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param tree_uri
	 * @param mime
	 * @param file_name
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageFile(final Context context,
		final Uri tree_uri, final String mime, final String file_name) {
		Log.i(TAG, "createStorageFile:" + file_name);

		if (BuildCheck.isLollipop()) {
			if ((context != null) && (tree_uri != null) && !TextUtils.isEmpty(file_name)) {
				final DocumentFile save_tree = DocumentFile.fromTreeUri(context, tree_uri);
				final DocumentFile target = save_tree.createFile(mime, file_name);
				final String path = UriHelper.getPath(context, target.getUri());
				if (!TextUtils.isEmpty(path)) {
					return new File(path);
				}
			}
		}
		return null;
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param tree_id
	 * @param mime
	 * @param file_name
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int createStorageFileFD(final Context context,
		final int tree_id, final String mime, final String file_name) {

		Log.i(TAG, "createStorageFileFD:" + file_name);
		return createStorageFileFD(context, getStorageUri(context, tree_id), mime, file_name);
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param tree_uri
	 * @param mime
	 * @param file_name
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int createStorageFileFD(final Context context,
		final Uri tree_uri, final String mime, final String file_name) {

		Log.i(TAG, "createStorageFileFD:" + file_name);
		if (BuildCheck.isLollipop()) {
			if ((context != null) && (tree_uri != null) && !TextUtils.isEmpty(file_name)) {
				final DocumentFile save_tree = DocumentFile.fromTreeUri(context, tree_uri);
				final DocumentFile target = save_tree.createFile(mime, file_name);
				try {
					final ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(target.getUri(), "rw");
					return fd != null ? fd.getFd() : 0;
				} catch (final FileNotFoundException e) {
					Log.w(TAG, e);
				}
			}
		}
		return 0;
	}
}
