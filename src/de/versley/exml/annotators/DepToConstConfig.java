package de.versley.exml.annotators;

import java.io.File;

public class DepToConstConfig implements AnnotatorConfig {
	public String modelName;
		
	@Override
	public Annotator create(GlobalConfig conf) {
		DepToConst result = new DepToConst(new File(conf.modelDir, modelName).toString());
		return result;
	}

}
