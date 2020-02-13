package com.serenegiant.view;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;

import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.widget.ITransformView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ITransformViewの内容のトランスフォーム用ヘルパークラス
 */
public class ViewTransformer
	implements IContentTransformer.IViewTransformer {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ViewTransformer.class.getSimpleName();

	@NonNull
	private final ITransformView mTargetView;
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
	private final Matrix mWork = new Matrix();
	/**
	 * Matrixアクセスのワーク用float配列
	 */
	private final float[] workArray = new float[9];
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
	public ViewTransformer(@NonNull final ITransformView view) {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mTargetView = view;
		updateTransform(true);
	}

	/**
	 * 操作対象Viewを取得する
	 * @return
	 */
	@NonNull
	public ITransformView getTargetView() {
		return mTargetView;
	}

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullなら単位行列が設定される
	 */
	@NonNull
	@Override
	public ViewTransformer setTransform(@Nullable final Matrix transform) {
		if (DEBUG) Log.v(TAG, "setTransform:" + transform);
		if (mTransform != transform) {
			mTransform.set(transform);
		}
		internalSetTransform(mTransform);
		calcValues(mTransform);
		return this;
	}

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	@Override
	public Matrix getTransform(@Nullable final Matrix transform) {
		Matrix result = transform;
		if (result == null) {
			result = new Matrix();
		}
		result.set(mTransform);
		return result;
	}

	@NonNull
	@Override
	public ViewTransformer updateTransform(final boolean saveAsDefault) {
		internalGetTransform(mTransform);
		if (saveAsDefault) {
			mDefaultTransform.set(mTransform);
			// mDefaultTranslateからの相対値なのでtranslate/scale/rotateをクリアする
			if (DEBUG) Log.v(TAG, "updateTransform:default=" + mDefaultTransform);
			resetValues();
		} else {
			calcValues(mTransform);
		}
		if (DEBUG) Log.v(TAG, "updateTransform:" + saveAsDefault + "," + mTransform);
		return this;
	}

	/**
	 * デフォルトのトランスフォームマトリックスを設定
	 * @param transform nullなら単位行列になる
	 * @return
	 */
	@NonNull
	@Override
	public ViewTransformer setDefault(@Nullable final Matrix transform) {
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
	public ViewTransformer setTranslate(final float x, final float y) {
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
	public ViewTransformer translate(final float dx, final float dy) {
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
	public ViewTransformer setScale(final float scaleX, final float scaleY) {
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
	public ViewTransformer setScale(final float scale) {
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
	public ViewTransformer scale(final float scaleX, final float scaleY) {
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
	public ViewTransformer scale(final float scale) {
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
	public ViewTransformer setRotate(final float degrees) {
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
	public ViewTransformer rotate(final float degrees) {
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

//--------------------------------------------------------------------------------

	/**
	 * デフォルトのトランスフォームマトリックスに
	 * 指定したトランスフォームマトリックスをかけ合わせてから
	 * TextureViewへ適用する
	 * @param transform
	 */
	protected void internalSetTransform(@Nullable final Matrix transform) {
		if (DEBUG) Log.v(TAG, "internalSetTransform:" + transform);
		mWork.set(mDefaultTransform);
		if (transform != null) {
			mWork.postConcat(mDefaultTransform);
		}
		mTargetView.setTransform(mWork);
	}

	/**
	 * TextureViewからのトランスフォームマトリックス取得処理
	 * @param transform
	 * @return
	 */
	@NonNull
	protected Matrix internalGetTransform(@Nullable final Matrix transform) {
		return mTargetView.getTransform(transform);
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
	protected ViewTransformer setTransform(
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
			final int w2 = mTargetView.getView().getWidth() >> 1;
			final int h2 = mTargetView.getView().getHeight() >> 1;
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
		mTransform.getValues(workArray);
		mCurrentTransX = workArray[Matrix.MTRANS_X];
		mCurrentTransY = workArray[Matrix.MTRANS_Y];
		mCurrentScaleX = workArray[Matrix.MSCALE_X];
		mCurrentScaleY = MatrixUtils.getScale(workArray);
		mCurrentRotate = MatrixUtils.getRotate(workArray);
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

}
