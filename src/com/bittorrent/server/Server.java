package com.bittorrent.server;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Scanner;

import com.bittorrent.util.ChordException;
import com.bittorrent.util.Util;

public class Server {

	private static final int S_PORT = 8888;
	public static final String ROOT_SPLIT_DIR = "img\\splits";
	public final int chunkSize = 100 * 1024;
	
	private static HashMap<File,Boolean> sentFiles = new HashMap<File,Boolean>();
	private static int numChunks = -1;

	public static void main(String[] args) throws Exception {
		System.out.println("The FileOwner is running.");

		File inputFile = null;

		// Get the file from command line arg, or ask for it
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
		
		// Split the file into fileChunk objects and write them to the splits directory
		numChunks = Util.splitFile(inputFile, ROOT_SPLIT_DIR);
		
		// Project requirement
		if(numChunks < 5){
			throw new ChordException("File must be large enough to split into at least 5, 100KB chunks");
		}

		System.out.println("Split file into " + numChunks  + " parts");
		System.out.println("Writing files into: " + ROOT_SPLIT_DIR);
		
		// TODO check use of hashmap here
		for(File f : new File("img\\splits").listFiles()){
			sentFiles.put(f, false);
		}

		// Once file is split, start listening for peers
		ServerSocket listener = new ServerSocket(S_PORT);
		System.out.println("Listening on port: " + S_PORT);

		int clientNum = 0;
		try {
			while (true) {
				// When a client connects, spawn a FileDistributer thread to handle
				new FileDistributer(listener.accept(), clientNum, numChunks).start();
				System.out.println("Client " + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		}

	}
	
	// TODO
	public synchronized boolean updateFileList(File f, boolean b){
		return sentFiles.put(f,b);
	}

}