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
 * This class imported from android.inputmethodservice.Keyboard on Android framework
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.serenegiant.common.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

public class Keyboard {
	private static final boolean DEBUG = false;    // set false on production
	private static final String TAG = Keyboard.class.getSimpleName();

	// Keyboard XML Tags
	private static final String TAG_KEYBOARD = "Keyboard";
	private static final String TAG_ROW = "Row";
	private static final String TAG_KEY = "Key";

	public static final int EDGE_LEFT = 0x01;
	public static final int EDGE_RIGHT = 0x02;
	public static final int EDGE_TOP = 0x04;
	public static final int EDGE_BOTTOM = 0x08;

	public static final int KEYCODE_SHIFT = -1;
	public static final int KEYCODE_MODE_CHANGE = -2;
	public static final int KEYCODE_CANCEL = -3;
	public static final int KEYCODE_DONE = -4;
	public static final int KEYCODE_DELETE = -5;
	public static final int KEYCODE_ALT = -6;

	/**
	 * Keyboard label
	 **/
	private CharSequence mLabel;

	/**
	 * Horizontal gap default for all rows
	 */
	private int mDefaultHorizontalGap;

	/**
	 * Default key width
	 */
	private int mDefaultWidth;

	/**
	 * Default key height
	 */
	private int mDefaultHeight;

	/**
	 * Default gap between rows
	 */
	private int mDefaultVerticalGap;

	/**
	 * Is the keyboard in the shifted state
	 */
	private boolean mShifted;

	/**
	 * Key instance for the shift key, if present
	 */
	private final Keyboard.Key[] mShiftKeys = {null, null};

	/**
	 * Key index for the shift key, if present
	 */
	private final int[] mShiftKeyIndices = {-1, -1};

	/**
	 * Current key width, while loading the keyboard
	 */
	private int mKeyWidth;

	/**
	 * Current key height, while loading the keyboard
	 */
	private int mKeyHeight;

	/**
	 * Total height of the keyboard, including the padding and keys
	 */
	private int mTotalHeight;

	/**
	 * Total width of the keyboard, including left side gaps and keys, but not any gaps on the
	 * right side.
	 */
	private int mTotalWidth;

	/**
	 * List of keys in this keyboard
	 */
	private final List<Key> mKeys;

	/**
	 * List of modifier keys such as Shift & Alt, if any
	 */
	private final List<Key> mModifierKeys;

	/**
	 * Width of the screen available to fit the keyboard
	 */
	private int mDisplayWidth;

	/**
	 * Height of the screen
	 */
	private int mDisplayHeight;

	/**
	 * Keyboard mode, or zero, if none.
	 */
	private int mKeyboardMode;

	// Variables for pre-computing nearest keys.

	private static final int GRID_WIDTH = 10;
	private static final int GRID_HEIGHT = 5;
	private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
	private int mCellWidth;
	private int mCellHeight;
	private int[][] mGridNeighbors;
	private int mProximityThreshold;
	/**
	 * Number of key widths from current touch point to search for nearest keys.
	 */
	private static float SEARCH_DISTANCE = 1.8f;

	private final List<Row> rows = new ArrayList<>();

	/**
	 * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
	 * Some of the key size defaults can be overridden per row from what the {@link Keyboard}
	 * defines.
	 *
	 * @attr ref styleable#Keyboard_keyWidth
	 * @attr ref styleable#Keyboard_keyHeight
	 * @attr ref styleable#Keyboard_horizontalGap
	 * @attr ref styleable#Keyboard_verticalGap
	 * @attr ref styleable#Keyboard_Row_rowEdgeFlags
	 * @attr ref styleable#Keyboard_Row_keyboardMode
	 */
	public static class Row {
		private static final String TAG = Row.class.getSimpleName();

		/**
		 * Default width of a key in this row.
		 */
		public int defaultWidth;
		/**
		 * Default height of a key in this row.
		 */
		public int defaultHeight;
		/**
		 * Default horizontal gap between keys in this row.
		 */
		public int defaultHorizontalGap;
		/**
		 * Vertical gap following this row.
		 */
		public int verticalGap;

