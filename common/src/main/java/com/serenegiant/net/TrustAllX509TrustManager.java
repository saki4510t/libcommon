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

import android.text.TextUtils;
import android.util.Log;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

import androidx.annotation.Nullable;

/**
 * SSL/TLSの証明書チェーンを常に信用するX509TrustManager実装
 * テストサーバー等でSSL/TLSの証明書のエラーで接続できない時に一時的にごまかすために使う
 * 本番環境では使わないこと
 */
public class TrustAllX509TrustManager implements X509TrustManager {
	private final boolean debug;
	private final String tag;

	public TrustAllX509TrustManager() {
		this(false, null);
	}

	public TrustAllX509TrustManager(final boolean debug) {
		this(debug, null);
	}

	public TrustAllX509TrustManager(final boolean debug, @Nullable final String tag) {
		this.debug = debug;
		this.tag = TextUtils.isEmpty(tag) ? TrustAllX509TrustManager.class.getSimpleName() : tag;
	}

	@Override
	public void checkClientTrusted(final X509Certificate[] chain,
		final String authType) throws CertificateException {

		if (debug) Log.v(tag, "checkClientTrusted:" + authType + "," + Arrays.toString(chain));
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain,
		final String authType) throws CertificateException {

		if (debug) Log.v(tag, "checkServerTrusted:" + authType + "," + Arrays.toString(chain));
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		if (debug) Log.v(tag, "getAcceptedIssuers:");
		return new X509Certificate[0];
	}
}
