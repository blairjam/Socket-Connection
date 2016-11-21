package com.connerblair.tcp;

/**
 * The thread that accepts messages from the server and returns them to the
 * client.
 * 
 * @author Conner Blair
 * @version 1.0
 */
class TCPClientInputReaderThread extends Thread {
	private TCPClient parentClient;

	/**
	 * Creates a new instance of the TCPClientInputReaderThread class with the
	 * specified TCPClient.
	 * 
	 * @param parentClient
	 *            The {@linkplain TCPClient} that owns this thread.
	 */
	TCPClientInputReaderThread(TCPClient parentClient) {
		super();
		this.parentClient = parentClient;
	}

	/**
	 * Accepts incoming messages from the server and sends them to the client.
	 */
	@Override
	public void run() {
		// Call the client connection opened hook method.
		parentClient.connectionOpened();

		Object msg;

		while (parentClient.isClientReaderThreadRunning()) {
			try {
				// Read the object and pass it to the client via the handle
				// message from server hook method.
				msg = parentClient.getConnectionInputStream().readObject();
				parentClient.handleMessageFromServer(msg);
			} catch (Exception e) {
				parentClient.handleException(e);
				parentClient.closeConnection();
			}
		}
	}
}
