package com.serenegiant.system
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

import android.content.Context

///**
// * システムサービスを取得するためのヘルパー拡張関数
// * 指定したクラスに対応するシステムサービスを取得できなければnullを返す
// * こっちはandroidx.coreのContext.ktに透過なものが含まれている
// */
//inline fun <reified T : Any> Context.getSystemService(serviceClass: Class<T>): T? {
//	return ContextUtils.getSystemService(this, serviceClass)
//}

/**
 * システムサービスを取得するためのヘルパー拡張関数
 * getSystemServiceと違って指定したクラスに対応するシステムサービスを取得できなければ
 * IllegalArgumentExceptionを投げる
 * @throws IllegalArgumentException
 */
@Throws(IllegalArgumentException::class)
inline fun <reified T : Any> Context.requireSystemService(serviceClass: Class<T>): T {
	return ContextUtils.requireSystemService(this, serviceClass)
}
