package com.connerblair.udp;

import java.io.IOException;
import java.net.DatagramPacket;

class UDPConnectorSocketReceiverThread extends Thread {
    private UDPConnector parentConnector;
    
    UDPConnectorSocketReceiverThread(UDPConnector parentConnector) {
        super();
        this.parentConnector = parentConnector;
    }
    
    @Override
    public void run() {
        parentConnector.receiverRunning();
        
        byte[] buf = parentConnector.getByteBuffer();
        
        DatagramPacket toReceive = new DatagramPacket(buf, buf.length);
        
        while (parentConnector.isReceiverThreadRunning()) {
            try {
                parentConnector.getSocket().receive(toReceive);
            } catch (IOException e) {
                parentConnector.handleError(e);
            }
            
            parentConnector.handlePacketReceived(toReceive);
        }
    }
}
