package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.List;

import de.versley.exml.treetransform.TreeTransformer;


public class BPConfig implements AnnotatorConfig{
	public String modelName="r6_train2.gr";
	public List<TreeTransformer> xform = new ArrayList<TreeTransformer>();
	
	public Annotator create(GlobalConfig cf) {
		return new BPAnnotator(cf.getModelDir(), this);
	}
}
