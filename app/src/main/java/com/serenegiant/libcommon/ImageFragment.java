package com.serenegiant.libcommon;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.mediastore.ImageLoader;
import com.serenegiant.mediastore.LoaderDrawable;
import com.serenegiant.mediastore.MediaInfo;
import com.serenegiant.widget.ZoomImageView;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * 静止画表示用のFragment
 * FIXME MainFragmentで選択したのと違う映像が読み込まれる！！
 */
public class ImageFragment extends BaseFragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = ImageFragment.class.getSimpleName();

	private static final boolean USU_LOADER_DRAWABLE = true;
	private static final String ARG_MEDIA_INFO = "ARG_MEDIA_INFO";

	public static ImageFragment newInstance(@NonNull final MediaInfo info) {
		final ImageFragment fragment = new ImageFragment();

		final Bundle args = new Bundle();
		args.putParcelable(ARG_MEDIA_INFO, info);
		fragment.setArguments(args);

		return fragment;
	}

//--------------------------------------------------------------------------------
	private MediaInfo mInfo;
	private View mRootView;
	private ZoomImageView mImageView;

	/**
	 * コンストラクタ
	 */
	public ImageFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
		final ViewGroup container, final Bundle savedInstanceState) {

		if (DEBUG) Log.v(TAG, "onCreateView:");
		Bundle args = savedInstanceState;
		if (args == null) {
			args = getArguments();
		}
		if (args != null) {
			mInfo = args.getParcelable(ARG_MEDIA_INFO);
		}
		mRootView = inflater.inflate(R.layout.fragment_image, container, false);
		initView(mRootView);
		return mRootView;
	}

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");
		final Bundle args = getArguments();
		if (args != null) {
			outState.putAll(args);
		}
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		super.onDestroy();
	}

//--------------------------------------------------------------------------------
	private void initView(@NonNull final View rootView) {
		if (DEBUG) Log.v(TAG, "initView:" + mInfo.getUri());
		mImageView = rootView.findViewById(R.id.image_view);
		mImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (DEBUG) Log.v(TAG, "onClick:" + v);
			}
		});
		if (USU_LOADER_DRAWABLE) {
			Drawable drawable = mImageView.getDrawable();
			if (!(drawable instanceof LoaderDrawable)) {
				drawable = new ImageLoaderDrawable(
					requireContext().getContentResolver(), -1, -1);
				mImageView.setImageDrawable(drawable);
			}
			((LoaderDrawable)drawable).startLoad(mInfo.mediaType, 0, mInfo.id);
		} else {
			mImageView.setImageURI(mInfo.getUri());
		}
	}

//--------------------------------------------------------------------------------
	private static class ImageLoaderDrawable extends LoaderDrawable {

		public ImageLoaderDrawable(final ContentResolver cr,
			final int width, final int height) {

			super(cr, width, height);
		}

		@Override
		protected ImageLoader createImageLoader() {
			return new MyImageLoader(this);
		}

		@Override
		protected Bitmap checkCache(final int groupId, final long id) {
			return null;
		}

		@Override
		protected void onBoundsChange(final Rect bounds) {
			super.onBoundsChange(bounds);
		}

	}

	private static class MyImageLoader extends ImageLoader {
		public MyImageLoader(final ImageLoaderDrawable parent) {
			super(parent);
		}

		@Override
		protected Bitmap loadBitmap(@NonNull final ContentResolver cr,
			final int mediaType, final int groupId, final long id,
			final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				result = BitmapHelper.asBitmap(cr, id, 0, 0);
				if (result != null) {
					final int w = result.getWidth();
					final int h = result.getHeight();
					final Rect bounds = new Rect();
					mParent.copyBounds(bounds);
					final int cx = bounds.centerX();
					final int cy = bounds.centerY();
					bounds.set(cx - w / 2, cy - h / w, cx + w / 2, cy + h / 2);
					((ImageLoaderDrawable)mParent).onBoundsChange(bounds);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			return result;
		}
	}

}
