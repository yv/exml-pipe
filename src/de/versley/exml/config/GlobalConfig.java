package de.versley.exml.config;

import java.io.File;
import java.io.FileOutputStream;
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

public class GlobalConfig extends SimpleModule {
	private static final long serialVersionUID = -4518063863770124713L;
	public String language="de";
	public String default_pipeline="mate";
	public String modelDir = "${user.dir}/data/pynlp/${lang}";

	public Map<String, List<Annotator>> pipelines;

	
	public List<Annotator> createAnnotators() {
		List<Annotator> pipeline = pipelines.get(
				String.format("%s.%s", language, default_pipeline));
		if (pipeline == null) {
			pipeline = pipelines.get(default_pipeline);
		}
		if (pipeline == null) {
			throw new RuntimeException("No such pipeline:" +default_pipeline);
		}
		for (Annotator conf: pipeline) {
			conf.loadModels();
		}
		return pipeline;
	}
	
	public File computeModelDir() {
		String s = modelDir;
		s.replace("${user.dir}", System.getProperty("user.dir"));
		s.replace("${lang}", language);
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
}
