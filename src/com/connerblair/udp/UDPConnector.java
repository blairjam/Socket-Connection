package com.connerblair.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

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

    protected UDPConnector() {
        this(DEF_PORT);
    }

    protected UDPConnector(int port) {
        this(port, null);
    }

    protected UDPConnector(int port, String address) {
        this.port = port;

        try {
            addr = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
            addr = null;
        }
    }

    public final void start() {
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

    public final void stop() {
        synchronized (receiverLock) {
            receiverThreadRunning = false;
        }
        synchronized (senderLock) {
            senderThreadRunning = false;
        }

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

        socket.close();
    }

    public final int getPort() {
        return port;
    }

    public final void setPort(int port) {
        if (isRunning()) {
            handleException(new ConnectionException("Cannot change port while server is running."));
        } else {
            this.port = port;
        }
    }

    public final InetAddress getAddr() {
        return addr;
    }

    public final void setAddr(String address) {
        try {
            setAddr(InetAddress.getByName(address));
        } catch (UnknownHostException e) {
            handleException(new ConnectionException("Host name could not be resolved. Name: " + address, e));
            addr = null;
        }
    }

    public final void setAddr(InetAddress addr) {
        if (isRunning()) {
            handleException(new ConnectionException("Cannot change address while server is running."));
        } else {
            this.addr = addr;
        }
    }

    public final boolean isRunning() {
    	boolean running;
    	
        synchronized(receiverLock) {
        	running = receiverThreadRunning;
        }
        
        synchronized(senderLock) {
        	running = running && senderThreadRunning;
        }
        
        return running;
    }

    DatagramSocket getSocket() {
        return socket;
    }

    boolean isReceiverThreadRunning() {
        synchronized (receiverLock) {
            return receiverThreadRunning;
        }
    }

    boolean isSenderThreadRunning() {
        synchronized (senderLock) {
            return senderThreadRunning;
        }
    }

    protected Object getReceiverLock() {
        return receiverLock;
    }

    protected Object getSenderLock() {
        return senderLock;
    }

    protected abstract void handleException(Exception e);

    protected abstract void handlePacketReceived(DatagramPacket packet);

    protected abstract byte[] getByteBuffer();

    protected abstract DatagramPacket createPacketToSend();

    protected abstract void receiverRunning();

    protected abstract void senderRunning();

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
