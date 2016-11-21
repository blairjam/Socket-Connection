package com.connerblair.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

/**
 * This class represents a server in the TCP client/server architecture.
 * 
 * @author Conner Blair
 * @version 1.0
 */
public abstract class TCPServer {
	public static final int DEF_PORT = -1;
	public static final int DEF_BACKLOG = 10;
	public static final int DEF_TIMEOUT = 500;

	private int port;
	private int backlog;
	private int timeout;
	private InetAddress address;

	private ServerSocket serverSocket;

	private ThreadGroup clientConnections;
	private final Object listenerLock = new Object();
	private TCPListenerState listenerThreadState = TCPListenerState.Stopped;
	private TCPServerConnectionListenerThread listenerThread;

	/**
	 * Creates a new instance of the TCPServer class, with the default port,
	 * backlog, and timeout.
	 */
	protected TCPServer() {
		this(DEF_PORT);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specifed port and
	 * the default backlog and timeout.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 */
	protected TCPServer(int port) {
		this(port, DEF_BACKLOG);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specified port
	 * and backlog, and with the default timeout.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 * @param backlog
	 *            The size of the backlog when accepting incoming connections.
	 */
	protected TCPServer(int port, int backlog) {
		this(port, backlog, DEF_TIMEOUT);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specifed port and
	 * address, and the default backlog and timeout.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 * @param address
	 *            The address to which the server should bind.
	 */
	protected TCPServer(int port, String address) {
		this(port, DEF_BACKLOG, address);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specified port,
	 * backlog, and timeout.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 * @param backlog
	 *            The size of the backlog when accepting incoming connections.
	 * @param timeout
	 *            The size of the timeout between accepting new connections in
	 *            miliseconds.
	 */
	protected TCPServer(int port, int backlog, int timeout) {
		this(port, backlog, timeout, null);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specifed port,
	 * backlog, address, and the default timeout.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 * @param backlog
	 *            The size of the backlog when accepting incoming connections.
	 * @param address
	 *            The address to which the server should bind.
	 */
	protected TCPServer(int port, int backlog, String address) {
		this(port, backlog, DEF_TIMEOUT, address);
	}

	/**
	 * Creates a new instance of the TCPServer class, with the specified port,
	 * backlog, timeout, and address.
	 * 
	 * @param port
	 *            The port to which the server should bind.
	 * @param backlog
	 *            The size of the backlog when accepting incoming connections.
	 * @param timeout
	 *            The size of the timeout between accepting new connections in
	 *            miliseconds.
	 * @param address
	 *            The address to which the server should bind.
	 */
	protected TCPServer(int port, int backlog, int timeout, String address) {
		this.port = port;
		this.backlog = backlog;
		this.timeout = timeout;

		// Convert the string representation to an InetAddress object.
		try {
			this.address = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
			this.address = null;
		}

		// Create a new thread group to hold all connections to clients.
		clientConnections = new ThreadGroup("ClientConnection_Threads") {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				handleClientException(((ClientConnectionInputReaderThread) t).getParentConnection(),
						new ConnectionException("Client Error.", e));
			}
		};
	}

	/**
	 * Starts the server.
	 */
	public final void start() {
		// This whole block is synchronized because it's path depends on the
		// value of the thread flag.
		synchronized (listenerLock) {
			switch (listenerThreadState) {
				case Running: {
					handleException(new ConnectionException("The listener thread is already running."));
					break;
				}
				case Paused: {
					listenerThreadState = TCPListenerState.Running;
					break;
				}
				case Stopped: {
					// Initialize the socket.
					if (!initialize()) {
						return;
					}

					// Start the listener thread running.
					listenerThreadState = TCPListenerState.Running;
					listenerThread = new TCPServerConnectionListenerThread(this);
					listenerThread.start();
					break;
				}
			}
		}
	}

	/**
	 * Pauses the server's listening thread.
	 */
	public final void pause() {
		// Synchronized access of the thread flag.
		synchronized (listenerLock) {
			listenerThreadState = TCPListenerState.Paused;
		}

		// Calls the server paused hook method.
		serverPaused();
	}

	/**
	 * Stops the server's listening thread.
	 */
	public final void stop() {
		// Synchronized access of the thread flag.
		synchronized (listenerLock) {
			listenerThreadState = TCPListenerState.Stopped;
		}

		// Join the listener thread.
		try {
			listenerThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

		// Close the socket.
		try {
			serverSocket.close();
		} catch (IOException e) {
			handleException(e);
		}

		// Call the server stopped hook method.
		serverStopped();
	}

	/**
	 * Sends the given message to all connected clients.
	 * 
	 * @param msg
	 *            The message to send to all clients.
	 */
	public final void sendToAllClients(Object msg) {
		// Get all connections.
		Thread[] connections = getAllConnections();

		// Loop through connections and send message to each one.
		for (Thread connection : connections) {
			ClientConnection client = ((ClientConnectionInputReaderThread) connection).getParentConnection();
			client.sendToClient(msg);
		}
	}

	/**
	 * Accessor method for the number of connected clients.
	 * 
	 * @return int The number of connected clients.
	 */
	public final int getNumberOfClients() {
		return clientConnections.activeCount();
	}

	/**
	 * Accessor method for the port of the server.
	 * 
	 * @return int The port of the server.
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * Mutator method for the port of the server. <br>
	 * If the server is not stopped, this call will have no effect.
	 * 
	 * @param port
	 *            The new port of the server.
	 */
	public final void setPort(int port) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the port while the server is running."));
			return;
		}

		this.port = port;
	}

	/**
	 * Accessor method for the backlog size of the server.
	 * 
	 * @return int The size of the backlog.
	 */
	public final int getBacklog() {
		return backlog;
	}

	/**
	 * Mutator method for the backlog size of the server. <br>
	 * If the server is not stopped, this call will have no effect.
	 * 
	 * @param backlog
	 *            The new size of the backlog.
	 */
	public final void setBacklog(int backlog) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the backlog while the server is running."));
			return;
		}

		this.backlog = backlog;
	}

	/**
	 * Accessor method for the timeout value of the server.
	 * 
	 * @return int The timeout value of the server.
	 */
	public final int getTimeout() {
		return timeout;
	}

	/**
	 * Mutator method for the timeout of the server. <br>
	 * If the server is not stopped, this call will have no effect.
	 * 
	 * @param timeout
	 *            The new value of the timeout.
	 */
	public final void setTimeout(int timeout) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the timeout while the server is running."));
			return;
		}

