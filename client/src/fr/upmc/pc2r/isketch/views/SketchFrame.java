package fr.upmc.pc2r.isketch.views;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeListener;

public class SketchFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JFrame frmIsketch;
	private JTextField txtUsername;
	private JTextArea txtrGuessArea;
	private JTextField txtGuessInputArea;
	private JScrollPane scrollPaneTxtrListPlayersArea;
	private JTextArea txtrListPlayersArea;
	private JScrollPane scrollPaneTxtrChatArea;
	private JTextArea txtrChatArea;
	private JTextField txtChatInputArea;
	private JButton btnSend;
	private JButton btnGuess;
	private SketchCanvas panelCanvas;
	private JColorChooser cc;
	private AbstractColorChooserPanel ccp;
	private JButton btnConnect;
	private JButton btnDisconnect;
	private JButton btnClear;
	private JButton btnAlert;
	private JButton btnSkip;
	private JButton btnSpectator;
	private JSlider sliderBrushSize;
	private JButton btnCurve;

	/**
	 * Create the application.
	 */
	public SketchFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmIsketch = this;
		frmIsketch.setResizable(false);
		frmIsketch.setTitle("iSketch App");
		frmIsketch.setBounds(100, 100, 850, 700);
		frmIsketch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmIsketch.getContentPane().setLayout(null);

		JScrollPane scrollPaneTxtrGuessArea = new JScrollPane();
		scrollPaneTxtrGuessArea
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneTxtrGuessArea.setBounds(164, 41, 206, 253);
		frmIsketch.getContentPane().add(scrollPaneTxtrGuessArea);

		txtrGuessArea = new JTextArea();
		txtrGuessArea.setLineWrap(true);
		txtrGuessArea.setWrapStyleWord(true);
		txtrGuessArea.setFont(new Font("Dialog", Font.PLAIN, 11));
		txtrGuessArea.setEditable(false);
		scrollPaneTxtrGuessArea.setViewportView(txtrGuessArea);

		txtGuessInputArea = new JTextField();
		txtGuessInputArea.setBounds(164, 299, 94, 32);
		frmIsketch.getContentPane().add(txtGuessInputArea);

		scrollPaneTxtrListPlayersArea = new JScrollPane();
		scrollPaneTxtrListPlayersArea
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneTxtrListPlayersArea.setBounds(12, 41, 140, 286);
		frmIsketch.getContentPane().add(scrollPaneTxtrListPlayersArea);

		txtrListPlayersArea = new JTextArea();
		txtrListPlayersArea.setFont(new Font("Dialog", Font.PLAIN, 11));
		txtrListPlayersArea.setFocusable(false);
		txtrListPlayersArea.setEditable(false);
		txtrListPlayersArea.setRows(10);
		txtrListPlayersArea.setColumns(1);
		scrollPaneTxtrListPlayersArea.setViewportView(txtrListPlayersArea);

		scrollPaneTxtrChatArea = new JScrollPane();
		scrollPaneTxtrChatArea
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneTxtrChatArea.setBounds(12, 339, 358, 289);
		frmIsketch.getContentPane().add(scrollPaneTxtrChatArea);

		txtrChatArea = new JTextArea();
		txtrChatArea.setLineWrap(true);
		txtrChatArea.setWrapStyleWord(true);
		txtrChatArea.setFont(new Font("Dialog", Font.PLAIN, 11));
		txtrChatArea.setEditable(false);
		scrollPaneTxtrChatArea.setViewportView(txtrChatArea);

		txtChatInputArea = new JTextField();
		txtChatInputArea.setBounds(12, 635, 256, 32);
		frmIsketch.getContentPane().add(txtChatInputArea);

		btnSend = new JButton("Send");
		btnSend.setIcon(new ImageIcon(
				SketchFrame.class
						.getResource("/fr/upmc/pc2r/isketch/icons/fugue/balloon-left.png")));

		btnSend.setBounds(280, 635, 90, 32);
		frmIsketch.getContentPane().add(btnSend);

		btnGuess = new JButton("Guess");
		btnGuess.setIcon(new ImageIcon(SketchFrame.class
				.getResource("/fr/upmc/pc2r/isketch/icons/fugue/tick.png")));

		btnGuess.setBounds(270, 299, 100, 32);
		frmIsketch.getContentPane().add(btnGuess);

		panelCanvas = new SketchCanvas();
		panelCanvas.setBounds(378, 208, 460, 460);
		frmIsketch.getContentPane().add(panelCanvas);

		SwingStuff.setCrossPlatformLookAndFeel();
		cc = new JColorChooser(Color.BLACK);
		// make things pretty
		SwingStuff.setSystemLookAndFeel();
		ccp = cc.getChooserPanels()[0];
		ccp.getComponent(0).setBackground(
				(Color) UIManager.getLookAndFeel().getDefaults()
						.get("Panel.background"));
		ccp.setBackground((Color) UIManager.getLookAndFeel().getDefaults()
				.get("Panel.background"));
		JPanel panelColorPicker = new JPanel();
		panelColorPicker.setBounds(382, 41, 449, 122);
		frmIsketch.getContentPane().add(panelColorPicker);
		panelColorPicker.add(ccp);

		JLabel lblUsername = new JLabel("Username");
		lblUsername.setBounds(12, 13, 72, 15);
		frmIsketch.getContentPane().add(lblUsername);

		txtUsername = new JTextField();
		txtUsername.setText("pc2r");
		txtUsername.setBounds(91, 5, 162, 32);
		frmIsketch.getContentPane().add(txtUsername);
		txtUsername.setColumns(10);

		btnConnect = new JButton("Join");
		btnConnect.setToolTipText("Join");
		btnConnect
				.setIcon(new ImageIcon(
						SketchFrame.class
								.getResource("/fr/upmc/pc2r/isketch/icons/fugue/plug-connect.png")));
		btnConnect.setSelectedIcon(null);

		btnConnect.setBounds(265, 4, 105, 32);
		frmIsketch.getContentPane().add(btnConnect);

		btnDisconnect = new JButton("Exit");
		btnDisconnect
				.setIcon(new ImageIcon(
						SketchFrame.class
								.getResource("/fr/upmc/pc2r/isketch/icons/fugue/plug-disconnect.png")));

		btnDisconnect.setBounds(382, 4, 105, 32);
		frmIsketch.getContentPane().add(btnDisconnect);

		btnClear = new JButton("Clear");
		btnClear.setIcon(new ImageIcon(SketchFrame.class
				.getResource("/fr/upmc/pc2r/isketch/icons/fugue/eraser.png")));
		btnClear.setBounds(382, 168, 105, 32);
		frmIsketch.getContentPane().add(btnClear);

		btnAlert = new JButton("Alert");
		btnAlert.setIcon(new ImageIcon(
				SketchFrame.class
						.getResource("/fr/upmc/pc2r/isketch/icons/fugue/exclamation-red.png")));
		btnAlert.setBounds(616, 4, 105, 32);
		frmIsketch.getContentPane().add(btnAlert);

		sliderBrushSize = new JSlider();
		sliderBrushSize.setPaintTicks(true);
		sliderBrushSize.setMinimum(2);
		sliderBrushSize.setMaximum(62);
		sliderBrushSize.setValue(2);
		sliderBrushSize.setMajorTickSpacing(20);
		sliderBrushSize.setMinorTickSpacing(20);
		sliderBrushSize.setBounds(616, 162, 222, 50);
		frmIsketch.getContentPane().add(sliderBrushSize);

		btnSkip = new JButton("Skip");
		btnSkip.setIcon(new ImageIcon(
				SketchFrame.class
						.getResource("/fr/upmc/pc2r/isketch/icons/fugue/control-skip.png")));

		btnSkip.setBounds(733, 4, 105, 32);
		getContentPane().add(btnSkip);

		btnSpectator = new JButton("Spec");
		btnSpectator.setIcon(new ImageIcon(SketchFrame.class
				.getResource("/fr/upmc/pc2r/isketch/icons/fugue/popcorn.png")));
		btnSpectator.setBounds(499, 4, 105, 32);
		getContentPane().add(btnSpectator);

		btnCurve = new JButton("Curve");
		btnCurve.setIcon(new ImageIcon(
				SketchFrame.class
						.getResource("/fr/upmc/pc2r/isketch/icons/fugue/layer-shape-curve.png")));
		btnCurve.setBounds(499, 168, 105, 32);
		getContentPane().add(btnCurve);
		enableDisconnectedUI();
	}

	public void displayErrorMessage(String errorMessage) {
		// JOptionPane.showMessageDialog(this, errorMessage);
		appendTxtrChatArea(errorMessage);
	}

	/* binding listeners */

	public void addBtnConnectListener(ActionListener listener) {
		btnConnect.addActionListener(listener);
	}

	public void addBtnDisconnectListener(ActionListener listener) {
		btnDisconnect.addActionListener(listener);
	}

	public void addBtnSpectatorListener(ActionListener listener) {
		btnSpectator.addActionListener(listener);
	}

	public void addBtnSendListener(ActionListener listener) {
		btnSend.addActionListener(listener);
	}

	public void addBtnGuessListener(ActionListener listener) {
		btnGuess.addActionListener(listener);
	}

	public void addBtnAlertListener(ActionListener listener) {
		btnAlert.addActionListener(listener);
	}

	public void addBtnSkipListener(ActionListener listener) {
		btnSkip.addActionListener(listener);
	}

	public void addBtnClearListener(ActionListener listener) {
		btnClear.addActionListener(listener);
	}

	public void addSliderBrushSizeListener(ChangeListener listener) {
		sliderBrushSize.addChangeListener(listener);
	}

	public void addColorChooserListener(ChangeListener listener) {
		cc.getSelectionModel().addChangeListener(listener);
	}

	public void addChatInputKeyListener(KeyListener listener) {
		txtChatInputArea.addKeyListener(listener);
	}

	public void addGuessInputKeyListener(KeyListener listener) {
		txtGuessInputArea.addKeyListener(listener);
	}

	public void addBtnCurveListener(ActionListener listener) {
		btnCurve.addActionListener(listener);
	}

	/* getters, setters */
	public String getTxtUsernameValue() {
		return txtUsername.getText();
	}

	public String getTxtChatInputAreaValue() {
		return txtChatInputArea.getText();
	}

	public String getTxtGuessInputAreaValue() {
		return txtGuessInputArea.getText();
	}

	public void disableEditTxtUsername() {
		txtUsername.setEditable(false);
		txtUsername.setFocusable(false);
	}

	public void enableEditTxtUsername() {
		txtUsername.setEditable(true);
		txtUsername.setFocusable(true);
	}

	public void appendTxtrChatArea(String str) {
		txtrChatArea.append(str);
		setCaretToEndTxtrChatArea();
	}

	public void appendTxtrGuessArea(String str) {
		txtrGuessArea.append(str);
		setCaretToEndTxtrGuessArea();
	}

	public void setCaretToEndTxtrChatArea() {
		txtrChatArea.setCaretPosition(txtrChatArea.getDocument().getLength());
	}

	public void setCaretToEndTxtrGuessArea() {
		txtrGuessArea.setCaretPosition(txtrGuessArea.getDocument().getLength());
	}

	public void clearTxtrChatArea() {
		txtrChatArea.setText("");
	}

	public void clearTxtrListPlayersArea() {
		txtrListPlayersArea.setText("");
	}

	public void clearTxtChatInputArea() {
		txtChatInputArea.setText("");
	}

	public void clearTxtGuessInputArea() {
		txtGuessInputArea.setText("");
	}

	public void setTxtrListPlayersArea(List<String> list) {
		clearTxtrListPlayersArea();
		for (Iterator<String> it = list.iterator(); it.hasNext();) {
			String str = (String) it.next();
			txtrListPlayersArea.append(str + "\n");
		}
	}

	public void disableBtnConnect() {
		btnConnect.setEnabled(false);
	}

	public void enableBtnConnect() {
		btnConnect.setEnabled(true);
	}

	public void setTxtUsernameValue(String string) {
		txtUsername.setText(string);
	}

	public SketchCanvas getCanvas() {
		return panelCanvas;
	}

	public int getSliderBrushSizeValue() {
		return sliderBrushSize.getValue();
	}

	public Color getColorChooserColor() {
		return cc.getColor();
	}

	public void pushBtnConnect() {
		btnConnect.doClick();

	}

	public void clearSketchCanvas() {
		panelCanvas.clear();
	}

	public void enableSpectatorUI() {
		btnConnect.setEnabled(false);
		btnDisconnect.setEnabled(false);
		btnSpectator.setEnabled(false);
		btnSend.setEnabled(false);
		btnGuess.setEnabled(false);
		btnAlert.setEnabled(false);
		btnSkip.setEnabled(false);
		btnCurve.setEnabled(false);
		btnClear.setEnabled(false);
		sliderBrushSize.setEnabled(false);
		cc.setEnabled(false);
		txtChatInputArea.setEnabled(false);
		txtGuessInputArea.setEnabled(false);
		panelCanvas.setEnabled(false);
	}

	public void enableArtistUI() { 
		btnSend.setEnabled(false);
		btnGuess.setEnabled(false);
		btnAlert.setEnabled(false);
		btnSkip.setEnabled(true);
		btnCurve.setEnabled(true);
		btnClear.setEnabled(true);
		sliderBrushSize.setEnabled(true);
		cc.setEnabled(true);
		txtChatInputArea.setEnabled(false);
		txtGuessInputArea.setEnabled(false);
		panelCanvas.setEnabled(true);
	}

	public void enableConnectedUI() {
		btnConnect.setEnabled(false);
		btnSpectator.setEnabled(false);
		btnDisconnect.setEnabled(true);
	}

	public void enableDisconnectedUI() {
		btnConnect.setEnabled(true);
		btnSpectator.setEnabled(true);
		btnDisconnect.setEnabled(true); // on laisse toujours le bouton actif
		btnSend.setEnabled(false);
		btnGuess.setEnabled(false);
		btnAlert.setEnabled(false);
		btnSkip.setEnabled(false);
		btnCurve.setEnabled(false);
		btnClear.setEnabled(false);
		sliderBrushSize.setEnabled(false);
		cc.setEnabled(false);
		txtChatInputArea.setEnabled(false);
		txtGuessInputArea.setEnabled(false);
		panelCanvas.setEnabled(false);
	}

	public void enableGuesserUI() {
		btnSend.setEnabled(true);
		btnGuess.setEnabled(true);
		btnAlert.setEnabled(true);
		btnSkip.setEnabled(false);
		btnCurve.setEnabled(false);
		btnClear.setEnabled(false);
		sliderBrushSize.setEnabled(false);
		cc.setEnabled(false);
		txtChatInputArea.setEnabled(true);
		txtGuessInputArea.setEnabled(true);
		panelCanvas.setEnabled(false);
	}
}
