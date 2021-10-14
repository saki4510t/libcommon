package com.serenegiant.net;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
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

import android.util.Log;

import java.util.Arrays;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import androidx.annotation.NonNull;

/**
 * SSLSession関係のヘルパークラス
 */
public class SSLSessionUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SSLSessionUtils.class.getSimpleName();

	private SSLSessionUtils() {
		// インスタンス化を防止するためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * dump SSLSession into logCat
	 * @param session
	 */
	public static void dump(@NonNull final SSLSession session) {
		Log.i(TAG, "id=" + Arrays.toString(session.getId()));
		Log.i(TAG, "context=" + session.getSessionContext());
		Log.i(TAG, "creationTime=" + session.getCreationTime());
		Log.i(TAG, "lastAccessedTime=" + session.getLastAccessedTime());
		Log.i(TAG, "isValid=" + session.isValid());
		final String[] names = session.getValueNames();
		if ((names != null) && (names.length > 0)) {
			for (final String name: names) {
				Log.i(TAG, "values[" + name + "]=" + session.getValue(name));
			}
		}
		try {
			Log.i(TAG, "peerCertificates=" + Arrays.toString(session.getPeerCertificates()));
		} catch (final SSLPeerUnverifiedException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		Log.i(TAG, "localCertificates=" + Arrays.toString(session.getLocalCertificates()));
		try {
			Log.i(TAG, "peerCertificateChain=" + Arrays.toString(session.getPeerCertificateChain()));
		} catch (final SSLPeerUnverifiedException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		try {
			Log.i(TAG, "peerPrincipal=" + session.getPeerPrincipal());
		} catch (final SSLPeerUnverifiedException e) {
			if (DEBUG) Log.w(TAG, e);
		}
		Log.i(TAG, "localPrincipal=" + session.getLocalPrincipal());
		Log.i(TAG, "cipherSuite=" + session.getCipherSuite());
		Log.i(TAG, "protocol=" + session.getProtocol());
		Log.i(TAG, "peerHost=" + session.getPeerHost());
		Log.i(TAG, "peerPort=" + session.getPeerPort());
		Log.i(TAG, "packetBufferSize=" + session.getPacketBufferSize());
		Log.i(TAG, "applicationBufferSize=" + session.getApplicationBufferSize());
	}
}
