package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("deprecation")
public interface EncoderListener2 extends EncoderListener {
	/**
	 * エンコード開始
	 * @param encoder
	 * @param source
	 * @param mayFail
	 */
	public void onStartEncode(@NonNull final Encoder encoder,
		@Nullable final Surface source, final boolean mayFail);
	/**
	 * エンコード終了
	 * @param encoder
	 */
	public void onStopEncode(@NonNull final Encoder encoder);
	/**
	 * Encoderが破棄された時のコールバック
	  */
	public void onDestroy(@NonNull final Encoder encoder);
	/**
	 * エラーが発生した時のコールバック
	 * @param e
	 */
	public void onError(@NonNull final Throwable e);

	/**
	 * すでにEncoderListener2であればそのまま返す
	 * EncoderListenerであればEncoderListener2にラップして返す
	 * @param listener
	 * @return
	 */
	@NonNull
	public static EncoderListener2 wrap(@NonNull final EncoderListener listener) {
		if (listener instanceof EncoderListener2) {
			return (EncoderListener2) listener;
		} else {
			return new EncoderListener2() {
				@Override
				public void onStartEncode(@NonNull final Encoder encoder, @Nullable final Surface source, final boolean mayFail) {
					listener.onStartEncode(encoder, source, 0, mayFail);
				}

				@Override
				public void onStopEncode(@NonNull final Encoder encoder) {
					listener.onStopEncode(encoder);
				}

				@Override
				public void onDestroy(@NonNull final Encoder encoder) {
					listener.onDestroy(encoder);
				}

				@Override
				public void onError(@NonNull final Throwable t) {
					listener.onError(t);
				}
			};
		}
	}
}
