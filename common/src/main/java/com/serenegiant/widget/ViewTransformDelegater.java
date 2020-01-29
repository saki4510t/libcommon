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

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

/**
 * 拡大縮小平行移動回転可能なViewのためのdelegater
 */
public class ViewTransformDelegater {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ViewTransformDelegater.class.getSimpleName();

	public interface ITransformView {
		public void invalidate();
		public boolean postDelayed(Runnable action, long delayMillis);
		public void onRestoreInstanceStateSp(final Parcelable state);
		public boolean removeCallbacks(final Runnable action);

		/**
		 * set maximum zooming scale
		 * @param maxScale
		 */
		public void setMaxScale(final float maxScale);
		/**
		 * set minimum zooming scale
		 * @param minScale
		 */
		public void setMinScale(final float minScale);
		public void setOnStartRotationListener(final OnStartRotationListener listener);
		public OnStartRotationListener getOnStartRotationListener();
		/**
		 * return current scale
		 * @return
		 */
		public float getScale();
		/**
		 * return current image translate values(offset)
		 * @param result
		 * @return
		 */
		public PointF getTranslate(final PointF result);
		/**
		 * get current rotating degrees
		 */
		public float getRotation();

		public RectF getBounds();
		@NonNull
		public Rect getDrawingRect();
		public Matrix getImageMatrixSp();
		public void setImageMatrixSp(final Matrix matrix);
		public void setColorFilterSp(final ColorFilter cf);

		public void init();
		public void onInit();
	}

	// constants
	/**
	 * State: idle
	 */
	private static final int STATE_NON = 0;
	/**
	 * State: wait action
	 */
	private static final int STATE_WAITING = 1;
	/**
	 * State: dragging
	*/
	private static final int STATE_DRAGING = 2;
	/**
	 * State: transition state to check starting zoom/rotation
	 */
	private static final int STATE_CHECKING = 3;
	/**
	 * State: zooming
	*/
	private static final int STATE_ZOOMING = 4;
	/**
	 * State: Rotating
	 */
	private static final int STATE_ROTATING = 5;

