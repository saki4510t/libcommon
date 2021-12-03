package com.serenegiant.widget

import com.serenegiant.glpipeline.IPipeline
import com.serenegiant.glutils.GLManager

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

	/**
	 * IPipeline/IPipelineSourceの処理に使うGLManagerインスタンスを取得する
	 */
	fun getGLManager(): GLManager
}