package com.serenegiant.mediastore;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.serenegiant.utils.FileUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MediaStoreへ保存するためのOutputStream実装
 */
public class MediaStoreOutputStream extends OutputStream {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = MediaStoreOutputStream.class.getSimpleName();

//--------------------------------------------------------------------------------
	@NonNull
	private final ContentResolver mCr;
	@NonNull
	private final ContentValues mValues = new ContentValues();
	@NonNull
	private final Uri mUri;
	@NonNull
	private final FileOutputStream mOut;

	/**
	 * コンストラクタ
	 * @param context
	 * @param mimeType
	 * @param nameWithExt
	 * @throws FileNotFoundException
	 */
	public MediaStoreOutputStream(@NonNull final Context context,
		@Nullable final String mimeType,
		@NonNull final String nameWithExt)
			throws FileNotFoundException {
		this(context, mimeType, null, nameWithExt);
	}

	/**
	 *
	 * @param context
	 * @param mimeType
	 * @param relativePath
	 * @param nameWithExt
	 * @throws FileNotFoundException
	 */
	public MediaStoreOutputStream(@NonNull final Context context,
	  @Nullable final String mimeType,
	  @Nullable final String relativePath,
	  @NonNull final String nameWithExt)
			throws FileNotFoundException {

		mCr = context.getContentResolver();
		@NonNull
		String _mimeType = mimeType != null ? mimeType.toLowerCase() : "";
		@NonNull
		final String ext = FileUtils.getExt(nameWithExt).toLowerCase();

		final Uri queryUri;
		if (_mimeType.startsWith("image/")
			|| ext.equalsIgnoreCase("png")
			|| ext.equalsIgnoreCase("jpg")
			|| ext.equalsIgnoreCase("jpeg")
			|| ext.equalsIgnoreCase("webp")) {

			// 静止画
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "image/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			mValues.put(MediaStore.Images.Media.DISPLAY_NAME, nameWithExt);
			mValues.put(MediaStore.Images.Media.MIME_TYPE, _mimeType);
			queryUri = MediaStoreUtils.QUERY_URI_IMAGES;
		} else if (_mimeType.startsWith("video/")
			|| ext.equalsIgnoreCase("mp4")
			|| ext.equalsIgnoreCase("h264")
			|| ext.equalsIgnoreCase("mjpeg")) {

			// 動画
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "video/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			mValues.put(MediaStore.Video.Media.DISPLAY_NAME, nameWithExt);
			mValues.put(MediaStore.Video.Media.MIME_TYPE, _mimeType);
			queryUri = MediaStoreUtils.QUERY_URI_VIDEO;
		} else if (_mimeType.startsWith("audio/")
			|| ext.equalsIgnoreCase("m4a")) {

			// 音声
			if (TextUtils.isEmpty(_mimeType)) {
				_mimeType = "audio/" + (TextUtils.isEmpty(ext) ? "*" : ext);
			}
			mValues.put(MediaStore.Audio.Media.DISPLAY_NAME, nameWithExt);
			mValues.put(MediaStore.Audio.Media.MIME_TYPE, _mimeType);
			queryUri = MediaStoreUtils.QUERY_URI_AUDIO;
		} else if (_mimeType.startsWith("*/")) {
			// ファイル
			mValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, nameWithExt);
			mValues.put(MediaStore.Files.FileColumns.MIME_TYPE, _mimeType);
			queryUri = MediaStoreUtils.QUERY_URI_FILES;
		} else {
			throw new IllegalArgumentException("unknown mimeType/file type," + mimeType + ",name=" + nameWithExt);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (!TextUtils.isEmpty(relativePath)) {
				mValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
			}
			mValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
		}
		mUri = mCr.insert(queryUri, mValues);
		final ParcelFileDescriptor pfd = mCr.openFileDescriptor(mUri, "w");
		mOut = new FileOutputStream(pfd.getFileDescriptor());
	}

	@NonNull
	public Uri getUri() {
		return mUri;
	}

	@NonNull
	public FileDescriptor getFd() throws IOException {
		return mOut.getFD();
	}

	/**
	 * Writes the specified byte to this output stream. The general
	 * contract for <code>write</code> is that one byte is written
	 * to the output stream. The byte to be written is the eight
	 * low-order bits of the argument <code>b</code>. The 24
	 * high-order bits of <code>b</code> are ignored.
	 * <p>
	 * Subclasses of <code>OutputStream</code> must provide an
	 * implementation for this method.
	 *
	 * @param b the <code>byte</code>.
	 * @throws IOException if an I/O error occurs. In particular,
	 *                     an <code>IOException</code> may be thrown if the
	 *                     output stream has been closed.
	 */
	public void write(final int b) throws IOException {
		mOut.write(b);
	}

	/**
	 * Writes <code>b.length</code> bytes from the specified byte array
	 * to this output stream. The general contract for <code>write(b)</code>
	 * is that it should have exactly the same effect as the call
	 * <code>write(b, 0, b.length)</code>.
	 *
	 * @param b the data.
	 * @throws IOException if an I/O error occurs.
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(@NonNull byte[] b) throws IOException {
		mOut.write(b, 0, b.length);
	}

	/**
	 * Writes <code>len</code> bytes from the specified byte array
	 * starting at offset <code>off</code> to this output stream.
	 * The general contract for <code>write(b, off, len)</code> is that
	 * some of the bytes in the array <code>b</code> are written to the
	 * output stream in order; element <code>b[off]</code> is the first
	 * byte written and <code>b[off+len-1]</code> is the last byte written
	 * by this operation.
	 * <p>
	 * The <code>write</code> method of <code>OutputStream</code> calls
	 * the write method of one argument on each of the bytes to be
	 * written out. Subclasses are encouraged to override this method and
	 * provide a more efficient implementation.
	 * <p>
	 * If <code>b</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown.
	 * <p>
	 * If <code>off</code> is negative, or <code>len</code> is negative, or
	 * <code>off+len</code> is greater than the length of the array
	 * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
	 *
	 * @param b   the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @throws IOException if an I/O error occurs. In particular,
	 *                     an <code>IOException</code> is thrown if the output
	 *                     stream is closed.
	 */
	public void write(@NonNull final byte[] b, final int off, final int len)
		throws IOException {

		if (closed && len > 0) {
			throw new IOException("Stream Closed");
		}
		mOut.write(b, off, len);
	}

	/**
	 * Flushes this output stream and forces any buffered output bytes
	 * to be written out. The general contract of <code>flush</code> is
	 * that calling it is an indication that, if any bytes previously
	 * written have been buffered by the implementation of the output
	 * stream, such bytes should immediately be written to their
	 * intended destination.
	 * <p>
	 * If the intended destination of this stream is an abstraction provided by
	 * the underlying operating system, for example a file, then flushing the
	 * stream guarantees only that bytes previously written to the stream are
	 * passed to the operating system for writing; it does not guarantee that
	 * they are actually written to a physical device such as a disk drive.
	 * <p>
	 * The <code>flush</code> method of <code>OutputStream</code> does nothing.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	public void flush() throws IOException {
		mOut.flush();
	}

	private final Object closeLock = new Object();
	private volatile boolean closed = false;

	/**
	 * Closes this output stream and releases any system resources
	 * associated with this stream. The general contract of <code>close</code>
	 * is that it closes the output stream. A closed stream cannot perform
	 * output operations and cannot be reopened.
	 * <p>
	 * The <code>close</code> method of <code>OutputStream</code> does nothing.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	public void close() throws IOException {
		synchronized (closeLock) {
			if (closed) {
				return;
			}
			closed = true;
		}

		try {
			mOut.close();
		} finally {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				mValues.clear();
				mValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
				mCr.update(mUri, mValues, null, null);
			}
		}
	}

}
