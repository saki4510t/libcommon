package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.serenegiant.graphics.BitmapHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static com.serenegiant.common.TestUtils.*;

@RunWith(AndroidJUnit4.class)
public class BitmapHelperTest {
	private static final String TAG = BitmapHelperTest.class.getSimpleName();

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	/**
	 * パイプラインの接続・切断・検索が正常に動作するかどうかを確認
	 */
	@Test
	public void invert() {
		final Bitmap b0 = BitmapHelper.genMaskImage(
			0, 200, 200,60,
			Color.RED,0, 100);
		final Bitmap inverted = BitmapHelper.invertAlpha(b0);
		// 2回アルファ値を反転させると元と一致するはず
		assertTrue(bitMapEquals(b0, BitmapHelper.invertAlpha(inverted)));
	}

}
