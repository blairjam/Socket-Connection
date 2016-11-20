package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

public abstract class TCPClient {
    public static int DEF_PORT = -1;
    public static String DEF_HOST = "localhost";
    
    private int port;
    private InetAddress host;
    
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    private final Object clientReaderLock = new Object();
    private boolean clientReaderRunning = false;
    private TCPClientInputReaderThread clientReaderThread;
    
    protected TCPClient() {
        this(DEF_PORT);
    }
    
    protected TCPClient(int port) {
        this(port, DEF_HOST);
    }
    
    protected TCPClient(int port, String host) {
        this.port = port;
        
        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            handleException(e);
            this.host = null;
        }
    }
    
    public final void openConnection() {
        
    }
    
    public final void closeConnection() {
        
    }
    
    public final void sendToServer(Object msg) {
        
    }
    
    public final int getPort() {
        return port;
    }
    
    public final void setPort() {
        
    }
    
    public final InetAddress getHost() {
        return host;
    }
    
    public final void setHost(String host) {
        try {
            setHost(InetAddress.getByName(host));
        } catch (UnknownHostException e) {
            handleError(new ConnectionException("Host name could not be"));
        }
    }
    
    public final void setHost(InetAddress host) {
        if (clientReaderRunning) {
            handleException(new ConnectionException("Can not chnage host addres while client is running."));
            this.host = null;
        } else {
            this.host = host;
        }
    }
    
    public final InetAddress getInetAddress() {
        return clientSocket.getInetAddress();
    }
    
    protected abstract void handleException(Exception e);
    
    protected abstract void connectionOpened();
    
    protected abstract void connectionClosed();
    
    protected abstract void handleConnectionException(Exception e);
    
    protected abstract void handleMessageFromServer(Object msg);
    
    private boolean initilize() {
        if (host == null) {
            return false;
        }
        
        try {
            clientSocket = new Socket(host, port);
        } catch (IOException e) {
            handleException(new ConnectionException("Can not intialize socket.", e));
            return false;
        }
        
        return true;
    }
}
