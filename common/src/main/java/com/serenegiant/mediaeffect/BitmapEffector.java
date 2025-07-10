package com.serenegiant.mediaeffect;

import android.graphics.Bitmap;
import android.media.effect.EffectContext;
import android.util.Log;

import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLSurface;
import com.serenegiant.gl.GLUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * IMediaEffectをBitmapへ適用するためのヘルパークラス
 */
public class BitmapEffector {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = BitmapEffector.class.getSimpleName();

	private static final EffectsBuilder EMPTY_BUILDER = new EffectsBuilder() {
		@NonNull
		@Override
		public List<IMediaEffect> buildEffects(@NonNull final EffectContext effectContext) {
			return EffectsBuilder.super.buildEffects(effectContext);
		}
	};

	/**
	 * 非同期的に映像効果付与を行う場合の完了コールバック
	 */
	public interface Callback {
		/**
		 * @param bitmap 映像効果付与中にエラーが発生した倍はnull
		 */
		public void onReady(@Nullable final Bitmap bitmap);
	}

	@NonNull
	private final GLManager mGLManager;
	private final boolean mOwnGLManager;
	@NonNull
	private EffectsBuilder mEffectBuilder = EMPTY_BUILDER;

	/**
	 * コンストラクタ
	 */
	public BitmapEffector() {
		this(null, null);
	}

	/**
	 * コンストラクタ
	 * @param glManager
	 */
	public BitmapEffector(@NonNull final GLManager glManager) {
		this(glManager, null);
	}

	/**
	 * コンストラクタ
	 * @param builder
	 */
	public BitmapEffector(@NonNull final EffectsBuilder builder) {
		this(null, builder);
	}

	/**
	 * コンストラクタ
	 * @param glManager
	 * @param builder
	 */
	public BitmapEffector(@Nullable final GLManager glManager, @Nullable final EffectsBuilder builder) {
		mOwnGLManager = (glManager == null);
		mGLManager = (glManager != null) ? glManager : new GLManager();
		if (builder != null) {
			mEffectBuilder = builder;
		}
	}

	public void release() {
		if (mOwnGLManager) {
			mGLManager.release();
		}
	}

	/**
	 * 映像効果を変更
	 * @param builder
	 * @throws IllegalStateException
	 */
	@AnyThread
	public void changeEffects(@NonNull final EffectsBuilder builder) throws IllegalStateException {
		if (mGLManager.isValid()) {
			mGLManager.runOnGLThread(new Runnable() {
				@Override
				public void run() {
					mEffectBuilder = builder;
				}
			});
		} else {
			throw new IllegalStateException("GLManager not ready, already released?");
		}
	}

	/**
	 * 同期的にビットマップへ映像効果を付与する
	 * 処理が完了するかタイムアウトするまで呼び出し元のスレッドをブロックする
	 * @param src
	 * @param maxWaitMs
	 * @return
	 * @throws InterruptedException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException 映像効果付与中にエラーが発生した
	 * @throws TimeoutException タイムアウトした
	 */
	@AnyThread
	@NonNull
	public Bitmap apply(@NonNull final Bitmap src, final long maxWaitMs)
		throws InterruptedException, IllegalStateException, IllegalArgumentException, TimeoutException {

		if (DEBUG) Log.v(TAG, "apply:src=" + src + ",maxWaitMs=" + maxWaitMs);
		if (mGLManager.isValid()) {
			final AtomicReference<Bitmap> result = new AtomicReference<>(src);
			final CountDownLatch latch = new CountDownLatch(1);
			apply(src, new Callback() {
				@Override
				public void onReady(@Nullable final Bitmap bitmap) {
					result.set(bitmap);
					latch.countDown();
				}
			});
			if (maxWaitMs > 0) {
				if (!latch.await(maxWaitMs, TimeUnit.MILLISECONDS)) {
					throw new TimeoutException();
				}
			} else {
				latch.await();
			}
			final Bitmap r = result.get();
			if (r == null) {
				throw new IllegalArgumentException("Failed to apply media effect");
			}
			return r;
		} else {
			throw new IllegalStateException("GLManager not ready, already released?");
		}
	}

	/**
	 * 非同期的にビットマップへ映像効果を付与する
	 * 結果は
	 * @param src
	 * @param callback
	 */
	public void apply(@NonNull final Bitmap src, @NonNull final Callback callback) {
		if (DEBUG) Log.v(TAG, "apply:src=" + src);
		if (mGLManager.isValid()) {
			mGLManager.runOnGLThread(new Runnable() {
				@Override
				public void run() {
					try {
						mGLManager.makeDefault(0xffff0000);
						final int width = src.getWidth();
						final int height = src.getHeight();
						final EffectContext effectContext = EffectContext.createWithCurrentGlContext();
						final MediaImageSource source = new MediaImageSource(mGLManager.isGLES3(), src);
						try {
							final List<IMediaEffect> effects = new ArrayList<>(mEffectBuilder.buildEffects(effectContext));
							if (DEBUG) Log.v(TAG, "apply:" + effects);
							// IMediaEffectを適用
							for (final IMediaEffect effect : effects) {
								if (DEBUG) Log.v(TAG, "apply:enabled=" + effect.enabled() + "," + effect);
								if (effect.enabled()) {
									source.apply(effect);
								}
							}
						} finally {
							effectContext.release();
						}
						// ビットマップとして取得
						final GLSurface output = source.getResultTexture();
						output.makeCurrent();
						// FIXME 映像効果を付与したビットマップを取得できない
						//       ここでglClearで塗りつぶすとBitmapに反映されるので
						//       オフスクリーンからBitmapへの変換は問題無さそう
//						final Bitmap result = GLUtils.glCopyTextureToBitmap(
//							output.isOES(), source.getWidth(), source.getHeight(),
//							source.getOutputTexId(), source.getTexMatrix(),
//							null);
						final Bitmap result = GLUtils.glReadPixelsToBitmap(null, width, height);
						if (DEBUG) Log.v(TAG, "apply:result=" + result);
						callback.onReady(result);
					} catch (final Exception e) {
						Log.d(TAG, "apply:", e);
						callback.onReady(null);
					}
				}
			});
		} else {
			throw new IllegalStateException("GLManager not ready, already released?");
		}
	}
}
