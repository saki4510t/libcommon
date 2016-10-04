package com.serenegiant.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * 指定したPathを描画するシェイプ
 */
public class PathShape extends BaseShape {

    private Path mPath = new Path();

    /**
     * PathShape constructor.
     *
     * @param path       Shape表示内容を定義するPath。Pathの座標値はShape内描画座標系で
     * Shapeの描画内容の位置関係を示すだけなので実際の表示サイズとは異なる。
     * @param stdWidth   Shape内座標系の最大幅
     * @param stdHeight  Shape内座標系の最大高さ
     */
    public PathShape(final Path path, final float stdWidth, final float stdHeight) {
    	super(stdWidth, stdHeight);
        setPath(path);
    }

    @Override
    protected void doDraw(final Canvas canvas, final Paint paint) {
        canvas.drawPath(mPath, paint);
    }

    @Override
    public PathShape clone() throws CloneNotSupportedException {
        final PathShape shape = (PathShape) super.clone();
        shape.mPath = new Path(mPath);
        return shape;
    }

    /**
     * Shape表示内容を定義するPathを設定する
     * @param path
     */
    public void setPath(final Path path) {
    	mPath.reset();
    	if (path != null && !path.isEmpty()) {
    		mPath.addPath(path);
    	}
    }

    /**
     * 設定されているPathを返す
     * @return
     */
    public Path getPath() {
    	return mPath;
    }
}
