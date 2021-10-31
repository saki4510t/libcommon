package com.serenegiant.usb;
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
 *
 *  moved from aAndUsb
*/

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.BufferHelper;
import com.serenegiant.system.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FIXME USB機器のモニター機能とパーミッション要求/open/close等を分割する
 */
public final class USBMonitor implements Const {

	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = "USBMonitor";

	private static final String ACTION_USB_PERMISSION_BASE = "com.serenegiant.USB_PERMISSION.";
	private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface OnDeviceConnectListener {
		/**
		 * USB機器が取り付けられたか電源が入った時
		 * @param device
		 */
		public void onAttach(@NonNull final UsbDevice device);
		/**
		 * USB機器が取り外されたか電源が切られた時(open中であればonDisconnectの後に呼ばれる)
		 * @param device
		 */
		public void onDetach(@NonNull final UsbDevice device);
		/**
		 * パーミッション要求結果が返ってきた時
		 * @param device
		 */
		public void onPermission(@NonNull final UsbDevice device);
		/**
		 * USB機器がopenされた時,
		 * 4.xx.yyと異なりUsbControlBlock#cloneでも呼ばれる
		 * @param device
		 * @param ctrlBlock
		 */
		public void onConnected(@NonNull final UsbDevice device,
			@NonNull final UsbControlBlock ctrlBlock);
		/**
		 * open中のUSB機器が取り外されたか電源が切られた時
		 * デバイスは既にclose済み(2015/01/06呼び出すタイミングをclose前からclose後に変更)
		 * @param device
		 */
		public void onDisconnect(@NonNull final UsbDevice device);
		/**
		 * キャンセルまたはユーザーからパーミッションを得られなかった時
		 * @param device
		 */
		public void onCancel(final UsbDevice device);
		/**
		 * パーミッション要求時等で非同期実行中にエラーになった時
		 * @param device
		 * @param t
		 */
		public void onError(final UsbDevice device, final Throwable t);
	}

	/**
	 * OpenしているUsbControlBlock一覧
	 */
	private final List<UsbControlBlock> mCtrlBlocks = new ArrayList<>();
	private final WeakReference<Context> mWeakContext;
	private final UsbManager mUsbManager;
	@NonNull
	private final OnDeviceConnectListener mOnDeviceConnectListener;
	private PendingIntent mPermissionIntent = null;
	private final List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();
	/**
	 * 現在接続されている機器一覧
	 */
	@NonNull
	private final Set<UsbDevice> mAttachedDevices = new HashSet<>();

	/**
	 * コールバックをワーカースレッドで呼び出すためのハンドラー
	 */
	private final Handler mAsyncHandler;
	private volatile boolean destroyed;
	/**
	 * ポーリングで接続されているUSB機器の変化をチェックするかどうか
	 * Android5以上ではデフォルトはfalseでregister直後を覗いてポーリングしない
	 */
	private boolean mEnablePolling = !BuildCheck.isAndroid5();

	/**
	 * コンストラクタ
	 * @param context
	 * @param listener
	 */
	public USBMonitor(@NonNull final Context context,
		@NonNull final OnDeviceConnectListener listener) {

		if (DEBUG) Log.v(TAG, "USBMonitor:コンストラクタ");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = ContextUtils.requireSystemService(context, UsbManager.class);
		mOnDeviceConnectListener = listener;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		destroyed = false;
		if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
	}

	/**
	 * 破棄処理
	 * 一旦releaseを呼ぶと再利用は出来ない
	 */
	public void release() {
		if (DEBUG) Log.i(TAG, "release:");
		unregister();
		if (!destroyed) {
			destroyed = true;
			mAsyncHandler.removeCallbacksAndMessages(null);
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
			try {
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				Log.e(TAG, "release:", e);
			}
		}
	}

