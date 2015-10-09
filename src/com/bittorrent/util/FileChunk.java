package com.bittorrent.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;


public class FileChunk implements Serializable, Comparable {
	
	private static final long serialVersionUID = 1L;
	private final long num;
	private final long totalNum;
	private final byte[] bytes;
	private final String fileName;
	
	public FileChunk(int num, long totalNum, byte[] bytes, String fileName){
		this.num = num;
		this.totalNum = totalNum;
		this.bytes = bytes;
		this.fileName = fileName;
	}
	
	public File toFile() throws IOException{
		File output = new File("img\\splits\\" + fileName + "." + num + "." + totalNum);
		BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(output));
		bw.write(bytes);
		bw.close();
		return output;
	}
	
	public String getChunkFilename(){
		return fileName + "#" + num + "." + totalNum;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getFileName() {
		return fileName;
	}

	public long getNum() {
		return num;
	}

	public long getTotalNum() {
		return totalNum;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + (int) (num ^ (num >>> 32));
		result = prime * result + (int) (totalNum ^ (totalNum >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileChunk other = (FileChunk) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (num != other.num)
			return false;
		if (totalNum != other.totalNum)
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append(fileName + " " + num + "/" + totalNum);
		
		return sb.toString();
	}

	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
