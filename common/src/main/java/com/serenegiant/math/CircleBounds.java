package com.serenegiant.math;

// 円(2D)/球(3D)オブジェクト
public class CircleBounds extends BaseBounds {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6571630061846420508L;

	public CircleBounds(final float x, final float y, final float z, final float radius) {
		position.set(x, y, z);
		this.radius = radius;
	}

	public CircleBounds(final float x, final float y, final float radius) {
		this(x, y, 0f, radius);
	}
	
	public CircleBounds(final Vector v, final float radius) {
		this(v.x, v.y, 0f, radius);
	}
	
	@Override
	public boolean ptInBounds(final float x, final float y, final float z) {
		return ptInBoundsSphere(x, y, z, radius);
	}
}
