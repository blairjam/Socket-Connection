package com.connerblair.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

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

	protected TCPServer() {
		this(DEF_PORT);
	}

	protected TCPServer(int port) {
		this(port, DEF_BACKLOG);
	}

	protected TCPServer(int port, int backlog) {
		this(port, backlog, DEF_TIMEOUT);
	}

	protected TCPServer(int port, String address) {
		this(port, DEF_BACKLOG, address);
	}

	protected TCPServer(int port, int backlog, int timeout) {
		this(port, backlog, timeout, null);
	}

	protected TCPServer(int port, int backlog, String address) {
		this(port, backlog, DEF_TIMEOUT, address);
	}

	protected TCPServer(int port, int backlog, int timeout, String address) {
		this.port = port;
		this.backlog = backlog;
		this.timeout = timeout;

		try {
			this.address = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
			this.address = null;
		}

		clientConnections = new ThreadGroup("ClientConnection_Threads") {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				handleClientException(((ClientConnectionInputReaderThread) t).getParentConnection(),
						new ConnectionException("Client Error.", e));
			}
		};
	}

	public final void start() {
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
				if (!initialize()) {
					return;
				}

				listenerThreadState = TCPListenerState.Running;

				listenerThread = new TCPServerConnectionListenerThread(this);
				listenerThread.start();
				break;
			}
			}
		}
	}

	public final void pause() {
		synchronized (listenerLock) {
			listenerThreadState = TCPListenerState.Paused;
		}
		serverPaused();
	}

	public final void stop() {
		synchronized (listenerLock) {
			listenerThreadState = TCPListenerState.Stopped;
		}

		try {
			listenerThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			handleException(e);
		}

		serverStopped();
	}

	public final void sendToAllClients(Object msg) {
		Thread[] connections = getAllConnections();

		for (Thread connection : connections) {
			ClientConnection client = ((ClientConnectionInputReaderThread) connection).getParentConnection();
			client.sendToClient(msg);
		}
	}

	public final int getNumberOfClients() {
		return clientConnections.activeCount();
	}

	public final int getPort() {
		return port;
	}

	public final void setPort(int port) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the port while the server is running."));
			return;
		}
		
		this.port = port;
	}

	public final int getBacklog() {
		return backlog;
	}

	public final void setBacklog(int backlog) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the backlog while the server is running."));
			return;
		}
		
		this.backlog = backlog;
	}

	public final int getTimeout() {
		return timeout;
	}

	public final void setTimeout(int timeout) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the timeout while the server is running."));
			return;
		}
		
		this.timeout = timeout;
	}

	public final InetAddress getAddress() {
		return address;
	}

	public final void setAddress(String address) {
		try {
			setAddress(InetAddress.getByName(address));
		} catch(UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
		}
	}

	public final void setAddress(InetAddress address) {
		if (!isStopped()) {
			handleException(new ConnectionException("Cannot change the address while the server is running."));
			return;
		}
		
		this.address = address;
	}

	public final boolean isRunning() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Running;
		}
	}

	public final boolean isPaused() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Paused;
		}
	}

	public final boolean isStopped() {
		synchronized (listenerLock) {
			return listenerThreadState == TCPListenerState.Stopped;
		}
	}

	ServerSocket getServerSocket() {
		return serverSocket;
	}

	ThreadGroup getClientConnections() {
		return clientConnections;
	}

	TCPListenerState getListenerState() {
		synchronized (listenerLock) {
			return listenerThreadState;
		}
	}

	protected abstract void clientConnected(ClientConnection client);

	protected abstract void clientDisconnected(ClientConnection client);

	protected abstract void clientMessageReceived(ClientConnection client, Object msg);

	protected abstract void handleClientException(ClientConnection client, Exception e);

	protected abstract void handleException(Exception e);

	protected abstract void serverStarted();

	protected abstract void serverPaused();

	protected abstract void serverStopped();

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

	private Thread[] getAllConnections() {
		Thread[] connections = new Thread[clientConnections.activeCount()];
		clientConnections.enumerate(connections);

		return connections;
	}
}
