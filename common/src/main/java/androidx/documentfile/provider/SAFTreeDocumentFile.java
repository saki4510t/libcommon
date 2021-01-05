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

import com.serenegiant.system.SAFUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SAFを使って取得したuriパーミッションに対応したTreeDocumentFile実装
 * #deleteを使って削除するとuriパーミッション自体も削除する
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SAFTreeDocumentFile extends TreeDocumentFile {

	@NonNull
	private final Context mContext;
	private final int mRequestCode;

	/**
	 * コンストラクタ
	 * @param parent
	 * @param context
	 * @param uri
	 * @param requestCode
	 */
	SAFTreeDocumentFile(
		@Nullable final DocumentFile parent,
		@NonNull final Context context,
		@NonNull final Uri uri,
		final int requestCode) {

		super(parent, context, uri);
		mContext = context;
		mRequestCode = requestCode;
	}

	@Override
	public boolean delete() {
		final boolean result = super.delete();
		if (result) {
			SAFUtils.releasePersistableUriPermission(mContext, mRequestCode);
		}
		return result;
	}

}
