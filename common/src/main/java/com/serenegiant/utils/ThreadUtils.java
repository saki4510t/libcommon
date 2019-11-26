package com.serenegiant.utils;

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
