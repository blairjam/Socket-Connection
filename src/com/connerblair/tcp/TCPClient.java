package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

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

	protected TCPClient() {
		this(DEF_PORT);
	}

	protected TCPClient(int port) {
		this(port, DEF_HOST);
	}

	protected TCPClient(int port, String host) {
		this.port = port;

		try {
			this.host = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			handleException(e);
			this.host = null;
		}
	}

	public final void openConnection() {
		if (isConnected()) {
			handleException(new ConnectionException("The client is already connected to the server"));
			return;
		}
		
		if (!initialize()) {
			return;
		}

		try {
			output = new ObjectOutputStream(clientSocket.getOutputStream());
			input = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			handleException(e);
			closeConnection();
		}

		clientReaderRunning = true;
		clientReaderThread = new TCPClientInputReaderThread(this);
		clientReaderThread.start();
	}

	public final void closeConnection() {
		synchronized (clientReaderLock) {
			clientReaderRunning = false;
		}

		try {
			clientReaderThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

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

		connectionClosed();
	}

	public final void sendToServer(Object msg) {
		if (clientSocket == null || output == null) {
			handleException(new ConnectionException("Client socket does not exist."));
			return;
		}

		try {
			output.writeObject(msg);
		} catch (IOException e) {
			handleException(e);
		}
	}

	public final boolean isConnected() {
		return clientReaderThread != null && clientReaderThread.isAlive();
	}

	public final int getPort() {
		return port;
	}

	public final void setPort(int port) {
		if (clientReaderRunning) {
			handleException(new ConnectionException("Can not change port while the client is running."));
		} else {
			this.port = port;
		}
	}

	public final InetAddress getHost() {
		return host;
	}

	public final void setHost(String host) {
		try {
			setHost(InetAddress.getByName(host));
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + host, e));
		}
	}

	public final void setHost(InetAddress host) {
		if (clientReaderRunning) {
			handleException(new ConnectionException("Can not chnage host addres while client is running."));
		} else {
			this.host = host;
		}
	}

	public final InetAddress getInetAddress() {
		return clientSocket.getInetAddress();
	}

	boolean isClientReaderThreadRunning() {
		synchronized (clientReaderLock) {
			return clientReaderRunning;
		}
	}

	ObjectInputStream getConnectionInputStream() {
		return input;
	}

	protected abstract void handleException(Exception e);

	protected abstract void connectionOpened();

	protected abstract void connectionClosed();

	protected abstract void handleMessageFromServer(Object msg);

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
