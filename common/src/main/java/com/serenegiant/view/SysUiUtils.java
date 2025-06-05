package com.serenegiant.view;
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
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.serenegiant.system.BuildCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;

/**
 * 全画面表示等の処理を行うためのヘルパークラス
 * androidxのWindowInsetsControllerCompatを使わない時用
 * 画面最上段の時計等が表示されている領域がステータスバー
 * タブレットで最下段に表示されるのがシステムバー(Android4.x?)
 * ハードウエアキーの無いスマホでホームボタンやバックキー等が表示されるのがナビゲーションバー
 * 上3つを総称してシステムバー(システムUI)
 */
public class SysUiUtils implements DefaultLifecycleObserver {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SysUiUtils.class.getSimpleName();

	@SuppressLint("InlinedApi")
	@IntDef({
		ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
		ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
		ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
		ActivityInfo.SCREEN_ORIENTATION_USER,
		ActivityInfo.SCREEN_ORIENTATION_BEHIND,
		ActivityInfo.SCREEN_ORIENTATION_SENSOR,
		ActivityInfo.SCREEN_ORIENTATION_NOSENSOR,
		ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
		ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
		ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
		ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
		ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
		ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,	// API>=18
		ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, // API>=18
		ActivityInfo.SCREEN_ORIENTATION_FULL_USER, // API>=18
		ActivityInfo.SCREEN_ORIENTATION_LOCKED // API>=18
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ScreenOrientation {}

	private static final int FULLSCREEN_FLAGS = getFullscreenFlags();

	private static final int getFullscreenFlags() {
		int flags =View.SYSTEM_UI_FLAG_LAYOUT_STABLE			// IMEやキーボードの影響を受けないようにレイアウトする
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION	// ナビゲーションバーの高さを無視してレイアウトする
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN			// ステータスバーの高さを無視してレイアウトする
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION			// ナビゲーションバーを隠す
				| View.SYSTEM_UI_FLAG_FULLSCREEN				// ステータスバーを隠す
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		return flags;
	}

	@NonNull
	private final WeakReference<Activity> mWeakActivity;
	/** 画面の向きを一時的に固定中かどうか, trueなら一時固定 */
	private boolean screenOrientationFixed;
	/**
	 * 没入モード(=ユーザー操作があってもシステムバーを隠したまま)にするかどうか
	 * Android4.4, API>=19以降
	 */
	private boolean mUseImmersiveMode = false;
	/**
	 * 没入モードにしてユーザー操作によって一時的にシステムバーが表示された時に
	 * 一定時間後に再度非表示に戻すかどうか
	 * Android4.4, API>=19以降
	 */
	private boolean mUseImmersiveSticky = true;

	public SysUiUtils(@NonNull final Activity activity) {
		mWeakActivity = new WeakReference<>(activity);
	}

	/**
	 * 画面の向きを一時的に固定するかどうか
	 * @param fixed
	 */
	public void  fixedScreenOrientation(final boolean fixed) {
		screenOrientationFixed = fixed;
	}

	/**
	 * 画面の回転制御を適用
	 * @param screenOrientation
	 */
	public void requestScreenRotation(@ScreenOrientation final int screenOrientation) {
		if (screenOrientationFixed && BuildCheck.isJellyBeanMR2()) {
			// 現在の向きに固定
			requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
		} else {
			if (DEBUG) Log.v(TAG, String.format("requestScreenRotation:orientation=%d", screenOrientation));
			requireActivity().setRequestedOrientation(screenOrientation);
		}
	}

	/**
	 * システムUIを隠してフルスクリーン表示にする
	 */
	public void hideSystemUi() {
		if (DEBUG) Log.v(TAG, "hideSystemUi:");
		requireActivity().getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_FLAGS);
	}

	/**
	 * Immersive Modeの有効/無効をセット
	 * このメソッドは以前と状態が変化した時のみ実際の処理を行う。
	 * 実際の処理は#internalSetSystemUIVisibilityで行う。
	 * @param immersive
	 */
	public void setImmersiveMode(final boolean immersive) {
		if (DEBUG) Log.v(TAG, "setImmersiveMode:immersive=" + immersive);
		if (mUseImmersiveMode != immersive) {
			mUseImmersiveMode = immersive;
			internalSetSystemUIVisibility(!immersive, immersive, mUseImmersiveSticky);
		}
	}

	/**
	 * sticky mode付きのImmersive Modeにするかどうかをセット
	 * このメソッドは以前と状態が変化した時のみ実際の処理を行う。
	 * 実際の処理は#internalSetSystemUIVisibilityで行う。
	 * @param immersive
	 * @param sticky
	 */
	public void setImmersiveMode(final boolean immersive, final boolean sticky) {
		if (DEBUG) Log.v(TAG, "setImmersiveMode:immersive=" + immersive + ", sticky=" + sticky);
		if ((mUseImmersiveMode != immersive) || (mUseImmersiveSticky != sticky)) {
			internalSetSystemUIVisibility(!immersive, immersive, sticky);
		}
	}

	/**
	 * system UIを表示するかどうかをセット
	 * 実際の処理は#internalSetSystemUIVisibilityで行う。
	 * @param visible
	 */
	public void setSystemUIVisibility(final boolean visible) {
		if (DEBUG) Log.v(TAG, "setSystemUIVisibility:visible=" + visible);
		internalSetSystemUIVisibility(visible, mUseImmersiveMode, mUseImmersiveSticky);
	}

	/**
	 * システムバーを表示するかをセット
	 * @param visible
	 * @param immersive
	 * @param sticky
	 */
	private void internalSetSystemUIVisibility(final boolean visible,
		final boolean immersive, final boolean sticky) {

		if (DEBUG) Log.v(TAG, "internalSetSystemUIVisibility:");
		mUseImmersiveMode = immersive;
		mUseImmersiveSticky = sticky;
		final Activity activity = requireActivity();
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (BuildCheck.isKitKat()) {
						// Android4.x以降, API>=19
						setSystemUIVisibilityKitkat(visible);
					} else if (BuildCheck.isJellyBean()) {
						// Android4.1.2以降, API>=16
						setSystemUIVisibilityJellyBean(visible);
					} else if (BuildCheck.isIcecreamSandwich()) {
						// Android4.x以降, API>=14
						setSystemUIVisibilityIcecreamSandwich(visible);
					} else if (visible) {
						// Android4未満でシステムバー表示する場合(通常)
						activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					} else {
						// Android4未満でシステムバー非表示の場合(全画面表示)
						activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
					}
				} catch (final Exception e) {
					// ignore
				}
			}
		});
	}

	/**
	 * 全画面表示を切り替えるためのフラグ
	 * ＠RequiresApi(Build.VERSION_CODES.JELLY_BEAN)はフィールドに対してはセット出来ないので
	 * getFlagsFullscreenスタティックメソッドで割り当てる
	 */
	private static final int FLAGS_FULLSCREEN = getFlagsFullscreen();

	private static final int getFlagsFullscreen() {
		return View.SYSTEM_UI_FLAG_LAYOUT_STABLE		// IMEやキーボードの影響を受けないようにレイアウトする
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION// ナビゲーションバーの高さを無視してレイアウトする
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN		// ステータスバーの高さを無視してレイアウトする
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION		// ナビゲーションバーを隠す
			| View.SYSTEM_UI_FLAG_FULLSCREEN;			// ステータスバーを隠す
	}

	/**
	 * Android4.0以降, APIレベル14以降(ICS)の場合のシステムバーの表示切替処理
	 * SYSTEM_UI_FLAG_FULLSCREEN:
	 * 		ステータスバーを隠す
	 * 		SYSTEM_UI_FLAG_LOW_PROFILE や SYSTEM_UI_FLAG_HIDE_NAVIGATIONと組み合わせると
	 * 		画面操作した時にナビゲーションバーと同時に表示される
	 * 		単体指定なら常時ステータスバー非表示
	 * SYSTEM_UI_FLAG_HIDE_NAVIGATION:
	 * 		ナビゲーションバーを非表示
	 * @param visible
	 */
	private void setSystemUIVisibilityIcecreamSandwich(final boolean visible) {
		final View decorView = requireActivity().getWindow().getDecorView();
		int flag = View.SYSTEM_UI_FLAG_VISIBLE;			// システムバーをすべて表示(=0, デフォルト)
		if (!visible) {
			flag = View.SYSTEM_UI_FLAG_LOW_PROFILE		// システムバーを暗くする
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;	// ナビゲーションバーを隠す
		}
		decorView.setSystemUiVisibility(flag);
	}

	/**
	 * Android4.1.2以降, APIレベル16以降(JellyBean)の場合のsystem UIの表示切替処理
	 * @param visible
	 */
	private void setSystemUIVisibilityJellyBean(final boolean visible) {
		final View decorView = requireActivity().getWindow().getDecorView();
		int flag = decorView.getSystemUiVisibility();
		if (visible) {
			flag &= ~FLAGS_FULLSCREEN;
		} else {
			flag = FLAGS_FULLSCREEN;
		}
		decorView.setSystemUiVisibility(flag);
	}

	/**
	 * Android4.4.x, APIレベル19以降(Kitkat)の場合のsystem UIの表示切替処理
	 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY:
	 * 	ユーザー操作でシステムバーが表示されても一定時間後に非表示に戻す
	 * SYSTEM_UI_FLAG_IMMERSIVE:
	 * 	画面ユーザー操作でシステムバーが表示された時に非表示には戻さない
	 * @param visible
	 */
	private void setSystemUIVisibilityKitkat(final boolean visible) {
		final View decorView = requireActivity().getWindow().getDecorView();
		int flag = decorView.getSystemUiVisibility();
		if (visible) {
			flag &= ~FLAGS_FULLSCREEN;
			flag &= ~(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_IMMERSIVE);
		} else {
			flag = FLAGS_FULLSCREEN;
			flag |= (mUseImmersiveSticky ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				: (mUseImmersiveMode ? View.SYSTEM_UI_FLAG_IMMERSIVE : 0));
		}
		decorView.setSystemUiVisibility(flag);
	}

	@NonNull
	private Activity requireActivity() throws IllegalStateException {
		final Activity activity = mWeakActivity.get();
		if (activity == null) {
			throw new IllegalStateException("already released!");
		}
		return activity;
	}
}
