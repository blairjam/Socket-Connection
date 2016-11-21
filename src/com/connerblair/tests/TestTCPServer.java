package com.connerblair.tests;

import com.connerblair.tcp.ClientConnection;
import com.connerblair.tcp.TCPServer;

public class TestTCPServer extends TCPServer {
	private int clientNum = 0;

	public TestTCPServer() {
		super(4875, "localhost");
	}

	@Override
	protected void clientConnected(ClientConnection client) {
		client.setInfo("Name", Integer.toString(clientNum));
		clientNum++;
		System.out.println("Client Connected.");
	}

	@Override
	protected void clientDisconnected(ClientConnection client) {
		clientNum--;
		System.out.println("Client Disconnected.");
	}

	@Override
	protected void clientMessageReceived(ClientConnection client, Object msg) {
		String clientMsg = (String) msg;
		System.out.println(client.getInfo("Name") + ": " + clientMsg);
		
		client.sendToClient("I got your message: " + clientMsg);
	}

	@Override
	protected void handleClientException(ClientConnection client, Exception e) {
		System.out.println(client.getInfo("Name") + ": " + e.getMessage());
	}

	@Override
	protected void handleException(Exception e) {
		System.out.println(e.getMessage());
	}

	@Override
	protected void serverStarted() {
		System.out.println("Server Started.");
	}

	@Override
	protected void serverPaused() {
		System.out.println("Server Paused.");
	}

	@Override
	protected void serverStopped() {
		System.out.println("Server Stopped.");
	}
	
	public static void main(String[] args) {
		TestTCPServer server = new TestTCPServer();
		
		server.start();
	}
}
