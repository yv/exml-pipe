package de.versley.exml.pipe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLStreamException;

import de.versley.exml.importers.Importer;
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
		options.addOption("gz", false, "write gzip-compressed output");
	}
	
	public static TuebaDocument importFile(String fname, GlobalConfig conf) throws IOException {
		List<Importer> importers = conf.createImporters();
		for (Importer imp: importers) {
			String basename = imp.matchFilename(fname);
			if (basename != null) {
				try {
					return imp.importFile(fname);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		return null;
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
		// TODO this seems to be redundant with getGlobalConfig
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
            boolean gzip = cmd.hasOption("gz");
            annotateDirectory(conf, pipeline, f_arg, targetDir, noclobber, gzip);
        } else {
			try {
				TuebaDocument doc = importFile(fname, conf);
				if (doc == null) {
					System.err.println("No importer found for "+fname);
					System.exit(1);
				}
				final OutputStream os;
				if (cmd.getArgList().size() > 1) {
				    String out_file =(String) cmd.getArgList().get(1);
					OutputStream os_file = new FileOutputStream(out_file);
					if (out_file.endsWith(".gz")) {
					    os = new GZIPOutputStream(os_file);
                    } else {
					    os = os_file;
                    }
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
	    annotateDirectory(conf, pipeline, sourceDir, targetDir, noclobber, false);
    }


    public static void annotateDirectory(GlobalConfig conf, Pipeline<TuebaDocument> pipeline,
                                     File sourceDir, File targetDir, boolean noclobber, boolean gzip) {
	    List<Importer> importers = conf.createImporters();
        for (File f_curr: sourceDir.listFiles()) {
            File f_out = new File(targetDir, f_curr.getName());
            TuebaDocument doc = null;
            if (SPECIAL_FILES.matcher(f_curr.getName()).matches()) {
                System.err.println("Copying special file:"+f_curr.toString());
                try {
                    FileUtils.getFileUtils().copyFile(f_curr, f_out);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                continue;
            } else {
                String basename = null;
                for (Importer imp: importers) {
                    basename = imp.matchFilename(f_curr.getName());
                    if (basename != null) {
                        try {
                            doc = imp.importFile(f_curr.getPath());
                            break;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                if (basename != null && doc != null) {
                    f_out = new File(targetDir, basename + ".exml.xml");
                } else {
                    System.err.println("No importer for file: " + f_curr.getName());
                    continue;
                }
            }
            if (noclobber && f_out.exists()) {
                try {
                    TuebaDocument doc_processed = TuebaDocument.loadDocument(f_out.getPath());
                    System.err.format("%s is a valid exml file with %d tokens, skipping",
                            f_out.getName(), doc_processed.size());
                    continue;
                } catch (IOException ex) {
                    // well, then we should re-annotate it
				}
            } else if (noclobber && new File(f_out.getAbsolutePath() + ".gz").exists()) {
                System.err.format("%s is a compressed exml file, skipping",
                        f_out.getName()+".gz");
                continue;
            }
            final File f_out_actual;
            if (gzip) {
                f_out_actual = new File(f_out.getAbsolutePath() + ".gz");
            } else {
                f_out_actual = f_out;
            }
            System.err.println("Processing: "+f_out.getName());
            try {
                pipeline.process(doc, (TuebaDocument result) -> {
                    OutputStream os;
                    try {
                        os = new FileOutputStream(f_out_actual);
                        if (gzip) {
                            os = new GZIPOutputStream(os);
                        }
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
			new HelpFormatter().printHelp("TextToEXML [-noclobber] [-pipeline pipeline] SourceDir DestDir", options);
			System.exit(1);
		}
		return conf;
	}

}
