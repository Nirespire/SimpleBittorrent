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
import java.util.Scanner;

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

	public Peer(int fileOwnerPort, int listenPort, int neighborPort) {
		this.fileOwnerPort = fileOwnerPort;
		this.uploadPort = listenPort;
		this.downloadPort = neighborPort;
		chunksIHaveFile = new File("client\\"+uploadPort+"clientChunks.txt");
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
				ObjectInputStream ois = new ObjectInputStream(
						new FileInputStream(f));
				FileChunk fc = (FileChunk) ois.readObject();
				if (chunksIHave == null) {
					chunksIHave = new FileChunk[(int) fc.getTotalNum()];
				}
				chunksIHave[(int) fc.getNum()-1] = fc;
			}

			Util.writeFileChunksToFiles("client\\splits", chunksIHave);

			writeChunksIHaveToFile();

			sendMessage("true");

			disconnectFromFileOwner();
			
			System.out.println("Listen for connection (1) or connect to neighbor(2)?");
			Scanner input = new Scanner(System.in);
			int choice = input.nextInt();
			
			PeerUploader uploader;
			PeerDownloader download;
			
			switch(choice){
			case 1:
				// Open connection to upload this peer's chunks
				uploader = new PeerUploader(uploadPort);
				uploader.run();

				// Connect to peer for download
				download = new PeerDownloader(downloadPort);
				download.run();
				break;
			case 2:
				download = new PeerDownloader(downloadPort);
				download.run();
				
				uploader = new PeerUploader(uploadPort);
				uploader.run();
				break;
				
			default:
				
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

	void sendMessage(String msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
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
			} catch (IOException e) {
				System.err.println("Failed to connect to peer for download");
			}
			System.out.println("Connected to " + downloadPort);
		}
		
		void sendMessage(Object msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void run() {
			try {
				
				while(chunksIHave.length != numChunks){
					// request for a file chunk you don't have
					
					// get it or get no
					
					// ask again till have all
					break;
				}
				
				
				// reconstruct file
				
				
			} 
			finally {
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
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void run() {
			try {
				System.out.println("Listening for connections on " + uploadPort);
				connection = uploadingSocket.accept();
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				
				
				// listen for chunk request, send chunk or say don't have it, till signal they are done
				
				while(true){
					String request = (String) in.readObject();
					if(request.equals("done")){
						break;
					}
					else{
						sendMessage(chunksIHave[Integer.parseInt(request)]);
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