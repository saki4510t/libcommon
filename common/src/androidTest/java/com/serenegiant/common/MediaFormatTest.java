package com.serenegiant.common;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

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
}
