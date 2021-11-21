package com.serenegiant.nio;

import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;

/**
 * StandardCharsetsを使えと言われて変更するとAPI<19で「実行時に」クラッシュしてしまうので
 * APIレベルに関係なく定数を使えるようにするためのヘルパークラス
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public class CharsetsUtils {
	private CharsetsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * Eight-bit UCS Transformation Format
	 */
	@NonNull
	public static final Charset UTF8;
	/**
	 * Sixteen-bit UCS Transformation Format, byte order identified by an
	 * optional byte-order mark
	 */
	@NonNull
	public static final Charset UTF16;
	/**
	 * Sixteen-bit UCS Transformation Format, big-endian byte order
	 */
	@NonNull
	public static final Charset UTF16BE;
	/**
	 * Sixteen-bit UCS Transformation Format, little-endian byte order
	 */
	@NonNull
	public static final Charset UTF16LE;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			UTF8 = StandardCharsets.UTF_8;
			UTF16 = StandardCharsets.UTF_16;
			UTF16BE = StandardCharsets.UTF_16BE;
			UTF16LE = StandardCharsets.UTF_16LE;
		} else {
			UTF8 = Charset.forName("UTF-8");
			UTF16 = Charset.forName("UTF-16");
			UTF16BE = Charset.forName("UTF-16BE");
			UTF16LE = Charset.forName("UTF-16LE");
		}
	}

}
