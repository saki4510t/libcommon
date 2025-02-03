package com.serenegiant.widget;
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

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import com.serenegiant.view.ViewTransformer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TextureViewへViewTransformerによる
 * 表示内容のトランスフォーム機能を追加
 */
public class TransformTextureView extends TextureView
	implements ITransformView<Matrix> {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = TransformTextureView.class.getSimpleName();

	@Nullable
	private ViewTransformer mViewTransformer;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public TransformTextureView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public TransformTextureView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public TransformTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	/**
	 * IViewTransformerをセット
	 * @param transformer
	 */
	public void setViewTransformer(@Nullable final ViewTransformer transformer) {
		mViewTransformer = transformer;
	}

	/**
	 * IViewTransformerを取得
	 * 設定されていなければデフォルトのIViewTransformerを生成＆設定して返す
	 * @return
	 */
	@NonNull
	public ViewTransformer getViewTransformer() {
		if (mViewTransformer == null) {
			mViewTransformer = new ViewTransformer(this);
		}
		return mViewTransformer;
	}

	/**
	 * ITransformViewの実装
	 * @return
	 */
	@NonNull
	@Override
	public View getView() {
		return this;
	}
}
