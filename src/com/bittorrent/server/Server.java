package com.bittorrent.server;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Scanner;

import com.bittorrent.util.ChordException;
import com.bittorrent.util.Util;

public class Server {

	private static final int S_PORT = 8888;
	private static final String ROOT_SPLIT_DIR = "server\\splits";
	private static final int CHUNK_SIZE = 100 * 1024;
	private static final int NUM_PEERS = 5;
	
	private static int numChunks = -1;
	private static int numChunksSent = 0;
	
	static synchronized int incNumChunksSent(){
		return numChunksSent++;
	}
	
	static synchronized int getNumChunksSent(){
		return numChunksSent;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("The FileOwner is running.");

		File inputFile = null;

		// Get the file to be distributed from command line arg, or ask for it
		if (args.length != 0) {
			System.out.println("File provided: " + args[0]);
			inputFile = new File(args[0]);
		} else {
			Scanner input = new Scanner(System.in);
			System.out.println("What file would you like to upload: ");
			inputFile = new File(input.next());
			input.close();
			System.out.println("File Selected: " + inputFile.getAbsolutePath());
		}
		
		// Clear the splits directory
		Util.deleteFolder(new File(ROOT_SPLIT_DIR));
		
		// Split the file into fileChunk objects and write them to the splits directory
		numChunks = Util.splitFile(inputFile, ROOT_SPLIT_DIR, CHUNK_SIZE);
		
		// Project requirement: File must be split into at least 5 parts
		if(numChunks < 5){
			throw new ChordException("File must be large enough to split into at least 5, 100KB chunks");
		}

		System.out.println("Split file into " + numChunks  + " parts");
		System.out.println("Writing files into: " + ROOT_SPLIT_DIR);
		

		// Once file is split, start listening for peers
		ServerSocket listener = new ServerSocket(S_PORT);
		System.out.println("Listening on port: " + S_PORT);

		int peerNum = 0;
		try {
			while (true) {
				// When a client connects, spawn a FileDistributer thread to handle sending FileChunks
				new FileDistributer(listener.accept(), peerNum, numChunks, NUM_PEERS, ROOT_SPLIT_DIR).start();
				System.out.println("Client " + peerNum + " is connected!");
				peerNum++;
				
				// Once all the peers have connected, halt
				if(peerNum == NUM_PEERS){
					break;
				}
			}
		} finally {
			listener.close();
		}

	}

}