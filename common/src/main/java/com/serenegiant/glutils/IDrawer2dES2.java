package com.serenegiant.glutils;

/**
 * Created by saki on 2016/12/10.
 *
 */
public interface IDrawer2dES2 extends IDrawer2D {
	public int glGetAttribLocation(final String name);
	public int glGetUniformLocation(final String name);
	public void glUseProgram();
}
