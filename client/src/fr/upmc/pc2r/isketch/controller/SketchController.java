package fr.upmc.pc2r.isketch.controller;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.upmc.pc2r.isketch.exceptions.ConnectException;
import fr.upmc.pc2r.isketch.exceptions.DisconnectException;
import fr.upmc.pc2r.isketch.exceptions.NotConnectedExcetion;
import fr.upmc.pc2r.isketch.model.DrawingTools;
import fr.upmc.pc2r.isketch.model.User;
import fr.upmc.pc2r.isketch.views.CursorFactory;
import fr.upmc.pc2r.isketch.views.SketchFrame;

public class SketchController {

	private SketchFrame view;
	private User model;
	private Thread listenThread;

	public SketchController(SketchFrame view, User model) {
		this.view = view;
		this.model = model;

		view.setTxtUsernameValue(model.getUsername());

		this.view.addBtnConnectListener(new BtnConnectListener());
		this.view.addBtnDisconnectListener(new BtnDisconnectListener());
		this.view.addBtnSpectatorListener(new BtnSpectatorListener());
		this.view.addBtnSendListener(new BtnSendListener());
		this.view.addBtnGuessListener(new BtnGuessListener());
		this.view.addBtnAlertListener(new BtnAlertListener());
		this.view.addBtnSkipListener(new BtnSkipListener());
		this.view.addBtnClearListener(new BtnClearListener());
		this.view.addBtnCurveListener(new BtnCurveListener());

		this.view.addSliderBrushSizeListener(new SliderSizeListener());
		this.view.addColorChooserListener(new ColorChooserListener());
		this.view.addChatInputKeyListener(new SubmitWithEnterChatKeyListener());
		this.view
				.addGuessInputKeyListener(new SubmitWithEnterGuessKeyListener());
		this.view.addWindowListener(new WindowExitListener());

		this.listenThread = new Thread(new SketchReader(view, model));

		this.view.getCanvas().addMouseMotionListener(
				new ImageMouseMotionListener());

		this.view.getCanvas().addMouseListener(new ImageMouseListener());
		listenThread.start();
	}