	/**
	 * default value of maximum zoom scale
	*/
	private static final float DEFAULT_MAX_SCALE = 8.f;
	/**
	 * default value of minimum zoom scale
	 */
	private static final float DEFAULT_MIN_SCALE = 0.1f;
	/**
	 * default value without zooming
	*/
	private static final float DEFAULT_SCALE = 1.f;
	/**
	 * minimum distance between touch positions when start zooming/rotating
	 */
	private static final float MIN_DISTANCE = 15.f;
	private static final float MIN_DISTANCE_SQUARE = MIN_DISTANCE * MIN_DISTANCE;
	/**
	 * limit value to prevent the image disappearing from the view when moving
	 */
	private static final int MOVE_LIMIT = 50;
	/**
     * the duration in milliseconds we will wait to start rotating(if multi touched)/reseting(if single touched)
	 */
    private static final int CHECK_TIMEOUT
    	= ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout();
    /**
     * the duration in milliseconds we will wait to reset reversing the color of image
     */
    private static final int REVERSING_TIMEOUT = 100;
    /**
	 * conversion factor from radian to degree
	 */
	private static final float TO_DEGREE = 57.2957795130823f;	// = (1.0f / Math.PI) * 180.0f;
	/**
	 * ColorMatrix data for reversing image
	 */
	private static final float[] REVERSE = {
	    -1.0f,   0.0f,   0.0f,  0.0f,  255.0f,
	     0.0f,  -1.0f,   0.0f,  0.0f,  255.0f,
	     0.0f,   0.0f,  -1.0f,  0.0f,  255.0f,
	     0.0f,   0.0f,   0.0f,  1.0f,    0.0f,
	};
	/**
	 *
	 */
	private static final float EPS = 0.1f;

//--------------------------------------------------------------------------------
// variables
	/**
	 * flag for save/restore state of this view
	 */
	private boolean mIsRestored;
	/**
	 * Matrix for zooming/moving/rotating
	 */
	protected final Matrix mImageMatrix = new Matrix();
	/**
	 * flag when mImageMatrix is changed(for updating Matrix cache)
	 */
	protected boolean mImageMatrixChanged;
	/**
	 * Matrix cache of mImageMatrix elements</br>
	 * to reduce overhead of JINI call in the Matrix
	 */
	protected final float[] mMatrixCache = new float[9];
	/**
	 * for save the Matrix when touch operation start
	 */
	private final Matrix mSavedImageMatrix = new Matrix();
	/**
	 * limit bounds that image can move
	 */
	private final RectF mLimitRect = new RectF();
	/**
	 * limit line segments tha image can move
	 */
	private final LineSegment[] mLimitSegments = new LineSegment[4];
	/**
	 * actual size of image in ImageView(copy from ImageView#getDrawable#getBounds)
	 */
	private final RectF mImageRect = new RectF();
	/**
	 * scaled and moved and rotated corner coordinates of image
	 * [(left,top),(right,top),(right,bottom),(left.bottom)]
	 */
	private final float[] mTrans = new float[8];
	/**
	 * touch ids for touch operations
	 */
	private int mPrimaryId, mSecondaryId;
	/**
	 * x/y coordinates of primary touch point
	 */
	private float mPrimaryX, mPrimaryY;
	/**
	 * x/y coordinates of second touch for rotation
	 */
	private float mSecondX, mSecondY;
	/**
	 * x/y coordinates of pivot point for zooming/rotating
	 */
	private float mPivotX, mPivotY;
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
	private float mMaxScale = DEFAULT_MAX_SCALE;
	/**
	 * Minimum zoom scale, set in #init as fit the image to this view bounds
	 */
	private float mMinScale = DEFAULT_MIN_SCALE;
	/**
	 * current state, -1/STATE_NON/STATE_WATING/STATE_DRAGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private int mState;
	/**
	 * listener for visual/sound feedback on start rotating
	 */
	private OnStartRotationListener mOnStartRotationListener;
	/**
	 * ColorFilter to reverse the color of the image
	 * for default visual feedbak on start rotating
	 */
	private ColorFilter mColorReverseFilter;
	/**
	 * backup of ColorFilter to restore after image color reversing
	 */
	private ColorFilter mSavedColorFilter;
	/**
	 * Runnable instance to wait starting image reset
	 */
	private Runnable mWaitImageReset;
	/**
	 * Runnable instance to wait starting rotation
	 */
	private Runnable mStartCheckRotate;
	/**
	 * Runnable instcance to wait restoring the image color
	 */
	private Runnable mWaitReverseReset;

//--------------------------------------------------------------------------------
	/**
	 * callback listener called when rotation started.
	 */
	public interface OnStartRotationListener {
		/**
		 * this method is called when rotating starts.</br>
		 * you will execute feedback something like sound and/or visual effects.
		 * @param view
		 * @return if return false, we execute a default visual effect(color reversing)
		 */
		public boolean onStartRotation(final ITransformView view);
	}

	/**
	 * Runnable to wait restoring the image color
	 */
	private final class WaitReverseReset implements Runnable {
		@Override
		public void run() {
			resetColorFilter();
		}
	}

	/**
	 * Runnable to wait resetting the image
	 */
	private final class WaitImageReset implements Runnable {
		@Override
		public void run() {
			mParent.init();
		}
	}

	/**
	 * Runnable to wait starting rotation
	 */
	private final class StartCheckRotate implements Runnable {
		@Override
		public void run() {
			if (mState == STATE_CHECKING) {
				setState(STATE_ROTATING);
				callOnStartRotationListener();
			}
		}
	}

	/**
	 * class for process to save and restore the view state
	 */
	public static final class SavedState extends View.BaseSavedState {

		private int mState;
		private float mMinScale;
		private float mCurrentDegrees;
		private float[] mMatrixCache = new float[9];

        /**
         * constractor to restore state
         */
        public SavedState(final Parcel in) {
            super(in);
            readFromParcel(in);
        }

        /**
         * constructor to saved state
         */
        public SavedState(final Parcelable superState) {
            super(superState);
        }

