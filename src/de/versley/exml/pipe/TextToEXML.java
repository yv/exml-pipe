package de.versley.exml.pipe;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.cli.Options;

import webcorp.tokens.DFATokenizer;
import webcorp.tokens.Token;
import exml.MarkableLevel;
import exml.MissingObjectException;
import exml.io.DocumentWriter;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaSentenceMarkable;
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
		MarkableLevel<TuebaSentenceMarkable> sentLevel = doc.sentences;
		int sent_no = 1;
		int w_no = 1;
		int sent_start = 0;
		for (Token tok: tokenizer.tokenize(string, 0)) {
			if (tok.isSentStart() && doc.size()!=sent_start) {
				try {
					TuebaSentenceMarkable m_sent = sentLevel.addMarkable(sent_start, doc.size());
					m_sent.setXMLId(String.format("s%d",  sent_no));
					sent_no++;
					w_no = 1;
					sent_start = doc.size();
				} catch (MissingObjectException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
			TuebaTerminal n = doc.createTerminal(tok.value);
			n.setXMLId(String.format("s%d_%d", sent_no, w_no));
			doc.nameForObject(n);
			// System.out.println(tok.value);
			n.setWord(tok.value);
			w_no++;
		}
		if (doc.size()!=sent_start) {
			try {
				TuebaSentenceMarkable m_sent = sentLevel.addMarkable(sent_start, doc.size());
				m_sent.setXMLId(String.format("s%d",  sent_no));
				sent_no++;
				w_no = 1;
			} catch (MissingObjectException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return doc;
	}

	public static void main(String[] args) {
		try {
			TuebaDocument doc = documentFromText(readFile(args[0]));
			OutputStream os;
			if (args.length > 1) {
				os = new FileOutputStream(args[1]);
			} else {
				os = System.out;
			}
			DocumentWriter.writeDocument(doc, os);
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
		while ((n=rd.read(buf))!=-1) {
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}
}
