package com.serenegiant.utils
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

/**
 * UIスレッド上で実行するためのヘルパー関数
 * 未実行の同じRunnableが存在する場合はキャンセルされる
 * @param task
 * @param duration
 */
fun runOnUiThread(task: Runnable, duration: Long = 0) {
	UIThreadHelper.removeFromUiThread(task)
	UIThreadHelper.runOnUiThread(task, duration)
}

/**
 * UIスレッド上で実行するためのヘルパー関数
 * 指定したRunnableが未実行であれば実行待ちキューから取り除く
 * @param task
 */
fun removeFromUiThread(task: Runnable) {
	UIThreadHelper.removeFromUiThread(task)
}

/**
 * 未実行のタスク全てを実行待ちキューから取り除く
 */
fun removeAllFromUiThread() {
	UIThreadHelper.removeAllFromUiThread()
}
