package com.serenegiant.usb;
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
 *
 *  moved from aAndUsb
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.utils.BufferHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * USB機器の接続/切断イベント処理、USB機器アクセスパーミッション要求処理
 * USB機器のopen/close処理を行うためのヘルパークラス
 */
public final class USBMonitor extends UsbDetector implements Const {

	private static final boolean DEBUG = false;	// XXX 実働時にはfalseにすること
	private static final String TAG = "USBMonitor";

	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface Callback extends UsbDetector.Callback, UsbPermission.Callback {
		/**
		 * USB機器がopenされた時,
		 * 4.xx.yyと異なりUsbControlBlock#cloneでも呼ばれる
		 * @param device
		 * @param ctrlBlock
		 */
		@AnyThread
		public void onConnected(@NonNull final UsbDevice device,
			@NonNull final UsbControlBlock ctrlBlock);
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
	 * Callbackのシノニム
	 * @deprecated Callbackを使うこと
	 */
	@Deprecated
	public interface OnDeviceConnectListener extends Callback {
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
			@NonNull final UsbControlBlock ctrlBlock) {
		}

		@Override
		public void onDisconnect(@NonNull final UsbDevice device) {
		}
	}

	/**
	 * デフォルトのPermissionCallback/Callback実装
	 */
	public static PermissionCallback DEFAULT_CALLBACK = new PermissionCallback() {
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
	/**
	 * OpenしているUsbControlBlock一覧
	 */
	@NonNull
	private final List<UsbControlBlock> mCtrlBlocks = new ArrayList<>();
	@NonNull
	private final Callback mCallback;

	@Nullable
	private final UsbPermission mPermissionUtils;

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
		mPermissionUtils = new UsbPermission(context, callback, getHandler());
	}

