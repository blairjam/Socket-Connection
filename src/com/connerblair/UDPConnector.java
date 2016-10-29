package com.connerblair;

import java.net.DatagramPacket;

public abstract class UDPConnector {
	private UDPThreadManager threadManager;
	
	protected UDPConnector() {
		this(-1);
	}
	
	protected UDPConnector(int port) {
		this("localhost", port);
	}

	protected UDPConnector(String addr, int port) {
		threadManager = new UDPThreadManager(addr, port,
											 (e) -> this.handleError(e),
											 (p) -> this.handlePacketReceived(p),
											 ()  -> this.createPacketToSend());
	}
	
	public void start(String addr) {
		if (threadManager.initialize()) {
			threadManager.start();
		}
	}
	
	public void stop() {
		threadManager.stop();
	}
	
	public int getPort() {
		return threadManager.getPort();
	}
	
	public void setPort(int port) {
		threadManager.setPort(port);
	}
	
	public abstract void handleError(Exception e);
	public abstract void handlePacketReceived(DatagramPacket packet);
	public abstract DatagramPacket createPacketToSend();
}
