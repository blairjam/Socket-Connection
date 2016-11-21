package com.connerblair.udp;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * The thread used by the UDPConnector to handle incomming packets.
 * 
 * @author Conner Blair
 * @version 1.0
 */
class UDPConnectorSocketReceiverThread extends Thread {
	private UDPConnector parentConnector;

	/**
	 * Creates a new instance of the UDPConnectorSocketReceiverThread, with the
	 * specified UDPConnector.
	 * 
	 * @param parentConnector
	 *            The connector that owns this thread.
	 */
	UDPConnectorSocketReceiverThread(UDPConnector parentConnector) {
		super();
		this.parentConnector = parentConnector;
	}

	/**
	 * Accepts incomming packets and calls the connector handle packet received
	 * hook method.
	 */
	@Override
	public void run() {
		// Call to connector receiver running hook.
		parentConnector.receiverRunning();

		byte[] buf = parentConnector.getByteBuffer();

		DatagramPacket toReceive = new DatagramPacket(buf, buf.length);

		// Receive packet and call packet received hook.
		while (parentConnector.isReceiverThreadRunning()) {
			try {
				parentConnector.getSocket().receive(toReceive);
			} catch (IOException e) {
				parentConnector.handleException(e);
			}

			parentConnector.handlePacketReceived(toReceive);
		}
	}
}
