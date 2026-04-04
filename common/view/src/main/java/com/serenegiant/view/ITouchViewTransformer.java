package com.serenegiant.view;

import android.view.View;
import android.view.ViewConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

public interface ITouchViewTransformer {
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
	public static final float MIN_DISTANCE_DP = 16;
	/**
	 * 平行移動時に表示内容がView外へ出てしまうのを防ぐための制限値
	 */
	public static final float MOVE_LIMIT_RATE = 0.2f;	// =Viewの幅/高さのそれぞれ20%
	/**
	 * マルチタッチ時に回転処理へ遷移するまでの待機時間[ミリ秒]/シングルロングタッチでリセットするまでの待機時間[ミリ秒]
	 */
	public static final int CHECK_TIMEOUT
		= ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout();

	/**
	 * callback listener called when rotation started.
	 */
	public interface ViewTransformListener<T> {
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
		public void onTransformed(@NonNull final View view, @NonNull final T transform);
	}
}
