package com.serenegiant.usb;

/**
 * UsbManager.ACTION_USB_DEVICE_ATTACHEDを受け取っときに
 * 対象となるUsbDeviceがnullだったときの例外
 */
public class UsbAttachException extends UsbException {

	private static final long serialVersionUID = -3877870687869763167L;

	public UsbAttachException() {
	}

	public UsbAttachException(final String message) {
		super(message);
	}

	public UsbAttachException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UsbAttachException(final Throwable cause) {
		super(cause);
	}
}
