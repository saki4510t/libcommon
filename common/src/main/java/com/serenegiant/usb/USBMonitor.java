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
 *
 *  moved from aAndUsb
*/

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * USB機器の接続/切断イベント処理、USB機器アクセスパーミッション要求処理
 * USB機器のopen/close処理を行うためのヘルパークラス
 */
public final class USBMonitor extends UsbDetector implements Const {

	private static final boolean DEBUG = false;	// XXX 実働時にはfalseにすること
	private static final String TAG = "USBMonitor";

	/**
	 * USB機器の接続/切断時のコールバックリスナー
	 */
	public interface ConnectionCallback {
		/**
		 * USB機器がopenされた時,
		 * 4.xx.yyと異なりUsbConnector#cloneでも呼ばれる
		 * XXX このコールバック内でUSBMonitor#openやconnector#clone等で新しく
		 *     UsbControlBlockを生成すると再度onConnectedが呼び出されるので連鎖的に
		 *     USB機器のopenが繰り返されてしまうので、
		 *     ・このコールバック内で新しくUsbControlBlockを生成しないようにする
		 *     ・このコールバックが呼ばれたときに既にopenしているかどうかを自前で確認する
		 *     ・#onConnected/#onDisconnectが呼び出されないようにUsbConnector(Context,UsbDevice)でUsbConnectorを生成する
		 *     などで意図せずに連鎖的にopen(onConnected)が繰り返されないようにする必要がある
		 * @param device
		 * @param connector
		 */
		@AnyThread
		public void onConnected(
			@NonNull final UsbDevice device,
			@NonNull final UsbConnector connector);
		/**
		 * open中のUSB機器が取り外されたか電源が切られた時
		 * デバイスは既にclose済み(2015/01/06呼び出すタイミングをclose前からclose後に変更)
		 * @param device
		 */
		@AnyThread
		public void onDisconnect(@NonNull final UsbDevice device);
	}

	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface Callback extends UsbDetector.Callback, UsbPermission.Callback, ConnectionCallback {
	}

	/**
	 * パーミッション要求時には呼ばれないコールバックリスナーを実装したCallback実装
	 */
	public static abstract class PermissionCallback implements Callback {
		@Override
		public void onAttach(@NonNull final UsbDevice device) {
		}

		@Override
		public void onDetach(@NonNull final UsbDevice device) {
		}

		@Override
		public void onConnected(
			@NonNull final UsbDevice device,
			@NonNull final UsbConnector connector) {
		}

		@Override
		public void onDisconnect(@NonNull final UsbDevice device) {
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * OpenしているUsbConnector一覧
	 */
	@NonNull
	private final List<UsbConnector> mConnectors = new ArrayList<>();
	@NonNull
	private final Callback mCallback;

	@Nullable
	private final UsbPermission mUsbPermission;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 */
	public USBMonitor(@NonNull final Context context,
		@NonNull final Callback callback) {
		super(context, callback);
		if (DEBUG) Log.v(TAG, "コンストラクタ");
		mCallback = callback;
		mUsbPermission = new UsbPermission(context, callback, getHandler());
	}

	/**
	 * 破棄処理
	 * 一旦releaseを呼ぶと再利用は出来ない
	 */
	@Override
	public void release() {
		if (DEBUG) Log.i(TAG, "release:");
		mUsbPermission.release();
		unregister();
		if (!isReleased()) {
			// モニターしているUSB機器を全てcloseする
			final List<UsbConnector> connectors;
			synchronized (mConnectors) {
				connectors = new ArrayList<>(mConnectors);
				mConnectors.clear();
			}
			for (final UsbConnector connector: connectors) {
				try {
					connector.close();
				} catch (final Exception e) {
					Log.e(TAG, "release:", e);
				}
			}
		}
		super.release();
	}

	/**
	 * 破棄処理
	 * 一旦destroyを呼ぶと再利用は出来ない
	 * #releaseのシノニム
	 */
	public void destroy() {
		release();
	}

	/**
	 * ブロードキャスト受信用のIntentFilterを生成する
	 * @return
	 */
	@Override
	protected IntentFilter createIntentFilter() {
		final IntentFilter filter = super.createIntentFilter();
		filter.addAction(UsbPermission.ACTION_USB_PERMISSION);
		return filter;
	}

//--------------------------------------------------------------------------------
	/**
	 * パーミッションが有るかどうかを問い合わせる
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 */
	public boolean hasPermission(final UsbDevice device) {
		return !isReleased()
			&& UsbPermission.hasPermission(getUsbManager(), device);
	}

	/**
	 * パーミッションを要求する
	 * @param device
	 * @return パーミッション要求が失敗したらtrueを返す
	 * @throws IllegalStateException
	 */
	public boolean requestPermission(@Nullable final UsbDevice device)
		throws IllegalStateException {

		return mUsbPermission.requestPermission(device);
	}

	/**
	 * パーミッションを要求する
	 * @param context
	 * @param device
	 * @throws IllegalStateException
	 * @deprecated UsbPermissionの同名メソッドを使う
	 */
	@Deprecated
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device)
			throws IllegalArgumentException {
		UsbPermission.requestPermission(context, device, UsbPermission.DEFAULT_CALLBACK);
	}

