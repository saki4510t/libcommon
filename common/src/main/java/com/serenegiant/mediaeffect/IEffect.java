package com.serenegiant.mediaeffect;
/*
 * Androusb
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 * Distributed under the terms of the GNU Lesser General Public License (LGPL v3.0) License.
 * License details are in the file license.txt, distributed as part of this software.
 */

public interface IEffect {
	public void apply(int[] src_tex_ids, int width, int height, int out_tex_id);
	public void apply(ISource src);
	public void release();
	public IEffect resize(final int width, final int height);
	public boolean enabled();
	public IEffect setEnable(final boolean enable);
}
