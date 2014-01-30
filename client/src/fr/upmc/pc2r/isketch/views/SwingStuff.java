package fr.upmc.pc2r.isketch.views;

import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SwingStuff {

	private static RenderingHints rh = null;

	public static RenderingHints getAppRenderingHints() {
		if (rh == null) {
			Map<Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
			// hintsMap.put(RenderingHints.KEY_RENDERING,
			// RenderingHints.VALUE_RENDER_QUALITY);
			// hintsMap.put(RenderingHints.KEY_DITHERING,
			// RenderingHints.VALUE_DITHER_ENABLE);
			// hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING,
			// RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			hintsMap.put(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			rh = new RenderingHints(hintsMap);
		}
		return rh;
	}

	public static void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}

	public static void setCrossPlatformLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
}
