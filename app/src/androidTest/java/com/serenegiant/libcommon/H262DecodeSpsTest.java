package com.serenegiant.libcommon;
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

import android.util.Log;

import com.serenegiant.media.MediaCodecUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class H262DecodeSpsTest {
   private static final String TAG = H262DecodeSpsTest.class.getSimpleName();

   @Test
   public void decodeSpsTest1() {
      // width=368,height=640,fps=24,profile=100
      final byte[] buf = {(byte)0x67,(byte)0x64,(byte)0x00,(byte)0x1e,(byte)0xac,(byte)0xd3,(byte)0x05,(byte)0xc1,(byte)0x47,(byte)0x97,(byte)0x9b,(byte)0x81,(byte)0x01,(byte)0x02,(byte)0xa0,(byte)0x00,(byte)0x00,(byte)0x03,(byte)0x00,(byte)0x20,(byte)0x00,0x00,(byte)0x06,(byte)0x11,(byte)0xe2,(byte)0xc5,(byte)0xa7};
      final ByteBuffer sps = ByteBuffer.wrap(buf);
      final MediaCodecUtils.H264SpsConfig config = MediaCodecUtils.decodeSps(sps);
      Log.i(TAG, "result=" + config);
      assertNotNull(config);
      assertEquals(368, config.width);
      assertEquals(640, config.height);
      assertEquals(24, config.fps);
      assertEquals(100, config.profile);
   }

   @Test
   public void decodeSpsTest2() {
      // width=720,height=1280,fps=0,profile=100
      final byte[] buf = {(byte)0x67,(byte)0x64,(byte)0x00,(byte)0x1f,(byte)0xac,(byte)0x56,(byte)0xc0,(byte)0xb4,(byte)0x0a,(byte)0x1a,(byte)0x6e,(byte)0x04,(byte)0x04,(byte)0x0a,(byte)0x04,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x28,(byte)0xee,(byte)0x3c,(byte)0xb0};
      final ByteBuffer sps = ByteBuffer.wrap(buf);
      final MediaCodecUtils.H264SpsConfig config = MediaCodecUtils.decodeSps(sps);
      Log.i(TAG, "result=" + config);
      assertNotNull(config);
      assertEquals(720, config.width);
      assertEquals(1280, config.height);
      assertEquals(0, config.fps);
      assertEquals(100, config.profile);
   }
}
