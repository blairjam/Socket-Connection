package com.connerblair.tcp;

/**
 * The thread that accepts message from a client connection and returns them to
 * the server.
 * 
 * @author Conner Blair
 * @version 1.0
 */
class ClientConnectionInputReaderThread extends Thread {
	private ClientConnection parentConnection;

	/**
	 * Creates a new instance of the ClientConnectionInputReaderThread class,
	 * with the specified ClientConnection.
	 * 
	 * @param parentConnection
	 *            The {@linkplain ClientConnection} object that represents the
	 *            connection that owns this object.
	 */
	ClientConnectionInputReaderThread(ClientConnection parentConnection) {
		super();
		this.parentConnection = parentConnection;
	}

	/**
	 * Accepts incoming messages from the client connection.
	 */
	@Override
	public void run() {
		// Call the client connected hook method.
		parentConnection.clientConnected();

		Object msg;

		while (parentConnection.isInputReaderThreadRunning()) {
			try {
				// Read the object and pass it to the server via the client
				// message received hook method.
				msg = parentConnection.getConnectionInputStream().readObject();
				parentConnection.clientMessageReceived(msg);
			} catch (Exception e) {
				parentConnection.handleClientException(e);
				parentConnection.closeConnection();
			}
		}
	}

	/**
	 * Accessor for the client connection that owns this thread.
	 * 
	 * @return {@linkplain ClientConnection} The client connection that owns
	 *         this thread.
	 */
	ClientConnection getParentConnection() {
		return parentConnection;
	}
}
