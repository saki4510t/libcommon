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

import java.nio.ByteBuffer;

public interface Encoder {
	/**
	 * エンコーダーを準備
	 * @throws Exception
	 */
	public void prepare()  throws Exception;

	/**
	 * エンコード開始
	 */
	public void start();;

	/**
	 * エンコード終了
	 */
	public void stop();

	/**
	 * エンコードを破棄
	 */
	public void release();

	/**
	 * エンコード終了指示
	 */
	public void signalEndOfInputStream();

	/**
	 * バイト配列をエンコードする場合
	 * @param buffer
	 * @param presentationTimeUs [マイクロ秒]
	 */
	public void encode(final ByteBuffer buffer, final long presentationTimeUs);

	/**
	 * エンコーダーへデータが入力されたことを通知
	 */
	public void frameAvailableSoon();

	/**
	 * エンコード中かどうかを取得
	 * @return
	 */
	public boolean isEncoding();
}
