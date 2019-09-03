package com.serenegiant.libcommon;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

public class BaseFragment extends Fragment {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = BaseFragment.class.getSimpleName();

	protected final Object mSync = new Object();

	public BaseFragment() {
		super();
	}

	@Override
	public void onAttach(@NonNull final Context context) {
		super.onAttach(context);
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG + ":async");
	}
	
	@Override
	public final void onStart() {
		super.onStart();
		if (BuildCheck.isAndroid7()) {
			internalOnResume();
		}
	}

	@Override
	public final void onResume() {
		super.onResume();
		if (!BuildCheck.isAndroid7()) {
			internalOnResume();
		}
	}

	@Override
	public final void onPause() {
		if (!BuildCheck.isAndroid7()) {
			internalOnPause();
		}
		super.onPause();
	}

	@Override
	public final void onStop() {
		if (BuildCheck.isAndroid7()) {
			internalOnPause();
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		internalRelease();
		super.onDestroy();
	}
	
	/**
	 * actual methods of #onResume (for before Android 7)
	 * #onStart(for Android 7 and later)
	 * never call #onResume, #onStart in this method otherwise it raise infinite loop.
	 * if you need, pls override this instead of #onResume/#onStart
	 */
	protected void internalOnResume() {
	}

	/**
	 * actual methods of #onPause (for before Android 7)
	 * #onStop(for Android 7 and later)
	 * never call #onResume, #onStart in this method otherwise it raise infinite loop.
	 * if you need, pls override this instead of #onPause/#onStop
	 */
	protected void internalOnPause() {
		clearToast();
	}

	protected void internalRelease() {
		mUIHandler.removeCallbacksAndMessages(null);
		if (mAsyncHandler != null) {
			mAsyncHandler.removeCallbacksAndMessages(null);
			try {
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			mAsyncHandler = null;
		}
	}
	
	/**
	 * helper method to handle back key for Fragment
	 * because originally there are no back key event on Fragment.
	 * @return true: handled
	 */
	public boolean onBackPressed() {
		return false;
	}

	protected void popBackStack() {
		final Activity activity = getActivity();
		if ((activity == null) || activity.isFinishing()) return;
		try {
			getFragmentManager().popBackStack();
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

//================================================================================
	/** Handler for UI operation  */
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private final Thread mUiThread = mUIHandler.getLooper().getThread();
	private Handler mAsyncHandler;

	/**
	 * helper method to run specific task on UI thread
	 * @param task
	 */
	public final void runOnUiThread(@NonNull final Runnable task) {
		runOnUiThread(task, 0L);
	}

	/**
	 * helper method to run specific task on UI thread
	 * @param task
	 * @param duration
	 */
	public final void runOnUiThread(@NonNull final Runnable task, final long duration) {
		mUIHandler.removeCallbacks(task);
		if ((duration > 0) || Thread.currentThread() != mUiThread) {
			mUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * remove specific task from UI thread if it is not run yet.
	 * @param task
	 */
	public final void removeFromUiThread(final Runnable task) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
	}
	
	/**
	 * remove all pending task from UI thread
	 */
	public final void removeFromUiThreadAll() {
		mUIHandler.removeCallbacksAndMessages(null);
	}

	/**
	 * helper method to run specific Runnable on worker thread
	 * if same Runnable is waiting to execute, it will removed.
	 * @param task
	 */
	protected void queueEvent(final Runnable task) {
		queueEvent(task, 0);
	}
	
	/**
	 * helper method to run specific Runnable on worker thread
	 * if same Runnable is waiting to execute, it will removed.
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	protected void queueEvent(final Runnable task, final long delay)
		throws IllegalStateException {
		
		if (task == null) return;
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
				if (delay > 0) {
					mAsyncHandler.postDelayed(task, delay);
				} else {
					mAsyncHandler.post(task);
				}
			} else {
				throw new IllegalStateException("worker thread is not ready");
			}
		}
	}

	/**
	 * helper method to remove specific Runnable from worker thread if it does not run yet.
	 * @param task
	 */
	protected void removeEvent(final Runnable task) {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
			}
		}
	}

	/**
	 * helper method to remove all pending Runnable/message from worker thread
	 */
	protected void removeEventAll() {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(null);
			}
		}
	}

//================================================================================
	@NonNull
	protected LayoutInflater getThemedLayoutInflater(
		@NonNull final LayoutInflater inflater, @StyleRes final int layout_style) {

		if (DEBUG) Log.v(TAG, "getThemedLayoutInflater:");
		final Activity context = getActivity();
		// create ContextThemeWrapper from the original Activity Context with the custom theme
		final Context contextThemeWrapper = new ContextThemeWrapper(context, layout_style);
		// clone the inflater using the ContextThemeWrapper
		return inflater.cloneInContext(contextThemeWrapper);
	}

//================================================================================
	private Toast mToast;

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 * @param args
	 */
	protected void showToast(final int duration, final String msg, final Object... args) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (mToast != null) {
						mToast.cancel();
						mToast = null;
					}
					final String _msg = args != null ? String.format(msg, args) : msg;
					mToast = Toast.makeText(getActivity(), _msg, duration);
					mToast.show();
				} catch (final Exception e) {
					// ignore
				}
			}
		}, 0);
	}

	/**
	 * Toastでメッセージを表示
	 * @param msg
	 */
	protected void showToast(final int duration, @StringRes final int msg, final Object... args) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (mToast != null) {
						mToast.cancel();
						mToast = null;
					}
					final String _msg = args != null ? getString(msg, args) : getString(msg);
					mToast = Toast.makeText(getActivity(), _msg, duration);
					mToast.show();
				} catch (final Exception e) {
					// ignore
				}
			}
		}, 0);
	}

	/**
	 * Toastが表示されていればキャンセルする
	 */
	protected void clearToast() {
		try {
			if (mToast != null) {
				mToast.cancel();
				mToast = null;
			}
		} catch (final Exception e) {
			// ignore
		}
	}
}