	/**
	 * 破棄処理
	 * 一旦destroyを呼ぶと再利用は出来ない
	 * #releaseのシノニム
	 */
	public void destroy() {
		release();
	}

//--------------------------------------------------------------------------------
	/**
	 * 接続/切断およびパーミッション要求に成功した時のブロードキャストを受信するためのブロードキャストレシーバーを登録する
	 * @throws IllegalStateException
	 */
	public synchronized void register() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		if (mPermissionIntent == null) {
			if (DEBUG) Log.i(TAG, "register:");
			final Context context = mWeakContext.get();
			if (context != null) {
				mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
				final IntentFilter filter = createIntentFilter();
				context.registerReceiver(mUsbReceiver, filter);
			} else {
				throw new IllegalStateException("context already released");
			}
			// すでに接続＆パーミッションを保持しているUSB機器にはATTACHイベントが来ないので
			// 少なくとも1回はポーリングする
			mAsyncHandler.postDelayed(mDeviceCheckRunnable, 500);
		}
	}

	/**
	 * 接続/切断およびパーミッション要求に成功した時のブロードキャストを受信するためのブロードキャストレシーバーを登録解除する
	 * @throws IllegalStateException
	 */
	public synchronized void unregister() throws IllegalStateException {
		// 接続チェック用Runnableを削除
		if (!destroyed) {
			mAsyncHandler.removeCallbacksAndMessages(null);
		}
		if (mPermissionIntent != null) {
			if (DEBUG) Log.i(TAG, "unregister:");
			final Context context = mWeakContext.get();
			try {
				if (context != null) {
					context.unregisterReceiver(mUsbReceiver);
				}
			} catch (final Exception e) {
				// ignore
			}
			mPermissionIntent = null;
		}
		synchronized (mAttachedDevices) {
			mAttachedDevices.clear();
		}
	}

	public synchronized boolean isRegistered() {
		return !destroyed && (mPermissionIntent != null);
	}

//--------------------------------------------------------------------------------
	/**
	 * デバイスフィルターを設定
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(@Nullable final DeviceFilter filter)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		if (filter != null) {
			mDeviceFilters.add(filter);
		}
	}

	/**
	 * デバイスフィルターを追加
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void addDeviceFilter(@NonNull final DeviceFilter filter)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.add(filter);
	}

	/**
	 * デバイスフィルターを削除
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void removeDeviceFilter(@Nullable final DeviceFilter filter)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.remove(filter);
	}

	/**
	 * set device filters
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(@Nullable final List<DeviceFilter> filters)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		if (filters != null) {
			mDeviceFilters.addAll(filters);
		}
	}

	/**
	 * add device filters
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void addDeviceFilter(@NonNull final List<DeviceFilter> filters)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.addAll(filters);
	}

	/**
	 * remove device filters
	 * @param filters
	 */
	public void removeDeviceFilter(final List<DeviceFilter> filters)
		throws IllegalStateException {

		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.removeAll(filters);
	}

//--------------------------------------------------------------------------------
	/**
	 * return the number of connected USB devices that matched device filter
	 * @return
	 */
	public int getDeviceCount() {
		return getDeviceList().size();
	}

	/**
	 * 設定してあるDeviceFilterに合うデバイスのリストを取得。合うのが無ければ空Listを返す(nullは返さない)
	 * @return
	 */
	@NonNull
	public List<UsbDevice> getDeviceList() {
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (destroyed) return result;
		final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		if (deviceList != null) {
			if (mDeviceFilters.isEmpty()) {
				result.addAll(deviceList.values());
			} else {
				for (final UsbDevice device: deviceList.values() ) {
					if (matches(device)) {
						result.add(device);
					}
				}
			}
		}
		return result;
	}

	/**
	 * フィルターにマッチするかどうかを確認
	 * @param device
	 * @return
	 */
	private boolean matches(@NonNull final UsbDevice device) {
		if (mDeviceFilters.isEmpty()) {
			// フィルタが空なら常時マッチする
			return true;
		} else {
			for (final DeviceFilter filter: mDeviceFilters) {
				if ((filter != null) && filter.matches(device)) {
					// フィルタにマッチした時
					if (!filter.isExclude) {
						if (DEBUG) Log.v(TAG, "matched:matched," + device + "\nfilter=" + filter);
						return true;
					}
					break; // excludeにマッチしたので終了
				}
			}
		}
		return false;
	}

	/**
	 * 指定したデバイス名に対応するUsbDeviceを取得する
	 * @param name　UsbDevice#getDeviceNameで取得できる値
	 * @return 見つからなければnull
	 */
	@Nullable
	public UsbDevice findDevice(final String name) {
		return UsbUtils.findDevice(getDeviceList(), name);
	}

	/**
	 * 接続中のUSB機器に対してattachイベントを再生成させる
	 */
	public void refreshDevices() {
		final List<UsbDevice> devices = getDeviceList();
		for (final UsbDevice device: devices) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onAttach(device);
				}
			});
		}
	}

	/**
	 * ポーリングによる接続機器のチェックを有効にするかどうか
	 * @return
	 */
	public boolean isEnablePolling() {
		return mEnablePolling;
	}

	/**
	 * ポーリングによる接続機器のチェックを有効にするかどうかを設定
	 * @param enable
	 */
	public synchronized void setEnablePolling(final boolean enable) {
		if (mEnablePolling != enable) {
			mEnablePolling = enable;
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
			if (enable && isRegistered()) {
				mAsyncHandler.postDelayed(mDeviceCheckRunnable, 500);
			}
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * パーミッションが有るかどうかを問い合わせる
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 */
	public final boolean hasPermission(final UsbDevice device) {
		return !destroyed
			&& (device != null) && mUsbManager.hasPermission(device);
	}

	/**
	 * パーミッションを要求する
	 * @param device
	 * @return パーミッション要求が失敗したらtrueを返す
	 */
	public synchronized boolean requestPermission(final UsbDevice device) {
		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device);
		boolean result = false;
		if (isRegistered()) {
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
				processCancel(device);
				result = true;
			}
		} else {
			processCancel(device);
			result = true;
		}
		return result;
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
	protected void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (ACTION_USB_PERMISSION.equals(action)) {
			// パーミッション要求の結果が返ってきた時
			synchronized (USBMonitor.this) {
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if ((device != null)
					&& (hasPermission(device)
						|| intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) ) {
					// パーミッションを取得できた時・・・デバイスとの通信の準備をする
					processPermission(device);
					return;
				}
				// パーミッションを取得できなかった時
				processCancel(device);
			}
		} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			// デバイスが取り付けられた時の処理・・・SC-06DはこのActionが来ない.ACTION_USB_DEVICE_DETACHEDは来る
			// Nexus7/5はaddActionしてれば来るけど、どのAndroidバージョンから来るのかわからない
			// Android5以降なら大丈夫そう
			final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			processAttach(device);
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			// デバイスが取り外された時
			final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (device != null) {
				processDettach(device);
			}
		}
	}

	/**
	 * ブロードキャスト受信用のIntentFilterを生成する
	 * @return
	 */
	protected IntentFilter createIntentFilter() {
		final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		if (BuildCheck.isAndroid5()) {
			filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);	// SC-06Dはこのactionが来ない
		}
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		return filter;
	}

