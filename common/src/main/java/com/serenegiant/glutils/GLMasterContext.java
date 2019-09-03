package com.serenegiant.glutils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2018-2019 saki t_saki@serenegiant.com
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

import androidx.annotation.WorkerThread;

/**
 * Created by saki on 2018/02/10.
 * 共有コンテキストのマスターをを保持するためだけのクラス
 * Applicationクラス等でシングルトンとして使う
 */
public class GLMasterContext {
	private static final String TAG = GLMasterContext.class.getSimpleName();

	private MasterTask mMasterTask;
	
	public GLMasterContext(final int maxClientVersion, final int flags) {
		mMasterTask = new MasterTask(maxClientVersion, flags);
		new Thread(mMasterTask, TAG).start();
		mMasterTask.waitReady();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public synchronized void release() {
		if (mMasterTask != null) {
			mMasterTask.release();
			mMasterTask = null;
		}
	}
	
	public synchronized EGLBase.IContext getContext()
		throws IllegalStateException {
		if (mMasterTask != null) {
			return mMasterTask.getContext();
		} else {
			throw new IllegalStateException("already released");
		}
	}
	
	private static class MasterTask extends EglTask {
		public MasterTask(final int maxClientVersion, final int flags) {
			super(maxClientVersion, null, flags);
		}
		
		@WorkerThread
		@Override
		protected void onStart() {
			// do nothing
		}
		
		@WorkerThread
		@Override
		protected void onStop() {
			// do nothing
		}
		
		@WorkerThread
		@Override
		protected Object processRequest(final int request,
			final int arg1, final int arg2, final Object obj) throws TaskBreak {
			// do nothing
			return null;
		}
	}
}
