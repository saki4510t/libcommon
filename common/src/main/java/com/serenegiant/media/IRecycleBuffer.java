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

	/**
	 * リサイクルしたかどうかを取得
	 * @return
	 */
	public boolean isRecycled();

	/**
	 * IRecycleBuffer生成用ファクトリーインターフェース
	 */
	public interface Factory<T extends IRecycleBuffer> {
		/**
		 * IRecycleBufferオブジェクトを生成する
		 * @param parent
		 * @param args IRecycleBufferの生成に必要なオプション引数, 省略可
		 *        正数を指定した場合はバッファのサイズ、ByteOrderを指定するとバイトオーダーが指定されたものになる
		 * @return
		 */
		@NonNull
		public T create(
			@NonNull final IRecycleParent<T> parent, @Nullable final Object... args);
	}
}
