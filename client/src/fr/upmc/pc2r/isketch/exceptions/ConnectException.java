package fr.upmc.pc2r.isketch.exceptions;

public class ConnectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConnectException() {
		super("You are already connected.\n"); 
	}

}
