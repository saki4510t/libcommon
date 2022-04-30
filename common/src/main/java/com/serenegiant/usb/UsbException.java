package com.serenegiant.usb;

import java.io.IOException;

/**
 * USB関係の例外
 */
public class UsbException extends IOException {
	private static final long serialVersionUID = 9211466216423287742L;

	public UsbException() {
	}

	public UsbException(final String message) {
		super(message);
	}

	public UsbException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UsbException(final Throwable cause) {
		super(cause);
	}
}