		private final List<Key> mKeys = new ArrayList<>();

		/**
		 * Edge flags for this row of keys. Possible values that can be assigned are
		 * {@link #EDGE_TOP EDGE_TOP} and {@link #EDGE_BOTTOM EDGE_BOTTOM}
		 */
		public int rowEdgeFlags;

		/**
		 * The keyboard mode for this row
		 */
		public int mode;

		private final Keyboard parent;

		public Row(@NonNull final Keyboard parent) {
			this.parent = parent;
		}

		public Row(Resources res, @NonNull final  Keyboard parent, XmlResourceParser parser) {
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
			this.parent = parent;
			TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
				R.styleable.Keyboard);
			defaultWidth = getDimensionOrFraction(a,
				R.styleable.Keyboard_keyWidth,
				parent.mDisplayWidth, parent.mDefaultWidth);
			defaultHeight = getDimensionOrFraction(a,
				R.styleable.Keyboard_keyHeight,
				parent.mDisplayHeight, parent.mDefaultHeight);
			defaultHorizontalGap = getDimensionOrFraction(a,
				R.styleable.Keyboard_horizontalGap,
				parent.mDisplayWidth, parent.mDefaultHorizontalGap);
			verticalGap = getDimensionOrFraction(a,
				R.styleable.Keyboard_verticalGap,
				parent.mDisplayHeight, parent.mDefaultVerticalGap);
			a.recycle();
			a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Row);
			rowEdgeFlags = a.getInt(R.styleable.Keyboard_Row_rowEdgeFlags, 0);
			mode = a.getResourceId(R.styleable.Keyboard_Row_keyboardMode, 0);
			a.recycle();
		}

		@NonNull
		@Override
		public String toString() {
			return "Row{" +
				"defaultWidth=" + defaultWidth +
				",defaultHeight=" + defaultHeight +
				",defaultHorizontalGap=" + defaultHorizontalGap +
				",verticalGap=" + verticalGap +
				",mKeys=" + mKeys +
				",rowEdgeFlags=" + rowEdgeFlags +
				",mode=" + mode +
				'}';
		}

	} // Row

	/**
	 * Class for describing the position and characteristics of a single key in the keyboard.
	 *
	 * @attr ref R.styleable#Keyboard_keyWidth
	 * @attr ref R.styleable#Keyboard_keyHeight
	 * @attr ref R.styleable#Keyboard_horizontalGap
	 * @attr ref R.styleable#Keyboard_Key_codes
	 * @attr ref R.styleable#Keyboard_Key_keyIcon
	 * @attr ref R.styleable#Keyboard_Key_keyLabel
	 * @attr ref R.styleable#Keyboard_Key_iconPreview
	 * @attr ref R.styleable#Keyboard_Key_isSticky
	 * @attr ref R.styleable#Keyboard_Key_isRepeatable
	 * @attr ref R.styleable#Keyboard_Key_isModifier
	 * @attr ref R.styleable#Keyboard_Key_popupKeyboard
	 * @attr ref R.styleable#Keyboard_Key_popupCharacters
	 * @attr ref R.styleable#Keyboard_Key_keyOutputText
	 * @attr ref R.styleable#Keyboard_Key_keyEdgeFlags
	 */
	public static class Key {
		private static final String TAG = Key.class.getSimpleName();
		/**
		 * All the key codes (unicode or custom code) that this key could generate, zero'th
		 * being the most important.
		 */
		public int[] codes;

		/**
		 * Label to display
		 */
		public CharSequence label;

		/**
		 * Icon to display instead of a label. Icon takes precedence over a label
		 */
		public Drawable icon;
		/**
		 * Preview version of the icon, for the preview popup
		 */
		public Drawable iconPreview;
		/**
		 * Width of the key, not including the gap
		 */
		public int width;
		/**
		 * Height of the key, not including the gap
		 */
		public int height;
		/**
		 * The horizontal gap before this key
		 */
		public int gap;
		/**
		 * Whether this key is sticky, i.e., a toggle key
		 */
		public boolean sticky;
		/**
		 * X coordinate of the key in the keyboard layout
		 */
		public int x;
		/**
		 * Y coordinate of the key in the keyboard layout
		 */
		public int y;
		/**
		 * The current pressed state of this key
		 */
		public boolean pressed;
		/**
		 * If this is a sticky key, is it on?
		 */
		public boolean on;
		/**
		 * Text to output when pressed. This can be multiple characters, like ".com"
		 */
		public CharSequence text;
		/**
		 * Popup characters
		 */
		public CharSequence popupCharacters;

		/**
		 * Flags that specify the anchoring to edges of the keyboard for detecting touch events
		 * that are just out of the boundary of the key. This is a bit mask of
		 * {@link #EDGE_LEFT}, {@link #EDGE_RIGHT}, {@link #EDGE_TOP} and
		 * {@link #EDGE_BOTTOM}.
		 */
		public int edgeFlags;
		/**
		 * Whether this is a modifier key, such as Shift or Alt
		 */
		public boolean modifier;
		/**
		 * The keyboard that this key belongs to
		 */
		private final Keyboard keyboard;
		/**
		 * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
		 * keyboard.
		 */
		public int popupResId;
		/**
		 * Whether this key repeats itself when held down
		 */
		public boolean repeatable;

		private final static int[] KEY_STATE_NORMAL_ON = {
			android.R.attr.state_checkable,
			android.R.attr.state_checked
		};

		private final static int[] KEY_STATE_PRESSED_ON = {
			android.R.attr.state_pressed,
			android.R.attr.state_checkable,
			android.R.attr.state_checked
		};

		private final static int[] KEY_STATE_NORMAL_OFF = {
			android.R.attr.state_checkable
		};

		private final static int[] KEY_STATE_PRESSED_OFF = {
			android.R.attr.state_pressed,
			android.R.attr.state_checkable
		};

		private final static int[] KEY_STATE_NORMAL = {
		};

		private final static int[] KEY_STATE_PRESSED = {
			android.R.attr.state_pressed
		};

		/**
		 * Create an empty key with no attributes.
		 */
		public Key(@NonNull final Row parent) {
//			if (DEBUG) Log.v(TAG, "コンストラクタ:");
			keyboard = parent.parent;
			height = parent.defaultHeight;
			width = parent.defaultWidth;
			gap = parent.defaultHorizontalGap;
			edgeFlags = parent.rowEdgeFlags;
		}

		/**
		 * Create a key with the given top-left coordinate and extract its attributes from
		 * the XML parser.
		 *
		 * @param res    resources associated with the caller's context
		 * @param parent the row that this key belongs to. The row must already be attached to
		 *               a {@link Keyboard}.
		 * @param x      the x coordinate of the top-left
		 * @param y      the y coordinate of the top-left
		 * @param parser the XML parser containing the attributes for this key
		 */
		public Key(@NonNull final  Resources res,
			@NonNull final  Keyboard.Row parent,
			final int x, final int y,
			final XmlResourceParser parser) {

			this(parent);

			this.x = x;
			this.y = y;

			TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
				R.styleable.Keyboard);

			width = getDimensionOrFraction(a,
				R.styleable.Keyboard_keyWidth,
				keyboard.mDisplayWidth, parent.defaultWidth);
			height = getDimensionOrFraction(a,
				R.styleable.Keyboard_keyHeight,
				keyboard.mDisplayHeight, parent.defaultHeight);
			gap = getDimensionOrFraction(a,
				R.styleable.Keyboard_horizontalGap,
				keyboard.mDisplayWidth, parent.defaultHorizontalGap);
			a.recycle();
			a = res.obtainAttributes(Xml.asAttributeSet(parser),
				R.styleable.Keyboard_Key);
			this.x += gap;
			TypedValue codesValue = new TypedValue();
			a.getValue(R.styleable.Keyboard_Key_codes, codesValue);
			if (DEBUG) Log.i(TAG, "Key:" + codesValue);
			if (codesValue.type == TypedValue.TYPE_INT_DEC
				|| codesValue.type == TypedValue.TYPE_INT_HEX) {
				codes = new int[]{codesValue.data};
			} else if (codesValue.type == TypedValue.TYPE_STRING) {
				codes = parseCSV(codesValue.string.toString());
			}

			iconPreview = a.getDrawable(R.styleable.Keyboard_Key_iconPreview);
			if (iconPreview != null) {
				iconPreview.setBounds(0, 0, iconPreview.getIntrinsicWidth(),
					iconPreview.getIntrinsicHeight());
			}
			popupCharacters = a.getText(
				R.styleable.Keyboard_Key_popupCharacters);
			popupResId = a.getResourceId(
				R.styleable.Keyboard_Key_popupKeyboard, 0);
			repeatable = a.getBoolean(
				R.styleable.Keyboard_Key_isRepeatable, false);
			modifier = a.getBoolean(
				R.styleable.Keyboard_Key_isModifier, false);
			sticky = a.getBoolean(
				R.styleable.Keyboard_Key_isSticky, false);
			edgeFlags = a.getInt(R.styleable.Keyboard_Key_keyEdgeFlags, 0);
			edgeFlags |= parent.rowEdgeFlags;

			icon = a.getDrawable(
				R.styleable.Keyboard_Key_keyIcon);
			if (icon != null) {
				icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
			}
			label = a.getText(R.styleable.Keyboard_Key_keyLabel);
			text = a.getText(R.styleable.Keyboard_Key_keyOutputText);

			if (codes == null && !TextUtils.isEmpty(label)) {
				codes = new int[]{label.charAt(0)};
			}
			a.recycle();
		}

		/**
		 * Informs the key that it has been pressed, in case it needs to change its appearance or
		 * state.
		 *
		 * @see #onReleased(boolean)
		 */
		public void onPressed() {
			pressed = !pressed;
		}

		/**
		 * Changes the pressed state of the key.
		 *
		 * <p>Toggled state of the key will be flipped when all the following conditions are
		 * fulfilled:</p>
		 *
		 * <ul>
		 * <li>This is a sticky key, that is, {@link #sticky} is {@code true}.
		 * <li>The parameter {@code inside} is {@code true}.
		 * <li>{@link android.os.Build.VERSION#SDK_INT} is greater than
		 * {@link android.os.Build.VERSION_CODES#LOLLIPOP_MR1}.
		 * </ul>
		 *
		 * @param inside whether the finger was released inside the key. Works only on Android M and
		 *               later. See the method document for details.
		 * @see #onPressed()
		 */
		public void onReleased(boolean inside) {
			pressed = !pressed;
			if (sticky && inside) {
				on = !on;
			}
		}

		int[] parseCSV(String value) {
			int count = 0;
			int lastIndex = 0;
			if (value.length() > 0) {
				count++;
				while ((lastIndex = value.indexOf(",", lastIndex + 1)) > 0) {
					count++;
				}
			}
			int[] values = new int[count];
			count = 0;
			StringTokenizer st = new StringTokenizer(value, ",");
			while (st.hasMoreTokens()) {
				try {
					values[count++] = Integer.parseInt(st.nextToken());
				} catch (NumberFormatException nfe) {
					Log.e(TAG, "Error parsing keycodes " + value);
				}
			}
			return values;
		}

		/**
		 * Detects if a point falls inside this key.
		 *
		 * @param x the x-coordinate of the point
		 * @param y the y-coordinate of the point
		 * @return whether or not the point falls inside the key. If the key is attached to an edge,
		 * it will assume that all points between the key and the edge are considered to be inside
		 * the key.
		 */
		public boolean isInside(int x, int y) {
			boolean leftEdge = (edgeFlags & EDGE_LEFT) > 0;
			boolean rightEdge = (edgeFlags & EDGE_RIGHT) > 0;
			boolean topEdge = (edgeFlags & EDGE_TOP) > 0;
			boolean bottomEdge = (edgeFlags & EDGE_BOTTOM) > 0;
			return ((x >= this.x || (leftEdge && x <= this.x + this.width))
				&& (x < this.x + this.width || (rightEdge && x >= this.x))
				&& (y >= this.y || (topEdge && y <= this.y + this.height))
				&& (y < this.y + this.height || (bottomEdge && y >= this.y)));
		}

		/**
		 * Returns the square of the distance between the center of the key and the given point.
		 *
		 * @param x the x-coordinate of the point
		 * @param y the y-coordinate of the point
		 * @return the square of the distance of the point from the center of the key
		 */
		public int squaredDistanceFrom(int x, int y) {
			int xDist = this.x + width / 2 - x;
			int yDist = this.y + height / 2 - y;
			return xDist * xDist + yDist * yDist;
		}

		/**
		 * Returns the drawable state for the key, based on the current state and type of the key.
		 *
		 * @return the drawable state of the key.
		 * @see android.graphics.drawable.StateListDrawable#setState(int[])
		 */
		public int[] getCurrentDrawableState() {
			int[] states = KEY_STATE_NORMAL;

			if (on) {
				if (pressed) {
					states = KEY_STATE_PRESSED_ON;
				} else {
					states = KEY_STATE_NORMAL_ON;
				}
			} else {
				if (sticky) {
					if (pressed) {
						states = KEY_STATE_PRESSED_OFF;
					} else {
						states = KEY_STATE_NORMAL_OFF;
					}
				} else {
					if (pressed) {
						states = KEY_STATE_PRESSED;
					}
				}
			}
			return states;
		}

		@NonNull
		@Override
		public String toString() {
			return "Key{" +
				"codes=" + Arrays.toString(codes) +
				",label=" + label +
				",icon=" + icon +
				",iconPreview=" + iconPreview +
				",width=" + width +
				",height=" + height +
				",gap=" + gap +
				",sticky=" + sticky +
				",x=" + x +
				",y=" + y +
				",pressed=" + pressed +
				",on=" + on +
				",text=" + text +
				",popupCharacters=" + popupCharacters +
				",edgeFlags=" + edgeFlags +
				",modifier=" + modifier +
				",keyboard=" + keyboard +
				",popupResId=" + popupResId +
				",repeatable=" + repeatable +
				'}';
		}
	} // Key

	/**
	 * Creates a keyboard from the given xml key layout file.
	 *
	 * @param context        the application or service context
	 * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
	 */
	public Keyboard(@NonNull final  Context context, @XmlRes final int xmlLayoutResId) {
		this(context, xmlLayoutResId, 0);
	}

	/**
	 * Creates a keyboard from the given xml key layout file. Weeds out rows
	 * that have a keyboard mode defined but don't match the specified mode.
	 *
	 * @param context        the application or service context
	 * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
	 * @param modeId         keyboard mode identifier
	 * @param width          sets width of keyboard
	 * @param height         sets height of keyboard
	 */
	public Keyboard(@NonNull final  Context context,
		@XmlRes int xmlLayoutResId, int modeId,
		final int width, final int height) {

		mDisplayWidth = width;
		mDisplayHeight = height;

		mDefaultHorizontalGap = 0;
		mDefaultWidth = mDisplayWidth / 10;
		mDefaultVerticalGap = 0;
		mDefaultHeight = mDefaultWidth;
		mKeys = new ArrayList<Key>();
		mModifierKeys = new ArrayList<Key>();
		mKeyboardMode = modeId;
		loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
	}

	/**
	 * Creates a keyboard from the given xml key layout file. Weeds out rows
	 * that have a keyboard mode defined but don't match the specified mode.
	 *
	 * @param context        the application or service context
	 * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
	 * @param modeId         keyboard mode identifier
	 */
	@SuppressWarnings("SuspiciousNameCombination")
	public Keyboard(@NonNull final Context context,
		@XmlRes final int xmlLayoutResId, final int modeId) {

		final DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mDisplayWidth = dm.widthPixels;
		mDisplayHeight = dm.heightPixels;
		//Log.v(TAG, "keyboard's display metrics:" + dm);

		mDefaultHorizontalGap = 0;
		mDefaultWidth = mDisplayWidth / 10;
		mDefaultVerticalGap = 0;
		mDefaultHeight = mDefaultWidth;
		mKeys = new ArrayList<Key>();
		mModifierKeys = new ArrayList<Key>();
		mKeyboardMode = modeId;
		loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
	}

	/**
	 * <p>Creates a blank keyboard from the given resource file and populates it with the specified
	 * characters in left-to-right, top-to-bottom fashion, using the specified number of columns.
	 * </p>
	 * <p>If the specified number of columns is -1, then the keyboard will fit as many keys as
	 * possible in each row.</p>
	 *
	 * @param context             the application or service context
	 * @param layoutTemplateResId the layout template file, containing no keys.
	 * @param characters          the list of characters to display on the keyboard. One key will be created
	 *                            for each character.
	 * @param columns             the number of columns of keys to display. If this number is greater than the
	 *                            number of keys that can fit in a row, it will be ignored. If this number is -1, the
	 *                            keyboard will fit as many keys as possible in each row.
	 */
	public Keyboard(@NonNull final Context context,
		int layoutTemplateResId,
		final CharSequence characters,
		final int columns, final int horizontalPadding) {

		this(context, layoutTemplateResId);
		int x = 0;
		int y = 0;
		int column = 0;
		mTotalWidth = 0;

		Row row = new Row(this);
		row.defaultHeight = mDefaultHeight;
		row.defaultWidth = mDefaultWidth;
		row.defaultHorizontalGap = mDefaultHorizontalGap;
		row.verticalGap = mDefaultVerticalGap;
		row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
		final int maxColumns = columns == -1 ? Integer.MAX_VALUE : columns;
		for (int i = 0; i < characters.length(); i++) {
			char c = characters.charAt(i);
			if (column >= maxColumns
				|| x + mDefaultWidth + horizontalPadding > mDisplayWidth) {
				x = 0;
				y += mDefaultVerticalGap + mDefaultHeight;
				column = 0;
			}
			final Key key = new Key(row);
			key.x = x;
			key.y = y;
			key.label = String.valueOf(c);
			key.codes = new int[]{c};
			column++;
			x += key.width + key.gap;
			mKeys.add(key);
			row.mKeys.add(key);
			if (x > mTotalWidth) {
				mTotalWidth = x;
			}
		}
		mTotalHeight = y + mDefaultHeight;
		rows.add(row);
	}

	final void resize(final int newWidth, final int newHeight) {
		if (DEBUG) Log.v(TAG, "resize:");
		int numRows = rows.size();
		for (int rowIndex = 0; rowIndex < numRows; ++rowIndex) {
			Row row = rows.get(rowIndex);
			int numKeys = row.mKeys.size();
			int totalGap = 0;
			int totalWidth = 0;
			for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
				Key key = row.mKeys.get(keyIndex);
				if (keyIndex > 0) {
					totalGap += key.gap;
				}
				totalWidth += key.width;
			}
			if (totalGap + totalWidth > newWidth) {
				int x = 0;
				float scaleFactor = (float) (newWidth - totalGap) / totalWidth;
				for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
					Key key = row.mKeys.get(keyIndex);
					key.width *= scaleFactor;
					key.x = x;
					x += key.width + key.gap;
				}
			}
		}
		mTotalWidth = newWidth;
		// TODO: This does not adjust the vertical placement according to the new size.
		// The main problem in the previous code was horizontal placement/size, but we should
		// also recalculate the vertical sizes/positions when we get this resize call.
	}

	public List<Key> getKeys() {
		return mKeys;
	}

	public List<Key> getModifierKeys() {
		return mModifierKeys;
	}

	protected int getHorizontalGap() {
		return mDefaultHorizontalGap;
	}

	protected void setHorizontalGap(int gap) {
		mDefaultHorizontalGap = gap;
	}

	protected int getVerticalGap() {
		return mDefaultVerticalGap;
	}

	protected void setVerticalGap(int gap) {
		mDefaultVerticalGap = gap;
	}

	protected int getKeyHeight() {
		return mDefaultHeight;
	}

	protected void setKeyHeight(int height) {
		mDefaultHeight = height;
	}

	protected int getKeyWidth() {
		return mDefaultWidth;
	}

	protected void setKeyWidth(int width) {
		mDefaultWidth = width;
	}

	/**
	 * Returns the total height of the keyboard
	 *
	 * @return the total height of the keyboard
	 */
	public int getHeight() {
		return mTotalHeight;
	}

	public int getMinWidth() {
		return mTotalWidth;
	}

	public boolean setShifted(boolean shiftState) {
		for (Key shiftKey : mShiftKeys) {
			if (shiftKey != null) {
				shiftKey.on = shiftState;
			}
		}
		if (mShifted != shiftState) {
			mShifted = shiftState;
			return true;
		}
		return false;
	}

	public boolean isShifted() {
		return mShifted;
	}

	/**
	 * @hide
	 */
	public int[] getShiftKeyIndices() {
		return mShiftKeyIndices;
	}

	public int getShiftKeyIndex() {
		return mShiftKeyIndices[0];
	}

	private void computeNearestNeighbors() {
		if (DEBUG) Log.v(TAG, "computeNearestNeighbors:");
		// Round-up so we don't have any pixels outside the grid
		mCellWidth = (getMinWidth() + GRID_WIDTH - 1) / GRID_WIDTH;
		mCellHeight = (getHeight() + GRID_HEIGHT - 1) / GRID_HEIGHT;
		mGridNeighbors = new int[GRID_SIZE][];
		int[] indices = new int[mKeys.size()];
		final int gridWidth = GRID_WIDTH * mCellWidth;
		final int gridHeight = GRID_HEIGHT * mCellHeight;
		for (int x = 0; x < gridWidth; x += mCellWidth) {
			for (int y = 0; y < gridHeight; y += mCellHeight) {
				int count = 0;
				for (int i = 0; i < mKeys.size(); i++) {
					final Key key = mKeys.get(i);
					if (key.squaredDistanceFrom(x, y) < mProximityThreshold ||
						key.squaredDistanceFrom(x + mCellWidth - 1, y) < mProximityThreshold ||
						key.squaredDistanceFrom(x + mCellWidth - 1, y + mCellHeight - 1)
							< mProximityThreshold ||
						key.squaredDistanceFrom(x, y + mCellHeight - 1) < mProximityThreshold) {
						indices[count++] = i;
					}
				}
				int[] cell = new int[count];
				System.arraycopy(indices, 0, cell, 0, count);
				mGridNeighbors[(y / mCellHeight) * GRID_WIDTH + (x / mCellWidth)] = cell;
			}
		}
	}

	/**
	 * Returns the indices of the keys that are closest to the given point.
	 *
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @return the array of integer indices for the nearest keys to the given point. If the given
	 * point is out of range, then an array of size zero is returned.
	 */
	public int[] getNearestKeys(final int x, final int y) {
		if (DEBUG) Log.v(TAG, "getNearestKeys:");
		if (mGridNeighbors == null) computeNearestNeighbors();
		if (x >= 0 && x < getMinWidth() && y >= 0 && y < getHeight()) {
			int index = (y / mCellHeight) * GRID_WIDTH + (x / mCellWidth);
			if (index < GRID_SIZE) {
				return mGridNeighbors[index];
			}
		}
		return new int[0];
	}

	protected Row createRowFromXml(@NonNull final Resources res,
		@NonNull final XmlResourceParser parser) {

		if (DEBUG) Log.v(TAG, "createRowFromXml:");
		return new Row(res, this, parser);
	}

	protected Key createKeyFromXml(@NonNull final Resources res,
		@NonNull final Row parent,
		final int x, final int y,
		@NonNull final XmlResourceParser parser) {

		if (DEBUG) Log.v(TAG, "createKeyFromXml:");
		return new Key(res, parent, x, y, parser);
	}

	private void loadKeyboard(Context context, XmlResourceParser parser) {
		if (DEBUG) Log.v(TAG, "loadKeyboard:");
		boolean inKey = false;
		boolean inRow = false;
		boolean leftMostKey = false;
		int row = 0;
		int x = 0;
		int y = 0;
		Key key = null;
		Row currentRow = null;
		Resources res = context.getResources();
		boolean skipRow;

		try {
			int event;
			while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
				if (event == XmlResourceParser.START_TAG) {
					String tag = parser.getName();
					if (TAG_ROW.equals(tag)) {
						inRow = true;
						x = 0;
						currentRow = createRowFromXml(res, parser);
						rows.add(currentRow);
						skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
						if (skipRow) {
							skipToEndOfRow(parser);
							inRow = false;
						}
					} else if (TAG_KEY.equals(tag)) {
						inKey = true;
						key = createKeyFromXml(res, currentRow, x, y, parser);
						Log.d(TAG, "loadKeyboard:key=" + key);
						mKeys.add(key);
						if (key.codes[0] == KEYCODE_SHIFT) {
							// Find available shift key slot and put this shift key in it
							for (int i = 0; i < mShiftKeys.length; i++) {
								if (mShiftKeys[i] == null) {
									mShiftKeys[i] = key;
									mShiftKeyIndices[i] = mKeys.size() - 1;
									break;
								}
							}
							mModifierKeys.add(key);
						} else if (key.codes[0] == KEYCODE_ALT) {
							mModifierKeys.add(key);
						}
						currentRow.mKeys.add(key);
					} else if (TAG_KEYBOARD.equals(tag)) {
						parseKeyboardAttributes(res, parser);
					}
				} else if (event == XmlResourceParser.END_TAG) {
					if (inKey) {
						inKey = false;
						x += key.gap + key.width;
						if (x > mTotalWidth) {
							mTotalWidth = x;
						}
					} else if (inRow) {
						inRow = false;
						y += currentRow.verticalGap;
						y += currentRow.defaultHeight;
						row++;
//					} else {
//						// TODO: error or extend?
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Parse error:" + e);
		}
		mTotalHeight = y - mDefaultVerticalGap;
	}

	private void skipToEndOfRow(XmlResourceParser parser)
		throws XmlPullParserException, IOException {

		if (DEBUG) Log.v(TAG, "skipToEndOfRow:");
		int event;
		while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
			if (event == XmlResourceParser.END_TAG
				&& parser.getName().equals(TAG_ROW)) {
				break;
			}
		}
	}

	private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
		if (DEBUG) Log.v(TAG, "parseKeyboardAttributes:");

		TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
			R.styleable.Keyboard);

		mDefaultWidth = getDimensionOrFraction(a,
			R.styleable.Keyboard_keyWidth,
			mDisplayWidth, mDisplayWidth / 10);
		mDefaultHeight = getDimensionOrFraction(a,
			R.styleable.Keyboard_keyHeight,
			mDisplayHeight, 50);
		mDefaultHorizontalGap = getDimensionOrFraction(a,
			R.styleable.Keyboard_horizontalGap,
			mDisplayWidth, 0);
		mDefaultVerticalGap = getDimensionOrFraction(a,
			R.styleable.Keyboard_verticalGap,
			mDisplayHeight, 0);
		mProximityThreshold = (int) (mDefaultWidth * SEARCH_DISTANCE);
		mProximityThreshold = mProximityThreshold * mProximityThreshold; // Square it for comparison
		a.recycle();
	}

	static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
		TypedValue value = a.peekValue(index);
		if (value == null) return defValue;
		if (value.type == TypedValue.TYPE_DIMENSION) {
			return a.getDimensionPixelOffset(index, defValue);
		} else if (value.type == TypedValue.TYPE_FRACTION) {
			// Round it to avoid values like 47.9999 from getting truncated
			return Math.round(a.getFraction(index, base, base, defValue));
		}
		return defValue;
	}
}
