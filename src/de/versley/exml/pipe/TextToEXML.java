package de.versley.exml.pipe;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.cli.Options;

import de.versley.exml.annotators.DepToConst;
import de.versley.exml.annotators.MATEAnnotator;
import exml.io.DocumentWriter;
import exml.tueba.TuebaDocument;

public class TextToEXML {
	static Options options;

	static {
		options = new Options();
		options.addOption("lang", true, "language (default:de)");
	}

    // TODO: pipeline implementation around it
	// * use input/output directory and globbing
	// * detect file type (.txt/.html.pdf)
	// * implement HTML de-boilerplating with boilerpipe
	// * use Tiedemann's pdf2xml postprocessing
	// TODO: actually use some of that stuff
	// * Pipeline 1: use BerkeleyParser + lemmatizer + maybe some funtag-style function labels
	// * Pipeline 2: use MATE-tools (newest version with integrated pos+morph tagging + maybe dep2const converter
	// more complex stuff:
	// * integrate NER (marmot+clusters w/TüBa or Stanford-based)
	// * use IMSCoref or BART [mention detection + coreference]
	// * sentiment? SRL? normalization?
	// additional output options:
	// * EXML-JSON
	public static void main(String[] args) {
		MATEAnnotator mate_ann = new MATEAnnotator("/home/yannick/data/mate_models/");
		DepToConst make_const = new DepToConst();
		try {
			ExmlDocBuilder db = new ExmlDocBuilder("de");
			db.addText(readFile(args[0]));
			TuebaDocument doc = db.getDocument();
			mate_ann.annotate(doc);
			make_const.annotate(doc);
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
		rd.close();
		return sb.toString();
	}
}
