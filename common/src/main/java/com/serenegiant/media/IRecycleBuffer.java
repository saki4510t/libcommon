package com.serenegiant.media;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 再利用可能バッファインターフェース
 */
public interface IRecycleBuffer {
	/**
	 * このオブジェクトをプールへ戻す
	 * 対応していなければUnsupportedOperationExceptionを投げる。
	 * 対応していないオブジェクトは自前でプールオブジェクトへ返さないといけない
	 */
	public void recycle();
	
	public interface Factory {
		public IRecycleBuffer create(
			@NonNull final Object parent, @Nullable final Object... objects);
	}
}
