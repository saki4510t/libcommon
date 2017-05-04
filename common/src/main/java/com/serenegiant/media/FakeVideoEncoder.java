package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
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

import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class FakeVideoEncoder extends AbstractFakeEncoder
	implements IVideoEncoder {

	protected int mWidth, mHeight;

	public FakeVideoEncoder(final String mime_type,
		final IRecorder recorder, final EncoderListener listener) {
		
		super(mime_type, recorder, listener);
	}
	
	public FakeVideoEncoder(final String mime_type,
		final IRecorder recorder, final EncoderListener listener, final int defaultFrameSz) {
		
		super(mime_type, recorder, listener, defaultFrameSz);
	}
	
	@Override
	public void prepare() throws Exception {
	}
	
	@Deprecated
	@Override
	public boolean isAudio() {
		return false;
	}
	
	@Override
	protected MediaFormat createOutputFormat(final byte[] csd,
		final int size, final int ix0, final int ix1) {
		
		final MediaFormat outFormat;
        if (ix0 >= 0) {
            outFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        	final ByteBuffer csd0 = ByteBuffer.allocateDirect(ix1 - ix0)
        		.order(ByteOrder.nativeOrder());
        	csd0.put(csd, ix0, ix1 - ix0);
        	csd0.flip();
            outFormat.setByteBuffer("csd-0", csd0);
            if (ix1 > ix0) {
				// FIXME ここのサイズはsize-ix1、今はたまたまix0=0だから大丈夫なのかも
            	final ByteBuffer csd1 = ByteBuffer.allocateDirect(size - ix1 + ix0)
            		.order(ByteOrder.nativeOrder());
            	csd1.put(csd, ix1, size - ix1 + ix0);
            	csd1.flip();
                outFormat.setByteBuffer("csd-1", csd1);
            }
        } else {
        	throw new RuntimeException("unexpected csd data came.");
        }
        return outFormat;
	}
	
	@Override
	public void setVideoSize(final int width, final int height)
		throws IllegalArgumentException, IllegalStateException {
	
		mWidth = width;
		mHeight = height;
	}
	
	@Override
	public int getWidth() {
		return mWidth;
	}
	
	@Override
	public int getHeight() {
		return mHeight;
	}
}
