package com.serenegiant.math;

// 球オブジェクト(3D)
public class SphereBounds extends CircleBounds {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5374122610666117206L;

	public SphereBounds(final float x, final float y, final float z, final float radius) {
		super(x, y, z, radius);
	}

	public SphereBounds(final float x, final float y, final float radius) {
		super(x, y, radius);
	}

	public SphereBounds(final Vector v, final float radius) {
		super(v, radius);
	}
	
}
