package com.connerblair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UDPThreadManager {
	private String addr;
	private int port;

	private volatile boolean receiverThreadRunning = false;
	private volatile boolean senderThreadRunning = false;

	private Consumer<Exception> functionHandleError;
	private Consumer<DatagramPacket> functionHandlePacketReceived;
	private Supplier<DatagramPacket> functionCreatePacketToSend;

	private DatagramSocket socket;
	private DatagramPacket packetToReceive = null;

	private SocketReceiver receiver;
	private SocketSender sender;

	private Thread receiverThread;
	private Thread senderThread;

	public UDPThreadManager(String addr, int port, 
							Consumer<Exception> functionHandleError,
							Consumer<DatagramPacket> functionHandlePacketReceived,
							Supplier<DatagramPacket> functionCreatePacketToSend) {
		this.addr = addr;
		this.port = port;
		this.functionHandleError = functionHandleError;
		this.functionHandlePacketReceived = functionHandlePacketReceived;
		this.functionCreatePacketToSend = functionCreatePacketToSend;
	}

	public boolean initialize() {
		InetAddress host = null;
		try {
			host = InetAddress.getByName(addr);
		} catch(Exception e) {
			functionHandleError.accept(new UDPCustomException("Host name could not be resolved. Name: " + addr));
			return false;
		}
		
		try {
			socket = new DatagramSocket(port, host);
			receiver = new SocketReceiver();
			sender = new SocketSender();
		} catch (SocketException e) {
			functionHandleError.accept(e);
			return false;
		}
		
		return true;
	}

	public void start() {
		receiverThreadRunning = true;
		senderThreadRunning = true;

		receiverThread = new Thread(receiver);
		senderThread = new Thread(sender);

		receiverThread.start();
		senderThread.start();
	}

	public void stop() {
		receiverThreadRunning = false;
		senderThreadRunning = false;

		try {
			receiverThread.join();
		} catch (InterruptedException e) {
			functionHandleError.accept(e);
		}

		try {
			senderThread.join();
		} catch (InterruptedException e) {
			functionHandleError.accept(e);
		}
	}
	
	public void setAddr(String addr) {
		this.addr = addr;
	}
	
	public String getAddr() {
		return addr;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	private class SocketReceiver implements Runnable {
		public void run() {
			System.out.println("Receiver running.");
			
			byte[] buf = new byte[256];
			
			packetToReceive = new DatagramPacket(buf, buf.length);
			while (receiverThreadRunning) {
				if (packetToReceive == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						functionHandleError.accept(e);
					}

					continue;
				}

				try {
					socket.receive(packetToReceive);
				} catch (IOException e) {
					functionHandleError.accept(e);
				}

				functionHandlePacketReceived.accept(packetToReceive);
			}
		}

	}

	private class SocketSender implements Runnable {
		public void run() {
			System.out.println("Sender running");
			while (senderThreadRunning) {
				DatagramPacket packetToSend = functionCreatePacketToSend.get();

				if (packetToSend == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						functionHandleError.accept(e);
					}

					continue;
				}

				try {
					socket.send(packetToSend);
				} catch (IOException e) {
					functionHandleError.accept(e);
				}
			}
		}
	}
}
