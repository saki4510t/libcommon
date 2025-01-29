package com.serenegiant.telephony;
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.serenegiant.system.ContextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

public class TelephonyUtils {
   private static final boolean DEBUG = false; // 実働時はfalseにすること
   private static final String TAG = TelephonyUtils.class.getSimpleName();
   private TelephonyUtils() {
      // インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
   }

   @SuppressLint({"HardwareIds", "InlinedApi"})
   @RequiresPermission(anyOf = {
       Manifest.permission.READ_PHONE_STATE,
       Manifest.permission.READ_SMS,
       Manifest.permission.READ_PHONE_NUMBERS   // API>=26
   })
   @Nullable
   public static String getPhoneNumber(@NonNull final Context context) {
      final TelephonyManager tm
         = ContextUtils.requireSystemService(context, TelephonyManager.class);
      return tm.getLine1Number();
   }
}
