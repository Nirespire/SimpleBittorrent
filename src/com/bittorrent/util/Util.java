package com.bittorrent.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Util {

	/**
	 * Given file and destination where splits should go (e.g. img\splits),
	 *  will split file into chunkSize KB chunks.
	 * 
	 * @param file Original file
	 * @param rootDir Where the split files should be written
	 * @param chunkSize How many bytes each FileChunk file should hold
	 * @return numChunksCreated How many FileChunk files were created from the original file
	 */
	public static int splitFile(File file, String rootDir, int chunkSize) {
		ArrayList<FileChunk> chunks = new ArrayList<FileChunk>();
		try {
			chunks = getFileChunks(new RandomAccessFile(file, "r"), file.getName(), chunkSize);
			FileChunk[] chunkArray = chunks.toArray(new FileChunk[chunks.size()]);
			writeFileChunksToFiles(rootDir, chunkArray);
		} catch (IOException | ChordException e) {
			e.printStackTrace();
		}
		return chunks.size();
	}

	/**
	 * Returns ArrayList of FileChunk objects of the file provided
	 * 
	 * @param raf Original file in RandomAccessFile format for performance
	 * @param fileName Name of the file to be stored in each FileChunk
	 * @param chunkSize How many bytes each FileChunk file should hold
	 * @return
	 * @throws IOException
	 * @throws ChordException
	 */
	public static ArrayList<FileChunk> getFileChunks(RandomAccessFile raf, String fileName, int chunkSize) throws IOException, ChordException {

		long numChunks = raf.length() / chunkSize;
		if (raf.length() % chunkSize != 0) {
			numChunks++;
		}

		ArrayList<FileChunk> output = new ArrayList<FileChunk>();

		for (int i = 1; i < numChunks + 1; i++) {

			byte[] bytes = new byte[chunkSize];
			int numBytes = raf.read(bytes);

			if (numBytes == -1) {
				throw new ChordException(
						"getFileChunks: failed to read bytes from file to fileChunk");
			}

			FileChunk fc = new FileChunk(i, numChunks, bytes, fileName);

			output.add(fc);
		}
		return output;
	}

	/**
	 * Serializes provided FileChunks to files into provided, relative directory.
	 * Used by Server to split the original file.
	 * Used by Peer to write their received splits to files.
	 * Naming scheme is <rootDir>\<originalFileName>#<currentChunkNum>.<numberOfChunks>
	 * 
	 * @param rootDir Where the .txt file should be stored
	 * @param chunks The FileChunks that should be recorded
	 * @throws IOException
	 */
	public static void writeFileChunksToFiles(String rootDir, FileChunk[] chunks)
			throws IOException {
		for (FileChunk chunk : chunks) {
			if (chunk == null) {
				continue;
			}
			FileOutputStream fos = new FileOutputStream(rootDir + "\\" + chunk.getChunkFilename());
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(chunk);

			oos.close();
			bos.close();
			fos.close();
		}
	}

	/**
	 * Provided an array of FileChunk objects, reconstructs the original file and writes it to the
	 * provided root directory with specified name.
	 * If all parts are not provided, throws an Exception.
	 * 
	 * @param chunks List of FileChunks that make up the complete file
	 * @param newFileName Desired name of the rebuilt file
	 * @param rootDir Relative directory where the new file should be written
	 * @return File object Reference to the created file
	 * @throws IOException
	 * @throws ChordException
	 */
	public static File rebuildFileFromFileChunks(FileChunk[] chunks, String newFileName, String rootDir) throws IOException, ChordException {
		return rebuildFileFromFileChunks(new ArrayList<FileChunk>(Arrays.asList(chunks)), newFileName, rootDir);
	}

	/**
	 * Provided an ArrayList of FileChunk objects, reconstructs the original file and writes it to the
	 * provided root directory with specified name.
	 * If all parts are not provided, throws an Exception.
	 * 
	 * @param chunks List of FileChunks that make up the complete file
	 * @param newFileName Desired name of the rebuilt file
	 * @param rootDir Relative directory where the new file should be written
	 * @return File object Reference to the created file
	 * @throws IOException
	 * @throws ChordException
	 */
	public static File rebuildFileFromFileChunks(ArrayList<FileChunk> chunks, String newFileName, String rootDir) 
			throws IOException, ChordException {
		if (chunks.isEmpty()) {
			return null;
		}

		int numChunks = chunks.size();

		HashMap<Long, FileChunk> orderedChunks = new HashMap<Long, FileChunk>();

		for (FileChunk chunk : chunks) {
			orderedChunks.put(chunk.getNum(), chunk);
		}

		if (orderedChunks.size() != numChunks) {
			throw new ChordException("rebuildFileFromChunks: error, complete set of FileChunks not provided");
		}

		File f = (newFileName == null ? new File(rootDir + "\\" + orderedChunks.get(0).getFileName()) : new File(rootDir + "\\" + newFileName));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));

		for (FileChunk chunk : orderedChunks.values()) {
			bos.write(chunk.getBytes());
		}

		bos.close();

		return f;
	}

	/**
	 * Function to halt till the Enter key is pressed.
	 */
	public static void pressAnyKeyToContinue() {
		System.out.println("Press any key to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}
	}

	/**
	 * Deletes all files from a provided directory, but not the directory itself.
	 * 
	 * @param folder File referencing directory to be cleared
	 */
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
	}

}
