package com.serenegiant.glutils.es2;

import android.opengl.GLES20;

public class OverlayDrawer2d extends GLDrawer2D {
	public OverlayDrawer2d(final boolean isOES) {
		super(isOES);
	}

	public OverlayDrawer2d(final float[] vertices, final float[] texcoord, final boolean isOES) {
		super(vertices, texcoord, isOES);
	}

	public synchronized void draw(
		final int texId1, final float[] tex_matrix1, final int offset1,
		final int texId2, final float[] tex_matrix2, final int offset2) {

//		if (DEBUG) Log.v(TAG, "draw");
		if (hProgram < 0) return;
		GLES20.glUseProgram(hProgram);
		if (tex_matrix1 != null) {
			// テクスチャ変換行列が指定されている時
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix1, offset1);
		}
		// モデルビュー変換行列をセット
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(mTexTarget, texId1);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);

		GLES20.glBindTexture(mTexTarget, 0);
        GLES20.glUseProgram(0);
	}
}
