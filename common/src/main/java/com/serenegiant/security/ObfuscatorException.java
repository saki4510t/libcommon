package com.serenegiant.security;

/**
 * 暗号化・復号時のエラーを通知するためのException実装
 */
public class ObfuscatorException extends Exception {
	private static final long serialVersionUID = -437726590003072651L;

	public ObfuscatorException() {
	}

	public ObfuscatorException(final String message) {
		super(message);
	}

	public ObfuscatorException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ObfuscatorException(final Throwable cause) {
		super(cause);
	}
}
