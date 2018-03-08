package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by saki on 2018/03/07.
 *
 */

public class SysFs {
	private static final boolean DEBUG = false;	// XXX set false on production
	private static final String TAG = SysFs.class.getSimpleName();
	
	private static final Map<String, WeakReference<ReentrantReadWriteLock>> sSysFs
		= new HashMap<String, WeakReference<ReentrantReadWriteLock>>();

	private final String mPath;
	private final String mName;
	protected final ReentrantReadWriteLock mLock;
	private final Lock mReadLock;
	private final Lock mWriteLock;

	public SysFs(@NonNull final String path) throws IOException {
		final File f = new File(path);
		if (!f.exists() || !f.canRead()) {
			throw new IOException(path + " does not exist or can't read.");
		}
		mPath = path;
		mName = f.getName();
		ReentrantReadWriteLock lock = null;
		synchronized (sSysFs) {
			if (sSysFs.containsKey(path)) {
				final WeakReference<ReentrantReadWriteLock> weakLock
					= sSysFs.get(path);
				lock = weakLock != null ? weakLock.get() : null;
			}
			if (lock == null) {
				lock = new ReentrantReadWriteLock();
				sSysFs.put(path, new WeakReference<ReentrantReadWriteLock>(lock));
			}
		}
		mLock = lock;
		mReadLock = lock.readLock();
		mWriteLock = lock.writeLock();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	public void release() {
	}
	
	public String getPath() {
		return mPath;
	}
	
	public String getName() {
		return mName;
	}

	public String read() throws IOException {
		String result = null;
		mReadLock.lock();
		try {
			final FileReader in = new FileReader(mPath);
			try {
				result = new BufferedReader(in).readLine();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
		return result;
	}
	
	public void write(@NonNull final String value) throws IOException {
		mWriteLock.lock();
		try {
			final FileWriter out = new FileWriter(mPath);
			try {
				out.write(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}
	
	@Override
	public String toString() {
		try {
			return String.format(Locale.US, "%s=%s", mPath, read());
		} catch (IOException e) {
			return String.format(Locale.US, "%s=null", mPath);
		}
	}
}
