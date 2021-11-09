package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

/**
 * コンテンツの拡大縮小方法をセット可能なViewのインターフェース
 */
public interface IScaledView {
	/** アスペクト比を保って最大化 */
	public static final int SCALE_MODE_KEEP_ASPECT = 0;
	/** 画面サイズに合わせて拡大縮小 */
	public static final int SCALE_MODE_STRETCH_TO_FIT = 1;
	/** アスペクト比を保って短辺がフィットするようにCROP_CENTER */
	public static final int SCALE_MODE_CROP = 2;

	@IntDef({
		SCALE_MODE_KEEP_ASPECT,
		SCALE_MODE_STRETCH_TO_FIT,
		SCALE_MODE_CROP})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ScaleMode {}

	/**
	 * 拡大縮小方法をセット
	 * @param scaleMode SCALE_MODE_KEEP_ASPECT, SCALE_MODE_STRETCH, SCALE_MODE_CROP
	 */
	public void setScaleMode(@ScaleMode final int scaleMode);

	/**
	 * 現在の拡大縮小方法を取得
	 * @return
	 */
	@ScaleMode
	public int getScaleMode();

	/**
	 * Viewの要求アスペクト比を設定する。アスペクト比=<code>幅 / 高さ</code>.
	 * @param aspectRatio
	 */
	public void setAspectRatio(final double aspectRatio);

	/**
	 * Viewの要求アスペクト比を設定する。アスペクト比=<code>幅 / 高さ</code>.
	 * @param width
	 * @param height
	 */
	public void setAspectRatio(final int width, final int height);

	/**
	 * 現在の要求アスペクト比を取得
	 * @return
	 */
	public double getAspectRatio();

	public void setNeedResizeToKeepAspect(final boolean keepAspect);
}
