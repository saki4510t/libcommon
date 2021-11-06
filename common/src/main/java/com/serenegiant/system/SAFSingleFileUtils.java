package com.serenegiant.system;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import java.io.FileNotFoundException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Storage Access Framework/DocumentFile関係のヘルパークラス
 * KITKAT以降で個別のファイル毎にパーミッション要求する場合をSAFUtilsより分離
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class SAFSingleFileUtils {

	private SAFSingleFileUtils() {
		// インスタンス化をエラーにするためにデフォルトオンストラクタをprivateに
	}

	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	public static void requestOpenDocument(
		@NonNull final Activity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}

	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	public static void requestOpenDocument(
		@NonNull final FragmentActivity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}

	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	@Deprecated
	public static void requestOpenDocument(@NonNull final android.app.Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}

	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	public static void requestOpenDocument(
		@NonNull final Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}

	/**
	 * ファイル読み込み用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime
	 * @return
	 */
	private static Intent prepareOpenDocumentIntent(@NonNull final String mime) {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime);
		return intent;
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final Activity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final Activity activity,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final FragmentActivity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final FragmentActivity activity,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final android.app.Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final android.app.Fragment fragment,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
 	 * @param fragment
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	public static void requestCreateDocument(
		@NonNull final Fragment fragment,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}

	/**
	 * ファイル保存用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime
	 * @param defaultName
	 * @return
	 */
	private static Intent prepareCreateDocument(
		final String mime, final String defaultName) {

		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime);
		if (!TextUtils.isEmpty(defaultName)) {
			intent.putExtra(Intent.EXTRA_TITLE, defaultName);
		}
		return intent;
	}

	/**
	 * ファイル削除要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param context
	 * @param uri
	 * @return
	 */
	public static boolean requestDeleteDocument(
		@NonNull final Context context, final Uri uri) {

		try {
			return BuildCheck.isKitKat()
				&& DocumentsContract.deleteDocument(context.getContentResolver(), uri);
		} catch (final FileNotFoundException e) {
			return false;
		}
	}

}
