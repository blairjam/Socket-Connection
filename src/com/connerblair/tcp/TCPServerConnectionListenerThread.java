package com.connerblair.tcp;

import java.io.IOException;
import java.net.Socket;

class TCPServerConnectionListenerThread extends Thread {
    private TCPServer parentServer;
    
    TCPServerConnectionListenerThread(TCPServer parentServer) { 
        this.parentServer = parentServer;
    }
    
    @Override
    public void run() {
        parentServer.serverStarted();
        
        while (parentServer.getListenerState() != TCPListenerState.Stopped) {
            // Listening thread is in a paused state. Sleep for 50ms and check again.
            if (parentServer.getListenerState() == TCPListenerState.Paused) {
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                }
                
                continue;
            }
            
            try {
                Socket clientSocket = parentServer.getServerSocket().accept();
                
                synchronized(this) {
                    new ClientConnection(parentServer, clientSocket);
                }
            } catch (IOException e) {
                parentServer.handleError(e);
            }
        }
    }
}
