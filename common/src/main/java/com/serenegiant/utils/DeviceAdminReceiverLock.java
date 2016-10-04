package com.serenegiant.utils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class DeviceAdminReceiverLock extends DeviceAdminReceiver {

	public static final String EXTRA_REQUEST_FINISH = "EXTRA_REQUEST_FINISH";

	private static final int REQ_SCREEN_LOCK = 412809;
	public static void requestScreenLock(final Activity activity, final boolean finish) {
		if (!checkScreenLock(activity, finish)) {
			// スクリーンをロックできなかった時はデバイス管理者が無効になってるはずなのでデバイス管理者有効画面を表示する
			final Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(activity, DeviceAdminReceiverLock.class));
			intent.putExtra(EXTRA_REQUEST_FINISH, finish);
			activity.startActivityForResult(intent, REQ_SCREEN_LOCK);
		}
	}

	/**
	 * スクリーンロックを行う
	 * @return スクリーンロックできればtrue
	 */
	private static boolean checkScreenLock(final Activity activity, final boolean finish) {
		final ComponentName cn = new ComponentName(activity, DeviceAdminReceiverLock.class);
		final DevicePolicyManager dpm = (DevicePolicyManager)activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
		if (dpm.isAdminActive(cn)){
			// デバイス管理者が有効ならスクリーンをロック
			dpm.lockNow();
			if (finish) {
				activity.finish();
			}
			return true;
		}
		return false;
	}

	public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQ_SCREEN_LOCK:
			if (resultCode == Activity.RESULT_OK) {
				final boolean finish = (data != null) && data.getBooleanExtra(EXTRA_REQUEST_FINISH, false);
				// 有効になった
				checkScreenLock(activity, finish);
				return true;
			} else {
				// キャンセルされた or 有効化出来なかった
			}
		}
		return false;
	}
}
