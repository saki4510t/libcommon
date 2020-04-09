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

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.serenegiant.widget.IScaledView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.serenegiant.widget.IScaledView.*;

/**
 * 拡大縮小平行移動回転可能なViewのためのdelegater
 */
public abstract class ViewTransformDelegater extends ViewTransformer {

	private static final boolean DEBUG = false;	// TODO for debugging
	private static final String TAG = ViewTransformDelegater.class.getSimpleName();

//--------------------------------------------------------------------------------
	// constants
	public static final int TOUCH_DISABLED			= 0x00000000;
	public static final int TOUCH_ENABLED_MOVE		= 0x00000001;
	public static final int TOUCH_ENABLED_ZOOM		= 0x00000001 << 1;
	public static final int TOUCH_ENABLED_ROTATE	= 0x00000001 << 2;
	public static final int TOUCH_ENABLED_ALL
		= TOUCH_ENABLED_MOVE | TOUCH_ENABLED_ZOOM | TOUCH_ENABLED_ROTATE;	// 0x00000007

	@IntDef(flag=true, value = {
		TOUCH_DISABLED,
		TOUCH_ENABLED_MOVE,
		TOUCH_ENABLED_ZOOM,
		TOUCH_ENABLED_ROTATE,
		TOUCH_ENABLED_ALL,})
	@Retention(RetentionPolicy.SOURCE)
	public @interface TouchMode {}

//--------------------------------------------------------------------------------
	public static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout() * 2;
	public static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

//--------------------------------------------------------------------------------
	/**
	 * ステートをリセットするときの一時値
	 */
	public static final int STATE_RESET = -1;
	/**
	 * State: ユーザー操作無し
	 */
	public static final int STATE_NON = 0;
	/**
	 * State: シングルタッチがあったのでユーザー操作待機中
	 */
	public static final int STATE_WAITING = 1;
	/**
	 * State: 平行移動処理中
	*/
	public static final int STATE_DRAGGING = 2;
	/**
	 * State: 拡大縮小・回転操作の待機中
	 */
	public static final int STATE_CHECKING = 3;
	/**
	 * State: 拡大縮小処理中
	*/
	public static final int STATE_ZOOMING = 4;
	/**
	 * State: 回転処理中
	 */
	public static final int STATE_ROTATING = 5;

	@IntDef({
		STATE_RESET,
		STATE_NON,
		STATE_WAITING,
		STATE_DRAGGING,
		STATE_CHECKING,
		STATE_ZOOMING,
		STATE_ROTATING})
	@Retention(RetentionPolicy.SOURCE)
	public @interface State {}

//--------------------------------------------------------------------------------
	/**
	 * 最大拡大率のデフォルト値
	*/
	public static final float DEFAULT_MAX_SCALE = 10.0f;
	/**
	 * 最小縮小率のデフォルト値
	 */
	public static final float DEFAULT_MIN_SCALE = 0.05f;
	/**
	 * 拡大縮小率のデフォルト値
	*/
	public static final float DEFAULT_SCALE = 1.f;
	/**
	 * 拡大縮小・回転処理開始時の最小タッチ間隔
	 * この値より小さい場合には拡大縮小・回転処理を行わない
	 */
	public static final float MIN_DISTANCE = 15.f;
	/**
	 * 拡大縮小・回転処理開始時の最小タッチ間隔の2乗
	 * 処理の高速化のためにタッチ間隔の計算で平方根の処理をおこなわずに済むように
	 */
	public static final float MIN_DISTANCE_SQUARE = MIN_DISTANCE * MIN_DISTANCE;
	/**
	 * 平行移動時に表示内容がView外へ出てしまうのを防ぐための制限値
	 */
	public static final float MOVE_LIMIT_RATE = 0.2f;	// =Viewの幅/高さのそれぞれ20%
	/**
     * マルチタッチ時に回転処理へ遷移するまでの待機時間[ミリ秒]/シングルロングタッチでリセットするまでの待機時間[ミリ秒]
	 */
	public static final int CHECK_TIMEOUT
    	= ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout();

//--------------------------------------------------------------------------------
	/**
	 * callback listener called when rotation started.
	 */
	public interface ViewTransformListener {
		/**
		 * タッチ状態が変化したとき
		 * @param view
		 * @param newState
		 */
		public void onStateChanged(@NonNull final View view, final int newState);
		/**
		 * トランスフォームマトリックスが変化したときの処理
 		 * @param view
		 * @param transform
		 */
		public void onTransformed(@NonNull final View view, @NonNull final Matrix transform);
	}

