package de.versley.exml.pipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import exml.GenericMarkable;
import exml.MarkableLevel;
import exml.MissingObjectException;
import exml.tueba.TuebaTopicMarkable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.tools.ant.util.FileUtils;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.async.Consumer;
import de.versley.exml.async.Pipeline;
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
		options.addOption("noclobber", false, "don't overwrite existing target files (default:no)");
	}
	
	public static TuebaDocument importFile(String fname, GlobalConfig conf) throws IOException {
		if (fname.endsWith(".txt")) {
			ExmlDocBuilder db = new ExmlDocBuilder(conf.language);
			MarkableLevel<TuebaTopicMarkable> paragraph_level = db.getDocument().topics;
			int num_paras = 0;
			for (String para_text: readFileParagraphs(fname)) {
			    int start_offset = db.getDocument().size();
			    db.addText(para_text);
                try {
                    TuebaTopicMarkable m = paragraph_level.addMarkable(start_offset, db.getDocument().size());
                    ++num_paras;
                    m.setXMLId(String.format("p%d", num_paras));
                } catch (MissingObjectException e) {
                    throw new RuntimeException("Cannot create markable", e);
                }
            }
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
		CommandLine cmd=null;
		try {
			cmd = new PosixParser().parse(options, args);
		} catch (ParseException ex) {
			new HelpFormatter().printHelp("TextToEXML SourceFile [DestFile]", options);
			System.exit(1);
		}
		GlobalConfig conf = getGlobalConfig(cmd);
		Pipeline<TuebaDocument> pipeline = new Pipeline<TuebaDocument>();
		for (Annotator anno: conf.createAnnotators()) {
			pipeline.addStage(anno);
		}
		pipeline.loadModels();

		String fname = (String) cmd.getArgList().get(0);
		File f_arg = new File(fname);
		if (f_arg.isDirectory()) {
			if (cmd.getArgList().size() < 2) {
				System.err.println("If the first argument is a directory, you need to specify an output directory!");
				System.exit(1);
			}
			File targetDir = new File((String)cmd.getArgList().get(1));
            boolean noclobber = cmd.hasOption("noclobber");
            annotateDirectory(conf, pipeline, f_arg, targetDir, noclobber);
        } else {
			try {
				TuebaDocument doc = importFile(fname, conf);
				final OutputStream os;
				if (cmd.getArgList().size() > 1) {
					os = new FileOutputStream((String) cmd.getArgList().get(1));
				} else {
					os = System.out;
					System.err.println("writing to stdout");
				}
				pipeline.process(doc, new Consumer<TuebaDocument>() {
					public void consume(TuebaDocument result) {
						try {
							DocumentWriter.writeDocument(result, os);
						} catch (XMLStreamException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		pipeline.close();
	}

    public static void annotateDirectory(GlobalConfig conf, Pipeline<TuebaDocument> pipeline,
                                         File sourceDir, File targetDir, boolean noclobber) {
        for (File f_curr: sourceDir.listFiles()) {
            File f_out = new File(targetDir, f_curr.getName());
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
if (noclobber &&
                    f_out.exists()) {
                try {
                    TuebaDocument doc = TuebaDocument.loadDocument(f_out.getPath());
                    System.err.format("%s is a valid exml file with %d tokens, skipping",
                            f_out.getName(), doc.size());
                    continue;
                } catch (IOException ex) {
                    // well, then we should re-annotate it
                }
            }
            final File f_out_actual = f_out;
            System.err.println("Processing: "+f_out.getName());
            try {
                TuebaDocument doc = importFile(f_curr.toString(), conf);
                pipeline.process(doc, (TuebaDocument result) -> {
                    OutputStream os;
                    try {
                        os = new FileOutputStream(f_out_actual);
                        DocumentWriter.writeDocument(result, os);
                        os.close();
                    } catch (Exception e) {
                        // TODO some kind of asynchronous error handling is needed here. Hm.
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // System.exit(1);
            }
        }
    }

    private static GlobalConfig getGlobalConfig(CommandLine cmd) {
		GlobalConfig conf;
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
		return conf;
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

	private static String[] readFileParagraphs(String filename) throws IOException {
	    String contents = readFile(filename);
	    return contents.split("\n\n+");
    }
}
