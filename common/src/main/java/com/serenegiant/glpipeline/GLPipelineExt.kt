package com.serenegiant.glpipeline
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

/**
 * 呼び出し元のGLPipelineを含むパイプラインチェーンの一番最後に指定したGLPipelineを追加する
 * @param pipeline
 */
fun GLPipeline.append(pipeline: GLPipeline) {
	GLPipeline.append(this, pipeline)
}

/**
 *呼び出し元のGLPipelineの直後に指定したGLPipelineを挿入する
 * @param pipeline
 */
fun GLPipeline.insert(pipeline: GLPipeline) {
	GLPipeline.append(this, pipeline)
}

/**
 * 呼び出し元のGLPipelineを含むパイプラインチェーンの一番最初のGLPipelineを取得する
 * @return GLPipeline 正常に繋がったパイプラインチェーンであればGLPipelineSourceのはず
 */
fun GLPipeline.findFirst(): GLPipeline {
	return GLPipeline.findFirst(this)
}

/**
 * 呼び出し元のGLPipelineを含むパイプラインチェーンの一番最後のGLPipelineを取得する
 * @return
 */
fun GLPipeline.findLast(): GLPipeline {
	return GLPipeline.findLast(this)
}

/**
 * パイプラインチェーンに含まれる指定したGLPipelineオブジェクトを取得する
 * 複数存在する場合は最初に見つかったものを返す
 * @param clazz GLPipelineを実装したクラスのClassオブジェクト
 * @return 見つからなければnull
 */
fun <T : GLPipeline> GLPipeline.find(clazz: Class<T>): T? {
	return GLPipeline.find(this, clazz)
}
