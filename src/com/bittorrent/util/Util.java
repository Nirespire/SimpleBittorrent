package com.bittorrent.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Util {
	public static void splitFileInMemory(File f) throws IOException {
		int partCounter = 1;// I like to name parts from 001, 002, 003, ...
							// you can change it to 0 if you want 000, 001, ...

		int sizeOfFiles = 1024 * 1024;// 1MB
		byte[] buffer = new byte[sizeOfFiles];

		try (BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f))) {// try-with-resources to ensure
											// closing stream
			String name = f.getName();

			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {
				// write each chunk of data into separate file with different
				// number in name
				File newFile = new File(f.getParent(), name + "."
						+ String.format("%03d", partCounter++));
				try (FileOutputStream out = new FileOutputStream(newFile)) {
					out.write(buffer, 0, tmp);// tmp is chunk size
				}
			}
		}
	}

	public static void splitFile(RandomAccessFile raf) throws IOException {
		// RandomAccessFile raf = new RandomAccessFile("test.csv", "r");
		long numSplits = 10; // from user input, extract it from args
		long sourceSize = raf.length();
		long bytesPerSplit = sourceSize / numSplits;
		long remainingBytes = sourceSize % numSplits;

		int maxReadBufferSize = 8 * 1024; // 8KB
		for (int destIx = 1; destIx <= numSplits; destIx++) {
			BufferedOutputStream bw = new BufferedOutputStream(
					new FileOutputStream("img\\splits\\split." + destIx));
			if (bytesPerSplit > maxReadBufferSize) {
				long numReads = bytesPerSplit / maxReadBufferSize;
				long numRemainingRead = bytesPerSplit % maxReadBufferSize;
				for (int i = 0; i < numReads; i++) {
					readWrite(raf, bw, maxReadBufferSize);
				}
				if (numRemainingRead > 0) {
					readWrite(raf, bw, numRemainingRead);
				}
			} else {
				readWrite(raf, bw, bytesPerSplit);
			}
			bw.close();
		}
		if (remainingBytes > 0) {
			BufferedOutputStream bw = new BufferedOutputStream(
					new FileOutputStream("img\\splits\\split." + numSplits + 1));
			readWrite(raf, bw, remainingBytes);
			bw.close();
		}
		raf.close();
	}

	static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if (val != -1) {
			bw.write(buf);
		}
	}

	public static void main(String[] args) throws IOException {
		splitFileInMemory(new File("img\\Lenna.png"));
		splitFile(new RandomAccessFile("img\\Lenna.png", "r"));
	}

}
