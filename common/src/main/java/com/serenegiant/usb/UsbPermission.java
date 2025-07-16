package com.serenegiant.usb;
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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.serenegiant.app.PendingIntentCompat;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;
import com.serenegiant.utils.ThreadPool;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

/**
 * USBMonitorからUSB機器アクセスパーミッション要求関係の処理を分離
 * XXX このクラスを単独で使う場合はregister/unregisterを呼び出してパーミッション要求結果の
 *     Intentを受け取るためのBroadcastReceiverを登録/登録解除する必要がある。
 *     USBMonitor等他のクラスで共通のBroadcastReceiverでIntentを受け取って
 *     処理する場合にはregister/unregisterを呼び出さずにIntent受信時に
 *     #onReceiveを呼び出すか、このクラスインスタンス自体をBroadcastReceiverとして
 *     Context.register/unregisterへ引き渡す
 */
public class UsbPermission extends BroadcastReceiver {
	private static final boolean DEBUG = false;	// XXX 実働時にはfalseにすること
	private static final String TAG = UsbPermission.class.getSimpleName();

	public static final String ACTION_USB_PERMISSION = "com.serenegiant.USB_PERMISSION";

	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface Callback {
		/**
		 * パーミッション要求結果が返ってきた時
		 * @param device
		 */
		@AnyThread
		public void onPermission(@NonNull final UsbDevice device);
		/**
		 * キャンセルまたはユーザーからパーミッションを得られなかった時
		 * @param device
		 */
		@AnyThread
		public void onCancel(@NonNull final UsbDevice device);
		/**
		 * パーミッション要求時等で非同期実行中にエラーになった時
		 * @param device
		 * @param t
		 */
		@AnyThread
		public void onError(@Nullable final UsbDevice device, @NonNull final Throwable t);
	}

	/**
	 * デフォルトのCallback実装
	 */
	public static Callback DEFAULT_CALLBACK = new Callback() {
		@Override
		public void onPermission(@NonNull final UsbDevice device) {
		}

		@Override
		public void onCancel(@NonNull final UsbDevice device) {
		}

		@Override
		public void onError(@Nullable final UsbDevice device, @NonNull final Throwable t) {
		}
	};

	//--------------------------------------------------------------------------------
	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final UsbManager mUsbManager;
	@NonNull
	private final Callback mCallback;
	@NonNull
	private PendingIntent mPermissionIntent;
	private final boolean mOwnHandler;
	@NonNull
	private final Handler mAsyncHandler;

	private volatile boolean mReleased;
	private boolean mRegistered;

	/**
	 * コンストラクタ
	 * @param context XXX Activity以外のコンテキストからUSBパーミッション要求等のシステムダイアログ/システムUIが
	 *                    表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合があるので
	 *                    Contextとしているけど可能な限りActivityを引き渡すこと。
	 *                    (パーミッション要求しなければアプリケーションコンテキストやサービスコンテキストでもOK)
	 * @param callback
	 */
	public UsbPermission(
		@NonNull final Context context,
		@NonNull final Callback callback) {

		this(context, callback, null);
	}

