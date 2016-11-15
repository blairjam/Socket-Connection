package com.connerblair.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import com.connerblair.exceptions.ConnectionException;

class TCPServerThreadManager {
	private int port;
	private int backlog;
	private int timeout;
	private String addr;
	
	private volatile ListenerState listenerState = ListenerState.Stopped;
	
	private TCPServer server;
	private ThreadGroup clientConnections;
	private ServerSocket serverSocket;
	private Thread listenerThread;
	
	TCPServerThreadManager(int port, int backlog, int timeout, String addr, TCPServer server) {
		this.port = port;
		this.backlog = backlog;
		this.timeout = timeout;
		this.addr = addr;
		this.server = server;
		
		clientConnections = new ThreadGroup("ClientConnetion_Threads") {
		    @Override
	        public void uncaughtException(Thread t, Throwable e) {
	            server.handleClientError(((ClientConnectionInputReaderThread) t).getClient(), new ConnectionException("Client Error", e));
	        };
		};
	}	
	
	void start() {
		switch (listenerState) {
			case Running: {
				server.handleError(new ConnectionException("The listener thread is already running."));
				break;
			}
			case Paused: {
				listenerState = ListenerState.Running;
				break;
			}
			case Stopped: {
				if (!initialize()) {
					return;
				}
				
				listenerState = ListenerState.Running;
				
				listenerThread = new Thread(new ConnectionListener());
				listenerThread.start();
				break;
			}
		}
	}
	
	void pause() {
		listenerState = ListenerState.Paused;
	}
	
	void stop() {
		listenerState = ListenerState.Stopped;
		
		try {
			listenerThread.join();
		} catch (InterruptedException e) {
		    server.handleError(e);
		}
		
		try {
			serverSocket.close();
		} catch (IOException e) {
		    server.handleError(e);
		}
	}
	
	private boolean initialize() {
		try {
			serverSocket = addr == null ? new ServerSocket(port, backlog) : new ServerSocket(port, backlog, InetAddress.getByName(addr));
		} catch (UnknownHostException e) {
		    server.handleError(new ConnectionException("Host name could not be resolved. Name: " + addr, e));
			return false;
		} catch (IOException e) {
		    server.handleError(new ConnectionException("A problem occured while initializing the socket.", e));
			return false;
		}
		
		return true;
	}
	
	private enum ListenerState {
		Stopped,
		Paused,
		Running
	}
	
	private class ConnectionListener implements Runnable {
		@Override
		public void run() {
			while (listenerState != ListenerState.Stopped) {
			    // Listening thread is in a paused state. Sleep for 50ms and check again.
				if (listenerState == ListenerState.Paused) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
					
					continue;
				}
				
				try {
                    Socket clientSocket = serverSocket.accept();
                    
                    synchronized(this) {
                        new ClientConnection(server, clientSocket, clientConnections);
                    }
                } catch (IOException e) {
                    server.handleError(e);
                } 
			}
		}
	}
}
