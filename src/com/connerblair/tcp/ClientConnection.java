package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;

import com.connerblair.exceptions.ConnectionException;

/**
 * This class represents the connection between the client and the server in a
 * TCP client/server architecture.
 * 
 * This class cannot be inherited from.
 * 
 * @author Conner Blair
 * @version 1.0
 */
public final class ClientConnection {
	private TCPServer parentServer;
	private Socket clientSocket;

	private Hashtable<String, String> info;

	private ObjectInputStream input;
	private ObjectOutputStream output;

	private final Object inputReaderLock = new Object();
	private boolean inputReaderThreadRunning = false;
	private ClientConnectionInputReaderThread inputReaderThread;

	/**
	 * Creates a new instance of the ClientConnection class, with the specified
	 * parent server and client socket.
	 * 
	 * @param parentServer
	 *            The {@linkplain TCPServer} that owns this client connection
	 *            object.
	 * @param clientSocket
	 *            The {@linkplain Socket} through which this client
	 *            communicates.
	 */
	public ClientConnection(TCPServer parentServer, Socket clientSocket) {
		this.parentServer = parentServer;
		this.clientSocket = clientSocket;

		// Create a Hashtable to store client information.
		info = new Hashtable<String, String>();

		// Set the server timeout.
		try {
			this.clientSocket.setSoTimeout(0);
		} catch (SocketException e) {
			parentServer.handleClientException(this, e);
		}

		// Create the input and output stream objects.
		try {
			input = new ObjectInputStream(clientSocket.getInputStream());
			output = new ObjectOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			parentServer.handleClientException(this, e);
		}

		// Start the input listening thread.
		synchronized (inputReaderLock) {
			inputReaderThreadRunning = true;
		}
		inputReaderThread = new ClientConnectionInputReaderThread(this);
		inputReaderThread.start();
	}

	/**
	 * Sends the given message to the client.
	 * 
	 * @param msg
	 *            The {@link Object} to send to the client.
	 */
	public void sendToClient(Object msg) {
		// Make sure the socket and output stream exist.
		if (clientSocket == null || output == null) {
			parentServer.handleClientException(this, new ConnectionException("Client socket does not exist."));
			return;
		}

		// Send the object over the socket.
		try {
			output.writeObject(msg);
		} catch (IOException e) {
			parentServer.handleClientException(this, e);
		}
	}

	/**
	 * Closes the connection between the server and this client.
	 */
	public void closeConnection() {
		// Set the running flag to false.
		synchronized (inputReaderLock) {
			inputReaderThreadRunning = false;
		}

		// Join the thread back into the master thread.
		try {
			inputReaderThread.join();
		} catch (InterruptedException e) {
			parentServer.handleClientException(this, e);
		}

		// Close the socket and the stream objects.
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
			parentServer.handleClientException(this, e);
		} finally {
			output = null;
			input = null;
			clientSocket = null;
		}

		// Call the server hook method for a disconnected client.
		parentServer.clientDisconnected(this);
	}

	/**
	 * Accessor for the host address of the socket.
	 * 
	 * @return {@linkplain InetAddress} The address of the socket.
	 */
	public InetAddress getInetAddress() {
		return clientSocket == null ? null : clientSocket.getInetAddress();
	}

	/**
	 * Mutator for the stored info about the client. <br>
	 * Client info is stored as a key/value pair.
	 * 
	 * @param key
	 *            A String representing the key by which to store the value.
	 * @param value
	 *            A string representing the value to store at the given key.
	 */
	public void setInfo(String key, String value) {
		info.put(key, value);
	}

	/**
	 * Accessor for the stored info about the client.
	 * 
	 * @param key
	 *            A string representing the key by which the needed data is
	 *            stored.
	 * @return String The value stored at the given key.
	 */
	public String getInfo(String key) {
		return info.get(key);
	}

	/**
	 * Accessor for the input stream provided by the client socket.
	 * 
	 * @return {@linkplain ObjectInputStream} The input stream provided by the
	 *         client socket.
	 */
	ObjectInputStream getConnectionInputStream() {
		return input;
	}

	/**
	 * Accessor for the flag that controls the running of the input reader
	 * thread. <br>
	 * The access of this flag is synchronized.
	 * 
	 * @return boolean The value of the flag that controls the input reader
	 *         thread.
	 */
	boolean isInputReaderThreadRunning() {
		synchronized (inputReaderLock) {
			return inputReaderThreadRunning;
		}
	}

	/**
	 * Performs a call to the parent server's client connected hook method.
	 */
	void clientConnected() {
		parentServer.clientConnected(this);
	}

	/**
	 * Performs a call to the parent server's client message received hook
	 * method, with the given Object.
	 * 
	 * @param msg
	 *            The {@linkplain Object} that is received from the client.
	 */
	void clientMessageReceived(Object msg) {
		parentServer.clientMessageReceived(this, msg);
	}

	/**
	 * Performs a call to the parent server's handle client exception hook
	 * method, with the given Exception.
	 * 
	 * @param e
	 *            The {@linkplain Exception} that is thrown by the client.
	 */
	void handleClientException(Exception e) {
		parentServer.handleClientException(this, e);
	}
}
