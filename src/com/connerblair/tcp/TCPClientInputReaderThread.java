package com.connerblair.tcp;

class TCPClientInputReaderThread extends Thread {
	private TCPClient parentClient;

	TCPClientInputReaderThread(TCPClient parentClient) {
		super();
		this.parentClient = parentClient;
	}

	@Override
	public void run() {
		parentClient.connectionOpened();

		Object msg;

		while (parentClient.isClientReaderThreadRunning()) {
			try {
				msg = parentClient.getConnectionInputStream().readObject();
				parentClient.handleMessageFromServer(msg);
			} catch (Exception e) {
				parentClient.handleException(e);
				parentClient.closeConnection();
			}
		}
	}
}
