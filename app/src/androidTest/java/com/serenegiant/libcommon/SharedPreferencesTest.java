package com.serenegiant.libcommon;

import android.content.Context;
import android.content.SharedPreferences;

import com.serenegiant.preference.PrefHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * com.serenegiant.preference.PrefHelperのインスツルメンテーションテスト用クラス
 */
@RunWith(AndroidJUnit4.class)
public class SharedPreferencesTest {
	private static final String TAG = SharedPreferencesTest.class.getSimpleName();

	/**
	 * PrefHelper#copyのテスト
	 * コピー先の共有プレファレンスが空の場合=コピーされる
	 */
	@Test
	public void copyTest1() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src,0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());

		PrefHelper.copy(src, dst);	// 常に上書きする
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 0);
		compare(src, dst, true);
	}

	/**
	 * PrefHelper#copyのテスト
	 * コピー先の共有プレファレンスに同じキーがある場合でも上書きされることを確認
	 */
	@Test
	public void copyTest2() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src,0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst,0, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);

		PrefHelper.copy(src, dst);	// 常に上書きする
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 0);
		compare(src, dst, true);
	}

	/**
	 * PrefHelper#copyのテスト
	 * キーが全て異なる場合は全てコピーされることを確認
	 */
	@Test
	public void copyTest3() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src,0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst,1000, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 1000, 1);

		PrefHelper.copy(src, dst);	// 常に上書きする
		assertEquals(1200, dst.getAll().size());
		check(dst, 0, 0);
		check(dst, 1000, 1);
		compare(src, dst, true);
	}

	/**
	 * PrefHelper#mergeのテスト
	 * コピー先の共有プレファレンスが空の場合=コピーされる
	 */
	@Test
	public void mergeTest1() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src, 0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());

		PrefHelper.merge(src, dst);	// 同じキーに対応する値は上書きしない
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 0);
		compare(src, dst, true);
	}

	/**
	 * PrefHelper#mergeのテスト
	 * コピー先の共有プレファレンスの同じキーに値がセットされているとき=コピーされない
	 */
	@Test
	public void mergeTest2() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src, 0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst, 0, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);

		PrefHelper.merge(src, dst);	// 同じキーに対応する値は上書きしない
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);
		compare(src, dst, false);
	}

	/**
	 * PrefHelper#mergeのテスト
	 * キーが全て異なる場合は全てコピーされることを確認
	 */
	@Test
	public void mergeTest3() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src, 0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst, 1000, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 1000, 1);

		PrefHelper.merge(src, dst);	// 同じキーに対応する値は上書きしない
		assertEquals(1200, dst.getAll().size());
		check(dst, 0, 0);
		check(dst, 1000, 1);
		compare(src, dst, true);
	}

	/**
	 * PrefHelper#mergeのテスト
	 * コピー先の共有プレファレンスの同じキーに値がセットされているとき=上書きするようにコールバックを指定
	 */
	@Test
	public void mergeTest4() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src, 0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst, 0, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);

		PrefHelper.merge(src, dst, new PrefHelper.SharedPreferencesMergeCallback() {
			@Override
			public boolean onMerge(
				@NonNull final String key,
				final boolean contains,
				final Object srcValue, final Object dstValue) {
				// 同じキーに対応する値を上書きしない
				return false;
			}
		});
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);
		compare(src, dst, false);
	}

	/**
	 * PrefHelper#mergeのテスト
	 * コピー先の共有プレファレンスの同じキーに値がセットされているとき=上書きするようにコールバックを指定
	 */
	@Test
	public void mergeTest5() {
		final Context context = ApplicationProvider.getApplicationContext();
		final SharedPreferences src = context.getSharedPreferences("SRC", 0);
		src.edit().clear().apply();
		assertEquals(0, src.getAll().size());
		init(src, 0, 0);
		assertEquals(600, src.getAll().size());
		check(src, 0, 0);

		final SharedPreferences dst = context.getSharedPreferences("DST", 0);
		dst.edit().clear().apply();
		assertEquals(0, dst.getAll().size());
		init(dst, 0, 1);
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 1);

		PrefHelper.merge(src, dst, new PrefHelper.SharedPreferencesMergeCallback() {
			@Override
			public boolean onMerge(
				@NonNull final String key, final boolean contains,
				final Object srcValue, final Object dstValue) {
				// 同じキーに対応する値を上書きする
				return true;
			}
		});
		assertEquals(600, dst.getAll().size());
		check(dst, 0, 0);
		compare(src, dst, true);
	}

