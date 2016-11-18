package com.connerblair.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

public abstract class TCPServer {
	public static final int DEF_PORT = -1;
	public static final int DEF_BACKLOG = 10;
	public static final int DEF_TIMEOUT = 500;
	
	private int port;
	private int backlog;
	private int timeout;
	private InetAddress addr;
	
	private volatile TCPListenerState listenerThreadState = TCPListenerState.Stopped;
	
	private ThreadGroup clientConnections;
	private ServerSocket serverSocket;
	private TCPServerConnectionListenerThread listenerThread;
	
	protected TCPServer() {
	    this(DEF_PORT);
	}
	
	protected TCPServer(int port) {
	    this(port, DEF_BACKLOG);
	}
	
	protected TCPServer(int port, int backlog) {
	    this(port, backlog, DEF_TIMEOUT);
	}
	
	protected TCPServer(int port, String address) {
	    this(port, DEF_BACKLOG, address);
	}
	
	protected TCPServer(int port, int backlog, int timeout) {
		this(port, backlog, timeout, null);
	}
	
	protected TCPServer(int port, int backlog, String address) {
	    this(port, backlog, DEF_TIMEOUT, address);
	}
	
	protected TCPServer(int port, int backlog, int timeout, String address) {
		this.port = port;
		this.backlog = backlog;
		this.timeout = timeout;
		
		try {
		    addr = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
		    handleError(new ConnectionException("Host name could not be resolved. Name: " + address, e));
		    addr = null;
		}
		
		clientConnections = new ThreadGroup("ClientConnection_Threads") {
		    @Override
		    public void uncaughtException(Thread t, Throwable e) {
		        handleClientError(((ClientConnectionInputReaderThread) t).getParentConnection(), new ConnectionException("Client Error.", e));
		    }
		};
	}
	
	public final void start() {
		switch (listenerThreadState) {
		    case Running: {
		        handleError(new ConnectionException("The listener thread is already running."));
		        break;
		    }
		    case Paused: {
		        listenerThreadState = TCPListenerState.Running;
		        break;
		    }
		    case Stopped: {
		        if (!initialize()) {
		            return;
		        }
		        
		        listenerThreadState = TCPListenerState.Running;
		        
		        listenerThread = new TCPServerConnectionListenerThread(this);
		        listenerThread.start();
		        break;
		    }
		}
	}
	
	public final void pause() {
		listenerThreadState = TCPListenerState.Paused;
		serverPaused();
	}
	
	public final void stop() {
		listenerThreadState = TCPListenerState.Stopped;
		
		try {
		    listenerThread.join();
		} catch (InterruptedException e) {
		    handleError(e);
		}
		
		try {
		    serverSocket.close();
		} catch (IOException e) {
		    handleError(e);
		}
		
		serverStopped();
	}
	
	public final void sendToAllClients(Object msg) {
	    Thread[] connections = getAllConnections();
	    
	    for (Thread connection : connections) {
	        ClientConnection client = ((ClientConnectionInputReaderThread) connection).getParentConnection();
	        client.sendToClient(msg);
	    }
	}
	
	public final int getNumberOfClients() {
	    return clientConnections.activeCount();
	}
	
	final ServerSocket getServerSocket() {
	    return serverSocket;
	}
	
	final ThreadGroup getClientConnections() {
	    return clientConnections;
	}
	
	final TCPListenerState getListenerState() { 
	    return listenerThreadState;
	}
	
	protected abstract void clientConnected(ClientConnection client);
	protected abstract void clientDisconnected(ClientConnection client);
	protected abstract void clientMessageReceived(ClientConnection client, Object msg);
	protected abstract void handleClientError(ClientConnection client, Exception e);
	protected abstract void handleError(Exception e);
	protected abstract void serverStarted();
	protected abstract void serverPaused();
	protected abstract void serverStopped();
	
	private boolean initialize() {
	    try {
	        serverSocket = addr == null ? new ServerSocket(port, backlog) : new ServerSocket(port, backlog, addr);
	        serverSocket.setSoTimeout(timeout);
	    } catch (IOException e) {
	        handleError(new ConnectionException("A problem occured while intializing the socket.", e));
	        return false;
	    }
	    
	    return true;
	}
	
	private Thread[] getAllConnections() {
	    Thread[] connections = new Thread[clientConnections.activeCount()];
	    clientConnections.enumerate(connections);
	    
	    return connections;
	}
}
