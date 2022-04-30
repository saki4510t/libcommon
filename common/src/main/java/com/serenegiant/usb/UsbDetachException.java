package com.serenegiant.usb;

/**
 * UsbManager.ACTION_USB_DEVICE_DETACHEDを受け取ったときに
 * 対象となるUsbDeviceがnullの時の例外
 */
public class UsbDetachException extends UsbException {
	private static final long serialVersionUID = 7103814156749087460L;

	public UsbDetachException() {
	}

	public UsbDetachException(final String message) {
		super(message);
	}

	public UsbDetachException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UsbDetachException(final Throwable cause) {
		super(cause);
	}
}