	/**
	 * コンストラクタ
	 * @param context XXX Activity以外のコンテキストからUSBパーミッション要求等のシステムダイアログ/システムUIが
	 *                    表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合があるので
	 *                    Contextとしているけど可能な限りActivityを引き渡すこと。
	 *                    (パーミッション要求しなければアプリケーションコンテキストやサービスコンテキストでもOK)
	 * @param callback
	 * @param asyncHandler
	 */
	public UsbPermission(
		@NonNull final Context context,
		@NonNull final Callback callback,
		@Nullable final Handler asyncHandler) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = ContextUtils.requireSystemService(context, UsbManager.class);
		mCallback = callback;
		mPermissionIntent = createIntent(context);
		mOwnHandler = (asyncHandler == null);
		if (mOwnHandler) {
			mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		} else {
			mAsyncHandler = asyncHandler;
		}
	}

	/**
	 * 関係するリソースを破棄する。再利用はできない
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		unregister();
		mReleased = true;
		if (mOwnHandler) {
			HandlerUtils.NoThrowQuit(mAsyncHandler);
		}
		mWeakContext.clear();
	}

	/**
	 * すでに破棄されたかどうかを取得
	 * @return
	 */
	public boolean isReleased() {
		return mReleased;
	}

	public boolean isRegistered() {
		return mRegistered;
	}

	/**
	 * USB機器アクセスパーミッション要求時のインテントを処理するためのブロードキャストレシーバーを登録する
	 * @throws IllegalStateException
	 */
	public synchronized void register() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "register:" + mRegistered);
		if (!mRegistered) {
			final Context context = requireContext();
			final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED);
			mRegistered = true;
		}
	}

	/**
	 * USB機器アクセスパーミッション要求時のインテントを処理するためのブロードキャストレシーバーを登録を解除する
	 * @throws IllegalStateException
	 */
	public synchronized void unregister() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "unregister:" + mRegistered);
		cancelRequestPermission();
		if (mRegistered) {
			mRegistered = false;
			final Context context = requireContext();
			context.unregisterReceiver(this);
		}
	}

	/**
	 * パーミッションを要求する
	 * @param device
	 * @return パーミッション要求が失敗したらtrueを返す
	 * @throws IllegalStateException
	 */
	public synchronized boolean requestPermission(@Nullable final UsbDevice device)
		throws IllegalStateException {

		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device);
		boolean result = false;
		if (!isReleased()) {
			if (device != null) {
				if (mUsbManager.hasPermission(device)) {
					// 既にパーミッションが有れば接続する
					processPermission(device);
				} else {
					try {
						// パーミッションがなければ要求する
						mUsbManager.requestPermission(device, mPermissionIntent);
					} catch (final Exception e) {
						// Android5.1.xのGALAXY系でandroid.permission.sec.MDM_APP_MGMT
						// という意味不明の例外生成するみたい
						Log.w(TAG, e);
						processCancel(device);
						result = true;
					}
				}
			} else {
				callOnError(device, new UsbPermissionException("device is null"));
				result = true;
			}
		} else {
			throw new IllegalStateException("already destroyed");
		}
		return result;
	}

	/**
	 * 要求中のUSB機器アクセスパーミッション要求をキャンセルする
	 * XXX requestPermissionへ渡したPendingIntentをキャンセルしても
	 *     パーミッション要求自体はキャンセルされずダイアログが表示された
	 *     ままになるので効果ない(´･ω･`)
	 */
	public synchronized void cancelRequestPermission() {
		if (DEBUG) Log.v(TAG, "cancelRRequestPermission:");
		mPermissionIntent.cancel();
		mPermissionIntent = createIntent(requireContext());
	}

	/**
	 * BroadcastReceiverの抽象メソッドの実装
	 * @param context The Context in which the receiver is running.
	 * @param intent The Intent being received.
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (DEBUG) Log.v(TAG, "onReceive:" + intent);
		final String action = intent.getAction();
		if (ACTION_USB_PERMISSION.equals(action)) {
			// パーミッション要求の結果が返ってきた時
			synchronized (this) {
				final UsbDevice device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice.class);
				if ((device != null)
					&& (hasPermission(mUsbManager, device)
					|| intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) ) {
					// パーミッションを取得できた時・・・デバイスとの通信の準備をする
					processPermission(device);
				} else if (device != null) {
					// パーミッションを取得できなかった時
					processCancel(device);
				} else {
					// パーミッションを取得できなかった時,
					// OS側がおかしいかAPI>=31でPendingIntentにFLAG_MUTABLEを指定していないとき
					callOnError(device, new UsbPermissionException("device is null"));
				}
			}
		}
	}

//--------------------------------------------------------------------------------
	private Context getContext() {
		return mWeakContext.get();
	}

	private Context requireContext() throws IllegalStateException {
		final Context result = getContext();
		if (mReleased || (result == null)) {
			throw new IllegalStateException("already released!");
		}
		return result;
	}

	/**
	 * パーミッション要求結果が返ってきた時の処理
	 * @param device
	 */
	private void processPermission(@NonNull final UsbDevice device) {
		if (DEBUG) Log.v(TAG, "processPermission:");
		mCallback.onPermission(device);
	}

	/**
	 * ユーザーキャンセル等でパーミッションを取得できなかったときの処理
	 * @param device
	 */
	private void processCancel(@NonNull final UsbDevice device) {
		if (DEBUG) Log.v(TAG, "processCancel:");
		if (!mReleased) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onCancel(device);
				}
			});
		}
	}

	/**
	 * エラーコールバック呼び出し処理
	 * @param device
	 * @param t
	 */
	private void callOnError(
		@Nullable final UsbDevice device,
		@NonNull final Throwable t) {

		if (DEBUG) Log.v(TAG, "callOnError:" + t);
		if (!mReleased) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onError(device, t);
				}
			});
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * USB機器アクセスパーミッション要求時に結果を受け取るためのPendingIntentを生成する
	 * @param context
	 * @return
	 */
	@SuppressLint({"WrongConstant"})
	private static PendingIntent createIntent(@NonNull final Context context) {
		int flags = 0;
		if (BuildCheck.isAPI31()) {
			// FLAG_MUTABLE指定必須
			// FLAG_IMMUTABLEだとOS側から返ってくるIntentでdeviceがnullになってしまう
			flags |= PendingIntentCompat.FLAG_MUTABLE;
		}
		final Intent intent = new Intent(ACTION_USB_PERMISSION);
		intent.setPackage(context.getPackageName());
		return PendingIntent.getBroadcast(context, 0, intent, flags);
	}

	/**
	 * 指定したUsbDeviceが示すUSB機器へのアクセスパーミッションがあるかどうかを取得
	 * @param context
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 */
	public static boolean hasPermission(
		@NonNull final Context context,
		@Nullable final UsbDevice device) {

		return hasPermission(ContextUtils.requireSystemService(context, UsbManager.class), device);
	}

	/**
	 * 指定したUsbDeviceが示すUSB機器へのアクセスパーミッションがあるかどうかを取得
	 * @param manager
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 */
	public static boolean hasPermission(
		@NonNull final UsbManager manager,
		@Nullable final UsbDevice device) {

		return (device != null) && manager.hasPermission(device);
	}

	/**
	 * パーミッションを要求する
	 * activityClassはアプリのデフォルトActivityを使う
	 * コールバックはしない(内部的にはDEFAULT_CALLBACKを使う)ので
	 * 結果の取得が必要であればグローバルブロードキャストを受け取ること
	 * @param context XXX Activity以外のコンテキストからUSBパーミッション要求等のシステムダイアログ/システムUIが
	 *                    表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合があるので
	 *                    Contextとしているけど可能な限りActivityを引き渡すこと。
	 *                    (パーミッション要求しなければアプリケーションコンテキストやサービスコンテキストでもOK)
	 * @param device
	 * @throws IllegalStateException
	 */
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device)
		throws IllegalArgumentException {

		requestPermission(context, device, DEFAULT_CALLBACK, 0);
	}

	/**
	 * パーミッションを要求する
	 * @param context XXX Activity以外のコンテキストからUSBパーミッション要求等のシステムダイアログ/システムUIが
	 *                    表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合があるので
	 *                    Contextとしているけど可能な限りActivityを引き渡すこと。
	 *                    (パーミッション要求しなければアプリケーションコンテキストやサービスコンテキストでもOK)
	 * @param device
	 * @param callback
	 * @throws IllegalStateException
	 */
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device,
		@NonNull final Callback callback) {

		requestPermission(context, device, callback, 0);
	}

	/**
	 * パーミッションを要求する
	 * @param context XXX Activity以外のコンテキストからUSBパーミッション要求等のシステムダイアログ/システムUIが
	 *                    表示される処理を要求すると、ダイアログ等がアプリの背面に回ってしまう場合があるので
	 *                    Contextとしているけど可能な限りActivityを引き渡すこと。
	 *                    (パーミッション要求しなければアプリケーションコンテキストやサービスコンテキストでもOK)
	 * @param device
	 * @param callback
	 * @param timeoutMs 最大待ち時間[ミリ秒] 0以下なら無限待ち
	 * @throws IllegalStateException
	 */
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device,
		@NonNull final Callback callback,
		final long timeoutMs)
		throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device.getDeviceName() + ",callback=" + callback);
		final UsbManager manager = ContextUtils.requireSystemService(context, UsbManager.class);
		ThreadPool.queueEvent(() -> {
			final CountDownLatch latch = new CountDownLatch(1);
			// USBMonitorインスタンスにセットしているコールバックも呼び出されるようにするために
			// パーミッションがあってもなくてもパーミッション要求する
			final BroadcastReceiver receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(final Context context, final Intent intent) {
					final String action = intent.getAction();
					try {
						if (ACTION_USB_PERMISSION.equals(action)) {
							// パーミッション要求の結果が返ってきた時
							final UsbDevice device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice.class);
							if ((device != null)
								&& (manager.hasPermission(device)
								|| intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) ) {
								// パーミッションを取得できた時・・・デバイスとの通信の準備をする
								callback.onPermission(device);
							} else if (device != null) {
								// パーミッションを取得できなかった時
								callback.onCancel(device);
							} else {
								// パーミッションを取得できなかった時,
								// OS側がおかしいかAPI>=31でPendingIntentにFLAG_MUTABLEを指定していないとき
								callback.onError(device, new UsbPermissionException("device is null"));
							}
						} else {
							callback.onCancel(device);
						}
					} finally {
						latch.countDown();
					}
				}
			};
			if (DEBUG) Log.v(TAG, "requestPermission#registerReceiver:");
			ContextCompat.registerReceiver(context, receiver, new IntentFilter(ACTION_USB_PERMISSION), ContextCompat.RECEIVER_EXPORTED);
			try {
				final PendingIntent intent = createIntent(context);
				manager.requestPermission(device, intent);
				if (timeoutMs > 0) {
					if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
						if (DEBUG) Log.v(TAG, "requestPermission:timeout, cancel PendingIntent");
						// XXX requestPermissionへ渡したPendingIntentをキャンセルしても
						//  パーミッション要求自体はキャンセルされずダイアログが表示されたままになる(´･ω･`)
						intent.cancel();
					}
				} else {
					// 無限待ち
					latch.await();
				}
			} catch (final Exception e) {
				// Android5.1.xのGALAXY系でandroid.permission.sec.MDM_APP_MGMT
				// という意味不明の例外生成するみたい
				Log.w(TAG, e);
				callback.onCancel(device);
			} finally {
				if (DEBUG) Log.v(TAG, "requestPermission#unregisterReceiver:");
				context.unregisterReceiver(receiver);
			}
		});
	}
}
