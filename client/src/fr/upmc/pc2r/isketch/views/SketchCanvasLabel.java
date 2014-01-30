package fr.upmc.pc2r.isketch.views;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class SketchCanvasLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private BufferedImage canvasImage;

	public SketchCanvasLabel(BufferedImage canvasImage) {
		super(new ImageIcon(canvasImage));
		this.setCanvasImage(canvasImage);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage(canvasImage, 0, 0, null);
	}

	public BufferedImage getCanvasImage() {
		return canvasImage;
	}

	public void setCanvasImage(BufferedImage canvasImage) {
		this.canvasImage = canvasImage;
	}

}
