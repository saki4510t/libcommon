package com.serenegiant.utils;
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

public class ThreadUtils {

	private ThreadUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * InterruptedExceptionを投げずにSleepする
	 * @param  millis
	 *         the length of time to sleep in milliseconds
	 * @throws  IllegalArgumentException
	 *          if the value of {@code millis} is negative, or the value of
	 *          {@code nanos} is not in the range {@code 0-999999}
	 */
	public static void NoThrowSleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			// ignore
		}
	}

	/**
	 * InterruptedExceptionを投げずにSleepする
	 * @param  millis
	 *         the length of time to sleep in milliseconds
	 * @param  nanos
	 *         {@code 0-999999} additional nanoseconds to sleep
	 * @throws  IllegalArgumentException
	 *          if the value of {@code millis} is negative, or the value of
	 *          {@code nanos} is not in the range {@code 0-999999}
	 */
	public static void NoThrowSleep(final long millis, final int nanos) {
		try {
			Thread.sleep(millis, nanos);
		} catch (final InterruptedException e) {
			// ignore
		}
	}
}
