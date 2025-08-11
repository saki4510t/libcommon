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
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.system.ContextUtils;
import com.serenegiant.utils.BufferHelper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * USB機器をopen/closeしてアクセスするためのヘルパークラス
 * USBMonitor#UsbControlBlockのうちUSBMonitorに関係する部分を省いた
 * USB機器アクセス部分だけのヘルパークラス
 */
public class UsbConnector implements Cloneable, Closeable {
   private static final boolean DEBUG = false;	// XXX 実働時にはfalseにすること
   private static final String TAG = "UsbConnector";

   @NonNull
   private final UsbManager mUsbManager;
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
    * コンストラクタ
    * 内部でUsbManager#openDeviceを呼び出すのでUSB機器アクセスパーミッションが無いと
    * IOExceptionを生成する
    * @param context
    * @param device
    * @throws IOException
    */
   public UsbConnector(@NonNull final Context context, @NonNull final UsbDevice device) throws IOException {
      this(ContextUtils.requireSystemService(context, UsbManager.class), device);
   }

   /**
    * コンストラクタ
    * 内部でUsbManager#openDeviceを呼び出すのでUSB機器アクセスパーミッションが無いと
    * IOExceptionを生成する
    * @param manager
    * @param device
    * @throws IOException
    */
   public UsbConnector(
      @NonNull final UsbManager manager,
      @NonNull final UsbDevice device) throws IOException {

      mUsbManager = manager;
      mDevice = device;
      try {
         mConnection = manager.openDevice(device);
      } catch (final Exception e) {
         throw new IOException(e);
      }
      final String name = device.getDeviceName();
      if (mConnection != null) {
         if (DEBUG) {
            final int fd = mConnection.getFileDescriptor();
            final byte[] rawDesc = mConnection.getRawDescriptors();
            Log.v(TAG, String.format(Locale.US,
                "name=%s,fd=%d,rawDesc=", name, fd)
                + BufferHelper.toHexString(rawDesc, 0, 16));
         }
      } else {
         // 多分ここには来ない(openDeviceの時点でIOException)けど念のために
         throw new IOException("could not connect to device " + name);
      }
      mInfo = UsbDeviceInfo.getDeviceInfo(mConnection, device, null);
   }

   /**
    * コピーコンストラクタ
    * 単純コピー(参照を共有)ではなく同じUsbDeviceへアクセスするための別のUsbDeviceConnection/UsbDeviceInfoを生成する
    * @param src
    * @throws IllegalStateException
    */
   private UsbConnector(@NonNull final UsbConnector src) throws IOException {
      final UsbDevice device = src.getDevice();
      final String name = device.getDeviceName();
      mUsbManager = src.mUsbManager;
      mDevice = device;
      // コピー元オブジェクトをcloseしてもこのオブジェクトがcloseしないように別途openし直す
      mConnection = mUsbManager.openDevice(device);
      if (mConnection == null) {
         throw new IOException("could not connect to device " + name);
      }
      mInfo = src.mInfo;
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
    * デバイスを閉じる
    * Java内でインターフェースをopenして使う時は開いているインターフェースも閉じる
    */
   @Override
   public void close() {
      if (DEBUG) Log.i(TAG, "close:");

      UsbDeviceConnection connection;
      synchronized (this) {
         connection = mConnection;
         mConnection = null;
      }
      if (connection != null) {
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
      }
   }

   /**
    * クローンで複製する。
    * 別途openし直すのでパーミッションが既に無いと失敗する。
    * @return
    * @throws CloneNotSupportedException
    */
   @NonNull
   @Override
   public UsbConnector clone() throws CloneNotSupportedException {
      // super#cloneはシャローコピー
      final UsbConnector result = (UsbConnector)super.clone();
      // このオブジェクトをcloseしてもcloneしたオブジェクトがcloseしないように別途openし直す
      mConnection = mUsbManager.openDevice(getDevice());
      if (mConnection == null) {
         throw new CloneNotSupportedException("could not connect to device " + getDeviceName());
      }

      return result;
   }

   /**
    * 対応するUSB機器がopenしていて使用可能かどうかを取得
    * @return
    */
   public synchronized boolean isValid() {
      return mConnection != null;
   }

   @NonNull
   public UsbManager getUsbManager() {
      return mUsbManager;
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
    * UsbConnectorでの排他制御から切り離されてしまうので注意
    * @return
    */
   @Nullable
   public synchronized UsbDeviceConnection getConnection() {
      return mConnection;
   }

   /**
    * UsbDeviceConnectionを取得
    * UsbConnectorでの排他制御から切り離されてしまうので注意
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
   public int getVendorId() {
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
    * @param interfaceId
    * @throws IllegalStateException
    */
   public synchronized UsbInterface getInterface(final int interfaceId)
       throws IllegalStateException {

      return getInterface(interfaceId, 0);
   }

   /**
    * インターフェースを取得する
    * @param interfaceId
    * @param altSetting
    * @return
    * @throws IllegalStateException
    */
   @SuppressLint("NewApi")
   public synchronized UsbInterface getInterface(final int interfaceId, final int altSetting)
       throws IllegalStateException {

      checkConnection();
      SparseArray<UsbInterface> intfs = mInterfaces.get(interfaceId);
      if (intfs == null) {
         intfs = new SparseArray<UsbInterface>();
         mInterfaces.put(interfaceId, intfs);
      }
      UsbInterface intf = intfs.get(altSetting);
      if (intf == null) {
         final int n = mDevice.getInterfaceCount();
         for (int i = 0; i < n; i++) {
            final UsbInterface temp = mDevice.getInterface(i);
            if ((temp.getId() == interfaceId) && (temp.getAlternateSetting() == altSetting)) {
               intf = temp;
               break;
            }
         }
         if (intf != null) {
            intfs.append(altSetting, intf);
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
   public synchronized int bulkTransfer(
      final UsbEndpoint endpoint,
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
   public synchronized int bulkTransfer(
      final UsbEndpoint endpoint,
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
   public synchronized int controlTransfer(
      final int requestType, final int request,
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
   public synchronized int controlTransfer(
      final int requestType, final int request,
      final int value, final int index,
      final byte[] buffer, final int length, final int timeout)
         throws IllegalStateException {

      checkConnection();
      return mConnection.controlTransfer(requestType, request,
          value, index, buffer, length, timeout);
   }

   @Override
   public boolean equals(final Object o) {
      if (o == null) return false;
      if (o instanceof UsbConnector) {
         final UsbDevice device = ((UsbConnector) o).getDevice();
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
}