//--------------------------------------------------------------------------------
	/**
	 * 指定した共有プレファレンスへ規定値をセット
	 * 6種類(boolean, int, float, long, string, set<string>)を各100個ずつセット
	 * @param preferences
	 * @param keyOffset セットするキーのオフセットを指定
	 * @param valueOffset セットする値のオフセットを指定
	 */
	private void init(
		@NonNull final SharedPreferences preferences,
		final int keyOffset, final int valueOffset) {

		final SharedPreferences.Editor editor = preferences.edit();
		try {
			editor.clear().apply();
			for (int i = 0; i < 100; i++) {
				editor.putBoolean("BOOLEAN" + (i + keyOffset), (i + valueOffset) % 2 == 0);
				editor.putInt("INT" + (i + keyOffset), (i + valueOffset));
				editor.putFloat("FLOAT" + (i + keyOffset), (i + valueOffset) * 123.0f);
				editor.putLong("LONG" + (i + keyOffset), (i + valueOffset) * 456L);
				editor.putString("STRING" + (i + keyOffset), Integer.toString((i + valueOffset) * 789));
				final Set<String> set = new HashSet<>();
				for (int j = 0; j < 10; j++) {
					set.add(Integer.toString(i + valueOffset + j));
				}
				editor.putStringSet("SET" + (i + keyOffset), set);
			}
		} finally {
			editor.apply();
		}
	}

	/**
	 * 指定した共有プレファレンスに規定値がセットされているかどうかを確認
	 * @param preferences
	 * @param keyOffset セットするキーのオフセットを指定
	 * @param valueOffset セットされているはずの値のオフセット
	 */
	private void check(
		@NonNull final SharedPreferences preferences,
		final int keyOffset, final int valueOffset) {

		final int offset2 = valueOffset + 10;
		for (int i = 0; i < 100; i++) {
			assertEquals((i + valueOffset) % 2 == 0, preferences.getBoolean("BOOLEAN" + (i + keyOffset), (i + offset2) % 2 == 0));
			assertEquals((i + valueOffset), preferences.getInt("INT" + (i + keyOffset), i + offset2));
			assertEquals((i + valueOffset) * 123.0f, preferences.getFloat("FLOAT" + (i + keyOffset), (i + offset2) *123.0f), 0.0001f);
			assertEquals((i + valueOffset) * 456L, preferences.getLong("LONG" + (i + keyOffset), (i + offset2) * 456L));
			assertEquals(Integer.toString((i + valueOffset) * 789), preferences.getString("STRING" + (i + keyOffset), Integer.toString((i + offset2) * 789)));
			final Set<String> set = new HashSet<>();
			final Set<String> wrongSet = new HashSet<>();
			for (int j = 0; j < 10; j++) {
				set.add(Integer.toString((i + valueOffset) + j));
				wrongSet.add(Integer.toString(i + j + offset2));
			}
			assertEquals(set, preferences.getStringSet("SET" + (i + keyOffset), wrongSet));
		}
	}

	/**
	 * srcで指定した共有プレファレンスの値が全てdstで指定した共有プレファレンスへセットされているかどうかを確認
	 * @param src
	 * @param dst
	 * @param isSame 同じ値かどうか
	 */
	private void compare(
		@NonNull final SharedPreferences src,
		@NonNull final SharedPreferences dst,
		final boolean isSame) {

		final Map<String, ?> srcValues = src.getAll();
		final Map<String, ?> dstValues = dst.getAll();
		for (final Map.Entry<String, ?> srcEntry: srcValues.entrySet()) {
			final String key = srcEntry.getKey();
			assertTrue(dst.contains(key));
			final Object srcValue = srcEntry.getValue();
			final Object dstValue = dstValues.get(key);
			if (isSame) {
				assertEquals(srcValue, dstValue);
			} else {
				assertNotEquals(srcValue, dstValue);
			}
		}
	}
}
