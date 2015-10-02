package com.bittorrent.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

public class Util {
	
	// 10KB chunks
	private final static int chunkSize = 100 * 1024;
	
	// Given file and destination where splits should go, splits file into 10KB chunks
	public static void splitFile(File file, String rootDir){
		try {
			ArrayList<FileChunk> chunks = getFileChunks(new RandomAccessFile(file, "r"),file.getName());
			writeFileChunksToFiles(rootDir, chunks);
		} 
		catch (IOException | ChordException e) {
			e.printStackTrace();
		}
	}
	
	// Returns arraylist of FileChunk objects of the file provided
	public static ArrayList<FileChunk> getFileChunks(RandomAccessFile raf, String fileName) throws IOException, ChordException{

		long numChunks = raf.length()/chunkSize;
		if(raf.length() % chunkSize != 0){
			numChunks++;
		}
		
		ArrayList<FileChunk> output = new ArrayList<FileChunk>();
		
		for(int i = 1; i < numChunks+1; i++){	
			
			byte[] bytes = new byte[chunkSize];
			int numBytes = raf.read(bytes);
			
			if(numBytes == -1){
				throw new ChordException("getFileChunks: failed to read bytes from file to fileChunk");
			}
			
			FileChunk fc = new FileChunk(i, numChunks, bytes, fileName);
			
			output.add(fc);
		}
		return output;	
	}
	
	// Writes file chunks to files into provided, relative directory
	// Naming scheme is <rootDir>\<originalFileName>#<currentChunkNum>.<numberOfChunks>
	public static void writeFileChunksToFiles(String rootDir, ArrayList<FileChunk> chunks) throws IOException{
		for(FileChunk chunk : chunks){
			FileOutputStream fos = new FileOutputStream(rootDir + "\\" + chunk.getChunkFilename());
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(chunk);
			
			oos.close();
			bos.close();
			fos.close();
		}
	} 
	
	// Provided an ArrayList of FileChunk objects, reconstructs the original file.
	// If all parts are not provided, 
	public static File rebuildFileFromFileChunks(ArrayList<FileChunk> chunks, String newFileName) throws IOException, ChordException{
		if(chunks.isEmpty()){
			return null;
		}
		
		int numChunks = chunks.size();
		
		HashMap<Long, FileChunk> orderedChunks = new HashMap<Long, FileChunk>();
		
		for(FileChunk chunk : chunks){
			orderedChunks.put(chunk.getNum(), chunk);
		}
		
		if(orderedChunks.size() != numChunks){
			throw new ChordException("rebuildFileFromChunks: complete set of FileChunks not provided");
		}
		
		File f = (newFileName == null ? new File("img\\" + orderedChunks.get(0).getFileName()) : new File("img\\" + newFileName));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
		
		
		for(FileChunk chunk : orderedChunks.values()){
			bos.write(chunk.getBytes());
		}
		
		bos.close();
		
		return f;
	}

}
