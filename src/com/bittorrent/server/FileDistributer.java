package com.bittorrent.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FileDistributer extends Thread {
	private String message; // message received from the client

	private Socket connection;
	private ObjectInputStream in; // stream read from the socket
	private ObjectOutputStream out; // stream write to the socket
	private int clientNum; // The index number of the client (start with 0)
	private int numChunks; // The number of chunks the file was split into
	private int numPeers;
	

	public FileDistributer(Socket connection, int no, int numChunks, int numPeers) {
		this.connection = connection;
		this.clientNum = no;
		this.numChunks = numChunks;
		this.numPeers = numPeers;
	}

	public void run() {
		try {
			// initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());

			File[] chunkFiles = new File(Server.ROOT_SPLIT_DIR).listFiles();

			try {
				while (true) {
					// Tell the client how many chunks you are sending
					sendMessage(numChunks/numPeers);
					// Send chunk to client
					for(int i = 0; i < numChunks/numPeers; i++){
						sendMessage(chunkFiles[Server.incNumChunksSent()]);
					}

					message = (String) in.readObject();

					// Make sure client got chunks fine, then disconnect
					if (message.equals("true")) {
						break;
					}

				}
			} catch (ClassNotFoundException classnot) {
				System.err.println("Data received in unknown format");
			}

		} catch (IOException ioException) {
			System.out.println("Disconnect with Client " + clientNum);
		} finally {
			try {
				in.close();
				out.close();
				connection.close();
				System.out.println("Disconnect with Client " + clientNum);
			} catch (IOException e) {
				System.out.println("Disconnect with Client " + clientNum);
			}
		}
	}

	public void sendMessage(Object msg) {
		try {
			out.writeObject(msg);
			out.flush();
			System.out.println("Send message: " + msg + " to Client "
					+ clientNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
