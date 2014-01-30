package fr.upmc.pc2r.isketch.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import fr.upmc.pc2r.isketch.exceptions.ConnectException;
import fr.upmc.pc2r.isketch.exceptions.DisconnectException;
import fr.upmc.pc2r.isketch.exceptions.NotConnectedExcetion;

public class User {
	private String username;
	private String host;
	private int port;
	private Socket sock;
	private Boolean isConnected = false;
	private Boolean isSpectator = false;
	private ArrayList<String> userList;
	private Point pos;
	private BufferedReader reader; /* reading into the socket */
	private PrintWriter writer; /* writing into the socket */
	private BlockingQueue<String> queue;
	private String currentArtist;
	private DrawingTools dt;
	private ArrayList<Point> currentBezierPoints;
	private Point mfp;

	public User(String username, String host, int port) {
		super();
		this.username = username;
		this.host = host;
		this.port = port;
		this.userList = new ArrayList<>();
		this.queue = new PriorityBlockingQueue<>();
		this.pos = new Point(0, 0);
		this.dt = DrawingTools.LINE;
		this.currentBezierPoints = new ArrayList<>();
	}

	public User(String username, int port) {
		this(username, "localhost", port);
	}

	public void connect() throws ConnectException, UnknownHostException,
			IOException {
		if (isConnected) {
			throw new ConnectException();
		}

		sock = new Socket(host, port);
		InputStreamReader streamreader = new InputStreamReader(
				sock.getInputStream());
		reader = new BufferedReader(streamreader);
		writer = new PrintWriter(sock.getOutputStream());

		Thread queueHandler = new Thread(new Runnable() {

			@Override
			public void run() {
				String stream;
				try {
					while ((stream = reader.readLine()) != null) {
						queue.add(stream);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		queueHandler.start();

		writer.println("CONNECT/" + username + "/");
		writer.flush();
	}

	public void connectSpectator() throws ConnectException,
			UnknownHostException, IOException {
		if (isConnected) {
			throw new ConnectException();
		}

		sock = new Socket(host, port);
		InputStreamReader streamreader = new InputStreamReader(
				sock.getInputStream());
		reader = new BufferedReader(streamreader);
		writer = new PrintWriter(sock.getOutputStream());

		Thread queueHandler = new Thread(new Runnable() {

			@Override
			public void run() {
				String stream;
				try {
					while ((stream = reader.readLine()) != null) {
						queue.add(stream);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		isSpectator = true;
		queueHandler.start();

		writer.println("SPECTATOR/");
		writer.flush();

	}

	public Boolean isSpectator() {
		return isSpectator;
	}

	public void disconnect() throws DisconnectException {
		if (!isConnected) {
			throw new DisconnectException();
		}
		writer.println("EXIT/" + username + "/");
		writer.flush();
		this.isConnected = false;
		userList.clear();
	}

	public void talk(String msg) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		writer.println("TALK/" + msg + "/");
		writer.flush();
	}

	public void guess(String word) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		writer.println("GUESS/" + word + "/");
		writer.flush();
	}

	public void alert() throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		writer.println("ALERT/" + currentArtist + "/");
		writer.flush();
	}

	public void skip() throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		writer.println("SKIP/");
		writer.flush();
	}

	public void clear(Dimension size) throws NotConnectedExcetion {
		// HACK Optimisation du nombre de lignes à tracer pour remplir le canvas
		// On utilise le fait que l'épaisseur du trait tracé est borné par les
		// valeurs du slider de l'interface graphique.
		for (int i = 0; i < size.width; i++) {
			sendSetLine(new Point(0, i), new Point(size.height, i));
		}
	}

	public void sendSetLine(Point point) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		if (point.x == pos.x && point.y == pos.y) {
			return;
		}
		pos = point;
		writer.println("SET_LINE/" + point.x + "/" + point.y + "/" + point.x
				+ "/" + point.y + "/");
		writer.flush();
	}

	public void sendSetLine(Point x, Point y) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}

		writer.println("SET_LINE/" + x.x + "/" + x.y + "/" + y.x + "/" + y.y
				+ "/");
		writer.flush();
	}

	public void sendSetCurve() throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentBezierPoints.size(); i++) {
			sb.append(currentBezierPoints.get(i).x + "/");
			sb.append(currentBezierPoints.get(i).y + "/");
		}
		writer.println("SET_COURBE/" + sb.toString());
		writer.flush();

	}

	public void sendSetSize(int size) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}

		writer.println("SET_SIZE/" + size + "/");
		writer.flush();
	}

	public void sendSetColor(Color color) throws NotConnectedExcetion {
		if (!isConnected) {
			throw new NotConnectedExcetion();
		}

		writer.println("SET_COLOR/" + color.getRed() + "/" + color.getGreen()
				+ "/" + color.getBlue() + "/");
		writer.flush();
	}

	/* properties */

	public BufferedReader getReader() {
		return reader;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public Socket getSock() {
		return sock;
	}

	public void setSock(Socket sock) {
		this.sock = sock;
	}

	public Boolean getIsConnected() {
		return isConnected;
	}

	public void setIsConnected(Boolean isConnected) {
		this.isConnected = isConnected;
	}

	public ArrayList<String> getUserList() {
		return userList;
	}

	public void addUserList(String user) {
		this.userList.add(user);
		Collections.sort(userList);
	}

	public void removeUserList(String user) {
		this.userList.remove(user);
	}

	public BlockingQueue<String> getQueue() {
		return this.queue;
	}

	public void setCurrentArtist(String currentArtist) {
		this.currentArtist = currentArtist;
	}

	public DrawingTools getDrawingTool() {
		return dt;
	}

	public void setDrawingTool(DrawingTools tool) {
		this.dt = tool;
	}

	public ArrayList<Point> getCurrentBezierPoints() {
		return currentBezierPoints;
	}

	public void addBezierPoints(Point e) {
		currentBezierPoints.add(e);
	}

	public void clearBezierPoints() {
		currentBezierPoints.clear();
	}

	public void resetMouseFirstPoint() {
		mfp = null;
	}

	public boolean isDefinedMouseFirstPoint() {
		return mfp != null;
	}

	public void setMouseFirstPoint(Point p) {
		mfp = new Point(p);
	}

	public Point getMouseFirstPoint() {
		return mfp;
	}

}
