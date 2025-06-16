package com.serenegiant.mediaeffect;
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

import androidx.annotation.NonNull;

public interface IMediaEffect {
	/**
	 * 映像効果・フィルターを適用する
	 * GLコンテキスト上で実行すること
	 * @param src
	 */
	public void apply(@NonNull final ISource src);
	public void release();
	public IMediaEffect resize(final int width, final int height);
	public boolean enabled();
	public IMediaEffect setEnable(final boolean enable);
}
