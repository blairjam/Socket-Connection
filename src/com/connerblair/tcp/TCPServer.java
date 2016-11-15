package com.connerblair.tcp;

public abstract class TCPServer {
	public static final int DEF_PORT = -1;
	public static final int DEF_BACKLOG = 10;
	public static final int DEF_TIMEOUT = 500;
	
	private TCPServerThreadManager threadManager;
	
	protected TCPServer(int port, int backlog, int timeout) {
		this(port, backlog, timeout, null);
	}
	
	protected TCPServer(int port, int backlog, int timeout, String addr) {
		threadManager = new TCPServerThreadManager(port, backlog, timeout, addr, this);
	}
	
	public final void start() {
		threadManager.start();
	}
	
	public final void pause() {
		threadManager.pause();
	}
	
	public final void stop() {
		threadManager.stop();
	}
	
	protected abstract void handleError(Exception e);
	protected abstract void clientDisconnected(ClientConnection client);
	protected abstract void handleClientError(ClientConnection client, Exception e);
}
