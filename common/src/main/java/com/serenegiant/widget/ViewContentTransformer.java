package com.serenegiant.widget;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
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

import android.graphics.Matrix;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Viewの表示内容の座標変換を行うためのヘルパークラス
 */
public abstract class ViewContentTransformer {
	private static final boolean DEBUG = true;	// TODO for debugging
	private static final String TAG = ViewContentTransformer.class.getSimpleName();

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param view
	 * @return
	 */
	@NonNull
	public static ViewContentTransformer newInstance(@NonNull final View view) {
		if (view instanceof TextureView) {
			return new TextureViewTransformer((TextureView)view);
		} else if (view instanceof ImageView) {
			return new ImageViewTransformer((ImageView) view);
		} else {
			return new DefaultTransformer(view);
		}
	}

//--------------------------------------------------------------------------------
	@NonNull
	protected final View mTargetView;
	/**
	 * デフォルトのトランスフォームマトリックス
	 * #setDefaultで変更していなければコンストラクタ実行時に
	 * Viewから取得したトランスフォームマトリックス
	 */
	@NonNull
	protected final Matrix mDefaultTransform = new Matrix();
	/**
	 * 現在のトランスフォームマトリックス
	 */
	@NonNull
	protected final Matrix mTransform = new Matrix();
	/**
	 * Matrixアクセスのワーク用float配列
	 */
	private final float[] work = new float[9];
	/**
	 * 平行移動量
	 */
	private float mCurrentTransX, mCurrentTransY;
	/**
	 * 拡大縮小率
	 */
	private float mCurrentScaleX, mCurrentScaleY;
	/**
	 * 回転角度
	 */
	private float mCurrentRotate;

	/**
	 * コンストラクタ
	 * @param view
	 */
	protected ViewContentTransformer(@NonNull final View view) {
		mTargetView = view;
		updateTransform();
		mDefaultTransform.set(mTransform);
	}

	@NonNull
	public View getTargetView() {
		return mTargetView;
	}

	/**
	 * ViewContentTransformerで保持しているトランスフォームマトリックスを
	 * ターゲットViewに設定されているトランスフォームマトリックスに設定する
	 * @return
	 */
	public abstract ViewContentTransformer updateTransform();

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullなら単位行列が設定される
	 */
	@CallSuper
	public void setTransform(@Nullable final Matrix transform) {
		if (mTransform != transform) {
			mTransform.set(transform);
		}
	}

	/**
	 * デフォルトのトランスフォームマトリックスを設定
	 * @param transform
	 * @return
	 */
	public ViewContentTransformer setDefault(@NonNull final Matrix transform) {
		mDefaultTransform.set(transform);
		return this;
	}

	/**
	 * トランスフォームマトリックスを初期状態に戻す
	 * #setDefaultで変更していなけれあコンストラクタ実行時の
	 * ターゲットViewのトランスフォームマトリックスに戻る
	 */
	public void reset() {
		if (DEBUG) Log.v(TAG, "reset:");
		setTransform(mDefaultTransform);
	}

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	public Matrix getTransform(@Nullable final Matrix transform) {
		final Matrix _transform = transform != null ? transform : new Matrix();
		_transform.set(mTransform);
		return _transform;
	}

	/**
	 * 指定位置に移動
	 * @param x
	 * @param y
	 * @return
	 */
	public ViewContentTransformer setTranslate(final float x, final float y) {
		return setTransform(x, y,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate);
	}

	/**
	 * 現在位置からオフセット
	 * @param dx
	 * @param dy
	 * @return
	 */
	public ViewContentTransformer translate(final float dx, final float dy) {
		return setTransform(mCurrentTransX + dx, mCurrentTransY + dy,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate);
	}

	/**
	 * 移動量を取得
	 * @return
	 */
	public float getTranslateX() {
		return mCurrentTransX;
	}

	/**
	 * 移動量を取得
	 * @return
	 */
	public float getTranslateY() {
		return mCurrentTransY;
	}

