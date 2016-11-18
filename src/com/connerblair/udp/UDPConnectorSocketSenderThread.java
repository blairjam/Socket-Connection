package com.connerblair.udp;

import java.io.IOException;
import java.net.DatagramPacket;

class UDPConnectorSocketSenderThread extends Thread {
    private UDPConnector parentConnector;

    UDPConnectorSocketSenderThread(UDPConnector parentConnector) {
        super();
        this.parentConnector = parentConnector;
    }

    @Override
    public void run() {
        parentConnector.senderRunning();

        DatagramPacket toSend;

        while (parentConnector.isSenderThreadRunning()) {
            toSend = parentConnector.createPacketToSend();

            if (toSend == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    parentConnector.handleError(e);
                }
                
                continue;
            }
            
            try {
                parentConnector.getSocket().send(toSend);
            } catch (IOException e) {
                parentConnector.handleError(e);
            }
        }
    }
}
