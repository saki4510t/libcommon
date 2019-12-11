package com.serenegiant.glutils;

import android.opengl.Matrix;

import static com.serenegiant.glutils.IRendererCommon.*;

public class GLUtils {
	private GLUtils() {
		// インスタンス化を防ぐためにデフォルトコンストラクタをprivateに
	}

	public static void setMirror(final float[] mvp, final int mirror) {
		switch (mirror) {
		case MIRROR_NORMAL:
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = Math.abs(mvp[5]);
			break;
		case MIRROR_HORIZONTAL:
			mvp[0] = -Math.abs(mvp[0]);	// flip left-right
			mvp[5] = Math.abs(mvp[5]);
			break;
		case MIRROR_VERTICAL:
			mvp[0] = Math.abs(mvp[0]);
			mvp[5] = -Math.abs(mvp[5]);	// flip up-side down
			break;
		case MIRROR_BOTH:
			mvp[0] = -Math.abs(mvp[0]);	// flip left-right
			mvp[5] = -Math.abs(mvp[5]);	// flip up-side down
			break;
		}
	}

	/**
	 * 現在のモデルビュー変換行列をxy平面で指定した角度回転させる
	 * @param mvp
	 * @param degrees
	 */
	public static void rotate(final float[] mvp, final int degrees) {
		if ((degrees % 180) != 0) {
			Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
		}
	}

	/**
	 * モデルビュー変換行列にxy平面で指定した角度回転させた回転行列をセットする
	 * @param mvp
	 * @param degrees
	 */
	public static void setRotation(final float[] mvp, final int degrees) {
		Matrix.setIdentityM(mvp, 0);
		if ((degrees % 180) != 0) {
			Matrix.rotateM(mvp, 0, degrees, 0.0f, 0.0f, 1.0f);
		}
	}
}
