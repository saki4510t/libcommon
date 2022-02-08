package com.serenegiant.usb;

public class USBDetachException extends USBException {
	private static final long serialVersionUID = 7103814156749087460L;

	public USBDetachException() {
	}

	public USBDetachException(final String message) {
		super(message);
	}

	public USBDetachException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public USBDetachException(final Throwable cause) {
		super(cause);
	}
}
