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
					TransformImageView.super.setImageMatrix(transform);
				}

				@NonNull
				@Override
				protected Matrix getTransform(@NonNull final View view, @Nullable final Matrix transform) {
					Matrix result = transform;
					if (result != null) {
						result.set(TransformImageView.super.getImageMatrix());
					} else {
						result = new Matrix(TransformImageView.super.getImageMatrix());
					}
					return result;
				}
			};
		}
		return mViewTransformer;
	}
}
