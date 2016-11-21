package com.connerblair.tests;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.connerblair.udp.UDPConnector;

public class TestUDPServer extends UDPConnector {
    private static int port = 4435;
    
    private final byte[] buf;

    private ConcurrentLinkedQueue<Packet> responses;

    public TestUDPServer() {
        super(port, "localhost");
        buf = new byte[128];
        responses = new ConcurrentLinkedQueue<Packet>();
        
    }

    @Override
    public synchronized void handleException(Exception e) {
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
        server.start();
    }

    @Override
    protected byte[] getByteBuffer() {
        return buf;
    }

    @Override
    protected synchronized void receiverRunning() {
        System.out.println("Receiver thread running.");
    }

    @Override
    protected synchronized void senderRunning() {
        System.out.println("Sender thread running.");
    }
    
    @Override
    protected void receiverStopped() {
        System.out.println("Receiver stopped.");        
    }

    @Override
    protected void senderStopped() {
        System.out.println("Sender stopped.");
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
