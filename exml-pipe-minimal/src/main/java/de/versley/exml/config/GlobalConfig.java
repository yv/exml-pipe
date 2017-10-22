package de.versley.exml.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import de.versley.exml.annotators.Annotator;
import de.versley.exml.importers.ExmlImporter;
import de.versley.exml.importers.Importer;
import de.versley.exml.importers.SegmentImporter;
import de.versley.exml.importers.TextImporter;

import java.io.*;
import java.util.*;

public class GlobalConfig extends SimpleModule {
	private static final long serialVersionUID = -4518063863770124713L;
	public String language="de";
	public String default_pipeline="mate";
	public String modelDir = "${user.home}/data/pynlp/${lang}";

	public Map<String, List<Annotator>> pipelines;

	public List<Importer> importers;

    /**
     * creates the list of annotators for the selected pipeline.
     *
     * Changed (2.0.2): we don't trigger model loading here because
     * TextToEXML will do this.
     *
     * @return
     */
	public List<Annotator> createAnnotators() {
		List<Annotator> pipeline = pipelines.get(
				String.format("%s.%s", language, default_pipeline));
		if (pipeline == null) {
			pipeline = pipelines.get(default_pipeline);
		}
		if (pipeline == null) {
			throw new RuntimeException("No such pipeline:" +default_pipeline);
		}
		return pipeline;
	}

	public Optional<List<Annotator>> createAnnotators(String name) {
        List<Annotator> pipeline = pipelines.get(name);
        if (pipeline == null) {
            return Optional.empty();
        } else {
            return Optional.of(pipeline);
        }
    }

	public List<Importer> createImporters() {
	    if (importers == null) {
	        importers = defaultImporters();
        }
        synchronized(this) {
			for (Importer imp : importers) {
				imp.setLanguage(language);
				imp.loadModels();
			}
		}
        return importers;
    }

    private List<Importer> defaultImporters() {
	    List<Importer> result = new ArrayList<>();
	    result.add(new TextImporter());
	    result.add(new ExmlImporter());
	    result.add(new SegmentImporter());
	    return result;
    }

	/** returns the root directory where the models are located
	 *
	 * @return a directory as File instance
	 */
	public File computeModelDir() {
		String s = modelDir;
		s = s.replace("${user.home}", System.getProperty("user.home"));
		s = s.replace("${lang}", language);
		String env_dir = System.getenv("EXMLPIPE_MODEL_DIR");
		if (env_dir != null) {
			s = env_dir;
		}
		return new File(s);
	}
	
	public void saveAs(String fname) {
		try {
			YAMLFactory f = new YAMLFactory();
			ObjectMapper m = new ObjectMapper(f);
			YAMLGenerator gen = f.createGenerator(new FileOutputStream(fname));
			m.writeValue(gen, this);
		} catch (Exception ex) {
			throw new RuntimeException("cannot save", ex);
		}
	}
	
	public GlobalConfig() {
		addDeserializer(FileReference.class, new FileReference.Deserializer(this));
	}
	
	public static GlobalConfig fromDefaults() {
		GlobalConfig result = new GlobalConfig();
		Map<String, List<Annotator>> pipelines = new HashMap<String, List<Annotator>>();
		result.pipelines = pipelines;
		return result;
	}

	public static GlobalConfig load(InputStream in, String configFname) {
        YAMLFactory f = new YAMLFactory();
        ObjectMapper m = new ObjectMapper(f);
        GlobalConfig conf = new GlobalConfig();
        m.registerModule(conf);
        try {
            ObjectReader r = m.readerFor(GlobalConfig.class);
            ObjectReader r2 = r.withValueToUpdate(conf);
            r2.readValue(in);
            return conf;
        } catch(Exception ex) {
            throw new RuntimeException("Cannot load config:" + configFname, ex);
        }
    }

	public static GlobalConfig load(String configFname) {
        try {
            return load(new FileInputStream(configFname), configFname);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Cannot load config:" + configFname, ex);
        }
    }

	@Override
	public String getModuleName() {
		return "GlobalConfig";
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	    out.writeObject("config.v1");
		out.writeObject(language);
		out.writeObject(default_pipeline);
		out.writeObject(modelDir);
		out.writeObject(pipelines);
		out.writeObject(importers);
	}

	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
        String versionId = (String)in.readObject();
        if ("config.v1".equals(versionId)) {
            language = (String) in.readObject();
            default_pipeline = (String) in.readObject();
            modelDir = (String) in.readObject();
            pipelines = (Map<String, List<Annotator>>) in.readObject();
            importers = (List<Importer>) in.readObject();
        }
    }
	private void readObjectNoData()
			throws ObjectStreamException {
	    // No idea what this should do.
    }
}
