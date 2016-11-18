package com.connerblair.tcp;

class ClientConnectionInputReaderThread extends Thread {
    private ClientConnection parentConnection;
    
    ClientConnectionInputReaderThread(ClientConnection parentConnection) {
        this.parentConnection = parentConnection;
    }
    
    @Override
    public void run() {
        parentConnection.clientConnected();
        
        Object msg;
        
        while (parentConnection.isInputReaderThreadRunning()) {
            try {
                msg = parentConnection.getConnectionInputStream().readObject();
                parentConnection.clientMessageReceived(msg);
            } catch (Exception e) {
                parentConnection.handleClientError(e);
                parentConnection.closeConnection();
            }
        }
    }
    
    ClientConnection getParentConnection() {
        return parentConnection;
    }
}
