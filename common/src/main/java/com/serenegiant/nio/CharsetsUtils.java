package com.serenegiant.nio;

import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class CharsetsUtils {
	private CharsetsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	/**
	 * Eight-bit UCS Transformation Format
	 */
	public static final Charset UTF8;
	/**
	 * Sixteen-bit UCS Transformation Format, byte order identified by an
	 * optional byte-order mark
	 */
	public static final Charset UTF16;
	/**
	 * Sixteen-bit UCS Transformation Format, big-endian byte order
	 */
	public static final Charset UTF16BE;
	/**
	 * Sixteen-bit UCS Transformation Format, little-endian byte order
	 */
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
