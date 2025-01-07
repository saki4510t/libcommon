package com.serenegiant.system;
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
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * 電池残量情報保持用のホルダークラス
 */
public class BatteryInfo implements Parcelable {

	@IntDef({
		BatteryManager.BATTERY_STATUS_UNKNOWN,
		BatteryManager.BATTERY_STATUS_CHARGING,
		BatteryManager.BATTERY_STATUS_DISCHARGING,
		BatteryManager.BATTERY_STATUS_NOT_CHARGING,
		BatteryManager.BATTERY_STATUS_FULL,
	})
	@Retention(SOURCE)
	public @interface BatteryStatus {}

	@IntDef({
		BatteryManager.BATTERY_HEALTH_UNKNOWN,
		BatteryManager.BATTERY_HEALTH_GOOD,
		BatteryManager.BATTERY_HEALTH_OVERHEAT,
		BatteryManager.BATTERY_HEALTH_DEAD,
		BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE,
		BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE,
		BatteryManager.BATTERY_HEALTH_COLD,
	})
	@Retention(SOURCE)
	public @interface BatteryHealth {}

	@SuppressLint("InlinedApi")
	@IntDef({
		0,
		BatteryManager.BATTERY_PLUGGED_AC,
		BatteryManager.BATTERY_PLUGGED_USB,
		BatteryManager.BATTERY_PLUGGED_WIRELESS,
	})
	@Retention(SOURCE)
	public @interface BatteryPlugged {}

	@SuppressLint("InlinedApi")
	public static final int BATTERY_PLUGGED_ANY
		= BatteryManager.BATTERY_PLUGGED_AC
			| BatteryManager.BATTERY_PLUGGED_USB
			| BatteryManager.BATTERY_PLUGGED_WIRELESS;

//--------------------------------------------------------------------------------
	/**
	 * BatteryManager.EXTRA_LEVELの値を保持
	 */
	public int level;
	/**
	 * BatteryManager.EXTRA_SCALEの値を保持
	 */
	public int scale;
	/**
	 * BatteryManager.EXTRA_TEMPERATUREの値を保持
	 */
	public float temperature;
	/**
	 * level, scaleから計算した電池残量[%], [0.0, 100.0]
	 */
	public float battery;
	/**
	 * バッテリー電圧[V]
	 * BatteryManager.EXTRA_VOLTAGEの値の1/1000
	 */
	public float voltage;
	/**
	 * 電池の種類
	 * BatteryManager.EXTRA_TECHNOLOGYの値
	 */
	public String technology;
	@BatteryStatus
	public int status;
	@BatteryHealth
	public int health;
	@BatteryPlugged
	public int plugged;

	/**
	 * デフォルトコンストラクタ
	 */
	public BatteryInfo() {
		clear();
	}

	/**
	 * コピーコンストラクタ
	 * @param src
	 */
	public BatteryInfo(@NonNull final BatteryInfo src) {
		level = src.level;
		scale = src.scale;
		temperature = src.temperature;
		battery = src.battery;
		voltage = src.voltage;
		technology = src.technology;
		status = src.status;
		health = src.health;
		plugged = src.plugged;
	}

	/**
	 * ブロードキャストを受け取ったときにIntentのバッテリー情報を解析するためのコンストラクタ
	 * Intent.ACTION_BATTERY_CHANGED, Intent.ACTION_BATTERY_LOW,
	 * Intent.ACTION_BATTERY_OKAY, Intent.ACTION_DOCK_EVENT
	 * @param batteryStatus
	 */
	public BatteryInfo(@NonNull final Intent batteryStatus) {
		set(batteryStatus);
	}

	/**
	 * Parcelable実装用のコンストラクタ
	 * @param in
	 */
	protected BatteryInfo(@NonNull final Parcel in) {
		level = in.readInt();
		scale = in.readInt();
		temperature = in.readFloat();
		battery = in.readFloat();
		voltage = in.readFloat();
		technology = in.readString();
		status = in.readInt();
		health = in.readInt();
		plugged = in.readInt();
	}

	/**
	 * セッター
	 * @param src
	 */
	public void set(@Nullable final BatteryInfo src) {
		if (src != null) {
			level = src.level;
			scale = src.scale;
			temperature = src.temperature;
			battery = src.battery;
			voltage = src.voltage;
			technology = src.technology;
			status = src.status;
			health = src.health;
			plugged = src.plugged;
		} else {
			clear();
		}
	}

	/**
	 * セッター
	 * @param batteryStatus
	 */
	public void set(@NonNull final Intent batteryStatus) {
		level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, level);
		scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, scale);
		temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Math.round(temperature * 10.0f)) / 10.0f;
		battery = (100.0f * level) / (scale != 0 ? scale : 100);
		voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Math.round(voltage * 1000.0f)) / 1000.0f;
		technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
		status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, status);
		health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, health);
		plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, plugged);
	}

	/**
	 * デフォルトにセット
	 */
	public void clear() {
		level = 0;
		scale = 100;
		temperature = 0.0f;
		battery = 0.0f;
		voltage = 0.0f;
		technology = null;
		status = BatteryManager.BATTERY_STATUS_UNKNOWN;
		health = BatteryManager.BATTERY_HEALTH_UNKNOWN;
		plugged = 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(level);
		dest.writeInt(scale);
		dest.writeFloat(temperature);
		dest.writeFloat(battery);
		dest.writeFloat(voltage);
		dest.writeString(technology);
		dest.writeInt(status);
		dest.writeInt(health);
		dest.writeInt(plugged);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@NonNull
	@Override
	public String toString() {
		return "BatteryInfo{" +
			"level=" + level +
			", scale=" + scale +
			", temperature=" + temperature +
			", battery=" + battery +
			", voltage=" + voltage +
			", technology=" + technology +
			", status=" + status +
			", health=" + health +
			", plugged=" + plugged +
			'}';
	}

	public boolean isCharging() {
		return status == BatteryManager.BATTERY_STATUS_CHARGING;
	}

//--------------------------------------------------------------------------------
	public static final Creator<BatteryInfo> CREATOR = new Creator<BatteryInfo>() {
		@Override
		public BatteryInfo createFromParcel(Parcel in) {
			return new BatteryInfo(in);
		}

		@Override
		public BatteryInfo[] newArray(int size) {
			return new BatteryInfo[size];
		}
	};
}
