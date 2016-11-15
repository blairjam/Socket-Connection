package com.connerblair.tcp;

import java.io.ObjectInputStream;
import java.net.Socket;

class ClientConnectionInputReaderThread extends Thread {
    private ClientConnection client;
    private Socket clientSocket;
    private ObjectInputStream input;
    
    ClientConnectionInputReaderThread(ThreadGroup group, ClientConnection client, Socket clientSocket, ObjectInputStream input) {
        super(group, (Runnable) null);
        this.client = client;
        this.input = input;
    }
    
    ClientConnection getClient() {
        return client;
    }
    
    @Override
    public void run() {
        
    }
}
