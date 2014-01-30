package fr.upmc.pc2r.isketch.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SketchCanvas extends JPanel {

	private static final long serialVersionUID = 1L;
	private BufferedImage backupImage = null;
	private BufferedImage canvasImage;
	private SketchCanvasLabel imageLabel;
	private Color color;
	private Stroke stroke = new BasicStroke(15, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	// private int iStroke = 15;
	private RenderingHints renderingHints;
	private Cursor circleCursor = CursorFactory.createDefaultCircleCursor();

	public SketchCanvas() {
		super();
		renderingHints = SwingStuff.getAppRenderingHints();
		setImage(new BufferedImage(449, 449, BufferedImage.TYPE_INT_RGB));
		JPanel imageView = new JPanel(new GridBagLayout());
		imageView.setPreferredSize(new Dimension(449, 449));
		imageLabel = new SketchCanvasLabel(canvasImage);
		JScrollPane imageScroll = new JScrollPane(imageView);
		imageView.add(imageLabel);
		this.add(imageScroll, BorderLayout.CENTER);
		setColor(Color.white);
		clear(canvasImage);
		setColor(Color.black);

	}

	public void addMouseMotionListener(MouseMotionListener listener) {
		imageLabel.addMouseMotionListener(listener);
	}

	public void addMouseListener(MouseAdapter listener) {
		imageLabel.addMouseListener(listener);
	}

	public void clear(BufferedImage bi) {
		Graphics2D g = bi.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.dispose();
		imageLabel.repaint();
	}

	public void clear() {
		Graphics2D g = canvasImage.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
		g.dispose();
		imageLabel.repaint();
	}

	private void setImage(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		canvasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = this.canvasImage.createGraphics();
		g.setRenderingHints(renderingHints);
		g.drawImage(image, 0, 0, this);
		g.dispose();
		if (this.imageLabel != null) {
			imageLabel.setIcon(new ImageIcon(canvasImage));
			this.imageLabel.repaint();
		}
	}

	private void restoreImage(BufferedImage image) {
		Graphics2D g = image.createGraphics();
		g.setRenderingHints(renderingHints);
		g.drawImage(image, 0, 0, this);
		g.dispose();
		imageLabel.setIcon(new ImageIcon(image));
		this.imageLabel.repaint();
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void drawPoint(Point point) {
		drawLine(point, point);
	}

	public void drawLine(Point a, Point b) {
		Graphics2D g = this.canvasImage.createGraphics();
		g.setRenderingHints(renderingHints);
		g.setColor(this.color);
		g.setStroke(stroke);
		g.draw(new Line2D.Double(a, b));
		g.dispose();

		// Point ulp = new Point(Math.min(a.x, b.x) - iStroke, Math.min(a.y,
		// b.y)
		// - iStroke);
		// Point brp = new Point(Math.max(a.x, b.x) + iStroke, Math.max(a.y,
		// b.y)
		// + iStroke);

		// this.imageLabel.repaint(new Rectangle(ulp.x, ulp.y, brp.x - ulp.x,
		// brp.y - ulp.y));
		imageLabel.setCanvasImage(canvasImage);
		imageLabel.repaint();
		// this.imageLabel.getGraphics().drawImage(canvasImage, 0, 0, null);
	}

	public void drawCourbe(Point p1, Point ctrl1, Point ctrl2, Point p2) {

		Graphics2D g = this.canvasImage.createGraphics();

		g.setRenderingHints(renderingHints);
		g.setColor(this.color);
		g.setStroke(stroke);
		CubicCurve2D cc2d = new CubicCurve2D.Double(p1.x, p1.y, ctrl1.x,
				ctrl1.y, ctrl2.x, ctrl2.y, p2.x, p2.y);
		g.draw(cc2d);
		g.dispose();
		this.imageLabel.repaint();

	}

	public void drawCourbe(ArrayList<Point> list) {
		/* restore image without points */
		restoreImage(backupImage);
		backupImage = null;

		drawCourbe(list.get(0), list.get(1), list.get(2), list.get(3));
	}

	// public void drawTempPoint(Point p) {
	// if (backupImage == null) {
	// System.err.println("backup");
	// backupImage = SwingStuff.deepCopy(canvasImage);
	// }
	// Graphics2D g = this.canvasImage.createGraphics();
	// g.setRenderingHints(renderingHints);
	// g.setColor(Color.black);
	// g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND,
	// BasicStroke.JOIN_ROUND));
	// g.draw(new Line2D.Double(p, p));
	// g.dispose();
	// Point ulp = new Point(p.x - iStroke, p.y - iStroke);
	// Point brp = new Point(p.x + iStroke, p.y + iStroke);
	// this.imageLabel.repaint(new Rectangle(ulp.x, ulp.y, brp.x - ulp.x,
	// brp.y - ulp.y));
	// }

	public void setStroke(int size) {
		this.stroke = new BasicStroke(size, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND);
		// this.iStroke = size;

	}

	public Cursor getCircleCursor() {
		return circleCursor;
	}

	public void setCircleCursor(Cursor cursor) {
		this.circleCursor = cursor;
	}

	public JLabel getImageLabel() {
		return imageLabel;
	}

}
