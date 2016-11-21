package com.connerblair.exceptions;

/**
 * Simple generic exception to be thrown during the runtime of the TCP and UDP
 * implementations.
 * 
 * @author Conner Blair
 * @version 1.0
 */
public class ConnectionException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of the ConnectionException class, with the given
	 * message.
	 * 
	 * @param message
	 *            The message that describes the exception.
	 */
	public ConnectionException(String message) {
		super(message);
	}

	/**
	 * Creates a new instance of the ConnectionException class, with the given
	 * message and throwable that caused the exception.
	 * 
	 * @param message
	 *            The message the describes the exception.
	 * @param cause
	 *            The cause of this exception.
	 */
	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
