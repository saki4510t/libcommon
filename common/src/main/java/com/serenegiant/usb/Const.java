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

import android.hardware.usb.UsbManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

public interface Const {
	/** android.hardware.usb.action.USB_DEVICE_ATTACHED */
	public static final String ACTION_USB_DEVICE_ATTACHED
		= UsbManager.ACTION_USB_DEVICE_ATTACHED;
	/** android.hardware.usb.action.USB_DEVICE_DETACHED */
	public static final String ACTION_USB_DEVICE_DETACHED
		= UsbManager.ACTION_USB_DEVICE_DETACHED;

	public static final int USB_DIR_OUT = 0;
	public static final int USB_DIR_IN = 0x80;
	public static final int USB_TYPE_MASK = (0x03 << 5);
	public static final int USB_TYPE_STANDARD = (0x00 << 5);
	public static final int USB_TYPE_CLASS = (0x01 << 5);
	public static final int USB_TYPE_VENDOR = (0x02 << 5);
	public static final int USB_TYPE_RESERVED = (0x03 << 5);
	public static final int USB_RECIP_MASK = 0x1f;
	public static final int USB_RECIP_DEVICE = 0x00;
	public static final int USB_RECIP_INTERFACE = 0x01;
	public static final int USB_RECIP_ENDPOINT = 0x02;
	public static final int USB_RECIP_OTHER = 0x03;
	public static final int USB_RECIP_PORT = 0x04;
	public static final int USB_RECIP_RPIPE = 0x05;
	public static final int USB_REQ_GET_STATUS = 0x00;
	public static final int USB_REQ_CLEAR_FEATURE = 0x01;
	public static final int USB_REQ_SET_FEATURE = 0x03;
	public static final int USB_REQ_SET_ADDRESS = 0x05;
	public static final int USB_REQ_GET_DESCRIPTOR = 0x06;
	public static final int USB_REQ_SET_DESCRIPTOR = 0x07;
	public static final int USB_REQ_GET_CONFIGURATION = 0x08;
	public static final int USB_REQ_SET_CONFIGURATION = 0x09;
	public static final int USB_REQ_GET_INTERFACE = 0x0A;
	public static final int USB_REQ_SET_INTERFACE = 0x0B;
	public static final int USB_REQ_SYNCH_FRAME = 0x0C;
	public static final int USB_REQ_SET_SEL = 0x30;
	public static final int USB_REQ_SET_ISOCH_DELAY = 0x31;
	public static final int USB_REQ_SET_ENCRYPTION = 0x0D;
	public static final int USB_REQ_GET_ENCRYPTION = 0x0E;
	public static final int USB_REQ_RPIPE_ABORT = 0x0E;
	public static final int USB_REQ_SET_HANDSHAKE = 0x0F;
	public static final int USB_REQ_RPIPE_RESET = 0x0F;
	public static final int USB_REQ_GET_HANDSHAKE = 0x10;
	public static final int USB_REQ_SET_CONNECTION = 0x11;
	public static final int USB_REQ_SET_SECURITY_DATA = 0x12;
	public static final int USB_REQ_GET_SECURITY_DATA = 0x13;
	public static final int USB_REQ_SET_WUSB_DATA = 0x14;
	public static final int USB_REQ_LOOPBACK_DATA_WRITE = 0x15;
	public static final int USB_REQ_LOOPBACK_DATA_READ = 0x16;
	public static final int USB_REQ_SET_INTERFACE_DS = 0x17;

