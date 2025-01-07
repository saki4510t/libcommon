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

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.serenegiant.media.AbstractAudioEncoder;
import com.serenegiant.media.AudioRecordCompat;
import com.serenegiant.media.MediaCodecUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaFormatTest {
   private static final int VIDEO_WIDTH = 1280;
   private static final int VIDEO_HEIGHT = 720;

   /**
    * オフスクリーンでMediaCodecのエンコーダーでH264エンコードして
    * MediaFormatを取得するテスト
    */
   @Test
   public void testVideoMediaFormatTest() throws IOException {
      final MediaCodecInfo info = MediaCodecUtils.selectVideoEncoder(MediaCodecUtils.MIME_VIDEO_AVC);
      Assert.assertNotNull(info);
      final MediaFormat format = MediaCodecUtils.testVideoMediaFormat(
         MediaCodecUtils.MIME_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
      Assert.assertNotNull(format);
      MediaCodecUtils.dump(format);
      final ByteBuffer csd0 = format.getByteBuffer("csd-0");
      Assert.assertNotNull(csd0);
      Assert.assertTrue(csd0.capacity() > 0);
      final ByteBuffer csd1 = format.getByteBuffer("csd-1");
      Assert.assertNotNull(csd1);
      Assert.assertTrue(csd1.capacity() > 0);
      Assert.assertEquals(VIDEO_WIDTH, format.getInteger("width"));
      Assert.assertEquals(VIDEO_HEIGHT, format.getInteger("height"));
   }

   @Test
   public void testAudioMediaFormatTest() throws IOException {
      final MediaFormat format = MediaCodecUtils.testAudioMediaFormat(
         MediaCodecUtils.MIME_AUDIO_AAC,
          AudioRecordCompat.DEFAULT_SAMPLE_RATE,
          1, AbstractAudioEncoder.DEFAULT_BIT_RATE);
      Assert.assertNotNull(format);
      MediaCodecUtils.dump(format);
      final ByteBuffer csd0 = format.getByteBuffer("csd-0");
      Assert.assertNotNull(csd0);
      Assert.assertTrue(csd0.capacity() > 0);
   }
}
