package com.serenegiant.libcommon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.serenegiant.dialog.MessageDialogFragmentV4;
import com.serenegiant.libcommon.list.DummyContent;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.PermissionCheck;

public class MainActivity extends AppCompatActivity
	implements TitleFragment.OnListFragmentInteractionListener,
		MessageDialogFragmentV4.MessageDialogListener {

	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private boolean mIsResumed;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DummyContent.createItems(this, R.array.list_items);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, TitleFragment.newInstance(1))
				.commit();
		}
	}
	
	@Override
	protected final void onStart() {
		super.onStart();
		if (BuildCheck.isAndroid7()) {
			mIsResumed = true;
			internalOnResume();
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
		if (!BuildCheck.isAndroid7()) {
			mIsResumed = true;
			internalOnResume();
		}
	}

	@Override
	protected final void onPause() {
		if (!BuildCheck.isAndroid7()) {
			internalOnPause();
			mIsResumed = false;
		}
		super.onPause();
	}

	@Override
	protected final void onStop() {
		if (BuildCheck.isAndroid7()) {
			internalOnPause();
			mIsResumed = false;
		}
		super.onStop();
	}

	protected void internalOnResume() {
	}

	protected void internalOnPause() {
		clearToast();
	}

	/**
	 * helper method to get this Activity is paused or not
	 * @return true: this Activity is paused, false: resumed
	 */
	public boolean isPaused() {
		return !mIsResumed;
	}
	
	@Override
	public void onBackPressed() {
		if (DEBUG) Log.v(TAG, "onBackPressed:");
		// Fragment内の子Fragmentを切り替えた時にbackキーを押すと
		// Fragment自体がpopBackされてしまうのに対するworkaround
		final FragmentManager fm = getSupportFragmentManager();
		final Fragment fragment = fm.findFragmentById(R.id.container);
		if (fragment instanceof BaseFragment) {
			final FragmentManager child_fm = fragment.getChildFragmentManager();
			if (child_fm.getBackStackEntryCount() > 0) {
				// HomeFragmentの子Fragmentがバックスタックに有る時はそれをpopBackする
				child_fm.popBackStack();
				return;
			}
			if (((BaseFragment) fragment).onBackPressed()) {
				return;
			}
		}
		super.onBackPressed();
	}

	@Override
	public void onListFragmentInteraction(final DummyContent.DummyItem item) {
		if (DEBUG) Log.v(TAG, "onListFragmentInteraction:" + item);
		
		Fragment fragment = null;
		switch (item.id) {
		case 0:
			fragment = NetworkConnectionFragment.newInstance();
			break;
		case 1:
			if (!checkPermissionCamera()
				|| !checkPermissionWriteExternalStorage()
				|| !checkPermissionAudio()) {

				return;
			}
			fragment = CameraFragment.newInstance();
			break;
		case 2:
			if (BuildCheck.isAndroid9()
				&& !checkPermissionCamera()) {

				return;
			}
			fragment = UsbFragment.newInstance();
			break;
		default:
			break;
		}
		if (fragment != null) {
			getSupportFragmentManager()
				.beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, fragment)
				.commit();
		}
	}
