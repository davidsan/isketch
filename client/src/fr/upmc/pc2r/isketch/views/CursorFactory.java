package fr.upmc.pc2r.isketch.views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class CursorFactory {
	public static Cursor createCircleCursor(Color cursorColor, int cursorSize) {

		BufferedImage bi = new BufferedImage(cursorSize + 2, cursorSize + 2,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		Color dotColor = new Color(255 - cursorColor.getRed(),
				255 - cursorColor.getGreen(), 255 - cursorColor.getBlue());
		g.setColor(cursorColor);
		g.drawOval(0, 0, cursorSize, cursorSize);
		g.setColor(dotColor);
		g.drawOval(1, 1, cursorSize - 2, cursorSize - 2);
		return Toolkit.getDefaultToolkit().createCustomCursor(bi,
				new Point(cursorSize / 2, cursorSize / 2),
				cursorColor + " Circle");
	}

	public static Cursor createFilledCircleCursor(Color cursorColor,
			int cursorSize) {

		BufferedImage bi = new BufferedImage(cursorSize + 2, cursorSize + 2,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		g.setColor(cursorColor);
		g.fillOval(0, 0, cursorSize, cursorSize);
		return Toolkit.getDefaultToolkit().createCustomCursor(bi,
				new Point(cursorSize / 2, cursorSize / 2),
				cursorColor + " Circle");
	}

	public static Cursor createCircleCursor(int cursorSize) {
		return createCircleCursor(Color.black, cursorSize);
	}

	public static Cursor createDefaultCircleCursor() {
		return createCircleCursor(Color.black, 2);
	}

	public static Cursor createBezierCircleCursor() {
		return createFilledCircleCursor(Color.black, 10);
	}
}
