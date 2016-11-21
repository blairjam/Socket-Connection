package com.connerblair.udp;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * The thread used by the UDPConnector to handle sending packets.
 * 
 * @author Conner Blair
 * @version 1.0
 */
class UDPConnectorSocketSenderThread extends Thread {
	private UDPConnector parentConnector;

	/**
	 * Creates a new instance of the UDPConnectorSocketSenderThread, with the
	 * specified UDPConnector.
	 * 
	 * @param parentConnector
	 *            The connector that owns this thread.
	 */
	UDPConnectorSocketSenderThread(UDPConnector parentConnector) {
		super();
		this.parentConnector = parentConnector;
	}

	/**
	 * Handles sending of packets.
	 */
	@Override
	public void run() {
		// Call to the connector sender running hook.
		parentConnector.senderRunning();

		DatagramPacket toSend;

		while (parentConnector.isSenderThreadRunning()) {
			// Get the packet to send as defined by the server.
			toSend = parentConnector.createPacketToSend();

			// If packet is null, wait 100ms and try again.
			if (toSend == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					parentConnector.handleException(e);
				}

				continue;
			}

			// Send the packet.
			try {
				parentConnector.getSocket().send(toSend);
			} catch (IOException e) {
				parentConnector.handleException(e);
			}
		}
	}
}
