package fr.upmc.pc2r.isketch.controller;

import java.awt.Color;
import java.awt.Point;

import fr.upmc.pc2r.isketch.exceptions.NotConnectedExcetion;
import fr.upmc.pc2r.isketch.model.User;
import fr.upmc.pc2r.isketch.views.SketchFrame;

public class SketchReader implements Runnable {

	private SketchFrame view;
	private User model;

	private String strConnected = "CONNECTED";
	private String strExited = "EXITED";
	private String strNewRound = "NEW_ROUND";
	private String strGuessed = "GUESSED";
	private String strWordFound = "WORD_FOUND";
	private String strWordFoundTimeout = "WORD_FOUND_TIMEOUT";
	private String strEndRound = "END_ROUND";
	private String strScoreRound = "SCORE_ROUND";
	private String strLine = "LINE";
	private String strCourbe = "COURBE";
	private String strListen = "LISTEN";
	private String strPlayers = "PLAYERS";
	private String strWelcome = "WELCOME";
	private String strBroadcast = "BROADCAST";
	private String strError = "ERROR";

	public SketchReader(SketchFrame view, User model) {
		this.view = view;
		this.model = model;
	}

	public void syncDrawingContext() {
		try {
			model.sendSetSize(view.getSliderBrushSizeValue());
			model.sendSetColor(view.getColorChooserColor());
		} catch (NotConnectedExcetion e) {
			e.printStackTrace();
		}
	}

