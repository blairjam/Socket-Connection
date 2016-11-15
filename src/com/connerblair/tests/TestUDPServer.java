package com.connerblair.tests;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.connerblair.udp.UDPConnector;

public class TestUDPServer extends UDPConnector {
	private static int port = 4435;
	
	private ConcurrentLinkedQueue<Packet> responses;
	
	public TestUDPServer() {
		super(port);
		responses = new ConcurrentLinkedQueue<Packet>();
	}

	@Override
	public synchronized void handleError(Exception e) {
		System.out.println(e.getMessage());
	}

	@Override
	public synchronized void handlePacketReceived(DatagramPacket packet) {
		String msg = new String(packet.getData()).trim();
		
		System.out.println("From client: " + msg);
		
		if (msg.equalsIgnoreCase("ping")) {			
			responses.offer(new Packet(packet.getAddress(), packet.getPort(), "pong"));
		}
	}

	@Override
	public synchronized DatagramPacket createPacketToSend() {
		Packet nextResponse = responses.poll();
		if (nextResponse == null) {
			return null;
		}
		
		byte[] data = nextResponse.Message.getBytes();
		
		return new DatagramPacket(data, data.length, nextResponse.Address, nextResponse.Port);
	}
	
	public static void main(String[] args) {
		TestUDPServer server = new TestUDPServer();
		server.start("localhost");
	}
	
	private class Packet {
		public InetAddress Address;
		public int Port;
		public String Message;
		
		public Packet(InetAddress address, int port, String message) {
			Address = address;
			Port = port;
			Message = message;
		}
	}
}
