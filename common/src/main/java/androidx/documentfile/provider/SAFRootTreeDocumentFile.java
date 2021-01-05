package androidx.documentfile.provider;
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
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.SAFUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * アプリがSAFを使って保持しているuriパーミッションの一覧を
 * 仮想的にディレクトリとしてアクセスできるようにするためのDocumentFile実装
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SAFRootTreeDocumentFile extends DocumentFile {

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param context
	 * @return
	 */
	public static DocumentFile fromContext(@NonNull final Context context) {
		return new SAFRootTreeDocumentFile(context);
	}

	@NonNull
	private final Context mContext;

	/**
	 * コンストラクタ
	 * パッケージローカルにする必要はないけど他のDocumentFileクラスにならって
	 * @param context
	 */
	SAFRootTreeDocumentFile(@NonNull final Context context) {
		super(null);
		mContext = context;
	}

	@Nullable
	@Override
	public DocumentFile createFile(@NonNull final String mimeType, @NonNull final String displayName) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	public DocumentFile createDirectory(@NonNull final String displayName) {
		throw new UnsupportedOperationException();
	}

	@NonNull
	@Override
	public Uri getUri() {
		return Uri.parse("content://com.serenegiant.SAFRootTreeDocumentFile");
	}

	@Nullable
	@Override
	public String getName() {
		return "SAFRootDir";
	}

	@Nullable
	@Override
	public String getType() {
		return null;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isVirtual() {
		return true;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@NonNull
	@Override
	public DocumentFile[] listFiles() {
		@NonNull
		final List<DocumentFile> result = new ArrayList<>();
		if (BuildCheck.isLollipop()) {
			@NonNull
			final Map<Integer, Uri> list = SAFUtils.getStorageUriAll(mContext);
			for (final Map.Entry<Integer, Uri> item: list.entrySet()) {
				result.add(new SAFTreeDocumentFile(this, mContext,
					DocumentsContract.buildDocumentUriUsingTree(
						item.getValue(),
						DocumentsContract.getTreeDocumentId(item.getValue())),
					item.getKey())
				);
			}
		}
		return result.toArray(new DocumentFile[0]);
	}

	@Override
	public boolean renameTo(@NonNull final String displayName) {
		return false;
	}

}
