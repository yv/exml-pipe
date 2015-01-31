package de.versley.exml.pipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.config.GlobalConfig;
import exml.io.DocumentWriter;
import exml.tueba.TuebaDocument;

public class TextToEXML {
	static Options options;

	static {
		options = new Options();
		options.addOption("lang", true, "language (default:de)");
		options.addOption("pipeline", true, "pipeline (default: mate)");
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
	private static final String CONFIG_FNAME="exmlpipe_config.yaml";
	public static void main(String[] args) {
		GlobalConfig conf;
		CommandLine cmd=null;
		try {
			cmd = new PosixParser().parse(options, args);
		} catch (ParseException ex) {
			new HelpFormatter().printHelp("TextToEXML", options);
			System.exit(1);
		}
		if (new File(CONFIG_FNAME).exists()) {
			conf = GlobalConfig.load(CONFIG_FNAME);
		} else {
			conf = GlobalConfig.fromDefaults();
		}
		if (cmd.hasOption("lang")) {
			conf.language = cmd.getOptionValue("lang");
		}
		if (cmd.hasOption("pipeline")) {
			conf.default_pipeline = cmd.getOptionValue("pipeline");
		}
		if (!new File(CONFIG_FNAME).exists()) {
			conf.saveAs(CONFIG_FNAME);
		}
		List<Annotator> annotators = conf.createAnnotators();
		//BPAnnotator bp_ann = new BPAnnotator("/home/yannick/data/r6_train2.gr");
		//bp_ann.add_transform(new NodeToFunction());
		//annotators.add(bp_ann);
		try {
			ExmlDocBuilder db = new ExmlDocBuilder(conf.language);
			db.addText(readFile((String) cmd.getArgList().get(0)));
			TuebaDocument doc = db.getDocument();
			for (Annotator ann: annotators) {
				ann.annotate(doc);
			}
			OutputStream os;
			if (cmd.getArgList().size() > 1) {
				os = new FileOutputStream((String) cmd.getArgList().get(1));
			} else {
				os = System.out;
				System.err.println("writing to stdout");
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
