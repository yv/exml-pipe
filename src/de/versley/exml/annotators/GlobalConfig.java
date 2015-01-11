package de.versley.exml.annotators;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class GlobalConfig {
	public String language="de";
	public String default_pipeline="mate";
	public File modelDir = new File(
			System.getProperty("user.home"), "data/mate_models/");
	

	public Map<String, List<AnnotatorConfig>> pipelines;

	
	public List<Annotator> createAnnotators() {
		List<Annotator> annotators;
		List<AnnotatorConfig> pipeline = pipelines.get(
				String.format("%s.%s", language, default_pipeline));
		if (pipeline == null) {
			pipeline = pipelines.get(default_pipeline);
		}
		if (pipeline == null) {
			throw new RuntimeException("No such pipeline:" +default_pipeline);
		}
		annotators = new ArrayList<Annotator>();
		for (AnnotatorConfig conf: pipeline) {
			annotators.add(conf.create(this));
		}
		return annotators;
	}
	
	public File getModelDir() {
		return modelDir;
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
	}
	
	public static GlobalConfig fromDefaults() {
		GlobalConfig result = new GlobalConfig();
		Map<String, List<AnnotatorConfig>> pipelines = new HashMap<String, List<AnnotatorConfig>>();
		List<AnnotatorConfig> pipeline;
		pipeline = new ArrayList<AnnotatorConfig>();
		pipeline.add(new MATEConfig());
		pipelines.put("de.mate", pipeline);
		pipeline = new ArrayList<AnnotatorConfig>();
		pipeline.add(new BPConfig());
		pipelines.put("de.pcfgla", pipeline);
		result.pipelines = pipelines;
		return result;
	}

	public static GlobalConfig load(String configFname) {
			YAMLFactory f = new YAMLFactory();
			ObjectMapper m = new ObjectMapper(f);
			try {
				return m.readValue(new File(configFname), GlobalConfig.class);
			} catch(Exception ex) {
				throw new RuntimeException("Cannot load config:"+configFname, ex);
			}
	}
}