	public static final int USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_DEVICE);		// 0x10
	public static final int USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE);			// 0x90
	public static final int USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x11
	public static final int USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x91
	public static final int USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);	// 0x12
	public static final int USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);		// 0x92

	public static final int USB_REQ_CS_DEVICE_SET  = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x20
	public static final int USB_REQ_CS_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);					// 0xa0
	public static final int USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0x21
	public static final int USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0xa1
	public static final int USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0x22
	public static final int USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0xa2

	public static final int USB_REQ_VENDER_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x40
	public static final int USB_REQ_VENDER_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0xc0
	public static final int USB_REQ_VENDER_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0x41
	public static final int USB_REQ_VENDER_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0xc1
	public static final int USB_REQ_VENDER_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0x42
	public static final int USB_REQ_VENDER_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0xc2

	public static final int USB_DT_DEVICE = 0x01;
	public static final int USB_DT_CONFIG = 0x02;
	public static final int USB_DT_STRING = 0x03;
	public static final int USB_DT_INTERFACE = 0x04;
	public static final int USB_DT_ENDPOINT = 0x05;
	public static final int USB_DT_DEVICE_QUALIFIER = 0x06;
	public static final int USB_DT_OTHER_SPEED_CONFIG = 0x07;
	public static final int USB_DT_INTERFACE_POWER = 0x08;
	public static final int USB_DT_OTG = 0x09;
	public static final int USB_DT_DEBUG = 0x0a;
	public static final int USB_DT_INTERFACE_ASSOCIATION = 0x0b;
	public static final int USB_DT_SECURITY = 0x0c;
	public static final int USB_DT_KEY = 0x0d;
	public static final int USB_DT_ENCRYPTION_TYPE = 0x0e;
	public static final int USB_DT_BOS = 0x0f;
	public static final int USB_DT_DEVICE_CAPABILITY = 0x10;
	public static final int USB_DT_WIRELESS_ENDPOINT_COMP = 0x11;
	public static final int USB_DT_WIRE_ADAPTER = 0x21;
	public static final int USB_DT_RPIPE = 0x22;
	public static final int USB_DT_CS_RADIO_CONTROL = 0x23;
	public static final int USB_DT_PIPE_USAGE = 0x24;
	public static final int USB_DT_SS_ENDPOINT_COMP = 0x30;
	public static final int USB_DT_CS_DEVICE = (USB_TYPE_CLASS | USB_DT_DEVICE);
	public static final int USB_DT_CS_CONFIG = (USB_TYPE_CLASS | USB_DT_CONFIG);
	public static final int USB_DT_CS_STRING = (USB_TYPE_CLASS | USB_DT_STRING);
	public static final int USB_DT_CS_INTERFACE = (USB_TYPE_CLASS | USB_DT_INTERFACE);
	public static final int USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS | USB_DT_ENDPOINT);
	public static final int USB_DT_DEVICE_SIZE = 18;
	
	// コントロールインターフェースの値取得・設定用の要求コード
	static final int REQ_SET_ = 0x00;
	static final int REQ_GET_ = 0x80;
	static final int REQ__CUR = 0x1;
	static final int REQ__MIN = 0x2;
	static final int REQ__MAX = 0x3;
	static final int REQ__RES = 0x4;
	static final int REQ__MEM = 0x5;
	static final int REQ__LEN = 0x5;
	static final int REQ__INFO = 0x6;
	static final int REQ__DEF = 0x7;

	// native側のreq_code_tに対応する定数
	public static final int REQ_CODE_UNDEFINED = 0x00;
	public static final int REQ_CODE_SET_CUR = (REQ_SET_ | REQ__CUR);	// bmRequestType=0x21
	public static final int REQ_CODE_SET_MIN = (REQ_SET_ | REQ__MIN);
	public static final int REQ_CODE_SET_MAX = (REQ_SET_ | REQ__MAX);
	public static final int REQ_CODE_SET_RES = (REQ_SET_ | REQ__RES);
	public static final int REQ_CODE_SET_LEN = (REQ_SET_ | REQ__LEN);
	public static final int REQ_CODE_SET_MEM = (REQ_SET_ | REQ__MEM);
	public static final int REQ_CODE_SET_INFO = (REQ_SET_ | REQ__INFO);
	public static final int REQ_CODE_SET_DEF = (REQ_SET_ | REQ__DEF);
	//
	public static final int REQ_CODE_GET_CUR = (REQ_GET_ | REQ__CUR);	// bmRequestType=0xa1
	public static final int REQ_CODE_GET_MIN = (REQ_GET_ | REQ__MIN);
	public static final int REQ_CODE_GET_MAX = (REQ_GET_ | REQ__MAX);	// ↑
	public static final int REQ_CODE_GET_RES = (REQ_GET_ | REQ__RES);	// ↑
	public static final int REQ_CODE_GET_LEN = (REQ_GET_ | REQ__LEN);
	public static final int REQ_CODE_GET_MEM = (REQ_GET_ | REQ__MEM);
	public static final int REQ_CODE_GET_INFO = (REQ_GET_ | REQ__INFO);// ↑
	public static final int REQ_CODE_GET_DEF = (REQ_GET_ | REQ__DEF);	// ↑
	//
	public static final int REQ_CODE_GET_START = 0xff;
	
	@IntDef({
		REQ_CODE_SET_CUR,
		REQ_CODE_SET_MIN,
		REQ_CODE_SET_MAX,
		REQ_CODE_SET_RES,
		REQ_CODE_SET_LEN,
		REQ_CODE_SET_INFO,
		REQ_CODE_SET_DEF,
		REQ_CODE_GET_CUR,
		REQ_CODE_GET_MIN,
		REQ_CODE_GET_MAX,
		REQ_CODE_GET_RES,
		REQ_CODE_GET_LEN,
		REQ_CODE_GET_INFO,
		REQ_CODE_GET_DEF,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface RequestCode {}
}
