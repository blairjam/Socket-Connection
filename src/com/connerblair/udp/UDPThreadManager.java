package com.connerblair.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

class UDPThreadManager {
	private String addr;
	private int port;

	private volatile boolean receiverThreadRunning = false;
	private volatile boolean senderThreadRunning = false;

	private UDPConnector udpConnector;

	private DatagramSocket socket;
	private DatagramPacket packetToReceive = null;

	private Thread receiverThread;
	private Thread senderThread;

	UDPThreadManager(String addr, int port, UDPConnector udpConnector) {
		this.addr = addr;
		this.port = port;
		this.udpConnector = udpConnector;
	}

	void start() {
		if (!initialize()) {
			return;
		}
		
		receiverThreadRunning = true;
		senderThreadRunning = true;

		receiverThread = new Thread(new SocketReceiver());
		senderThread = new Thread(new SocketSender());

		receiverThread.start();
		senderThread.start();
	}

	void stop() {
		receiverThreadRunning = false;
		senderThreadRunning = false;

		try {
			receiverThread.join();
		} catch (InterruptedException e) {
			udpConnector.handleError(e);
		}

		try {
			senderThread.join();
		} catch (InterruptedException e) {
		    udpConnector.handleError(e);
		}
		
		socket.close();
	}

	void setAddr(String addr) {
		if (isRunning()) {
		    udpConnector.handleError(new ConnectionException("Cannot change address while the server is running."));
		} else {
			this.addr = addr;
		}
	}

	String getAddr() {
		return addr;
	}

	void setPort(int port) {
		if (isRunning()) {
		    udpConnector.handleError(new ConnectionException("Cannot change port while the server is running."));
		} else {
			this.port = port;
		}
	}

	int getPort() {
		return port;
	}

	boolean isRunning() {
		return receiverThreadRunning || senderThreadRunning;
	}
	
	private boolean initialize() {
		try {
			socket = addr == null ? new DatagramSocket(port) : new DatagramSocket(port, InetAddress.getByName(addr));
		} catch (UnknownHostException e) {
		    udpConnector.handleError(new ConnectionException("Host name could not be resolved. Name: " + addr, e));
			return false;
		} catch (SocketException e) {
		    udpConnector.handleError(new ConnectionException("A problem occured while initializing the socket.", e));
			return false;
		}

		return true;
	}

	private class SocketReceiver implements Runnable {
		@Override
		public void run() {
			udpConnector.receiverRunning();

			byte[] buf = new byte[256];

			packetToReceive = new DatagramPacket(buf, buf.length);
			while (receiverThreadRunning) {
				if (packetToReceive == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					    udpConnector.handleError(e);
					}

					continue;
				}

				try {
					socket.receive(packetToReceive);
				} catch (IOException e) {
				    udpConnector.handleError(e);
				}

				udpConnector.handlePacketReceived(packetToReceive);
			}
		}

	}

	private class SocketSender implements Runnable {
		@Override
		public void run() {
			udpConnector.senderRunning();
			
			while (senderThreadRunning) {
				DatagramPacket packetToSend = udpConnector.createPacketToSend();

				if (packetToSend == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					    udpConnector.handleError(e);
					}

					continue;
				}

				try {
					socket.send(packetToSend);
				} catch (IOException e) {
				    udpConnector.handleError(e);
				}
			}
		}
	}
}
