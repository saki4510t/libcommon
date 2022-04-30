package com.serenegiant.usb;

import java.io.IOException;

/**
 * USB関係の例外
 */
public class USBException extends IOException {
	private static final long serialVersionUID = 9211466216423287742L;

	public USBException() {
	}

	public USBException(final String message) {
		super(message);
	}

	public USBException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public USBException(final Throwable cause) {
		super(cause);
	}
}
