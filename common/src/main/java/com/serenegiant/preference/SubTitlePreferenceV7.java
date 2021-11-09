package com.serenegiant.preference;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.utils.TypedArrayUtils;
import com.serenegiant.view.ViewUtils;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public final class SubTitlePreferenceV7 extends Preference {
	private static final boolean DEBUG = false;
	private static final String TAG = SubTitlePreferenceV7.class.getSimpleName();

	@LayoutRes
	private final int mSubTitleLayoutId;
	@IdRes
	private final int mSubTitleTvId;
	private CharSequence mSubTitle;
	private TextView mSubTitleTextView;
	@ViewUtils.Visibility
	private int mSubTitleVisibility;

	// setWidgetLayoutResource()はPreference画面のpreferenceリストの右側に表示されるView
	// (例えばチェックボックスとか)のレイアウトを差し替えるときに使う
	// preference全体を変えてしまう時は、onCreateViewで必要なViewを生成する
	//			setWidgetLayoutResource(R.layout.seekbar_preference);

	public SubTitlePreferenceV7(final Context context) {
		this(context, null);
	}

	public SubTitlePreferenceV7(final Context context, final AttributeSet attrs) {
		this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle,
			android.R.attr.preferenceStyle));
	}

	public SubTitlePreferenceV7(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		TypedArray attribs = context.getTheme().obtainStyledAttributes(
			attrs, R.styleable.SubTitlePreference, defStyle, 0);
		mSubTitleLayoutId = attribs.getResourceId(R.styleable.SubTitlePreference_subtitle_layout, R.layout.subtitle);
		mSubTitleTvId = attribs.getResourceId(R.styleable.SubTitlePreference_subtitle_id, R.id.subtitle);
		mSubTitle = attribs.getString(R.styleable.SubTitlePreference_subtitle);
		attribs.recycle();
	}

	@Override
	public void onBindViewHolder(final PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
//		if (DEBUG) Log.w(TAG, "onBindView:");
		if (mSubTitleLayoutId == 0) return;
		RelativeLayout parent = null;
		final ViewGroup group;
		if (holder.itemView instanceof ViewGroup) {
			group = (ViewGroup)holder.itemView;
			for (int i = group.getChildCount() - 1; i >= 0; i--) {
				final View v = group.getChildAt(i);
				if (v instanceof RelativeLayout) {
					parent = (RelativeLayout)v;
					break;
				}
			}
		} else {
			group = null;
		}
		if (parent == null) return;	// これにかかることはないはず
		// サブタイトル用のレイアウトを生成する
        final LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        final View extraview = layoutInflater.inflate(mSubTitleLayoutId, group, false);
        if (extraview != null) {
			// titleとsummaryの間に挿入する
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.BELOW, android.R.id.title);
			parent.addView(extraview, params);
			final View summary = parent.findViewById(android.R.id.summary);
			if (summary != null) {
				// summaryをサブタイトルの下に移動する
				// XXX extraviewがViewGroupでsubtitle以外が含まれている場合にはextraviewにidを振らないといけない
				int id = extraview.getId();
				if (id == 0) {
					id = mSubTitleTvId;
				}
				params = (RelativeLayout.LayoutParams)summary.getLayoutParams();
				params.addRule(RelativeLayout.BELOW, id);
				summary.setLayoutParams(params);
			}

			mSubTitleTextView = extraview.findViewById(mSubTitleTvId);
			setSubtitle(mSubTitle);
		}
	}

	@UiThread
	public void setSubtitle(@StringRes final int subtitleId) {
		if (mSubTitleTextView != null) {
			mSubTitleTextView.setText(subtitleId);
			mSubTitle = mSubTitleTextView.getText();
			mSubTitleTextView.setVisibility(TextUtils.isEmpty(mSubTitle) ? View.GONE : mSubTitleVisibility);
		}
	}

	@UiThread
	public void setSubtitle(final CharSequence subtitle) {
		mSubTitle = subtitle;
		if (mSubTitleTextView != null) {
			mSubTitleTextView.setText(subtitle);
			mSubTitleTextView.setVisibility(TextUtils.isEmpty(mSubTitle) ? View.GONE : mSubTitleVisibility);
		}
	}

	@UiThread
	public void setSubTitleVisibility(@ViewUtils.Visibility final int visibility) {
		mSubTitleVisibility = visibility;
		if (mSubTitleTextView != null) {
			mSubTitleTextView.setVisibility(visibility);
		}
	}
}
