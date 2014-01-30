package fr.upmc.pc2r.isketch.main;

import java.awt.EventQueue;

import fr.upmc.pc2r.isketch.controller.SketchController;
import fr.upmc.pc2r.isketch.model.User;
import fr.upmc.pc2r.isketch.views.SwingStuff;
import fr.upmc.pc2r.isketch.views.SketchFrame;

public class SketchMain {
	protected static String name;
	protected static String host;
	protected static int port;

	protected static boolean autoconnect;

	public static void main(String[] args) {

		switch (args.length) {
		case 0:
			port = 2013;
			name = "pc2r";
			autoconnect = false;
			break;
		case 3:
			host = args[2];
		case 2:
			port = Integer.parseInt(args[0]);
			name = args[1];
			autoconnect = true;
			break;
		default:
			System.err.println("Arguments : [[<port> <name> [<host>]]]");
			return;
		}
		
		SwingStuff.setSystemLookAndFeel();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					User model = new User(name, host, port);
					SketchFrame window = new SketchFrame();
					@SuppressWarnings("unused")
					SketchController controller = new SketchController(window,
							model);
					window.setVisible(true);
					if (autoconnect) {
						window.pushBtnConnect();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