//--------------------------------------------------------------------------------
	/**
	 * パーミッション取得・USB機器のモニター用のBroadcastReceiver
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (destroyed) return;
			USBMonitor.this.onReceive(context, intent);
		}
	};

	/**
	 * 古い一部機種向けのポーリングで接続機器をチェックするためのRunnable
	 * 定期的に接続しているデバイスを確認して数が変更されていればonAttachを呼び出す
	 */
	private final Runnable mDeviceCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (destroyed) return;
			if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable#run");
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
			// 現在接続されている機器
			final List<UsbDevice> currentDevices = getDeviceList();
			if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable:current=" + currentDevices.size());
			final List<UsbDevice> mChanged = new ArrayList<>();
			final Collection<UsbDevice> prevDevices;
			synchronized (mAttachedDevices) {
				prevDevices = new HashSet<>(mAttachedDevices);
				mAttachedDevices.clear();
				mAttachedDevices.addAll(currentDevices);
			}
			// 現在は接続されているが以前は接続されていなかった機器を探す
			for (final UsbDevice device: currentDevices) {
				if (!prevDevices.contains(device)) {
					mChanged.add(device);
				}
			}
			final int n = mChanged.size();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					final UsbDevice device = mChanged.get(i);
					mAsyncHandler.post(new Runnable() {
						@Override
						public void run() {
							mOnDeviceConnectListener.onAttach(device);
						}
					});
				}
			}
			if (mEnablePolling) {
				mAsyncHandler.postDelayed(mDeviceCheckRunnable, 1000);	// 1秒に1回確認
			}
		}
	};

	/**
	 * パーミッション要求結果が返ってきた時の処理
	 * @param device
	 */
	private final void processPermission(@NonNull final UsbDevice device) {
		mOnDeviceConnectListener.onPermission(device);
	}

	/**
	 * 指定したUSB機器をopenした時の処理
	 * @param device
	 */
	private final void processConnect(@NonNull final UsbDevice device,
		@NonNull final UsbControlBlock ctrlBlock) {

		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processConnect:");
		synchronized (mCtrlBlocks) {
			mCtrlBlocks.add(ctrlBlock);
		}
		if (hasPermission(device)) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
//					if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
					mOnDeviceConnectListener.onConnected(device, ctrlBlock);
				}
			});
		}
	}

	/**
	 * ユーザーキャンセル等でパーミッションを取得できなかったときの処理
	 * @param device
	 */
	private final void processCancel(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processCancel:");
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onCancel(device);
			}
		});
	}

	/**
	 * 端末にUSB機器が接続されたときの処理
	 * @param device
	 */
	private final void processAttach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processAttach:");
		if (matches(device)) {
			// フィルタにマッチした
			hasPermission(device);
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onAttach(device);
				}
			});
		}
	}

	/**
	 * 端末からUSB機器が取り外されたときの処理
	 * @param device
	 */
	private final void processDettach(@NonNull final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processDettach:");
		if (matches(device)) {
			// フィルタにマッチした
			// 切断されずに取り外されるときのために取り外されたUsbDeviceに関係するUsbControlBlockをすべて削除する
			removeAll(device);
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onDetach(device);
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

		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "callOnDisconnect:");
		synchronized (mCtrlBlocks) {
			mCtrlBlocks.remove(ctrlBlock);
		}
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onDisconnect(ctrlBlock.getDevice());
			}
		});
	}

	/**
	 * エラーコールバック呼び出し処理
	 * @param device
	 * @param t
	 */
	private void callOnError(@NonNull final UsbDevice device,
		@NonNull final Throwable t) {

		if (DEBUG) Log.v(TAG, "callOnError:");
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onError(device, t);
			}
		});
	}

	/**
	 * 指定したUsbDeviceに関係するUsbControlBlockの一覧を取得する
	 * @param device
	 * @return
	 */
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
	 * 指定したUsbDeviceに関係するUsbControlBlockをすべてmCtrlBlocksから削除する
	 * @param device
	 */
	private void removeAll(@NonNull final UsbDevice device) {
		final List<UsbControlBlock> list = findCtrlBlocks(device);
		for (final UsbControlBlock ctrlBlock: list) {
			synchronized (mCtrlBlocks) {
				mCtrlBlocks.remove(ctrlBlock);
			}
		}
	}
