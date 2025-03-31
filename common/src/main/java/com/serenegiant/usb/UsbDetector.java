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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

/**
 * USBMonitorからUSB機器の接続・切断イベントに関する処理を分離
 */
public class UsbDetector {
	private static final boolean DEBUG = false;	// XXX 実働時にはfalseにすること
	private static final String TAG = "UsbDetector";

	/**
	 * USB機器の状態変更時のコールバックリスナー
	 */
	public interface Callback {
		/**
		 * USB機器が取り付けられたか電源が入った時
		 * @param device
		 */
		@AnyThread
		public void onAttach(@NonNull final UsbDevice device);
		/**
		 * USB機器が取り外されたか電源が切られた時(open中であればonDisconnectの後に呼ばれる)
		 * @param device
		 */
		@AnyThread
		public void onDetach(@NonNull final UsbDevice device);
		/**
		 * パーミッション要求時等で非同期実行中にエラーになった時
		 * @param device
		 * @param t
		 */
		@AnyThread
		public void onError(@Nullable final UsbDevice device, @NonNull final Throwable t);
	}

//--------------------------------------------------------------------------------
	@NonNull
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final UsbManager mUsbManager;
	@NonNull
	private final Callback mCallback;
	@NonNull
	private final List<DeviceFilter> mDeviceFilters = new ArrayList<>();
	/**
	 * 現在接続されている機器一覧
	 */
	@NonNull
	private final Set<UsbDevice> mAttachedDevices = new HashSet<>();
	/**
	 * コールバックをワーカースレッドで呼び出すためのハンドラー
	 */
	private final Handler mAsyncHandler;

	private volatile boolean mReleased;
	@Nullable
	private BroadcastReceiver mUsbReceiver;
	/**
	 * ポーリングで接続されているUSB機器の変化をチェックするかどうか
	 * Android5以上ではデフォルトはfalseでregister直後を覗いてポーリングしない
	 */
	private boolean mEnablePolling = !BuildCheck.isAndroid5();
	/**
	 * ポーリングの周期[ミリ秒]
	 */
	private long mPollingIntervalsMs = 1000L;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 */
	public UsbDetector(@NonNull final Context context,
		@NonNull final Callback callback) {

		if (DEBUG) Log.v(TAG, "コンストラクタ");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = ContextUtils.requireSystemService(context, UsbManager.class);
		mCallback = callback;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		mReleased = false;
		if (DEBUG) Log.v(TAG, "mUsbManager=" + mUsbManager);
	}

	/**
	 * 関係するリソースを破棄する。再利用はできない
	 */
	public void release() {
		if (DEBUG) Log.i(TAG, "release:");
		unregister();
		if (!mReleased) {
			mReleased = true;
			mAsyncHandler.removeCallbacksAndMessages(null);
			HandlerUtils.NoThrowQuit(mAsyncHandler);
			mWeakContext.clear();
		}
	}

	/**
	 * すでに破棄されたかどうかを取得
	 * @return
	 */
	public boolean isReleased() {
		return mReleased;
	}

	protected Context getContext() {
		return mWeakContext.get();
	}

	protected Context requireContext() throws IllegalStateException {
		final Context result = getContext();
		if (mReleased || (result == null)) {
			throw new IllegalStateException("already released!");
		}
		return result;
	}

	@NonNull
	protected UsbManager getUsbManager() {
		return mUsbManager;
	}

	public Handler getHandler() throws IllegalStateException {
		if (isReleased()) {
			throw new IllegalStateException("already released");
		}
		return mAsyncHandler;
	}

