package fr.upmc.pc2r.isketch.exceptions;

public class DisconnectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DisconnectException() {
		super("You are already disconnected.\n"); 
	}

}