	/**
	 * パーミッションを要求する
	 * @param context
	 * @param device
	 * @param callback
	 * @throws IllegalStateException
	 * @deprecated UsbPermissionの同名メソッドを使う
	 */
	@Deprecated
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device,
		@NonNull final Callback callback)
			throws IllegalArgumentException {

		UsbPermission.requestPermission(context, device, callback);
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUsbDeviceをopenする
	 * @param device
	 * @return
	 * @throws SecurityException パーミッションがなければSecurityExceptionを投げる
	 */
	public UsbConnector openDevice(final UsbDevice device) throws IOException {
		if (DEBUG) Log.v(TAG, "openDevice:device=" + device);
		if (hasPermission(device)) {
			return new UsbControlBlock(USBMonitor.this, device);    // この中でopenDeviceする
		} else {
			throw new IOException("has no permission or invalid UsbDevice(already disconnected?)");
		}
	}
	
//--------------------------------------------------------------------------------
	/**
	 * パーミッション取得・USB機器のモニター用のBroadcastReceiverの処理の実態
	 * @param context
	 * @param intent
	 */
	@Override
	protected void onReceive(final Context context, final Intent intent) {
		super.onReceive(context, intent);
		mUsbPermission.onReceive(context, intent);
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUSB機器をopenした時の処理
	 * @param device
	 */
	private void processConnect(@NonNull final UsbDevice device,
		@NonNull final UsbConnector connector) {

		if (isReleased()) return;
		if (DEBUG) Log.v(TAG, "processConnect:");
		synchronized (mConnectors) {
			mConnectors.add(connector);
		}
		if (hasPermission(device)) {
			post(new Runnable() {
				@Override
				public void run() {
//					if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
					mCallback.onConnected(device, connector);
				}
			});
		}
	}

	/**
	 * USB機器との接続がcloseされたときの処理
	 * @param connector
	 */
	private void callOnDisconnect(@NonNull final UsbDevice device,
		@NonNull final UsbConnector connector) {

		if (isReleased()) return;
		if (DEBUG) Log.v(TAG, "callOnDisconnect:");
		synchronized (mConnectors) {
			mConnectors.remove(connector);
		}
		post(new Runnable() {
			@Override
			public void run() {
				mCallback.onDisconnect(connector.getDevice());
			}
		});
	}

	/**
	 * 指定したUsbDeviceに関係するUsbConnectorの一覧を取得する
	 * @param device
	 * @return
	 */
	@NonNull
	private List<UsbConnector> findConnectors(@NonNull final UsbDevice device) {
		final List<UsbConnector> result = new ArrayList<>();
		synchronized (mConnectors) {
			for (final UsbConnector connector: mConnectors) {
				if (connector.getDevice().equals(device)) {
					result.add(connector);
				}
			}
		}
		return result;
	}

	/**
	 * 指定したUsbDeviceに関係するUsbConnectorをすべてmConnectorsから削除してcloseする
	 * @param device
	 */
	private void removeAll(@NonNull final UsbDevice device) {
		@NonNull
		final List<UsbConnector> list = findConnectors(device);
		synchronized (mConnectors) {
			mConnectors.removeAll(list);
		}
		for (final UsbConnector connector: list) {
			connector.close();
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * USB機器をopenして管理するためのクラス
	 * 一度closeすると再利用は出来ないので、再度生成すること
	 * UsbMonitor内に参照一覧を保持しているので明示的にcloseを呼び出すまではopenしたまま維持される
	 * (UsbControlBlockを生成してファイルディスクリプタを取得してネイティブ側へ引き渡したときに
	 * 勝手にcloseされてしまわないようにするため)
	 */
	private static final class UsbControlBlock extends UsbConnector {
		@Nullable
		private WeakReference<USBMonitor> mWeakMonitor;

		/**
		 * 指定したUsbDeviceに関係づけたUsbControlBlockインスタンスを生成する
		 * 内部でopenDeviceをするのでパーミションを取得してないとダメ
		 * @param monitor
		 * @param device
		 */
		private UsbControlBlock(@NonNull final USBMonitor monitor, @NonNull final UsbDevice device)
			throws IOException {

			super(monitor.getUsbManager(), device);
//			if (DEBUG) Log.v(TAG, "UsbControlBlock:device=" + device);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			monitor.processConnect(device, this);
		}

		/**
		 * コピーコンストラクタ
		 * 単純コピー(参照を共有)ではなく同じUsbDeviceへアクセスするための別のUsbDeviceConnection/UsbDeviceInfoを生成する
		 * @param src
		 * @throws IllegalStateException
		 * @throws IOException
		 */
		private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException, IOException {
			super(src.getUsbManager(), src.getDevice());
			final USBMonitor monitor = src.getMonitor();
			if (monitor == null) {
				throw new IllegalStateException("USBMonitor is already released?");
			}
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			monitor.processConnect(getDevice(), this);
		}

		/**
		 * クローンで複製する。
		 * 別途openし直すのでパーミッションが既に無いと失敗する。
		 * @return
		 * @throws CloneNotSupportedException
		 */
		@NonNull
		@Override
		public UsbControlBlock clone() throws CloneNotSupportedException {
			final UsbControlBlock result = (UsbControlBlock)super.clone();
			// USBMonitorの弱参照は別途生成する
			final USBMonitor monitor = getMonitor();
			result.mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			monitor.processConnect(getDevice(), result);
			return result;
		}

		@Nullable
		public USBMonitor getMonitor() {
			return mWeakMonitor != null ? mWeakMonitor.get() : null;
		}

		/**
		 * デバイスを閉じる
		 * Java内でインターフェースをopenして使う時は開いているインターフェースも閉じる
		 */
		public void close() {
			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

			super.close();
			final UsbDevice device = getDevice();
			final USBMonitor monitor = getMonitor();
			mWeakMonitor = null;
			if (monitor != null) {
				monitor.callOnDisconnect(device, this);
			}
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null) return false;
			if (o instanceof UsbControlBlock) {
				final UsbDevice device = ((UsbControlBlock) o).getDevice();
				return device.equals(getDevice());
			}
			return super.equals(o);
		}

	} // end ofUsbControlBlock

}