//--------------------------------------------------------------------------------
	/**
	 * USB機器をopenして管理するためのクラス
	 * 一度closeすると再利用は出来ないので、再度生成すること
	 */
	public static final class UsbControlBlock implements Cloneable {
		@NonNull
		private final WeakReference<USBMonitor> mWeakMonitor;
		@NonNull
		private final WeakReference<UsbDevice> mWeakDevice;
		@NonNull
		private final UsbDeviceInfo mInfo;
		@NonNull
		private final SparseArray<SparseArray<UsbInterface>>
			mInterfaces = new SparseArray<SparseArray<UsbInterface>>();
		@Nullable
		protected UsbDeviceConnection mConnection;

		/**
		 * 指定したUsbDeviceに関係づけたUsbControlBlockインスタンスを生成する
		 * 内部でopenDeviceをするのでパーミションを取得してないとダメ
		 * @param monitor
		 * @param device
		 */
		private UsbControlBlock(final USBMonitor monitor, final UsbDevice device)
			throws IOException {

//			if (DEBUG) Log.v(TAG, "UsbControlBlock:device=" + device);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			// XXX UsbManager#openDeviceはIllegalArgumentExceptionを投げる可能性がある
			try {
				mConnection = monitor.mUsbManager.openDevice(device);
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
			mInfo = UsbDeviceInfo.getDeviceInfo(monitor.mUsbManager, device, null);
			monitor.processConnect(device, this);
		}

		/**
		 * コピーコンストラクタ
		 * @param src
		 * @throws IllegalStateException
		 */
		private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException {
			final USBMonitor monitor = src.getMonitor();
			final UsbDevice device = src.getDevice();
			if (device == null) {
				throw new IllegalStateException("device may already be removed");
			}
			mConnection = monitor.mUsbManager.openDevice(device);
			if (mConnection == null) {
				throw new IllegalStateException("device may already be removed or have no permission");
			}
			mInfo = UsbDeviceInfo.getDeviceInfo(monitor.mUsbManager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
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

		@Nullable
		public final UsbDevice getDevice() {
			return mWeakDevice.get();
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
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceName() : "";
		}

		/**
		 * 機器IDを取得
		 * UsbDevice#getDeviceIdを呼び出しているので
		 * 端末内でユニークな値だけど抜き差しすれば変わる
		 * @return
		 */
		public int getDeviceId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceId() : 0;
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
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getVendorId() : 0;
		}

		/**
		 * プロダクトIDを取得
		 * @return
		 */
		public int getProductId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getProductId() : 0;
		}

		/**
		 * USBのバージョンを取得
		 * @return
		 */
		public String getUsbVersion() {
			return mInfo.usb_version;
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
				final UsbDevice device = mWeakDevice.get();
				final int n = device.getInterfaceCount();
				for (int i = 0; i < n; i++) {
					final UsbInterface temp = device.getInterface(i);
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
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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
				if ((monitor != null) && (device != null)) {
					monitor.callOnDisconnect(device, this);
				}
			}
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null) return false;
			if (o instanceof UsbControlBlock) {
				final UsbDevice device = ((UsbControlBlock) o).getDevice();
				return device == null ? mWeakDevice.get() == null
						: device.equals(mWeakDevice.get());
			} else if (o instanceof UsbDevice) {
				return o.equals(mWeakDevice.get());
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