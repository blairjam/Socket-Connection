package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientConnection {
    private TCPServer server;
    private Socket clientSocket;
    
    private volatile boolean inputReaderThreadRunning = false;
    
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    private ClientConnectionInputReaderThread inputReaderThread;
    
    public ClientConnection(TCPServer server, Socket clientSocket, ThreadGroup clientConnections) {
        this.server = server;
        this.clientSocket = clientSocket;
        
        try {
            this.clientSocket.setSoTimeout(0);
        } catch (SocketException e) {
            server.handleClientError(this, e);
        }
        
        try {
            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            server.handleClientError(this, e);
        }
        
        inputReaderThread = new ClientConnectionInputReaderThread(clientConnections, this, this.clientSocket, input);
        inputReaderThread.start();
    }
    
    private void closeConnection() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
            
            if (input != null) {
                input.close();
            }
            
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            server.handleClientError(this, e);
        } finally {
            output = null;
            input = null;
            clientSocket = null;
        }
    }
}
