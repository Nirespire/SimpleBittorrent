package com.bittorrent.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.bittorrent.util.ChordException;

public class FileDistributer extends Thread {
    
	private Socket connection;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private int clientNum; // The index number of the client (start with 0)
	private int numChunks; // The number of chunks the file was split into
	private int numPeers; // Total number of peers there are in the system
	private String rootSplitDir; // What directory to look in for the file chunks
	

	public FileDistributer(Socket connection, int no, int numChunks, int numPeers, String rootSplitDir) {
		this.connection = connection;
		this.clientNum = no;
		this.numChunks = numChunks;
		this.numPeers = numPeers;
		this.rootSplitDir = rootSplitDir;
	}

	public void run() {
		try {
			// initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());

			// Get references to all the split files created by the FileOwner
			File[] chunkFiles = new File(rootSplitDir).listFiles();

			try {
				while (true) {
					// Figure out how many chunks each client should get
				    // Round up numChunks/numPeers and make sure the last Peer gets the leftovers
				    // if numChunks % numPeers % != 0
					int numChunksPerPeer = (int)Math.ceil((double)numChunks/(double)numPeers);
					
					// Make sure last peer doesn't get any overflow
					if(Server.getNumChunksSent() + numChunksPerPeer > numChunks){
						numChunksPerPeer -= Server.getNumChunksSent() + numChunksPerPeer - numChunks;
					}
					
					// Tell the client how many chunks you are sending
					sendMessage(numChunksPerPeer);
					
					// Send chunks to client
					for(int i = 0; i < numChunksPerPeer; i++){
						sendMessage(chunkFiles[Server.incNumChunksSent()]);
					}

	                // Make sure client got chunks fine, then disconnect
					String response = (String) in.readObject();
					if (response.equals("true")) {
						break;
					}
					else{
					    throw new ChordException("FileDistributer: Peer is not satisfied with chunks it received");
					}
				}
			} catch (ClassNotFoundException e) {
				System.err.println("Data received in unknown format");
			} catch (ChordException e) {
                e.printStackTrace();
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
