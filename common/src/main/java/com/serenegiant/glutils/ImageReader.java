package com.serenegiant.glutils;
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

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 映像受け取り用インターフェース
 * @param <T>
 */
public interface ImageReader<T> {
	/**
	 * 映像を取得可能になったときに呼ばれるコールバックリスナー
	 * @param <T>
	 */
	public interface OnImageAvailableListener<T> {
		public void onImageAvailable(@NonNull final ImageReader<T> reader);
	}

	/**
	 * 読み取った映像データの準備ができたときのコールバックリスナーを登録
	 * @param listener
	 * @param handler
	 * @throws IllegalArgumentException
	 */
	public void setOnImageAvailableListener(
		@Nullable final OnImageAvailableListener<T> listener,
		@Nullable final Handler handler) throws IllegalArgumentException;

	/**
	 * 最新の映像を取得する。最新以外の古い映像は全てrecycleされる。
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像が準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public T acquireLatestImage() throws IllegalStateException;
	/**
	 * 次の映像を取得する
	 * コンストラクタで指定した同時取得可能な最大の映像数を超えて取得しようとするとIllegalStateExceptionを投げる
	 * 映像がが準備できていなければnullを返す
	 * null以外が返ったときは#recycleで返却して再利用可能にすること
	 * @return
	 * @throws IllegalStateException
	 */
	@Nullable
	public T acquireNextImage() throws IllegalStateException;
	/**
	 * 使った映像を返却して再利用可能にする
	 * @param image
	 */
	public void recycle(@NonNull final T image);
	/**
	 * ImageReaderの有効無効をセットする
	 * enabled=falseなら映像の受け取り処理を行わない
	 * @param enabled
	 */
	public void setEnabled(final Boolean enabled);
}