//================================================================================
	
	/**
	 * callback listener from MessageDialogFragment
	 *
	 * @param dialog
	 * @param requestCode
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	@Override
	public void onMessageDialogResult(
		@NonNull final MessageDialogFragmentV4 dialog, final int requestCode,
		@NonNull final String[] permissions, final boolean result) {
		
		if (result) {
			// メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode);
				return;
			}
		}
		// メッセージダイアログでキャンセルされた時とAndroid6でない時は自前でチェックして#checkPermissionResultを呼び出す
		for (final String permission : permissions) {
			checkPermissionResult(requestCode, permission,
				PermissionCheck.hasPermission(this, permission));
		}
	}
	
	/**
	 * override this method to handle result of permission request
	 * actual handling of requesting permission is delegated on #checkPermissionResult
	 *
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(final int requestCode,
		@NonNull final String[] permissions, @NonNull final int[] grantResults) {
		
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);    // 何もしてないけど一応呼んどく
		final int n = Math.min(permissions.length, grantResults.length);
		for (int i = 0; i < n; i++) {
			checkPermissionResult(requestCode, permissions[i],
				grantResults[i] == PackageManager.PERMISSION_GRANTED);
		}
	}
	
	/**
	 * actual handling of requesting permission
	 * this method just show Toast if permission request failed
	 *
	 * @param requestCode
	 * @param permission
	 * @param result
	 */
	protected void checkPermissionResult(final int requestCode,
		final String permission, final boolean result) {
		
		// パーミッションがないときにはメッセージを表示する
		if (!result && (permission != null)) {
			if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_audio);
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_ext_storage);
			}
			if (Manifest.permission.CAMERA.equals(permission)) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_camera);
			}
			if (Manifest.permission.INTERNET.equals(permission)) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_network);
			}
			if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
				showToast(Toast.LENGTH_SHORT, R.string.permission_location);
			}
		}
	}
	
	private static final int ID_PERMISSION_REASON_AUDIO = R.string.permission_audio_recording_reason;
	private static final int ID_PERMISSION_REQUEST_AUDIO = R.string.permission_audio_recording_request;
	private static final int ID_PERMISSION_REASON_NETWORK = R.string.permission_network_reason;
	private static final int ID_PERMISSION_REQUEST_NETWORK = R.string.permission_network_request;
	private static final int ID_PERMISSION_REASON_EXT_STORAGE = R.string.permission_ext_storage_reason;
	private static final int ID_PERMISSION_REQUEST_EXT_STORAGE = R.string.permission_ext_storage_request;
	private static final int ID_PERMISSION_REASON_CAMERA = R.string.permission_camera_reason;
	private static final int ID_PERMISSION_REQUEST_CAMERA = R.string.permission_camera_request;
	private static final int ID_PERMISSION_REQUEST_HARDWARE_ID = R.string.permission_hardware_id_request;
	private static final int ID_PERMISSION_REASON_LOCATION = R.string.permission_location_reason;
	private static final int ID_PERMISSION_REQUEST_LOCATION = R.string.permission_location_request;

	/** request code for WRITE_EXTERNAL_STORAGE permission */
	private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345;
	/** request code for RECORD_AUDIO permission */
	private static final int REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567;
	/** request code for CAMERA permission */
	private static final int REQUEST_PERMISSION_CAMERA = 0x345678;
	/** request code for INTERNET permission */
	private static final int REQUEST_PERMISSION_NETWORK = 0x456789;
	/** request code for READ_PHONE_STATE permission */
	private static final int REQUEST_PERMISSION_HARDWARE_ID = 0x567890;
    /** request code for ACCESS_FINE_LOCATION permission */
	private static final int REQUEST_PERMISSION_LOCATION = 0x678901;
	
	/**
	 * check permission to access external storage
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access external storage
	 */
	protected boolean checkPermissionWriteExternalStorage() {
		if (!PermissionCheck.hasWriteExternalStorage(this)) {
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
				R.string.permission_title, ID_PERMISSION_REQUEST_EXT_STORAGE,
				new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
			return false;
		}
		return true;
	}
	
	/**
	 * check permission to record audio
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to record audio
	 */
	protected boolean checkPermissionAudio() {
		if (!PermissionCheck.hasAudio(this)) {
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.permission_title, ID_PERMISSION_REQUEST_AUDIO,
				new String[]{Manifest.permission.RECORD_AUDIO});
			return false;
		}
		return true;
	}
	
	/**
	 * check permission to access internal camera
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access internal camera
	 */
	protected boolean checkPermissionCamera() {
		if (!PermissionCheck.hasCamera(this)) {
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_CAMERA,
				R.string.permission_title, ID_PERMISSION_REQUEST_CAMERA,
				new String[]{Manifest.permission.CAMERA});
			return false;
		}
		return true;
	}
	
	/**
	 * check permission to access network
	 * and request to show detail dialog to request permission
	 *
	 * @return true already have permission to access network
	 */
	protected boolean checkPermissionNetwork() {
		if (!PermissionCheck.hasNetwork(this)) {
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_NETWORK,
				R.string.permission_title, ID_PERMISSION_REQUEST_NETWORK,
				new String[]{Manifest.permission.INTERNET});
			return false;
		}
		return true;
	}

	/**
	 * check permission to access gps
	 * and request to show detail dialog to request permission
	 * @return true already have permission to access gps
	 */
	protected boolean checkPermissionLocation(){
		if (!PermissionCheck.hasAccessLocation(this)) {
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_LOCATION,
					R.string.permission_title, ID_PERMISSION_REQUEST_LOCATION,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION});
			return false;
		}
		return true;
	}
	/**
	 * check permission to of READ_PHONE_STATE
	 * and request to show detail dialog to request permission
	 * This permission is necessarily to get hardware ID on device like IMEI.
	 *
	 * @return true already have permission of READ_PHONE_STATE
	 */
	protected boolean checkPermissionHardwareId() {
		if (!PermissionCheck.hasPermission(this,
			Manifest.permission.READ_PHONE_STATE)) {
			
			MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_HARDWARE_ID,
				R.string.permission_title, ID_PERMISSION_REQUEST_HARDWARE_ID,
				new String[]{Manifest.permission.READ_PHONE_STATE});
			return false;
		}
		return true;
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
					mToast = Toast.makeText(MainActivity.this, _msg, duration);
					mToast.show();
				} catch (final Exception e) {
					// ignore
				}
			}
		});
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
					mToast = Toast.makeText(MainActivity.this, _msg, duration);
					mToast.show();
				} catch (final Exception e) {
					// ignore
				}
			}
		});
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
