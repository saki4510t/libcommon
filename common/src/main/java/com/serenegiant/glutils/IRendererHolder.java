package com.serenegiant.glutils;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * 分配描画インターフェース
 */
public interface IRendererHolder extends IRendererCommon {

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
	 * @param id 普通はSurface#hashCodeを使う
	 * @param surface
	 * @param isRecordable
	 */
	public void addSurface(final int id, final Surface surface, final boolean isRecordable);

	/**
	 * 分配描画用のSurfaceを削除
	 * @param id
	 */
	public void removeSurface(final int id);

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
