package com.serenegiant.libcommon;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsbFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = UsbFragment.class.getSimpleName();

	public static UsbFragment newInstance() {
		final UsbFragment fragment = new UsbFragment();
		final Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

//================================================================================
	private USBMonitor mUSBMonitor;

	public UsbFragment() {
		super();
	}

	@Override
	public void onAttach(@NonNull final Context context) {
		super.onAttach(context);
		if (DEBUG) Log.v(TAG, "onAttach:" + mUSBMonitor);
		if (mUSBMonitor == null) {
			mUSBMonitor = new USBMonitor(context, mOnDeviceConnectListener);
			final List<DeviceFilter> filters
				= DeviceFilter.getDeviceFilters(context, R.xml.device_filter_exclude);
			mUSBMonitor.setDeviceFilter(filters);
		}

	}

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		if ((mUSBMonitor != null) && !mUSBMonitor.isRegistered()) {
			if (DEBUG) Log.v(TAG, "onCreate:register USBMonitor");
			mUSBMonitor.register();
		}
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		final LayoutInflater custom_inflater
			= getThemedLayoutInflater(inflater, R.style.AppTheme_Usb);
		final View rootView
			= custom_inflater.inflate(R.layout.fragment_usb, container, false);

		return rootView;
	}

	@Override
	protected void internalOnResume() {
		super.internalOnResume();
		if (DEBUG) Log.v(TAG, "internalOnResume:");
	}

	@Override
	protected void internalOnPause() {
		if (DEBUG) Log.v(TAG, "internalOnPause:");
		super.internalOnPause();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		try {
			if ((mUSBMonitor != null) && mUSBMonitor.isRegistered()) {
				if (DEBUG) Log.v(TAG, "onDestroy:unregister USBMonitor");
				mUSBMonitor.unregister();
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:");
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		super.onDetach();
	}

//--------------------------------------------------------------------------------

	private final USBMonitor.OnDeviceConnectListener
		mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {

		@Override
		public void onAttach(@NonNull final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onAttach:");
			// USB機器が接続された時
		}

		@Override
		public void onPermission(@NonNull final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onAttach:");
			// パーミッションを取得できた時
		}

		@Override
		public void onConnected(@NonNull final UsbDevice device, @NonNull final USBMonitor.UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onConnected:");
			// USB機器がopenした時
		}

		@Override
		public void onDisconnect(@NonNull final UsbDevice device) {

			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onDisconnect:");
			// USB機器がcloseした時
		}

		@Override
		public void onDetach(@NonNull final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "OnDeviceConnectListener:onDetach:");
			// USB機器が取り外された時
		}

		@Override
		public void onCancel(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onCancel:");
			// パーミッションを取得できなかった時
		}

		@Override
		public void onError(final UsbDevice usbDevice, final Throwable throwable) {
			Log.w(TAG, throwable);
		}
	};

}
