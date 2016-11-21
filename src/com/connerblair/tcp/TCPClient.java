package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

/**
 * This class represent a client in the TCP client/server architecture.
 * 
 * @author Conner Blair
 * @version 1.0
 */
public abstract class TCPClient {
	public static int DEF_PORT = -1;
	public static String DEF_HOST = "localhost";

	private int port;
	private InetAddress host;

	private Socket clientSocket;
	private ObjectInputStream input;
	private ObjectOutputStream output;

	private final Object clientReaderLock = new Object();
	private boolean clientReaderRunning = false;
	private TCPClientInputReaderThread clientReaderThread;

	/**
	 * Creates a new instance of the TCPClient class with the default port and
	 * host.
	 */
	protected TCPClient() {
		this(DEF_PORT);
	}

	/**
	 * Creates a new instance of the TCPClient class with the specified port and
	 * the default host.
	 * 
	 * @param port
	 *            The port to access.
	 */
	protected TCPClient(int port) {
		this(port, DEF_HOST);
	}

	/**
	 * Creates a new instance of the TCPClient class with the specified port and
	 * host.
	 * 
	 * @param port
	 *            The port to access.
	 * @param host
	 *            The address of the host.
	 */
	protected TCPClient(int port, String host) {
		this.port = port;

		// Convert string representation to InetAddress.
		try {
			this.host = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			handleException(e);
			this.host = null;
		}
	}

	/**
	 * Opens the connection to the server and starts the input listening thread.
	 */
	public final void openConnection() {
		if (isConnected()) {
			handleException(new ConnectionException("The client is already connected to the server"));
			return;
		}

		if (!initialize()) {
			return;
		}

		// Create the input and output objects.
		try {
			output = new ObjectOutputStream(clientSocket.getOutputStream());
			input = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			handleException(e);
			closeConnection();
		}

		// Start the input reader thread.
		clientReaderRunning = true;
		clientReaderThread = new TCPClientInputReaderThread(this);
		clientReaderThread.start();
	}

	/**
	 * Closes the connection to the server.
	 */
	public final void closeConnection() {
		// Set the running flag to false.
		synchronized (clientReaderLock) {
			clientReaderRunning = false;
		}

		// Join the reader thread.
		try {
			clientReaderThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

		// Close all objects.
		try {
			if (clientSocket != null) {
				clientSocket.close();
			}

			if (input != null) {
				input.close();
			}

			if (output != null) {
				output.close();
			}
		} catch (IOException e) {
			handleException(e);
		} finally {
			output = null;
			input = null;
			clientSocket = null;
		}

		// Call connection closed hook method.
		connectionClosed();
	}

	/**
	 * Sends the given Object to the server.
	 * 
	 * @param msg
	 *            The {@linkplain Object} to send to the server.
	 */
	public final void sendToServer(Object msg) {
		// Check for null socket and output stream.
		if (clientSocket == null || output == null) {
			handleException(new ConnectionException("Client socket does not exist."));
			return;
		}

		// Send object.
		try {
			output.writeObject(msg);
		} catch (IOException e) {
			handleException(e);
		}
	}

	/**
	 * Accessor method to see if the client is currently connected to the
	 * server.
	 * 
	 * @return boolean True if the client is connected, false if not.
	 */
	public final boolean isConnected() {
		// If the client reader thread is alive, then the client is connected.
		return clientReaderThread != null && clientReaderThread.isAlive();
	}

	/**
	 * Accessor method for the port the client is connected to.
	 * 
	 * @return int The port the client is connected to.
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * Mutator method to change the port the client is connected to.
	 * 
	 * @param port
	 *            The new port the client should connect to.
	 */
	public final void setPort(int port) {
		if (clientReaderRunning) {
			handleException(new ConnectionException("Can not change port while the client is running."));
		} else {
			this.port = port;
		}
	}

	/**
	 * Accessor method for the host that the client is connected to.
	 * 
	 * @return {@linkplain InetAddress} The address of the host.
	 */
	public final InetAddress getHost() {
		return host;
	}

	/**
	 * Mutator method to change the host the client is connected to .
	 * 
	 * @param host
	 *            The new host the client should connect to.
	 */
	public final void setHost(String host) {
		// Convert the string to a InetAddress and call the setHost method.
		try {
			setHost(InetAddress.getByName(host));
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + host, e));
		}
	}

	/**
	 * Mutator method to change the host the client is connected to.
	 * 
	 * @param host
	 *            The new host the client should connect to.
	 */
	public final void setHost(InetAddress host) {
		if (clientReaderRunning) {
			handleException(new ConnectionException("Can not chnage host addres while client is running."));
		} else {
			this.host = host;
		}
	}

	/**
	 * Accessor method to check the value of the client reader thread flag. <br>
	 * The access of the flag is synchronized.
	 * 
	 * @return boolean The value of the client reader thread flag.
	 */
	boolean isClientReaderThreadRunning() {
		synchronized (clientReaderLock) {
			return clientReaderRunning;
		}
	}

	/**
	 * Accessor method to get the object input stream for the socket.
	 * 
	 * @return {@linkplain ObjectInputStream} The stream through which messages
	 *         from the server are sent.
	 */
	ObjectInputStream getConnectionInputStream() {
		return input;
	}

	/**
	 * Hook method called when exceptions are thrown and must be handled.
	 * 
	 * @param e
	 *            The exception thrown.
	 */
	protected abstract void handleException(Exception e);

	/**
	 * Hook method called when the connection to the server is opened.
	 */
	protected abstract void connectionOpened();

	/**
	 * Hook method called when the connection to the server is closed.
	 */
	protected abstract void connectionClosed();

	/**
	 * Hook method called when the server has sent a message to the client.
	 * 
	 * @param msg
	 *            The message sent by the server.
	 */
	protected abstract void handleMessageFromServer(Object msg);

	/**
	 * Initializes the socket.
	 * 
	 * @return boolean Returns true if the socket was initialized successfully,
	 *         false if not.
	 */
	private boolean initialize() {
		if (host == null) {
			return false;
		}

		try {
			clientSocket = new Socket(host, port);
		} catch (IOException e) {
			handleException(new ConnectionException("Can not intialize socket.", e));
			return false;
		}

		return true;
	}
}
