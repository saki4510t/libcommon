package com.serenegiant.nio;

import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class CharsetsUtils {
	private CharsetsUtils() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	public static final Charset UTF8;
	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			UTF8  = StandardCharsets.UTF_8;
		} else {
			UTF8 = Charset.forName("UTF-8");
		}
	}

	public static final Charset UTF16;
	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			UTF16  = StandardCharsets.UTF_16;
		} else {
			UTF16 = Charset.forName("UTF-16");
		}
	}

	/**
	 * Sixteen-bit UCS Transformation Format, big-endian byte order
	 */
	public static final Charset UTF16BE;
	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			UTF16BE  = StandardCharsets.UTF_16BE;
		} else {
			UTF16BE = Charset.forName("UTF-16BE");
		}
	}

	/**
	 * Sixteen-bit UCS Transformation Format, little-endian byte order
	 */
	public static final Charset UTF16LE;
	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			UTF16LE  = StandardCharsets.UTF_16LE;
		} else {
			UTF16LE = Charset.forName("UTF-16LE");
		}
	}

}
