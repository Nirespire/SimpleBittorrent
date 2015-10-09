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
    private final String CLIENT_ROOT_DIR = "client";
    
	private Socket fileOwnerSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private int fileOwnerPort = -1;
	private int uploadPort = -1; // port that other Peer with connect to
	private int downloadPort = -1; // port this Peer will connect to
	
	private FileChunk[] chunksIHave = null; // hold chunk i at chunksIHave[i], else entry will be null
	
	// .txt file used to record what files chunks this Peer has
	// naming scheme is: <CLIENT_ROOT_DIR>\<uploadPort>clientChunks.txt
	private File chunksIHaveFile;
	private long numChunks = -1L; // total number of unique chunks this Peer should expect
	
	// Queue to maintain this Peer's requests as well as requests from other peers
	// that cannot be fulfilled by this Peer so they can be forwarded to the next one
	private ConcurrentLinkedQueue<Integer> chunkRequests = new ConcurrentLinkedQueue<Integer>();

	public Peer(int fileOwnerPort, int listenPort, int neighborPort) {
		this.fileOwnerPort = fileOwnerPort;
		this.uploadPort = listenPort;
		this.downloadPort = neighborPort;
		chunksIHaveFile = new File(CLIENT_ROOT_DIR + "\\" + uploadPort + "clientChunks.txt");
	}

	/**
	 * Open a TCP connection to the file owner and set up
	 * Input and Output streams
	 */
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

	/**
	 * Gracefully close connection with FileOwner
	 */
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

	/**
	 * Writes to the .txt file, the current chunks
	 * this peer has.
	 * 
	 * @throws IOException
	 */
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
			
			// Read the number of files being sent to this Peer
			Object request = in.readObject();
			System.out.println("Number of chunks being sent: " + request);

			// Read in all the chunks and store them
			for (int i = 0; i < (Integer) request; i++) {
				File f = (File) in.readObject();
				System.out.println("Chunk received: " + f);
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
				FileChunk fc = (FileChunk) ois.readObject();
				// Get total number of chunks from first chunk sent
				if (chunksIHave == null) {
					numChunks = fc.getTotalNum();
					chunksIHave = new FileChunk[(int) numChunks];
				}
				chunksIHave[(int) fc.getNum() - 1] = fc;
			}

			// Write the FileChunk objects to seperate files
			Util.writeFileChunksToFiles(CLIENT_ROOT_DIR + "\\splits", chunksIHave);

			// Record what file chunks this Peer has
			writeChunksIHaveToFile();

			// Signal to FileDistributer that this Peer received all chunks OK
			sendMessage("true");

			// Gracefully disconnect from the FileDistributer
			disconnectFromFileOwner();
			
			// Open connection to upload this peer's chunks
			PeerUploader uploader = new PeerUploader(uploadPort);
			uploader.start();
			
			// Connect to peer for download
			PeerDownloader download = new PeerDownloader(downloadPort);
			download.start();


		} catch (ConnectException e) {
			System.err.println("Connection refused");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
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

	/**
	 * Returns a random FileChunk num-1 value that this peer does not have.
	 * If this peer has all its chunks, return -1;
	 * 
	 * @return random index from chunksIHave that is null, or -1
	 */
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

	/**
	 * Returns the number of FileChunks this peer currently has
	 * 
	 * @return the number of non-null elements in chunksIHave
	 */
	synchronized int countNumChunksIHave() {
		int i = 0;
		for (FileChunk f : chunksIHave) {
			if (f != null) {
				i++;
			}
		}
		return i;
	}
	
	// TODO USE THIS
	/**
	 * Inserts a FileChunk to chunksIHave at specified index
	 * 
	 * @param f FileChunk to be inserted
	 * @param i index where the chunk should be inserted
	 */
	synchronized void addToChunksIHave(FileChunk f, int i){
		chunksIHave[i] = f;
	}
	
	/**
	 * Prints chunksIHave array to console with either the
	 * chunk number or 'X' for each element
	 */
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
		private ObjectInputStream in;
		private ObjectOutputStream out;
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

		// PeerDownloader function
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
		    System.out.println("Start PeerDownloader");
		    
		    try {
		        
		        // While this Peer doesn't have all chunks
				while (countNumChunksIHave() != numChunks) {
					
					// Get chunk number of random chunk this Peer doesn't have
				    // Add it to the queue of requests to be sent
					chunkRequests.add(getChunkNumINeed());
					
					// Get request at head of queue and request it from neighbor
					int requestNum = chunkRequests.remove();
					System.out.println("Requesting chunk " + requestNum + " from neighbor");
					sendMessage(new Integer(requestNum));

					// Get requested FileChunk or get null if neighbor doesn't have it
					FileChunk response = (FileChunk) in.readObject();

					// If got it, add it to the chunksIHave
					// Update .txt file keeping track of chunks this Peer has
					if (response != null) {
						System.out.println("Received chunk " + response + " from neighbor");
						addToChunksIHave(response, (int)response.getNum()-1);
						// debug
						printChunksIHave();
						writeChunksIHaveToFile();
					}

				}
				
				System.out.println("Got all file chunks");
				System.out.println(chunksIHave.length);

				// Signal neighbor that this Peer has all chunks
				sendMessage(new Integer(-1));
				
				// Reconstruct file
				Util.rebuildFileFromFileChunks(chunksIHave, "Rebuild" + uploadPort + chunksIHave[0].getFileName(), CLIENT_ROOT_DIR);

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

		// PeerUploader function
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
			System.out.println("start PeerUploader");
			
			try {
				System.out.println("Listening for connections on " + uploadPort);
				connection = uploadingSocket.accept();
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
			
				// Listen for chunk request, send chunk or null if don't have it.
				// Continue till neighbor signals they have all chunks
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
					System.out.println("Upload socket gracefully closed");
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