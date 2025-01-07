package com.serenegiant.io;
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
import java.io.InputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

public class ByteBufferInputStream extends InputStream implements IReadable {
	@NonNull
	private final ByteBuffer wrappedBuffer;

	public ByteBufferInputStream(@NonNull final ByteBuffer wrappedBuffer) {
		this.wrappedBuffer = wrappedBuffer;
	}

	@Override
	public int available() throws IOException {
		return wrappedBuffer.remaining();
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(final int ignored) {
		wrappedBuffer.mark();
	}

	@Override
	public void reset() {
		wrappedBuffer.rewind();
	}

	@Override
	public int read(final ByteBuffer dst) throws IOException {
		final int bytes = Math.min(dst.remaining(), wrappedBuffer.remaining());
		if (bytes > 0) {
			final byte[] buf = new byte[bytes];
			wrappedBuffer.get(buf);
			return bytes;
		}
		return 0;
	}

	@Override
	public int read() throws IOException {
		return wrappedBuffer.getInt();
	}
}
