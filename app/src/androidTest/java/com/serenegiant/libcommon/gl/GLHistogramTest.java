package com.serenegiant.libcommon.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gl.GLHistogram;
import com.serenegiant.gl.GLManager;
import com.serenegiant.gl.GLUtils;
import com.serenegiant.glutils.GLFrameAvailableCallback;
import com.serenegiant.glutils.GLTextureSource;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.graphics.MatrixUtils;
import com.serenegiant.math.Fraction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.libcommon.TestUtils.bitmapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class GLHistogramTest {
	private static final String TAG = GLHistogramTest.class.getSimpleName();
	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;
	private static final int NUM_FRAMES = 30;

	@Nullable
	private GLManager mManager;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
		mManager = new GLManager();
		assertTrue(mManager.isValid());
		mManager.getEgl();
		assertEquals(1, mManager.getMasterWidth());
		assertEquals(1, mManager.getMasterHeight());
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
		if (mManager != null) {
			mManager.release();
			mManager = null;
		}
	}

	/**
	 * Bitmap → GLTextureSource
	 * 			→ GLFrameAvailableCallback → GLSurface.wrap → glReadPixels → Bitmap
	 */
	@Test
	public void glHistogramTest1() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);

		final GLManager manager = mManager;

		final GLHistogram[] histogram = new GLHistogram[1];
		manager.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				histogram[0] = new GLHistogram(false);
			}
		});
		// 映像ソース用にImageTextureSourceを生成
		final GLTextureSource source = new GLTextureSource(manager, original, new Fraction(30));
		// 映像受け取り用にGLFrameAvailableCallbackをセット
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		source.setFrameAvailableListener(new GLFrameAvailableCallback() {
			@Override
			public void onFrameAvailable(
				final boolean isGLES3, final boolean isOES,
				final int width, final int height,
				final int texId, @NonNull final float[] texMatrix) {
				manager.makeDefault();
				histogram[0].compute(
					width, height,
					GLES20.GL_TEXTURE0, texId, texMatrix, 0);
				manager.makeDefault();
				histogram[0].draw(
					width, height,
					GLES20.GL_TEXTURE0, texId, texMatrix, 0);
				manager.swap();
				if (cnt.incrementAndGet() == NUM_FRAMES) {
					Log.v(TAG, "onFrameAvailable:create Bitmap from texture, texMatrix=" + MatrixUtils.toGLMatrixString(texMatrix));
					result.set(GLUtils.glCopyTextureToBitmap(
						false, width, height, texId, texMatrix, null));
					sem.release();
				}
			}
		});
		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			source.release();
			manager.runOnGLThread(new Runnable() {
				@Override
				public void run() {
					histogram[0].release();
				}
			});
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitmapEquals(original, resultBitmap));
		} catch (final InterruptedException e) {
			fail();
		}
	}
}
