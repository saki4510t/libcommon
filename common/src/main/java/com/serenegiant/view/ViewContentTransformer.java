package com.serenegiant.view;
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
import android.graphics.PointF;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.serenegiant.graphics.MatrixUtils;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Viewの表示内容の座標変換を行うためのヘルパークラス
 */
@Deprecated
public abstract class ViewContentTransformer
	implements IContentTransformer.IViewTransformer {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ViewContentTransformer.class.getSimpleName();

	/**
	 * Transform可能Viewを示すインターフェース
	 */
	public interface ITransformView {
		@NonNull
		public Matrix getTransform(@Nullable Matrix transform);
		public void setTransform(Matrix transform);
	}

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
		} else if (view instanceof ITransformView) {
			return new ITransformViewTransformer((ITransformView)view);
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
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mTargetView = view;
		updateTransform(true);
	}

	@NonNull
	public View getTargetView() {
		return mTargetView;
	}

	/**
	 * ViewContentTransformerで保持しているトランスフォームマトリックスを
	 * ターゲットViewに設定されているトランスフォームマトリックスに設定する
	 * @param setAsDefault 設定したトランスフォームマトリックスをデフォルトにトランスフォームマトリックスとして使うかどうか
	 * @return
	 */
	@NonNull
	@Override
	public abstract ViewContentTransformer updateTransform(final boolean setAsDefault);

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullなら単位行列が設定される
	 */
	@NonNull
	@Override
	public final ViewContentTransformer setTransform(@Nullable final Matrix transform) {
		if (DEBUG) Log.v(TAG, "setTransform:" + transform);
		if (mTransform != transform) {
			mTransform.set(transform);
		}
		internalSetTransform(mTransform);
		calcValues(mTransform);
		return this;
	}

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullまたは要素数が9未満なら単位行列が設定される
	 */
	public final void setTransform(@Nullable final float[] transform) {
		if (DEBUG) Log.v(TAG, "setTransform:" + Arrays.toString(transform));
		if ((transform != null) && (transform.length >= 9)) {
			mTransform.setValues(transform);
		} else {
			mTransform.set(null);
		}
		internalSetTransform(mTransform);
		calcValues(mTransform);
	}

	/**
	 * トランスフォームマトリックスを実際にView側へ適用する
	 * @param transform
	 */
	protected abstract void internalSetTransform(@NonNull final Matrix transform);

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	@Override
	public Matrix getTransform(@Nullable final Matrix transform) {
		if (transform != null) {
			transform.set(mTransform);
			return transform;
		} else {
			return new Matrix(mTransform);
		}
	}

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullまたは要素数が9未満なら内部で新しいfloat配列を生成して返す, そうでなければコピーする
	 * @param transform
	 * @return
	 */
	public float[] getTransform(@Nullable final float[] transform) {
		if ((transform != null) && (transform.length >= 9)) {
			mTransform.getValues(transform);
			return transform;
		} else {
			final float[] result = new float[9];
			mTransform.getValues(result);
			return result;
		}
	}

	/**
	 * デフォルトのトランスフォームマトリックスを設定
	 * @param transform
	 * @return
	 */
	@NonNull
	@Override
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
	 * 指定位置に移動
	 * @param x
	 * @param y
	 * @return
	 */
	public ViewContentTransformer setTranslate(final float x, final float y) {
		if (DEBUG) Log.v(TAG, String.format("setTranslate:(%f,%f)", x, y));
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
		if (DEBUG) Log.v(TAG, String.format("translate:(%f,%f)", dx, dy));
		return setTransform(mCurrentTransX + dx, mCurrentTransY + dy,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate);
	}

	/**
	 * 移動量を取得
	 * @param tr
	 * @return
	 */
	public PointF getTranslate(@Nullable final PointF tr) {
		if (tr != null) {
			tr.set(mCurrentTransX, mCurrentTransY);
			return tr;
		} else {
			return new PointF(mCurrentTransX, mCurrentTransY);
		}
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
		if (DEBUG) Log.v(TAG, String.format("setScale:(%f,%f)", scaleX, scaleY));
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
		if (DEBUG) Log.v(TAG, String.format("setScale:(%f)", scale));
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
		if (DEBUG) Log.v(TAG, String.format("scale:(%f,%f)", scaleX, scaleY));
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
		if (DEBUG) Log.v(TAG, String.format("scale:(%f)", scale));
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX * scale, mCurrentScaleY * scale,
			mCurrentRotate);
	}

	/**
	 * 現在の拡大縮小率(横方向)を取得
	 * @return
	 */
	public float getScaleX() {
		return mCurrentScaleX;
	}

	/**
	 * 現在の拡大縮小率(縦方向)を取得
 	 * @return
	 */
	public float getScaleY() {
		return mCurrentScaleY;
	}

	/**
	 * 縦横の拡大縮小率のうち小さい方を取得
	 * @return
	 */
	public float getScale() {
		return Math.min(mCurrentScaleX, mCurrentScaleY);
	}

	/**
	 * 指定角度に回転
	 * @param degrees
	 * @return
	 */
	public ViewContentTransformer setRotate(final float degrees) {
		if (DEBUG) Log.v(TAG, String.format("setRotate:(%f)", degrees));
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
		if (DEBUG) Log.v(TAG, String.format("rotate:(%f)", degrees));
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate + degrees);
	}

	/**
	 * 現在の回転角度[度]を取得
	 * @return
	 */
	public float getRotation() {
		return mCurrentRotate;
	}

	/**
	 * 指定した座標配列をトランスフォームマトリックスで変換する
	 * @param points
	 */
	public void mapPoints(@NonNull final float[] points) {
		mTransform.mapPoints(points);
	}

	/**
	 * 指定した座標配列をトランスフォームマトリックスで変換する
	 * @param dst 代入先の座標配列(x,y ペア)
	 * @param src 変換元の座標配列(x,y ペア)
	 */
	public void mapPoints(@NonNull final float[] dst, @NonNull final float[] src) {
		mTransform.mapPoints(dst, src);
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
			// 回転 → 拡大縮小 → 平行移動 → デフォルト
			// デフォルトトランスフォームマトリックスを適用
			mTransform.postConcat(mDefaultTransform);
			// 平行移動
			mTransform.postTranslate(transX, transY);
			// 拡大縮小
			mTransform.postScale(
				scaleX, scaleY,
				w2, h2);
			// 回転
			if (degrees != Float.MAX_VALUE) {
				mTransform.postRotate(mCurrentRotate,
					w2, h2);
			}
			// apply to target view
			internalSetTransform(mTransform);
		}
		return this;
	}

	/**
	 * Matrixからtranslate/scale/rotateの値を計算する
	 * @param transform
	 * @return
	 */
	protected void calcValues(@NonNull final Matrix transform) {
		if (DEBUG) Log.v(TAG, "calcValues:" + transform);
		mTransform.getValues(work);
		mCurrentTransX = work[Matrix.MTRANS_X];
		mCurrentTransY = work[Matrix.MTRANS_Y];
		mCurrentScaleX = work[Matrix.MSCALE_X];
		mCurrentScaleY = MatrixUtils.getScale(work);
		mCurrentRotate = MatrixUtils.getRotate(work);
		if (DEBUG) Log.v(TAG, String.format("calcValues:tr(%fx%f),scale(%f,%f),rot=%f",
			mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate));
	}

	/**
	 * translate/scale/rotateの値をデフォルト(トランスフォームマトリックスとして単位行列)
	 */
	protected void resetValues() {
		mCurrentTransX = mCurrentTransY = 0.0f;
		mCurrentScaleX = mCurrentScaleY = 1.0f;
		mCurrentRotate = 0.0f;
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
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		@Override
		public DefaultTransformer updateTransform(final boolean setAsDefault) {
			// 今は何もしない
			mTransform.reset();
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
				// mDefaultTranslateからの相対値なのでtranslate/scale/rotateをクリアする
				if (DEBUG) Log.v(TAG, "updateTransform:default=" + mDefaultTransform);
				resetValues();
			} else {
				calcValues(mTransform);
			}
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault + "," + mTransform);
			return this;
		}

		@Override
		protected void internalSetTransform(@Nullable final Matrix transform) {
			if (DEBUG) Log.v(TAG, "internalSetTransform:" + transform);
			// ローカルキャッシュ
			final View targetView = mTargetView;
			// XXX これだとView自体の大きさとかが変わってしまいそう
			targetView.setTranslationX(getTranslateX());
			targetView.setTranslationY(getTranslateY());
			targetView.setPivotX(targetView.getWidth() >> 1);
			targetView.setPivotY(targetView.getHeight() >> 1);
			targetView.setRotation(getRotation());
			targetView.setScaleX(getScaleX());
			targetView.setScaleX(getScaleY());
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
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		@Override
		public TextureView getTargetView() {
			return (TextureView)mTargetView;
		}

		@NonNull
		@Override
		public TextureViewTransformer updateTransform(final boolean setAsDefault) {
			getTargetView().getTransform(mTransform);
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
				// mDefaultTranslateからの相対値なのでtranslate/scale/rotateをクリアする
				if (DEBUG) Log.v(TAG, "updateTransform:default=" + mDefaultTransform);
				resetValues();
			} else {
				calcValues(mTransform);
			}
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault + "," + mTransform);
			return this;
		}

		@Override
		protected void internalSetTransform(@Nullable final Matrix transform) {
			if (DEBUG) Log.v(TAG, "internalSetTransform:" + transform);
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
	 * ITransformView用のViewContentTransformer実装
	 */
	protected static class ITransformViewTransformer extends ViewContentTransformer {
		private static final String TAG = TextureViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private ITransformViewTransformer(@NonNull final ITransformView view) {
			super((View)view);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		public ITransformView getTargetViewInterface() {
			return (ITransformView)mTargetView;
		}

		@NonNull
		@Override
		public ITransformViewTransformer updateTransform(final boolean setAsDefault) {
			getTargetViewInterface().getTransform(mTransform);
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
				// mDefaultTranslateからの相対値なのでtranslate/scale/rotateをクリアする
				if (DEBUG) Log.v(TAG, "updateTransform:default=" + mDefaultTransform);
				resetValues();
			} else {
				calcValues(mTransform);
			}
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault + "," + mTransform);
			return this;
		}

		@Override
		protected void internalSetTransform(@Nullable final Matrix transform) {
			if (DEBUG) Log.v(TAG, "internalSetTransform:" + transform);
			getTargetViewInterface().setTransform(transform);
		}

		@NonNull
		@Override
		public Matrix getTransform(@Nullable final Matrix transform) {
			return getTargetViewInterface().getTransform(transform);
		}

	} // ITransformViewTransformer

//--------------------------------------------------------------------------------
	/**
	 * ImageView用ImageViewTransformer実装
	 */
	protected static class ImageViewTransformer extends ViewContentTransformer {
		private static final String TAG = ImageViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private ImageViewTransformer(@NonNull final ImageView view) {
			super(view);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		@Override
		public ImageView getTargetView() {
			return (ImageView)mTargetView;
		}

		@NonNull
		@Override
		public ImageViewTransformer updateTransform(final boolean setAsDefault) {
			mTransform.set(getTargetView().getImageMatrix());
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
				// mDefaultTranslateからの相対値なのでtranslate/scale/rotateをクリアする
				if (DEBUG) Log.v(TAG, "updateTransform:default=" + mDefaultTransform);
				resetValues();
			} else {
				calcValues(mTransform);
			}
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault + "," + mTransform);
			return this;
		}

		@Override
		protected void internalSetTransform(@Nullable final Matrix transform) {
			if (DEBUG) Log.v(TAG, "internalSetTransform:" + transform);
			getTargetView().setImageMatrix(transform);
		}

	}	// ImageViewTransformer

}
