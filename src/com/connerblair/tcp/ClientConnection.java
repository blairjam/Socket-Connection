package com.connerblair.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import com.connerblair.exceptions.ConnectionException;

public final class ClientConnection {
    private TCPServer server;
    private Socket clientSocket;

    private volatile boolean inputReaderThreadRunning = false;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    private InputReaderThread inputReaderThread;

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

        inputReaderThreadRunning = true;
        inputReaderThread = new InputReaderThread(clientConnections, this);
        inputReaderThread.start();
    }

    public void sendToClient(Object msg) {
        if (clientSocket == null || output == null) {
            server.handleClientError(this, new ConnectionException("Client socket does not exist."));
            return;
        }

        try {
            output.writeObject(msg);
        } catch (IOException e) {
            server.handleClientError(this, e);
        }
    }

    public void closeConnection() {
        inputReaderThreadRunning = false;

        try {
            inputReaderThread.join();
        } catch (InterruptedException e) {
            server.handleClientError(this, e);
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
            server.handleClientError(this, e);
        } finally {
            output = null;
            input = null;
            clientSocket = null;
        }

        server.clientDisconnected(this);
    }

    public InetAddress getInetAddress() {
        return clientSocket == null ? null : clientSocket.getInetAddress();
    }

    private class InputReaderThread extends Thread implements ClientConnectable {
        private ClientConnection client;

        public InputReaderThread(ThreadGroup group, ClientConnection client) {
            super(group, (Runnable) null);
            this.client = client;
        }

        @Override
        public void run() {
            server.clientConnected(client);

            Object msg;

            while (inputReaderThreadRunning) {
                try {
                    msg = input.readObject();
                    server.clientMessageReceived(client, msg);
                } catch (Exception e) {
                    server.handleClientError(client, e);
                    closeConnection();
                }
            }
        }

        @Override
        public ClientConnection getClient() {
            return client;
        }
    }
}
