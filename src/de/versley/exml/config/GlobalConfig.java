package de.versley.exml.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.annotators.BPAnnotator;
import de.versley.exml.annotators.MATEAnnotator;
import de.versley.exml.importers.ExmlImporter;
import de.versley.exml.importers.Importer;
import de.versley.exml.importers.SegmentImporter;
import de.versley.exml.importers.TextImporter;

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

	public List<Importer> createImporters() {
	    if (importers == null) {
	        importers = defaultImporters();
        }
        for (Importer imp: importers) {
	        imp.setLanguage(language);
	        imp.loadModels();
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
	
	public File computeModelDir() {
		String s = modelDir;
		s = s.replace("${user.home}", System.getProperty("user.home"));
		s = s.replace("${lang}", language);
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
		List<Annotator> pipeline;
		pipeline = new ArrayList<Annotator>();
		pipeline.add(new MATEAnnotator());
		pipelines.put("de.mate", pipeline);
		pipeline = new ArrayList<Annotator>();
		pipeline.add(new BPAnnotator());
		pipelines.put("de.pcfgla", pipeline);
		result.pipelines = pipelines;
		return result;
	}

	public static GlobalConfig load(String configFname) {
			YAMLFactory f = new YAMLFactory();
			ObjectMapper m = new ObjectMapper(f);
			GlobalConfig conf = new GlobalConfig();
			m.registerModule(conf);
			try {
				ObjectReader r = m.reader(GlobalConfig.class);
				ObjectReader r2 = r.withValueToUpdate(conf);
				r2.readValue(new File(configFname));

				return conf;
			} catch(Exception ex) {
				throw new RuntimeException("Cannot load config:"+configFname, ex);
			}
	}

	@Override
	public String getModuleName() {
		return "GlobalConfig";
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	    out.writeObject("config.v1");
		out.writeObject(language);
		out.writeObject(modelDir);
		out.writeObject(pipelines);
		out.writeObject(importers);
	}

	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
        String versionId = (String)in.readObject();
        if ("config.v1".equals(versionId)) {
            language = (String) in.readObject();
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
