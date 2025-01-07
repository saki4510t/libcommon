package com.serenegiant.media;
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
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.system.BuildCheck;
import com.serenegiant.system.ContextUtils;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class AudioDeviceUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = AudioDeviceUtils.class.getSimpleName();

	private AudioDeviceUtils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * AudioDeviceInfo#getTypeを文字列化
	 * @param info
	 * @return
	 */
	@SuppressLint("SwitchIntDef")
	@RequiresApi(api = Build.VERSION_CODES.M)
	public static String getAudioTypeString(@NonNull final AudioDeviceInfo info) {
		return switch (info.getType()) {
			case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "TYPE_BUILTIN_EARPIECE";
			case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "TYPE_BUILTIN_SPEAKER";
			case AudioDeviceInfo.TYPE_WIRED_HEADSET -> "TYPE_WIRED_HEADSET";
			case AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "TYPE_WIRED_HEADPHONES";
			case AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "TYPE_BLUETOOTH_SCO";
			case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "TYPE_BLUETOOTH_A2DP";
			case AudioDeviceInfo.TYPE_HDMI -> "TYPE_HDMI";
			case AudioDeviceInfo.TYPE_DOCK -> "TYPE_DOCK";
			case AudioDeviceInfo.TYPE_USB_ACCESSORY -> "TYPE_USB_ACCESSORY";
			case AudioDeviceInfo.TYPE_USB_DEVICE -> "TYPE_USB_DEVICE";
			case AudioDeviceInfo.TYPE_USB_HEADSET -> "TYPE_USB_HEADSET";
			case AudioDeviceInfo.TYPE_TELEPHONY -> "TYPE_TELEPHONY";
			case AudioDeviceInfo.TYPE_LINE_ANALOG -> "TYPE_LINE_ANALOG";
			case AudioDeviceInfo.TYPE_HDMI_ARC -> "TYPE_HDMI_ARC";
			case AudioDeviceInfo.TYPE_HDMI_EARC -> "TYPE_HDMI_EARC";
			case AudioDeviceInfo.TYPE_LINE_DIGITAL -> "TYPE_LINE_DIGITAL";
			case AudioDeviceInfo.TYPE_FM -> "TYPE_FM";
			case AudioDeviceInfo.TYPE_AUX_LINE -> "TYPE_AUX_LINE";
			case AudioDeviceInfo.TYPE_IP -> "TYPE_IP";
			case AudioDeviceInfo.TYPE_BUS -> "TYPE_BUS";
			case AudioDeviceInfo.TYPE_HEARING_AID -> "TYPE_HEARING_AID";
			case AudioDeviceInfo.TYPE_BUILTIN_MIC -> "TYPE_BUILTIN_MIC";
			case AudioDeviceInfo.TYPE_FM_TUNER -> "TYPE_FM_TUNER";
			case AudioDeviceInfo.TYPE_TV_TUNER -> "TYPE_TV_TUNER";
			case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "TYPE_BUILTIN_SPEAKER_SAFE";
			case AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "TYPE_REMOTE_SUBMIX";
			case AudioDeviceInfo.TYPE_BLE_HEADSET -> "TYPE_BLE_HEADSET";
			case AudioDeviceInfo.TYPE_BLE_SPEAKER -> "TYPE_BLE_SPEAKER";
			case 28 -> "TYPE_ECHO_REFERENCE";    // TYPE_ECHO_REFERENCEがエラーになるので即値で指定
			case AudioDeviceInfo.TYPE_BLE_BROADCAST -> "TYPE_BLE_BROADCAST";
			default -> "UNKNOWN" + info.getType();
		};
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	public static String toString(@Nullable final AudioDeviceInfo info) {
		if (info == null) {
			return "AudioDeviceInfo{null}";
		} else {
			final StringBuilder sb = new StringBuilder("AudioDeviceInfo{");
			sb.append("name=").append(info.getProductName())
				.append(",type=").append(getAudioTypeString(info)).append("(").append(info.getType()).append(")")
				.append(",id=").append(info.getId())
				.append(",isSource=").append(info.isSource())
				.append(",isSink=").append(info.isSink())
				.append(",channelCounts=").append(Arrays.toString(info.getChannelCounts()))
				.append(",channelIndexMasks=").append(Arrays.toString(info.getChannelIndexMasks()))
				.append(",channelMasks=").append(Arrays.toString(info.getChannelMasks()))
				.append(",sampleRates=").append(Arrays.toString(info.getSampleRates()))
				.append(",encodings=").append(Arrays.toString(info.getEncodings()));
			if (BuildCheck.isAPI28()) {
				sb.append(",address=").append(info.getAddress());
			}
			if (BuildCheck.isAPI30()) {
				sb.append(",encapsulationModes=").append(Arrays.toString(info.getEncapsulationModes()))
					.append(",encapsulationMetadataTypes=").append(Arrays.toString(info.getEncapsulationMetadataTypes()));
			}
			if (BuildCheck.isAPI31()) {
				sb.append(",profiles=").append(info.getAudioProfiles())
					.append(",descriptors").append(info.getAudioDescriptors());
			}
			sb.append("}");
			return sb.toString();
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	public static void dumpAudioDevice(@NonNull final Context context, final String tag) {
		final String _tag = TextUtils.isEmpty(tag) ? TAG : tag;
		@NonNull
		final AudioManager manager = ContextUtils.requireSystemService(context, AudioManager.class);
		@NonNull
		final AudioDeviceInfo[] infos = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS | AudioManager.GET_DEVICES_INPUTS);
		int i = 0;
		for (final AudioDeviceInfo info: infos) {
			Log.i(_tag, i + ")" + toString(info));
			i++;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	public static void dumpInputAudioDevice(@NonNull final Context context, final String tag) {
		final String _tag = TextUtils.isEmpty(tag) ? TAG : tag;
		@NonNull
		final AudioManager manager = ContextUtils.requireSystemService(context, AudioManager.class);
		@NonNull
		final AudioDeviceInfo[] infos = manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
		int i = 0;
		for (final AudioDeviceInfo info: infos) {
			Log.i(_tag, i + ")" + toString(info));
			i++;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	public static void dumpOutputAudioDevice(@NonNull final Context context, final String tag) {
		final String _tag = TextUtils.isEmpty(tag) ? TAG : tag;
		@NonNull
		final AudioManager manager = ContextUtils.requireSystemService(context, AudioManager.class);
		@NonNull
		final AudioDeviceInfo[] infos = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
		int i = 0;
		for (final AudioDeviceInfo info: infos) {
			Log.i(_tag, i + ")" + toString(info));
			i++;
		}
	}
}
