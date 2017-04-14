package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.support.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;

/**
 * 全部ネットワークバイトオーダー = ビッグエンディアンやからな
 */
public class ChannelHelper {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	/**
	 * ByteChannelからbooleanを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static boolean readBoolean(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(1);
		final int readBytes = channel.read(buf);
		if (readBytes != 1) throw new IOException();
		buf.clear();
		return buf.get() != 0;
	}
	
	/**
	 * ByteChannelからbyeを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static byte readByte(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(1);
		final int readBytes = channel.read(buf);
		if (readBytes != 1) throw new IOException();
		buf.clear();
		return buf.get();
	}
	
	/**
	 * ByteChannelからcharを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static char readChar(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(2);
		final int readBytes = channel.read(buf);
		if (readBytes != 2) throw new IOException();
		buf.clear();
		return buf.getChar();
	}
	
	/**
	 * ByteChannelからshortを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static short readShort(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(2);
		final int readBytes = channel.read(buf);
		if (readBytes != 2) throw new IOException();
		buf.clear();
		return buf.getShort();
	}
	
	/**
	 * ByteChannelからintを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static int readInt(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(4);
		final int readBytes = channel.read(buf);
		if (readBytes != 4) throw new IOException();
		buf.clear();
		return buf.getInt();
	}
	
	/**
	 * ByteChannelからlongを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static long readLong(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(8);
		final int readBytes = channel.read(buf);
		if (readBytes != 8) throw new IOException();
		buf.clear();
		return buf.getLong();
	}
	
	/**
	 * ByteChannelからfloatを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static float readFloat(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(4);
		final int readBytes = channel.read(buf);
		if (readBytes != 4) throw new IOException();
		buf.clear();
		return buf.getFloat();
	}
	
	/**
	 * ByteChannelからdoubleを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static double readDouble(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(8);
		final int readBytes = channel.read(buf);
		if (readBytes != 8) throw new IOException();
		buf.clear();
		return buf.getDouble();
	}
	
	/**
	 * ByteChannelからStringを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static String readString(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int bytes = readInt(channel);
		final byte[] buf = new byte[bytes];
		final ByteBuffer b = ByteBuffer.wrap(buf);
		final int readBytes = channel.read(b);
		if (readBytes != bytes) throw new IOException();
		return new String(buf, UTF8);
	}
	
	/**
	 * ByteChannelからboolean配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static boolean[] readBooleanArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		buf.clear();
		final boolean[] result = new boolean[n];
		for (int i = 0; i < n; i++) {
			result[i] = buf.get() != 0;
		}
		return result;
	}
	
	/**
	 * ByteChannelからbyte配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static byte[] readByteArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final byte[] result = new byte[n];
		final ByteBuffer buf = ByteBuffer.wrap(result);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		return result;
	}
	
	/**
	 * ByteChannelからchar配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static char[] readCharArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 2);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 2) throw new IOException();
		buf.clear();
		final CharBuffer result = buf.asCharBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final char[] b = new char[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからshort配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static short[] readShortArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 2);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 2) throw new IOException();
		buf.clear();
		final ShortBuffer result = buf.asShortBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final short[] b = new short[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからint配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static int[] readIntArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 4);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 4) throw new IOException();
		buf.clear();
		final IntBuffer result = buf.asIntBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final int[] b = new int[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからlong配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static long[] readLongArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 8);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 8) throw new IOException();
		buf.clear();
		final LongBuffer result = buf.asLongBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final long[] b = new long[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからfloat配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static float[] readFloatArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 4);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 4) throw new IOException();
		buf.clear();
		final FloatBuffer result = buf.asFloatBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final float[] b = new float[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからdouble配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static double[] readDoubleArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 8);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 8) throw new IOException();
		buf.clear();
		final DoubleBuffer result = buf.asDoubleBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final double[] b = new double[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからByteBufferを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static ByteBuffer readByteBuffer(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocateDirect(n);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		buf.position(n);
		buf.flip();
		return buf;
	}
	
	/**
	 * ByteChannelからByteBufferを読み込む
	 * 指定したbufがnullまたは読み込むサイズよりも小さい場合は
	 * ダミーリード後IOExceptionを投げる
	 * @param channel
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public static ByteBuffer readByteBuffer(@NonNull final ByteChannel channel,
		@NonNull final ByteBuffer buf) throws IOException {
		
		final int n = readInt(channel);
		if (buf.remaining() < n) {
			final ByteBuffer temp = ByteBuffer.allocate(n);
			channel.read(temp);	// dummy read
			throw new IOException();
		}
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		buf.position(n);
		buf.flip();
		return buf;
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final boolean value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put((byte)(value ? 1 : 0));
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final byte value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final char value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putChar(value);
		buf.flip();
		channel.write(buf);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final short value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final int value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putInt(value);
		buf.flip();
		channel.write(buf);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final long value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putLong(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final float value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putFloat(value);
		buf.flip();
		channel.write(buf);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final double value) throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putDouble(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final String value) throws IOException {
		
		final byte[] buf = value.getBytes(UTF8);
		write(channel, buf.length);
		channel.write(ByteBuffer.wrap(buf));
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final boolean[] value) throws IOException {
		
		final int n = value.length;
		write(channel, n);
		final ByteBuffer buf = ByteBuffer.allocate(n);
		for (int i = 0; i < n; i++) {
			buf.put((byte)(value[i] ? 1 : 0));
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final byte[] value) throws IOException {
		
		write(channel, value.length);
		channel.write(ByteBuffer.wrap(value));
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final char[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 2);
		for (int i = 0; i < n; i++) {
			buf.putChar(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final short[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 2);
		for (int i = 0; i < n; i++) {
			buf.putShort(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final int[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 4);
		for (int i = 0; i < n; i++) {
			buf.putInt(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final long[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 8);
		for (int i = 0; i < n; i++) {
			buf.putLong(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final float[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 4);
		for (int i = 0; i < n; i++) {
			buf.putFloat(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final double[] value) throws IOException {
		
		final int n = value.length;
		final ByteBuffer buf = ByteBuffer.allocate(n * 8);
		for (int i = 0; i < n; i++) {
			buf.putDouble(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final ByteBuffer value) throws IOException {
		
		write(channel, value.remaining());
		channel.write(value);
	}
	
}