	/**
	 * Runnable to wait resetting the image
	 */
	private final class WaitImageReset implements Runnable {
		@Override
		public void run() {
			getTargetView().requestLayout();
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
			}
		}
	}

	/**
	 * class for process to save and restore the view state
	 */
	public static class SavedState extends View.BaseSavedState {

		private int mState;
		private int mHandleTouchEvent;
		private float mMinScale;
		private float mMaxScale;
		private float mCurrentDegrees;
		private final float[] mMatrixCache = new float[9];

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
			mHandleTouchEvent = in.readInt();
            mMinScale = in.readFloat();
			mMaxScale = in.readFloat();
            mCurrentDegrees = in.readFloat();
            in.readFloatArray(mMatrixCache);
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            // should write as same order when reading
            out.writeInt(mState);
            out.writeInt(mHandleTouchEvent);
            out.writeFloat(mMinScale);
			out.writeFloat(mMaxScale);
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
// variables
	/**
	 * スケールモード
	 */
	@IScaledView.ScaleMode
	private int mScaleMode;
	/**
	 * スケールモードがキープアスペクトの場合にViewのサイズをアスペクト比に合わせて変更するかどうか
	 */
	private boolean mNeedResizeToKeepAspect;
	/**
	 * タッチ操作の有効無効設定
	 */
	@TouchMode
	private int mHandleTouchEvent = TOUCH_ENABLED_ALL;
	/**
	 * flag for save/restore state of this view
	 */
	private boolean mIsRestored;
	/**
	 * 表示内容のトランスフォームマトリックス
	 */
	protected final Matrix mImageMatrix = new Matrix();
	/**
	 * タッチ操作開始時のトランスフォームマトリックスを保存
	 */
	private final Matrix mSavedImageMatrix = new Matrix();
	/**
	 * 移動可能範囲を指定
	 */
	private final RectF mLimitRect = new RectF();
	/**
	 * 移動範囲制限のためのLineSegment配列
	 */
	private final ViewUtils.LineSegment[] mLimitSegments = new ViewUtils.LineSegment[4];
	/**
	 * 表示内容の実際のサイズ
	 */
	private final RectF mContentRect = new RectF();
	/**
	 * 拡大縮小回転平行移動した表示内容の四隅の座標
	 * [(left,top),(right,top),(right,bottom),(left.bottom)]
	 */
	private final float[] mTransCoords = new float[8];
	/**
	 * タッチ操作時のタッチID
	 */
	private int mPrimaryId, mSecondaryId;
	/**
	 * 最初にタッチした座標を保持
	 */
	private float mPrimaryX, mPrimaryY;
	/**
	 * 2つめのタッチ座標を保持
	 */
	private float mSecondX, mSecondY;
	/**
	 * 拡大縮小・回転時のピボット(中心)座標を保持
	 */
	private float mPivotX, mPivotY;
	/**
	 * マルチタッチ時の最初のタッチ間隔(拡大率計算用)
	 */
	private float mTouchDistance;
	/**
	 * 現在の回転角度
	 */
	private float mCurrentDegrees;
	/**
	 * 表示内容を回転しているかどうか
	 */
	private boolean mIsRotating;
	/**
	 * 最大拡大率
	 */
	private float mMaxScale = DEFAULT_MAX_SCALE;
	/**
	 * Minimum zoom scale, set in #init as fit the image to this view bounds
	 * 最小縮小率
	 * #initで初期化される
	 */
	private float mMinScale = DEFAULT_MIN_SCALE;
	/**
	 * current state, -1/STATE_NON/STATE_WATING/STATE_DRAGGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	@State
	private int mState = STATE_NON;
	/**
	 * listener for visual/sound feedback on start rotating
	 */
	@Nullable
	private ViewTransformListener mViewTransformListener;
	/**
	 * シングルロングタッチでリセットを開始するのを待機するためのRunnable
	 */
	private Runnable mWaitImageReset;
	/**
	 * 回転処理開始待ちを行うRunnable
	 */
	private Runnable mStartCheckRotate;
	/**
	 * フィードバック用に表示内容の色を反転させた後にもとに戻すためのRunnable
	 */
	private Runnable mWaitReverseReset;

//--------------------------------------------------------------------------------
	/**
	 * コンストラクタ
	 * @param parent
	 */
	public ViewTransformDelegater(@NonNull final View parent) {
		super(parent);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (parent instanceof ViewTransformListener) {
			mViewTransformListener = (ViewTransformListener)parent;
		}
	}

	@NonNull
	@Override
	public ViewTransformer setTransform(@Nullable final Matrix transform) {
		if (DEBUG) Log.v(TAG, "setTransform:" + transform);
		super.setTransform(transform);
		if (mViewTransformListener != null) {
			mViewTransformListener.onTransformed(getTargetView(), transform);
		}
		return this;
	}

	/**
	 * View#onRestoreInstanceStateの追加処理
	 * Viewの状態を復帰
	 * @param state
	 */
	public void onRestoreInstanceState(final Parcelable state) {
		if (DEBUG) Log.v(TAG, "onRestoreInstanceState:");
		if (state instanceof SavedState) {
			final SavedState saved = (SavedState)state;
			mIsRestored = true;
			mImageMatrix.setValues(saved.mMatrixCache);
			mState = saved.mState;
			mHandleTouchEvent = saved.mHandleTouchEvent;
			mMinScale = saved.mMinScale;
			mMaxScale = saved.mMaxScale;
			mCurrentDegrees = saved.mCurrentDegrees;
		}
	}

	/**
	 * View#onSaveInstanceStateの追加処理
	 * Viewの状態を保存
	 * @param superState
	 * @return
	 */
	public Parcelable onSaveInstanceState(final Parcelable superState) {
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");

		final SavedState saveState = new SavedState(superState);
		saveState.mState = mState;
		saveState.mHandleTouchEvent = mHandleTouchEvent;
		saveState.mMinScale = mMinScale;
		saveState.mMaxScale = mMaxScale;
		saveState.mCurrentDegrees = mCurrentDegrees;
		mImageMatrix.getValues(saveState.mMatrixCache);
		return saveState;
	}

	/**
	 * View#onConfigurationChangedの追加処理
	 * @param newConfig
	 */
	public void onConfigurationChanged(final Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "onConfigurationChanged:" + newConfig);

		mIsRestored = false;
		// XXX need something?
	}

	/**
	 * View#onTouchEventの処理
	 * falseを返したときにはView#super.onTouchEventでデフォルトの処理をすること
	 * @param event
	 * @return
	 */
	@SuppressLint("SwitchIntDef")
	public boolean onTouchEvent(final MotionEvent event) {
//		if (DEBUG) Log.v(TAG, "onTouchEvent:");

		if (mHandleTouchEvent == TOUCH_DISABLED) {
			return false;
		}

		final View view = getTargetView();
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
				view.removeCallbacks(mWaitImageReset);
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
			case STATE_WAITING:
				if (((mHandleTouchEvent & TOUCH_ENABLED_MOVE) == TOUCH_ENABLED_MOVE)
					&& checkTouchMoved(event)) {

					view.removeCallbacks(mWaitImageReset);
					setState(STATE_DRAGGING);
					return true;
				}
				break;
			case STATE_DRAGGING:
				if (processDrag(event))
					return true;
				break;
			case STATE_CHECKING:
				if (checkTouchMoved(event)
					&& ((mHandleTouchEvent & TOUCH_ENABLED_ZOOM) == TOUCH_ENABLED_ZOOM)) {

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
			// pass through
		case MotionEvent.ACTION_UP:
			view.removeCallbacks(mWaitImageReset);
			view.removeCallbacks(mStartCheckRotate);
			if ((actionCode == MotionEvent.ACTION_UP) && (mState == STATE_WAITING)) {
				final long downTime = SystemClock.uptimeMillis() - event.getDownTime();
				if (downTime > LONG_PRESS_TIMEOUT) {
					view.performLongClick();
				} else if (downTime < TAP_TIMEOUT) {
					view.performClick();
				}
			}
			// pass through
		case MotionEvent.ACTION_POINTER_UP:
			setState(STATE_NON);
			break;
		}
		return false;
	}

	/**
	 * 回転処理開始時のコールバックリスナー(ユーザーフィードバック用)を設定
	 * @param listener
	 */
	public void setViewTransformListener(@Nullable final ViewTransformListener listener) {
		mViewTransformListener = listener;
	}

	/**
	 * 現在設定されている回転処理開始時のコールバックリスナーを取得
	 * @return
	 */
	public ViewTransformListener getViewTransformListener() {
		return mViewTransformListener;
	}

	/**
	 * タッチ操作の有効無効設定
	 * @param enabled
	 */
	public void setEnableHandleTouchEvent(@TouchMode final int enabled) {
		if (DEBUG) Log.v(TAG, "setEnableHandleTouchEvent:" + enabled);
		mHandleTouchEvent = enabled;
	}

	/**
	 * タッチ操作の有効無効設定を取得
	 * @return
	 */
	@TouchMode
	public int getEnableHandleTouchEvent() {
		return mHandleTouchEvent;
	}

	/**
	 * 最大拡大率を設定
	 * @param maxScale
	 */
	public void setMaxScale(final float maxScale)
		throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setMaxScale:" + maxScale);
		if ((mMinScale > maxScale) || (maxScale <= 0)) {
			throw new IllegalArgumentException();
		}
		if (mMaxScale != maxScale) {
			mMaxScale = maxScale;
			checkScale();
		}
	}

	/**
	 * 現在の最大拡大率を取得
	 * @return
	 */
	public float getMaxScale() {
		return mMaxScale;
	}

	/**
	 * 最小縮小率を設定
	 * @param minScale
	 */
	public void setMinScale(final float minScale)
		throws IllegalArgumentException {

		if (DEBUG) Log.v(TAG, "setMinScale:" + minScale);
		if ((mMaxScale < minScale) || (minScale <= 0)) {
			throw new IllegalArgumentException();
		}
		if (mMinScale != minScale) {
			mMinScale = minScale;
			checkScale();
		}
	}

	/**
	 * 現在の最小縮小率を取得
	 * @return
	 */
	public float getMinScale() {
		return mMinScale;
	}

	/**
	 * 拡大縮小率を相対値で設定
	 * @param scaleRelative
	 * @return
	 */
	public ViewTransformDelegater setScaleRelative(final float scaleRelative) {
		if (DEBUG) Log.v(TAG, String.format("setScaleRelative:%f,min=%f,max=%f",
			scaleRelative, mMinScale, mMaxScale));
		// restore the Matrix
		getTransform(mImageMatrix);
		// calculate the zooming scale from the distance between touched positions
		// calculate the applied zooming scale
		final float tmpScale = scaleRelative * getScale();
		if ((tmpScale >= mMinScale) && (tmpScale <= mMaxScale)) {
			// change scale with scale value and pivot point
			if (mImageMatrix.postScale(scaleRelative, scaleRelative,
				getViewWidth() / 2.0f, getViewHeight() / 2.0f)) {

				// when Matrix is changed, apply to target view
				setTransform(mImageMatrix);
			}
		}

		return this;
	}

	/**
	 * スケールモードを設定する
	 * @param scaleMode
	 */
	public void setScaleMode(@ScaleMode final int scaleMode) {
		if (DEBUG) Log.v(TAG, "setScaleMode:" + scaleMode);
		if (mScaleMode != scaleMode) {
			mScaleMode = scaleMode;
			getTargetView().requestLayout();
		}
	}

	/**
	 * 現在のスケールモードを取得する
	 * @return
	 */
	@ScaleMode
	public int getScaleMode() {
		return mScaleMode;
	}

	/**
	 * View表内容の拡大縮小回転平行移動を初期化
	 */
	public void init() {
		if (DEBUG) Log.v(TAG, "init:" + mIsRestored);
		mState = STATE_RESET;
		clearPendingTasks();
		if (!mIsRestored) {
			onInit();
			updateTransform(true);
			mCurrentDegrees = 0.f;
		}
		mIsRestored = false;
		mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > ViewUtils.EPS;

		// set limit rectangle that the image can move
		mLimitRect.set(getDrawingRect());
		if (DEBUG) Log.v(TAG, "init:mLimitRect=" + mLimitRect);
		// update image size
		final RectF bounds = getContentBounds();
		if ((bounds != null) && !bounds.isEmpty()) {
			mContentRect.set(bounds);
		} else {
			mContentRect.set(mLimitRect);
		}
		if (DEBUG) Log.v(TAG, "init:mContentRect=" + mContentRect);
		mLimitRect.inset((MOVE_LIMIT_RATE * getViewWidth()), (MOVE_LIMIT_RATE * getViewHeight()));
		mLimitSegments[0] = null;
		setupDefaultTransform();
		setState(STATE_NON);
	}

	/**
	 * 拡大縮小回転状態を初期化
	 * スケールモードと要求アスペクト比に合わせてトランスフォームマトリックスを初期化する
	 */
	private void setupDefaultTransform() {
		final int viewWidth = getViewWidth();
		final int viewHeight = getViewHeight();
		final float contentWidth = mContentRect.width();
		final float contentHeight = mContentRect.height();
		final float scaleX = viewWidth / contentWidth;
		final float scaleY = viewHeight / contentHeight;
		if (DEBUG) Log.v(TAG,
			String.format("setupDefaultTransform:" +
			 	"view(%dx%d),content(%.0fx%.0f)",
				viewWidth, viewHeight,
				mContentRect.width(), mContentRect.height()));

		// apply matrix
		mImageMatrix.reset();
		switch (mScaleMode) {
		case SCALE_MODE_STRETCH_TO_FIT:
			mImageMatrix.setScale(scaleX, scaleY);
			break;
		case SCALE_MODE_KEEP_ASPECT:
		case SCALE_MODE_CROP:
		{
			final float scale = mScaleMode == SCALE_MODE_CROP
				? Math.max(scaleX, scaleY)	// SCALE_MODE_CROP
				: Math.min(scaleX, scaleY);	// SCALE_MODE_KEEP_ASPECT
			final float dx = Math.round((viewWidth - contentWidth * scale) * 0.5f);
			final float dy = Math.round((viewHeight - contentHeight * scale) * 0.5f);

			mImageMatrix.setScale(scale, scale);
			mImageMatrix.postTranslate(dx, dy);
			if (DEBUG) Log.v(TAG,
				String.format("setupDefaultTransform:scale(%f,%f)→%f, d(%f,%f)",
					scaleX,scaleY, scale,
					dx, dy));
			break;
		}
		}
		if (DEBUG) Log.v(TAG, "setupDefaultTransform:scaleMode=" + mScaleMode + "," + mImageMatrix);
		setTransform(mImageMatrix);
		setDefault(mImageMatrix);
	}

	/**
	 * 実行待機中のタスクがあればクリアする
	 */
	public void clearPendingTasks() {
		if (DEBUG) Log.v(TAG, "clearPendingTasks:");
		final View view = getTargetView();
		if (mWaitImageReset != null)
			view.removeCallbacks(mWaitImageReset);
		if (mStartCheckRotate != null)
			view.removeCallbacks(mStartCheckRotate);
		if (mWaitReverseReset != null)
			view.removeCallbacks(mWaitReverseReset);
	}

	/**
	 * 拡大縮小率の範囲チェック
	 */
	private void checkScale() {
		if (DEBUG) Log.v(TAG, "checkScale:");
		float scale = getScale();
		if (scale < mMinScale) {
			scale = mMinScale;
			getTransform(mImageMatrix);
			mImageMatrix.setScale(scale, scale);
			setTransform(mImageMatrix);
		} else if (scale > mMaxScale) {
			scale = mMaxScale;
			getTransform(mImageMatrix);
			mImageMatrix.setScale(scale, scale);
			setTransform(mImageMatrix);
		}
	}

	/**
	 * 現在のステートを設定、内部使用のトランスフォームマトリックスを保存
	 * @param state:	-1/STATE_NON/STATE_DRAGGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private void setState(@State final int state) {
		if (DEBUG) Log.v(TAG, String.format("setState:%d→%d", mState, state));

		if (mState != state) {
			mState = state;
			// get and save the internal Matrix of super class
			getTransform(mSavedImageMatrix);
			mImageMatrix.set(mSavedImageMatrix);
			if (mViewTransformListener != null) {
				mViewTransformListener.onStateChanged(getTargetView(), state);
			}
		}
	}

	/**
	 * ユーザー操作開始時の処理
	 * 回転
	 * @param event
	 */
	private void startWaiting(@NonNull final MotionEvent event) {
		if (DEBUG) Log.v(TAG, "startWaiting:");

		mPrimaryId = 0;
		mSecondaryId = -1;
		mPrimaryX = mSecondX = event.getX();
		mPrimaryY = mSecondY = event.getY();
		if (mWaitImageReset == null) mWaitImageReset = new WaitImageReset();
		getTargetView().postDelayed(mWaitImageReset, CHECK_TIMEOUT);
		setState(STATE_WAITING);
	}

	/**
	 * 平行移動処理
	 * @param event
	 */
	private boolean processDrag(@NonNull final MotionEvent event) {

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
					// when Matrix is changed, apply to target view
					setTransform(mImageMatrix);
				}
			}
		}
		mPrimaryX = event.getX();
		mPrimaryY = event.getY();
		return canMove;
	}

	/**
	 * 拡大縮小回転操作待ちを開始
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
			if ((mHandleTouchEvent & TOUCH_ENABLED_ROTATE) == TOUCH_ENABLED_ROTATE) {
				if (mStartCheckRotate == null)
					mStartCheckRotate = new StartCheckRotate();
				getTargetView().postDelayed(mStartCheckRotate, CHECK_TIMEOUT);
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

		getTargetView().removeCallbacks(mStartCheckRotate);
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
		// calculate the zooming scale from the distance between touched positions
		final float scale = calcScale(event);
		// calculate the applied zooming scale
		final float tmpScale = scale * getScale();
		if (tmpScale < mMinScale) {
			// skip if the applied scale is smaller than minimum scale
			return false;
		} else if (tmpScale > mMaxScale) {
			// skip if the applied scale is bigger than maximum scale
			return false;
		}

		// change scale with scale value and pivot point
		if (mImageMatrix.postScale(scale, scale, mPivotX, mPivotY)) {
			// when Matrix is changed, apply to target view
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
			mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > ViewUtils.EPS;
			if (mIsRotating && mImageMatrix.postRotate(mCurrentDegrees, mPivotX, mPivotY)) {
				// when Matrix is changed, apply to target view
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
	 * restore the Matrix to the one when state changed
	 */
	protected void restoreMatrix() {
		mImageMatrix.set(mSavedImageMatrix);
	}

	/**
	 * ITransformViewの実装
	 * View#getDrawingRectを呼び出してViewの描画領域の大きさを取得
	 * @return
	 */
	@NonNull
	private Rect getDrawingRect() {
		final Rect r = new Rect();
		getTargetView().getDrawingRect(r);
		return r;
	}

	private int getViewWidth() {
		return getTargetView().getWidth();
	}

	private int getViewHeight() {
		return getTargetView().getHeight();
	}

	/**
	 * View表示内容の大きさを取得
	 * @return
	 */
	protected abstract RectF getContentBounds();
	/**
	 * View表内容の拡大縮小回転平行移動を初期化時の追加処理
	 * 親Viewデフォルトの拡大縮小率にトランスフォームマトリックスを設定させる
	 */
	protected abstract void onInit();

}
