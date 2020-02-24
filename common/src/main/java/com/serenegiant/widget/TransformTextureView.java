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
	}

	/**
	 * IViewTransformerをセット
	 * @param transformer
	 */
	public void setViewTransformer(@Nullable final IViewTransformer transformer) {
		mViewTransformer = transformer;
	}

	protected void superSetTransform(@Nullable final Matrix transform) {
		super.setTransform(transform);
	}

	protected Matrix superGetTransform(@Nullable final Matrix transform) {
		return super.getTransform(transform);
	}

	/**
	 * IViewTransformerを取得
	 * 設定されていなければデフォルトのIViewTransformerを生成＆設定して返す
	 * @return
	 */
	@NonNull
	public IViewTransformer getViewTransformer() {
		if (mViewTransformer == null) {
			mViewTransformer = new DefaultViewTransformer(this);
		}
		return mViewTransformer;
	}

//--------------------------------------------------------------------------------
	public static class DefaultViewTransformer extends ViewTransformer {
		public DefaultViewTransformer(@NonNull final TransformTextureView view) {
			super(view);
		}

		@Override
		protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
			((TransformTextureView)getTargetView()).superSetTransform(transform);
		}

		@NonNull
		@Override
		protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
			return ((TransformTextureView)getTargetView()).superGetTransform(transform);
		}
	}

}
