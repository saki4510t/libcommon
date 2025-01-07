package com.serenegiant.usb;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2025 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import java.io.IOException;
import java.io.Serial;

/**
 * USB関係の例外
 */
public class UsbException extends IOException {
	@Serial
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
