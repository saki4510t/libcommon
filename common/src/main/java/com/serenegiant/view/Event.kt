package com.serenegiant.view
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

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

interface Event<T> {
    val value: T
}

private data class EventImpl<T>(override val value: T) : Event<T> {
    var consumed = false
}

typealias LiveDataEvent<T> = LiveData<Event<T>>
typealias MutableLiveDataEvent<T> = MutableLiveData<Event<T>>

fun <T> LiveData<Event<T>>.observeEvent(owner: LifecycleOwner, observer: Observer<in T>) {
    observe(owner) {
        check(it is EventImpl)
        if (!it.consumed) {
            it.consumed = true
            observer.onChanged(it.value)
        }
    }
}

@MainThread
fun <T> MutableLiveData<Event<T>>.setValue(value: T) {
    this.value = EventImpl(value)
}

fun <T> MutableLiveData<Event<T>>.postValue(value: T) {
    this.postValue(EventImpl(value))
}
