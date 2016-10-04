package com.serenegiant.graphics;

/**
 * 二等辺三角形シェイプ
 */
public class IsoscelesTriangleShape extends TriangleShape {

	/**
	 * 高さと幅を指定して二等辺三角形Shapeを生成する
	 * ここでの高さと幅はShape内描画座標系でShapeの描画内容の位置関係を示すだけなので
	 * 実際の表示サイズとは異なる。単に高さと幅の比率だけ指定するだけでよい
	 * @param height
	 * @param width
	 */
	public IsoscelesTriangleShape(final float height, final float width) {
		super(new float[] {0, height, width, height, width / 2, 0});
	}

}
