package com.serenegiant.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import com.serenegiant.view.IViewTransformer;
import com.serenegiant.view.ViewTransformer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TransformTextureView extends TextureView {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = TransformTextureView.class.getSimpleName();

	@Nullable
	private IViewTransformer mViewTransformer;

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
		// デフォルトのIViewTransformerを生成させる
		getViewTransformer();
	}

	/**
	 * IViewTransformerをセット
	 * @param transformer
	 */
	public void setViewTransformer(@Nullable final IViewTransformer transformer) {
		mViewTransformer = transformer;
	}

	/**
	 * IViewTransformerを取得
	 * 設定されていなければデフォルトのIViewTransformerを生成＆設定して返す
	 * @return
	 */
	@NonNull
	public IViewTransformer getViewTransformer() {
		if (mViewTransformer == null) {
			mViewTransformer = new ViewTransformer(this) {
				@Override
				protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
					TransformTextureView.super.setTransform(transform);
				}

				@NonNull
				@Override
				protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
					return TransformTextureView.super.getTransform(transform);
				}
			};
		}
		return mViewTransformer;
	}
}