	/**
	 * 破棄処理
	 * 一旦releaseを呼ぶと再利用は出来ない
	 */
	@Override
	public void release() {
		if (DEBUG) Log.i(TAG, "release:");
		mPermissionUtils.release();
		unregister();
		if (!isReleased()) {
			// モニターしているUSB機器を全てcloseする
			final List<UsbControlBlock> ctrlBlocks;
			synchronized (mCtrlBlocks) {
				ctrlBlocks = new ArrayList<>(mCtrlBlocks);
				mCtrlBlocks.clear();
			}
			for (final UsbControlBlock ctrlBlock: ctrlBlocks) {
				try {
					ctrlBlock.close();
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

		return mPermissionUtils.requestPermission(device);
	}

	/**
	 * パーミッションを要求する
	 * @param context
	 * @param device
	 * @throws IllegalStateException
	 */
	public static void requestPermission(
		@NonNull final Context context,
		@NonNull final UsbDevice device)
			throws IllegalArgumentException {
		UsbPermission.requestPermission(context, device, DEFAULT_CALLBACK);
	}

	/**
	 * パーミッションを要求する
	 * @param context
	 * @param device
	 * @param callback
	 * @throws IllegalStateException
	 */
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
	public UsbControlBlock openDevice(final UsbDevice device) throws IOException {
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
		mPermissionUtils.onReceive(context, intent);
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定したUSB機器をopenした時の処理
	 * @param device
	 */
	private void processConnect(@NonNull final UsbDevice device,
		@NonNull final UsbControlBlock ctrlBlock) {

		if (isReleased()) return;
		if (DEBUG) Log.v(TAG, "processConnect:");
		synchronized (mCtrlBlocks) {
			mCtrlBlocks.add(ctrlBlock);
		}
		if (hasPermission(device)) {
			post(new Runnable() {
				@Override
				public void run() {
//					if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
					mCallback.onConnected(device, ctrlBlock);
				}
			});
		}
	}

	/**
	 * USB機器との接続がcloseされたときの処理
	 * @param ctrlBlock
	 */
	private void callOnDisconnect(@NonNull final UsbDevice device,
		@NonNull final UsbControlBlock ctrlBlock) {

		if (isReleased()) return;
		if (DEBUG) Log.v(TAG, "callOnDisconnect:");
		synchronized (mCtrlBlocks) {
			mCtrlBlocks.remove(ctrlBlock);
		}
		post(new Runnable() {
			@Override
			public void run() {
				mCallback.onDisconnect(ctrlBlock.getDevice());
			}
		});
	}

	/**
	 * 指定したUsbDeviceに関係するUsbControlBlockの一覧を取得する
	 * @param device
	 * @return
	 */
	@NonNull
	private List<UsbControlBlock> findCtrlBlocks(@NonNull final UsbDevice device) {
		final List<UsbControlBlock> result = new ArrayList<>();
		synchronized (mCtrlBlocks) {
			for (final UsbControlBlock ctrlBlock: mCtrlBlocks) {
				if (ctrlBlock.getDevice().equals(device)) {
					result.add(ctrlBlock);
				}
			}
		}
		return result;
	}

	/**
	 * 指定したUsbDeviceに関係するUsbControlBlockをすべてmCtrlBlocksから削除してcloseする
	 * @param device
	 */
	private void removeAll(@NonNull final UsbDevice device) {
		@NonNull
		final List<UsbControlBlock> list = findCtrlBlocks(device);
		synchronized (mCtrlBlocks) {
			mCtrlBlocks.removeAll(list);
		}
		for (final UsbControlBlock ctrlBlock: list) {
			ctrlBlock.close();
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
	public static final class UsbControlBlock implements Cloneable {
		@NonNull
		private final WeakReference<USBMonitor> mWeakMonitor;
		@NonNull
		private final UsbDevice mDevice;
		@NonNull
		private final UsbDeviceInfo mInfo;
		@NonNull
		private final SparseArray<SparseArray<UsbInterface>>
			mInterfaces = new SparseArray<SparseArray<UsbInterface>>();
		@Nullable
		private UsbDeviceConnection mConnection;

		/**
		 * 指定したUsbDeviceに関係づけたUsbControlBlockインスタンスを生成する
		 * 内部でopenDeviceをするのでパーミションを取得してないとダメ
		 * @param monitor
		 * @param device
		 */
		private UsbControlBlock(@NonNull final USBMonitor monitor, @NonNull final UsbDevice device)
			throws IOException {

//			if (DEBUG) Log.v(TAG, "UsbControlBlock:device=" + device);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mDevice = device;
			final UsbManager manager = monitor.getUsbManager();
			// XXX UsbManager#openDeviceはIllegalArgumentExceptionを投げる可能性がある
			try {
				mConnection = manager.openDevice(device);
			} catch (final Exception e) {
				throw new IOException(e);
			}
			final String name = device.getDeviceName();
			if (mConnection != null) {
				final int fd = mConnection.getFileDescriptor();
				final byte[] rawDesc = mConnection.getRawDescriptors();
				Log.i(TAG, String.format(Locale.US,
					"name=%s,fd=%d,rawDesc=", name, fd)
						+ BufferHelper.toHexString(rawDesc, 0, 16));
			} else {
				// 多分ここには来ない(openDeviceの時点でIOException)けど年のために
				throw new IOException("could not connect to device " + name);
			}
			mInfo = UsbDeviceInfo.getDeviceInfo(manager, device, null);
			monitor.processConnect(device, this);
		}

		/**
		 * コピーコンストラクタ
		 * 単純コピー(参照を共有)ではなく同じUsbDeviceへアクセスするための別のUsbDeviceConnection/UsbDeviceInfoを生成する
		 * @param src
		 * @throws IllegalStateException
		 */
		private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException {
			final USBMonitor monitor = src.getMonitor();
			if (monitor == null) {
				throw new IllegalStateException("USBMonitor is already released?");
			}
			final UsbDevice device = src.getDevice();
			final UsbManager manager = monitor.getUsbManager();
			mConnection = manager.openDevice(device);
			if (mConnection == null) {
				throw new IllegalStateException("device may already be removed or have no permission");
			}
			mInfo = UsbDeviceInfo.getDeviceInfo(manager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mDevice = device;
			monitor.processConnect(device, this);
		}

		@Override
		protected void finalize() throws Throwable {
			try {
				close();
			} finally {
				super.finalize();
			}
		}

		/**
		 * 対応するUSB機器がopenしていて使用可能かどうかを取得
		 * @return
		 */
		public synchronized boolean isValid() {
			return mConnection != null;
		}

		/**
		 * クローンで複製する。
		 * 別途openし直すのでパーミッションが既に無いと失敗する。
		 * 複製したUsbControlBlockはUSBMonitorのリストに保持されていないので自前で破棄処理をすること
		 * @return
		 * @throws CloneNotSupportedException
		 */
		@SuppressWarnings("CloneDoesntCallSuperClone")
		@NonNull
		@Override
		public UsbControlBlock clone() throws CloneNotSupportedException {
			final UsbControlBlock ctrlblock;
			try {
				ctrlblock = new UsbControlBlock(this);
			} catch (final IllegalStateException e) {
				throw new CloneNotSupportedException(e.getMessage());
			}
			return ctrlblock;
		}

		@Nullable
		public USBMonitor getMonitor() {
			return mWeakMonitor.get();
		}

		@NonNull
		public UsbDevice getDevice() {
			return mDevice;
		}

		@NonNull
		public UsbDeviceInfo getInfo() {
			return mInfo;
		}

		/**
		 * 機器名を取得
		 * UsbDevice#mUsbDeviceを呼び出しているので
		 * 端末内でユニークな値だけど抜き差しすれば変わる
		 * すでに取り外されたり破棄されているときは空文字列が返る
		 * @return
		 */
		@NonNull
		public String getDeviceName() {
			return mDevice.getDeviceName();
		}

		/**
		 * 機器IDを取得
		 * UsbDevice#getDeviceIdを呼び出しているので
		 * 端末内でユニークな値だけど抜き差しすれば変わる
		 * @return
		 */
		public int getDeviceId() {
			return mDevice.getDeviceId();
		}

		/**
		 * UsbDeviceConnectionを取得
		 * UsbControlBlockでの排他制御から切り離されてしまうので注意
		 * @return
		 */
		@Nullable
		public synchronized UsbDeviceConnection getConnection() {
	
			return mConnection;
		}

		/**
		 * UsbDeviceConnectionを取得
		 * UsbControlBlockでの排他制御から切り離されてしまうので注意
		 * @return
		 * @throws IllegalStateException
		 */
		@NonNull
		public  synchronized  UsbDeviceConnection requireConnection()
			throws IllegalStateException {

			checkConnection();
			return mConnection;
		}

		/**
		 * Usb機器へアクセスするためのファイルディスクリプタを取得
		 * 使用不可の場合は0を返す
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int getFileDescriptor() {
			return mConnection != null ? mConnection.getFileDescriptor() : 0;
		}

		/**
		 * Usb機器へアクセスするためのファイルディスクリプタを取得
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int requireFileDescriptor() throws IllegalStateException {
			checkConnection();
			return mConnection.getFileDescriptor();
		}

		/**
		 * Usb機器のディスクリプタを取得
		 * 使用不可の場合はnullを返す
		 * @return
		 * @throws IllegalStateException
		 */
		@Nullable
		public synchronized byte[] getRawDescriptors() {
			checkConnection();
			return mConnection != null ? mConnection.getRawDescriptors() : null;
		}

		/**
		 * Usb機器のディスクリプタを取得
		 * @return
		 * @throws IllegalStateException
		 */
		@NonNull
		public synchronized byte[] requireRawDescriptors() throws IllegalStateException {
			checkConnection();
			return mConnection.getRawDescriptors();
		}

		/**
		 * ベンダーIDを取得
		 * @return
		 */
		public int getVenderId() {
			return mDevice.getVendorId();
		}

		/**
		 * プロダクトIDを取得
		 * @return
		 */
		public int getProductId() {
			return mDevice.getProductId();
		}

		/**
		 * USBのバージョンを取得
		 * @return
		 */
		public String getUsbVersion() {
			return mInfo.bcdUsb;
		}

		/**
		 * マニュファクチャ名(ベンダー名)を取得
		 * @return
		 */
		public String getManufacture() {
			return mInfo.manufacturer;
		}

		/**
		 * 製品名を取得
		 * @return
		 */
		public String getProductName() {
			return mInfo.product;
		}

		/**
		 * 製品のバージョンを取得
		 * @return
		 */
		public String getVersion() {
			return mInfo.version;
		}

		/**
		 * シリアルナンバーを取得
		 * @return
		 */
		public String getSerial() {
			return mInfo.serial;
		}

		/**
		 * インターフェースを取得する
		 * Java内でインターフェースをopenして使う時
		 * @param interface_id
		 * @throws IllegalStateException
		 */
		public synchronized UsbInterface getInterface(final int interface_id)
			throws IllegalStateException {

			return getInterface(interface_id, 0);
		}

		/**
		 * インターフェースを取得する
		 * @param interface_id
		 * @param altsetting
		 * @return
		 * @throws IllegalStateException
		 */
		@SuppressLint("NewApi")
		public synchronized UsbInterface getInterface(final int interface_id, final int altsetting)
			throws IllegalStateException {

			checkConnection();
			SparseArray<UsbInterface> intfs = mInterfaces.get(interface_id);
			if (intfs == null) {
				intfs = new SparseArray<UsbInterface>();
				mInterfaces.put(interface_id, intfs);
			}
			UsbInterface intf = intfs.get(altsetting);
			if (intf == null) {
				final int n = mDevice.getInterfaceCount();
				for (int i = 0; i < n; i++) {
					final UsbInterface temp = mDevice.getInterface(i);
					if ((temp.getId() == interface_id) && (temp.getAlternateSetting() == altsetting)) {
						intf = temp;
						break;
					}
				}
				if (intf != null) {
					intfs.append(altsetting, intf);
				}
			}
			return intf;
		}

		/**
		 * インターフェースを開く
		 * @param intf
		 * @throws IllegalStateException
		 */
		public synchronized void claimInterface(final UsbInterface intf)
			throws IllegalStateException {

			claimInterface(intf, true);
		}

		/**
		 * インターフェースを開く
		 * @param intf
		 * @param force
		 * @throws IllegalStateException
		 */
		public synchronized void claimInterface(final UsbInterface intf, final boolean force)
			throws IllegalStateException {

			checkConnection();
			mConnection.claimInterface(intf, force);
		}

		/**
		 * インターフェースを閉じる
		 * @param intf
		 * @throws IllegalStateException
		 */
		public synchronized void releaseInterface(final UsbInterface intf)
			throws IllegalStateException {

			checkConnection();
			final SparseArray<UsbInterface> intfs = mInterfaces.get(intf.getId());
			if (intfs != null) {
				final int index = intfs.indexOfValue(intf);
				intfs.removeAt(index);
				if (intfs.size() == 0) {
					mInterfaces.remove(intf.getId());
				}
			}
			mConnection.releaseInterface(intf);
		}
		
		/**
		 * 指定したエンドポイントに対してバルク転送を実行する
		 * @param endpoint
		 * @param buffer
		 * @param offset
		 * @param length
		 * @param timeout
		 * @return
		 * @throws IllegalStateException
		 */
		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public synchronized int bulkTransfer(final UsbEndpoint endpoint,
			final byte[] buffer, final int offset, final int length, final int timeout)
				throws IllegalStateException {
				
			checkConnection();
			return mConnection.bulkTransfer(endpoint, buffer, offset, length, timeout);
		}
		
		/**
		 * 指定したエンドポイントに対してバルク転送を実行する
 		 * @param endpoint
		 * @param buffer
		 * @param length
		 * @param timeout
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int bulkTransfer(final UsbEndpoint endpoint,
			final byte[] buffer, final int length, final int timeout)
				throws IllegalStateException {
			
			checkConnection();
			return mConnection.bulkTransfer(endpoint, buffer, length, timeout);
		}
		
		/**
		 * コントロール転送を実行する
 		 * @param requestType
		 * @param request
		 * @param value
		 * @param index
		 * @param buffer
		 * @param offset
		 * @param length
		 * @param timeout
		 * @return
		 * @throws IllegalStateException
		 */
		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public synchronized int controlTransfer(final int requestType, final int request,
			final int value, final int index,
			final byte[] buffer, final int offset, final int length, final int timeout)
				throws IllegalStateException {
			
			checkConnection();
			return mConnection.controlTransfer(requestType, request,
				value, index, buffer, offset, length, timeout);
		}
		
		/**
		 * コントロール転送を実行する
		 * @param requestType
		 * @param request
		 * @param value
		 * @param index
		 * @param buffer
		 * @param length
		 * @param timeout
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int controlTransfer(final int requestType, final int request,
			final int value, final int index,
			final byte[] buffer, final int length, final int timeout)
				throws IllegalStateException {

			checkConnection();
			return mConnection.controlTransfer(requestType, request,
				value, index, buffer, length, timeout);
		}

		/**
		 * デバイスを閉じる
		 * Java内でインターフェースをopenして使う時は開いているインターフェースも閉じる
		 */
		public void close() {
			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

			UsbDeviceConnection connection;
			synchronized (this) {
				connection = mConnection;
				mConnection = null;
			}
			if (connection != null) {
				// 2015/01/06 closeしてからonDisconnectを呼び出すように変更
				// openしているinterfaceが有れば閉じる XXX Java側でインターフェースを使う時
				final int n = mInterfaces.size();
				for (int i = 0; i < n; i++) {
					final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
					if (intfs != null) {
						final int m = intfs.size();
						for (int j = 0; j < m; j++) {
							final UsbInterface intf = intfs.valueAt(j);
							connection.releaseInterface(intf);
						}
						intfs.clear();
					}
				}
				mInterfaces.clear();
				connection.close();
				final USBMonitor monitor = getMonitor();
				final UsbDevice device = getDevice();
				if (monitor != null) {
					monitor.callOnDisconnect(device, this);
				}
			}
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null) return false;
			if (o instanceof UsbControlBlock) {
				final UsbDevice device = ((UsbControlBlock) o).getDevice();
				return device.equals(mDevice);
			} else if (o instanceof UsbDevice) {
				return o.equals(mDevice);
			}
			return super.equals(o);
		}

		private synchronized void checkConnection() throws IllegalStateException {
			if (mConnection == null) {
				throw new IllegalStateException("already closed");
			}
		}

	} // end ofUsbControlBlock

}
