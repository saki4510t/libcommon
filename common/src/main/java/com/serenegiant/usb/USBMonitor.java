package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class USBMonitor implements Const {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = "USBMonitor";

	private static final String ACTION_USB_PERMISSION_BASE = "com.serenegiant.USB_PERMISSION.";
	private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

	/**
	 * openしているUsbControlBlock
	 */
	private final ConcurrentHashMap<UsbDevice, UsbControlBlock> mCtrlBlocks
		= new ConcurrentHashMap<UsbDevice, UsbControlBlock>();
	private final SparseArray<WeakReference<UsbDevice>> mHasPermissions
		= new SparseArray<WeakReference<UsbDevice>>();

	private final WeakReference<Context> mWeakContext;
	private final UsbManager mUsbManager;
	private final OnDeviceConnectListener mOnDeviceConnectListener;
	private PendingIntent mPermissionIntent = null;
	private List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

	/**
	 * コールバックをワーカースレッドで呼び出すためのハンドラー
	 */
	private final Handler mAsyncHandler;
	private volatile boolean destroyed;
	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface OnDeviceConnectListener {
		/**
		 * USB機器が取り付けられたか電源が入った時
		 * @param device
		 */
		public void onAttach(UsbDevice device);
		/**
		 * USB機器が取り外されたか電源が切られた時(open中であればonDisconnectの後に呼ばれる)
		 * @param device
		 */
		public void onDettach(UsbDevice device);
		/**
		 * USB機器がopenされた時
		 * @param device
		 * @param ctrlBlock
		 * @param createNew
		 */
		public void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew);
		/**
		 * open中のUSB機器が取り外されたか電源が切られた時
		 * デバイスは既にclose済み(2015/01/06呼び出すタイミングをclose前からclose後に変更)
		 * @param device
		 * @param ctrlBlock
		 */
		public void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock);
		/**
		 * キャンセルまたはユーザーからパーミッションを得られなかった時
		 * @param device
		 */
		public void onCancel(UsbDevice device);
	}

	public USBMonitor(@NonNull final Context context,
		@NonNull final OnDeviceConnectListener listener) {
//		if (DEBUG) Log.v(TAG, "USBMonitor:コンストラクタ");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		mOnDeviceConnectListener = listener;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		destroyed = false;
//		if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
	}

	/**
	 * 破棄処理
	 * 一旦destroyを呼ぶと再利用は出来ない
	 */
	public void destroy() {
//		if (DEBUG) Log.i(TAG, "destroy:");
		unregister();
		if (!destroyed) {
			destroyed = true;
			mAsyncHandler.removeCallbacksAndMessages(null);
			// モニターしているUSB機器を全てcloseする
			final Set<UsbDevice> keys = mCtrlBlocks.keySet();
			if (keys != null) {
				UsbControlBlock ctrlBlock;
				try {
					for (final UsbDevice key: keys) {
						ctrlBlock = mCtrlBlocks.remove(key);
						if (ctrlBlock != null) {
							ctrlBlock.close();
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "destroy:", e);
				}
			}
			mCtrlBlocks.clear();
			try {
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				Log.e(TAG, "destroy:", e);
			}
		}
	}

	/**
	 * 接続/切断およびパーミッション要求に成功した時のブロードキャストを受信するためのブロードキャストレシーバーを登録する
	 * @throws IllegalStateException
	 */
	public synchronized void register() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		if (mPermissionIntent == null) {
//			if (DEBUG) Log.i(TAG, "register:");
			final Context context = mWeakContext.get();
			if (context != null) {
				mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
				final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//				filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);	// SC-06Dはこのactionが来ない, マニフェストに記載したのでここには追加しない
				filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
				context.registerReceiver(mUsbReceiver, filter);
			} else {
				throw new IllegalStateException("context already released");
			}
			// 接続チェック開始
			mDeviceCounts = 0;
			mAsyncHandler.postDelayed(mDeviceCheckRunnable, 500);
		}
	}

	/**
	 * 接続/切断およびパーミッション要求に成功した時のブロードキャストを受信するためのブロードキャストレシーバーを登録解除する
	 * @throws IllegalStateException
	 */
	public synchronized void unregister() throws IllegalStateException {
		// 接続チェック用Runnableを削除
		mDeviceCounts = 0;
		if (!destroyed) {
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
		}
		if (mPermissionIntent != null) {
//			if (DEBUG) Log.i(TAG, "unregister:");
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
	}

	public synchronized boolean isRegistered() {
		return !destroyed && (mPermissionIntent != null);
	}

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
	public List<UsbDevice> getDeviceList() {
		return getDeviceList(mDeviceFilters);
	}

	/**
	 * 指定したDeviceFilterに合うデバイスのリストを取得
	 * @param filters nullならフィルターしない
	 * @return 合うデバイスが無ければ空のListを返す(nullは返さない)
	 */
	public List<UsbDevice> getDeviceList(final List<DeviceFilter> filters) {
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (destroyed) return result;
		final HashMap<String, UsbDevice> deviceList = getDeviceList(mUsbManager);
		if (deviceList != null) {
			if ((filters == null) || filters.isEmpty()) {
				result.addAll(deviceList.values());
			} else {
				for (final UsbDevice device: deviceList.values() ) {
					for (final DeviceFilter filter: filters) {
						if ((filter != null) && filter.matches(device)) {
							// フィルタにマッチした時
							if (!filter.isExclude) {
								// excludeで無い時のみ追加する
								result.add(device);
							}
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * 指定したDeviceFilterに合うデバイスのリストを取得
	 * @param filter nullならフィルターしない
	 * @return 合うデバイスが無ければ空のListを返す(nullは返さない)
	 */
	public List<UsbDevice> getDeviceList(final DeviceFilter filter) {
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (destroyed) return result;
		final HashMap<String, UsbDevice> deviceList = getDeviceList(mUsbManager);
		if (deviceList != null) {
			for (final UsbDevice device: deviceList.values() ) {
				if ((filter == null) || (filter.matches(device) && !filter.isExclude)) {
					result.add(device);
				}
			}
		}
		return result;
	}

	/**
	 * デバイスリストを取得(フィルター無し)
	 * @return
	 */
	public Iterator<UsbDevice> getDevices() {
		if (destroyed) return null;
		Iterator<UsbDevice> iterator = null;
		final HashMap<String, UsbDevice> list = getDeviceList(mUsbManager);
		if (list != null)
			iterator = list.values().iterator();
		return iterator;
	}

	/**
	 * 接続されているUSBの機器リストをLogCatに出力
	 */
	public final void dumpDevices() {
		final HashMap<String, UsbDevice> list = getDeviceList(mUsbManager);
		if (list != null) {
			final Set<String> keys = list.keySet();
			if (keys != null && keys.size() > 0) {
				final StringBuilder sb = new StringBuilder();
				for (final String key: keys) {
					final UsbDevice device = list.get(key);
					final int num_interface = device != null ? device.getInterfaceCount() : 0;
					sb.setLength(0);
					for (int i = 0; i < num_interface; i++) {
						sb.append(String.format(Locale.US, "interface%d:%s",
							i, device.getInterface(i).toString()));
					}
					Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
				}
			} else {
				Log.i(TAG, "no device");
			}
		} else {
			Log.i(TAG, "no device");
		}
	}

	/**
	 * パーミッションが有るかどうかを問い合わせる
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 */
	public final boolean hasPermission(final UsbDevice device) {
		return !destroyed
			&& updatePermission(device, device != null && hasPermission(mUsbManager, device));
	}

	/**
	 * 内部で保持しているパーミッション状態を更新
	 * @param device
	 * @param hasPermission
	 * @return hasPermission
	 */
	private boolean updatePermission(final UsbDevice device, final boolean hasPermission) {
		final int deviceKey = getDeviceKey(device, true);
		synchronized (mHasPermissions) {
			if (hasPermission) {
				if (mHasPermissions.get(deviceKey) == null) {
					mHasPermissions.put(deviceKey, new WeakReference<UsbDevice>(device));
				}
			} else {
				mHasPermissions.remove(deviceKey);
			}
		}
		return hasPermission;
	}

	/**
	 * パーミッションを要求する
	 * @param device
	 * @return パーミッション要求が失敗したらtrueを返す
	 */
	public synchronized boolean requestPermission(final UsbDevice device) {
//		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device);
		boolean result = false;
		if (isRegistered()) {
			if (device != null) {
				if (hasPermission(mUsbManager, device)) {
					// 既にパーミッションが有れば接続する
					processConnect(device);
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

	/**
	 * 指定したUsbDeviceをopenする
	 * @param device
	 * @return
	 * @throws SecurityException パーミッションがなければSecurityExceptionを投げる
	 */
	public UsbControlBlock openDevice(final UsbDevice device) throws SecurityException {
		if (hasPermission(device)) {
			UsbControlBlock result = mCtrlBlocks.get(device);
			if (result == null) {
				result = new UsbControlBlock(USBMonitor.this, device);    // この中でopenDeviceする
				mCtrlBlocks.put(device, result);
			}
			return result;
		} else {
			throw new SecurityException("has no permission");
		}
	}
	
	/**
	 * 指定したUsbDeviceをopenしているかどうかを返す
	 * USBMonitorで管理していないときは正しい値を返さないので注意
	 * @param device
	 * @return true: openしていてこのUSBMonitorで管理している,
	 *         false: それ以外(パーミッションが無い、openしていないなど)
	 */
	public boolean isOpened(final UsbDevice device) {
		if (hasPermission(device)) {
			UsbControlBlock result = mCtrlBlocks.get(device);
			return result != null;
		}
		return false;
	}
	
	/**
	 * パーミッション取得・USB機器のモニター用のBroadcastReceiver
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (destroyed) return;
			final String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				// パーミッション要求の結果が返ってきた時
				synchronized (USBMonitor.this) {
					final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							// パーミッションを取得できた時・・・デバイスとの通信の準備をする
							processConnect(device);
						}
					} else {
						// パーミッションを取得できなかった時
						processCancel(device);
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				// デバイスが取り付けられた時の処理・・・SC-06DはこのActionが来ない.ACTION_USB_DEVICE_DETACHEDは来る
				// Nexus7/5はaddActionしてれば来るけど、どのAndroidバージョンから来るのかわからないので、addActionはせずに
				// マニュフェストにintent filterを追加＆ポーリングでデバイス接続を確認するように変更
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				processAttach(device);
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// デバイスが取り外された時
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device != null) {
					UsbControlBlock ctrlBlock = mCtrlBlocks.remove(device);
					if (ctrlBlock != null) {
						// デバイスとの通信をクリーンアップして閉じるためのメソッドを呼び出す
						ctrlBlock.close();
					}
					mDeviceCounts = 0;	// 更新が必要
					processDettach(device);
				}
			}
		}
	};

	/** 接続されているデバイスの数 */
	private volatile int mDeviceCounts = 0;
	/**
	 * 定期的に接続しているデバイスを確認して数が変更されていればonAttachを呼び出す
	 */
	private final Runnable mDeviceCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (destroyed) return;
			// FIXME 新規に接続されたものだけに限定したい
			final List<UsbDevice> devices = getDeviceList();
			final int n = devices.size();
			final int hasPermissionCounts;
			final int m;
			synchronized (mHasPermissions) {
				hasPermissionCounts = mHasPermissions.size();
				mHasPermissions.clear();
				for (final UsbDevice device: devices) {
					hasPermission(device);
				}
				m = mHasPermissions.size();
			}
			if ((n > mDeviceCounts) || (m > hasPermissionCounts)) {
				mDeviceCounts = n;
				if (mOnDeviceConnectListener != null) {
					for (int i = 0; i < n; i++) {
						final UsbDevice device = devices.get(i);
						mAsyncHandler.post(new Runnable() {
							@Override
							public void run() {
								mOnDeviceConnectListener.onAttach(device);
							}
						});
					}
				}
			}
			mAsyncHandler.postDelayed(this, 1000);	// 1秒に1回確認
		}
	};

	/**
	 * 指定したUSB機器との接続をopenする
	 * @param device
	 */
	private final void processConnect(final UsbDevice device) {
		if (destroyed) return;
//		if (DEBUG) Log.v(TAG, "processConnect:");
		updatePermission(device, true);
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
//				if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
				UsbControlBlock ctrlBlock;
				final boolean createNew;
				ctrlBlock = mCtrlBlocks.get(device);
				if (ctrlBlock == null) {
					ctrlBlock = new UsbControlBlock(USBMonitor.this, device);    // この中でopenDeviceする
					mCtrlBlocks.put(device, ctrlBlock);
					createNew = true;
				} else {
					createNew = false;
				}
				if (mOnDeviceConnectListener != null) {
					mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
				}
			}
		});
	}

	private final void processCancel(final UsbDevice device) {
		if (destroyed) return;
//		if (DEBUG) Log.v(TAG, "processCancel:");
		updatePermission(device, false);
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onCancel(device);
				}
			});
		}
	}

	private final void processAttach(final UsbDevice device) {
		if (destroyed) return;
//		if (DEBUG) Log.v(TAG, "processAttach:");
		hasPermission(device);
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onAttach(device);
				}
			});
		}
	}

	private final void processDettach(final UsbDevice device) {
		if (destroyed) return;
//		if (DEBUG) Log.v(TAG, "processDettach:");
		updatePermission(device, false);
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onDettach(device);
				}
			});
		}
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じキー名になるので注意
	 * @param device nullなら空文字列を返す
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device) {
		return getDeviceKeyName(device, null, false);
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI) {
		return getDeviceKeyName(device, null, useNewAPI);
	}
	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。この機器名をHashMapのキーにする
	 * UsbDeviceがopenしている時のみ有効
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * serialがnullや空文字でなければserialを含めたデバイスキー名を生成する
	 * useNewAPI=trueでAPIレベルを満たしていればマニュファクチャ名, バージョン, コンフィギュレーションカウントも使う
	 * @param device nullなら空文字列を返す
	 * @param serial	UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI) {
		return getDeviceKeyName(device, serial, useNewAPI, false);
	}
	
	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。この機器名をHashMapのキーにする
	 * UsbDeviceがopenしている時のみ有効
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * serialがnullや空文字でなければserialを含めたデバイスキー名を生成する
	 * useNewAPI=trueでAPIレベルを満たしていればマニュファクチャ名, バージョン, コンフィギュレーションカウントも使う
	 * usuNonce=trueならUsbDevice#getDeviceNameも含める。ただし機器をつなぎ直すと異なるデバイスキー名になるので注意
	 * @param device nullなら空文字列を返す
	 * @param serial	UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @param usuNonce シリアルキーが無い同種の機器を区別するための追加の文字列として
	 * 					UsbDevice#getDeviceNameを使うかどうか。
	 * 					ただし機器をつなぎ直すと異なるデバイスキー名になるので注意
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI, final boolean usuNonce) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId()).append("#");		// API >= 12
		sb.append(device.getProductId()).append("#");		// API >= 12
		sb.append(device.getDeviceClass()).append("#");		// API >= 12
		sb.append(device.getDeviceSubclass()).append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());				// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#").append(serial);
		}
		if (usuNonce) {
			sb.append("#").append(device.getDeviceName());	// API >= 12
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			sb.append("#");
			if (TextUtils.isEmpty(serial)) {
				sb.append(device.getSerialNumber()).append("#");	// API >= 21
			}
			sb.append(device.getManufacturerName()).append("#");	// API >= 21
			sb.append(device.getConfigurationCount()).append("#");	// API >= 21
			if (BuildCheck.isMarshmallow()) {
				sb.append(device.getVersion()).append("#");			// API >= 23　XXX ここで末尾に付く#が余分だった...
			}
		}
		return sb.toString();
		// FIXME 同じハッシュにならないので一時的に戻す
