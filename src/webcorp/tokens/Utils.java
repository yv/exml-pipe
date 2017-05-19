package webcorp.tokens;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;


public class Utils {

	public static class ReadLines implements Iterable<String>, Iterator<String> {
		private BufferedReader buf;
		boolean at_eof=false;
		String curLine=null;
		
		public ReadLines(String fname, String encoding) throws IOException {
			buf=openTextIn(fname,encoding);
		}
		
		@Override
		public Iterator<String> iterator() {
			return this;
		}
	
		@Override
		public boolean hasNext() {
			if (curLine!=null || at_eof) {
				return !at_eof;
			} else {
				try {
					curLine=buf.readLine();
					at_eof=(curLine==null);
					if (at_eof) {
						buf.close();
					}
					return !at_eof;
				} catch (IOException ex) {
					ex.printStackTrace();
					return false;
				}
			}
		}
	
		@Override
		public String next() {
			if (!hasNext()) {
				return null;
			}
			String val=curLine;
			curLine=null;
			return val;
		}
	
		@Override
		public void remove() {
		}
		
	}
	
	public static Iterable<String> readLines(String fname) throws IOException {
		return new ReadLines(fname,"UTF-8");
	}

	public static BufferedReader openTextIn(String fname, String encoding) throws IOException {
		InputStream stream=wrapInput(fname);
		return new BufferedReader(new InputStreamReader(stream, Charset.forName(encoding)));
	}
	
	public static BufferedReader openResourceIn(String fname, String encoding) throws IOException {
		InputStream stream=Utils.class.getResourceAsStream(fname);
		if (stream == null) {
			throw new RuntimeException("Could not load resource "+fname);
		}
		return new BufferedReader(new InputStreamReader(stream, Charset.forName(encoding)));
	}

	public static BufferedReader openResourceIn(Class cls, String fname, String encoding) throws IOException {
	    String packagePrefix = String.format("/%s/", cls.getPackage().getName().replace('.', '/'));
	    System.err.format("Package: %s\n", packagePrefix);
		InputStream stream=cls.getResourceAsStream(packagePrefix+fname);
		if (stream == null) {
			throw new RuntimeException("Could not load resource "+packagePrefix+fname);
		}
		return new BufferedReader(new InputStreamReader(stream, Charset.forName(encoding)));
	}

	public static ObjectOutputStream openObjectOut(String fname) throws IOException {
		return new ObjectOutputStream(wrapOutput(fname));
	}

	public static ObjectInputStream openObjectIn(String fname) throws IOException {
		InputStream stream=wrapInput(fname);
		return new ObjectInputStream(stream);
	}
	
	private static OutputStream wrapOutput(String fname) throws IOException {
		OutputStream stream=new FileOutputStream(fname);
		if (fname.endsWith(".gz")) {
			stream=new GZIPOutputStream(stream);
		} else if (fname.endsWith(".bz") || fname.endsWith(".bz2")) {
			stream=new CBZip2OutputStream(stream);
		}
		return new BufferedOutputStream(stream);
	}
	
	private static InputStream wrapInput(String fname) throws IOException {
		InputStream stream=new BufferedInputStream(new FileInputStream(fname));
		if (fname.endsWith(".gz")) {
			stream=new GZIPInputStream(stream);
		} else if (fname.endsWith(".bz") || fname.endsWith(".bz2")) {
			stream=new CBZip2InputStream(stream);
		}
		return stream;
	}


}
