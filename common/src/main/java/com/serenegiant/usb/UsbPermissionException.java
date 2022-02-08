package com.serenegiant.usb;

public class UsbPermissionException extends USBException {
	private static final long serialVersionUID = -8430122770852248672L;

	public UsbPermissionException() {
	}

	public UsbPermissionException(final String message) {
		super(message);
	}

	public UsbPermissionException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UsbPermissionException(final Throwable cause) {
		super(cause);
	}
}
