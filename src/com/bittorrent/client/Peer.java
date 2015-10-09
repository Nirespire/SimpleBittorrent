package com.bittorrent.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bittorrent.util.ChordException;
import com.bittorrent.util.FileChunk;
import com.bittorrent.util.Util;

public class Peer {
	private Socket fileOwnerSocket; // socket connect to the server
	private ObjectOutputStream out; // stream write to the socket
	private ObjectInputStream in; // stream read from the socket

	private int fileOwnerPort = -1;
	private int uploadPort = -1;
	private int downloadPort = -1;

	private Object MESSAGE; // capitalized message read from the server

	private FileChunk[] chunksIHave = null;
	private File chunksIHaveFile;
	private long numChunks = -1L;
	private ConcurrentLinkedQueue<Integer> chunkRequests = new ConcurrentLinkedQueue<Integer>();

	public Peer(int fileOwnerPort, int listenPort, int neighborPort) {
		this.fileOwnerPort = fileOwnerPort;
		this.uploadPort = listenPort;
		this.downloadPort = neighborPort;
		chunksIHaveFile = new File("client\\" + uploadPort + "clientChunks.txt");
	}

	private void connectToFileOwner() {
		try {
			fileOwnerSocket = new Socket("localhost", fileOwnerPort);
			System.out.println("Connected to localhost in port "
					+ fileOwnerPort);
			out = new ObjectOutputStream(fileOwnerSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(fileOwnerSocket.getInputStream());
		} catch (IOException e) {
			System.err.println("ERROR: unable to connect");
		}
	}

	private void disconnectFromFileOwner() {
		try {
			out.flush();
			out.close();
			in.close();
			fileOwnerSocket.close();
		} catch (IOException e) {
			System.err.println("ERROR: disconnected from fileOwner");
		}

	}

	private void writeChunksIHaveToFile() throws IOException {
		FileWriter fw = new FileWriter(chunksIHaveFile);
		for (FileChunk fc : chunksIHave) {
			if (fc != null) {
				fw.write(fc.getFileName() + " " + fc.getNum() + " "
						+ fc.getTotalNum() + "\n");
			}
		}
		fw.close();

	}

	public void run() {
		try {

			connectToFileOwner();
			
			// Read the number of files being sent to us
			MESSAGE = in.readObject();
			System.out.println("Number of chunks being sent: " + MESSAGE);

			for (int i = 0; i < (Integer) MESSAGE; i++) {
				File f = (File) in.readObject();
				System.out.println("Chunk received: " + f);
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
				FileChunk fc = (FileChunk) ois.readObject();
				if (chunksIHave == null) {
					chunksIHave = new FileChunk[(int) fc.getTotalNum()];
					numChunks = fc.getTotalNum();
				}
				chunksIHave[(int) fc.getNum() - 1] = fc;
			}

			Util.writeFileChunksToFiles("client\\splits", chunksIHave);

			writeChunksIHaveToFile();

			sendMessage("true");

			disconnectFromFileOwner();
			
			// Open connection to upload this peer's chunks
			PeerUploader uploader = new PeerUploader(uploadPort);
			uploader.start();

//			System.out.println("Press any key to connect to download neighbor");
//			Util.pressAnyKeyToContinue();
			
			// Connect to peer for download
			PeerDownloader download = new PeerDownloader(downloadPort);
			download.start();


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

	// Peer function
	void sendMessage(String msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	synchronized int getChunkNumINeed() {
		Random r = new Random();

		FileChunk f = null;
		int i = -1;

		do {
			i = r.nextInt(chunksIHave.length);
			f = chunksIHave[i];
		} while (f != null);

		return i + 1;
	}

	synchronized int countNumChunksIHave() {
		int i = 0;
		for (FileChunk f : chunksIHave) {
			if (f != null) {
				i++;
			}
		}
		return i;
	}
	
	synchronized void addToChunksIHave(FileChunk f, int i){
		chunksIHave[i] = f;
	}
	
	void printChunksIHave(){
		for(FileChunk f : chunksIHave){
			if(f == null){
				System.out.print("X");
			}
			else{
				System.out.print(f.getNum());
			}
		}
		System.out.println();
	}

	public class PeerDownloader extends Thread {
		ObjectInputStream in;
		ObjectOutputStream out;
		private Socket connection;

		public PeerDownloader(int downloadPort) {
			try {
				connection = new Socket("localhost", downloadPort);
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
			} catch (IOException e) {
				System.err.println("Failed to connect to peer for download");
			}
			System.out.println("Connected to " + downloadPort);
		}

		void sendMessage(Object msg) {
			System.out.println("PeerDownloader send: " + msg);
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void run() {
			System.out.println("PeerDownloader.run()");
			try {

				while (countNumChunksIHave() != numChunks) {
					
					System.out.println("I have " + countNumChunksIHave() + " chunks");

					// request for a file chunk you don't have
					chunkRequests.add(getChunkNumINeed());
					
					int requestNum = chunkRequests.remove();
					System.out.println("Requesting chunk " + requestNum + " from neighbor");
					sendMessage(new Integer(requestNum));

					// get it or get null if neighbor doesn't have it
					FileChunk response = (FileChunk) in.readObject();

					if (response != null) {
						System.out.println("Received chunk " + response + " from neighbor");
						addToChunksIHave(response, (int)response.getNum()-1);
						printChunksIHave();
						writeChunksIHaveToFile();
					}

				}
				
				System.out.println("Got all file chunks");
				System.out.println(chunksIHave.length);

				sendMessage(new Integer(-1));
				// TODO reconstruct file
				
				Util.rebuildFileFromFileChunks(chunksIHave, "Rebuild" + uploadPort + chunksIHave[0].getFileName());

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch(IOException e){
				e.printStackTrace();
			} catch (ChordException e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
					out.close();
					connection.close();
					System.out.println("Download socket gracefully closed");
				} catch (IOException e) {
					System.out.println("Peer failed to close download socket");
				}
			}
		}
	}

	public class PeerUploader extends Thread {
		ObjectInputStream in;
		ObjectOutputStream out;
		private ServerSocket uploadingSocket;
		private Socket connection;

		public PeerUploader(int uploadPort) throws IOException {
			uploadingSocket = new ServerSocket(uploadPort);
		}

		void sendMessage(Object msg) {
			System.out.println("PeerUploader send: " + msg);
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void run() {
			System.out.println("PeerUploader.run()");
			try {
				System.out.println("Listening for connections on " + uploadPort);
				connection = uploadingSocket.accept();
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
			
				// listen for chunk request, send chunk or say don't have it,
				// till signal they are done

				while (true) {
					Integer request = (Integer) in.readObject();
					System.out.println("Upload neighbor is requesting chunk " + request);
					
					if (request.equals(new Integer(-1))) {
						System.out.println("Upload neighbor has all their chunks");
						break;
					} else {
						chunkRequests.add(request);
						System.out.println("Sending chunk " + request);
						sendMessage(chunksIHave[request - 1]);
					}
				}

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					connection.close();
					in.close();
					out.close();
					uploadingSocket.close();
				} catch (IOException e) {
					System.out.println("Peer failed to close upload socket");
				}
			}
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
		} else {
			System.out.println("File Owner Port: ");
			fileOwnerPort = input.nextInt();
		}

		if (args.length > 1) {
			listenPort = Integer.parseInt(args[1]);
		} else {
			System.out.println("Listening Port: ");
			listenPort = input.nextInt();
		}

		if (args.length > 2) {
			neighborPort = Integer.parseInt(args[2]);
		} else {
			System.out.println("Neighbor Port: ");
			neighborPort = input.nextInt();
		}

		Peer client = new Peer(fileOwnerPort, listenPort, neighborPort);

		client.run();
	}

}