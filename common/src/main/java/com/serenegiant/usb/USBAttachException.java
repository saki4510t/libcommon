package com.serenegiant.usb;

public class USBAttachException extends USBException {

	private static final long serialVersionUID = -3877870687869763167L;

	public USBAttachException() {
	}

	public USBAttachException(final String message) {
		super(message);
	}

	public USBAttachException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public USBAttachException(final Throwable cause) {
		super(cause);
	}
}
