package com.serenegiant.view;

import android.view.View;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class ViewUtils {

	@IntDef({
		View.VISIBLE,
		View.INVISIBLE,
		View.GONE,
	})
	@Retention(SOURCE)
	public @interface Visibility {}

}
