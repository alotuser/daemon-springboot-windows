package cn.daemon.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;

public class FilesUtil {

	
	public static void copyStreamToFile(final String source, final File destination) throws IOException {
		copyStreamToFile(new ByteArrayInputStream(source.getBytes("UTF-8")), destination);
	}
	

	public static void copyStreamToFile(final InputStream input, final File destination) throws IOException {
		// does destination directory exist ?
		if (destination.getParentFile() != null && !destination.getParentFile().exists()) {
			destination.getParentFile().mkdirs();
		}

		// make sure we can write to destination
		if (destination.exists() && !destination.canWrite()) {
			final String message = "Unable to open file " + destination + " for writing.";
			throw new IOException(message);
		}

		FileOutputStream output = null;
		try {
			output = new FileOutputStream(destination);
			IOUtil.copy(input, output);
		} finally {
			IOUtil.close(input);
			IOUtil.close(output);
		}
	}
}