	class BtnConnectListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			model.setUsername(view.getTxtUsernameValue());
			try {
				model.connect();
				view.appendTxtrChatArea("=====\n");
				view.disableEditTxtUsername();
			} catch (ConnectException exn) {
				view.displayErrorMessage(exn.getMessage());
			} catch (UnknownHostException exn) {
				view.displayErrorMessage(exn.getMessage() + ".\n");
			} catch (IOException exn) {
				view.displayErrorMessage(exn.getMessage() + ".\n");
			}

		}
	}

	class BtnDisconnectListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			model.setUsername(view.getTxtUsernameValue());
			try {
				model.disconnect();
				view.clearTxtrListPlayersArea();
				view.clearSketchCanvas();
				view.enableEditTxtUsername();
				view.appendTxtrChatArea("You have been disconnected.\n");
				view.enableDisconnectedUI();
			} catch (DisconnectException exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class BtnSpectatorListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			model.setUsername(view.getTxtUsernameValue());
			try {
				model.connectSpectator();
				view.appendTxtrChatArea("=====\n");
				view.disableEditTxtUsername();
				view.enableSpectatorUI();
			} catch (ConnectException exn) {
				view.displayErrorMessage(exn.getMessage());
			} catch (UnknownHostException exn) {
				view.displayErrorMessage(exn.getMessage() + ".\n");
			} catch (IOException exn) {
				view.displayErrorMessage(exn.getMessage() + ".\n");
			}

		}
	}

	class BtnSendListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String message = view.getTxtChatInputAreaValue();
			if (message.compareTo("") == 0) {
				return;
			}
			view.clearTxtChatInputArea();
			try {
				model.talk(message);
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class BtnGuessListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String word = view.getTxtGuessInputAreaValue();
			if (word.compareTo("") == 0) {
				return;
			}
			view.clearTxtGuessInputArea();
			try {
				model.guess(word);
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class BtnAlertListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				model.alert();
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class BtnSkipListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				model.skip();
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class BtnClearListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				model.clear(view.getCanvas().getSize());
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}
	}

	class SliderSizeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (((JSlider) (e.getSource())).getValueIsAdjusting()) {
				return;
			}
			try {

				int size = view.getSliderBrushSizeValue();
				model.sendSetSize(size);
				view.getCanvas().setCircleCursor(
						CursorFactory.createCircleCursor(size));

			} catch (NotConnectedExcetion exn) {
				// view.displayErrorMessage(exn.getMessage());
			}
		}
	}

	class ColorChooserListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent arg0) {
			Color c = view.getColorChooserColor();
			try {
				model.sendSetColor(c);
			} catch (NotConnectedExcetion exn) {
				// view.displayErrorMessage(exn.getMessage());
			}
		}

	}

	class SubmitWithEnterChatKeyListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() != KeyEvent.VK_ENTER) {
				return;
			}
			String message = view.getTxtChatInputAreaValue();
			if (message.compareTo("") == 0) {
				return;
			}
			view.clearTxtChatInputArea();
			try {
				model.talk(message);
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

	}

	class SubmitWithEnterGuessKeyListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() != KeyEvent.VK_ENTER) {
				return;
			}
			String word = view.getTxtGuessInputAreaValue();
			if (word.compareTo("") == 0) {
				return;
			}
			view.clearTxtGuessInputArea();
			try {
				model.guess(word);
			} catch (NotConnectedExcetion exn) {
				view.displayErrorMessage(exn.getMessage());
			}

		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

	}

	/* will disconnect user when closing window */
	class WindowExitListener extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			if (model.getIsConnected()) {
				try {
					model.disconnect();
				} catch (DisconnectException e1) {
					// should never happen
					e1.printStackTrace();
				}
			}
			System.exit(0);
		}
	}

	class ImageMouseListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			try {
				if (model.getDrawingTool() == DrawingTools.LINE) {
					// if (e.getPoint().x > 0 && e.getPoint().y > 0
					// && e.getPoint().x < view.getCanvas().getWidth()
					// && e.getPoint().y < view.getCanvas().getHeight()) {
					// model.sendSetLine(e.getPoint());
					// }
				} else if (model.getDrawingTool() == DrawingTools.CURVE) {
					model.addBezierPoints(e.getPoint());
					// view.getCanvas().drawTempPoint(e.getPoint());
					if (model.getCurrentBezierPoints().size() == 4) {
						model.sendSetCurve();
						model.setDrawingTool(DrawingTools.LINE);
					}
				}
			} catch (NotConnectedExcetion exn) {
				// view.displayErrorMessage(exn.getMessage());
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			model.resetMouseFirstPoint();
		}
	}

	class ImageMouseMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
			try {
				Point tmp = new Point(e.getPoint());
				if (model.getDrawingTool() == DrawingTools.LINE) {
					if (tmp.x > 0 && tmp.y > 0
							&& tmp.x < view.getCanvas().getWidth()
							&& tmp.y < view.getCanvas().getHeight()) {
						if (!model.isDefinedMouseFirstPoint()) {
							model.setMouseFirstPoint(tmp);
						}

						model.sendSetLine(model.getMouseFirstPoint(), tmp);
						model.setMouseFirstPoint(tmp);
					}
				}
			} catch (NotConnectedExcetion exn) {
				// view.displayErrorMessage(exn.getMessage());
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {

			final Rectangle cellBounds = view.getCanvas().getImageLabel()
					.getVisibleRect();
			if (cellBounds != null && cellBounds.contains(e.getPoint())) {
				if (model.getDrawingTool() == DrawingTools.LINE) {

					view.getCanvas().getImageLabel()
							.setCursor(view.getCanvas().getCircleCursor());
				} else if (model.getDrawingTool() == DrawingTools.CURVE) {
					view.getCanvas()
							.getImageLabel()
							.setCursor(CursorFactory.createBezierCircleCursor());
				}
			} else {
				view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}

	}

	class BtnCurveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			model.clearBezierPoints();
			model.setDrawingTool(DrawingTools.CURVE);
		}
	}

}
