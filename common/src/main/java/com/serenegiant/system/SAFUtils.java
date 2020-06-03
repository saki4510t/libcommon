package com.serenegiant.system;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Storage Access Framework/DocumentFile関係のヘルパークラス
 */
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
		public boolean onResult(
			final int requestCode,
			@NonNull final Uri uri, @NonNull final Intent data);

		public void onFailed(
			final int requestCode, @Nullable final Intent data);
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
	 * @param context
	 * @param requestCode
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static boolean hasPermission(
		@NonNull final Context context,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(requestCode));
			if (uri != null) {
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();
				return hasPermission(list, uri);
			}
		} else {
			throw new UnsupportedOperationException("should be API>=21");
		}
		return false;
	}

	public static boolean hasPermission(
		@NonNull final List<UriPermission> persistedUriPermissions,
		@NonNull final Uri uri) {

		for (final UriPermission item: persistedUriPermissions) {
			if (item.getUri().equals(uri)) {
				return true;
			}
		}
		return false;
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
					if ("..".equals(d)) {
						tree = tree.getParentFile();
						if (tree == null) {
							throw new IOException("failed to get parent directory");
						}
					} else if (!".".equals(d)) {
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
		final List<String> removes = new ArrayList<>();
		final List<UriPermission> list
			= context.getContentResolver().getPersistedUriPermissions();
		for (final String key: values.keySet()) {
			if (key.startsWith(KEY_PREFIX)) {
				final Object value = values.get(key);
				if (value instanceof String) {
					try {
						final int requestCode = Integer.parseInt(key.substring(KEY_PREFIX.length()));
						final Uri uri = Uri.parse((String)value);
						if (hasPermission(list, uri)) {
							result.put(requestCode, uri);
						} else {
							removes.add(key);
						}
					} catch (final NumberFormatException e) {
						Log.d(TAG, "getStorageUriAll:unexpected key format," + key + ",value=" + value);
					}
				} else {
					Log.d(TAG, "getStorageUriAll:unexpected key-value pair,key=" + key + ",value=" + value);
				}
			}
		}
		if (!removes.isEmpty()) {
			for (final String key: removes) {
				clearUri(context, key);
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
	 * ドキュメントツリーへのアクセスのためのIntentを返す
	 * @return
	 */
	private static Intent prepareStorageAccessPermission() {
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	}
}