//		final boolean b = useNewAPI && BuildCheck.isAndroid5();
//		return getDeviceKeyName(device, useNewAPI,
//			!TextUtils.isEmpty(serial) ? serial : (b && TextUtils.isEmpty(serial) ? device.getSerialNumber() : null),
//			b ? device.getManufacturerName() : null,
//			b ? device.getConfigurationCount() : 0,
//			b && BuildCheck.isMarshmallow() ? device.getVersion() + "#" : null
//		);
	}
	
	/**
	 * デバイスキー文字列を生成
	 * @param device
	 * @param serial
	 * @param manufactureName
	 * @param configCount
	 * @param deviceVersion
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI,
		final String serial, final String manufactureName, final int configCount, final String deviceVersion) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId());			sb.append("#");	// API >= 12
		sb.append(device.getProductId());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceClass());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceSubclass());		sb.append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());						// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			if (!TextUtils.isEmpty(manufactureName)) {
				sb.append("#");	sb.append(manufactureName);
			}
			if (configCount >= 0) {
				sb.append("#");	sb.append(configCount);
			}
			if (!TextUtils.isEmpty(deviceVersion)) {
				sb.append("#");	sb.append(deviceVersion);
			}
		}
		return sb.toString();
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * 同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device) {
		return device != null ? getDeviceKeyName(device, null, false).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI) {
		return device != null ? getDeviceKeyName(device, null, useNewAPI).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @param useNonce
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI, final boolean useNonce) {
		return device != null ? getDeviceKeyName(device, null, useNewAPI, useNonce).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * serialがnullでuseNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @param serial UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device, final String serial,
		final boolean useNewAPI) {

		return device != null ? getDeviceKeyName(device, serial, useNewAPI).hashCode() : 0;
	}
	
	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * 同じ機器でもつなぎ直すと違うデバイスキーになるので注意
	 * @param device
	 * @param serial
	 * @param useNewAPI
	 * @param useNonce
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device, final String serial,
		final boolean useNewAPI, final boolean useNonce) {

		return device != null ? getDeviceKeyName(device, serial, useNewAPI, useNonce).hashCode() : 0;
	}

	/**
	 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを取得する
	 * シリアルナンバーを取得できなければgetDeviceKeyNameと同じ
	 * @param context
	 * @param device
	 * @return
	 */
	public static String getDeviceKeyNameWithSerial(final Context context, final UsbDevice device) {
		final UsbDeviceInfo info = getDeviceInfo(context, device);
		return getDeviceKeyName(device, true, info.serial, info.manufacturer, info.configCounts, info.version);
	}

	/**
	 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを整数として取得
	 * getDeviceKeyNameWithSerialで得られる文字列のhasCodeを取得
	 * シリアルナンバーを取得できなければgetDeviceKeyと同じ
	 * @return
	 */
	public static int getDeviceKeyWithSerial(final Context context, final UsbDevice device) {
		return getDeviceKeyNameWithSerial(context, device).hashCode();
	}

	public static class UsbDeviceInfo {
		/** 機器が対応しているUSB規格 */
		public String usb_version;
		/** ベンダー名 */
		public String manufacturer;
		/** プロダクト名 */
		public String product;
		/** 機器のバージョン */
		public String version;
		/** 機器のシリアル番号 */
		public String serial;
		/** コンフィギュレーションの個数 */
		public int configCounts;

		private void clear() {
			usb_version = manufacturer = product = version = serial = null;
			configCounts = -1;
		}

		@Override
		public String toString() {
			return String.format("UsbDeviceInfo:usb_version=%s,manufacturer=%s,product=%s,version=%s,serial=%s,configCounts=%s",
				usb_version != null ? usb_version : "",
				manufacturer != null ? manufacturer : "",
				product != null ? product : "",
				version != null ? version : "",
				serial != null ? serial : "",
				configCounts >= 0 ? Integer.toString(configCounts) : "");
		}
	}

	/**
	 * 指定したIDのStringディスクリプタから文字列を取得する。取得できなければnull
	 * @param connection
	 * @param id
	 * @param languageCount
	 * @param languages
	 * @return
	 */
	private static String getString(final UsbDeviceConnection connection, final int id, final int languageCount, final byte[] languages) {
		final byte[] work = new byte[256];
		String result = null;
		for (int i = 1; i <= languageCount; i++) {
			int ret = connection.controlTransfer(
				USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
				USB_REQ_GET_DESCRIPTOR,
				(USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
			if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
				// skip first two bytes(bLength & bDescriptorType), and copy the rest to the string
				try {
					result = new String(work, 2, ret - 2, "UTF-16LE");
					if (!"Љ".equals(result)) {	// 変なゴミが返ってくる時がある
						break;
					} else {
						result = null;
					}
				} catch (final UnsupportedEncodingException e) {
					// ignore
				}
			}
		}
		return result;
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * @param device
	 * @return
	 */
	public UsbDeviceInfo getDeviceInfo(final UsbDevice device) {
		return updateDeviceInfo(mUsbManager, device, null);
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * #updateDeviceInfo(final UsbManager, final UsbDevice, final UsbDeviceInfo)のヘルパーメソッド
	 * @param context
	 * @param device
	 * @return
	 */
	public static UsbDeviceInfo getDeviceInfo(final Context context, final UsbDevice device) {
		return updateDeviceInfo((UsbManager)context.getSystemService(Context.USB_SERVICE), device, new UsbDeviceInfo());
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * @param manager
	 * @param device
	 * @param _info
	 * @return
	 */
	@SuppressLint("NewApi")
	public static UsbDeviceInfo updateDeviceInfo(final UsbManager manager, final UsbDevice device, final UsbDeviceInfo _info) {
		final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
		info.clear();

		if (device != null) {
			if (BuildCheck.isLollipop()) {	// API >= 21
				info.manufacturer = device.getManufacturerName();
				info.product = device.getProductName();
				info.serial = device.getSerialNumber();
				info.configCounts = device.getConfigurationCount();
			}
			if (BuildCheck.isMarshmallow()) {	// API >= 23
				info.version = device.getVersion();
			}
			if ((manager != null) && hasPermission(manager, device)) {
				final UsbDeviceConnection connection = manager.openDevice(device);
				if (connection != null) {
					try {
						final byte[] desc = connection.getRawDescriptors();
						if (desc != null) {
							if (TextUtils.isEmpty(info.usb_version)) {
								info.usb_version = String.format("%x.%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
							}
							if (TextUtils.isEmpty(info.version)) {
								info.version = String.format("%x.%02x", ((int)desc[13] & 0xff), ((int)desc[12] & 0xff));
							}
							if (TextUtils.isEmpty(info.serial)) {
								info.serial = connection.getSerial();
							}
							if (info.configCounts < 0) {
								// FIXME 未実装 デバイスディスクリプタをパースせんとなりゃん
								info.configCounts = 1;
							}

							final byte[] languages = new byte[256];
							int languageCount = 0;
							// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
							int result = connection.controlTransfer(
								USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
								USB_REQ_GET_DESCRIPTOR,
								(USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
							if (result > 0) {
								languageCount = (result - 2) / 2;
							}
							if (languageCount > 0) {
								if (TextUtils.isEmpty(info.manufacturer)) {
									info.manufacturer = getString(connection, desc[14], languageCount, languages);
								}
								if (TextUtils.isEmpty(info.product)) {
									info.product = getString(connection, desc[15], languageCount, languages);
								}
								if (TextUtils.isEmpty(info.serial)) {
									info.serial = getString(connection, desc[16], languageCount, languages);
								}
							}
						}
					} finally {
						connection.close();
					}
				}
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = USBVendorId.vendorName(device.getVendorId());
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = String.format("%04x", device.getVendorId());
			}
			if (TextUtils.isEmpty(info.product)) {
				info.product = String.format("%04x", device.getProductId());
			}
		}
		return info;
	}

	/**
	 * USB機器をopenして管理するためのクラス
	 * 一度closeすると再利用は出来ないので、再度生成すること
	 */
	public static final class UsbControlBlock implements Cloneable {
		private final WeakReference<USBMonitor> mWeakMonitor;
		private final WeakReference<UsbDevice> mWeakDevice;
		protected UsbDeviceConnection mConnection;
		protected final UsbDeviceInfo mInfo;
		private final int mBusNum;
		private final int mDevNum;
		private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

		/**
		 * 指定したUsbDeviceに関係づけたUsbControlBlockインスタンスを生成する
		 * 内部でopenDeviceをするのでパーミションを取得してないとダメ
		 * @param monitor
		 * @param device
		 */
		private UsbControlBlock(final USBMonitor monitor, final UsbDevice device) {
//			if (DEBUG) Log.v(TAG, "UsbControlBlock:device=" + device);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mConnection = monitor.mUsbManager.openDevice(device);
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			final String name = device.getDeviceName();
			final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
			int busnum = 0;
			int devnum = 0;
			if (v != null) {
				busnum = Integer.parseInt(v[v.length-2]);
				devnum = Integer.parseInt(v[v.length-1]);
			}
			mBusNum = busnum;
			mDevNum = devnum;
//			if (DEBUG) {
				if (mConnection != null) {
					final int desc = mConnection.getFileDescriptor();
					final byte[] rawDesc = mConnection.getRawDescriptors();
					Log.i(TAG, String.format(Locale.US, "name=%s,desc=%d,busnum=%d,devnum=%d,rawDesc=", name, desc, busnum, devnum) + rawDesc);
				} else {
					Log.e(TAG, "could not connect to device " + name);
				}
//			}
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
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mBusNum = src.mBusNum;
			mDevNum = src.mDevNum;
			// FIXME USBMonitor.mCtrlBlocksに追加する(今はHashMapなので追加すると置き換わってしまうのでだめ, ListかHashMapにListをぶら下げる?)
		}

		/**
		 * クローンで複製する。
		 * 別途openし直すのでパーミッションが既に無いと失敗する。
		 * 複製したUsbControlBlockはUSBMonitorのリストに保持されていないので自前で破棄処理をすること
		 * @return
		 * @throws CloneNotSupportedException
		 */
		@SuppressWarnings("CloneDoesntCallSuperClone")
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

		public USBMonitor getMonitor() {
			return mWeakMonitor.get();
		}

		public final UsbDevice getDevice() {
			return mWeakDevice.get();
		}

		/**
		 * 機器名を取得
		 * UsbDevice#mUsbDeviceを呼び出しているので
		 * 端末内でユニークな値だけど抜き差しすれば変わる
		 * @return
		 */
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
		 * 機器毎にユニークなデバイスキー文字列を取得
		 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
		 * 抜き差ししても変わらない。ただし同種の機器なら同じ値が返る
		 * @return
		 */
		public String getDeviceKeyName() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get());
		}

		/**
		 * 機器毎にユニークなデバイスキー文字列を取得
		 * 抜き差ししても変わらない。シリアルを取得可能な機器であれば同種でも異なる値が返る
		 * @param useNewAPI
		 * @return
		 * @throws IllegalStateException
		 */
		public String getDeviceKeyName(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * 機器毎にユニークなデバイスキー文字列を取得
		 * 抜き差しすると異なる値が返る可能性がある
		 * @param useNewAPI
		 * @return
		 * @throws IllegalStateException
		 */
		public String getDeviceKeyName(final boolean useNewAPI, final boolean useNonce) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI, useNonce);
		}

		/**
		 * デバイスキーを整数として取得
		 * getDeviceKeyNameで得られる文字列のhasCodeを取得
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey() throws IllegalStateException {
			checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get());
		}

		/**
		 * デバイスキーを整数として取得
		 * 抜き差ししても変わらない。シリアルを取得可能な機器であれば同種でも異なる値が返る
		 * @param useNewAPI
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * デバイスキーを整数として取得
		 * 抜き差しすると異なる値が返る可能性がある
		 * @param useNewAPI
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey(final boolean useNewAPI, final boolean useNonce) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI, useNonce);
		}

		/**
		 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを取得する
		 * シリアルナンバーを取得できなければgetDeviceKeyNameと同じ
		 * @return
		 */
		public String getDeviceKeyNameWithSerial() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), true,
				mInfo.serial, mInfo.manufacturer, mInfo.configCounts, mInfo.version);
		}

		/**
		 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを整数として取得
		 * getDeviceKeyNameWithSerialで得られる文字列のhasCodeを取得
		 * シリアルナンバーを取得できなければgetDeviceKeyと同じ
		 * @return
		 */
		public int getDeviceKeyWithSerial() {
			return getDeviceKeyNameWithSerial().hashCode();
		}

		/**
		 * UsbDeviceConnectionを取得
		 * UsbControlBlockでの排他制御から切り離されてしまうので注意
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized UsbDeviceConnection getConnection()
			throws IllegalStateException {
	
			checkConnection();
			return mConnection;
		}

		/**
		 * Usb機器へアクセスするためのファイルディスクリプタを取得
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int getFileDescriptor() throws IllegalStateException {
			checkConnection();
			return mConnection.getFileDescriptor();
		}

		/**
		 * Usb機器のディスクリプタを取得
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized byte[] getRawDescriptors() throws IllegalStateException {
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

		public int getBusNum() {
			return mBusNum;
		}

		public int getDevNum() {
			return mDevNum;
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
		 */
		public synchronized void claimInterface(final UsbInterface intf) {
			claimInterface(intf, true);
		}

		public synchronized void claimInterface(final UsbInterface intf, final boolean force) {
			checkConnection();
			mConnection.claimInterface(intf, force);
		}

		/**
		 * インターフェースを閉じる
		 * @param intf
		 * @throws IllegalStateException
		 */
		public synchronized void releaseInterface(final UsbInterface intf) throws IllegalStateException {
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
		public synchronized void close() {
//			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

			if (mConnection != null) {
				// 2015/01/06 closeしてからonDisconnectを呼び出すように変更
				// openしているinterfaceが有れば閉じる XXX Java側でインターフェースを使う時
				final int n = mInterfaces.size();
				for (int i = 0; i < n; i++) {
					final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
					if (intfs != null) {
						final int m = intfs.size();
						for (int j = 0; j < m; j++) {
							final UsbInterface intf = intfs.valueAt(j);
							mConnection.releaseInterface(intf);
						}
						intfs.clear();
					}
				}
				mInterfaces.clear();
				mConnection.close();
				mConnection = null;
				final USBMonitor monitor = mWeakMonitor.get();
				if (monitor != null) {
					if (monitor.mOnDeviceConnectListener != null) {
						monitor.mOnDeviceConnectListener.onDisconnect(mWeakDevice.get(), UsbControlBlock.this);
					}
					monitor.mCtrlBlocks.remove(getDevice());	// 2014/09/22 追加 mCtrlBlocksから削除する
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

//		@Override
//		protected void finalize() throws Throwable {
///			close();
//			super.finalize();
//		}

		private synchronized void checkConnection() throws IllegalStateException {
			if (mConnection == null) {
				throw new IllegalStateException("already closed");
			}
		}
	}

	private static boolean hasPermission(final UsbManager manager, final UsbDevice device) {
		boolean hasPermission = false;
		if (null != manager && null != device) {
			try {
				hasPermission = manager.hasPermission(device);
			} catch (Throwable e) {
				Log.w(TAG, e);
			}
		}
		return hasPermission;
	}

	private static HashMap<String, UsbDevice> getDeviceList(UsbManager usbManager) {
		HashMap<String, UsbDevice> list = null;
		if (null != usbManager) {
			try {
				list = usbManager.getDeviceList();
			} catch (Throwable e) {
				Log.w(TAG, e);
			}
		}
		return list;
	}

//	private void requestPermissionDialog(final Context context, final UsbDevice device, final String packageName, PendingIntent pi) {
//		final int uid = Binder.getCallingUid();
//
//		// compare uid with packageName to foil apps pretending to be someone else
//		try {
//			final ApplicationInfo aInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
//			if (aInfo.uid != uid) {
//				throw new IllegalArgumentException("package " + packageName +
//					" does not match caller's uid " + uid);
//			}
//		} catch (PackageManager.NameNotFoundException e) {
//			throw new IllegalArgumentException("package " + packageName + " not found");
//		}
//
//		final long identity = Binder.clearCallingIdentity();
//		final Intent intent = new Intent();
//		intent.setClassName("com.android.systemui",
//          "com.android.systemui.usb.UsbPermissionActivity");
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		intent.putExtra(Intent.EXTRA_INTENT, pi);
//		intent.putExtra("package", packageName);
//		intent.putExtra(Intent.EXTRA_UID, uid);
//		intent.putExtra(UsbManager.EXTRA_DEVICE, device);
//		try {
//			// システムアプリにしてandroid.permission.MANAGE_USBパーミッションが無いと呼び出せない
//			context.startActivity(intent);
//		} catch (ActivityNotFoundException e) {
//			Log.e(TAG, "unable to start UsbPermissionActivity");
//		} finally {
//			Binder.restoreCallingIdentity(identity);
//		}
//	}
}