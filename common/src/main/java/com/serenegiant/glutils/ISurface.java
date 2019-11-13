package com.serenegiant.glutils;

public interface ISurface {
	public void release();
	public void makeCurrent();
	public void swap();
	public boolean isValid();
}
