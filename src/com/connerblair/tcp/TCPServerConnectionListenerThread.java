package com.connerblair.tcp;

import java.io.IOException;
import java.net.Socket;

/**
 * The thread that is responsible for accepting incoming connections from new
 * clients.
 * 
 * @author Conner Blair
 * @version 1.0
 */
class TCPServerConnectionListenerThread extends Thread {
	private TCPServer parentServer;

	/**
	 * Creates a new instance of the TCPServerConnectionListenerThread, with the
	 * specified TCPServer.
	 * 
	 * @param parentServer
	 *            The server that owns this thread.
	 */
	TCPServerConnectionListenerThread(TCPServer parentServer) {
		super();
		this.parentServer = parentServer;
	}

	/**
	 * Accepts incoming connections and creates a new thread for each one.
	 */
	@Override
	public void run() {
		// Call to server's server started hook method.
		parentServer.serverStarted();

		while (parentServer.getListenerState() != TCPListenerState.Stopped) {
			// Listening thread is in a paused state. Sleep for 50ms and check
			// again.
			if (parentServer.getListenerState() == TCPListenerState.Paused) {
				try {
					Thread.sleep(50);
				} catch (Exception e) {
				}

				continue;
			}

			// Accepts new connections and spawns new threads for each one.
			try {
				Socket clientSocket = parentServer.getServerSocket().accept();

				synchronized (this) {
					new ClientConnection(parentServer, clientSocket);
				}
			} catch (IOException e) {
			}
		}
	}
}
