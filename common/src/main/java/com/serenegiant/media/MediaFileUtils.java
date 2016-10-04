package com.serenegiant.media;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

import android.os.Environment;
import android.util.Log;

import com.serenegiant.common.BuildConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MediaFileUtils {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "MediaFileUtils";

	private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

	/**
	 * generate output file
	 * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
	 * @param ext .mp4(.m4a for audio) or .png
	 * @return return null when this app has no writing permission to external storage.
	 */
	public static final File getCaptureFile(final String dir_name, final String type, final String ext) {
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), dir_name);
		if (DEBUG) Log.d(TAG, "path=" + dir.toString());
		dir.mkdirs();
		if (dir.canWrite()) {
			return new File(dir, getDateTimeString() + ext);
		}
		return null;
	}

	/**
	 * get current date and time as String
	 * @return
	 */
	private static final String getDateTimeString() {
		final GregorianCalendar now = new GregorianCalendar();
		return mDateTimeFormat.format(now.getTime());
	}
}
