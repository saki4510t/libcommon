package com.serenegiant.glutils;

import java.io.IOException;

/**
 * Created by saki on 2016/10/19.
 *
 */
public interface ITexture {
	void release();

	void bind();
	void unbind();

	int getTexTarget();
	int getTexture();

	float[] getTexMatrix();
	void getTexMatrix(float[] matrix, int offset);

	int getTexWidth();
	int getTexHeight();

	void loadTexture(String filePath) throws NullPointerException, IOException;
}
