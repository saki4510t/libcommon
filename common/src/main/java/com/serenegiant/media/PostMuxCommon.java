package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2016-2022 saki t_saki@serenegiant.com
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

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.io.ChannelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Locale;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * PostMux録画サービス関係の共通処理クラス
 *
 * 映像エンコード処理と音声エンコード処理とmux処理を同時に実行すると
 * 映像and/or音声が正常に記録されない端末がいくつかあるので、
 * 一度一時ファイルへ書き出しておいてエンコード終了後にmux処理を
 * 行うPostMux関係のクラス
 */
public class PostMuxCommon {
	private static final boolean DEBUG = false; // FIXME set false on production
	private static final String TAG = PostMuxCommon.class.getSimpleName();

	static final int TYPE_VIDEO = 0;
	static final int TYPE_AUDIO = 1;

	@IntDef({TYPE_VIDEO,
		TYPE_AUDIO,
	})
	@Retention(RetentionPolicy.SOURCE)
	@interface MediaType {}

	/*package*/ static final String VIDEO_NAME = "video.raw";
	/*package*/ static final String AUDIO_NAME = "audio.raw";

	/**
	 * write MediaFormat data into intermediate file
	 * @param out
	 * @param codecFormat
	 * @param outputFormat
	 * @throws IOException
	 */
	/*package*/ static final void writeFormat(
		@NonNull final DataOutputStream out,
		@NonNull final MediaFormat codecFormat,
		@NonNull final MediaFormat outputFormat) throws IOException {

		if (DEBUG) Log.v(TAG, "writeFormat:format=" + outputFormat);
		final String codecFormatStr = asString(codecFormat);
		final String outputFormatStr = asString(outputFormat);
		final int size = (TextUtils.isEmpty(codecFormatStr) ? 0 : codecFormatStr.length())
			+ (TextUtils.isEmpty(outputFormatStr) ? 0 : outputFormatStr.length());

		writeHeader(out, 0, 0, -1, size, 0);
		out.writeUTF(codecFormatStr);
		out.writeUTF(outputFormatStr);
	}
	
	/**
	 * write MediaFormat data into intermediate file
	 * @param out
	 * @param codecFormat
	 * @param outputFormat
	 * @throws IOException
	 */
	/*package*/ static final void writeFormat(
		@NonNull final ByteChannel out,
		@NonNull final MediaFormat codecFormat,
		@NonNull final MediaFormat outputFormat) throws IOException {

		if (DEBUG) Log.v(TAG, "writeFormat:format=" + outputFormat);
		final String codecFormatStr = asString(codecFormat);
		final String outputFormatStr = asString(outputFormat);
		final int size = (TextUtils.isEmpty(codecFormatStr) ? 0 : codecFormatStr.length())
			+ (TextUtils.isEmpty(outputFormatStr) ? 0 : outputFormatStr.length());

		writeHeader(out, 0, 0, -1, size, 0);
		ChannelHelper.write(out, codecFormatStr);
		ChannelHelper.write(out, outputFormatStr);
	}

	/**
	 * read MediaFormat from intermediate file
	 * @param in
	 * @return
	 */
	/*package*/ static MediaFormat readFormat(@NonNull final DataInputStream in) {
		if (DEBUG) Log.v(TAG, "readFormat:");
		MediaFormat format = null;
		try {
			readHeader(in);
			in.readUTF();	// skip MediaFormat data for configure
			format = asMediaFormat(in.readUTF());
		} catch (final IOException e) {
			Log.e(TAG, "readFormat:", e);
		}
		if (DEBUG) Log.v(TAG, "readFormat:format=" + format);
		return format;
	}

