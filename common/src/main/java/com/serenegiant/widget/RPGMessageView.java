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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.serenegiant.common.R;
import com.serenegiant.graphics.CanvasUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * RPGのテキスト表示風に文字を表示するためのカスタムビュー
 * FIXME 文字の表示方向・行送り方向を指定できるようにする
 * FIXME 文字を表示するときに効果音を鳴らせるようにする
 */
public class RPGMessageView extends View {
	private static final boolean DEBUG = false;	// set false on production
	private static final boolean SHOW_GRID = false; 	// set false on production
	private static final String TAG = RPGMessageView.class.getSimpleName();

	/**
	 * コールバックリスナーインターフェース
	 */
	public interface MessageEventListener {
		/**
		 * セットしてあるメッセージが最後まで表示されたときのイベントコールバック
		 * @param view
		 * @return -1: すべてのメッセージが消えるまで行送りする, 0以上: 行送りはせずに返した値[ミリ秒]待機してからクリアする
		 */
		public int onMessageEnd(@NonNull final RPGMessageView view);

		/**
		 * メッセージがクリアされたときのイベントコールバック
		 * @param view
		 */
		public void onCleared(@NonNull final RPGMessageView view);
	}

	private static final char EMPTY_CHAR = '\0';
	private static final String CR_STR = "\n";
	/**
	 * 1行あたりの文字数の最大値
	 */
	private static final int MAX_COLS = 64;
	/**
	 * 1行あたりの文字数のデフォルト値
	 */
	private static final int DEFAULT_COLS = 16;
	/**
	 * 最大行数
	 */
	private static final int MAX_ROWS = 16;
	/**
	 * デフォルトの行数
	 */
	private static final int DEFAULT_ROWS = 4;
	/**
	 * 1文字表示するのに費やす時間のデフォルト値(50ms)
	 */
	private static final int DEFAULT_DURATION_MS_PER_CHAR = 50;
	/**
	 * 行送りした後次に描画するまでの遅延時間のデフォルト値(500ms)
	 */
	private static final int DEFAULT_DURATION_MS_PER_LINE = 500;

//--------------------------------------------------------------------------------
	/**
	 * Viewのサイズを示すRectFインスタンス
	 * #saveLayer呼び出し時に使用される描画領域の最大サイズ
	 */
	private final RectF mViewBoundsF = new RectF();
	/**
	 * #saveLayer呼び出し時にViewのPaintを保存するPaintインスタンス
	 */
	private final Paint mPaintCopied = new Paint();
	/**
	 * デバッグ用にグリッド線を引くときに使うPaintインスタンス
	 */
	private final Paint mGridPaint = new Paint();
	/**
	 * 文字を描画するときに使うPaintインスタンス
	 */
	private final Paint mTextPaint = new Paint();
	/**
	 * 文字の輪郭を描画する時に使うPaintインスタンス
	 */
	private final Paint mTextStrokePaint = new Paint();
	/**
	 * 文字描画の背景drawable
	 */
	@Nullable
	private Drawable mTextBackground;
	/**
	 * 文字描画のTypeface
	 */
	@Nullable
	private Typeface mTextTypeface = null;
	/**
	 * 文字を描画する際の文字領域取得用
	 */
	private final Rect mTextBound = new Rect();

