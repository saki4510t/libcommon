package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import android.media.AudioDeviceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class AudioDeviceUtils {
	private AudioDeviceUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * AudioDeviceInfo#getTypeを文字列化
	 * @param info
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	public static String getAudioTypeString(@NonNull final AudioDeviceInfo info) {
		switch (info.getType()) {
		case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return "TYPE_BUILTIN_EARPIECE";
		case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "TYPE_BUILTIN_SPEAKER";
		case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "TYPE_WIRED_HEADSET";
		case AudioDeviceInfo.TYPE_WIRED_HEADPHONES: return "TYPE_WIRED_HEADPHONES";
		case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "TYPE_BLUETOOTH_SCO";
		case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return "TYPE_BLUETOOTH_A2DP";
		case AudioDeviceInfo.TYPE_HDMI: return "TYPE_HDMI";
		case AudioDeviceInfo.TYPE_DOCK: return "TYPE_DOCK";
		case AudioDeviceInfo.TYPE_USB_ACCESSORY: return "TYPE_USB_ACCESSORY";
		case AudioDeviceInfo.TYPE_USB_DEVICE: return "TYPE_USB_DEVICE";
		case AudioDeviceInfo.TYPE_USB_HEADSET: return "TYPE_USB_HEADSET";
		case AudioDeviceInfo.TYPE_TELEPHONY: return "TYPE_TELEPHONY";
		case AudioDeviceInfo.TYPE_LINE_ANALOG: return "TYPE_LINE_ANALOG";
		case AudioDeviceInfo.TYPE_HDMI_ARC: return "TYPE_HDMI_ARC";
		case AudioDeviceInfo.TYPE_HDMI_EARC: return "TYPE_HDMI_EARC";
		case AudioDeviceInfo.TYPE_LINE_DIGITAL: return "TYPE_LINE_DIGITAL";
		case AudioDeviceInfo.TYPE_FM: return "TYPE_FM";
		case AudioDeviceInfo.TYPE_AUX_LINE: return "TYPE_AUX_LINE";
		case AudioDeviceInfo.TYPE_IP: return "TYPE_IP";
		case AudioDeviceInfo.TYPE_BUS: return "TYPE_BUS";
		case AudioDeviceInfo.TYPE_HEARING_AID: return "TYPE_HEARING_AID";
		case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "TYPE_BUILTIN_MIC";
		case AudioDeviceInfo.TYPE_FM_TUNER: return "TYPE_FM_TUNER";
		case AudioDeviceInfo.TYPE_TV_TUNER: return "TYPE_TV_TUNER";
		case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE: return "TYPE_BUILTIN_SPEAKER_SAFE";
		case AudioDeviceInfo.TYPE_REMOTE_SUBMIX: return "TYPE_REMOTE_SUBMIX";
		case AudioDeviceInfo.TYPE_BLE_HEADSET: return "TYPE_BLE_HEADSET";
		case AudioDeviceInfo.TYPE_BLE_SPEAKER: return "TYPE_BLE_SPEAKER";
		case 28: return "TYPE_ECHO_REFERENCE";	// TYPE_ECHO_REFERENCEがエラーになるので即値で指定
		case AudioDeviceInfo.TYPE_BLE_BROADCAST: return "TYPE_BLE_BROADCAST";
		default:
			return "UNKNOWN" + info.getType();
		}
	}

}
