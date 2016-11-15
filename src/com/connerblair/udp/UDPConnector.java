package com.connerblair.udp;

import java.net.DatagramPacket;

public abstract class UDPConnector {
	private static final int DEF_PORT = -1;
	
	private UDPThreadManager threadManager;
	
	protected UDPConnector() {
		this(DEF_PORT);
	}
	
	protected UDPConnector(int port) {
		this(null, port);
	}

	protected UDPConnector(String addr, int port) {
		threadManager = new UDPThreadManager(addr, port, this);
	}
	
	public final void start() {
		threadManager.start();
	}
	
	public final void stop() {
		threadManager.stop();
	}
	
	public final int getPort() {
		return threadManager.getPort();
	}
	
	public final void setPort(int port) {
		threadManager.setPort(port);
	}
	
	public final String getAddr() {
		return threadManager.getAddr();
	}
	
	public final void setAddr(String addr) {
		threadManager.setAddr(addr);
	}
	
	protected abstract void handleError(Exception e);
    protected abstract void handlePacketReceived(DatagramPacket packet);
    protected abstract DatagramPacket createPacketToSend();
    protected abstract void receiverRunning();
    protected abstract void senderRunning();
}