	public void run() {
		String[] data;
		String stream;
		try {
			while ((stream = model.getQueue().take()) != null) {
				data = stream.split("/");
				if (data[0].compareTo(strListen) == 0) {
					view.appendTxtrChatArea("<" + data[1] + "> " + data[2]
							+ "\n");

				} else if (data[0].compareTo(strConnected) == 0) {
					view.appendTxtrChatArea("== " + data[1] + " has joined\n");
					model.addUserList(data[1]);
					view.setTxtrListPlayersArea(model.getUserList());
				} else if (data[0].compareTo(strLine) == 0) {
					// System.err.println(stream);
					Point x = new Point(Integer.parseInt(data[1]),
							Integer.parseInt(data[2]));
					Point y = new Point(Integer.parseInt(data[3]),
							Integer.parseInt(data[4]));
					Color c = new Color(Integer.parseInt(data[5]),
							Integer.parseInt(data[6]),
							Integer.parseInt(data[7]));
					int size = Integer.parseInt(data[8]);
					view.getCanvas().setStroke(size);
					view.getCanvas().setColor(c);
					view.getCanvas().drawLine(x, y);

				}

				else if (data[0].compareTo(strCourbe) == 0) {
					Point p1 = new Point(Integer.parseInt(data[1]),
							Integer.parseInt(data[2]));

					Point ctrl1 = new Point(Integer.parseInt(data[3]),
							Integer.parseInt(data[4]));

					Point ctrl2 = new Point(Integer.parseInt(data[5]),
							Integer.parseInt(data[6]));

					Point p2 = new Point(Integer.parseInt(data[7]),
							Integer.parseInt(data[8]));

					Color c = new Color(Integer.parseInt(data[9]),
							Integer.parseInt(data[10]),
							Integer.parseInt(data[11]));
					int size = Integer.parseInt(data[12]);
					view.getCanvas().setStroke(size);
					view.getCanvas().setColor(c);
					view.getCanvas().drawCourbe(p1, ctrl1, ctrl2, p2);

				} else if (data[0].compareTo(strExited) == 0) {
					view.appendTxtrChatArea("== " + data[1] + " has quit\n");
					// view.setCaretToEndTxtrChatArea();
					model.removeUserList(data[1]);
					view.setTxtrListPlayersArea(model.getUserList());
				} else if (data[0].compareTo(strBroadcast) == 0) {
					if (data[1].compareTo("ARTIST_IS") == 0) {
						view.appendTxtrChatArea(data[2]
								+ " is drawing for this round.\n");
						continue;
					}
					if (data[1].compareTo("ROUND_END_IN") == 0) {
						view.appendTxtrChatArea("Round ends in " + data[2]
								+ " seconds.\n");
						continue;
					}
					if (data[1].compareTo("GAME_START_IN") == 0) {
						view.appendTxtrChatArea("Game starts in " + data[2]
								+ " seconds.\n");
						continue;
					}
					if (data[1].compareTo("EVERYBODY_HAS_GUESSED_THE_WORD") == 0) {
						view.appendTxtrChatArea("Everyone has correctly guessed the word!\n");
						continue;
					}
					if (data[1]
							.compareTo("A_GUESSER_REMINDS_THE_ARTIST_NOT_TO_VIOLATE_DRAWING_RULES") == 0) {
						view.appendTxtrChatArea("A guesser reminds the artist not to violate drawing rules.\n");
						continue;
					}
					if (data[1].compareTo("ALREADY_GUESSED") == 0) {
						view.appendTxtrGuessArea("You already guessed the word.\n");
						continue;
					}
					if (data[1]
							.compareTo("ALL_SCORES_WILL_BE_LOST_WHEN_NEW_GAME_START") == 0) {
						view.appendTxtrChatArea("Save your scores! All scores will be lost when the new game starts.\n");
						continue;
					}
					if (data[1].compareTo("THE_ARTIST_HAS_SKIPPED") == 0) {
						view.appendTxtrChatArea("The artist (" + data[2]
								+ ") has skipped.\n");
						continue;
					}

					if (data[1]
							.compareTo("GAME_WILL_START_WHEN_ENOUGH_PLAYERS") == 0) {
						view.appendTxtrChatArea("The game will automatically start when there are enough players.\n");
						continue;
					}

					if (data[1].compareTo("GAME_STARTED") == 0) {
						view.appendTxtrChatArea("The game has started.\n");
						continue;
					}

					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < data.length; i++) {
						sb.append(data[i] + " ");
					}
					view.appendTxtrChatArea("== " + sb.toString() + "\n");
					// view.setCaretToEndTxtrChatArea();
				} else if (data[0].compareTo(strError) == 0) {
					if (data[1].compareTo("NO_AVAILABLE_SLOT") == 0) {
						view.appendTxtrChatArea("No available slot. Try again later.\n");
						continue;
					}
					if (data[1].compareTo("CANT_SKIP") == 0) {
						view.appendTxtrChatArea("Someone already guessed the word, you can't skip anymore.\n");
						continue;
					}
					if (data[1].compareTo("CANT_ALERT_WHEN_ARTIST") == 0) {
						view.appendTxtrChatArea("You can't alert when you are the artist.\n");
						continue;
					}
					if (data[1].compareTo("CANT_ALERT_TWICE") == 0) {
						view.appendTxtrChatArea("You already alerted the artist.\n");
						continue;
					}
					if (data[1].compareTo("CANT_ALERT_NOT_THE_ARTIST") == 0) {
						view.appendTxtrChatArea("You can't alert someone that is not the artist.\n");
						continue;
					}
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < data.length; i++) {
						sb.append(data[i] + " ");
					}
					view.appendTxtrChatArea("== ERR : " + sb.toString() + "\n");
				} else if (data[0].compareTo(strWelcome) == 0) {
					/* si le nom a changé */
					if (model.getUsername().compareTo(data[1]) != 0) {
						model.setUsername(data[1]);
						view.setTxtUsernameValue(data[1]);
					}

					model.setIsConnected(true);
					view.enableConnectedUI();
					syncDrawingContext();
				} else if (data[0].compareTo(strNewRound) == 0) {
					view.getCanvas().clear();
					model.setCurrentArtist(data[2]); // this is for cheat
														// command
														// if
														// (data[1].compareTo("dessinateur")
														// == 0) {
					if (model.getUsername().compareTo(data[2]) == 0) {
						view.appendTxtrGuessArea("You have to draw the word "
								+ data[3] + ".\n");
						if (!model.isSpectator())
							view.enableArtistUI();
					} else {
						if (!model.isSpectator()) {
							view.appendTxtrGuessArea("You have to guess the word.\n");
							view.enableGuesserUI();
						}
					}
				} else if (data[0].compareTo(strGuessed) == 0) {
					view.appendTxtrGuessArea(data[2] + " : " + data[1] + "\n");

				} else if (data[0].compareTo(strWordFound) == 0) {
					if (data[1].compareTo(model.getUsername()) == 0) {

						view.appendTxtrGuessArea("You found the word!\n");
					} else {
						view.appendTxtrGuessArea(data[1]
								+ " has found the word.\n");
					}
				} else if (data[0].compareTo(strWordFoundTimeout) == 0) {
					view.appendTxtrGuessArea("The word has been found!\n");
					view.appendTxtrGuessArea("The round will end in " + data[1]
							+ " seconds!\n");

				} else if (data[0].compareTo(strScoreRound) == 0) {
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < data.length; i++) {
						sb.append(data[i] + " ");
						if (i % 2 == 0) {
							sb.append("\n");
						} else {
							sb.append(": ");
						}
					}

					sb.append("\n");
					view.appendTxtrChatArea(sb.toString());
				} else if (data[0].compareTo(strEndRound) == 0) {

					view.appendTxtrGuessArea("The word to guess was : "
							+ data[2] + ".\n" + "The winner for this round is "
							+ data[1] + ".\n");

					if (!model.isSpectator())
						view.enableGuesserUI();
				} else if (data[0].compareTo(strPlayers) == 0) {
				} else {
					System.err.println("Unknow command received : " + stream);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
