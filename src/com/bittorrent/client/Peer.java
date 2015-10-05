package com.bittorrent.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import com.bittorrent.util.FileChunk;
import com.bittorrent.util.Util;

public class Peer {
	private Socket fileOwnerSocket; // socket connect to the server
	private ObjectOutputStream out; // stream write to the socket
	private ObjectInputStream in; // stream read from the socket

	private int fileOwnerPort = -1;
	private int listenPort = -1;
	private int neighborPort = -1;

	private String message; // message send to the server
	private Object MESSAGE; // capitalized message read from the server
	private boolean connectedToServer = false;

	private ArrayList<FileChunk> chunksIHave;
	private File chunksIHaveFile = new File("client\\clientChunks.txt");
	private long numChunks = -1L;

	public Peer(int fileOwnerPort, int listenPort, int neighborPort) {
		this.fileOwnerPort = fileOwnerPort;
		this.listenPort = listenPort;
		this.neighborPort = neighborPort;
	}

	private void connect() {
		try {
			fileOwnerSocket = new Socket("localhost", fileOwnerPort);
			System.out.println("Connected to localhost in port " + fileOwnerPort);
			connectedToServer = true;
			out = new ObjectOutputStream(fileOwnerSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(fileOwnerSocket.getInputStream());
		} catch (IOException e) {
			System.err.println("ERROR: unable to connect");
		}
	}
	
	private void writeChunksIHaveToFile() throws IOException{

		FileWriter fw = new FileWriter(chunksIHaveFile);
		
		for(FileChunk fc : chunksIHave){
			fw.write(fc.getFileName() + " " + fc.getNum() + " " + fc.getTotalNum() + "\n");
		}
		
		fw.close();

	}

	public void run() {
		try {
			
			connect();
			
			while (true) {

				// Read the number of files being sent to us
				MESSAGE = in.readObject();
				System.out.println(MESSAGE);

				chunksIHave = new ArrayList<FileChunk>();

				for (int i = 0; i < (Integer) MESSAGE; i++) {
					File f = (File) in.readObject();
					System.out.println(f);
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
					chunksIHave.add((FileChunk) ois.readObject());
				}

				Util.writeFileChunksToFiles("client\\splits", chunksIHave);

				for (FileChunk chunk : chunksIHave) {
					if (numChunks == -1L) {
						numChunks = chunk.getTotalNum();
						break;
					}
				}
				
				writeChunksIHaveToFile();

				sendMessage("true");

				// Connect to peer for upload

				// Connect to peer for download

				break;
			}

		} catch (ConnectException e) {
			System.err
					.println("Connection refused. You need to initiate a server first.");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				in.close();
				out.close();
				fileOwnerSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// send a message to the output stream
	void sendMessage(String msg) {
		try {
			// stream write the message
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	// main method
	public static void main(String args[]) {
		int fileOwnerPort;
		int listenPort;
		int neighborPort;
		
		Scanner input = new Scanner(System.in);
		
		if (args.length > 0) {
			fileOwnerPort = Integer.parseInt(args[0]);
		}
		else{
			System.out.println("File Owner Port: ");
			fileOwnerPort = input.nextInt();
		}
		
		if (args.length > 1) {
			listenPort = Integer.parseInt(args[1]);
		}
		else{
			System.out.println("Listening Port: ");
			listenPort = input.nextInt();
		}
		
		if (args.length > 2) {
			neighborPort = Integer.parseInt(args[2]);
		}
		else{
			System.out.println("Neighbor Port: ");
			neighborPort = input.nextInt();
		}

		Peer client = new Peer(fileOwnerPort, listenPort, neighborPort);

		client.run();
	}

	public class ClientDownloader extends Thread {
		public void run() {

		}
	}

	public class ClientUploader extends Thread {
		public void run() {

		}
	}

}