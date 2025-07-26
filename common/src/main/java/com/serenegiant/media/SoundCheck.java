package com.serenegiant.media;
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

import com.serenegiant.utils.ThreadUtils;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 音量取得のためのヘルパークラス
 */
public class SoundCheck {

	public interface SoundCheckCallback {
		public void onStart();
		public void onStop();
		/**
		 * このメソッドは任意のスレッド上で呼び出される可能性がある
		 * @param amplitude
		 */
		public void onCheck(int amplitude);
	}

	private static SoundCheck mSoundCheck = null;

	/**
	 * シングルトンパターンでアクセスできるようにインスタンス取得のためのヘルパーメソッド
	 * @return
	 */
	public static synchronized SoundCheck getInstance() {
		if (mSoundCheck == null) {
			mSoundCheck = new SoundCheck();
		}
		return mSoundCheck;
	}

	@Nullable
	private IAudioSampler mAudioSampler;
	@Nullable
    private SoundCheckCallback mSoundCheckCallback;

    private SoundCheck() {
	}

	/**
	 * 音量取得中かどうか
	 * @return
	 */
	public boolean isRunning() {
		return mSoundCheckRunning;
	}

	/**
	 * IAudioSamplerをクリアして音量チェックを終了する
	 * #setAudioSampler(null, null)のシノニム
	 */
	public void clearSampler() {
		setAudioSampler(null, null);
	}

	/**
	 * IAudioSamplerとコールバック(SoundCheckCallback)をセット
	 * @param sampler
	 * @param callback
	 */
	public void setAudioSampler(@Nullable final IAudioSampler sampler, @Nullable final SoundCheckCallback callback) {
		if (mAudioSampler != sampler) {
			mSoundCheckRunning = false;
			if (mAudioSampler != null) {
				synchronized (mSoundSync) {
					mSoundSync.notify();
				}
				ThreadUtils.NoThrowSleep(10L);	// #onStopコールバックを呼び出せるように一瞬待機
				mAudioSampler.removeCallback(mSoundSamplerCallback);
			}
			mAudioSampler = sampler;
			mSoundCheckCallback = callback;
			if (sampler != null) {
				mAudioSampler.addCallback(mSoundSamplerCallback);
	    		mSoundCheckRunning = true;
	    		new Thread(mSoundCheckTask, "SoundCheck").start();
			}
		}
	}

	public IAudioSampler getAudioSampler() {
		return mAudioSampler;
	}

	private final Object mSoundSync = new Object();
	private volatile boolean mBusy;
	private volatile boolean mSoundCheckRunning;
	private short[] mSoundBuffer;
	private int mSoundBufferSize;

	private static final long MIN_SOUND_CHECK_INTERVAL_US = 300 * 1000;	// 300ミリ秒=1秒約3回
	private final AudioSampler.AudioSamplerCallback mSoundSamplerCallback
		= new AudioSampler.AudioSamplerCallback() {

		private long prevSamplingUs;

		@Override
		public void onData(@NonNull final ByteBuffer buffer, final long presentationTimeUs) {
			if (mBusy || (presentationTimeUs - prevSamplingUs < MIN_SOUND_CHECK_INTERVAL_US)) return;
			mBusy = true;
			synchronized (mSoundSync) {
				prevSamplingUs = presentationTimeUs;
				mSoundBufferSize = buffer.remaining() / 2;
				final ShortBuffer buf = buffer.asShortBuffer();	// FIXME 16ビットPCMのみ対応
				if ((mSoundBuffer == null) || (mSoundBuffer.length < mSoundBufferSize)) {
					mSoundBuffer = new short[mSoundBufferSize];
				}
				buf.get(mSoundBuffer, 0, mSoundBufferSize);
				mSoundSync.notify();
			}
		}

		@Override
		public void onError(@NonNull final Throwable t) {
//			Log.w(TAG, "onError:" + t);
		}
	};

	private final Runnable mSoundCheckTask = new Runnable() {
		@Override
		public void run() {
			final SoundCheckCallback callback = mSoundCheckCallback;
			if (callback != null) {
				try {
					callback.onStart();
				} catch (final Exception e) {
//					Log.w(TAG, "error on #onStart:", e);
				}
			}
			for ( ; mSoundCheckRunning ; ) {
				synchronized (mSoundSync) {
					if (mBusy) {
						if (mSoundCheckCallback != null) {
							try {
								mSoundCheckCallback.onCheck(calcAmplitude(mSoundBuffer, mSoundBufferSize));
							} catch (final Exception e) {
//								Log.w(TAG, "error on #onCheck:", e);
							}
						}
						mBusy = false;
					} else {
						try {
							mSoundSync.wait(300);
						} catch (final InterruptedException e) {
							break;
						}
					}
				}
			} // for
			if (callback != null) {
				try {
					callback.onStop();
				} catch (final Exception e) {
//					Log.w(TAG, "error on #onStop:", e);
				}
			}
		}
	};

	private static final int calcAmplitude(final short [] buffer, final int size) {
		double amp = 0;
		for (int i = 0; i < size; i++) {
			final short a = buffer[i];
			amp = amp + a * a;
		}
		amp = Math.sqrt(amp / size);
		if (amp == 0) amp = 1;
		return (int)(Math.log10(amp) * 30) - 30;
	}

}
