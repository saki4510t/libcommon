package com.serenegiant.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

import com.serenegiant.view.IViewTransformer;
import com.serenegiant.view.ViewTransformer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView(AppCompatImageView)へIViewTransformerによる
 * 表示内容のトランスフォーム機能を追加
 */
public class TransformImageView extends AppCompatImageView {

	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = TransformImageView.class.getSimpleName();

	@Nullable
	private IViewTransformer mViewTransformer;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public TransformImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public TransformImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public TransformImageView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// デフォルトのIViewTransformerを生成させる
		super.setScaleType(ScaleType.MATRIX);
	}

	protected void superSetScaleType(final ScaleType scaleType) {
		super.setScaleType(scaleType);
	}

	protected void superSetImageMatrix(@Nullable final Matrix transform) {
		super.setImageMatrix(transform);
	}

	protected Matrix superGetImageMatrix(@Nullable final Matrix transform) {
		Matrix result = transform;
		if (result != null) {
			result.set(super.getImageMatrix());
		} else {
			result = new Matrix(super.getImageMatrix());
		}
		return result;
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
			mViewTransformer = new DefaultViewTransformer(this);
		}
		return mViewTransformer;
	}
//--------------------------------------------------------------------------------
	/**
	 * トランスフォームマトリックス設定用のヘルパーメソッド
	 * @param transform
	 */
	public void setTransform(@Nullable final Matrix transform) {
		getViewTransformer().setTransform(transform);
	}

	/**
	 * トランスフォームマトリックス取得用のヘルパーメソッド
	 * @param transform
	 * @return
	 */
	public Matrix getTransform(@Nullable final Matrix transform) {
		return getViewTransformer().getTransform(transform);
	}

//--------------------------------------------------------------------------------
	public static class DefaultViewTransformer extends ViewTransformer {
		public DefaultViewTransformer(@NonNull final TransformImageView view) {
			super(view);
		}

		@Override
		protected void setTransform(@NonNull final View view, @Nullable final Matrix transform) {
			((TransformImageView)getTargetView()).superSetImageMatrix(transform);
		}

		@NonNull
		@Override
		protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
			return ((TransformImageView)getTargetView()).superGetImageMatrix(transform);
		}
	}
}
