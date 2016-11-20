package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import com.connerblair.exceptions.ConnectionException;

public final class ClientConnection {
    private TCPServer parentServer;
    private Socket clientSocket;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    private final Object inputReaderLock = new Object();
    private boolean inputReaderThreadRunning = false;
    private ClientConnectionInputReaderThread inputReaderThread;

    public ClientConnection(TCPServer parentServer, Socket clientSocket) {
        this.parentServer = parentServer;
        this.clientSocket = clientSocket;

        try {
            this.clientSocket.setSoTimeout(0);
        } catch (SocketException e) {
            parentServer.handleClientException(this, e);
        }

        try {
            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            parentServer.handleClientException(this, e);
        }

        synchronized (inputReaderLock) {
            inputReaderThreadRunning = true;
        }
        inputReaderThread = new ClientConnectionInputReaderThread(this);
        inputReaderThread.start();
    }

    public void sendToClient(Object msg) {
        if (clientSocket == null || output == null) {
            parentServer.handleClientException(this, new ConnectionException("Client socket does not exist."));
            return;
        }

        try {
            output.writeObject(msg);
        } catch (IOException e) {
            parentServer.handleClientException(this, e);
        }
    }

    public void closeConnection() {
        synchronized (inputReaderLock) {
            inputReaderThreadRunning = false;
        }

        try {
            inputReaderThread.join();
        } catch (InterruptedException e) {
            parentServer.handleClientException(this, e);
        }

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
            parentServer.handleClientException(this, e);
        } finally {
            output = null;
            input = null;
            clientSocket = null;
        }

        parentServer.clientDisconnected(this);
    }

    public InetAddress getInetAddress() {
        return clientSocket == null ? null : clientSocket.getInetAddress();
    }

    ObjectInputStream getConnectionInputStream() {
        return input;
    }

    boolean isInputReaderThreadRunning() {
        synchronized (inputReaderLock) {
            return inputReaderThreadRunning;
        }
    }

    void clientConnected() {
        parentServer.clientConnected(this);
    }

    void clientMessageReceived(Object msg) {
        parentServer.clientMessageReceived(this, msg);
    }

    void handleClientError(Exception e) {
        parentServer.handleClientException(this, e);
    }
}
