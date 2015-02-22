package de.versley.exml.pipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.tools.ant.util.FileUtils;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.config.GlobalConfig;
import exml.io.DocumentWriter;
import exml.tueba.TuebaDocument;

public class TextToEXML {
	static Pattern SPECIAL_FILES = Pattern.compile("offsets(?:_[a-z]+)?\\.txt");
	static Options options;

	static {
		options = new Options();
		options.addOption("lang", true, "language (default:de)");
		options.addOption("pipeline", true, "pipeline (default: mate)");
	}
	
	public static TuebaDocument importFile(String fname, GlobalConfig conf) throws IOException {
		if (fname.endsWith(".txt")) {
			ExmlDocBuilder db = new ExmlDocBuilder(conf.language);
			db.addText(readFile(fname));
			return db.getDocument();
		} else if (fname.endsWith(".exml.xml")) {
			return TuebaDocument.loadDocument(fname);
		} else {
			throw new RuntimeException("Don't know how to load file:"+fname);
		}
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
			new HelpFormatter().printHelp("TextToEXML SourceFile [DestFile]", options);
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
		if (cmd.getArgList().size() < 1) {
			System.err.println("Not enough arguments.");
			new HelpFormatter().printHelp("TextToEXML SourceFile [DestFile]", options);
			System.exit(1);
		}
		List<Annotator> annotators = conf.createAnnotators();
		//BPAnnotator bp_ann = new BPAnnotator("/home/yannick/data/r6_train2.gr");
		//bp_ann.add_transform(new NodeToFunction());
		//annotators.add(bp_ann);
		String fname = (String) cmd.getArgList().get(0);
		File f_arg = new File(fname);
		if (f_arg.isDirectory()) {
			if (cmd.getArgList().size() < 2) {
				System.err.println("If the first argument is a directory, you need to specify an output directory!");
				System.exit(1);
			}
			for (File f_curr: f_arg.listFiles()) {
				File f_out = new File((String)cmd.getArgList().get(1), f_curr.getName());
				if (SPECIAL_FILES.matcher(f_curr.getName()).matches()) {
					System.err.println("Copying special file:"+f_curr.toString());
					try {
						FileUtils.getFileUtils().copyFile(f_curr, f_out);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					continue;
				}
				if (!f_out.getName().endsWith(".exml.xml")) {
					String tmp = f_out.toString();
					tmp=tmp.replaceAll("\\.(txt|html|xml)$", "");
					f_out = new File(tmp+".exml.xml");
				}
				System.err.println("Processing: "+f_out.getName());
				try {
					TuebaDocument doc = importFile(f_curr.toString(), conf);
					for (Annotator ann: annotators) {
						ann.annotate(doc);
					}
					OutputStream os;
					os = new FileOutputStream(f_out);
					DocumentWriter.writeDocument(doc, os);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		} else {
			try {
				TuebaDocument doc = importFile(fname, conf);

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
