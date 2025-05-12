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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.FileUtils;

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
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

/**
 * Storage Access Framework/DocumentFile関係のヘルパークラス
 */
public class SAFUtils {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = SAFUtils.class.getSimpleName();

	private SAFUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したドキュメントツリーIDに対応するUriへアクセス可能かどうかを取得
	 * @param context
	 * @param treeId
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static boolean hasPermission(
		@NonNull final Context context,
		final int treeId) {

		if (BuildCheck.isKitKat()) {
			final Uri uri = loadUri(context, getKey(treeId));
			if (uri != null) {
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();	// API>=19
				return hasPermission(list, uri);
			}
		} else {
			throw new UnsupportedOperationException("should be API>=19");
		}
		return false;
	}

	public static boolean hasPermission(
		@NonNull final List<UriPermission> persistedUriPermissions,
		@NonNull final Uri uri) {

		for (final UriPermission item: persistedUriPermissions) {
			if (item.getUri().equals(uri)) {	// UriPermission#getUri API>=19
				return true;
			}
		}
		return false;
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
		final int treeId, @NonNull final Uri treeUri) {

		return takePersistableUriPermission(context,
			treeId, treeUri,
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
		final int treeId, @NonNull final Uri treeUri, final int flags) {

		if (BuildCheck.isLollipop()) {
			context.getContentResolver().takePersistableUriPermission(treeUri, flags);
			saveUri(context, getKey(treeId), treeUri);
			return treeUri;
		} else {
			throw new UnsupportedOperationException("should be API>=19");
		}
	}
	
	/**
	 * 恒常的にアクセスできるように取得したパーミッションを開放する
	 * @param context
	 * @param treeId
	 * @throws UnsupportedOperationException
	 */
	public static void releasePersistableUriPermission(
		@NonNull final Context context,
		final int treeId) {
		final String key = getKey(treeId);
		final Uri uri = loadUri(context, key);
		if (uri != null) {
			try {
				context.getContentResolver().releasePersistableUriPermission(uri,	// API>=19
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			} catch (final SecurityException e) {
				if (DEBUG) Log.w(TAG, e);
			}
			clearUri(context, key);
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
		if (DEBUG) Log.v(TAG, "getFile:parent=" + parent.getUri() + ",tree=" + tree.getUri() + ",name=" + name);
		final DocumentFile file = tree.findFile(name);
		if (file != null) {
			if (file.isFile()) {
				return file;
			} else {
				throw new IOException("directory with same name already exists");
			}
		} else {
			return createFile(tree, mime, name);
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
				createFile(tree, mime, name).getUri());
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
						createFile(tree, mime, name).getUri(), "rw");
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
				createFile(tree, mime, name).getUri(), "rw");
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
						final int treeId = Integer.parseInt(key.substring(KEY_PREFIX.length()));
						final Uri uri = Uri.parse((String)value);
						if (hasPermission(list, uri)) {
							result.put(treeId, uri);
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
	 * uriを保存する際に使用する共有プレファレンスのキー名をドキュメントツリーIDから生成する
	 * "SDUtils-${要求コード}"を返す
	 * @param treeId
	 * @return
	 */
	@NonNull
	private static String getKey(final int treeId) {
		return String.format(Locale.US, KEY_PREFIX + "%d", treeId);	// XXX ここは互換性維持のためにSDUtilsの名を残す
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
	 * 指定したドキュメントツリーIDに対応するUriが存在していて恒常的パーミッションがあればそれを返す, なければnullを返す
	 * @param context
	 * @param treeId
	 * @return
	 * @throws UnsupportedOperationException
	 */
	@Nullable
	public static Uri getStorageUri(
		@NonNull final Context context,
		final int treeId) throws UnsupportedOperationException {

		final Uri uri = loadUri(context, getKey(treeId));
		if (uri != null) {
			boolean found = false;
			// 恒常的に保持しているUriパーミッションの一覧を取得する
			final List<UriPermission> list
				= context.getContentResolver().getPersistedUriPermissions();	// API>=19
			for (final UriPermission item: list) {
				if (item.getUri().equals(uri)) {	// API>=19
					// 指定したドキュメントツリーIDに対応するUriへのパーミッションを恒常的に保持していた時
					found = true;
					break;
				}
			}
			if (found) {
				return uri;
			}
		}
		return null;
	}

	/**
	 * requestStorageAccessの下請け
	 * ドキュメントツリーへのアクセスのためのIntentを返す
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private static Intent prepareStorageAccessPermission() {
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);	// API>=21
	}

	/**
	 * DocumentFileから指定したmime/nameを持つDocumentFileを生成する
	 * nameに拡張子が指定されている場合にはmimeのサブタイプを拡張子に置き換えて、
	 * nameから拡張子を除いたものをdisplayNameとしてDocumentFileを生成する
	 * @param tree
	 * @param mime
	 * @param name
	 * @return
	 */
	private static DocumentFile createFile(
		@NonNull final DocumentFile tree,
		@NonNull final String mime, @NonNull final String name) {

		// XXX mimeがvideo/mp4の様にサブタイプまで指定されている場合にnameが拡張子だとmimeに応じた拡張子が付加されて拡張子が二重になってしまうので対策
		if (DEBUG) Log.v(TAG, "createFile:mine=" + mime + ",name=" + name);
		final String ext = FileUtils.getExt(name);
		if (TextUtils.isEmpty(ext)) {
			// 拡張子がなければそのままDocumentFile#createFileを呼び出す
			return tree.createFile(mime, name);
		} else {
			if (DEBUG) Log.v(TAG, "createFile:拡張子が指定されているのでmimeのサブタイプとして拡張子を使う,"
				+ "ext=" + ext + ",displayName=" + FileUtils.removeFileExtension(name));
			// 拡張子付きの場合はmimeのサブタイプを拡張子に置き換えて拡張子を除いた名前でDocumentFile#createFileを呼び出す
			final String[] types = mime.split("/");
			return tree.createFile(types[0] + "/" + ext, FileUtils.removeFileExtension(name));
		}
	}
}
