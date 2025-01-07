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

public interface DecoderListener {
	/**
	 * デコード開始
	 * @param decoder
	 */
	public void onStartDecode(@NonNull final Decoder decoder);
	/**
	 * エンコード終了
	 * @param decoder
	 */
	public void onStopDecode(@NonNull final Decoder decoder);
	/**
	 * Decoder破棄された時のコールバック
	  */
	public void onDestroy(@NonNull final Decoder decoder);
	/**
	 * エラーが発生した時のコールバック
	 * @param t
	 */
	public void onError(@NonNull final Throwable t);
}
