package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * 分配描画インターフェース
 */
public interface IRendererHolder extends IRendererCommon {
	/**
	 * 実行中かどうか
	 * @return
	 */
	public boolean isRunning();
	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	/**
	 * マスター用の映像を受け取るためのSurfaceを取得
	 * @return
	 */
	public Surface getSurface();

	/**
	 * マスター用の映像を受け取るためのSurfaceTextureを取得
	 * @return
	 */
	public SurfaceTexture getSurfaceTexture();

	/**
	 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
	 */
	public void reset();

	/**
	 * マスター映像サイズをサイズ変更要求
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height);

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id 普通は#hashCodeを使う
	 * @param surface, should be one of Surface, SurfaceTexture or SurfaceHolder
	 * @param isRecordable
	 */
	public void addSurface(final int id, final Object surface, final boolean isRecordable);

	/**
	 * 分配描画用のSurfaceを追加
	 * @param id 普通は#hashCodeを使う
	 * @param surface, should be one of Surface, SurfaceTexture or SurfaceHolder
	 * @param isRecordable
	 * @param maxFps 0以下なら制限しない
	 */
	public void addSurface(final int id, final Object surface, final boolean isRecordable, final int maxFps);

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	public void removeSurface(final int id);

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	public boolean isEnabled(final int id);
	
	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	public void setEnabled(final int id, final boolean enable);
	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame();

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount();

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 * @param path
	 */
	public void captureStillAsync(final String path);

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param path
	 */
	public void captureStill(final String path);

}
