package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
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

import com.serenegiant.utils.BufferHelper;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

	/** USB機器の接続状態を保持 */
	private  final ConcurrentHashMap<UsbDevice, UsbDeviceState>
		mDeviceStates = new ConcurrentHashMap<>();

	private final WeakReference<Context> mWeakContext;
	private final UsbManager mUsbManager;
	@NonNull
	private final OnDeviceConnectListener mOnDeviceConnectListener;
	private PendingIntent mPermissionIntent = null;
	private final List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

	/**
	 * コールバックをワーカースレッドで呼び出すためのハンドラー
	 */
	private final Handler mAsyncHandler;
	private volatile boolean destroyed;

	/**
	 * コンストラクタ
	 * @param context
	 * @param listener
	 */
	public USBMonitor(@NonNull final Context context,
		@NonNull final OnDeviceConnectListener listener) {

		if (DEBUG) Log.v(TAG, "USBMonitor:コンストラクタ");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		mOnDeviceConnectListener = listener;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		destroyed = false;
		if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
	}

	/**
	 * 破棄処理
	 * 一旦destroyを呼ぶと再利用は出来ない
	 */
	public void destroy() {
		if (DEBUG) Log.i(TAG, "destroy:");
		unregister();
		if (!destroyed) {
			destroyed = true;
			mAsyncHandler.removeCallbacksAndMessages(null);
			// モニターしているUSB機器を全てcloseする
			final Set<UsbDevice> keys = mDeviceStates.keySet();
			if (keys != null) {
				try {
					for (final UsbDevice key: keys) {
						final UsbDeviceState state = mDeviceStates.remove(key);
						if (state != null) {
							state.close();
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "destroy:", e);
				}
			}
			mDeviceStates.clear();
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
			if (DEBUG) Log.i(TAG, "register:");
			final Context context = mWeakContext.get();
			if (context != null) {
				mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
				final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
				if (BuildCheck.isAndroid5()) {
					filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);	// SC-06Dはこのactionが来ない
				}
				filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
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
					for (final DeviceFilter filter: mDeviceFilters) {
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
	 * 指定したデバイス名に対応するUsbDeviceを取得する
	 * @param name　UsbDevice#getDeviceNameで取得できる値
	 * @return 見つからなければnull
	 */
	@Nullable
	public UsbDevice findDevice(final String name) {
		UsbDevice result = null;
		final List<UsbDevice> devices = getDeviceList();
		for (final UsbDevice device: devices) {
			if (device.getDeviceName().equals(name)) {
				result = device;
				break;
			}
		}
		return result;
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
	 * 接続されているUSBの機器リストをLogCatに出力
	 */
	public final void dumpDevices() {
		final HashMap<String, UsbDevice> list = mUsbManager != null ? mUsbManager.getDeviceList() : null;
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
			&& updateDeviceState(device, device != null && mUsbManager.hasPermission(device));
	}

	/**
	 * 内部で保持しているパーミッション状態を更新
	 * @param device
	 * @param hasPermission
	 * @return hasPermission
	 */
	private boolean updateDeviceState(final UsbDevice device, final boolean hasPermission) {
		if (DEBUG) Log.v(TAG, "updateDeviceState:");
		if (device != null) {
			requireDeviceState(device);
		}
		return hasPermission;
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

	/**
	 * 指定したUsbDeviceをopenする
	 * @param device
	 * @return
	 * @throws SecurityException パーミッションがなければSecurityExceptionを投げる
	 */
	public UsbControlBlock openDevice(final UsbDevice device) throws IOException {
		if (DEBUG) Log.v(TAG, "openDevice:device=" + device);
		if (hasPermission(device)) {
			final UsbDeviceState state = requireDeviceState(device);
			if (state.mCtrlBlock == null) {
				state.mCtrlBlock = new UsbControlBlock(USBMonitor.this, device);    // この中でopenDeviceする
			}
			return state.mCtrlBlock;
		} else {
			throw new IOException("has no permission or invalid UsbDevice(already disconnected?)");
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
		if (hasPermission(device) && mDeviceStates.containsKey(device)) {
			return mDeviceStates.get(device).isOpened();
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
					final UsbDeviceState state = mDeviceStates.remove(device);
					if (state != null) {
						// デバイスとの通信をクリーンアップして閉じるためのメソッドを呼び出す
						state.close();
					}
					processDettach(device);
				}
			}
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
			// 以前接続されていたはずの機器
			final Collection<UsbDevice> prevDevices = mDeviceStates.keySet();
			if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable:prev=" + prevDevices.size());
			// 現在の接続機器に含まれていないものを取り除く
			for (final UsbDevice key: prevDevices) {
				if (!currentDevices.contains(key)) {
					if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable#remove " + key);
					final UsbDeviceState state = mDeviceStates.remove(key);
					if (state != null) {
						state.close();
					}
				}
			}
			// 現在は接続されているが以前は接続されていなかった機器を探す
			final List<UsbDevice> mChanged = new ArrayList<>();
			for (final UsbDevice device: currentDevices) {
				if (!prevDevices.contains(device)) {
					mChanged.add(device);
					hasPermission(device);
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
			if (!BuildCheck.isAndroid5()) {
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
		if (hasPermission(device)) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
//					if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
					final boolean createNew;
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
		updateDeviceState(device, false);
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
		hasPermission(device);
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onAttach(device);
			}
		});
	}

	/**
	 * 端末からUSB機器が取り外されたときの処理
	 * @param device
	 */
	private final void processDettach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processDettach:");
		updateDeviceState(device, false);
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onDetach(device);
			}
		});
	}

	/**
	 * USB機器との接続がcloseされたときの処理
	 * @param device
	 */
	private void callOnDisconnect(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "callOnDisconnect:");
		final UsbDeviceState state = getDeviceState(device);
		if (state != null) {
			state.mCtrlBlock = null;
		}
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnDeviceConnectListener.onDisconnect(device);
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
	 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを取得する
	 * シリアルナンバーを取得できなければgetDeviceKeyNameと同じ
	 * @param context
	 * @param device
	 * @return
	 */
	public static String getDeviceKeyNameWithSerial(final Context context, final UsbDevice device) {
		final UsbDeviceInfo info = getDeviceInfo(context, device);
		return UsbUtils.getDeviceKeyName(device, true, info.serial, info.manufacturer, info.configCounts, info.version);
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

	/**
	 * 機器情報保持のためのヘルパークラス
	 */
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

		@NonNull
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
	private static String getString(@NonNull final UsbDeviceConnection connection,
		final int id, final int languageCount, final byte[] languages) {

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
	public static UsbDeviceInfo getDeviceInfo(@NonNull final Context context, final UsbDevice device) {
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
	public static UsbDeviceInfo updateDeviceInfo(@Nullable final UsbManager manager,
		final UsbDevice device, final UsbDeviceInfo _info) {

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
			if ((manager != null) && manager.hasPermission(device)) {
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
		private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

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
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			final String name = device.getDeviceName();
//			final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
//			int busnum = 0;
//			int devnum = 0;
//			if (v != null) {
//				busnum = Integer.parseInt(v[v.length-2]);
//				devnum = Integer.parseInt(v[v.length-1]);
//			}
///			mBusNum = busnum;
//			mDevNum = devnum;
			if (mConnection != null) {
//				if (DEBUG) {
					final int desc = mConnection.getFileDescriptor();
					final byte[] rawDesc = mConnection.getRawDescriptors();
					Log.i(TAG, String.format(Locale.US,
						"name=%s,desc=%d,rawDesc=", name, desc)
							+ BufferHelper.toHexString(rawDesc, 0, 16));
//				}
			} else {
				throw new IOException("could not connect to device " + name);
			}
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
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			// FIXME USBMonitor.mCtrlBlocksに追加する(今はHashMapなので追加すると置き換わってしまうのでだめ, ListかHashMapにListをぶら下げる?)
			monitor.processConnect(device, this);
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
			return UsbUtils.getDeviceKeyName(mWeakDevice.get());
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
			return UsbUtils.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI);
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
			return UsbUtils.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI, useNonce);
		}

		/**
		 * デバイスキーを整数として取得
		 * getDeviceKeyNameで得られる文字列のhasCodeを取得
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey() throws IllegalStateException {
			checkConnection();
			return UsbUtils.getDeviceKey(mWeakDevice.get());
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
			return UsbUtils.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI);
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
			return UsbUtils.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI, useNonce);
		}

		/**
		 * シリアルナンバーを取得できる機器の場合にはシリアルナンバーを含めたデバイスキーを取得する
		 * シリアルナンバーを取得できなければgetDeviceKeyNameと同じ
		 * @return
		 */
		public String getDeviceKeyNameWithSerial() {
			return UsbUtils.getDeviceKeyName(mWeakDevice.get(), true,
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
			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

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
					monitor.callOnDisconnect(getDevice());
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
	}

	@Nullable
	private UsbDeviceState getDeviceState(@Nullable final UsbDevice device) {
		return mDeviceStates.containsKey(device) ? mDeviceStates.get(device) : null;
	}

	@NonNull
	private UsbDeviceState requireDeviceState(@NonNull final UsbDevice device) {
		UsbDeviceState state = mDeviceStates.containsKey(device)
			? mDeviceStates.get(device) : null;
		if (state == null) {
			state = new UsbDeviceState(device);
			mDeviceStates.put(device, state);
		}
		return state;
	}

	/**
	 * USB機器の接続状態を保持するためのクラス
	 */
	private static class UsbDeviceState {
		@NonNull
		private final UsbDevice mDevice;
		private UsbControlBlock mCtrlBlock;

		private UsbDeviceState(@NonNull final UsbDevice device) {
			mDevice = device;
		}

		@NonNull
		private UsbDevice getDevice() {
			return mDevice;
		}

		private void close() {
			if (mCtrlBlock != null) {
				mCtrlBlock.close();
				mCtrlBlock = null;
			}
		}

		private boolean isOpened() {
			return mCtrlBlock != null;
		}
	}
}