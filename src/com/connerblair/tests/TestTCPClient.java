package com.connerblair.tests;

import com.connerblair.tcp.TCPClient;

public class TestTCPClient extends TCPClient {
	
	public TestTCPClient() {
		super(4875, "localhost");
	}

	@Override
	protected void handleException(Exception e) {
		System.out.println(e.getMessage());
	}

	@Override
	protected void connectionOpened() {
		System.out.println("Connection Opened");
		sendToServer("Hello Server.");
	}

	@Override
	protected void connectionClosed() {
		System.out.println("Connected Closed");
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		String serverMsg = (String) msg;
		System.out.println("Server: " + serverMsg);		
	}
	
	public static void main(String[] args) {
		TestTCPClient client = new TestTCPClient();
		
		client.openConnection();
	}
}
