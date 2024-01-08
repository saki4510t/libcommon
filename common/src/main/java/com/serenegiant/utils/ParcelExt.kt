package com.serenegiant.utils
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2024 saki t_saki@serenegiant.com
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

import android.os.Parcel
import com.serenegiant.system.BuildCheck

fun <T> Parcel.read(loader: ClassLoader?, clazz: Class<T>): T? {
    @Suppress("DEPRECATION")
    return if (BuildCheck.isAPI33()) {
        this.readParcelable(loader, clazz)
    } else {
        this.readParcelable(loader)
    }
}

fun <T> Parcel.read(clazz: Class<T>): T? {
    @Suppress("DEPRECATION")
    return if (BuildCheck.isAPI33()) {
        this.readParcelable(clazz.classLoader, clazz)
    } else {
        this.readParcelable(clazz.classLoader)
    }
}
