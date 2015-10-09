
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.bittorrent.util.ChordException;
import com.bittorrent.util.FileChunk;
import com.bittorrent.util.Util;

public class UtilTest {
	@Test
	public void testGetFileChunks() throws IOException, ChordException{
		RandomAccessFile f = new RandomAccessFile("img\\Lenna.png", "r");
		ArrayList<FileChunk> output = Util.getFileChunks(f, "Lenna.png", 1024*100);
		
		Assert.assertNotNull(output);
	}
	
	@Test
	public void testWriteFileChunksToFiles() throws IOException, ChordException{
		RandomAccessFile f = new RandomAccessFile("img\\Lenna.png", "r");
		ArrayList<FileChunk> output = Util.getFileChunks(f, "Lenna.png", 1024*100);
		
		Assert.assertNotNull(output);
		
		Util.writeFileChunksToFiles("img\\splits", output.toArray(new FileChunk[output.size()]));
	}
	
	@Test
	public void testRebuildFileFromFileChunks() throws IOException, ChordException{
		RandomAccessFile f = new RandomAccessFile("img\\Lenna.png", "r");
		ArrayList<FileChunk> output = Util.getFileChunks(f, "Lenna.png", 1024*100);
		
		Assert.assertNotNull(output);
		
		Util.writeFileChunksToFiles("img\\splits", output.toArray(new FileChunk[output.size()]));
		
		Util.rebuildFileFromFileChunks(output, "LennaRebuild.png", "img");
	}
	
	@Test
	public void testSplitFile(){
		File f = new File("img\\Lenna.png");
		Util.splitFile(f, "img\\splits", 1024*100);
	}

}
