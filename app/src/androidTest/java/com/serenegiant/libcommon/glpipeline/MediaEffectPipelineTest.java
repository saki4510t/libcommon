package com.serenegiant.libcommon.glpipeline;
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

import android.content.Context;
import android.graphics.Bitmap;
import android.media.effect.EffectContext;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.gl.GLManager;
import com.serenegiant.glpipeline.GLPipeline;
import com.serenegiant.glpipeline.GLPipelineSurfaceSource;
import com.serenegiant.glpipeline.ImageSourcePipeline;
import com.serenegiant.glpipeline.MediaEffectPipeline;
import com.serenegiant.glpipeline.SurfaceSourcePipeline;
import com.serenegiant.glutils.GLSurfaceReceiver;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.mediaeffect.EffectsBuilder;
import com.serenegiant.mediaeffect.IMediaEffect;
import com.serenegiant.mediaeffect.MediaEffectAutoFix;
import com.serenegiant.mediaeffect.MediaEffectBackDropper;
import com.serenegiant.mediaeffect.MediaEffectBitmapOverlay;
import com.serenegiant.mediaeffect.MediaEffectBlackWhite;
import com.serenegiant.mediaeffect.MediaEffectContrast;
import com.serenegiant.mediaeffect.MediaEffectCrop;
import com.serenegiant.mediaeffect.MediaEffectCrossProcess;
import com.serenegiant.mediaeffect.MediaEffectDocumentary;
import com.serenegiant.mediaeffect.MediaEffectDuoTone;
import com.serenegiant.mediaeffect.MediaEffectFillLight;
import com.serenegiant.mediaeffect.MediaEffectFishEye;
import com.serenegiant.mediaeffect.MediaEffectFlip;
import com.serenegiant.mediaeffect.MediaEffectGLAlphaBlend;
import com.serenegiant.mediaeffect.MediaEffectGLBrightness;
import com.serenegiant.mediaeffect.MediaEffectGLCanny;
import com.serenegiant.mediaeffect.MediaEffectGLDilation;
import com.serenegiant.mediaeffect.MediaEffectGLEffect;
import com.serenegiant.mediaeffect.MediaEffectGLEmboss;
import com.serenegiant.mediaeffect.MediaEffectGLErosion;
import com.serenegiant.mediaeffect.MediaEffectGLExposure;
import com.serenegiant.mediaeffect.MediaEffectGLExtraction;
import com.serenegiant.mediaeffect.MediaEffectGLMaskedAlphaBlend;
import com.serenegiant.mediaeffect.MediaEffectGLPosterize;
import com.serenegiant.mediaeffect.MediaEffectGLSaturate;
import com.serenegiant.mediaeffect.MediaEffectGLTexProjection;
import com.serenegiant.mediaeffect.MediaEffectGrain;
import com.serenegiant.mediaeffect.MediaEffectGrayScale;
import com.serenegiant.mediaeffect.MediaEffectLomoish;
import com.serenegiant.mediaeffect.MediaEffectNegative;
import com.serenegiant.mediaeffect.MediaEffectNull;
import com.serenegiant.mediaeffect.MediaEffectRedEye;
import com.serenegiant.mediaeffect.MediaEffectRotate;
import com.serenegiant.mediaeffect.MediaEffectSaturate;
import com.serenegiant.mediaeffect.MediaEffectSepia;
import com.serenegiant.mediaeffect.MediaEffectSharpen;
import com.serenegiant.mediaeffect.MediaEffectStraighten;
import com.serenegiant.mediaeffect.MediaEffectTemperature;
import com.serenegiant.mediaeffect.MediaEffectTint;
import com.serenegiant.mediaeffect.MediaEffectVignette;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.gl.GLEffect.EFFECT_ADAPTIVE_BIN;
import static com.serenegiant.gl.GLEffect.EFFECT_KERNEL_SOBEL_HV;
import static com.serenegiant.gl.GLEffect.EFFECT_NON;
import static com.serenegiant.libcommon.TestUtils.createGLSurfaceReceiver;
import static com.serenegiant.libcommon.TestUtils.createImageReceivePipeline;
import static com.serenegiant.libcommon.TestUtils.inputImagesAsync;
import static com.serenegiant.libcommon.TestUtils.validatePipelineOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class MediaEffectPipelineTest {
	private static final String TAG = MediaEffectPipelineTest.class.getSimpleName();
	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;
	private static final int NUM_FRAMES = 50;

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

	@Test
	public void emptyEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void nullEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectNull(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void autoFixEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectAutoFix(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void backDropperEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectBackDropper(effectContext, ""/*FIXME ちゃんとしたURL文字列を渡す*/));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void bitmapOverlayEffectTest() {
		final Bitmap bitmap = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT,15, 18,
			Bitmap.Config.ARGB_8888);
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectBitmapOverlay(effectContext,
					bitmap.copy(Bitmap.Config.ARGB_8888, false)));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void blackWhiteEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectBlackWhite(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void contrastEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectContrast(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void cropEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectCrop(effectContext, 0, 0, 10, 10));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void crossProcessEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectCrossProcess(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void documentaryEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectDocumentary(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void duoToneEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectDuoTone(effectContext, 0xffff0000, 0xff00ff00));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void fillLightEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectFillLight(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void fishEyeEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectFishEye(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void flipEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectFlip(effectContext, true, true));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glAlphaBlendEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLAlphaBlend(0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glBrightnessEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLBrightness(0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glCannyEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLCanny(0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glDilationEffectTest() {
		for (int i = 0; i < 5; i++) {
			final int radius = i;
			final EffectsBuilder builder
				= new EffectsBuilder() {
				@NonNull
				public List<IMediaEffect> buildEffects(
					@NonNull final EffectContext effectContext) {
					final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
					result.add(new MediaEffectGLDilation(radius));
					return result;
				}
			};
			mediaEffectPipelineTest(builder);
		}
	}

	@Test
	public void glEmbossEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLEmboss(5));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glErosionEffectTest() {
		for (int i = 0; i < 5; i++) {
			final int radius = i;
			final EffectsBuilder builder
				= new EffectsBuilder() {
				@NonNull
				public List<IMediaEffect> buildEffects(
					@NonNull final EffectContext effectContext) {
					final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
					result.add(new MediaEffectGLErosion(radius));
					return result;
				}
			};
			mediaEffectPipelineTest(builder);
		}
	}

	@Test
	public void glExposureEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLExposure(5.0f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glExtractionEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLExtraction());
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glMaskedAlphaBlendEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLMaskedAlphaBlend());
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glPosterizeEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLPosterize(127));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glSaturateEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLSaturate(0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glTexProjectionEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLTexProjection());
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void grainEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGrain(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void grayScaleEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGrayScale(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void lomoishEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectLomoish(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void negativeEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectNegative(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void redEyeEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectRedEye(effectContext, new float[] { 0.0f, 1.0f, 2.0f, 4.0f }/*FIXME このパラメータは適当*/));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void rotateEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectRotate(effectContext, 90));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void saturateEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectSaturate(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void sepiaEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectSepia(effectContext));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void sharpenEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectSharpen(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void straightenEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectStraighten(effectContext, 30.0f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void temperatureEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectTemperature(effectContext, 0.75f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void tintEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectTint(effectContext, 0xffff0000));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void vignetteEffectTest() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectVignette(effectContext, 0.5f));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glEffectEffectTestNon() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLEffect(EFFECT_NON));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glEffectEffectTestAdaptiveBin() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLEffect(EFFECT_ADAPTIVE_BIN));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	@Test
	public void glEffectEffectTestSobelHV() {
		final EffectsBuilder builder
			= new EffectsBuilder() {
			@NonNull
			public List<IMediaEffect> buildEffects(
				@NonNull final EffectContext effectContext) {
				final List<IMediaEffect> result = new ArrayList<IMediaEffect>();
				result.add(new MediaEffectGLEffect(EFFECT_KERNEL_SOBEL_HV));
				return result;
			}
		};
		mediaEffectPipelineTest(builder);
	}

	//--------------------------------------------------------------------------------
	private void mediaEffectPipelineTest(
		@NonNull final EffectsBuilder builder) {
		mediaEffectPipelineTest1(builder);
		mediaEffectPipelineTest2(builder);
		mediaEffectPipelineTest3(builder);
		mediaEffectPipelineOESTest1(builder);
		mediaEffectPipelineOESTest2(builder);
		mediaEffectPipelineOESTest3(builder);
		drawerPipelineOESTestWithSurface1(builder);
		mediaEffectPipelineWithSurfaceTest1(builder);
	}

	/**
	 * MediaEffectPipelineが動作するかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ MediaEffectPipeline
	 * 				→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	private void mediaEffectPipelineTest1(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineを2つ連結処理したときに想定通りに動作になるかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ MediaEffectPipeline
	 * 				→ MediaEffectPipeline
	 * 					→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	private void mediaEffectPipelineTest2(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline2 = new MediaEffectPipeline(manager, builder);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(pipeline2);
		pipeline2.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			// パイプラインを経由して読み取った映像データをビットマップに戻す
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineを3つ連結処理したときに想定通りに動作になるかどうかを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ MediaEffectPipeline
	 * 				→ MediaEffectPipeline
	 * 					→ MediaEffectPipeline
	 * 						→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	private void mediaEffectPipelineTest3(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);

		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline2 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline3 = new MediaEffectPipeline(manager, builder);

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		source.setPipeline(pipeline1);
		pipeline1.setPipeline(pipeline2);
		pipeline2.setPipeline(pipeline3);
		pipeline3.setPipeline(proxy);

		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, pipeline3, proxy));

		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
//			dump(resultBitmap);
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → MediaEffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	private void mediaEffectPipelineOESTest1(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "MediaEffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "MediaEffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するMediaEffectPipelineを生成
		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → MediaEffectPipeline → MediaEffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	private void mediaEffectPipelineOESTest2(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "MediaEffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "MediaEffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するMediaEffectPipelineを生成
		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline2 = new MediaEffectPipeline(manager, builder);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, pipeline2);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → MediaEffectPipeline → MediaEffectPipeline → MediaEffectPipeline
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 */
	private void mediaEffectPipelineOESTest3(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final AtomicInteger cnt = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result, cnt);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "MediaEffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "MediaEffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するMediaEffectPipelineを生成
		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline2 = new MediaEffectPipeline(manager, builder);
		final MediaEffectPipeline pipeline3 = new MediaEffectPipeline(manager, builder);

		GLPipeline.append(source, pipeline1);
		GLPipeline.append(source, pipeline2);
		GLPipeline.append(source, pipeline3);
		GLPipeline.append(source, proxy);
		assertTrue(validatePipelineOrder(source, source, pipeline1, pipeline2, pipeline3, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
//			dump(result);
			assertEquals(NUM_FRAMES, cnt.get());
			final Bitmap resultBitmap = result.get();
			assertNotNull(resultBitmap);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineへ繋いだProxyPipelineとMediaEffectPipelineへセットしたSurfaceの
	 * 両方へ映像が転送されることを検証
	 * 映像ソースがImageSourcePipelineなのでGL_TEXTURE_2D
	 * (FIXME 個別の映像効果付与が想定通りかどうかは未検証)
	 * Bitmap → ImageSourcePipeline
	 * 			→ MediaEffectPipeline
	 * 				↓
	 * 				→ ProxyPipeline	→ GLSurface.wrap → glReadPixels → Bitmap
	 * 				→ (Surface) → GLSurfaceReceiver	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	private void mediaEffectPipelineWithSurfaceTest1(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		// 映像ソースを生成
		final ImageSourcePipeline source = new ImageSourcePipeline(manager, original, null);
		// テスト対象のMediaEffectPipelineを生成
		final MediaEffectPipeline pipeline = new MediaEffectPipeline(manager, builder);

		final Semaphore sem = new Semaphore(0);

		// パイプラインを経由した映像の受け取り用にProxyPipelineを生成する
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(proxy);
		pipeline.setPipeline(proxy);

		// Surfaceを経由した映像の受け取り用にGLSurfaceReceiverを生成する
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver bitmapReceiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(bitmapReceiver);
		final Surface receiverSurface = bitmapReceiver.getSurface();
		assertNotNull(receiverSurface);
		pipeline.setSurface(receiverSurface);

		source.setPipeline(pipeline);
		assertTrue(validatePipelineOrder(source, source, pipeline, proxy));

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}

	/**
	 * MediaEffectPipelineが動作するかどうかを検証
	 * SurfaceSourcePipelineからの映像ソースなのでGL_TEXTURE_EXTERNAL_OESテクスチャ
	 * Bitmap → inputImagesAsync
	 * 				↓
	 * 				→ (Surface) → SurfaceSourcePipeline
	 * 								 → MediaEffectPipeline
	 * 								 	↓
	 * 								 	→ ProxyPipeline → テクスチャ読み取り
	 * 									→ (Surface) → GLSurfaceReceiver	→ GLSurface.wrap → glReadPixels → Bitmap
	 */
	private void drawerPipelineOESTestWithSurface1(
		@NonNull final EffectsBuilder builder) {
		// テストに使用するビットマップを生成
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(original);

		final GLManager manager = mManager;

		final Semaphore sem = new Semaphore(0);

		// 映像ソースを生成
		final SurfaceSourcePipeline source = new SurfaceSourcePipeline(
			manager, WIDTH, HEIGHT,
			new GLPipelineSurfaceSource.PipelineSourceCallback() {
				@Override
				public void onCreate(@NonNull final Surface surface) {
					Log.v(TAG, "MediaEffectPipelineTest#onCreate:" + surface);
					sem.release();
				}

				@Override
				public void onDestroy() {
					Log.v(TAG, "MediaEffectPipelineTest#onDestroy:");
				}
			});
		try {
			// Surfaceの生成を待機
			assertTrue(sem.tryAcquire(1200, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail();
		}
		final Surface inputSurface = source.getInputSurface();
		assertNotNull(inputSurface);

		// 検証するMediaEffectPipelineを生成
		final MediaEffectPipeline pipeline1 = new MediaEffectPipeline(manager, builder);

		// パイプラインを経由した映像の受け取り用にProxyPipelineを生成する
		final AtomicReference<Bitmap> result1 = new AtomicReference<>();
		final AtomicInteger cnt1 = new AtomicInteger();
		final GLPipeline proxy = createImageReceivePipeline(WIDTH, HEIGHT, NUM_FRAMES, sem, result1, cnt1);
		assertNotNull(proxy);
		pipeline1.setPipeline(proxy);

		// Surfaceを経由した映像の受け取り用にGLSurfaceReceiverを生成する
		final AtomicReference<Bitmap> result2 = new AtomicReference<>();
		final AtomicInteger cnt2 = new AtomicInteger();
		final GLSurfaceReceiver bitmapReceiver = createGLSurfaceReceiver(
			manager, WIDTH, HEIGHT, NUM_FRAMES, sem, result2, cnt2);
		assertNotNull(bitmapReceiver);
		final Surface receiverSurface = bitmapReceiver.getSurface();
		assertNotNull(receiverSurface);
		pipeline1.setSurface(receiverSurface);

		source.setPipeline(pipeline1);
		assertTrue(validatePipelineOrder(source, source, pipeline1, proxy));

		// 実際の映像はSurfaceを経由して映像を書き込む
		final AtomicBoolean requestStop = new AtomicBoolean();
		inputImagesAsync(original, inputSurface, NUM_FRAMES + 2, requestStop);

		try {
			assertTrue(sem.tryAcquire(2, NUM_FRAMES * 50L, TimeUnit.MILLISECONDS));
			requestStop.set(true);
			source.release();
			assertTrue(cnt1.get() >= NUM_FRAMES);
			assertTrue(cnt2.get() >= NUM_FRAMES);
			final Bitmap resultBitmap1 = result1.get();
			assertNotNull(resultBitmap1);
			final Bitmap resultBitmap2 = result2.get();
			assertNotNull(resultBitmap2);
		} catch (final InterruptedException e) {
			Log.d(TAG, "interrupted", e);
		}
	}
}