	/**
	 * read MediaFormat from intermediate file
	 * @param in
	 * @return
	 */
	/*package*/ static MediaFormat readFormat(@NonNull final ByteChannel in) {
		if (DEBUG) Log.v(TAG, "readFormat:");
		MediaFormat format = null;
		try {
			readHeader(in);
			ChannelHelper.readString(in); // skip MediaFormat data for configure
			format = asMediaFormat(ChannelHelper.readString(in));
		} catch (final IOException e) {
			Log.e(TAG, "readFormat:", e);
		}
		if (DEBUG) Log.v(TAG, "readFormat:format=" + format);
		return format;
	}

	/**
	 * MediaFormatのシリアライズ用, Gsonの方が良かった？
	 * @param format
	 * @return
	 */
	@SuppressLint("InlinedApi")
	/*package*/ static final String asString(@NonNull final MediaFormat format) {
		final JSONObject map = new JSONObject();
		try {
			if (format.containsKey(MediaFormat.KEY_MIME))
				map.put(MediaFormat.KEY_MIME,
					format.getString(MediaFormat.KEY_MIME));
			if (format.containsKey(MediaFormat.KEY_WIDTH))
				map.put(MediaFormat.KEY_WIDTH,
					format.getInteger(MediaFormat.KEY_WIDTH));
			if (format.containsKey(MediaFormat.KEY_HEIGHT))
				map.put(MediaFormat.KEY_HEIGHT,
					format.getInteger(MediaFormat.KEY_HEIGHT));
			if (format.containsKey(MediaFormat.KEY_BIT_RATE))
				map.put(MediaFormat.KEY_BIT_RATE,
					format.getInteger(MediaFormat.KEY_BIT_RATE));
			if (format.containsKey(MediaFormat.KEY_COLOR_FORMAT))
				map.put(MediaFormat.KEY_COLOR_FORMAT,
					format.getInteger(MediaFormat.KEY_COLOR_FORMAT));
			if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
				map.put(MediaFormat.KEY_FRAME_RATE,
					format.getInteger(MediaFormat.KEY_FRAME_RATE));
			if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL))
				map.put(MediaFormat.KEY_I_FRAME_INTERVAL,
					format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL));
			if (format.containsKey(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER))
				map.put(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
					format.getLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER));
			if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
				map.put(MediaFormat.KEY_MAX_INPUT_SIZE,
					format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
			if (format.containsKey(MediaFormat.KEY_DURATION))
				map.put(MediaFormat.KEY_DURATION,
					format.getInteger(MediaFormat.KEY_DURATION));
			if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
				map.put(MediaFormat.KEY_CHANNEL_COUNT,
					format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
			if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
				map.put(MediaFormat.KEY_SAMPLE_RATE,
					format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
			if (format.containsKey(MediaFormat.KEY_CHANNEL_MASK))
				map.put(MediaFormat.KEY_CHANNEL_MASK,
					format.getInteger(MediaFormat.KEY_CHANNEL_MASK));
			if (format.containsKey(MediaFormat.KEY_AAC_PROFILE))
				map.put(MediaFormat.KEY_AAC_PROFILE,
					format.getInteger(MediaFormat.KEY_AAC_PROFILE));
			if (format.containsKey(MediaFormat.KEY_AAC_SBR_MODE))
				map.put(MediaFormat.KEY_AAC_SBR_MODE,
					format.getInteger(MediaFormat.KEY_AAC_SBR_MODE));
			if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
				map.put(MediaFormat.KEY_MAX_INPUT_SIZE,
					format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
			if (format.containsKey(MediaFormat.KEY_IS_ADTS))
				map.put(MediaFormat.KEY_IS_ADTS,
					format.getInteger(MediaFormat.KEY_IS_ADTS));
			if (format.containsKey("what"))
				map.put("what", format.getInteger("what"));
			if (format.containsKey("csd-0"))
				map.put("csd-0", asString(format.getByteBuffer("csd-0")));
			if (format.containsKey("csd-1"))
				map.put("csd-1", asString(format.getByteBuffer("csd-1")));
			if (format.containsKey("csd-2"))
				map.put("csd-2", asString(format.getByteBuffer("csd-2")));
		} catch (final JSONException e) {
			Log.e(TAG, "writeFormat:", e);
		}

		return map.toString();
	}
	
	/**
	 * MediaFormatのデシリアライズ用, Gsonの方が良かった？
	 * @param format_str
	 * @return
	 */
	@SuppressLint("InlinedApi")
	/*package*/ static final MediaFormat asMediaFormat(@NonNull final String format_str) {
		MediaFormat format = new MediaFormat();
		try {
			final JSONObject map = new JSONObject(format_str);
			if (map.has(MediaFormat.KEY_MIME))
				format.setString(MediaFormat.KEY_MIME,
					(String)map.get(MediaFormat.KEY_MIME));
			if (map.has(MediaFormat.KEY_WIDTH))
				format.setInteger(MediaFormat.KEY_WIDTH,
					(Integer)map.get(MediaFormat.KEY_WIDTH));
			if (map.has(MediaFormat.KEY_HEIGHT))
				format.setInteger(MediaFormat.KEY_HEIGHT,
					(Integer)map.get(MediaFormat.KEY_HEIGHT));
			if (map.has(MediaFormat.KEY_BIT_RATE))
				format.setInteger(MediaFormat.KEY_BIT_RATE,
					(Integer)map.get(MediaFormat.KEY_BIT_RATE));
			if (map.has(MediaFormat.KEY_COLOR_FORMAT))
				format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
					(Integer)map.get(MediaFormat.KEY_COLOR_FORMAT));
			if (map.has(MediaFormat.KEY_FRAME_RATE))
				format.setInteger(MediaFormat.KEY_FRAME_RATE,
					(Integer)map.get(MediaFormat.KEY_FRAME_RATE));
			if (map.has(MediaFormat.KEY_I_FRAME_INTERVAL))
				format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
					(Integer)map.get(MediaFormat.KEY_I_FRAME_INTERVAL));
			if (map.has(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER))
				format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
					(Long)map.get(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER));
			if (map.has(MediaFormat.KEY_MAX_INPUT_SIZE))
				format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,
					(Integer)map.get(MediaFormat.KEY_MAX_INPUT_SIZE));
			if (map.has(MediaFormat.KEY_DURATION))
				format.setInteger(MediaFormat.KEY_DURATION,
					(Integer)map.get(MediaFormat.KEY_DURATION));
			if (map.has(MediaFormat.KEY_CHANNEL_COUNT))
				format.setInteger(MediaFormat.KEY_CHANNEL_COUNT,
					(Integer) map.get(MediaFormat.KEY_CHANNEL_COUNT));
			if (map.has(MediaFormat.KEY_SAMPLE_RATE))
				format.setInteger(MediaFormat.KEY_SAMPLE_RATE,
					(Integer) map.get(MediaFormat.KEY_SAMPLE_RATE));
			if (map.has(MediaFormat.KEY_CHANNEL_MASK))
				format.setInteger(MediaFormat.KEY_CHANNEL_MASK,
					(Integer) map.get(MediaFormat.KEY_CHANNEL_MASK));
			if (map.has(MediaFormat.KEY_AAC_PROFILE))
				format.setInteger(MediaFormat.KEY_AAC_PROFILE,
					(Integer) map.get(MediaFormat.KEY_AAC_PROFILE));
			if (map.has(MediaFormat.KEY_AAC_SBR_MODE))
				format.setInteger(MediaFormat.KEY_AAC_SBR_MODE,
					(Integer) map.get(MediaFormat.KEY_AAC_SBR_MODE));
			if (map.has(MediaFormat.KEY_MAX_INPUT_SIZE))
				format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,
					(Integer) map.get(MediaFormat.KEY_MAX_INPUT_SIZE));
			if (map.has(MediaFormat.KEY_IS_ADTS))
				format.setInteger(MediaFormat.KEY_IS_ADTS,
					(Integer) map.get(MediaFormat.KEY_IS_ADTS));
			if (map.has("what"))
				format.setInteger("what", (Integer)map.get("what"));
			if (map.has("csd-0"))
				format.setByteBuffer("csd-0", asByteBuffer((String)map.get("csd-0")));
			if (map.has("csd-1"))
				format.setByteBuffer("csd-1", asByteBuffer((String)map.get("csd-1")));
			if (map.has("csd-2"))
				format.setByteBuffer("csd-2", asByteBuffer((String)map.get("csd-2")));
		} catch (final JSONException e) {
			Log.e(TAG, "writeFormat:" + format_str, e);
			format = null;
		}
		return format;
	}

	/**
	 * バイトバッファーの内容を文字列に変換するためのヘルパーメソッド
	 * @param buffer
	 * @return
	 */
	/*package*/ static final String asString(@NonNull final ByteBuffer buffer) {
		final byte[] temp = new byte[16];
		final StringBuilder sb = new StringBuilder();
		int n = (buffer != null ? buffer.limit() : 0);
		if (n > 0) {
			buffer.rewind();
			int sz = Math.min(n, 16);
			n -= sz;
			for (; sz > 0; sz = Math.min(n, 16), n -= sz) {
				buffer.get(temp, 0, sz);
				for (int i = 0; i < sz; i++) {
					sb.append(temp[i]).append(',');
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 文字列表記から元のバイトバッファーに戻すためのヘルパーメソッド
	 * @param str
	 * @return
	 */
	/*package*/ static final ByteBuffer asByteBuffer(@NonNull final String str) {
		final String[] hex = str.split(",");
		final int m = hex.length;
		final byte[] temp = new byte[m];
		int n = 0;
		for (int i = 0; i < m; i++) {
			if (!TextUtils.isEmpty(hex[i]))
				temp[n++] = (byte)Integer.parseInt(hex[i]);
		}
		return (n > 0) ? ByteBuffer.wrap(temp, 0, n) : null;
	}
	

//----------------------------------------------------------------------
	/** 将来の拡張に備えてダミーデータを書くためのバッファ, longを5個 = 8バイト x 5 = 40バイト */
	private static final byte[] RESERVED = new byte[40];
	
	/**
	 * フレームデータの前に付加するフレームヘッダー,
	 * MediaCodec.BufferInfoのデータを保存するため
	 */
	/* package */static class MediaFrameHeader {
		public int sequence;
		public int frameNumber;
		public long presentationTimeUs;
		public int size;
		public int flags;
		
		/**
		 * フレームヘッダーの内容をMediaCodec.BufferInfoとして取得
		 * @return
		 */
		@NonNull
		public MediaCodec.BufferInfo asBufferInfo() {
			final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			info.set(0, size, presentationTimeUs, flags);
			return info;
		}
		
		/**
		 * フレームヘッダーの内容をMediaCodec.BufferInfoとして取得
		 * @param info
		 * @return
		 */
		@NonNull
		public MediaCodec.BufferInfo asBufferInfo(@NonNull final MediaCodec.BufferInfo info) {
			info.set(0, size, presentationTimeUs, flags);
			return info;
		}
		
		/**
		 * フレームヘッダーを中間ファイルへ出力
 		 * @param out
		 * @throws IOException
		 */
		public void writeTo(@NonNull final DataOutputStream out) throws IOException {
			out.writeInt(sequence);
			out.writeInt(frameNumber);
			out.writeLong(presentationTimeUs);
			out.writeInt(size);
			out.writeInt(flags);
			//
			out.write(RESERVED, 0, 40);
		}
		
		/**
		 * フレームヘッダーを中間ファイルへ出力
 		 * @param out
		 * @throws IOException
		 */
		public void writeTo(@NonNull final ByteChannel out) throws IOException {
			ChannelHelper.write(out, sequence);
			ChannelHelper.write(out, sequence);
			ChannelHelper.write(out, frameNumber);
			ChannelHelper.write(out, presentationTimeUs);
			ChannelHelper.write(out, size);
			ChannelHelper.write(out, flags);
			//
			ChannelHelper.write(out, RESERVED);
		}

		@NonNull
		@Override
		public String toString() {
			return String.format(Locale.US,
				"MediaFrameHeader(sequence=%d,frameNumber=%d,presentationTimeUs=%d,size=%d,flags=%d)",
				sequence, frameNumber, presentationTimeUs, size, flags);
		}
	}

	/**
	 * フレームヘッダーを書き込む
	 * @param sequence
	 * @param frame_number
	 * @param presentation_time_us
	 * @param size
	 * @param flag
	 * @throws IOException
	 */
	/*package*/ static void writeHeader(@NonNull final DataOutputStream out,
		final int sequence, final int frame_number,
		final long presentation_time_us, final int size, final int flag)
			throws IOException {

		out.writeInt(sequence);
		out.writeInt(frame_number);
		out.writeLong(presentation_time_us);
		out.writeInt(size);
		out.writeInt(flag);
		//
		out.write(RESERVED, 0, 40);
	}
	
	/**
	 * フレームヘッダーを書き込む
	 * @param sequence
	 * @param frame_number
	 * @param presentation_time_us
	 * @param size
	 * @param flag
	 * @throws IOException
	 */
	/*package*/ static void writeHeader(@NonNull final ByteChannel out,
		final int sequence, final int frame_number,
		final long presentation_time_us, final int size, final int flag)
			throws IOException {

		ChannelHelper.write(out, sequence);
		ChannelHelper.write(out, frame_number);
		ChannelHelper.write(out, presentation_time_us);
		ChannelHelper.write(out, size);
		ChannelHelper.write(out, flag);
		//
		ChannelHelper.write(out, RESERVED);
	}
	
	/**
	 * フレームヘッダーを読み込む
	 * @param in
	 * @param header
	 * @return
	 * @throws IOException
	 */
	/*package*/ static MediaFrameHeader readHeader(@NonNull final DataInputStream in,
		@NonNull final MediaFrameHeader header) throws IOException {

		header.size = 0;
		header.sequence = in.readInt();
		header.frameNumber = in.readInt();	// frame number
		header.presentationTimeUs = in.readLong();
		header.size = in.readInt();
		header.flags = in.readInt();
		in.skipBytes(40);	// long x 5
		return header;
	}
	
	/**
	 * フレームヘッダーを読み込む
	 * @param in
	 * @return
	 * @throws IOException
	 */
	/*package*/ static MediaFrameHeader readHeader(@NonNull final DataInputStream in)
		throws IOException {

		final MediaFrameHeader header = new MediaFrameHeader();
		return readHeader(in, header);
	}

	/**
	 * フレームヘッダーを読み込む
	 * @param in
	 * @param header
	 * @return
	 * @throws IOException
	 */
	/*package*/ static MediaFrameHeader readHeader(@NonNull final ByteChannel in,
		@NonNull final MediaFrameHeader header) throws IOException {

		header.size = 0;
		header.sequence = ChannelHelper.readInt(in);
		header.frameNumber = ChannelHelper.readInt(in);	// frame number
		header.presentationTimeUs = ChannelHelper.readLong(in);
		header.size = ChannelHelper.readInt(in);
		header.flags = ChannelHelper.readInt(in);
		ChannelHelper.readByteArray(in);
		return header;
	}

	/**
	 * フレームヘッダーを読み込む
	 * @param in
	 * @return
	 * @throws IOException
	 */
	/*package*/ static MediaFrameHeader readHeader(@NonNull final ByteChannel in)
		throws IOException {

		final MediaFrameHeader header = new MediaFrameHeader();
		return readHeader(in, header);
	}

	/**
	 * フレームヘッダーを読み込んでフレームサイズを返す
	 * @param in
	 * @return
	 * @throws IOException
	 */
	/*package*/ static int readFrameSize(@NonNull final DataInputStream in)
		throws IOException {

		final MediaFrameHeader header = readHeader(in);
		return header.size;
	}

	/**
	 * フレームヘッダーを読み込んでフレームサイズを返す
	 * @param in
	 * @return
	 * @throws IOException
	 */
	/*package*/ static int readFrameSize(@NonNull final ByteChannel in)
		throws IOException {

		final MediaFrameHeader header = readHeader(in);
		return header.size;
	}

	/**
	 * MediaCodecでエンコード済みのフレームデータをファイルに書き込む
	 * @param out
	 * @param sequence
	 * @param frameNumber
	 * @param info
	 * @param buffer
	 * @param work DataOutputStreamはByteBufferを直接書き込めないので一旦byte[]に取り出すためのワーク
	 * @throws IOException
	 */
	/*package*/ static final void writeStream(@NonNull final DataOutputStream out,
		final int sequence, final int frameNumber,
		@NonNull final MediaCodec.BufferInfo info,
		@NonNull final ByteBuffer buffer,
		@NonNull byte[] work) throws IOException {

		buffer.position(info.offset);
		buffer.get(work, 0, info.size);	// will throw BufferUnderflowException
		try {
			writeHeader(out, sequence, frameNumber,
				info.presentationTimeUs, info.size, info.flags);
			out.write(work, 0, info.size);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * MediaCodecでエンコード済みのフレームデータをファイルに書き込む
	 * @param out
	 * @param sequence
	 * @param frameNumber
	 * @param info
	 * @param buffer
	 * @throws IOException
	 */
	/*package*/ static final void writeStream(@NonNull final ByteChannel out,
		final int sequence, final int frameNumber,
		@NonNull final MediaCodec.BufferInfo info,
		@NonNull final ByteBuffer buffer) throws IOException {

		buffer.position(info.offset);
		buffer.limit(info.offset + info.size);
		try {
			writeHeader(out, sequence, frameNumber,
				info.presentationTimeUs, info.size, info.flags);
			ChannelHelper.write(out, buffer);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * read raw bit stream from specific intermediate file
	 * @param in
	 * @param header
	 * @param buffer
	 * @param readBuffer
	 * @throws IOException
	 * @throws BufferOverflowException
	 */
	/*package*/ static ByteBuffer readStream(
		@NonNull final DataInputStream in,
		@NonNull final MediaFrameHeader header,
		@Nullable ByteBuffer buffer,
		@NonNull final byte[] readBuffer) throws IOException {

		readHeader(in, header);
		if ((buffer == null) || header.size > buffer.capacity()) {
			buffer = ByteBuffer.allocateDirect(header.size);
		}
		buffer.clear();
		final int max_bytes = Math.min(readBuffer.length, header.size);
		int read_bytes;
		for (int i = header.size; i > 0; i -= read_bytes) {
			read_bytes = in.read(readBuffer, 0, Math.min(i, max_bytes));
			if (read_bytes <= 0) break;
			buffer.put(readBuffer, 0, read_bytes);
		}
		buffer.flip();
		return buffer;
	}

	/**
	 * read raw bit stream from specific intermediate file
	 * @param in
	 * @param header
	 * @param buffer
	 * @throws IOException
	 * @throws BufferOverflowException
	 */
	/*package*/ static ByteBuffer readStream(
		@NonNull final ByteChannel in,
		@NonNull final MediaFrameHeader header,
		@Nullable ByteBuffer buffer) throws IOException {

		readHeader(in, header);
		if ((buffer == null) || header.size > buffer.capacity()) {
			buffer = ByteBuffer.allocateDirect(header.size);
		}
		buffer.clear();
		ChannelHelper.readByteBuffer(in, buffer);
		return buffer;
	}
}
