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
 *
 * This class imported from android.inputmethodservice.KeyboardView on Android framework
 *
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.system.ContextUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class KeyboardView extends View implements View.OnClickListener {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = KeyboardView.class.getSimpleName();

	private static final int NOT_A_KEY = -1;
	private static final int[] KEY_DELETE = {Keyboard.KEYCODE_DELETE};
	private static final int[] LONG_PRESSABLE_STATE_SET = {android.R.attr.state_long_pressable};

	private static final int MSG_SHOW_PREVIEW = 1;
	private static final int MSG_REMOVE_PREVIEW = 2;
	private static final int MSG_REPEAT = 3;
	private static final int MSG_LONGPRESS = 4;

	private static final int DELAY_BEFORE_PREVIEW = 0;
	private static final int DELAY_AFTER_PREVIEW = 70;
	private static final int DEBOUNCE_TIME = 70;

	private static final int REPEAT_INTERVAL = 50; // ~20 keys per second
	private static final int REPEAT_START_DELAY = 400;
	private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

	private static final int MAX_NEARBY_KEYS = 12;

	/**
	 * Listener for virtual keyboard events.
	 */
	public interface OnKeyboardActionListener {

		/**
		 * Called when the user presses a key. This is sent before the {@link #onKey} is called.
		 * For keys that repeat, this is only called once.
		 *
		 * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
		 *                    key, the value will be zero.
		 */
		void onPress(int primaryCode);

		/**
		 * Called when the user releases a key. This is sent after the {@link #onKey} is called.
		 * For keys that repeat, this is only called once.
		 *
		 * @param primaryCode the code of the key that was released
		 */
		void onRelease(int primaryCode);

		/**
		 * Send a key press to the listener.
		 *
		 * @param primaryCode this is the key that was pressed
		 * @param keyCodes    the codes for all the possible alternative keys
		 *                    with the primary code being the first. If the primary key code is
		 *                    a single character such as an alphabet or number or symbol, the alternatives
		 *                    will include other characters that may be on the same key or adjacent keys.
		 *                    These codes are useful to correct for accidental presses of a key adjacent to
		 *                    the intended key.
		 */
		void onKey(int primaryCode, int[] keyCodes);

		/**
		 * Sends a sequence of characters to the listener.
		 *
		 * @param text the sequence of characters to be displayed.
		 */
		void onText(CharSequence text);

		/**
		 * Called when the user quickly moves the finger from right to left.
		 */
		void swipeLeft();

		/**
		 * Called when the user quickly moves the finger from left to right.
		 */
		void swipeRight();

		/**
		 * Called when the user quickly moves the finger from up to down.
		 */
		void swipeDown();

		/**
		 * Called when the user quickly moves the finger from down to up.
		 */
		void swipeUp();
	}

	private Keyboard mKeyboard;
	private int mCurrentKeyIndex = NOT_A_KEY;
	private final float mLabelTextSize;
	private final int mLabelTextColor;
	private final float mKeyTextSize;
	private final int mKeyTextColor;
	private final float mShadowRadius;
	private final int mShadowColor;
	private float mBackgroundDimAmount;

	private TextView mPreviewText;
	private final PopupWindow mPreviewPopup;
	private int mPreviewTextSizeLarge;
	private final int mPreviewOffset;
	private final int mPreviewHeight;
	// Working variable
	private final int[] mCoordinates = new int[2];

	private final PopupWindow mPopupKeyboard;
	private View mMiniKeyboardContainer;
	private KeyboardView mMiniKeyboard;
	private boolean mMiniKeyboardOnScreen;
	private View mPopupParent;
	private int mMiniKeyboardOffsetX;
	private int mMiniKeyboardOffsetY;
	private final Map<Keyboard.Key, View> mMiniKeyboardCache;
	private Keyboard.Key[] mKeys;

	/**
	 * Listener for {@link OnKeyboardActionListener}.
	 */
	private OnKeyboardActionListener mKeyboardActionListener;

	private final int mVerticalCorrection;
	private int mProximityThreshold;

	private boolean mPreviewCentered = false;
	private boolean mShowPreview = true;
	private boolean mShowTouchPoints = true;

	private int mLastX;
	private int mLastY;
	private int mStartX;
	private int mStartY;

	private boolean mProximityCorrectOn;

	@NonNull
	private final Paint mPaint;
	@NonNull
	private final Rect mPadding;

	private long mDownTime;
	private long mLastMoveTime;
	private int mLastKey;
	private int mLastCodeX;
	private int mLastCodeY;
	private int mCurrentKey = NOT_A_KEY;
	private int mDownKey = NOT_A_KEY;
	private long mLastKeyTime;
	private long mCurrentKeyTime;
	private final int[] mKeyIndices = new int[12];
	private GestureDetector mGestureDetector;
	private int mRepeatKeyIndex = NOT_A_KEY;
	private final int mPopupLayout;
	private boolean mAbortKey;
	private Keyboard.Key mInvalidatedKey;
	@NonNull
	private final Rect mClipRegion = new Rect(0, 0, 0, 0);
	private boolean mPossiblePoly;
	private final SwipeTracker mSwipeTracker = new SwipeTracker();
	private final int mSwipeThreshold;
	private final boolean mDisambiguateSwipe;

	// Variables for dealing with multiple pointers
	private int mOldPointerCount = 1;
	private float mOldPointerX;
	private float mOldPointerY;

	private final Drawable mKeyBackground;

	private final int[] mDistances = new int[MAX_NEARBY_KEYS];

	// For multi-tap
	private int mLastSentIndex;
	private int mTapCount;
	private long mLastTapTime;
	private boolean mInMultiTap;
	private static final int MULTITAP_INTERVAL = 800; // milliseconds
	@NonNull
	private final StringBuilder mPreviewLabel = new StringBuilder(1);

	/**
	 * Whether the keyboard bitmap needs to be redrawn before it's blitted.
	 **/
	private boolean mDrawPending;
	/**
	 * The dirty region in the keyboard bitmap
	 */
	private final Rect mDirtyRect = new Rect();
	/**
	 * The keyboard bitmap for faster updates
	 */
	private Bitmap mBuffer;
	/**
	 * Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.
	 */
	private boolean mKeyboardChanged;
	/**
	 * The canvas for the above mutable keyboard bitmap
	 */
	private Canvas mCanvas;
	/**
	 * The accessibility manager for accessibility support
	 */
	private final AccessibilityManager mAccessibilityManager;
//	/**
//	 * The audio manager for accessibility support
//	 */
//	private AudioManager mAudioManager;
	/**
	 * Whether the requirement of a headset to hear passwords if accessibility is enabled is announced.
	 */
	private boolean mHeadsetRequiredToHearPasswordsAnnounced;

	Handler mHandler;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public KeyboardView(@NonNull final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public KeyboardView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public KeyboardView(@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyleAttr) {

		super(context, attrs, defStyleAttr);

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.KeyboardView, defStyleAttr, 0);

		final LayoutInflater inflate = LayoutInflater.from(context);
		final Resources resources = context.getResources();

		mKeyBackground = a.getDrawable(R.styleable.KeyboardView_keyBackground);
		if (DEBUG) Log.v(TAG, "コンストラクタ:mKeyBackground=" + mKeyBackground);
		mVerticalCorrection = a.getDimensionPixelOffset( R.styleable.KeyboardView_verticalCorrection, 0);
		if (DEBUG) Log.v(TAG, "コンストラクタ:mVerticalCorrection=" + mVerticalCorrection);
		int previewLayout = a.getResourceId(R.styleable.KeyboardView_keyPreviewLayout, 0);
		if (DEBUG) Log.v(TAG, "コンストラクタ:previewLayout=" + previewLayout);
		mPreviewOffset = a.getDimensionPixelOffset(R.styleable.KeyboardView_keyPreviewOffset, 0);
		if (DEBUG) Log.v(TAG, "コンストラクタ:mPreviewOffset=" + mPreviewOffset);
		mPreviewHeight = a.getDimensionPixelSize(R.styleable.KeyboardView_keyPreviewHeight,
			resources.getDimensionPixelSize(R.dimen.keyboard_key_preview_height));
		if (DEBUG) Log.v(TAG, "コンストラクタ:mPreviewHeight=" + mPreviewHeight);
		mKeyTextSize = a.getDimension(R.styleable.KeyboardView_keyTextSize,
			resources.getDimension(R.dimen.keyboard_key_text_sz));
		if (DEBUG) Log.v(TAG, "コンストラクタ:mKeyTextSize=" + mKeyTextSize);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			mKeyTextColor = a.getColor(R.styleable.KeyboardView_keyTextColor,
				resources.getColor(R.color.keyboard_key_text_color, null));
			mLabelTextColor = a.getColor(R.styleable.KeyboardView_labelTextColor,
				resources.getColor(R.color.keyboard_key_label_color, null));
			mShadowColor = a.getColor(R.styleable.KeyboardView_shadowColor,
				resources.getColor(R.color.keyboard_key_label_color, null));
		} else {
			mKeyTextColor = a.getColor(R.styleable.KeyboardView_keyTextColor,
				resources.getColor(R.color.keyboard_key_text_color));
			mLabelTextColor = a.getColor(R.styleable.KeyboardView_labelTextColor,
				resources.getColor(R.color.keyboard_key_label_color));
			mShadowColor = a.getColor(R.styleable.KeyboardView_shadowColor,
				resources.getColor(R.color.keyboard_key_label_color));
		}
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:mKeyTextColor=%08x", mKeyTextColor));
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:mLabelTextColor=%08x", mLabelTextColor));
		if (DEBUG) Log.v(TAG, String.format("コンストラクタ:mShadowColor=%08x", mShadowColor));
		mLabelTextSize = a.getDimension(R.styleable.KeyboardView_labelTextSize,
			resources.getDimension(R.dimen.keyboard_label_sz));
		if (DEBUG) Log.v(TAG, "コンストラクタ:mLabelTextSize=" + mLabelTextSize);
		mPopupLayout = a.getResourceId(R.styleable.KeyboardView_popupLayout, 0);
		if (DEBUG) Log.v(TAG, "コンストラクタ:mPopupLayout=" + mPopupLayout);
		mShadowRadius = a.getFloat(R.styleable.KeyboardView_shadowRadius, 0f);
		if (DEBUG) Log.v(TAG, "コンストラクタ:mShadowRadius=" + mShadowRadius);
		a.recycle();

		mPreviewPopup = new PopupWindow(context);
		if (previewLayout != 0) {
			mPreviewText = (TextView) inflate.inflate(previewLayout, null);
			mPreviewTextSizeLarge = (int) mPreviewText.getTextSize();
			mPreviewPopup.setContentView(mPreviewText);
			mPreviewPopup.setBackgroundDrawable(null);
		} else {
			mShowPreview = false;
		}

		mPreviewPopup.setTouchable(false);

		mPopupKeyboard = new PopupWindow(context);
		mPopupKeyboard.setBackgroundDrawable(null);
		//mPopupKeyboard.setClippingEnabled(false);

		mPopupParent = this;
		//mPredicting = true;

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(0);
		mPaint.setTextAlign(Paint.Align.CENTER);
		mPaint.setAlpha(255);

		mPadding = new Rect(0, 0, 0, 0);
		mMiniKeyboardCache = new HashMap<Keyboard.Key, View>();
		mKeyBackground.getPadding(mPadding);

		mSwipeThreshold = (int) (500 * getResources().getDisplayMetrics().density);
		mDisambiguateSwipe = getResources().getBoolean(R.bool.config_swipeDisambiguation);

		mAccessibilityManager
			= ContextUtils.requireSystemService(context, AccessibilityManager.class);
//		mAudioManager = ContextUtils.requireSystemService(context, AudioManager.class);

		resetMultiTap();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		initGestureDetector();
		if (mHandler == null) {
			mHandler = new KeyHandler(this);
		}
	}

	private static class KeyHandler extends Handler {
		private final KeyboardView mParent;
		private KeyHandler(@NonNull final KeyboardView parent) {
			super();
			if (DEBUG) Log.v(TAG, "KeyHandler#コンストラクタ:");
			mParent = parent;
		}

		@Override
		public void handleMessage(@NonNull final Message msg) {
			switch (msg.what) {
			case MSG_SHOW_PREVIEW -> mParent.showKey(msg.arg1);
			case MSG_REMOVE_PREVIEW -> mParent.mPreviewText.setVisibility(INVISIBLE);
			case MSG_REPEAT -> {
				if (mParent.repeatKey()) {
					Message repeat = Message.obtain(this, MSG_REPEAT);
					sendMessageDelayed(repeat, REPEAT_INTERVAL);
				}
			}
			case MSG_LONGPRESS -> mParent.openPopupIfRequired((MotionEvent) msg.obj);
			}
		}
	}

	private void initGestureDetector() {
		if (DEBUG) Log.v(TAG, "initGestureDetector:");
		if (mGestureDetector == null) {
			mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onFling(
					final MotionEvent me1,
					@NonNull final MotionEvent me2,
					float velocityX, float velocityY) {

					if (mPossiblePoly) return false;
					final float absX = Math.abs(velocityX);
					final float absY = Math.abs(velocityY);
					float deltaX = me2.getX() - me1.getX();
					float deltaY = me2.getY() - me1.getY();
					int travelX = getWidth() / 2; // Half the keyboard width
					int travelY = getHeight() / 2; // Half the keyboard height
					mSwipeTracker.computeCurrentVelocity(1000);
					final float endingVelocityX = mSwipeTracker.getXVelocity();
					final float endingVelocityY = mSwipeTracker.getYVelocity();
					boolean sendDownKey = false;
					if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelX) {
						if (mDisambiguateSwipe && endingVelocityX < velocityX / 4) {
							sendDownKey = true;
						} else {
							swipeRight();
							return true;
						}
					} else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelX) {
						if (mDisambiguateSwipe && endingVelocityX > velocityX / 4) {
							sendDownKey = true;
						} else {
							swipeLeft();
							return true;
						}
					} else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelY) {
						if (mDisambiguateSwipe && endingVelocityY > velocityY / 4) {
							sendDownKey = true;
						} else {
							swipeUp();
							return true;
						}
					} else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelY) {
						if (mDisambiguateSwipe && endingVelocityY < velocityY / 4) {
							sendDownKey = true;
						} else {
							swipeDown();
							return true;
						}
					}

					if (sendDownKey) {
						detectAndSendKey(mDownKey, mStartX, mStartY, me1.getEventTime());
					}
					return false;
				}
			});

			mGestureDetector.setIsLongpressEnabled(false);
		}
	}

	public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
		mKeyboardActionListener = listener;
	}

	/**
	 * Returns the {@link OnKeyboardActionListener} object.
	 *
	 * @return the listener attached to this keyboard
	 */
	protected OnKeyboardActionListener getOnKeyboardActionListener() {
		return mKeyboardActionListener;
	}

	/**
	 * Attaches a keyboard to this view. The keyboard can be switched at any time and the
	 * view will re-layout itself to accommodate the keyboard.
	 *
	 * @param keyboard the keyboard to display in this view
	 * @see Keyboard
	 * @see #getKeyboard()
	 */
	public void setKeyboard(@NonNull final Keyboard keyboard) {
		if (DEBUG) Log.v(TAG, "setKeyboard:" + keyboard);
		if (mKeyboard != null) {
			showPreview(NOT_A_KEY);
		}
		// Remove any pending messages
		removeMessages();
		mKeyboard = keyboard;
		List<Keyboard.Key> keys = mKeyboard.getKeys();
		mKeys = keys.toArray(new Keyboard.Key[0]);
		requestLayout();
		// Hint to reallocate the buffer if the size changed
		mKeyboardChanged = true;
		invalidateAllKeys();
		computeProximityThreshold(keyboard);
		mMiniKeyboardCache.clear(); // Not really necessary to do every time, but will free up views
		// Switching to a different keyboard should abort any pending keys so that the key up
		// doesn't get delivered to the old or new keyboard
		mAbortKey = true; // Until the next ACTION_DOWN
	}

	/**
	 * Returns the current keyboard being displayed by this view.
	 *
	 * @return the currently attached keyboard
	 * @see #setKeyboard(Keyboard)
	 */
	@Nullable
	public Keyboard getKeyboard() {
		return mKeyboard;
	}

	/**
	 * Sets the state of the shift key of the keyboard, if any.
	 *
	 * @param shifted whether or not to enable the state of the shift key
	 * @return true if the shift key state changed, false if there was no change
	 * @see #isShifted()
	 */
	public boolean setShifted(boolean shifted) {
		if (mKeyboard != null) {
			if (mKeyboard.setShifted(shifted)) {
				// The whole keyboard probably needs to be redrawn
				invalidateAllKeys();
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the state of the shift key of the keyboard, if any.
	 *
	 * @return true if the shift is in a pressed state, false otherwise. If there is
	 * no shift key on the keyboard or there is no keyboard attached, it returns false.
	 * @see #setShifted(boolean)
	 */
	public boolean isShifted() {
		if (mKeyboard != null) {
			return mKeyboard.isShifted();
		}
		return false;
	}

	/**
	 * Enables or disables the key feedback popup. This is a popup that shows a magnified
	 * version of the depressed key. By default the preview is enabled.
	 *
	 * @param previewEnabled whether or not to enable the key feedback popup
	 * @see #isPreviewEnabled()
	 */
	public void setPreviewEnabled(boolean previewEnabled) {
		mShowPreview = previewEnabled;
	}

	/**
	 * Returns the enabled state of the key feedback popup.
	 *
	 * @return whether or not the key feedback popup is enabled
	 * @see #setPreviewEnabled(boolean)
	 */
	public boolean isPreviewEnabled() {
		return mShowPreview;
	}

	public void setVerticalCorrection(int verticalOffset) {

	}

	public void setPopupParent(View v) {
		mPopupParent = v;
	}

	public void setPopupOffset(int x, int y) {
		mMiniKeyboardOffsetX = x;
		mMiniKeyboardOffsetY = y;
		if (mPreviewPopup.isShowing()) {
			mPreviewPopup.dismiss();
		}
	}

	/**
	 * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include key
	 * codes for adjacent keys.  When disabled, only the primary key code will be
	 * reported.
	 *
	 * @param enabled whether or not the proximity correction is enabled
	 */
	public void setProximityCorrectionEnabled(boolean enabled) {
		mProximityCorrectOn = enabled;
	}

	/**
	 * Returns true if proximity correction is enabled.
	 */
	public boolean isProximityCorrectionEnabled() {
		return mProximityCorrectOn;
	}

	/**
	 * Popup keyboard close button clicked.
	 *
	 * @hide
	 */
	public void onClick(View v) {
		dismissPopupKeyboard();
	}

	private CharSequence adjustCase(CharSequence label) {
		if (mKeyboard.isShifted() && label != null && label.length() < 3
			&& Character.isLowerCase(label.charAt(0))) {
			label = label.toString().toUpperCase();
		}
		return label;
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Round up a little
		if (mKeyboard == null) {
			setMeasuredDimension(
				getPaddingLeft() + getPaddingRight(),
				getPaddingTop() + getPaddingBottom());
		} else {
			int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
			if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
				width = MeasureSpec.getSize(widthMeasureSpec);
			}
			setMeasuredDimension(
				width,
				mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
		}
	}

	/**
	 * Compute the average distance between adjacent keys (horizontally and vertically)
	 * and square it to get the proximity threshold. We use a square here and in computing
	 * the touch distance from a key's center to avoid taking a square root.
	 *
	 * @param keyboard
	 */
	private void computeProximityThreshold(@Nullable final Keyboard keyboard) {
		if (keyboard == null) return;
		final Keyboard.Key[] keys = mKeys;
		if (keys == null) return;
		int length = keys.length;
		int dimensionSum = 0;
		for (int i = 0; i < length; i++) {
			Keyboard.Key key = keys[i];
			dimensionSum += Math.min(key.width, key.height) + key.gap;
		}
		if (dimensionSum < 0 || length == 0) return;
		mProximityThreshold = (int) (dimensionSum * 1.4f / length);
		mProximityThreshold *= mProximityThreshold; // Square it
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (DEBUG) Log.v(TAG, String.format("onSizeChanged:(%dx%d)→(%dx%d)",
			oldw, oldh, w, h));
		if (mKeyboard != null) {
			mKeyboard.resize(w, h);
		}
		// Release the buffer, if any and it will be reallocated on the next draw
		mBuffer = null;
	}

	@Override
	public void onDraw(@NonNull final Canvas canvas) {
		super.onDraw(canvas);
		if (mDrawPending || mBuffer == null || mKeyboardChanged) {
			onBufferDraw();
		}
		canvas.drawBitmap(mBuffer, 0, 0, null);
	}

	private void onBufferDraw() {
//		if (DEBUG) Log.v(TAG, "onBufferDraw:");
		if (mBuffer == null || mKeyboardChanged) {
			if (mBuffer == null || mKeyboardChanged &&
				(mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
				// Make sure our bitmap is at least 1x1
				final int width = Math.max(1, getWidth());
				final int height = Math.max(1, getHeight());
				mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				mCanvas = new Canvas(mBuffer);
			}
			invalidateAllKeys();
			mKeyboardChanged = false;
		}

		if (mKeyboard == null) return;

		mCanvas.save();
		final Canvas canvas = mCanvas;
		canvas.clipRect(mDirtyRect);

		final Paint paint = mPaint;
		final Drawable keyBackground = mKeyBackground;
		final Rect clipRegion = mClipRegion;
		final Rect padding = mPadding;
		final int kbdPaddingLeft = getPaddingLeft();
		final int kbdPaddingTop = getPaddingTop();
		final Keyboard.Key[] keys = mKeys;
		final Keyboard.Key invalidKey = mInvalidatedKey;

		paint.setColor(mKeyTextColor);
		boolean drawSingleKey = false;
		if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
			// Is clipRegion completely contained within the invalidated key?
			if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
				invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
				invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
				invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
				drawSingleKey = true;
			}
		}
		canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
		final int keyCount = keys.length;
		for (int i = 0; i < keyCount; i++) {
			final Keyboard.Key key = keys[i];
			if (drawSingleKey && invalidKey != key) {
				continue;
			}
			int[] drawableState = key.getCurrentDrawableState();
			keyBackground.setState(drawableState);

			// Switch the character to uppercase if shift is pressed
			String label = key.label == null ? null : adjustCase(key.label).toString();

			final Rect bounds = keyBackground.getBounds();
			if (key.width != bounds.right ||
				key.height != bounds.bottom) {
				keyBackground.setBounds(0, 0, key.width, key.height);
			}
			canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
			keyBackground.draw(canvas);

			if (DEBUG) Log.d(TAG, "onBufferDraw:label=" + label + ",icon=" + key.icon);
			if (label != null) {
				// For characters, use large font. For labels like "Done", use small font.
				if (label.length() > 1 && key.codes.length < 2) {
					if (DEBUG) Log.v(TAG, "onBufferDraw:textSz=" + mLabelTextSize);
					paint.setColor(mLabelTextColor);
					paint.setTextSize(mLabelTextSize);
					paint.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					if (DEBUG) Log.v(TAG, "onBufferDraw:textSz=" + mKeyTextSize);
					paint.setColor(mKeyTextColor);
					paint.setTextSize(mKeyTextSize);
					paint.setTypeface(Typeface.DEFAULT);
				}
				// Draw a drop shadow for the text
				paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
				// Draw the text
				canvas.drawText(label,
					(key.width - padding.left - padding.right) / 2.0f
						+ padding.left,
					(key.height - padding.top - padding.bottom) / 2.0f
						+ (paint.getTextSize() - paint.descent()) / 2.0f + padding.top,
					paint);
				// Turn off drop shadow
				paint.setShadowLayer(0, 0, 0, 0);
			} else if (key.icon != null) {
				final int drawableX = (key.width - padding.left - padding.right
					- key.icon.getIntrinsicWidth()) / 2 + padding.left;
				final int drawableY = (key.height - padding.top - padding.bottom
					- key.icon.getIntrinsicHeight()) / 2 + padding.top;
				canvas.translate(drawableX, drawableY);
				key.icon.setBounds(0, 0,
					key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
				key.icon.draw(canvas);
				canvas.translate(-drawableX, -drawableY);
			}
			canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
		}
		mInvalidatedKey = null;
		// Overlay a dark rectangle to dim the keyboard
		if (mMiniKeyboardOnScreen) {
			paint.setColor((int) (mBackgroundDimAmount * 0xFF) << 24);
			canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
		}

		if (DEBUG && mShowTouchPoints) {
			paint.setAlpha(128);
			paint.setColor(0xFFFF0000);
			canvas.drawCircle(mStartX, mStartY, 3, paint);
			canvas.drawLine(mStartX, mStartY, mLastX, mLastY, paint);
			paint.setColor(0xFF0000FF);
			canvas.drawCircle(mLastX, mLastY, 3, paint);
			paint.setColor(0xFF00FF00);
			canvas.drawCircle((mStartX + mLastX) / 2.0f, (mStartY + mLastY) / 2.0f, 2, paint);
		}
		mCanvas.restore();
		mDrawPending = false;
		mDirtyRect.setEmpty();
	}

	private int getKeyIndices(int x, int y, int[] allKeys) {
		final Keyboard.Key[] keys = mKeys;
		int primaryIndex = NOT_A_KEY;
		int closestKey = NOT_A_KEY;
		int closestKeyDist = mProximityThreshold + 1;
		java.util.Arrays.fill(mDistances, Integer.MAX_VALUE);
		int[] nearestKeyIndices = mKeyboard.getNearestKeys(x, y);
		final int keyCount = nearestKeyIndices.length;
		for (int i = 0; i < keyCount; i++) {
			final Keyboard.Key key = keys[nearestKeyIndices[i]];
			int dist = 0;
			boolean isInside = key.isInside(x, y);
			if (isInside) {
				primaryIndex = nearestKeyIndices[i];
			}

			if (((mProximityCorrectOn
				&& (dist = key.squaredDistanceFrom(x, y)) < mProximityThreshold)
				|| isInside)
				&& key.codes[0] > 32) {
				// Find insertion point
				final int nCodes = key.codes.length;
				if (dist < closestKeyDist) {
					closestKeyDist = dist;
					closestKey = nearestKeyIndices[i];
				}

				if (allKeys == null) continue;

				for (int j = 0; j < mDistances.length; j++) {
					if (mDistances[j] > dist) {
						// Make space for nCodes codes
						System.arraycopy(mDistances, j, mDistances, j + nCodes,
							mDistances.length - j - nCodes);
						System.arraycopy(allKeys, j, allKeys, j + nCodes,
							allKeys.length - j - nCodes);
						for (int c = 0; c < nCodes; c++) {
							allKeys[j + c] = key.codes[c];
							mDistances[j + c] = dist;
						}
						break;
					}
				}
			}
		}
		if (primaryIndex == NOT_A_KEY) {
			primaryIndex = closestKey;
		}
		return primaryIndex;
	}

	private void detectAndSendKey(int index, int x, int y, long eventTime) {
		if (index != NOT_A_KEY && index < mKeys.length) {
			final Keyboard.Key key = mKeys[index];
			if (key.text != null) {
				mKeyboardActionListener.onText(key.text);
				mKeyboardActionListener.onRelease(NOT_A_KEY);
			} else {
				int code = key.codes[0];
				//TextEntryState.keyPressedAt(key, x, y);
				int[] codes = new int[MAX_NEARBY_KEYS];
				Arrays.fill(codes, NOT_A_KEY);
				getKeyIndices(x, y, codes);
				// Multi-tap
				if (mInMultiTap) {
					if (mTapCount != -1) {
						mKeyboardActionListener.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE);
					} else {
						mTapCount = 0;
					}
					code = key.codes[mTapCount];
				}
				mKeyboardActionListener.onKey(code, codes);
				mKeyboardActionListener.onRelease(code);
			}
			mLastSentIndex = index;
			mLastTapTime = eventTime;
		}
	}

	/**
	 * Handle multi-tap keys by producing the key label for the current multi-tap state.
	 */
	private CharSequence getPreviewText(Keyboard.Key key) {
		if (mInMultiTap) {
			// Multi-tap
			mPreviewLabel.setLength(0);
			mPreviewLabel.append((char) key.codes[Math.max(mTapCount, 0)]);
			return adjustCase(mPreviewLabel);
		} else {
			return adjustCase(key.label);
		}
	}

	private void showPreview(int keyIndex) {
		int oldKeyIndex = mCurrentKeyIndex;
		final PopupWindow previewPopup = mPreviewPopup;

		mCurrentKeyIndex = keyIndex;
		// Release the old key and press the new key
		final Keyboard.Key[] keys = mKeys;
		if (oldKeyIndex != mCurrentKeyIndex) {
			if (oldKeyIndex != NOT_A_KEY && keys.length > oldKeyIndex) {
				Keyboard.Key oldKey = keys[oldKeyIndex];
				oldKey.onReleased(mCurrentKeyIndex == NOT_A_KEY);
				invalidateKey(oldKeyIndex);
				final int keyCode = oldKey.codes[0];
				sendAccessibilityEventForUnicodeCharacter(AccessibilityEvent.TYPE_VIEW_HOVER_EXIT,
					keyCode);
				// TODO: We need to implement AccessibilityNodeProvider for this view.
				sendAccessibilityEventForUnicodeCharacter(
					AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, keyCode);
			}
			if (mCurrentKeyIndex != NOT_A_KEY && keys.length > mCurrentKeyIndex) {
				Keyboard.Key newKey = keys[mCurrentKeyIndex];
				newKey.onPressed();
				invalidateKey(mCurrentKeyIndex);
				final int keyCode = newKey.codes[0];
				sendAccessibilityEventForUnicodeCharacter(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
					keyCode);
				// TODO: We need to implement AccessibilityNodeProvider for this view.
				sendAccessibilityEventForUnicodeCharacter(
					AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, keyCode);
			}
		}
		// If key changed and preview is on ...
		if (oldKeyIndex != mCurrentKeyIndex && mShowPreview) {
			mHandler.removeMessages(MSG_SHOW_PREVIEW);
			if (previewPopup.isShowing()) {
				if (keyIndex == NOT_A_KEY) {
					mHandler.sendMessageDelayed(mHandler
							.obtainMessage(MSG_REMOVE_PREVIEW),
						DELAY_AFTER_PREVIEW);
				}
			}
			if (keyIndex != NOT_A_KEY) {
				if (previewPopup.isShowing() && mPreviewText.getVisibility() == VISIBLE) {
					// Show right away, if it's already visible and finger is moving around
					showKey(keyIndex);
				} else {
					mHandler.sendMessageDelayed(
						mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0),
						DELAY_BEFORE_PREVIEW);
				}
			}
		}
	}

	private void showKey(final int keyIndex) {
		final PopupWindow previewPopup = mPreviewPopup;
		final Keyboard.Key[] keys = mKeys;
		if (keyIndex < 0 || keyIndex >= mKeys.length) return;
		Keyboard.Key key = keys[keyIndex];
		if (key.icon != null) {
			mPreviewText.setCompoundDrawables(null, null, null,
				key.iconPreview != null ? key.iconPreview : key.icon);
			mPreviewText.setText(null);
		} else {
			mPreviewText.setCompoundDrawables(null, null, null, null);
			mPreviewText.setText(getPreviewText(key));
			if (key.label.length() > 1 && key.codes.length < 2) {
				mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize);
				mPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge);
				mPreviewText.setTypeface(Typeface.DEFAULT);
			}
		}
		mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
			MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		int popupWidth = Math.max(mPreviewText.getMeasuredWidth(), key.width
			+ mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight());
		final int popupHeight = mPreviewHeight;
		ViewGroup.LayoutParams lp = mPreviewText.getLayoutParams();
		if (lp != null) {
			lp.width = popupWidth;
			lp.height = popupHeight;
		}
		int popupPreviewX;
		int popupPreviewY;
		if (!mPreviewCentered) {
			popupPreviewX = key.x - mPreviewText.getPaddingLeft() + getPaddingLeft();
			popupPreviewY = key.y - popupHeight + mPreviewOffset;
		} else {
			// TODO: Fix this if centering is brought back
			popupPreviewX = 160 - mPreviewText.getMeasuredWidth() / 2;
			popupPreviewY = -mPreviewText.getMeasuredHeight();
		}
		mHandler.removeMessages(MSG_REMOVE_PREVIEW);
		getLocationInWindow(mCoordinates);
		mCoordinates[0] += mMiniKeyboardOffsetX; // Offset may be zero
		mCoordinates[1] += mMiniKeyboardOffsetY; // Offset may be zero

		// Set the preview background state
		mPreviewText.getBackground().setState(
			key.popupResId != 0 ? LONG_PRESSABLE_STATE_SET : EMPTY_STATE_SET);
		popupPreviewX += mCoordinates[0];
		popupPreviewY += mCoordinates[1];

		// If the popup cannot be shown above the key, put it on the side
		getLocationOnScreen(mCoordinates);
		if (popupPreviewY + mCoordinates[1] < 0) {
			// If the key you're pressing is on the left side of the keyboard, show the popup on
			// the right, offset by enough to see at least one key to the left/right.
			if (key.x + key.width <= getWidth() / 2) {
				popupPreviewX += (int) (key.width * 2.5);
			} else {
				popupPreviewX -= (int) (key.width * 2.5);
			}
			popupPreviewY += popupHeight;
		}

		if (previewPopup.isShowing()) {
			previewPopup.update(popupPreviewX, popupPreviewY,
				popupWidth, popupHeight);
		} else {
			previewPopup.setWidth(popupWidth);
			previewPopup.setHeight(popupHeight);
			previewPopup.showAtLocation(mPopupParent, Gravity.NO_GRAVITY,
				popupPreviewX, popupPreviewY);
		}
		mPreviewText.setVisibility(VISIBLE);
	}

	private void sendAccessibilityEventForUnicodeCharacter(int eventType, int code) {
		if (mAccessibilityManager.isEnabled()) {
			AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
			onInitializeAccessibilityEvent(event);
			final String text = switch (code) {
				case Keyboard.KEYCODE_ALT ->
					getContext().getString(R.string.keyboardview_keycode_alt);
				case Keyboard.KEYCODE_CANCEL ->
					getContext().getString(R.string.keyboardview_keycode_cancel);
				case Keyboard.KEYCODE_DELETE ->
					getContext().getString(R.string.keyboardview_keycode_delete);
				case Keyboard.KEYCODE_DONE ->
					getContext().getString(R.string.keyboardview_keycode_done);
				case Keyboard.KEYCODE_MODE_CHANGE ->
					getContext().getString(R.string.keyboardview_keycode_mode_change);
				case Keyboard.KEYCODE_SHIFT ->
					getContext().getString(R.string.keyboardview_keycode_shift);
				case '\n' -> getContext().getString(R.string.keyboardview_keycode_enter);
				default -> String.valueOf((char) code);
			};
			event.getText().add(text);
			mAccessibilityManager.sendAccessibilityEvent(event);
		}
	}

	/**
	 * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
	 * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
	 * draws the cached buffer.
	 *
	 * @see #invalidateKey(int)
	 */
	public void invalidateAllKeys() {
		mDirtyRect.union(0, 0, getWidth(), getHeight());
		mDrawPending = true;
		invalidate();
	}

	/**
	 * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
	 * one key is changing it's content. Any changes that affect the position or size of the key
	 * may not be honored.
	 *
	 * @param keyIndex the index of the key in the attached {@link Keyboard}.
	 * @see #invalidateAllKeys
	 */
	public void invalidateKey(int keyIndex) {
		if (mKeys == null) return;
		if (keyIndex < 0 || keyIndex >= mKeys.length) {
			return;
		}
		final Keyboard.Key key = mKeys[keyIndex];
		mInvalidatedKey = key;
		mDirtyRect.union(key.x + getPaddingLeft(), key.y + getPaddingTop(),
			key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
		onBufferDraw();
		invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
			key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
	}

	private boolean openPopupIfRequired(MotionEvent me) {
		// Check if we have a popup layout specified first.
		if (mPopupLayout == 0) {
			return false;
		}
		if (mCurrentKey < 0 || mCurrentKey >= mKeys.length) {
			return false;
		}

		Keyboard.Key popupKey = mKeys[mCurrentKey];
		boolean result = onLongPress(popupKey);
		if (result) {
			mAbortKey = true;
			showPreview(NOT_A_KEY);
		}
		return result;
	}

	/**
	 * Called when a key is long pressed. By default this will open any popup keyboard associated
	 * with this key through the attributes popupLayout and popupCharacters.
	 *
	 * @param popupKey the key that was long pressed
	 * @return true if the long press is handled, false otherwise. Subclasses should call the
	 * method on the base class if the subclass doesn't wish to handle the call.
	 */
	protected boolean onLongPress(final Keyboard.Key popupKey) {
		int popupKeyboardId = popupKey.popupResId;

		if (popupKeyboardId != 0) {
			mMiniKeyboardContainer = mMiniKeyboardCache.get(popupKey);
			if (mMiniKeyboardContainer == null) {
				final LayoutInflater inflater = LayoutInflater.from(getContext());
				mMiniKeyboardContainer = inflater.inflate(mPopupLayout, null);
				mMiniKeyboard = mMiniKeyboardContainer.findViewById(
					R.id.keyboardView);
				View closeButton = mMiniKeyboardContainer.findViewById(
					R.id.closeButton);
				if (closeButton != null) closeButton.setOnClickListener(this);
				mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
					public void onKey(int primaryCode, int[] keyCodes) {
						mKeyboardActionListener.onKey(primaryCode, keyCodes);
						dismissPopupKeyboard();
					}

					public void onText(CharSequence text) {
						mKeyboardActionListener.onText(text);
						dismissPopupKeyboard();
					}

					public void swipeLeft() {
					}

					public void swipeRight() {
					}

					public void swipeUp() {
					}

					public void swipeDown() {
					}

					public void onPress(int primaryCode) {
						mKeyboardActionListener.onPress(primaryCode);
					}

					public void onRelease(int primaryCode) {
						mKeyboardActionListener.onRelease(primaryCode);
					}
				});
				//mInputView.setSuggest(mSuggest);
				Keyboard keyboard;
				if (popupKey.popupCharacters != null) {
					keyboard = new Keyboard(getContext(), popupKeyboardId,
						popupKey.popupCharacters, -1, getPaddingLeft() + getPaddingRight());
				} else {
					keyboard = new Keyboard(getContext(), popupKeyboardId);
				}
				mMiniKeyboard.setKeyboard(keyboard);
				mMiniKeyboard.setPopupParent(this);
				mMiniKeyboardContainer.measure(
					MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));

				mMiniKeyboardCache.put(popupKey, mMiniKeyboardContainer);
			} else {
				mMiniKeyboard = mMiniKeyboardContainer.findViewById(
					R.id.keyboardView);
			}
			getLocationInWindow(mCoordinates);
			int popupX = popupKey.x + getPaddingLeft();
			int popupY = popupKey.y + getPaddingTop();
			popupX = popupX + popupKey.width - mMiniKeyboardContainer.getMeasuredWidth();
			popupY = popupY - mMiniKeyboardContainer.getMeasuredHeight();
			final int x = popupX + mMiniKeyboardContainer.getPaddingRight() + mCoordinates[0];
			final int y = popupY + mMiniKeyboardContainer.getPaddingBottom() + mCoordinates[1];
			mMiniKeyboard.setPopupOffset(Math.max(x, 0), y);
			mMiniKeyboard.setShifted(isShifted());
			mPopupKeyboard.setContentView(mMiniKeyboardContainer);
			mPopupKeyboard.setWidth(mMiniKeyboardContainer.getMeasuredWidth());
			mPopupKeyboard.setHeight(mMiniKeyboardContainer.getMeasuredHeight());
			mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
			mMiniKeyboardOnScreen = true;
			//mMiniKeyboard.onTouchEvent(getTranslatedEvent(me));
			invalidateAllKeys();
			return true;
		}
		return false;
	}

	@Override
	public boolean onHoverEvent(MotionEvent event) {
		if (mAccessibilityManager.isTouchExplorationEnabled() && event.getPointerCount() == 1) {
			final int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_HOVER_ENTER ->
				event.setAction(MotionEvent.ACTION_DOWN);
			case MotionEvent.ACTION_HOVER_MOVE ->
				event.setAction(MotionEvent.ACTION_MOVE);
			case MotionEvent.ACTION_HOVER_EXIT ->
				event.setAction(MotionEvent.ACTION_UP);
			}
			return onTouchEvent(event);
		}
		return true;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		// Convert multi-pointer up/down events to single up/down events to
		// deal with the typical multi-pointer behavior of two-thumb typing
		final int pointerCount = me.getPointerCount();
		final int action = me.getAction();
		boolean result;
		final long now = me.getEventTime();

		if (pointerCount != mOldPointerCount) {
			if (pointerCount == 1) {
				// Send a down event for the latest pointer
				MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
					me.getX(), me.getY(), me.getMetaState());
				result = onModifiedTouchEvent(down, false);
				down.recycle();
				// If it's an up action, then deliver the up as well.
				if (action == MotionEvent.ACTION_UP) {
					result = onModifiedTouchEvent(me, true);
				}
			} else {
				// Send an up event for the last pointer
				MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP,
					mOldPointerX, mOldPointerY, me.getMetaState());
				result = onModifiedTouchEvent(up, true);
				up.recycle();
			}
		} else {
			if (pointerCount == 1) {
				result = onModifiedTouchEvent(me, false);
				mOldPointerX = me.getX();
				mOldPointerY = me.getY();
			} else {
				// Don't do anything when 2 pointers are down and moving.
				result = true;
			}
		}
		mOldPointerCount = pointerCount;

		return result;
	}

	private boolean onModifiedTouchEvent(final MotionEvent me, final boolean possiblePoly) {
		int touchX = (int) me.getX() - getPaddingLeft();
		int touchY = (int) me.getY() - getPaddingTop();
		if (touchY >= -mVerticalCorrection)
			touchY += mVerticalCorrection;
		final int action = me.getAction();
		final long eventTime = me.getEventTime();
		int keyIndex = getKeyIndices(touchX, touchY, null);
		mPossiblePoly = possiblePoly;

		// Track the last few movements to look for spurious swipes.
		if (action == MotionEvent.ACTION_DOWN) mSwipeTracker.clear();
		mSwipeTracker.addMovement(me);

		// Ignore all motion events until a DOWN.
		if (mAbortKey
			&& action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
			return true;
		}

		if (mGestureDetector.onTouchEvent(me)) {
			showPreview(NOT_A_KEY);
			mHandler.removeMessages(MSG_REPEAT);
			mHandler.removeMessages(MSG_LONGPRESS);
			return true;
		}

		// Needs to be called after the gesture detector gets a turn, as it may have
		// displayed the mini keyboard
		if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
			return true;
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN -> {
			mAbortKey = false;
			mStartX = touchX;
			mStartY = touchY;
			mLastCodeX = touchX;
			mLastCodeY = touchY;
			mLastKeyTime = 0;
			mCurrentKeyTime = 0;
			mLastKey = NOT_A_KEY;
			mCurrentKey = keyIndex;
			mDownKey = keyIndex;
			mDownTime = me.getEventTime();
			mLastMoveTime = mDownTime;
			checkMultiTap(eventTime, keyIndex);
			mKeyboardActionListener.onPress(keyIndex != NOT_A_KEY ?
				mKeys[keyIndex].codes[0] : 0);
			if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
				mRepeatKeyIndex = mCurrentKey;
				Message msg = mHandler.obtainMessage(MSG_REPEAT);
				mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY);
				repeatKey();
				// Delivering the key could have caused an abort
				if (mAbortKey) {
					mRepeatKeyIndex = NOT_A_KEY;
					break;
				}
			}
			if (mCurrentKey != NOT_A_KEY) {
				Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
				mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT);
			}
			showPreview(keyIndex);
		}
		case MotionEvent.ACTION_MOVE -> {
			boolean continueLongPress = false;
			if (keyIndex != NOT_A_KEY) {
				if (mCurrentKey == NOT_A_KEY) {
					mCurrentKey = keyIndex;
					mCurrentKeyTime = eventTime - mDownTime;
				} else {
					if (keyIndex == mCurrentKey) {
						mCurrentKeyTime += eventTime - mLastMoveTime;
						continueLongPress = true;
					} else if (mRepeatKeyIndex == NOT_A_KEY) {
						resetMultiTap();
						mLastKey = mCurrentKey;
						mLastCodeX = mLastX;
						mLastCodeY = mLastY;
						mLastKeyTime =
							mCurrentKeyTime + eventTime - mLastMoveTime;
						mCurrentKey = keyIndex;
						mCurrentKeyTime = 0;
					}
				}
			}
			if (!continueLongPress) {
				// Cancel old longpress
				mHandler.removeMessages(MSG_LONGPRESS);
				// Start new longpress if key has changed
				if (keyIndex != NOT_A_KEY) {
					Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
					mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT);
				}
			}
			showPreview(mCurrentKey);
			mLastMoveTime = eventTime;
		}
		case MotionEvent.ACTION_UP -> {
			removeMessages();
			if (keyIndex == mCurrentKey) {
				mCurrentKeyTime += eventTime - mLastMoveTime;
			} else {
				resetMultiTap();
				mLastKey = mCurrentKey;
				mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime;
				mCurrentKey = keyIndex;
				mCurrentKeyTime = 0;
			}
			if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME
				&& mLastKey != NOT_A_KEY) {
				mCurrentKey = mLastKey;
				touchX = mLastCodeX;
				touchY = mLastCodeY;
			}
			showPreview(NOT_A_KEY);
			Arrays.fill(mKeyIndices, NOT_A_KEY);
			// If we're not on a repeating key (which sends on a DOWN event)
			if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
				detectAndSendKey(mCurrentKey, touchX, touchY, eventTime);
			}
			invalidateKey(keyIndex);
			mRepeatKeyIndex = NOT_A_KEY;
		}
		case MotionEvent.ACTION_CANCEL -> {
			removeMessages();
			dismissPopupKeyboard();
			mAbortKey = true;
			showPreview(NOT_A_KEY);
			invalidateKey(mCurrentKey);
		}
		}
		mLastX = touchX;
		mLastY = touchY;
		return true;
	}

	private boolean repeatKey() {
		Keyboard.Key key = mKeys[mRepeatKeyIndex];
		detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime);
		return true;
	}

	protected void swipeRight() {
		mKeyboardActionListener.swipeRight();
	}

	protected void swipeLeft() {
		mKeyboardActionListener.swipeLeft();
	}

	protected void swipeUp() {
		mKeyboardActionListener.swipeUp();
	}

	protected void swipeDown() {
		mKeyboardActionListener.swipeDown();
	}

	public void closing() {
		if (mPreviewPopup.isShowing()) {
			mPreviewPopup.dismiss();
		}
		removeMessages();

		dismissPopupKeyboard();
		mBuffer = null;
		mCanvas = null;
		mMiniKeyboardCache.clear();
	}

	private void removeMessages() {
		if (mHandler != null) {
			mHandler.removeMessages(MSG_REPEAT);
			mHandler.removeMessages(MSG_LONGPRESS);
			mHandler.removeMessages(MSG_SHOW_PREVIEW);
		}
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		closing();
	}

	private void dismissPopupKeyboard() {
		if (mPopupKeyboard.isShowing()) {
			mPopupKeyboard.dismiss();
			mMiniKeyboardOnScreen = false;
			invalidateAllKeys();
		}
	}

	public boolean handleBack() {
		if (mPopupKeyboard.isShowing()) {
			dismissPopupKeyboard();
			return true;
		}
		return false;
	}

	private void resetMultiTap() {
		mLastSentIndex = NOT_A_KEY;
		mTapCount = 0;
		mLastTapTime = -1;
		mInMultiTap = false;
	}

	private void checkMultiTap(long eventTime, int keyIndex) {
		if (keyIndex == NOT_A_KEY) return;
		Keyboard.Key key = mKeys[keyIndex];
		if (key.codes.length > 1) {
			mInMultiTap = true;
			if (eventTime < mLastTapTime + MULTITAP_INTERVAL
				&& keyIndex == mLastSentIndex) {
				mTapCount = (mTapCount + 1) % key.codes.length;
				return;
			} else {
				mTapCount = -1;
				return;
			}
		}
		if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
			resetMultiTap();
		}
	}

	private static class SwipeTracker {

		static final int NUM_PAST = 4;
		static final int LONGEST_PAST_TIME = 200;

		final float[] mPastX = new float[NUM_PAST];
		final float[] mPastY = new float[NUM_PAST];
		final long[] mPastTime = new long[NUM_PAST];

		float mYVelocity;
		float mXVelocity;

		public void clear() {
			mPastTime[0] = 0;
		}

		public void addMovement(MotionEvent ev) {
			long time = ev.getEventTime();
			final int N = ev.getHistorySize();
			for (int i = 0; i < N; i++) {
				addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i),
					ev.getHistoricalEventTime(i));
			}
			addPoint(ev.getX(), ev.getY(), time);
		}

		private void addPoint(float x, float y, long time) {
			int drop = -1;
			int i;
			final long[] pastTime = mPastTime;
			for (i = 0; i < NUM_PAST; i++) {
				if (pastTime[i] == 0) {
					break;
				} else if (pastTime[i] < time - LONGEST_PAST_TIME) {
					drop = i;
				}
			}
			if (i == NUM_PAST && drop < 0) {
				drop = 0;
			}
			if (drop == i) drop--;
			final float[] pastX = mPastX;
			final float[] pastY = mPastY;
			if (drop >= 0) {
				final int start = drop + 1;
				final int count = NUM_PAST - drop - 1;
				System.arraycopy(pastX, start, pastX, 0, count);
				System.arraycopy(pastY, start, pastY, 0, count);
				System.arraycopy(pastTime, start, pastTime, 0, count);
				i -= (drop + 1);
			}
			pastX[i] = x;
			pastY[i] = y;
			pastTime[i] = time;
			i++;
			if (i < NUM_PAST) {
				pastTime[i] = 0;
			}
		}

		public void computeCurrentVelocity(int units) {
			computeCurrentVelocity(units, Float.MAX_VALUE);
		}

		public void computeCurrentVelocity(int units, float maxVelocity) {
			final float[] pastX = mPastX;
			final float[] pastY = mPastY;
			final long[] pastTime = mPastTime;

			final float oldestX = pastX[0];
			final float oldestY = pastY[0];
			final long oldestTime = pastTime[0];
			float accumX = 0;
			float accumY = 0;
			int N = 0;
			while (N < NUM_PAST) {
				if (pastTime[N] == 0) {
					break;
				}
				N++;
			}

			for (int i = 1; i < N; i++) {
				final int dur = (int) (pastTime[i] - oldestTime);
				if (dur == 0) continue;
				float dist = pastX[i] - oldestX;
				float vel = (dist / dur) * units;   // pixels/frame.
				if (accumX == 0) accumX = vel;
				else accumX = (accumX + vel) * .5f;

				dist = pastY[i] - oldestY;
				vel = (dist / dur) * units;   // pixels/frame.
				if (accumY == 0) accumY = vel;
				else accumY = (accumY + vel) * .5f;
			}
			mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity)
				: Math.min(accumX, maxVelocity);
			mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity)
				: Math.min(accumY, maxVelocity);
		}

		public float getXVelocity() {
			return mXVelocity;
		}

		public float getYVelocity() {
			return mYVelocity;
		}
	}
}
