package com.serenegiant.system;
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

import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;

public final class BuildCheck {

	private BuildCheck() {
		// インスタンス化をエラーにするためにデフォルトコンストラクタをprivateに
	}

	@ChecksSdkIntAtLeast(parameter = 0)
	private static boolean check(final int value) {
		return (Build.VERSION.SDK_INT >= value);
	}

	/**
	 * Magic version number for a current development build,
	 * which has not yet turned into an official release. API=10000
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CUR_DEVELOPMENT)
	public static boolean isCurrentDevelopment() {
		return check((Build.VERSION_CODES.CUR_DEVELOPMENT));
	}

	/**
	 * API>=1
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BASE)
	public static boolean isAPI1() {
		return check((Build.VERSION_CODES.BASE));
	}

	/**
	 * October 2008: The original, first, version of Android.  Yay!, API>=1
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BASE)
	public static boolean isBase() {
		return check(Build.VERSION_CODES.BASE);
	}

	/**
	 * API>=2
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BASE_1_1)
	public static boolean isAPI2() {
		return check((Build.VERSION_CODES.BASE_1_1));
	}

	/**
	 * February 2009: First Android update, officially called 1.1., API>=2
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BASE_1_1)
	public static boolean isBase11() {
		return check(Build.VERSION_CODES.BASE_1_1);
	}

	/**
	 * API>=3
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CUPCAKE)
	public static boolean isAPI3() {
		return check((Build.VERSION_CODES.CUPCAKE));
	}

	/**
	 * May 2009: Android 1.5., API>=3
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CUPCAKE)
	public static boolean isCupcake() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/**
	 * May 2009: Android 1.5., API>=3
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CUPCAKE)
	public static boolean isAndroid1_5() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/**
	 * API>=4
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.DONUT)
	public static boolean isAPI4() {
		return check((Build.VERSION_CODES.DONUT));
	}

	/**
	 * September 2009: Android 1.6., API>=4
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.DONUT)
	public static boolean isDonut() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/**
	 * September 2009: Android 1.6., API>=4
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.DONUT)
	public static boolean isAndroid1_6() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/**
	 * API>=5
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR)
	public static boolean isAPI5() {
		return check((Build.VERSION_CODES.ECLAIR));
	}

	/**
	 * November 2009: Android 2.0, API>=5
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR)
	public static boolean isEclair() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/**
	 * November 2009: Android 2.0, API>=5
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR)
	public static boolean isAndroid2_0() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/**
	 * API>=6
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR_0_1)
	public static boolean isAPI6() {
		return check((Build.VERSION_CODES.ECLAIR_0_1));
	}

	/**
	 * December 2009: Android 2.0.1, API>=6
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR_0_1)
	public static boolean isEclair01() {
		return check(Build.VERSION_CODES.ECLAIR_0_1);
	}

	/**
	 * API>=7
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR_MR1)
	public static boolean isAPI7() {
		return check((Build.VERSION_CODES.ECLAIR_MR1));
	}

	/**
	 * January 2010: Android 2.1, API>=7
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ECLAIR_MR1)
	public static boolean isEclairMR1() {
		return check(Build.VERSION_CODES.ECLAIR_MR1);
	}

	/**
	 * API>=8
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.FROYO)
	public static boolean isAPI8() {
		return check((Build.VERSION_CODES.FROYO));
	}

	/**
	 * June 2010: Android 2.2, API>=8
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.FROYO)
	public static boolean isFroyo() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/**
	 * June 2010: Android 2.2, API>=8
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.FROYO)
	public static boolean isAndroid2_2() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/**
	 * API>=9
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD)
	public static boolean isAPI9() {
		return check((Build.VERSION_CODES.GINGERBREAD));
	}

	/**
	 * November 2010: Android 2.3, API>=9
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD)
	public static boolean isGingerBread() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/**
	 * November 2010: Android 2.3, API>=9
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD)
	public static boolean isAndroid2_3() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/**
	 * API>=10
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD_MR1)
	public static boolean isAPI10() {
		return check((Build.VERSION_CODES.GINGERBREAD_MR1));
	}

	/**
	 * February 2011: Android 2.3.3., API>=10
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD_MR1)
	public static boolean isGingerBreadMR1() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/**
	 * February 2011: Android 2.3.3., API>=10
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD_MR1)
	public static boolean isAndroid2_3_3() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/**
	 * API>=11
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB)
	public static boolean isAPI11() {
		return check((Build.VERSION_CODES.HONEYCOMB));
	}

	/**
	 * February 2011: Android 3.0., API>=11
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB)
	public static boolean isHoneyComb() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/**
	 * February 2011: Android 3.0., API>=11
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB)
	public static boolean isAndroid3() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/**
	 * API>=12
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR1)
	public static boolean isAPI12() {
		return check((Build.VERSION_CODES.HONEYCOMB_MR1));
	}

	/**
	 * May 2011: Android 3.1., API>=12
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR1)
	public static boolean isHoneyCombMR1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/**
	 * May 2011: Android 3.1., API>=12
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR1)
	public static boolean isAndroid3_1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/**
	 * API>=13
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR2)
	public static boolean isAPI13() {
		return check((Build.VERSION_CODES.HONEYCOMB_MR2));
	}

	/**
	 * June 2011: Android 3.2., API>=13
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR2)
	public static boolean isHoneyCombMR2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	/**
	 * June 2011: Android 3.2., API>=13
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.HONEYCOMB_MR2)
	public static boolean isAndroid3_2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isAPI14() {
		return check((Build.VERSION_CODES.ICE_CREAM_SANDWICH));
	}

	/**
	 * October 2011: Android 4.0., API>=14
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isIcecreamSandwich() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	/**
	 * October 2011: Android 4.0., API>=14
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isAndroid4() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	public static boolean isAPI15() {
		return check((Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1));
	}

	/**
	 * December 2011: Android 4.0.3., API>=15
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	public static boolean isIcecreamSandwichMR1() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	/**
	 * December 2011: Android 4.0.3., API>=15
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	public static boolean isAndroid4_0_3() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN)
	public static boolean isAPI16() {
		return check((Build.VERSION_CODES.JELLY_BEAN));
	}

	/**
	 * June 2012: Android 4.1., API>=16
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN)
	public static boolean isJellyBean() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	/**
	 * June 2012: Android 4.1., API>=16
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN)
	public static boolean isAndroid4_1() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static boolean isAPI17() {
		return check((Build.VERSION_CODES.JELLY_BEAN_MR1));
	}

	/**
	 * November 2012: Android 4.2, Moar jelly beans!, API>=17
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static boolean isJellyBeanMr1() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	/**
	 * November 2012: Android 4.2, Moar jelly beans!, API>=17
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static boolean isAndroid4_2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static boolean isAPI18() {
		return check((Build.VERSION_CODES.JELLY_BEAN_MR2));
	}

	/**
	 * July 2013: Android 4.3, the revenge of the beans., API>=18
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static boolean isJellyBeanMR2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/**
	 * July 2013: Android 4.3, the revenge of the beans., API>=18
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static boolean isAndroid4_3() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT)
	public static boolean isAPI19() {
		return check((Build.VERSION_CODES.KITKAT));
	}

	/**
	 * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT)
	public static boolean isKitKat() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	/**
	 * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT)
	public static boolean isAndroid4_4() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT_WATCH)
	public static boolean isAPI20() {
		return check((Build.VERSION_CODES.KITKAT_WATCH));
	}

	/**
	 * Android 4.4W: KitKat for watches, snacks on the run., API>=20
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.KITKAT_WATCH)
	public static boolean isKitKatWatch() {
		return check(Build.VERSION_CODES.KITKAT_WATCH);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isAPI21() {
		return check((Build.VERSION_CODES.LOLLIPOP));
	}

	/**
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isL() {
		return check(Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isLollipop() {
		return check(Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isAndroid5() {
		return check(Build.VERSION_CODES.LOLLIPOP);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static boolean isAPI22() {
		return check((Build.VERSION_CODES.LOLLIPOP_MR1));
	}

	/**
	 * Lollipop with an extra sugar coating on the outside!, API>=22
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static boolean isLollipopMR1() {
		return check((Build.VERSION_CODES.LOLLIPOP_MR1));
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
	public static boolean isAPI23() {
		return check((Build.VERSION_CODES.M));
	}

	/**
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
	public static boolean isM() {
		return check(Build.VERSION_CODES.M);
	}

	/**
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
	public static boolean isMarshmallow() {
		return check(Build.VERSION_CODES.M);
	}

	/**
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
	public static boolean isAndroid6() {
		return check(Build.VERSION_CODES.M);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	public static boolean isAPI24() {
		return check((Build.VERSION_CODES.N));
	}

	/**
	 * 虫歯の元, API >= 24
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	public static boolean isN() {
		return check(Build.VERSION_CODES.N);
	}

	/**
	 * 歯にくっつくやつ, API >= 24
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	public static boolean isNougat() {
		return check(Build.VERSION_CODES.N);
	}
	/**
	 * API >= 24
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	public static boolean isAndroid7() {
		return check(Build.VERSION_CODES.N);
	}
	
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
	public static boolean isAPI25() {
		return check((Build.VERSION_CODES.N_MR1));
	}

	/**
	 * API>=25
 	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
	public static boolean isNMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}
	
	/**
	 * API>=25
 	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
	public static boolean isNougatMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	public static boolean isAPI26() {
		return check((Build.VERSION_CODES.O));
	}

	/**
	 * おれおれぇー API>=26
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	public static boolean isO() {
		return check(Build.VERSION_CODES.O);
	}
	
	/**
	 * おれおれぇー API>=26
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	public static boolean isOreo() {
		return check(Build.VERSION_CODES.O);
	}
	
	/**
	 * おれおれぇー API>=26
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	public static boolean isAndroid8() {
		return check(Build.VERSION_CODES.O);
	}
	
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
	public static boolean isAPI27() {
		return check((Build.VERSION_CODES.O_MR1));
	}

	/**
	 * おれおれぇー API>=27
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
	public static boolean isOMR1() {
		return check(Build.VERSION_CODES.O_MR1);
	}

	/**
	 * おれおれぇー MR1 API>=27
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
	public static boolean isOreoMR1() {
		return check((Build.VERSION_CODES.O_MR1));
	}
	
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
	public static boolean isAPI28() {
		return check((Build.VERSION_CODES.P));
	}

	/**
	 * おっ！ぱい API>=28
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isP() {
		return check((Build.VERSION_CODES.P));
	}

	/**
	 * おっ！ぱい API>=28
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isPie() {
		return check((Build.VERSION_CODES.P));
	}

	/**
	 * おっ！ぱい API>=28
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isAndroid9() {
		return check((Build.VERSION_CODES.P));
	}

	/**
	 * きゅぅっ API>=29
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isAPI29() {
		return check((Build.VERSION_CODES.Q));
	}

	/**
	 * きゅぅっ API>=29
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isQ() {
		return check((Build.VERSION_CODES.Q));
	}

	/**
	 * きゅぅっ API>=29
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isAndroid10() {
		return check((Build.VERSION_CODES.Q));
	}

	/**
	 * あぁーる API>=30
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	public static boolean isAPI30() {
		return check((Build.VERSION_CODES.R));
	}

	/**
	 * あぁーる API>=30
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	public static boolean isAndroidR() {
		return check((Build.VERSION_CODES.R));
	}

	/**
	 * あぁーる API>=30
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	public static boolean isR() {
		return check((Build.VERSION_CODES.R));
	}

	/**
	 * あぁーる API>=30
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	public static boolean isAndroid11() {
		return check((Build.VERSION_CODES.R));
	}

	/**
	 * えすどす API>=31
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	public static boolean isAPI31() {
		return check((Build.VERSION_CODES.S));
	}

	/**
	 * えすどす API>=31
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	public static boolean isAndroidS() {
		return check((Build.VERSION_CODES.S));
	}

	/**
	 * えすどす API>=31
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	public static boolean isS() {
		return check((Build.VERSION_CODES.S));
	}

	/**
	 * えすどす API>=31
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	public static boolean isAndroid12() {
		return check(Build.VERSION_CODES.S);
	}

	/**
	 * えすぶいつぅ API>=32
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
	public static boolean isS2() {
		return check(Build.VERSION_CODES.S_V2);
	}

	/**
	 * えすぶいつぅ API>=32
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
	public static boolean isAndroid12S2() {
		return check(Build.VERSION_CODES.S_V2);
	}

	/**
	 * えすぶいつぅ API>=32
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
	public static boolean isAPI32() {
		return check(Build.VERSION_CODES.S_V2);
	}

	/**
	 * てぃらみっす API>=33
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	public static boolean isT() {
		return check(Build.VERSION_CODES.TIRAMISU);
	}

	/**
	 * てぃらみっす API>=33
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	public static boolean isTiramisu() {
		return check(Build.VERSION_CODES.TIRAMISU);
	}

	/**
	 * てぃらみっす API>=33
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	public static boolean isAndroid13() {
		return check(Build.VERSION_CODES.TIRAMISU);
	}

	/**
	 * てぃらみっす API>=33
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	public static boolean isAPI33() {
		return check(Build.VERSION_CODES.TIRAMISU);
	}

	/**
	 * けーきをひっくりかえすな API>=34
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	public static boolean isUpsizeDownCake() {
		return check(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
	}

	/**
	 * けーきをひっくりかえすな API>=34
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	public static boolean isAndroid14() {
		return check(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
	}

	/**
	 * けーきをひっくりかえすな API>=34
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	public static boolean isAPI34() {
		return check(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
	}

	/**
	 * バニラアイス API>=35
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = 35/*Build.VERSION_CODES.VANILLA_ICE_CREAM*/)
	public static boolean isAndroid15() {
		return check(35/*Build.VERSION_CODES.VANILLA_ICE_CREAM*/);
	}

	/**
	 * バニラアイス API>=35
	 * @return
	 */
	@ChecksSdkIntAtLeast(api = 35/*Build.VERSION_CODES.VANILLA_ICE_CREAM*/)
	public static boolean isAPI35() {
		return check(35/*Build.VERSION_CODES.VANILLA_ICE_CREAM*/);
	}
}
