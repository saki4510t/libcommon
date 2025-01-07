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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.serenegiant.common.R;
import com.serenegiant.glutils.IMirror;
import com.serenegiant.view.ViewUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.view.ViewTransformDelegater.*;

/**
 * FIXME 拡大縮小回転移動処理をDelegaterへ分けたい
 * FIXME ViewTransformDelegaterを使うように変更する？AspectScaledTextureViewを継承しているから難しそう
 */
public class ZoomAspectScaledTextureView
	extends AspectScaledTextureView implements IMirror {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ZoomAspectScaledTextureView.class.getSimpleName();

	/**
	 * タッチ操作の有効無効設定
	 */
	@TouchMode
	private int mHandleTouchEvent;
	private float mManualScale = 1.0f;
	private float mManualRotate = Float.MAX_VALUE;

	/**
	 * 拡大縮小回転移動のための射影行列のデフォルト値
	 */
	protected final Matrix mDefaultMatrix = new Matrix();
	/**
	 * 射影行列が変更されたかどうかのフラグ(キャッシュを更新するため)
	 */
	protected boolean mImageMatrixChanged;
	/**
	 * 毎回射影行列そのものにアクセスするとJNIのオーバーヘッドがあるのでJavaのfloat配列としてキャッシュ
	 */
	protected final float[] mMatrixCache = new float[9];
	/**
	 * タッチ操作開始時の射影行列保持用
	 */
	private final Matrix mSavedImageMatrix = new Matrix();
	/**
	 * 映像を移動可能な領域
	 */
	private final RectF mLimitRect = new RectF();
	/**
	 * 映像を移動可能な領域を示すLineSegment配列
	 */
	private final ViewUtils.LineSegment[] mLimitSegments = new ViewUtils.LineSegment[4];
	/**
	 * 表示されるViewの実際のサイズ
	 */
	private final RectF mContentRect = new RectF();
	/**
	 * scaled and moved and rotated corner coordinates of image
	 * [(left,top),(right,top),(right,bottom),(left.bottom)]
	 */
	private final float[] mTransCoords = new float[8];
	/**
	 * タッチイベントのID
	 */
	private int mPrimaryId, mSecondaryId;
	/**
	 * 1つ目のタッチでのx,y座標
	 */
	private float mPrimaryX, mPrimaryY;
	/**
	 * 2つ目のタッチでのx,y座標
	 */
	private float mSecondX, mSecondY;
	/**
	 * 拡大縮小・回転時に使用するピボット座標
	 */
	private float mPivotX, mPivotY;
	/**
	 * 平行移動量
	 */
	private float mTransX, mTransY;
	/**
	 * distance between touch points when start multi touch, for calculating zooming scale
	 */
	private float mTouchDistance;
	/**
	 * current rotating degree
	 */
	private float mCurrentDegrees;
	private boolean mIsRotating;
	/**
	 * Maximum zoom scale
	 */
	protected final float mMaxScale = DEFAULT_MAX_SCALE;
	/**
	 * Minimum zoom scale, set in #init as fit the image to this view bounds
	 */
	private float mMinScale = DEFAULT_MIN_SCALE;
	/**
	 * current state, -1/STATE_NON/STATE_WAITING/STATE_DRAGGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	@State
	private int mState = STATE_NON;
	/**
	 * Runnable instance to wait starting image reset
	 */
	private Runnable mWaitImageReset;
	/**
	 * Runnable instance to wait starting rotation
	 */
	private Runnable mStartCheckRotate;

	@MirrorMode
    private int mMirrorMode = MIRROR_NORMAL;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ZoomAspectScaledTextureView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		if (DEBUG) Log.v(TAG, "コンストラクタ");
		final TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.ZoomAspectScaledTextureView, defStyleAttr, 0);
		try {
			// getIntegerは整数じゃなければUnsupportedOperationExceptionを投げる
			mHandleTouchEvent = a.getInteger(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, TOUCH_ENABLED_ALL);
		} catch (final UnsupportedOperationException e) {
			Log.d(TAG, TAG, e);
			final boolean b = a.getBoolean(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, true);
			mHandleTouchEvent = b ? TOUCH_ENABLED_ALL : TOUCH_DISABLED;
		} finally {
			a.recycle();
		}
	}

	@SuppressLint({"ClickableViewAccessibility", "SwitchIntDef"})
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		if (DEBUG) Log.v(TAG, "onTouchEvent:");

		if (handleOnTouchEvent(event)) {
			return true;	// 処理済み
		}

		if (mHandleTouchEvent == TOUCH_DISABLED) {
			return super.onTouchEvent(event);
		}

		final int actionCode = event.getActionMasked();	// >= API8

		switch (actionCode) {
		case MotionEvent.ACTION_DOWN:
			// single touch
			startWaiting(event);
			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
		{	// マルチタッチ時の処理
			switch (mState) {
			case STATE_WAITING:
				// 最初のマルチタッチ → 拡大縮小・回転操作待機開始
				removeCallbacks(mWaitImageReset);
				// pass through
			case STATE_DRAGGING:
				if (event.getPointerCount() > 1) {
					startCheck(event);
					return true;
				}
				break;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE:
		{
			// moving with single and multi touch
			switch (mState) {
			case STATE_WAITING -> {
				if (((mHandleTouchEvent & TOUCH_ENABLED_MOVE) == TOUCH_ENABLED_MOVE)
					&& checkTouchMoved(event)) {

					removeCallbacks(mWaitImageReset);
					setState(STATE_DRAGGING);
					return true;
				}
			}
			case STATE_DRAGGING -> {
				if (processDrag(event))
					return true;
			}
			case STATE_CHECKING -> {
				if (checkTouchMoved(event)
					&& ((mHandleTouchEvent & TOUCH_ENABLED_ZOOM) == TOUCH_ENABLED_ZOOM)) {

					startZoom(event);
					return true;
				}
			}
			case STATE_ZOOMING -> {
				if (processZoom(event))
					return true;
			}
			case STATE_ROTATING -> {
				if (processRotate(event))
					return true;
			}
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
			// pass through
		case MotionEvent.ACTION_UP:
			removeCallbacks(mWaitImageReset);
			removeCallbacks(mStartCheckRotate);
			if ((actionCode == MotionEvent.ACTION_UP) && (mState == STATE_WAITING)) {
				final long downTime = SystemClock.uptimeMillis() - event.getDownTime();
				if (downTime > LONG_PRESS_TIMEOUT) {
					performLongClick();
				} else if (downTime < TAP_TIMEOUT) {
					performClick();
				}
			}
			// pass through
		case MotionEvent.ACTION_POINTER_UP:
			setState(STATE_NON);
			break;
		}
		return super.onTouchEvent(event);
	}

	protected boolean handleOnTouchEvent(final MotionEvent event) {
//		if (DEBUG) Log.v(TAG, "handleOnTouchEvent:" + event);
		return false;
	}

	protected void onReset() {
		if (DEBUG) Log.v(TAG, "onReset");
	}

//================================================================================
	/**
	 * TextureViewに関連付けられたSurfaceTextureが利用可能になった時の処理
	 */
	@Override
	public void onSurfaceTextureAvailable(@NonNull final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureAvailable(surface, width, height);
		if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureAvailable:(%dx%d)", width, height));
		setMirror(MIRROR_NORMAL);	// デフォルトだから適用しなくていいけど
	}

	/**
	 * SurfaceTextureのバッファーのサイズが変更された時の処理
	 */
	@Override
	public void onSurfaceTextureSizeChanged(@NonNull final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureSizeChanged(surface, width, height);
		if (DEBUG) Log.v(TAG, String.format("onSurfaceTextureSizeChanged:(%dx%d)", width, height));
		applyMirrorMode();
	}

//	/**
//	 * SurfaceTextureが破棄される時の処理
//	 * trueを返すとこれ以降描画処理は行われなくなる
//	 * falseを返すと自分でSurfaceTexture#release()を呼び出さないとダメ
//	 * ほとんどのアプリではtrueを返すべきである
//	 */
//	@Override
//	public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
//		super.onSurfaceTextureDestroyed(surface)
//		return true;
//	}

//================================================================================
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (DEBUG) Log.v(TAG, "setMirror" + mirror);
		if (mMirrorMode != mirror) {
			mMirrorMode = mirror;
			applyMirrorMode();
		}
	}

	@Override
	@MirrorMode
	public int getMirror() {
		return mMirrorMode;
	}
	
	/**
	 * タッチ操作の有効無効設定
	 * @param enabled
	 */
	public void setEnableHandleTouchEvent(@TouchMode final int enabled) {
		if (DEBUG) Log.v(TAG, "setEnableHandleTouchEvent" + enabled);
		mHandleTouchEvent = enabled;
	}

	public void reset() {
		init();
		onReset();
	}

//================================================================================
	@Override
	protected void init() {
		if (DEBUG) Log.v(TAG, "init:");
		// set the initial state to idle, get and save the internal Matrix.
		mState = STATE_RESET; setState(STATE_NON);
		// get the internally calculated zooming scale to fit the view
		mMinScale = DEFAULT_MIN_SCALE; // getMatrixScale();
		mCurrentDegrees = 0.f;
		mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > ViewUtils.EPS;

		// update image size
		// current implementation of ImageView always hold its image as a Drawable
		// (that can get ImageView#getDrawable)
		// therefore update the image size from its Drawable
		// set limit rectangle that the image can move
		final Rect tmp = new Rect();
		getDrawingRect(tmp);
		mLimitRect.set(tmp);
		// update image size
		final RectF bounds = getContentBounds();
		if ((bounds != null) && !bounds.isEmpty()) {
			mContentRect.set(bounds);
		} else {
			mContentRect.set(mLimitRect);
		}
		mLimitRect.inset((MOVE_LIMIT_RATE * getWidth()), (MOVE_LIMIT_RATE * getHeight()));
		mLimitSegments[0] = null;
		mTransX = mTransY = 0.0f;
		super.init();
		mDefaultMatrix.set(mImageMatrix);
	}

	@Nullable
	protected RectF getContentBounds() {
		if (DEBUG) Log.v(TAG, "getContentBounds:");
		return null;
	}

	/**
	 * set current state, get and save the internal Matrix int super class
	 * @param state:	-1/STATE_NON/STATE_DRAGGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private final void setState(@State final int state) {
		if (mState != state) {
			mState = state;
			// get and save the internal Matrix of super class
			getTransform(mSavedImageMatrix);
			if (!mImageMatrix.equals(mSavedImageMatrix)) {
				mImageMatrix.set(mSavedImageMatrix);
				mImageMatrixChanged = true;
			}
		}
	}

	/**
	 * start waiting
	 * @param event
	 */
	private final void startWaiting(final MotionEvent event) {
		mPrimaryId = 0;
		mSecondaryId = -1;
		mPrimaryX = mSecondX = event.getX();
		mPrimaryY = mSecondY = event.getY();
		if (mWaitImageReset == null) mWaitImageReset = new WaitImageReset();
		postDelayed(mWaitImageReset, CHECK_TIMEOUT);
		setState(STATE_WAITING);
	}

	/**
	 * move the image
	 * @param event
	 */
	private final boolean processDrag(final MotionEvent event) {
		float dx = event.getX() - mPrimaryX;
		float dy = event.getY() - mPrimaryY;

		// calculate the corner coordinates of image applied matrix
		// [(left,top),(right,top),(right,bottom),(left.bottom)]
		mTransCoords[0] = mTransCoords[6] = mContentRect.left;
		mTransCoords[1] = mTransCoords[3] = mContentRect.top;
		mTransCoords[5] = mTransCoords[7] = mContentRect.bottom;
		mTransCoords[2] = mTransCoords[4] = mContentRect.right;
		mImageMatrix.mapPoints(mTransCoords);
		for (int i = 0; i < 8; i += 2) {
			mTransCoords[i] += dx;
			mTransCoords[i+1] += dy;
		}
		// check whether the image can move
		// if we can ignore rotating, the limit check is more easy...
		boolean canMove
			// check whether at lease one corner of image bounds is in the limitRect
			 = mLimitRect.contains(mTransCoords[0], mTransCoords[1])
			|| mLimitRect.contains(mTransCoords[2], mTransCoords[3])
			|| mLimitRect.contains(mTransCoords[4], mTransCoords[5])
			|| mLimitRect.contains(mTransCoords[6], mTransCoords[7])
			// check whether at least one corner of limitRect is in the image bounds
			|| ViewUtils.ptInPoly(mLimitRect.left, mLimitRect.top, mTransCoords)
			|| ViewUtils.ptInPoly(mLimitRect.right, mLimitRect.top, mTransCoords)
			|| ViewUtils.ptInPoly(mLimitRect.right, mLimitRect.bottom, mTransCoords)
			|| ViewUtils.ptInPoly(mLimitRect.left, mLimitRect.bottom, mTransCoords);
		if (!canMove) {
			// when no corner is in, we need additional check whether at least
			// one side of image bounds intersect with the limit rectangle
			if (mLimitSegments[0] == null) {
				mLimitSegments[0] = new ViewUtils.LineSegment(mLimitRect.left, mLimitRect.top, mLimitRect.right, mLimitRect.top);
				mLimitSegments[1] = new ViewUtils.LineSegment(mLimitRect.right, mLimitRect.top, mLimitRect.right, mLimitRect.bottom);
				mLimitSegments[2] = new ViewUtils.LineSegment(mLimitRect.right, mLimitRect.bottom, mLimitRect.left, mLimitRect.bottom);
				mLimitSegments[3] = new ViewUtils.LineSegment(mLimitRect.left, mLimitRect.bottom, mLimitRect.left, mLimitRect.top);
			}
			final ViewUtils.LineSegment side = new ViewUtils.LineSegment(mTransCoords[0], mTransCoords[1], mTransCoords[2], mTransCoords[3]);
			canMove = ViewUtils.checkIntersect(side, mLimitSegments);
			if (!canMove) {
				side.set(mTransCoords[2], mTransCoords[3], mTransCoords[4], mTransCoords[5]);
				canMove = ViewUtils.checkIntersect(side, mLimitSegments);
				if (!canMove) {
					side.set(mTransCoords[4], mTransCoords[5], mTransCoords[6], mTransCoords[7]);
					canMove = ViewUtils.checkIntersect(side, mLimitSegments);
					if (!canMove) {
						side.set(mTransCoords[6], mTransCoords[7], mTransCoords[0], mTransCoords[1]);
						canMove = ViewUtils.checkIntersect(side, mLimitSegments);
					}
				}
			}
		}
		if (canMove) {
			// TODO we need adjust dx/dy not to penetrate into the limit rectangle
			// otherwise the image can not move when one side is on the border of limit rectangle.
			// only calculate without rotation now because its calculation is to heavy when rotation applied.
			if (!mIsRotating) {
				final float left = Math.min(Math.min(mTransCoords[0], mTransCoords[2]), Math.min(mTransCoords[4], mTransCoords[6]));
				final float right = Math.max(Math.max(mTransCoords[0], mTransCoords[2]), Math.max(mTransCoords[4], mTransCoords[6]));
				final float top = Math.min(Math.min(mTransCoords[1], mTransCoords[3]), Math.min(mTransCoords[5], mTransCoords[7]));
				final float bottom = Math.max(Math.max(mTransCoords[1], mTransCoords[3]), Math.max(mTransCoords[5], mTransCoords[7]));

				if (right < mLimitRect.left) {
					dx = mLimitRect.left - right;
				} else if (left + ViewUtils.EPS > mLimitRect.right) {
					dx = mLimitRect.right - left - ViewUtils.EPS;
				}
				if (bottom < mLimitRect.top) {
					dy = mLimitRect.top - bottom;
				} else if (top + ViewUtils.EPS > mLimitRect.bottom) {
					dy = mLimitRect.bottom - top - ViewUtils.EPS;
				}
			}
			if ((dx != 0) || (dy != 0)) {
//				if (DEBUG) Log.v(TAG, String.format("processDrag:dx=%f,dy=%f", dx, dy));
				// apply move
				if (mImageMatrix.postTranslate(dx, dy)) {
					mTransX += dx;
					mTransY += dy;
					// when image is really moved?
					mImageMatrixChanged = true;
					// apply to super class
					setTransform(mImageMatrix);
				}
			}
		}
		mPrimaryX = event.getX();
		mPrimaryY = event.getY();
		return canMove;
	}

	/**
	 * start checking whether zooming/rotating
	 * @param event
	 */
	private final void startCheck(final MotionEvent event) {

		if (event.getPointerCount() > 1) {
			// primary touch
			mPrimaryId = event.getPointerId(0);
			mPrimaryX = event.getX(0);
			mPrimaryY = event.getY(0);
			// secondary touch
			mSecondaryId = event.getPointerId(1);
			mSecondX = event.getX(1);
			mSecondY = event.getY(1);
			// calculate the distance between first and second touch
			final float dx = mSecondX - mPrimaryX;
			final float dy = mSecondY - mPrimaryY;
			final float distance = (float)Math.hypot(dx, dy);
			if (distance < MIN_DISTANCE) {
				//  ignore when the touch distance is too short
				return;
			}

			mTouchDistance = distance;
			// set pivot position to the middle coordinate
			mPivotX = (mPrimaryX + mSecondX) / 2.f;
			mPivotY = (mPrimaryY + mSecondY) / 2.f;
			//
			if ((mHandleTouchEvent & TOUCH_ENABLED_ROTATE) == TOUCH_ENABLED_ROTATE) {
				if (mStartCheckRotate == null) {
					mStartCheckRotate = new StartCheckRotate();
				}
				postDelayed(mStartCheckRotate, CHECK_TIMEOUT);
			}
			setState(STATE_CHECKING); 		// start zoom/rotation check
		}
	}

	/**
	 * start zooming
	 * @param event
	 * @return
	 */
	private final void startZoom(final MotionEvent event) {

		removeCallbacks(mStartCheckRotate);
		setState(STATE_ZOOMING);
	}

	/**
	 * zooming
	 * @param event
	 * @return
	 */
	private final boolean processZoom(final MotionEvent event) {
		// restore the Matrix
		restoreMatrix();
		// get current zooming scale
		final float currentScale = getMatrixScale();
		// calculate the zooming scale from the distance between touched positions
		final float scale = calcScale(event);
		// calculate the applied zooming scale
		final float tmpScale = scale * currentScale;
		if (tmpScale < mMinScale) {
			// skip if the applied scale is smaller than minimum scale
			return false;
		} else if (tmpScale > mMaxScale) {
			// skip if the applied scale is bigger than maximum scale
			return false;
		}
		// change scale with scale value and pivot point
		if (mImageMatrix.postScale(scale, scale, mPivotX, mPivotY)) {
			// when Matrix is changed
			mImageMatrixChanged = true;
			// apply to super class
			setTransform(mImageMatrix);
		}
		return true;
	}

	/**
	 * calculate the zooming scale from the distance between touched position</br>
	 * this method ony use the index of 0 and 1 for touched position
	 * @param event
	 * @return
	 */
	private final float calcScale(final MotionEvent event) {
		final float dx = event.getX(0) - event.getX(1);
		final float dy = event.getY(0) - event.getY(1);
		final float distance = (float)Math.hypot(dx, dy);

		return distance / mTouchDistance;
	}

	/**
	 * check whether the touch position changed
	 * @param event
	 * @return true if the touch position changed
	 */
	private final boolean checkTouchMoved(final MotionEvent event) {
		final boolean result = true;
		final int ix0 = event.findPointerIndex(mPrimaryId);
		final int ix1 = event.findPointerIndex(mSecondaryId);
		if (ix0 >= 0) {
			// check primary touch
			float x = event.getX(ix0) - mPrimaryX;
			float y = event.getY(ix0) - mPrimaryY;
			if (x * x + y * y < MIN_DISTANCE_SQUARE) {
				// primary touch is at the almost same position
				if (ix1 >= 0) {
					// check secondary touch
					x = event.getX(ix1) - mSecondX;
					y = event.getY(ix1) - mSecondY;
					if (x * x + y * y < MIN_DISTANCE_SQUARE) {
						// secondary touch is also at the almost same position.
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return result;
	}

	/**
	 * rotating image
	 * @param event
	 * @return
	 */
	private final boolean processRotate(final MotionEvent event) {
		if (checkTouchMoved(event)) {
			// restore the Matrix
			restoreMatrix();
			mCurrentDegrees = calcAngle(event);
			mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > ViewUtils.EPS;
			if (mIsRotating && mImageMatrix.postRotate(mCurrentDegrees, mPivotX, mPivotY)) {
				// when Matrix is changed
				mImageMatrixChanged = true;
				// apply to super class
				setTransform(mImageMatrix);
				return true;
			}
		}
		return false;
	}

	/**
	 * calculate the rotating angle</br>
	 * first vector Za=(X0,Y0), second vector Zb=(X1,Y1), angle between two vectors=φ</br>
	 * cos φ ＝ Za・Zb / (|Za| |Zb|)</br>
	 *  =(X0X1+Y0Y1) / √{(X0^2 + Y0^2)(X1^2 + Y1^2)}</br>
	 * ∴result angle φ=Arccos(cosφ)</br>
	 * the result of Arccos if 0-π[rad] therefor we need to convert to degree
	 * and adjust the rotating direction using cross-product of vector Za and Zb
	 * @param event
	 * @return
	 */
	private final float calcAngle(final MotionEvent event) {
		final int ix0 = event.findPointerIndex(mPrimaryId);
		final int ix1 = event.findPointerIndex(mSecondaryId);
		float angle = 0.f;
		if ((ix0 >= 0) && (ix1 >= 0)) {
			// first vector (using touch points when start rotating)
			final float x0 = mSecondX - mPrimaryX;
			final float y0 = mSecondY - mPrimaryY;
			// second vector (using current touch points)
			final float x1 = event.getX(ix1) - event.getX(ix0);
			final float y1 = event.getY(ix1) - event.getY(ix0);
			//
			final double s = (x0 * x0 + y0 * y0) * (x1 * x1 + y1 * y1);
			final double cos = ViewUtils.dotProduct(x0, y0, x1, y1) / Math.sqrt(s);
			angle = ViewUtils.TO_DEGREE * (float)Math.acos(cos) * Math.signum(ViewUtils.crossProduct(x0, y0, x1, y1));
		}
		return angle;
	}

	/**
	 * get the zooming scale</br>
	 * return minimum one of MSCALE_X and MSCALE_Y
	 * @return return DEFAULT_SCALE when the scale is smaller than or equal to zero
	 */
	private final float getMatrixScale() {
		updateMatrixCache();
		final float scale = Math.min(mMatrixCache[Matrix.MSCALE_X], mMatrixCache[Matrix.MSCALE_X]);
		if (scale <= 0f) {	// for prevent disappearing or reversing
//			if (DEBUG) Log.w(TAG, "getMatrixScale:scale<=0, set to default");
			return DEFAULT_SCALE;
		}
		return scale;
	}

	/**
	 * restore the Matrix to the one when state changed
	 */
	private final void restoreMatrix() {
		mImageMatrix.set(mSavedImageMatrix);
		mImageMatrixChanged = true;
	}

	/**
	 * update the matrix caching float[]
	 */
	private final boolean updateMatrixCache() {
		if (mImageMatrixChanged) {
			mImageMatrix.getValues(mMatrixCache);
			mImageMatrixChanged = false;
			return true;
		}
		return false;
	}

//--------------------------------------------------------------------------------
	/**
	 * ミラーモードをTextureViewに適用
	 */
	private void applyMirrorMode() {
		if (DEBUG) Log.v(TAG, "applyMirrorMode");
		switch (mMirrorMode) {
		case MIRROR_HORIZONTAL -> {
			setScaleX(-1.0f);
			setScaleY(1.0f);
		}
		case MIRROR_VERTICAL -> {
			setScaleX(1.0f);
			setScaleY(-1.0f);
		}
		case MIRROR_BOTH -> {
			setScaleX(-1.0f);
			setScaleY(-1.0f);
		}
//		case IMirror.MIRROR_NORMAL,
		default -> {
			setScaleX(1.0f);
			setScaleY(1.0f);
		}
		}
	}

	/**
	 * リセット待ちのためのRunnable
	 */
	private final class WaitImageReset implements Runnable {
		@Override
		public void run() {
			init();
		}
	}

	/**
	 * 回転待ちのためのRunnable
	 */
	private final class StartCheckRotate implements Runnable {
		@Override
		public void run() {
			if (mState == STATE_CHECKING) {
				setState(STATE_ROTATING);
			}
		}
	}

//================================================================================
	public void setManualScale(final float scale) {
		setMatrix(mTransX, mTransY, scale, mManualRotate);
	}

	public float getManualScale() {
		return mManualScale;
	}

	public PointF getTranslate(@NonNull final PointF result) {
		result.set(mManualScale, mTransX);
		return result;
	}

	public float getTranslateX() {
		return mTransX;
	}

	public float getTranslateY() {
		return mTransY;
	}

	public void setTranslate(final float dx, final float dy) {
		setMatrix(dx, dy, mManualScale, mManualRotate);
	}

	public void setRotate(final float degrees) {
		setMatrix(mTransX, mTransY, mManualScale, degrees);
	}

	public float getRotate() {
		return mManualRotate;
	}

	public void setMatrix(final float dx, final float dy, final float scale) {

		setMatrix(dx, dy, scale, Float.MAX_VALUE);
	}

	public void setMatrix(final float dx, final float dy,
		final float scale, final float degrees) {

		if ((mTransX != dx) || (mTransY != dy)
			|| (mManualScale != scale)
			|| (mCurrentDegrees != degrees)) {

			if (DEBUG) Log.v(TAG, "setMatrix");
			mManualScale = scale <= 0.0f ? mManualScale : scale;
			mTransX = dx;
			mTransY = dy;
			mManualRotate = degrees;
			if (degrees != Float.MAX_VALUE) {
				while (mManualRotate > 360) {
					mManualRotate -= 360;
				}
				while (mManualRotate < -360) {
					mManualRotate += 360;
				}
			}
			final float[] work = new float[9];
			mDefaultMatrix.getValues(work);
			final int w2 = getWidth() >> 1;
			final int h2 = getHeight() >> 1;
//			mImageMatrix.setScale(
//				work[Matrix.MSCALE_X] * mManualScale,
//				work[Matrix.MSCALE_Y] * mManualScale,
//				w2, h2);
			mImageMatrix.reset();
			mImageMatrix.postTranslate(dx, dy);
			mImageMatrix.postScale(
				work[Matrix.MSCALE_X] * mManualScale,
				work[Matrix.MSCALE_Y] * mManualScale,
				w2, h2);
			if (degrees != Float.MAX_VALUE) {
				mImageMatrix.postRotate(mManualRotate,
					w2, h2);
			}
			// when Matrix is changed
			mImageMatrixChanged = true;
			// apply to super class
			setTransform(mImageMatrix);
		}
	}
}