        private void readFromParcel(final Parcel in) {
            // should read as same order when writing
            mState = in.readInt();
            mMinScale = in.readFloat();
            mCurrentDegrees = in.readFloat();
            in.readFloatArray(mMatrixCache);
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            // should write as same order when reading
            out.writeInt(mState);
            out.writeFloat(mMinScale);
            out.writeFloat(mCurrentDegrees);
            out.writeFloatArray(mMatrixCache);
        }

        public static final Creator<SavedState> CREATOR
        	= new Creator<SavedState>() {

            public SavedState createFromParcel(final Parcel source) {
                return new SavedState(source);
            }

            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
    }

//--------------------------------------------------------------------------------

	@NonNull
	private final ITransformView mParent;
	public ViewTransformDelegater(@NonNull final ITransformView parent) {
		mParent = parent;
	}

	public void onRestoreInstanceState(final Parcelable state) {
		if (state instanceof SavedState) {
			final SavedState saved = (SavedState)state;
			mIsRestored = true;
			System.arraycopy(saved.mMatrixCache, 0, mMatrixCache, 0, saved.mMatrixCache.length);
			mImageMatrix.setValues(mMatrixCache);
			mState = saved.mState;
			mMinScale = saved.mState;
			mCurrentDegrees = saved.mCurrentDegrees;
		}
	}

	public Parcelable onSaveInstanceState(final Parcelable superState) {
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");

		final SavedState saveState = new SavedState(superState);
		updateMatrixCache();
		saveState.mState = mState;
		saveState.mMinScale = mMinScale;
		saveState.mCurrentDegrees = mCurrentDegrees;
		saveState.mMatrixCache = mMatrixCache;
		return saveState;
	}

	public void onConfigurationChanged(final Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "onConfigurationChanged:" + newConfig);

		mIsRestored = false;
		// XXX need something?
	}

	public void setColorFilter(final ColorFilter cf) {

		// save the ColorFilter to restore after default visual feedback on start rotating
		mSavedColorFilter = cf;
	}

