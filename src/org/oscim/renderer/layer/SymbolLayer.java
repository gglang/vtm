/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer.layer;

import java.nio.ShortBuffer;

import org.oscim.renderer.TextureObject;
import org.oscim.renderer.TextureRenderer;

import android.graphics.Canvas;
import android.util.Log;

// TODO share one static texture for all poi map symabols

public final class SymbolLayer extends TextureLayer {
	private static String TAG = SymbolLayer.class.getSimpleName();

	private final static int TEXTURE_WIDTH = TextureObject.TEXTURE_WIDTH;
	private final static int TEXTURE_HEIGHT = TextureObject.TEXTURE_HEIGHT;
	private final static float SCALE = 8.0f;

	private static short[] mVertices;

	SymbolItem symbols;

	public SymbolLayer() {
		if (mVertices == null)
			mVertices = new short[TextureRenderer.MAX_ITEMS * 24];
	}

	public void addSymbol(SymbolItem item) {

		verticesCnt += 4;

		SymbolItem it = symbols;

		for (; it != null; it = it.next) {
			if (it.bitmap == item.bitmap) {
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = symbols;
		symbols = item;
	}

	private final static int LBIT_MASK = 0xfffffffe;

	// TODO ... reuse texture when only symbol position changed
	@Override
	public void compile(ShortBuffer sbuf) {

		short numIndices = 0;
		short offsetIndices = 0;

		int pos = 0;
		short buf[] = mVertices;
		int bufLen = buf.length;

		int advanceY = 0;
		float x = 0;
		float y = 0;

		Canvas canvas = TextureObject.getCanvas();

		for (SymbolItem it = symbols; it != null;) {

			// add bitmap
			float width = it.bitmap.getWidth();
			float height = it.bitmap.getHeight();

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = 0;
				y += advanceY;
				advanceY = (int) (height + 0.5f);

				if (y + height > TEXTURE_HEIGHT) {
					Log.d(TAG, "reached max symbols");

					TextureObject to = TextureObject.uploadCanvas(offsetIndices, numIndices);
					offsetIndices = numIndices;

					to.next = textures;
					textures = to;

					sbuf.put(buf, 0, pos);
					pos = 0;

					x = 0;
					y = 0;
					advanceY = (int) height;
				}
			}

			canvas.drawBitmap(it.bitmap, x, y, null);

			float hw = width / 2.0f;
			float hh = height / 2.0f;

			short x1 = (short) (SCALE * (-hw));
			short x2 = (short) (SCALE * (hw));
			short y1 = (short) (SCALE * (hh));
			short y2 = (short) (SCALE * (-hh));

			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add symbol items referencing the same bitmap
			for (SymbolItem it2 = it;; it2 = it2.next) {

				if (it2 == null || it2.bitmap != it.bitmap) {
					it = it2;
					break;
				}

				// add vertices
				short tx = (short) ((int) (SCALE * it2.x) & LBIT_MASK | (it2.billboard ? 1 : 0));
				short ty = (short) (SCALE * it2.y);

				// top-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y1;
				buf[pos++] = u1;
				buf[pos++] = v2;
				// top-right
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x2;
				buf[pos++] = y1;
				buf[pos++] = u2;
				buf[pos++] = v2;
				// bot-right
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x2;
				buf[pos++] = y2;
				buf[pos++] = u2;
				buf[pos++] = v1;
				// bot-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y2;
				buf[pos++] = u1;
				buf[pos++] = v1;

				// six elements used to draw the four vertices
				numIndices += 6;

				// FIXME this does not work, need to draw bitmap on next
				// texture...
				if (pos == bufLen) {
					sbuf.put(buf, 0, pos);
					pos = 0;
				}

				x += width;
			}
		}

		TextureObject to = TextureObject.uploadCanvas(offsetIndices, numIndices);

		to.next = textures;
		textures = to;

		sbuf.put(buf, 0, pos);
	}
}