	/**
	 * 指定倍率に拡大縮小
	 * @param scaleX
	 * @param scaleY
	 * @return
	 */
	public ViewContentTransformer setScale(final float scaleX, final float scaleY) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			scaleX, scaleY,
			mCurrentRotate);
	}

	/**
	 * 指定倍率に拡大縮小
	 * @param scale
	 * @return
	 */
	public ViewContentTransformer setScale(final float scale) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			scale, scale,
			mCurrentRotate);
	}

	/**
	 * 現在の倍率から拡大縮小
	 * @param scaleX
	 * @param scaleY
	 * @return
	 */
	public ViewContentTransformer scale(final float scaleX, final float scaleY) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX * scaleX, mCurrentScaleY * scaleY,
			mCurrentRotate);
	}

	/**
	 * 現在の倍率から拡大縮小
	 * @param scale
	 * @return
	 */
	public ViewContentTransformer scale(final float scale) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX * scale, mCurrentScaleY * scale,
			mCurrentRotate);
	}

	public float getScaleX() {
		return mCurrentScaleX;
	}

	public float getScaleY() {
		return mCurrentScaleY;
	}

	/**
	 * 指定角度に回転
	 * @param degrees
	 * @return
	 */
	public ViewContentTransformer setRotate(final float degrees) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			degrees);
	}

	/**
	 * 現在の回転角度から回転
	 * @param degrees
	 * @return
	 */
	public ViewContentTransformer rotate(final float degrees) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate + degrees);
	}

	public float getRotation() {
		return mCurrentRotate;
	}

	/**
	 * トランスフォームマトリックスを設定
	 * @param transX
	 * @param transY
	 * @param scaleX
	 * @param scaleY
	 * @param degrees
	 * @return
	 */
	protected ViewContentTransformer setTransform(
		final float transX, final float transY,
		final float scaleX, final float scaleY,
		final float degrees) {

		if ((mCurrentTransX != transX) || (mCurrentTransY != transY)
			|| (mCurrentScaleX != scaleX) || (mCurrentScaleY != scaleY)
			|| (mCurrentRotate != degrees)) {

			mCurrentScaleX = scaleX;
			mCurrentScaleY = scaleY;
			mCurrentTransX = transX;
			mCurrentTransY = transY;
			mCurrentRotate = degrees;
			if (degrees != Float.MAX_VALUE) {
				while (mCurrentRotate > 360) {
					mCurrentRotate -= 360;
				}
				while (mCurrentRotate < -360) {
					mCurrentRotate += 360;
				}
			}
			mDefaultTransform.getValues(work);
			final int w2 = mTargetView.getWidth() >> 1;
			final int h2 = mTargetView.getHeight() >> 1;
			mTransform.reset();
			mTransform.postTranslate(transX, transY);
			mTransform.postScale(
				work[Matrix.MSCALE_X] * mCurrentScaleX,
				work[Matrix.MSCALE_Y] * mCurrentScaleY,
				w2, h2);
			if (degrees != Float.MAX_VALUE) {
				mTransform.postRotate(mCurrentRotate,
					w2, h2);
			}
			// apply to target view
			setTransform(mTransform);
		}
		return this;
	}

//--------------------------------------------------------------------------------
	protected static class DefaultTransformer extends  ViewContentTransformer {
		private static final String TAG = DefaultTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private DefaultTransformer(@NonNull final View view) {
			super(view);
		}

		@Override
		public DefaultTransformer updateTransform() {
			// 今は何もしない
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			mTransform.set(transform);
			// FIXME 未実装
		}

	} // DefaultTransformer

//--------------------------------------------------------------------------------
	/**
	 * TextureView用ViewContentTransformer実装
	 */
	protected static class TextureViewTransformer extends ViewContentTransformer {
		private static final String TAG = TextureViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private TextureViewTransformer(@NonNull final TextureView view) {
			super(view);
		}

		@NonNull
		@Override
		public TextureView getTargetView() {
			return (TextureView)mTargetView;
		}

		@Override
		public TextureViewTransformer updateTransform() {
			getTargetView().getTransform(mTransform);
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			getTargetView().setTransform(transform);
		}

		@NonNull
		@Override
		public Matrix getTransform(@Nullable final Matrix transform) {
			return getTargetView().getTransform(transform);
		}

	} // TextureViewTransformer

//--------------------------------------------------------------------------------
	/**
	 * ImageView用ImageViewTransformer実装
	 */
	protected static class ImageViewTransformer extends ViewContentTransformer {
		private static final String TAG = TextureViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private ImageViewTransformer(@NonNull final ImageView view) {
			super(view);
		}

		@NonNull
		@Override
		public ImageView getTargetView() {
			return (ImageView)mTargetView;
		}

		@Override
		public ImageViewTransformer updateTransform() {
			getTargetView().getImageMatrix();
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			super.setTransform(transform);
			getTargetView().setImageMatrix(mTransform);
		}

	}	// ImageViewTransformer
}