	public void onLayout(final boolean changed,
		final int left, final int top, final int right, final int bottom) {

		if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)-(%d,%d)",
			left, top, right, bottom));
		mState = -1;	// reset state
		mParent.init();
	}

	public boolean onTouchEvent(final MotionEvent event) {

		if (DEBUG) Log.v(TAG, "onTouchEvent:");

		final int actionCode = event.getActionMasked();	// >= API8

		switch (actionCode) {
		case MotionEvent.ACTION_DOWN:
			// single touch
			startWaiting(event);
			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
		{
			// start multi touch, zooming/rotating
			switch (mState) {
			case STATE_WAITING:
				mParent.removeCallbacks(mWaitImageReset);
			case STATE_DRAGING:
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
			case STATE_WAITING:
				if (checkTouchMoved(event)) {
					mParent.removeCallbacks(mWaitImageReset);
					setState(STATE_DRAGING);
					return true;
				}
				break;
			case STATE_DRAGING:
				if (processDrag(event))
					return true;
				break;
			case STATE_CHECKING:
				if (checkTouchMoved(event)) {
					startZoom(event);
					return true;
				}
				break;
			case STATE_ZOOMING:
				if (processZoom(event))
					return true;
				break;
			case STATE_ROTATING:
				if (processRotate(event))
					return true;
				break;
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mParent.removeCallbacks(mWaitImageReset);
			mParent.removeCallbacks(mStartCheckRotate);
			resetColorFilter();
		case MotionEvent.ACTION_POINTER_UP:
			setState(STATE_NON);
			break;
		}
		return false;
	}

	/**
	 * set maximum zooming scale
	 * @param maxScale
	 */
	public void setMaxScale(final float maxScale) {
		if ((mMinScale > maxScale) || (maxScale <= 0)) return;
		if (mMaxScale != maxScale) {
			mMaxScale = maxScale;
			checkScale();
		}
	}

	/**
	 * set minimum zooming scale
	 * @param minScale
	 */
	public void setMinScale(final float minScale) {
		if ((mMaxScale < minScale) || (minScale <= 0)) return;
		if (mMinScale != minScale) {
			mMinScale = minScale;
			checkScale();
		}
	}

	/**
	 * reset the zooming/rotating;
	 */
	public void reset() {
		if (DEBUG) Log.v(TAG, "reset:");
		init();
	}

	/**
	 * set listener on start rotating (for visual/sound feedback)
	 * @param listener
	 */
	public void setOnStartRotationListener(final OnStartRotationListener listener) {
		mOnStartRotationListener = listener;
	}

	/**
	 * return current listener
	 * @return
	 */
	public OnStartRotationListener getOnStartRotationListener() {
		return mOnStartRotationListener;
	}

	/**
	 * return current scale
	 * @return
	 */
	public float getScale() {
		return getMatrixScale();
	}

	/**
	 * return current image translate values(offset)
	 * @param result
	 * @return
	 */
	public PointF getTranslate(final PointF result) {
		updateMatrixCache();
		if (result != null) {
			result.set(mMatrixCache[Matrix.MTRANS_X], mMatrixCache[Matrix.MTRANS_Y]);
		}
		return result;
	}

	/**
	 * get current rotating degrees
	 */
	public float getRotation() {
		return mCurrentDegrees;
	}

	/**
	 * initialization of ITransformView called from #init
	 */
	public void init() {
		if (DEBUG) Log.v(TAG, "init:" + mIsRestored);
		clearCallbacks();
		if (!mIsRestored) {
			mParent.onInit();
			// set the initial state to idle, get and save the internal Matrix.
			mState = -1; setState(STATE_NON);
			// get the internally calculated zooming scale to fit the view
			mMinScale = getMatrixScale();
			mCurrentDegrees = 0.f;
		}
		mIsRestored = false;
		mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > EPS;

		// update image size
		// current implementation of ImageView always hold its image as a Drawable
		// (that can get ImageView#getDrawable)
		// therefore update the image size from its Drawable
		final RectF bounds = mParent.getBounds();
		if (bounds != null) {
			mImageRect.set(bounds);
		} else {
			mImageRect.setEmpty();
		}
		// set limit rectangle that the image can move
		mLimitRect.set(mParent.getDrawingRect());
		mLimitRect.inset(MOVE_LIMIT, MOVE_LIMIT);
		mLimitSegments[0] = null;
	}

	/**
	 * remove all callbacks if they are in the message queue
	 */
	public void clearCallbacks() {
		if (DEBUG) Log.v(TAG, "clearCallbacks:");
		if (mWaitImageReset != null)
			mParent.removeCallbacks(mWaitImageReset);
		if (mStartCheckRotate != null)
			mParent.removeCallbacks(mStartCheckRotate);
		if (mWaitReverseReset != null)
			mParent.removeCallbacks(mWaitReverseReset);
	}

	/**
	 * check zooming scale range
	 */
	private final void checkScale() {
		if (DEBUG) Log.v(TAG, "checkScale:");
		float scale = getMatrixScale();
		if (scale < mMinScale) {
			scale = mMinScale;
			mImageMatrix.setScale(scale, scale);
			mImageMatrixChanged = true;
			mParent.invalidate();
		} else if (scale > mMaxScale) {
			scale = mMaxScale;
			mImageMatrix.setScale(scale, scale);
			mImageMatrixChanged = true;
			mParent.invalidate();
		}
	}

	/**
	 * set current state, get and save the internal Matrix int super class
	 * @param state:	-1/STATE_NON/STATE_DRAGING/STATECHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private final void setState(final int state) {
		if (DEBUG) Log.v(TAG, String.format("setState:%d→%d", mState, state));

		if (mState != state) {
			mState = state;
			// get and save the internal Matrix of super class
			mSavedImageMatrix.set(mParent.getImageMatrixSp());
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
		if (DEBUG) Log.v(TAG, "startWaiting:");

		mPrimaryId = 0;
		mSecondaryId = -1;
		mPrimaryX = mSecondX = event.getX();
		mPrimaryY = mSecondY = event.getY();
		if (mWaitImageReset == null) mWaitImageReset = new WaitImageReset();
		mParent.postDelayed(mWaitImageReset, CHECK_TIMEOUT);
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
		mTrans[0] = mTrans[6] = mImageRect.left;
		mTrans[1] = mTrans[3] = mImageRect.top;
		mTrans[5] = mTrans[7] = mImageRect.bottom;
		mTrans[2] = mTrans[4] = mImageRect.right;
		mImageMatrix.mapPoints(mTrans);
		for (int i = 0; i < 8; i += 2) {
			mTrans[i] += dx;
			mTrans[i+1] += dy;
		}
		// check whether the image can move
		// if we can ignore rotating, the limit check is more easy...
		boolean canMove
			// check whether at lease one corner of image bounds is in the limitRect
			 = mLimitRect.contains(mTrans[0], mTrans[1])
			|| mLimitRect.contains(mTrans[2], mTrans[3])
			|| mLimitRect.contains(mTrans[4], mTrans[5])
			|| mLimitRect.contains(mTrans[6], mTrans[7])
			// check whether at least one corner of limitRect is in the image bounds
			|| ptInPoly(mLimitRect.left, mLimitRect.top, mTrans)
			|| ptInPoly(mLimitRect.right, mLimitRect.top, mTrans)
			|| ptInPoly(mLimitRect.right, mLimitRect.bottom, mTrans)
			|| ptInPoly(mLimitRect.left, mLimitRect.bottom, mTrans);
		if (!canMove) {
			// when no corner is in, we need additional check whether at least
			// one side of image bounds intersect with the limit rectangle
			if (mLimitSegments[0] == null) {
				mLimitSegments[0] = new LineSegment(mLimitRect.left, mLimitRect.top, mLimitRect.right, mLimitRect.top);
				mLimitSegments[1] = new LineSegment(mLimitRect.right, mLimitRect.top, mLimitRect.right, mLimitRect.bottom);
				mLimitSegments[2] = new LineSegment(mLimitRect.right, mLimitRect.bottom, mLimitRect.left, mLimitRect.bottom);
				mLimitSegments[3] = new LineSegment(mLimitRect.left, mLimitRect.bottom, mLimitRect.left, mLimitRect.top);
			}
			final LineSegment side = new LineSegment(mTrans[0], mTrans[1], mTrans[2], mTrans[3]);
			canMove = checkIntersect(side, mLimitSegments);
			if (!canMove) {
				side.set(mTrans[2], mTrans[3], mTrans[4], mTrans[5]);
				canMove = checkIntersect(side, mLimitSegments);
				if (!canMove) {
					side.set(mTrans[4], mTrans[5], mTrans[6], mTrans[7]);
					canMove = checkIntersect(side, mLimitSegments);
					if (!canMove) {
						side.set(mTrans[6], mTrans[7], mTrans[0], mTrans[1]);
						canMove = checkIntersect(side, mLimitSegments);
					}
				}
			}
		}
		if (canMove) {
			// TODO we need adjust dx/dy not to penetrate into the limit rectangle
			// otherwise the image can not move when one side is on the border of limit rectangle.
			// only calculate without rotation now because its calculation is to heavy when rotation applied.
			if (!mIsRotating) {
				final float left = Math.min(Math.min(mTrans[0], mTrans[2]), Math.min(mTrans[4], mTrans[6]));
				final float right = Math.max(Math.max(mTrans[0], mTrans[2]), Math.max(mTrans[4], mTrans[6]));
				final float top = Math.min(Math.min(mTrans[1], mTrans[3]), Math.min(mTrans[5], mTrans[7]));
				final float bottom = Math.max(Math.max(mTrans[1], mTrans[3]), Math.max(mTrans[5], mTrans[7]));

				if (right < mLimitRect.left) {
					dx = mLimitRect.left - right;
				} else if (left + EPS > mLimitRect.right) {
					dx = mLimitRect.right - left - EPS;
				}
				if (bottom < mLimitRect.top) {
					dy = mLimitRect.top - bottom;
				} else if (top + EPS > mLimitRect.bottom) {
					dy = mLimitRect.bottom - top - EPS;
				}
			}
			if ((dx != 0) || (dy != 0)) {
//				if (DEBUG) Log.v(TAG, String.format("processDrag:dx=%f,dy=%f", dx, dy));
				// apply move
				if (mImageMatrix.postTranslate(dx, dy)) {
					// when image is really moved?
					mImageMatrixChanged = true;
					// apply to super class
					mParent.setImageMatrixSp(mImageMatrix);
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
		if (DEBUG) Log.v(TAG, "startCheck:" + event);

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
			if (mStartCheckRotate == null)
				mStartCheckRotate = new StartCheckRotate();
			mParent.postDelayed(mStartCheckRotate, CHECK_TIMEOUT);
			setState(STATE_CHECKING); 		// start zoom/rotation check
		}
	}

	/**
	 * start zooming
	 * @param event
	 * @return
	 */
	private final void startZoom(final MotionEvent event) {

		mParent.removeCallbacks(mStartCheckRotate);
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
			mParent.setImageMatrixSp(mImageMatrix);
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

		boolean result = true;
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
			mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > EPS;
			if (mIsRotating && mImageMatrix.postRotate(mCurrentDegrees, mPivotX, mPivotY)) {
				// when Matrix is changed
				mImageMatrixChanged = true;
				// apply to super class
				mParent.setImageMatrixSp(mImageMatrix);
				return true;
			}
		}
		return false;
	}

	@SuppressLint("NewApi")
	private final void callOnStartRotationListener() {
		if (DEBUG) Log.v(TAG, "callOnStartRotationListener:");

		boolean result = false;
		if (mOnStartRotationListener != null)
		try {
			result = mOnStartRotationListener.onStartRotation(mParent);
		} catch (Exception e) {
			if (DEBUG) Log.w(TAG, e);
		}
		if (!result) {
			if (mColorReverseFilter == null) {
				mColorReverseFilter = new ColorMatrixColorFilter(new ColorMatrix(REVERSE));
			}
			mParent.setColorFilterSp(mColorReverseFilter);
			// post runnable to reset the color reversing
			if (mWaitReverseReset == null) mWaitReverseReset = new WaitReverseReset();
			mParent.postDelayed(mWaitReverseReset, REVERSING_TIMEOUT);
		}
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
			final double cos = dotProduct(x0, y0, x1, y1) / Math.sqrt(s);
			angle = TO_DEGREE * (float)Math.acos(cos) * Math.signum(crossProduct(x0, y0, x1, y1));
		}
		return angle;
	}

	private static final float dotProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * x1 + y0 * y1;
	}

	private static final float crossProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * y1 - x1 * y0;
	}

	private static final float crossProduct(final Vector v1, final Vector v2) {
		return v1.x * v2.y - v2.x * v1.y;
	}

	private static final Vector sPtInPoly_v1 = new Vector();
	private static final Vector sPtInPoly_v2 = new Vector();
	/**
	 * check whether the point is in the clockwise 2D polygon
	 * @param x
	 * @param y
	 * @param poly: the array of polygon coordinates(x,y pairs)
	 * @return
	 */
	private static final boolean ptInPoly(final float x, final float y, final float[] poly) {

		final int n = poly.length & 0x7fffffff;
		// minimum 3 points(3 pair of x/y coordinates) need to calculate >> length >= 6
		if (n < 6) return false;
		boolean result = true;
		for (int i = 0; i < n; i += 2) {
			sPtInPoly_v1.set(x, y).dec(poly[i], poly[i + 1]);
			if (i + 2 < n) sPtInPoly_v2.set(poly[i + 2], poly[i + 3]);
			else sPtInPoly_v2.set(poly[0], poly[1]);
			sPtInPoly_v2.dec(poly[i], poly[i + 1]);
			if (crossProduct(sPtInPoly_v1, sPtInPoly_v2) > 0) {
				if (DEBUG) Log.v(TAG, "pt is outside of a polygon:");
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * helper for intersection check etc.
	 */
	private static final class Vector {

		public float x, y;

		public Vector() {
		}

/*		public Vector(Vector src) {
			set(src);
		} */

		public Vector(final float x, final float y) {
			set(x, y);
		}

		public Vector set(final float x, final float y) {
			this.x = x;
			this.y = y;
			return this;
		}

/*		public Vector set(final Vector other) {
			x = other.x;
			y = other.y;
			return this;
		} */

/*		public Vector add(final Vector other) {
			return new Vector(x + other.x, y + other.y);
		} */

/*		public Vector add(final float x, final float y) {
			return new Vector(this.x + x, this.y + y);
		} */

/*		public Vector inc(final Vector other) {
			x += other.x;
			y += other.y;
			return this;
		} */

/*		public Vector inc(final float x, final float y) {
			this.x += x;
			this.y += y;
			return this;
		} */

		public Vector sub(final Vector other) {
			return new Vector(x - other.x, y - other.y);
		}

/*		public Vector sub(final float x, final float y) {
			return new Vector(this.x - x, this.y - y);
		} */

/*		public Vector dec(final Vector other) {
			x -= other.x;
			y -= other.y;
			return this;
		} */

		public Vector dec(final float x, final float y) {
			this.x -= x;
			this.y -= y;
			return this;
		}
	}

	private static final class LineSegment {

		public final Vector p1;
		public final Vector p2;

		public LineSegment (final float x0, final float y0, final float x1, final float y1) {
			p1 = new Vector(x0, y0);
			p2 = new Vector(x1, y1);
		}

		public LineSegment set(final float x0, final float y0, final float x1, final float y1) {
			p1.set(x0, y0);
			p2.set(x1,  y1);
			return this;
		}

/*		@Override
		public String toString() {
			return String.format(Locale.US, "p1=(%f,%f),p2=(%f,%f)", p1.x, p1.y, p2.x, p2.y);
		} */
	}

	/**
	 * check whether line segment(seg) intersects with at least one of line segments in the array
	 * @param seg
	 * @param segs array of segment
	 * @return true if line segment intersects with at least one of other line segment.
	 */
	private static final boolean checkIntersect(final LineSegment seg, final LineSegment[] segs) {

		boolean result = false;
		final int n = segs != null ? segs.length : 0;

		final Vector a = seg.p2.sub(seg.p1);
		Vector b, c, d;
		for (int i= 0; i < n; i++) {
			c = segs[i].p1.sub(seg.p1);
			d = segs[i].p2.sub(seg.p1);
			result = crossProduct(a, c) * crossProduct(a, d) < EPS;
			if (result) {
				b = segs[i].p2.sub(segs[i].p1);
				c = seg.p1.sub(segs[i].p1);
				d = seg.p2.sub(segs[i].p1);
				result = crossProduct(b, c) * crossProduct(b, d) < EPS;
				if (result) {
					break;
				}
			}
		}
		return result;
	}

	private void resetColorFilter() {
		mParent.setColorFilterSp(mSavedColorFilter);
	}

	/**
	 * get the zooming scale</br>
	 * return minimum one of MSCALE_X and MSCALE_Y
	 * @return return DEFAULT_SCALE when the scale is smaller than or equal to zero
	 */
	private final float getMatrixScale() {
		updateMatrixCache();
		final float scale = Math.min(mMatrixCache[Matrix.MSCALE_X], mMatrixCache[Matrix.MSCALE_X]);
		if (scale <= 0f) {	// for prevent disappearing reversing
			if (DEBUG) Log.w(TAG, "getMatrixScale:scale<=0, set to default");
			return DEFAULT_SCALE;
		}
		return scale;
	}

	/**
	 * set a value to mImageMatrix
	 * @param index
	 * @param value
	 */
	protected void setMatrixValue(final int index, final float value) {
		updateMatrixCache();
		mMatrixCache[index] = value;
		mImageMatrix.setValues(mMatrixCache);
	}

	/**
	 * restore the Matrix to the one when state changed
	 */
	protected void restoreMatrix() {
		mImageMatrix.set(mSavedImageMatrix);
		mImageMatrixChanged = true;
	}

	/**
	 * update the matrix caching float[]
	 */
	protected boolean updateMatrixCache() {
		if (mImageMatrixChanged) {
			mImageMatrix.getValues(mMatrixCache);
			mImageMatrixChanged = false;
			return true;
		}
		return false;
	}

	public Matrix getImageMatrix() {
		return mImageMatrix;
	}

}