	protected void post(@NonNull final Runnable task) {
		if (!isReleased()) {
			mAsyncHandler.post(task);
		}
	}
//--------------------------------------------------------------------------------
	/**
	 * 接続/切断およびパーミッション要求に成功した時のブロードキャストを受信するためのブロードキャストレシーバーを登録する
	 * @throws IllegalStateException
	 */
	@SuppressLint({"InlinedApi", "WrongConstant"})
	public synchronized void register() throws IllegalStateException {
		if (mReleased) throw new IllegalStateException("already destroyed");
		if (mUsbReceiver == null) {
			if (DEBUG) Log.i(TAG, "register:");
			final Context context = requireContext();
			mUsbReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(final Context context, final Intent intent) {
					UsbDetector.this.onReceive(context, intent);
				}
			};
			final IntentFilter filter = createIntentFilter();
			ContextCompat.registerReceiver(context, mUsbReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
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
		if (!mReleased) {
			mAsyncHandler.removeCallbacksAndMessages(null);
		}
		if (mUsbReceiver != null) {
			if (DEBUG) Log.i(TAG, "unregister:");
			final Context context = mWeakContext.get();
			try {
				if (context != null) {
					context.unregisterReceiver(mUsbReceiver);
				}
			} catch (final Exception e) {
				// ignore
			}
			mUsbReceiver = null;
		}
		synchronized (mAttachedDevices) {
			mAttachedDevices.clear();
		}
	}

	public synchronized boolean isRegistered() {
		return !mReleased && (mUsbReceiver != null);
	}

	/**
	 * ブロードキャスト受信用のIntentFilterを生成する
	 * @return
	 */
	protected IntentFilter createIntentFilter() {
		final IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		if (BuildCheck.isAndroid5()) {
			filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);	// SC-06Dはこのactionが来ない
		}
		return filter;
	}

//--------------------------------------------------------------------------------
	/**
	 * デバイスフィルターを設定
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(@Nullable final DeviceFilter filter)
		throws IllegalStateException {

		if (mReleased) throw new IllegalStateException("already destroyed");
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

		if (mReleased) throw new IllegalStateException("already destroyed");
		mDeviceFilters.add(filter);
	}

	/**
	 * デバイスフィルターを削除
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void removeDeviceFilter(@Nullable final DeviceFilter filter)
		throws IllegalStateException {

		if (mReleased) throw new IllegalStateException("already destroyed");
		mDeviceFilters.remove(filter);
	}

	/**
	 * set device filters
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(@Nullable final List<DeviceFilter> filters)
		throws IllegalStateException {

		if (mReleased) throw new IllegalStateException("already destroyed");
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

		if (mReleased) throw new IllegalStateException("already destroyed");
		mDeviceFilters.addAll(filters);
	}

	/**
	 * remove device filters
	 * @param filters
	 */
	public void removeDeviceFilter(final List<DeviceFilter> filters)
		throws IllegalStateException {

		if (mReleased) throw new IllegalStateException("already destroyed");
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
		final List<UsbDevice> result = new ArrayList<>();
		if (mReleased) return result;
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
	protected boolean matches(@NonNull final UsbDevice device) {
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
	 * 以前接続されていたが現在は接続されていないUSB機器があればデタッチイベントを起こす
	 */
	public void refreshDevices() {
		final List<UsbDevice> currentDevices = getDeviceList();
		// 現在は接続されていいないが以前は接続されていた機器を探す
		// アプリがポーズ中/バックグラウンド中にUSB機器が取り外された時のため
		final Collection<UsbDevice> prevDevices;
		synchronized (mAttachedDevices) {
			prevDevices = new HashSet<>(mAttachedDevices);
			mAttachedDevices.clear();
			mAttachedDevices.addAll(currentDevices);
		}
		// ポーズ中/バックグラウンド中に取り外されたUSB機器を探す
		final List<UsbDevice> removed = new ArrayList<>();
		for (final UsbDevice device: prevDevices) {
			if (!currentDevices.contains(device)) {
				removed.add(device);
			}
		}
		// 取り外された機器に対してデタッチイベントを起こす
		for (final UsbDevice device: removed) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onDetach(device);
				}
			});
		}
		// 現在接続されているUSB機器全てに対してアタッチイベントを起こす
		for (final UsbDevice device: currentDevices) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onAttach(device);
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
	public void setEnablePolling(final boolean enable) {
		setEnablePolling(enable, mPollingIntervalsMs);
	}

	/**
	 * ポーリングによる接続機器のチェックを有効にするかどうかを設定
	 * @param enable
	 * @param intervalsMs ポーリング周期[ミリ秒], 100未満の場合は1000ミリ秒
	 */
	public synchronized void setEnablePolling(final boolean enable, final long intervalsMs) {
		mPollingIntervalsMs = (intervalsMs >= 100) ? intervalsMs : 1000L;
		if (mEnablePolling != enable) {
			mEnablePolling = enable;
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
			if (enable && isRegistered()) {
				mAsyncHandler.postDelayed(mDeviceCheckRunnable, 500L);
			}
		}
	}

	/**
	 * 古い一部機種向けのポーリングで接続機器をチェックするためのRunnable
	 * 定期的に接続しているデバイスを確認して数が変更されていればonAttachを呼び出す
	 */
	private final Runnable mDeviceCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (mReleased) return;
			if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable#run");
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
			// 現在接続されている機器
			final List<UsbDevice> currentDevices = getDeviceList();
			if (DEBUG) Log.v(TAG, "mDeviceCheckRunnable:current=" + currentDevices.size());
			final Collection<UsbDevice> prevDevices;
			synchronized (mAttachedDevices) {
				prevDevices = new HashSet<>(mAttachedDevices);
				mAttachedDevices.clear();
				mAttachedDevices.addAll(currentDevices);
			}
			// 現在は接続されていいないが以前は接続されていた機器を探す
			// アプリがポーズ中/バックグラウンド中にUSB機器が取り外された時のため
			final List<UsbDevice> removed = new ArrayList<>();
			for (final UsbDevice device: prevDevices) {
				if (!currentDevices.contains(device)) {
					removed.add(device);
				}
			}
			// 現在は接続されているが以前は接続されていなかった機器を探す
			// アプリ起動前に接続されたUSB機器の検出のため
			final List<UsbDevice> added = new ArrayList<>();
			for (final UsbDevice device: currentDevices) {
				if (!prevDevices.contains(device)) {
					added.add(device);
				}
			}
			// 取り外された機器に対してデタッチイベントを起こす
			for (final UsbDevice device: removed) {
				mAsyncHandler.post(new Runnable() {
					@Override
					public void run() {
						mCallback.onDetach(device);
					}
				});
			}
			// 新たに追加された機器に対してアタッチイベントを起こす
			for (final UsbDevice device: added) {
				mAsyncHandler.post(new Runnable() {
					@Override
					public void run() {
						mCallback.onAttach(device);
					}
				});
			}
			if (mEnablePolling) {
				mAsyncHandler.postDelayed(mDeviceCheckRunnable, mPollingIntervalsMs);	// 1秒に1回確認
			}
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * パーミッション取得・USB機器のモニター用のBroadcastReceiverの処理の実態
	 * @param context
	 * @param intent
	 */
	protected void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			// デバイスが取り付けられた時の処理・・・SC-06DはこのActionが来ない.ACTION_USB_DEVICE_DETACHEDは来る
			// Nexus7/5はaddActionしてれば来るけど、どのAndroidバージョンから来るのかわからない
			// Android5以降なら大丈夫そう
			final UsbDevice device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice.class);
			if (device != null) {
				processAttach(device);
			} else {
				callOnError(device, new UsbAttachException("device is null"));
			}
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			// デバイスが取り外された時
			final UsbDevice device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice.class);
			if (device != null) {
				processDettach(device);
			} else {
				callOnError(device, new UsbDetachException("device is null"));
			}
		}
	}

	/**
	 * 端末にUSB機器が接続されたときの処理
	 * @param device
	 */
	protected void processAttach(@NonNull final UsbDevice device) {
		if (mReleased) return;
		if (DEBUG) Log.v(TAG, "processAttach:");
		if (matches(device)) {
			// フィルタにマッチした
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onAttach(device);
				}
			});
		}
	}

	/**
	 * 端末からUSB機器が取り外されたときの処理
	 * @param device
	 */
	protected void processDettach(@NonNull final UsbDevice device) {
		if (mReleased) return;
		if (DEBUG) Log.v(TAG, "processDettach:");
		if (matches(device)) {
			// フィルタにマッチした
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mCallback.onDetach(device);
				}
			});
		}
	}

	/**
	 * エラーコールバック呼び出し処理
	 * @param device
	 * @param t
	 */
	protected void callOnError(@Nullable final UsbDevice device,
		@NonNull final Throwable t) {

		if (DEBUG) Log.v(TAG, "callOnError:");
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				mCallback.onError(device, t);
			}
		});
	}

}
