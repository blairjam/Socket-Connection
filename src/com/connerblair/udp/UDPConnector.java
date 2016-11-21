package com.connerblair.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

/**
 * This class represents a UDP connection to a socket.
 * 
 * @author Conner Blair
 * @version 1.0
 */
public abstract class UDPConnector {
	public static final int DEF_PORT = -1;

	private int port;
	private InetAddress addr;

	private DatagramSocket socket;

	private final Object receiverLock = new Object();
	private final Object senderLock = new Object();
	private boolean receiverThreadRunning = false;
	private boolean senderThreadRunning = false;
	private UDPConnectorSocketReceiverThread receiverThread;
	private UDPConnectorSocketSenderThread senderThread;

	/**
	 * Creates a new instance of the UPDConnector class, with the default port.
	 */
	protected UDPConnector() {
		this(DEF_PORT);
	}

	/**
	 * Creates a new instance of the UDPConnector class, with the specified
	 * port.
	 * 
	 * @param port
	 *            The port to which this connector is bound.
	 */
	protected UDPConnector(int port) {
		this(port, null);
	}

	/**
	 * Creates a new instance of the UDPConnector class, with the specified port
	 * and address.
	 * 
	 * @param port
	 *            The port to which this connector is bound.
	 * @param address
	 *            The address to which this connector is bound.
	 */
	protected UDPConnector(int port, String address) {
		this.port = port;

		// Convert the string address to a InetAddress.
		try {
			addr = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
			addr = null;
		}
	}

	/**
	 * Starts the receiver and sender threads running.
	 */
	public final void start() {
		if (isRunning()) {
			handleException(new ConnectionException("This connector is already running."));
			return;
		}

		if (!initialize()) {
			return;
		}

		synchronized (receiverLock) {
			receiverThreadRunning = true;
		}
		synchronized (senderLock) {
			senderThreadRunning = true;
		}

		receiverThread = new UDPConnectorSocketReceiverThread(this);
		senderThread = new UDPConnectorSocketSenderThread(this);

		receiverThread.start();
		senderThread.start();
	}

	/**
	 * Stops the receiver and sender threads.
	 */
	public final void stop() {
		synchronized (receiverLock) {
			receiverThreadRunning = false;
		}
		synchronized (senderLock) {
			senderThreadRunning = false;
		}

		// Join the two threads.
		try {
			receiverThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

		try {
			senderThread.join();
		} catch (InterruptedException e) {
			handleException(e);
		}

		// Close the socket.
		socket.close();

		// Call the stopped hook methods.
		receiverStopped();
		senderStopped();
	}

	/**
	 * Accessor method for the port to which this connector is bound.
	 * 
	 * @return int The port of this connector.
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * Mutator method for the port to which this connector is bound. <br>
	 * If the connector is not stopped, this call will have no effect.
	 * 
	 * @param port
	 *            The new port to bind the connector to.
	 */
	public final void setPort(int port) {
		if (isRunning()) {
			handleException(new ConnectionException("Cannot change port while server is running."));
		} else {
			this.port = port;
		}
	}

	/**
	 * Accessor method for the address of the connector.
	 * 
	 * @return {@linkplain InetAddress} The address of the connector.
	 */
	public final InetAddress getAddr() {
		return addr;
	}

	/**
	 * Mutator method for the address of the connector. <br>
	 * If the connector is not stopped, this call will have no effect.
	 * 
	 * @param address
	 *            The new address of the connector.
	 */
	public final void setAddr(String address) {
		try {
			setAddr(InetAddress.getByName(address));
		} catch (UnknownHostException e) {
			handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
		}
	}

	/**
	 * Mutator method for the address of the connector. <br>
	 * If the connector is not stopped, this call will have no effect.
	 * 
	 * @param address
	 *            The new address of the connector.
	 */
	public final void setAddr(InetAddress addr) {
		if (isRunning()) {
			handleException(new ConnectionException("Cannot change address while server is running."));
		} else {
			this.addr = addr;
		}
	}

	/**
	 * Accessor method to see if the connector is running.
	 * 
	 * @return boolean True if the connector is running, false if not.
	 */
	public final boolean isRunning() {
		boolean running;

		synchronized (receiverLock) {
			running = receiverThreadRunning;
		}

		synchronized (senderLock) {
			running = running && senderThreadRunning;
		}

		return running;
	}

	/**
	 * Accessor method for the socket of the connector.
	 * 
	 * @return {@linkplain DatagramSocket} The socket of the connector.
	 */
	DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * Accessor method to see if the receiver thread is running.
	 * 
	 * @return boolean True if the receiver thread is running, false if not.
	 */
	boolean isReceiverThreadRunning() {
		synchronized (receiverLock) {
			return receiverThreadRunning;
		}
	}

	/**
	 * Accessor method to see if the receiver thread is running.
	 *
	 * @return boolean True if the sender thread is running, false if not.
	 */
	boolean isSenderThreadRunning() {
		synchronized (senderLock) {
			return senderThreadRunning;
		}
	}

	/**
	 * Accessor method for the receiver lock object.
	 * 
	 * @return {@linkplain Object} The receiver lock.
	 */
	protected Object getReceiverLock() {
		return receiverLock;
	}

	/**
	 * Accessor method for the sender lock object.
	 * 
	 * @return {@linkplain Object} The sender lock.
	 */
	protected Object getSenderLock() {
		return senderLock;
	}

	/**
	 * Hook method called when the connector throws an exception.
	 * 
	 * @param e
	 *            The exception thrown.
	 */
	protected abstract void handleException(Exception e);

	/**
	 * Hook method called when the connector
	 * 
	 * @param packet
	 */
	protected abstract void handlePacketReceived(DatagramPacket packet);

	/**
	 * Slot method called when the connector needs a byte array to store
	 * incoming data.
	 * 
	 * @return byte[] To store data in.
	 */
	protected abstract byte[] getByteBuffer();

	/**
	 * Slot method called when the connector needs a packet to send. <br>
	 * If the packet is null, this calling method skips it.
	 * 
	 * @return {@linkplain DatagramPacket} The packet to send.
	 */
	protected abstract DatagramPacket createPacketToSend();

	/**
	 * Hook method called when the receiver thread is started.
	 */
	protected abstract void receiverRunning();

	/**
	 * Hook method called when the sender thread is started.
	 */
	protected abstract void senderRunning();

	/**
	 * Hook method called when the receiver thread is stopped.
	 */
	protected abstract void receiverStopped();

	/**
	 * Hook method called when the sender thread is stopped.
	 */
	protected abstract void senderStopped();

	/**
	 * Initializes the socket with the port and address.
	 * 
	 * @return boolean True if the socket is initialized successfully, false if
	 *         not.
	 */
	private boolean initialize() {
		try {
			socket = addr == null ? new DatagramSocket(port) : new DatagramSocket(port, addr);
		} catch (SocketException e) {
			handleException(new ConnectionException("A problem occured while intilizing the socket.", e));
			return false;
		}

		return true;
	}
}
