package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * transfers a file from one source to the other
 * @author kluge
 *
 */
public class TransferFile {
		
	public static File copyLogFileToTmpBase(File source, boolean err, boolean append) throws IOException {
		return copyFileToBase(source, Functions.generateRandomLogFile(err, true), append);
	}

	/**
	 * copies a file
	 * @param source
	 * @param dest
	 * @return
	 * @throws IOException 
	 */
	public static File copyFileToBase(File source, File dest, boolean append) throws IOException {
		if(source == null || dest == null)
			return null;
		else if(source.getCanonicalFile().equals(dest.getCanonicalFile()))
				return dest;
		else {
			// ensure that file is deleted in not append mode
			if(append == false) {
				dest.delete();
			}
			FileChannel in = null;
			FileChannel out = null;
			FileOutputStream outStream = null;
			FileInputStream inStream = null;
			try {
				// open the streams
				outStream = new FileOutputStream(dest, append);
				inStream = 	new FileInputStream(source);
			    in = inStream.getChannel();
			    out = outStream.getChannel();
			    
	            int maxCount = 16777216;
	            long size = in.size();
	            long p = 0;
	            // transfer the stuff
	            while(p < size) {
	               p += in.transferTo(p, maxCount, out);
	            }
	            // force that stuff is written to file system
	            outStream.getFD().sync();
			} finally {
			    if (outStream != null)
			    	outStream.close();
			    if (inStream != null)
			    	inStream.close();
			}
		}
		return dest;
	}
}