package fr.upmc.pc2r.isketch.exceptions;

public class NotConnectedExcetion extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotConnectedExcetion() {
		super("You are not connected.\n");
	}

}