		this.timeout = timeout;
	}

	/**
	 * Accessor method for the address of the server.
	 * 
	 * @return {@linkplain InetAddress} The address of the server.
	 */
	public final InetAddress getAddress() {
		return address;
	}

	/**
	 * Mutator method for the address of the server. <br>
	 * If the server is not stopped, this call will have no effect.
	 * 
	 * @param address
	 *            The new address of the server.
	 */
	public final void setAddress(String address) {
		try {
			setAddress(InetAddress.getByName(address));
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
		}
	}

	/**
	 * Mutator method for the address of the server. <br>
	 * If the server is not stopped, this call will have no effect.
	 * 
	 * @param address
	 *            The new address of the server.
	 */
	public final void setAddress(InetAddress address) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the address while the server is running."));
			return;
		}

		this.address = address;
	}

	/**
	 * Accessor method to check if the server is currently in a running state.
	 * <br>
	 * Access to the listener flag is synchronized.
	 * 
	 * @return boolean True if the sever is in a running state, false if not.
	 */
	public final boolean isRunning() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Running;
		}
	}

	/**
	 * Accessor method to check if the server is currently in a paused state.
	 * <br>
	 * Access to the listener flag is synchronized.
	 * 
	 * @return boolean True if the server is in paused state, false if not.
	 */
	public final boolean isPaused() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Paused;
		}
	}

	/**
	 * Accessor method to check if the server is currently in a stopped state.
	 * <br>
	 * Access to the listener flag is synchronized.
	 * 
	 * @return boolean True if the server is in a stopped state, false if not.
	 */
	public final boolean isStopped() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Stopped;
		}
	}

	/**
	 * Accessor method for the socket of the server.
	 * 
	 * @return {@linkplain ServerSocket} The socket of the server.
	 */
	ServerSocket getServerSocket() {
		return serverSocket;
	}

	/**
	 * Accessor method for the thread group of the server.
	 * 
	 * @return {@linkplain ThreadGroup} The thread group of the server.
	 */
	ThreadGroup getClientConnections() {
		return clientConnections;
	}

	/**
	 * Accessor for the listener thread state. <br>
	 * Access of the flag is synchronized.
	 * 
	 * @return {@linkplain TCPListenerState} The value of the listener thread
	 *         state flag.
	 */
	TCPListenerState getListenerState() {
		synchronized (listenerLock) {
			return listenerThreadState;
		}
	}

	/**
	 * Hook method called when a new client has connected to the server.
	 * 
	 * @param client
	 *            The client that has connected to the server.
	 */
	protected abstract void clientConnected(ClientConnection client);

	/**
	 * Hook method called when a client has disconnected from the server.
	 * 
	 * @param client
	 *            The client that has disconnected from the server.
	 */
	protected abstract void clientDisconnected(ClientConnection client);

	/**
	 * Hook method called when a message from a client has been received.
	 * 
	 * @param client
	 *            The client that sent the message.
	 * @param msg
	 *            The message sent by the client.
	 */
	protected abstract void clientMessageReceived(ClientConnection client, Object msg);

	/**
	 * Hook method called when a client has thrown an exception.
	 * 
	 * @param client
	 *            The client that threw the exception.
	 * @param e
	 *            The exception that was thrown by the client.
	 */
	protected abstract void handleClientException(ClientConnection client, Exception e);

	/**
	 * Hook method called when the server has thrown an exception.
	 * 
	 * @param e
	 *            The exception thrown by the server.
	 */
	protected abstract void handleException(Exception e);

	/**
	 * Hook method called when the server is started.
	 */
	protected abstract void serverStarted();

	/**
	 * Hook method called when the server is paused.
	 */
	protected abstract void serverPaused();

	/**
	 * Hook method called when the server is stopped.
	 */
	protected abstract void serverStopped();

	/**
	 * Initializes the server socket using the port, backlog, timeout, and
	 * address.
	 * 
	 * @return boolean True if the socket was initialized successfully, false if
	 *         not.
	 */
	private boolean initialize() {
		try {
			serverSocket = address == null ? new ServerSocket(port, backlog) : new ServerSocket(port, backlog, address);
			serverSocket.setSoTimeout(timeout);
		} catch (IOException e) {
			handleException(new ConnectionException("A problem occured while intializing the socket.", e));
			return false;
		}

		return true;
	}

	/**
	 * Accessor method for the threads in the client connection thread group.
	 * 
	 * @return {@linkplain Thread}[] The array of thread stored in the thread
	 *         group.
	 */
	private Thread[] getAllConnections() {
		Thread[] connections = new Thread[clientConnections.activeCount()];
		clientConnections.enumerate(connections);

		return connections;
	}
}
