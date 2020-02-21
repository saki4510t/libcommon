package com.serenegiant.view;

import android.view.View;

import com.serenegiant.widget.IScaledView;

import androidx.annotation.NonNull;

import static com.serenegiant.widget.IScaledView.SCALE_MODE_KEEP_ASPECT;

public class MeasureSpecDelegater {
	public static class MeasureSpec {
		public int widthMeasureSpec;
		public int heightMeasureSpec;

		private MeasureSpec(final int widthMeasureSpec, final int heightMeasureSpec) {
			this.widthMeasureSpec = widthMeasureSpec;
			this.heightMeasureSpec = heightMeasureSpec;
		}
	}

	public static MeasureSpec onMeasure(@NonNull final View target,
		final double requestedAspect,
		@IScaledView.ScaleMode final int scaleMode,
		final boolean needResizeToKeepAspect,
		final int widthMeasureSpec, final int heightMeasureSpec) {

		final MeasureSpec result = new MeasureSpec(widthMeasureSpec, heightMeasureSpec);
//		if (DEBUG) Log.v(TAG, "onMeasure:requestedAspect=" + requestedAspect);
// 		要求されたアスペクト比が負の時(初期生成時)は何もしない
		if ((requestedAspect > 0)
			&& (scaleMode == SCALE_MODE_KEEP_ASPECT)
			&& needResizeToKeepAspect) {

			int initialWidth = View.MeasureSpec.getSize(widthMeasureSpec);
			int initialHeight = View.MeasureSpec.getSize(heightMeasureSpec);
			final int horizPadding = target.getPaddingLeft() + target.getPaddingRight();
			final int vertPadding = target.getPaddingTop() + target.getPaddingBottom();
			initialWidth -= horizPadding;
			initialHeight -= vertPadding;

			final double viewAspectRatio = (double)initialWidth / initialHeight;
			final double aspectDiff = requestedAspect / viewAspectRatio - 1;

			// 計算誤差が生じる可能性が有るので指定した値との差が小さければそのままにする
			if (Math.abs(aspectDiff) > 0.005) {
				if (aspectDiff > 0) {
					// 幅基準で高さを決める
					initialHeight = (int) (initialWidth / requestedAspect);
				} else {
					// 高さ基準で幅を決める
					initialWidth = (int) (initialHeight * requestedAspect);
				}
				initialWidth += horizPadding;
				initialHeight += vertPadding;
				result.widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialWidth, View.MeasureSpec.EXACTLY);
				result.heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialHeight, View.MeasureSpec.EXACTLY);
			}
		}

		return result;
	}
}
