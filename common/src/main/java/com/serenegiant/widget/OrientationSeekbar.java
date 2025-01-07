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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.serenegiant.common.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * 縦表示・横表示横指定可能なSeekBar
 */
public class OrientationSeekbar extends AppCompatSeekBar {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = OrientationSeekbar.class.getSimpleName();

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	@IntDef({
		HORIZONTAL,
		VERTICAL})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Orientation {}

	/**
	 * SeekBarが横表示か縦表示か
	 */
	@Orientation
	private int mOrientation = 0;    // 水平
	/**
	 * 親のSeekBarにはOnSeekBarChangeListenerのゲッターが無いので自前で保存しておく
	 */
	@Nullable
	private OnSeekBarChangeListener onChangeListener;

	/**
	 * デバッグ表示用のPaint
	 */
	@Nullable
	private Paint mDebugPaint;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public OrientationSeekbar(@NonNull final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public OrientationSeekbar(@NonNull final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public OrientationSeekbar(@NonNull final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.OrientationSeekbar, defStyleAttr, 0);
		mOrientation = a.getInt(R.styleable.OrientationSeekbar_android_orientation, mOrientation);
		a.recycle();
		if (DEBUG) Log.v(TAG, "コンストラクタ:orientation=" + mOrientation);
	}

	/**
	 * SeekBarの向きを取得
	 * @return
	 */
	@Orientation
	public int getOrientation() {
		return mOrientation;
	}

	public void setOrientation(@Orientation final int orientation) {
		mOrientation = orientation;
		requestLayout();
	}

	/**
	 * 親のSeekBarにはOnSeekBarChangeListenerのゲッターが無いので自前で保存しておく
	 * @param listener
	 */
	@Override
	public void setOnSeekBarChangeListener(@Nullable final OnSeekBarChangeListener listener) {
		super.setOnSeekBarChangeListener(listener);
		onChangeListener = listener;
	}

	@Override
	protected void onSizeChanged(
		final int width, final int height,
		final int oldWidth, final int oldHeight) {
		// 横のときはそのまま、縦のときは幅と高さを入れ替えて引き渡す
		if (mOrientation == HORIZONTAL) {
			super.onSizeChanged(width, height, oldWidth, oldHeight);
		} else {
			super.onSizeChanged(height, width, oldHeight, oldWidth);
		}
	}

	@Override
	protected synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		final boolean isHorizontal = mOrientation == HORIZONTAL;
		// 横のときはそのまま、縦のときは幅と高さを入れ替えて引き渡す
		super.onMeasure(
			isHorizontal ? widthMeasureSpec : heightMeasureSpec,
			isHorizontal ? heightMeasureSpec : widthMeasureSpec);
		setMeasuredDimension(
			isHorizontal ? getMeasuredWidth() : getMeasuredHeight(),
			isHorizontal ? getMeasuredHeight() : getMeasuredWidth());
	}

	/**
	 * 一般的にはonDrawをoverrideすべきなんだけど、onDrawをoverrideしただけだと
	 * canvasへ回転＆平行移動がthumbを手動で操作したときに表示される薄い丸へ適用されない
	 * (canvasを回転していないのと同じところ=Viewの外側に表示されてしまう)ので
	 * draw自体をoverrideしてcanvasへ回転と平行移動を適用してからsuper.drawｗ呼び出す
	 * @param canvas
	 */
	@Override
	public void draw(@NonNull final Canvas canvas) {
		final boolean isHorizontal = mOrientation == HORIZONTAL;
		final float w = isHorizontal ? getWidth() : getHeight();
		final float h = isHorizontal ? getHeight() : getWidth();
		final float w2 = w / 2.0f;
		final float h2 = h / 2.0f;
		if (!isHorizontal) {
			// 下が0, 上がmaxになるように回転させる
			canvas.rotate(270);
			canvas.translate(-w, 0);
//			// これだと下が0, 上がmaxになる
//			canvas.rotate(90);
//			canvas.translate(0, -h);
		}
		if (DEBUG) {
			if (mDebugPaint == null) {
				mDebugPaint = new Paint();
			}
			// View全体を塗りつぶす
			mDebugPaint.setColor(0x3f00ff00);
			canvas.drawRect(0, 0, w, h, mDebugPaint);
		}
		super.draw(canvas);
	}

	private int lastProgress = 0;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		if (!isEnabled()) {
			// レイアウトエディタで表示中はタッチ操作を受け付けない
			return false;
		}
		if (mOrientation == HORIZONTAL) {
			// 水平のときはデフォルトの動作にする
			return super.onTouchEvent(event);
		}
		// 以下は縦の場合の処理
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN -> {
			onChangeListener.onStartTrackingTouch(this);
			setPressed(true);
			setSelected(true);
		}
		case MotionEvent.ACTION_MOVE -> {
			super.onTouchEvent(event);
			// 下が0、上が最大値
			int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
			// シークバーの値を設定する。
			if (progress < 0) {
				progress = 0;
			}
			if (progress > getMax()) {
				progress = getMax();
			}
			setProgress(progress);
			if (progress != lastProgress) {
				lastProgress = progress;
				onChangeListener.onProgressChanged(this, progress, true);
			}
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			onChangeListener.onProgressChanged(
				this, (int) (getMax() * event.getY() / getHeight()), true);
			// シークバーを動かす
			setPressed(true);
			setSelected(true);
		}
		case MotionEvent.ACTION_UP -> {
			onChangeListener.onStopTrackingTouch(this);
			setPressed(false);
			setSelected(false);
		}
		case MotionEvent.ACTION_CANCEL -> {
			super.onTouchEvent(event);
			setPressed(false);
			setSelected(false);
		}
		}
		return true;
	}

}