	/**
	 * コールバックリスナー
	 */
	@Nullable
	private MessageEventListener mEventListener;
	/**
	 * 文字を描画する間隔[ミリ秒]
	 */
	private long mDrawDurationMsPerChar;
	/**
	 * 行送り後の待機時間[ミリ秒]
	 */
	private long mDrawDurationMsPerLine;
	/**
	 * 1行あたりに表示する文字数
	 */
	private int mCols = DEFAULT_COLS;
	/**
	 * 表示する行数
	 */
	private int mRows = DEFAULT_ROWS;
	/**
	 * 1文字の描画領域の幅
	 */
	private float mColWidth = 0.0f;
	/**
	 * 1文字の描画領域の高さ
	 */
	private float mRowHeight = 0.0f;
	/**
	 * onDrawで描画するときの文字配列
	 * mCols x mRows
	 */
	@NonNull
	private char[] mMessage = new char[1];
	/**
	 * 次に表示する文字配列の位置[0, mCols)
	 */
	private int mNextMessageColIx = 0;
	/**
	 * 表示待ちの文字列
	 */
	private final LinkedList<String> mLines = new LinkedList<>();
	/**
	 * 次に追加する行文字列
	 */
	@NonNull
	private String mFirstLine = "";
	/**
	 * 次に表示用に追加するmText中の文字の位置[-1, mText.length)
	 */
	private int mNextAppendIx = 0;
	/**
	 * 最後までメッセージを表示したときのアクション
	 * Integer.MAX_VALUE: 無効なのでイベントリスナーを呼び出す,
	 * -1: すべてのメッセージが消えるまで行送りする,
	 * 0以上: クリアするまでの待機時間をミリ秒として返す
	 */
	private int mActionOnMessageEnd = Integer.MAX_VALUE;
	/**
	 * 文字に輪郭線を付加して表示するかどうか
	 */
	private boolean mHasTextStroke = false;
	/**
	 * 次にinvalidate予定の時刻
	 */
	private long mNextInvalidateMs = 0L;
	/**
	 * 文字がセットされている行数
	 */
	private int mVisibleRows = 0;
	/**
	 * 最後にセットまたは追加されたメッセージ
	 */
	private String mLastText = null;
//--------------------------------------------------------------------------------
	/**
	 * コンストラクタ
	 * @param context
	 */
	public RPGMessageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 */
	public RPGMessageView(final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public RPGMessageView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		final TypedArray a = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.RPGMessageView, defStyleAttr, 0);
		try {
			mCols = a.getInteger(R.styleable.RPGMessageView_cols, mCols);
			mRows = a.getInteger(R.styleable.RPGMessageView_rows, mRows);
			// 文字色
			mTextPaint.setColor(a.getColor(R.styleable.RPGMessageView_android_textColor, mTextPaint.getColor()));
			final int strokeColor = a.getColor(R.styleable.RPGMessageView_textStrokeColor, mTextPaint.getColor());
			final float strokeWidth = a.getDimension(R.styleable.RPGMessageView_textStrokeWidth, 0);
			if (DEBUG) Log.v(TAG, String.format("stroke cl=0x%08x,w=%f", strokeColor, strokeWidth));
			if ((strokeColor != mTextPaint.getColor()) && (strokeWidth > 0)) {
				mHasTextStroke = true;
				mTextStrokePaint.setStyle(Paint.Style.STROKE);
				mTextStrokePaint.setStrokeWidth(strokeWidth);
				mTextStrokePaint.setColor(strokeColor);
			}
			// 文字間の表示待機時間[ミリ秒]
			mDrawDurationMsPerChar = a.getInteger(R.styleable.RPGMessageView_durationPerChar, DEFAULT_DURATION_MS_PER_CHAR);
			if (mDrawDurationMsPerChar < 0) {
				mDrawDurationMsPerChar = DEFAULT_DURATION_MS_PER_CHAR;
			}
			// 行間の表示待機時間[ミリ秒]
			mDrawDurationMsPerLine = a.getInteger(R.styleable.RPGMessageView_durationPerLine, DEFAULT_DURATION_MS_PER_LINE);
			if (mDrawDurationMsPerLine < 0) {
				mDrawDurationMsPerLine = DEFAULT_DURATION_MS_PER_LINE;
			}
			// 文字背景用drawable
			mTextBackground = a.getDrawable(R.styleable.RPGMessageView_textBackground);
//			// 文字表示に使うTypeface
//			// XXX API26以降でTypedArray#getFontが使えるはずだけどinflate時にクラッシュするので無効に
//			if (BuildCheck.isAPI26()) {
//				mTextTypeface = a.getFont(R.styleable.RPGMessageView_android_typeface);
//			}
		} finally {
			a.recycle();
		}
		resetGrid();
	}

	@Override
	protected void onLayout(
		final boolean changed,
		final int left, final int top,
		final int right, final int bottom) {

		super.onLayout(changed, left, top, right, bottom);
		final int width = right - left;
		final int height = bottom - top;
		if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)-(%d,%d)(%dx%d)",
			left, top, right, bottom, width, height));
		if ((width == 0) || (height == 0)) return;
		resize(width, height);
	}

	final float[] textWidth = new float[1];
	@Override
	protected void onDraw(@NonNull final Canvas canvas) {
//		if (DEBUG) Log.v(TAG, "onDraw:");
		final int saveCount = CanvasUtils.saveLayer(canvas, mViewBoundsF, mPaintCopied);
		try {
			super.onDraw(canvas);
			final float width = mViewBoundsF.width();
			final float height = mViewBoundsF.height();
			if (SHOW_GRID) {
				// デバッグ用にグリッド線を描画する
				for (int ix = 0; ix <= mCols; ix++) {
					final float x = ix * mColWidth;
					canvas.drawLine(x, 0.0f, x, height, mGridPaint);
					for (int iy = 0; iy <= mRows; iy++) {
						final float y = iy * mRowHeight;
						canvas.drawLine(0.0f, y, width, y, mGridPaint);
					}
				}
			}
			if (mNextMessageColIx >= 0) {
				// 表示する文字を更新
				updateMessage();
			}
			final int left = getPaddingLeft();
			final int top = getPaddingTop();
			mTextPaint.getTextBounds("W", 0, 1, mTextBound);
			final int boundHeight = mTextBound.height();
			for (int iy = 0; iy < mRows; iy++) {
				final float y = (mRows - iy) * mRowHeight + top;
				final int px = iy * mCols;
				for (int ix = 0; ix < mCols; ix++) {
					final int pos = px + ix;
					if (mMessage[pos] == EMPTY_CHAR) break;	// EMPTY_CHAR以降は描画をスキップ
					final float x = ix * mColWidth + left;
					mTextPaint.getTextWidths(mMessage, pos, 1, textWidth);
					// 上下左右中央に表示
					final float xx = x + (mColWidth - textWidth[0]) / 2.0f;
					final float yy = y - (mRowHeight - boundHeight/*mTextBound.height()*/) / 2.0f;
					if (mTextBackground != null) {
						mTextBackground.setBounds((int)(x), (int)(y - mRowHeight), (int)(x + mColWidth), (int)(y));
						mTextBackground.draw(canvas);
					}
					if (mHasTextStroke) {
						canvas.drawText(mMessage, pos, 1, xx, yy, mTextStrokePaint);
					}
					canvas.drawText(mMessage, pos, 1, xx, yy, mTextPaint);
				}
			}
		} catch (Exception e) {
			if (DEBUG) Log.w(TAG, e);
		} finally {
			canvas.restoreToCount(saveCount);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * イベントリスナーをセット
	 * @param listener
	 */
	public void setEventListener(@Nullable final MessageEventListener listener) {
		mEventListener = listener;
	}

	/**
	 * 現在セットされているイベントリスナーを取得
	 * @return
	 */
	public MessageEventListener getEventListener() {
		return mEventListener;
	}

	/**
	 * 1行あたりの文字数をセット
	 * @param cols
	 * @throws IllegalArgumentException
	 */
	public void setCols(@IntRange(from = 1) final int cols) throws IllegalArgumentException {
		if (cols < 1) {
			throw new IllegalArgumentException("cols should grater than 0");
		}
		mCols = cols;
		resetGrid();
	}

	/**
	 * 1行あたりの文字数を取得
	 * @return
	 */
	public int getCols() {
		return mCols;
	}

	/**
	 * 表示行数をセット
	 * @param rows
	 * @throws IllegalArgumentException
	 */
	public void setRows(@IntRange(from = 1) final int rows) throws IllegalArgumentException {
		if (rows < 1) {
			throw new IllegalArgumentException("rows should grater than 0");
		}
		mRows = rows;
		resetGrid();
	}

	/**
	 * 表示行数を取得
	 * @return
	 */
	public int getRows() {
		return mRows;
	}

	/**
	 * 1行に表示する文字数と行数をセット
	 * @param cols
	 * @param rows
	 */
	public void setSize(@IntRange(from = 1) final int cols, @IntRange(from = 1) final int rows) {
		if ((cols < 1) || (rows < 1)) {
			throw new IllegalArgumentException("cols/rows should grater than 0");
		}
		mCols = cols;
		mRows = rows;
		resetGrid();
	}

	/**
	 * 文字の背景に使う色/drawableを設定
	 * @param drawable
	 */
	public void setTextBackground(@Nullable final Drawable drawable) {
		mTextBackground = drawable;
	}

	/**
	 * 文字の背景に使う色/drawableを取得
	 * 内部変数をそのまま返すので変更は要注意
	 * @return
	 */
	public Drawable getTextBackground() {
		return mTextBackground;
	}

	/**
	 * 文字描画に使うTypefaceを指定
	 * nullの場合はデフォルトのTypeface.M
	 * @param typeface
	 */
	public void setTypeface(@Nullable final Typeface typeface) {
		mTextTypeface = typeface;
		updatePaints();
	}

	/**
	 * 文字描画に使うTypefaceを取得
	 * 内部変数をそのまま返すので変更してはだめ!
	 * @return
	 */
	public Typeface getTypeface() {
		return mTextTypeface;
	}

	/**
	 * 文字色を設定
	 * @param color
	 */
	public void setTextColor(@ColorInt final int color) {
		mTextPaint.setColor(color);
	}

	/**
	 * 文字色を取得
	 * @return
	 */
	@ColorInt
	public int getTextColor() {
		return mTextPaint.getColor();
	}

	/**
	 * 文字の輪郭線色を設定
	 * @param color 文字色と同じ場合は輪郭線を表示しない
	 */
	public void setTextStrokeColor(@ColorInt final int color) {
		setTextStroke(color, mTextStrokePaint.getStrokeWidth());
	}

	/**
	 * 文字の輪郭線色を取得
	 * @return
	 */
	@ColorInt
	public int getTextStrokeColor() {
		return mTextStrokePaint.getColor();
	}

	/**
	 * 文字の輪郭線幅を設定
	 * @param width 0なら輪郭線を表示しない
	 */
	public void setTextStrokeWidth(final int width) {
		setTextStroke(mTextStrokePaint.getColor(), width);
	}

	/**
	 * 文字の輪郭線幅を設定
	 */
	public int getTextStrokeWidth() {
		return mHasTextStroke ? mTextStrokePaint.getColor() : 0;
	}

	/**
	 * 文字の輪郭線を設定
	 * @param color 文字色と同じ場合は輪郭線を表示しない
	 * @param width 0なら輪郭線を表示しない
	 */
	public void setTextStroke(@ColorInt final int color, final float width) {
		mTextStrokePaint.setColor(color);
		mTextStrokePaint.setStrokeWidth(width);
		mHasTextStroke = (color != mTextPaint.getColor()) && (width > 0);
	}

	/**
	 * 文字を描画する間隔[ミリ秒]を設定
	 * @param durationMs
	 */
	public void setDurationPerChar(final long durationMs) {
		if (durationMs >= 0) {
			mDrawDurationMsPerChar = durationMs;
		}
	}

	/**
	 * 文字を描画する間隔[ミリ秒]を取得
	 * @return
	 */
	public long getDurationPerChar() {
		return mDrawDurationMsPerChar;
	}

	/**
	 * 行送り後の待機時間[ミリ秒]を設定
	 * @param durationMs
	 */
	public void setDurationPerLine(final long durationMs) {
		if (durationMs >= 0) {
			mDrawDurationMsPerLine = durationMs;
		}
	}

	/**
	 * 行送り後の待機時間[ミリ秒]を取得
	 * @return
	 */
	public long getDurationPerLine() {
		return mDrawDurationMsPerLine;
	}
//--------------------------------------------------------------------------------
	/**
	 * 行追加する
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * @param text
	 */
	public void addLine(@Nullable final String text) {
		addLine(text, true);
	}

	/**
	 * 行追加する
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * @param text
	 * @param force 前回と同じテキストがセットされたときにもセットするかどうか
	 */
	public void addLine(@Nullable final String text, final boolean force) {
		if (DEBUG) Log.v(TAG, "addLine:" + text);
		if (TextUtils.isEmpty(text)) return;
		if (!force && Objects.equals(text, mLastText)) return;
		mLastText = text;
		removeCallbacks(mClearTask);
		final long durationMs = mNextInvalidateMs - System.currentTimeMillis();
		if (mFirstLine.isEmpty() && mLines.isEmpty() && (mVisibleRows > 0)) {
			// 既に表示するメッセージが無いとき
			// ...ただしクリア待ちで前回のメッセージが表示されているかもしれないのでリセットする
			if (durationMs > 0) {
				lineFeed();
			} else {
				reset();
			}
		}
		mLines.addAll(splitLines(text));
		if (popFirstIfNeed()) {
			// 新たに1行目をとりだしたときだけ再描画要求する
			if (durationMs <= 0) {
				postInvalidate();
			} else {
				postInvalidateDelayed(durationMs);
			}
		}
	}

	/**
	 * 文字列を追加
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * 追加する先頭行を既存の最後の行に連結する
	 * @param text
	 */
	public void addText(@Nullable final String text) {
		addText(text, true);
	}

	/**
	 * 文字列を追加
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * 追加する先頭行を既存の最後の行に連結する
	 * @param text
	 * @param force 前回と同じテキストがセットされたときにもセットするかどうか
	 */
	public void addText(@Nullable final String text, final boolean force) {
		if (DEBUG) Log.v(TAG, "addText:" + text);
		if (TextUtils.isEmpty(text)) return;
		if (!force && Objects.equals(text, mLastText)) return;
		mLastText = text;
		removeCallbacks(mClearTask);
		final long durationMs = mNextInvalidateMs - System.currentTimeMillis();
		if (mFirstLine.isEmpty() && mLines.isEmpty() && (mVisibleRows > 0)) {
			// 既に表示するメッセージが無いとき
			// ...ただしクリア待ちで前回のメッセージが表示されているかもしれないのでリセットする
			if (durationMs > 0) {
				lineFeed();
			} else {
				reset();
			}
		}
		final List<String> lines = splitLines(text);
		if (mLines.isEmpty()) {
			// 描画するメッセージが無いときはそのまま追加
			mLines.addAll(lines);
		} else if (!lines.isEmpty()) {
			// 先頭行はmTextの最後の文字列に連結する
			final String last = mLines.removeLast();
			mLines.add(last + lines.get(0));
			lines.remove(0);
			// それ以外は行として追加する
			mLines.addAll(lines);
		}
		if (popFirstIfNeed()) {
			// 新たに1行目をとりだしたときだけ再描画要求する
			if (durationMs <= 0) {
				postInvalidate();
			} else {
				postInvalidateDelayed(durationMs);
			}
		}
	}

	/**
	 * 文字列を置き換え
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * @param text
	 */
	public void setText(@Nullable final String text) {
		setText(text, true);
	}

	/**
	 * 文字列を置き換え
	 * 文字列に改行コードが含まれる場合は複数行に分割して追加する
	 * @param text
	 * @param force 前回と同じテキストがセットされたときにもセットするかどうか
	 */
	public void setText(@Nullable final String text, final boolean force) {
		if (DEBUG) Log.v(TAG, "setText:" + text);
		if (!force && Objects.equals(text, mLastText)) return;
		mLastText = text;
		reset();
		mLines.addAll(splitLines(text));
		if (popFirstIfNeed()) {
			// 新たに1行目をとりだしたときだけ再描画要求する
			postInvalidate();
		}
	}

	/**
	 * 表示中のメッセージがあれば消去する
	 */
	public void clear() {
		if (DEBUG) Log.v(TAG, "clear:");
		reset();
		mLastText = null;
		postInvalidate();
		if (mEventListener != null) {
			mEventListener.onCleared(this);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * clearを遅延呼び出しするためのRunnable
	 */
	private final Runnable mClearTask = new Runnable() {
		@Override
		public void run() {
			clear();
		}
	};

	/**
	 * 表示関係の変数をリセットする
	 */
	private void reset() {
		if (DEBUG) Log.v(TAG, "reset:");
		removeCallbacks(mClearTask);
		mLines.clear();
		mVisibleRows = 0;
		// 文字の追加処理をしない
		mNextAppendIx = -1;
		mNextInvalidateMs = -1L;
		// 表示するmMessage文字配列の追加位置をリセット
		mNextMessageColIx = 0;
		mActionOnMessageEnd = Integer.MAX_VALUE;
		// 未使用部分を埋める
		final int len = mCols * mRows;
		Arrays.fill(mMessage, 0, len, EMPTY_CHAR);
	}

	/**
	 * 必要であれば＆可能であれば先頭行を取り出す
	 * @return 先頭行を取り出したときはtrue
	 */
	private boolean popFirstIfNeed() {
		boolean result = false;

		if (((mNextAppendIx < 0) || mFirstLine.isEmpty()) && !mLines.isEmpty()) {
			if (DEBUG) Log.v(TAG, "popFirstIfNeed:pop first line");
			// 先頭行はmTextへ取り出しておく
			mFirstLine = mLines.removeFirst();
			// 次に表示用に追加するmText中の文字位置をリセット
			mNextAppendIx = 0;
			mNextMessageColIx = 0;
			mVisibleRows = (mVisibleRows + 1) % mRows;
			result = true;
		}

		return result;
	}

	/**
	 * 描画グリッドをリセット
	 */
	private void resetGrid() {
		if (mCols < 1) {
			mCols = 1;
		} else if (mCols > MAX_COLS) {
			mCols = MAX_COLS;
		}
		if (mRows < 1) {
			mRows = 1;
		} else if (mRows > MAX_ROWS) {
			mRows = MAX_ROWS;
		}
		if (DEBUG) Log.v(TAG, String.format("resetGrid:(%dx%d)", mCols, mRows));
		final int len = mCols * mRows;
		mMessage = new char[len];
		reset();
		postInvalidate();
	}

	/**
	 * Viewのサイズ変更時の処理
	 * @param width Viewの幅
	 * @param height Viewの高さ
	 */
	private void resize(final int width, final int height) {
		if (DEBUG) Log.v(TAG, String.format("resize:(%dx%d)", width, height));
		mViewBoundsF.set(0, 0, width, height);
		final float clientWidth = width - getPaddingLeft() - getPaddingRight();
		final float clientHeight = height - getPaddingTop() - getPaddingBottom();
		// 1文字あたりの描画領域のサイズを計算
		mColWidth = clientWidth / mCols;
		mRowHeight = clientHeight / mRows;
		updatePaints();
		final List<String> copy = new ArrayList<>(mLines);
		reset();
		mLines.addAll(copy);
	}

	/**
	 * 描画用のPaintの設定を更新
	 */
	private void updatePaints() {
		if (mTextTypeface != null) {
			mTextPaint.setTypeface(mTextTypeface);
			mTextStrokePaint.setTypeface(mTextTypeface);
		} else {
			// デフォルトは等幅フォント
			mTextPaint.setTypeface(Typeface.MONOSPACE);
			mTextStrokePaint.setTypeface(Typeface.MONOSPACE);
		}
		// テキストサイズを計算
		final float sz = Math.min(mColWidth, mRowHeight) * 0.95f;
		mTextPaint.setTextSize(sz);
		mTextStrokePaint.setTextSize(sz);
	}

	/**
	 * 表示する文字を更新
	 */
	private void updateMessage() {
		if (DEBUG) Log.v(TAG, String.format("updateMessage:text=%s,ix=%d", mFirstLine, mNextAppendIx));
		if ((mNextAppendIx >= 0) && !mFirstLine.isEmpty()) {
			// 次の描画要求が必要かどうか
			boolean hasNextMessage = true;
			// 次に表示する文字
			char ch = (mNextAppendIx < mFirstLine.length()) ? mFirstLine.charAt(mNextAppendIx) : EMPTY_CHAR;
			mNextAppendIx++;
			// 行末かどうか
			final boolean lineEnd = mNextAppendIx == mFirstLine.length();
			if ((ch == EMPTY_CHAR) && (mNextAppendIx == mFirstLine.length() + 1)) {
				// 行末+1文字目で行送りする
				mFirstLine = "";
				mNextAppendIx = -1;
				if (!mLines.isEmpty()) {
					lineFeed();
					popFirstIfNeed();
				}
			}
			if (ch != EMPTY_CHAR) {
				mMessage[mNextMessageColIx] = ch;
				mNextMessageColIx++;
			}
			if (mFirstLine.isEmpty() && mLines.isEmpty()) {
				if (DEBUG) Log.v(TAG, "updateMessage:end of message");
				// 最後までメッセージが表示されたとき
				if (mActionOnMessageEnd == Integer.MAX_VALUE) {
					// メッセー表示完了時のアクションが不明ならイベントリスナーからの取得を試みる
					mActionOnMessageEnd = mEventListener != null ? mEventListener.onMessageEnd(this) : 2000;
				}
				if (mActionOnMessageEnd >= 0) {
					// 指定時間後にクリアする場合
					mNextAppendIx = -1;
					hasNextMessage = false;
					mNextInvalidateMs = System.currentTimeMillis() + mActionOnMessageEnd;
					postDelayed(mClearTask, mActionOnMessageEnd);
					mActionOnMessageEnd = Integer.MAX_VALUE;
				} else {
					// すべての表示が無くなるまで行送りする場合
					mVisibleRows = mRows - 1;
				}
			}
			if (hasNextMessage) {
//				if (DEBUG) Log.v(TAG, "updateMessage:postInvalidateDelayed");
				// 行末かどうかで次に描画するまでの時間を調整する
				final long durationMs = lineEnd ? mDrawDurationMsPerLine : mDrawDurationMsPerChar;
				mNextInvalidateMs = System.currentTimeMillis() + durationMs;
				postInvalidateDelayed(durationMs);
			}
		} else if (mActionOnMessageEnd < 0) {
			// メッセージ無くなるまで行送りのためにスペースを追加するとき
			if (DEBUG) Log.v(TAG, "updateMessage:空行送り," + mVisibleRows);
			final long durationMs = mDrawDurationMsPerLine;
			mNextInvalidateMs = System.currentTimeMillis() + durationMs;
			if (mVisibleRows > 0) {
				lineFeed();
				mVisibleRows--;
				postInvalidateDelayed(durationMs);
			} else {
				postDelayed(mClearTask, durationMs);
			}
		}
	}

	/**
	 * 表示用文字配列を1行分繰り上げる
	 */
	private void lineFeed() {
		if (DEBUG) Log.v(TAG, "lineFeed:");
		final int len = mCols * mRows;
		// 1行分前にずらす
		System.arraycopy(mMessage, 0, mMessage, mCols, (len - mCols));
		Arrays.fill(mMessage, 0, mCols, EMPTY_CHAR);
	}

	/**
	 * 文字列を行に分割する
	 * ・改行があれば分割
	 * ・mColsより長い文字列も分割
	 * @param text
	 * @return
	 */
	@NonNull
	private List<String> splitLines(@Nullable final String text) {
		final List<String> result = new ArrayList<>();

		if (!TextUtils.isEmpty(text)) {
			final String[] splits = text.split(CR_STR);
			for (int i = 0; i < splits.length; i++) {
				String s = splits[i];
				while (!s.isEmpty()) {
					if (s.length() <= mCols) {
						// 横幅より短ければそのまま追加
						result.add(s);
						break;
					} else {
						// 横幅より長いときは横幅毎に追加
						final int ix = Math.min(s.length(), mCols);
						result.add(s.substring(0, ix));
						s = s.substring(ix);
					}
				}
			}
		}
		if (DEBUG) Log.v(TAG, "splitLines:" + result);

		return result;
	}
}
