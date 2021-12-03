package com.serenegiant.widget

import com.serenegiant.glpipeline.IPipeline

/**
 * IPipelineSource/IPipelineで描画処理分配処理を行うViewの共通メソッドを定義するインターフェース
 */
interface IPipelineView {
	/**
	 * 指定したIPipelineオブジェクトをパイプラインチェーンに追加
	 * (削除するときはIPipeline#removeを使うこと)
	 * @param pipeline
	 */
	fun addPipeline(pipeline: IPipeline)
}