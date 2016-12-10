package com.serenegiant.glutils.es1;

import android.opengl.GLES10;
import android.opengl.Matrix;

import com.serenegiant.glutils.IDrawer2D;
import com.serenegiant.glutils.ITexture;
import com.serenegiant.glutils.TextureOffscreen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * Created by saki on 2016/10/19.
 *
 */
public class GLDrawer2D implements IDrawer2D {
	private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
	private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;

	private final float[] mMvpMatrix = new float[16];
	private final FloatBuffer pVertex;
	private final FloatBuffer pTexCoord;
	private final int mTexTarget;

	/**
	 * コンストラクタ
	 * GLコンテキスト/EGLレンダリングコンテキストが有効な状態で呼ばないとダメ
	 * @param isOES 外部テクスチャ(GL_TEXTURE_EXTERNAL_OES)を使う場合はtrue。通常の2Dテキスチャならfalse
	 */
	public GLDrawer2D(final boolean isOES) {
		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(VERTICES);
		pVertex.flip();
		pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(TEXCOORD);
		pTexCoord.flip();
		// モデルビュー変換行列を初期化
		Matrix.setIdentityM(mMvpMatrix, 0);
	}

	@Override
	public void release() {

	}

	/**
	 * モデルビュー変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	@Override
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	@Override
	public IDrawer2D setMvpMatrix(final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		return this;
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	@Override
	public void getMvpMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
	}

	@Override
	public void draw(final int texId, final float[] tex_matrix, final int offset) {
		// FIXME Matrixを適用
		GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		pVertex.position(0);
		GLES10.glVertexPointer(2, GLES10.GL_FLOAT, VERTEX_SZ, pVertex);
//--------------------------------------------------------------------------------
		GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
		pTexCoord.position(0);
		GLES10.glTexCoordPointer(VERTEX_NUM, GLES10.GL_FLOAT, VERTEX_SZ, pTexCoord);
		GLES10.glActiveTexture(GLES10.GL_TEXTURE0);
		GLES10.glBindTexture(mTexTarget, texId);
//--------------------------------------------------------------------------------
		GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
//--------------------------------------------------------------------------------
		GLES10.glBindTexture(mTexTarget, 0);
		GLES10.glDisableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
//--------------------------------------------------------------------------------
		GLES10.glDisableClientState(GLES10.GL_VERTEX_ARRAY);
	}

	@Override
	public void draw(final ITexture texture) {
		draw(texture.getTexture(), texture.getTexMatrix(), 0);
	}

	@Override
	public void draw(final TextureOffscreen offscreen) {
		draw(offscreen.getTexture(), offscreen.getTexMatrix(), 0);
	}
}
