package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2023 saki t_saki@serenegiant.com
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

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;

import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.HandlerUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertTrue;

/**
 * Choreographerのフレームコールバックの呼び出し周期を確認するテスト
 * 33までと60は大丈夫っぽいけどそれ以外は1-4割りぐらいずれる
 * でも画面に指で触れていればおよそ指定通りになることもあるみたい
 */
@RunWith(AndroidJUnit4.class)
public class ChoreographerTest {
   private static final String TAG = ChoreographerTest.class.getSimpleName();

   private static final int MAX_FRAMES = 120;
   private static final long MAX_WAIT_MS = 20000L;

   @Test
   public void countFrames() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      final AtomicInteger numFrames = new AtomicInteger();
      final CountDownLatch latch = new CountDownLatch(1);
      final Choreographer.FrameCallback callback
         = new Choreographer.FrameCallback() {
         @Override
         public void doFrame(final long frameTimeNanos) {
            final int n = numFrames.incrementAndGet();
            if (n < MAX_FRAMES) {
               Choreographer.getInstance().postFrameCallbackDelayed(this, 0);
            } else {
               latch.countDown();
            }
         }
      };
      final long startTimeNs = SystemClock.elapsedRealtimeNanos();
      asyncHandler.post(() -> {
         Choreographer.getInstance().postFrameCallbackDelayed(callback, 0);
      });
      try {
         assertTrue(latch.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
         final long endTimeNs = SystemClock.elapsedRealtimeNanos();
         final int n = numFrames.get();
         final float fps = (n * 1000000000f) / (endTimeNs - startTimeNs);
         Log.i(TAG, "numFrames=" + n);
         Log.i(TAG, "fps=" + fps);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      HandlerUtils.quit(asyncHandler);
   }

// API33未満の端末だとクラス読み込み時点でクラッシュして実行できない
//   @TargetApi(33)
//   @Test
//   public void countVsyncFrames() {
//      if (BuildCheck.isAPI33()) {
//         final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
//         final AtomicInteger numFrames = new AtomicInteger();
//         final CountDownLatch latch = new CountDownLatch(1);
//         final Choreographer.VsyncCallback callback
//            = new Choreographer.VsyncCallback() {
//            @Override
//            public void onVsync(@NonNull final Choreographer.FrameData data) {
//               final int n = numFrames.incrementAndGet();
//               if (n >= MAX_FRAMES) {
//                  Choreographer.getInstance().postVsyncCallback(this);
//               } else {
//                  Choreographer.getInstance().removeVsyncCallback(this);
//                  latch.countDown();
//               }
//            }
//         };
//         final long startTimeNs = SystemClock.elapsedRealtimeNanos();
//         asyncHandler.post(() -> {
//            Choreographer.getInstance().postVsyncCallback(callback);
//         });
//         try {
//            assertTrue(latch.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
//            final long endTimeNs = SystemClock.elapsedRealtimeNanos();
//            final int n = numFrames.get();
//            final float fps = (n * 1000000000f) / (endTimeNs - startTimeNs);
//            Log.i(TAG, "numFrames=" + n);
//            Log.i(TAG, "fps=" + fps);
//         } catch (InterruptedException e) {
//            e.printStackTrace();
//         }
//         HandlerUtils.quit(asyncHandler);
//      }
//   }

   @Test
   public void frameRate10() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 10);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate15() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 15);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate20() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 20);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate30() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 30);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate33() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 33);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate40() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 40);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate45() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 45);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate50() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG);
      frameRate(asyncHandler, 50);
      HandlerUtils.quit(asyncHandler);
   }

   @Test
   public void frameRate60() {
      final Handler asyncHandler = HandlerThreadHandler.createHandler(TAG, true);
      frameRate(asyncHandler, 60);
      HandlerUtils.quit(asyncHandler);
   }

   private static void frameRate(final Handler asyncHandler, final int requestFps) {
      final long frameIntervalNs = Math.round(1000000000.0 / requestFps);

      final AtomicInteger numFrames = new AtomicInteger();
      final CountDownLatch latch = new CountDownLatch(1);
      final Choreographer.FrameCallback callback
         = new Choreographer.FrameCallback() {
         private long firstTimeNs = -1L;
         @Override
         public void doFrame(final long frameTimeNanos) {
            final int n = numFrames.incrementAndGet();
            final Choreographer choreographer = Choreographer.getInstance();
            final long current = System.nanoTime();
            if (firstTimeNs < 0) {
               firstTimeNs = System.nanoTime();
            }
            long ms = (firstTimeNs + frameIntervalNs * (n + 1) - current) / 1000000L;
            if (ms < 5L) {
               ms = 0L;
            }
            if (n < MAX_FRAMES) {
               choreographer.postFrameCallbackDelayed(this, ms);
            } else {
               choreographer.removeFrameCallback(this);
               latch.countDown();
            }
         }
      };
      final long startTimeNs = SystemClock.elapsedRealtimeNanos();
      asyncHandler.post(() -> {
         Choreographer.getInstance().postFrameCallbackDelayed(callback, 0);
      });
      try {
         assertTrue(latch.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
         final long endTimeNs = SystemClock.elapsedRealtimeNanos();
         final int n = numFrames.get();
         final float fps = (n * 1000000000f) / (endTimeNs - startTimeNs);
         Log.i(TAG, "numFrames=" + n);
         Log.i(TAG, "fps=" + fps  + "/" + requestFps);
         // フレームレートが指定値の±10%以内にはいっているかどうか
         assertTrue((fps > requestFps * 0.90f) && (fps < requestFps * 1.1f));
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

}
