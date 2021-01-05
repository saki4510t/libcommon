package com.serenegiant.utils;
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Storage Access Framework/DocumentFile関係のヘルパークラス
 * systemパッケージに移動したのでそちらを使うこと
 */
@Deprecated
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SAFUtils {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = SAFUtils.class.getSimpleName();

	private SAFUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理のうち
	 * Storage Access Framework関係の処理を行うためのdelegater
	 */
	public interface handleOnResultDelegater {
		public boolean onResult(final int requestCode,
			@NonNull final Uri uri, @NonNull final Intent data);

		public void onFailed(final int requestCode, @Nullable final Intent data);
	}
	
	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理をdelegaterで
	 * 処理するためのヘルパーメソッド
	 * @param context
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @param delegater
	 * @return true if successfully handled, false otherwise
	 */
	public static boolean handleOnResult(
		@NonNull final Context context,
		final int requestCode, final int resultCode,
		@Nullable final Intent data,
		@NonNull final SAFUtils.handleOnResultDelegater delegater) {

		if ((data != null) && (resultCode == Activity.RESULT_OK)) {
			final Uri uri = data.getData();
			if (uri != null) {
				try {
					return delegater.onResult(requestCode, uri, data);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
		try {
			clearUri(context, getKey(requestCode));
			delegater.onFailed(requestCode, data);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		return false;
	}
	
//--------------------------------------------------------------------------------
	/**
	 * requestCodeに対応するUriへアクセス可能かどうか
	 * deprecatedなので#requestPermissionを代わりに使う
	 * @param context
	 * @param requestCode
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	public static boolean hasStorageAccess(
		@NonNull final Context context,
		final int requestCode) {

		return hasPermission(context, requestCode);
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * deprecatedなので#requestPermissionを代わりに使う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccess(
		@NonNull final Activity activity,
		final int requestCode) {

		return requestPermission(activity, requestCode);
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * deprecatedなので#requestPermissionを代わりに使う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccess(
		@NonNull final FragmentActivity activity,
		final int requestCode) {

		return requestPermission(activity, requestCode);
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccess(
		@NonNull final android.app.Fragment fragment,
		final int requestCode) {

		final Uri uri = getStorageUri(fragment.getActivity(), requestCode);
		if (uri == null) {
			// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
			fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
		}
		return uri;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * deprecatedなので#requestPermissionを代わりに使う
	 * @param fragment
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccess(
		@NonNull final Fragment fragment,
		final int requestCode) {

		return requestPermission(fragment, requestCode);
	}

	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * deprecatedなので#takePersistableUriPermissionを代わりに使う
	 * @param context
	 * @param treeUri
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccessPermission(
		@NonNull final Context context,
		final int requestCode, final Uri treeUri) {

		return takePersistableUriPermission(context, requestCode, treeUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
	}

	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * deprecatedなので#takePersistableUriPermissionを代わりに使う
	 * @param context
	 * @param treeUri
	 * @param flags
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Nullable
	public static Uri requestStorageAccessPermission(
		@NonNull final Context context,
		final int requestCode, final Uri treeUri, final int flags) {

		return takePersistableUriPermission(context, requestCode, treeUri, flags);
	}

	/**
	 * 恒常的にアクセスできるように取得したパーミッションを開放する
	 * deprecatedなので#releasePersistableUriPermissionを代わりに使う
	 * @param context
	 * @param requestCode
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	public static void releaseStorageAccessPermission(
		@NonNull final Context context,
		final int requestCode) {

		releasePersistableUriPermission(context, requestCode);
	}

//--------------------------------------------------------------------------------
	/**
	 * requestCodeに対応するUriへアクセス可能かどうか
	 * @param context
	 * @param requestCode
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static boolean hasPermission(
		@NonNull final Context context,
		final int requestCode) {

		boolean found = false;
		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(requestCode));
			if (uri != null) {
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// requestCodeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
		return found;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Nullable
	public static Uri requestPermission(
		@NonNull final Activity activity,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, requestCode);
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 */
	@Nullable
	public static Uri requestPermission(
		@NonNull final FragmentActivity activity,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, requestCode);
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 * @throws UnsupportedOperationException
	 * @throws IllegalStateException
	 */
	@Nullable
	public static Uri requestPermission(
		@NonNull final Fragment fragment,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			@NonNull
			final Uri uri = getStorageUri(fragment.requireContext(), requestCode);
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * @param context
	 * @param treeUri
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@NonNull
	public static Uri takePersistableUriPermission(
		@NonNull final Context context,
		final int requestCode, @NonNull final Uri treeUri) {

		return takePersistableUriPermission(context,
			requestCode, treeUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
	}
	
	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * @param context
	 * @param treeUri
	 * @param flags
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@NonNull
	public static Uri takePersistableUriPermission(
		@NonNull final Context context,
		final int requestCode, @NonNull final Uri treeUri, final int flags) {

		if (BuildCheck.isLollipop()) {
			context.getContentResolver().takePersistableUriPermission(treeUri, flags);
			saveUri(context, getKey(requestCode), treeUri);
			return treeUri;
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}
	
	/**
	 * 恒常的にアクセスできるように取得したパーミッションを開放する
	 * @param context
	 * @param requestCode
	 * @throws UnsupportedOperationException
	 */
	public static void releasePersistableUriPermission(
		@NonNull final Context context,
		final int requestCode) {
		if (BuildCheck.isLollipop()) {
			final String key = getKey(requestCode);
			final Uri uri = loadUri(context, key);
			if (uri != null) {
				try {
					context.getContentResolver().releasePersistableUriPermission(uri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				} catch (final SecurityException e) {
					if (DEBUG) Log.w(TAG, e);
				}
				clearUri(context, key);
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したidに対応するUriが存在する時に対応するDocumentFileを返す
	 * @param context
	 * @param treeId
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorage(
		@NonNull final Context context,
		final int treeId) throws IOException {
		
		return getDir(context, treeId, null);
	}
	
	/**
	 * 指定したidに対応するUriが存在して書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param context
	 * @param treeId
	 * @param dirs スラッシュ(`/`)で区切られたパス文字列
	 * @return 一番下のディレクトリに対応するDocumentFile, Uriが存在しないときや書き込めない時はnull
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorage(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs) throws IOException {

		return getDir(context, treeId, dirs);
	}

	/**
	 * 指定したDocumentFileが書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param parent
	 * @param dirs
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorage(
		@NonNull final DocumentFile parent, @Nullable final String dirs)
			throws IOException {

		return getDir(parent, dirs);
	}

	/**
	 * 指定したidに対応するUriが存在して書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param context
	 * @param treeId
	 * @param dirs スラッシュ(`/`)で区切られたパス文字列
	 * @return 一番下のディレクトリに対応するDocumentFile, Uriが存在しないときや書き込めない時はnull
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static DocumentFile getDir(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs) throws IOException {

		if (BuildCheck.isLollipop()) {
			final Uri treeUri = getStorageUri(context, treeId);
			if (treeUri != null) {
				return getDir(DocumentFile.fromTreeUri(context, treeUri), dirs);
			} else {
				throw new FileNotFoundException("specific dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}
	
	/**
	 * 指定したDocumentFileが書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param parent
	 * @param dirs
	 * @return
	 * @throws IOException
	 */
	@NonNull
	public static DocumentFile getDir(
		@NonNull final DocumentFile parent, @Nullable final String dirs)
			throws IOException {
		
		DocumentFile tree = parent;
		if (!TextUtils.isEmpty(dirs)) {
			final String[] dir = dirs.split("/");
			for (final String d: dir) {
				if (!TextUtils.isEmpty(d)) {
					final DocumentFile t = tree.findFile(d);
					if ((t != null) && t.isDirectory()) {
						// 既に存在している時は何もしない
						tree = t;
					} else if (t == null) {
						if (tree.canWrite()) {
							// 存在しないときはディレクトリを生成
							tree = tree.createDirectory(d);
						} else {
							throw new IOException("can't create directory");
						}
					} else {
						throw new IOException("can't create directory, file with same name already exists");
					}
				}
			}
		}
		return tree;
	}

//--------------------------------------------------------------------------------
	/**
	 * DocumentFileのフィルター処理用インターフェース
	 */
	public interface FileFilter {
		public boolean accept(@NonNull final DocumentFile file);
	}

	/**
	 * 指定したディレクトリ配下に存在するファイルの一覧を取得
	 * @param dir
	 * @param filter nullなら存在するファイルを全て追加
	 * @return
	 */
	@NonNull
	public static List<DocumentFile> listFiles(
		@NonNull final DocumentFile dir,
		@Nullable final FileFilter filter) {

		final List<DocumentFile> result = new ArrayList<DocumentFile>();
		if (dir.isDirectory()) {
			final DocumentFile[] files = dir.listFiles();
			for (final DocumentFile file: files) {
				if ((filter == null) || (filter.accept(file))) {
					result.add(file);
				}
			}
		}
		return result;
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUriが存在する時に対応するファイルを参照するためのDocumentFileオブジェクトを生成する
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorageFile(
		@NonNull final Context context,
		final int treeId, final String mime, final String name) throws IOException {

		return getFile(context, treeId, null, mime, name);
	}

	/**
	 * 指定したUriが存在する時に対応するファイルを参照するためのDocumentFileオブジェクトを生成する
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorageFile(
		@NonNull final Context context,
		final int treeId,
		@Nullable final String dirs,
		@NonNull final String mime,
		@NonNull final String name) throws IOException {

		return getFile(context, treeId, dirs, mime, name);
	}

	/**
	 * 指定したDocumentFileの下にファイルを生成する
	 * dirsがnullまたは空文字列ならDocumentFile#createFileを呼ぶのと同じ
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getStorageFile(
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getFile(parent, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時に対応するファイルを参照するためのDocumentFileオブジェクトを生成する
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	@NonNull
	public static DocumentFile getFile(
		@NonNull final Context context,
		final int treeId,
		@Nullable final String dirs,
		@NonNull final String mime,
		@NonNull final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getDir(context, treeId, dirs);
			if (tree != null) {
				return getFile(tree, null, mime, name);
			} else {
				throw new IOException("specific dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}
	
	/**
	 * 指定したDocumentFileの下にファイルを生成する
	 * dirsがnullまたは空文字列ならDocumentFile#createFileを呼ぶのと同じ
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws IOException
	 */
	@NonNull
	public static DocumentFile getFile(
		@NonNull final DocumentFile parent,
		@Nullable final String dirs,
		@NonNull final String mime,
		@NonNull final String name) throws IOException {
		
		final DocumentFile tree = getDir(parent, dirs);
		final DocumentFile file = tree.findFile(name);
		if (file != null) {
			if (file.isFile()) {
				return file;
			} else {
				throw new IOException("directory with same name already exists");
			}
		} else {
			return tree.createFile(mime, name);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static OutputStream getStorageOutputStream(
		@NonNull final Context context,
		final int treeId,
		final String mime, final String name) throws IOException {
		
		return getOutputStream(context, treeId, null, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static OutputStream getStorageOutputStream(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getOutputStream(context, treeId, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static OutputStream getStorageOutputStream(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getOutputStream(context, parent, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@NonNull
	public static OutputStream getOutputStream(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getDir(context, treeId, dirs);
			if (tree != null) {
				return getOutputStream(context, tree, null, mime, name);
			} else {
				throw new FileNotFoundException("specific dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@NonNull
	public static OutputStream getOutputStream(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getDir(parent, dirs);
		final DocumentFile file = tree.findFile(name);
		if (file != null) {
			if (file.isFile()) {
				return context.getContentResolver().openOutputStream(
					file.getUri());
			} else {
				throw new IOException("directory with same name already exists");
			}
		} else {
			return context.getContentResolver().openOutputStream(
				tree.createFile(mime, name).getUri());
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUriが存在する時にその下に入力用ファイルを生成してInputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static InputStream getStorageInputStream(
		@NonNull final Context context,
		final int treeId,
		final String mime, final String name) throws IOException {
		
		return getInputStream(context, treeId, null, mime, name);
	}
	
	/**
	 * 指定したUriが存在する時にその下の入力用ファイルをInputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static InputStream getStorageInputStream(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getInputStream(context, treeId, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static InputStream getStorageInputStream(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getInputStream(context, parent, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下の入力用ファイルをInputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static InputStream getInputStream(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getDir(context, treeId, dirs);
			if (tree != null) {
				return getInputStream(context, tree, null, mime, name);
			} else {
				throw new FileNotFoundException("specifc dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}
	
	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static InputStream getInputStream(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getDir(parent, dirs);
		final DocumentFile file = tree.findFile(name);
		if (file != null) {
			if (file.isFile()) {
				return context.getContentResolver().openInputStream(
					file.getUri());
			} else {
				throw new IOException("directory with same name already exists");
			}
		} else {
			throw new FileNotFoundException("specific file not found");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUriが存在する時にその下に入力用ファイルを生成して入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static ParcelFileDescriptor getStorageFileFD(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getFd(context, treeId, dirs, mime, name);
	}

	/**
	 * 指定したDocumentFileの示すディレクトリが存在していれば入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static ParcelFileDescriptor getStorageFileFD(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		return getFd(context, parent, dirs, mime, name);
	}

	/**
	 * 指定したUriが存在する時にその下に入力用ファイルを生成して入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static ParcelFileDescriptor getFd(
		@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getDir(context, treeId, dirs);
			if (tree != null) {
				final DocumentFile file = tree.findFile(name);
				if (file != null) {
					if (file.isFile()) {
						return context.getContentResolver().openFileDescriptor(
							file.getUri(), "rw");
					} else {
						throw new IOException("directory with same name already exists");
					}
				} else {
					return context.getContentResolver().openFileDescriptor(
						tree.createFile(mime, name).getUri(), "rw");
				}
			} else {
				throw new FileNotFoundException("specific dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}
	
	/**
	 * 指定したDocumentFileの示すディレクトリが存在していれば入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@NonNull
	public static ParcelFileDescriptor getFd(
		@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getDir(parent, dirs);
		final DocumentFile file = tree.findFile(name);
		if (file != null) {
			if (file.isFile()) {
				return context.getContentResolver().openFileDescriptor(
					file.getUri(), "rw");
			} else {
				throw new IOException("directory with same name already exists");
			}
		} else {
			return context.getContentResolver().openFileDescriptor(
				tree.createFile(mime, name).getUri(), "rw");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したidに対応するUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param treeId
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 */
	@Deprecated
	@NonNull
	public static File createStorageDir(
		@NonNull final Context context, final int treeId)
		throws FileNotFoundException, UnsupportedOperationException {

		if (BuildCheck.isLollipop()) {
			final Uri treeUri = getStorageUri(context, treeId);
			if (treeUri != null) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final String path = UriHelper.getPath(context, saveTree.getUri());
				if (!TextUtils.isEmpty(path)) {
					return new File(path);
				} else {
					throw new FileNotFoundException("specific file not found");
				}
			} else {
				throw new FileNotFoundException("specific dir not found");
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に指定したFileを生成して返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param fileName
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static File createStorageFile(
		@NonNull final Context context,
		final int treeId, final String mime, final String fileName)
			throws IOException {

		//noinspection deprecation
		return createStorageFile(context, getStorageUri(context, treeId), mime, fileName);
	}

	/**
	 * 指定したUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param treeUri
	 * @param mime
	 * @param fileName
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static File createStorageFile(
		@NonNull final Context context,
		final Uri treeUri, final String mime, final String fileName)
			throws IOException {

		if (DEBUG) Log.v(TAG, "createStorageFile:" + fileName);

		if (BuildCheck.isLollipop()) {
			if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final DocumentFile target = saveTree.createFile(mime, fileName);
				final String path = UriHelper.getPath(context, target.getUri());
				if (!TextUtils.isEmpty(path)) {
					return new File(path);
				}
			}
			throw new FileNotFoundException();
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param fileName
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	public static int createStorageFileFD(
		@NonNull final Context context,
		final int treeId, final String mime, final String fileName)
			throws IOException {

		if (DEBUG) Log.v(TAG, "createStorageFileFD:" + fileName);
		//noinspection deprecation
		return createStorageFileFD(context, getStorageUri(context, treeId), mime, fileName);
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param treeUri
	 * @param mime
	 * @param fileName
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	public static int createStorageFileFD(
		@NonNull final Context context,
		final Uri treeUri, final String mime, final String fileName)
			throws IOException, UnsupportedOperationException {

		if (DEBUG) Log.v(TAG, "createStorageFileFD:" + fileName);
		if (BuildCheck.isLollipop()) {
			if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final DocumentFile target = saveTree.createFile(mime, fileName);
				try {
					final ParcelFileDescriptor fd
						= context.getContentResolver().openFileDescriptor(target.getUri(), "rw");
					return fd != null ? fd.getFd() : 0;
				} catch (final FileNotFoundException e) {
					Log.w(TAG, e);
				}
			}
			throw new FileNotFoundException();
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したDocumentFileが指し示すフォルダの下に指定した相対パスのディレクトリ階層を生成する
	 * フォルダが存在していない時に書き込み可能でなければIOExceptionを投げる
	 * deprecated #getDirを使うこと
	 * @param context
	 * @param baseDoc
	 * @param dirs
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getDocumentFile(
		@NonNull Context context,
		@NonNull final DocumentFile baseDoc, @Nullable final String dirs)
			throws IOException {
		
		DocumentFile tree = baseDoc;
		if (!TextUtils.isEmpty(dirs)) {
			final String[] dir = dirs.split("/");
			for (final String d: dir) {
				if (!TextUtils.isEmpty(d)) {
					final DocumentFile t = tree.findFile(d);
					if ((t != null) && t.isDirectory()) {
						// 既に存在している時は何もしない
						tree = t;
					} else if (t == null) {
						if (tree.canWrite()) {
							// 存在しないときはディレクトリを生成
							tree = tree.createDirectory(d);
						} else {
							throw new IOException("can't create directory");
						}
					} else {
						throw new IOException("can't create directory, file with same name already exists");
					}
				}
			}
		}
		return tree;
	}
	
	/**
	 * 指定したUriがDocumentFileの下に存在するフォルダを指し示していれば
	 * 対応するDocumentFileを取得して返す
	 * フォルダが存在していない時に書き込み可能でなければIOExceptionを投げる
	 * @param context
	 * @param baseDoc
	 * @param uri
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@NonNull
	public static DocumentFile getDocumentFile(
		@NonNull Context context,
		@NonNull final DocumentFile baseDoc, @NonNull final Uri uri)
			throws IOException {
		
		final String basePathString = UriHelper.getPath(context, baseDoc.getUri());
		final String uriString = UriHelper.getPath(context, uri);
		if (!TextUtils.isEmpty(basePathString)
			&& !TextUtils.isEmpty(uriString)
			&& uriString.startsWith(basePathString)) {

			return getDir(baseDoc,
				uriString.substring(basePathString.length()));
		}
		throw new FileNotFoundException();
	}

//--------------------------------------------------------------------------------
	/**
	 * パーミッションを保持しているリクエストコードとUriのペアを保持するMapを取得する
	 * @param context
	 * @return
	 */
	@NonNull
	public static Map<Integer, Uri> getStorageUriAll(@NonNull final Context context) {
		final Map<Integer, Uri> result = new HashMap<>();
		final SharedPreferences pref
			= context.getSharedPreferences(context.getPackageName(), 0);
		final Map<String, ?> values = pref.getAll();
		for (final String key: values.keySet()) {
			if (key.startsWith(KEY_PREFIX)) {
				final Object value = values.get(key);
				if (value instanceof String) {
					try {
						final int requestCode = Integer.parseInt(key.substring(KEY_PREFIX.length()));
						final Uri uri = Uri.parse((String)value);
						result.put(requestCode, uri);
					} catch (final NumberFormatException e) {
						Log.d(TAG, "getStorageUriAll:unexpected key format," + key + ",value=" + value);
					}
				} else {
					Log.d(TAG, "getStorageUriAll:unexpected key-value pair,key=" + key + ",value=" + value);
				}
			}
		}
		return result;
	}


	private static final String KEY_PREFIX = "SDUtils-";

	/**
	 * uriを保存する際に使用する共有プレファレンスのキー名を要求コードから生成する
	 * "SDUtils-${要求コード}"を返す
	 * @param requestCode
	 * @return
	 */
	@NonNull
	private static String getKey(final int requestCode) {
		return String.format(Locale.US, KEY_PREFIX + "%d", requestCode);	// XXX ここは互換性維持のためにSDUtilsの名を残す
	}

	/**
	 * uriを共有プレファレンスに保存する
	 * @param context
	 * @param key
	 * @param uri
	 */
	private static void saveUri(
		@NonNull final Context context,
		@NonNull final String key, @NonNull final Uri uri) {

		final SharedPreferences pref
			= context.getSharedPreferences(context.getPackageName(), 0);
		if (pref != null) {
			pref.edit().putString(key, uri.toString()).apply();
		}
	}

	/**
	 * 共有プレファレンスに保存しているuriを取得する
	 * @param context
	 * @param key
	 * @return
	 */
	@Nullable
	private static Uri loadUri(
		@NonNull final Context context, @NonNull final String key) {

		Uri result = null;
		final SharedPreferences pref
			= context.getSharedPreferences(context.getPackageName(), 0);
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
	private static void clearUri(
		@NonNull final Context context, @Nullable final String key) {

		final SharedPreferences pref
			= context.getSharedPreferences(context.getPackageName(), 0);
		if ((pref != null) && pref.contains(key)) {
			try {
				pref.edit().remove(key).apply();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * requestCodeに対応するUriが存在していて恒常的パーミッションがあればそれを返す, なければnullを返す
	 * @param context
	 * @param requestCode
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@Nullable
	private static Uri getStorageUri(
		@NonNull final Context context,
		final int requestCode) throws UnsupportedOperationException {

		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(requestCode));
			if (uri != null) {
				boolean found = false;
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// requestCodeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
				if (found) {
					return uri;
				}
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
		return null;
	}

	/**
	 * requestStorageAccessの下請け
	 * @return
	 */
	private static Intent prepareStorageAccessPermission() {
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	}
}
