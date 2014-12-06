package de.versley.exml.pipe;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.apache.commons.cli.Options;
import org.apache.tools.ant.input.InputRequest;

import webcorp.tokens.DFATokenizer;
import webcorp.tokens.Token;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;

public class TextToEXML {
	static Options options;

	static {
		options = new Options();
	}

	public static TuebaDocument documentFromText(String string) {
		// TODO instantiate tokenizer, read in file, return document
		DFATokenizer tokenizer = new DFATokenizer("de");
		TuebaDocument doc = new TuebaDocument();
		for (Token tok: tokenizer.tokenize(string, 0)) {
			TuebaTerminal n = doc.createTerminal(tok.value);
		}
		return doc;
	}

	public static void main(String[] args) {
		try {
			TuebaDocument doc = documentFromText(readFile(args[0]));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static String readFile(String filename) throws IOException {
		FileInputStream in = new FileInputStream(filename);
		InputStreamReader rd = new InputStreamReader(in, Charset.forName("UTF-8"));
		StringBuffer sb = new StringBuffer();
		char[] buf = new char[4096];
		int n;
		while ((n=rd.read(buf))!=0) {
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}
}
