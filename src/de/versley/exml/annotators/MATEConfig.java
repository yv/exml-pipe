package de.versley.exml.annotators;


public class MATEConfig implements AnnotatorConfig {

	public String lemma_fname="lemma-ger-3.6.model";
	public String parser_fname="pet-ger-S2a-40-0.25-0.1-2-2-ht4-hm4-kk0";

	public Annotator create(GlobalConfig cf) {
		return new MATEAnnotator(cf.getModelDir(), this);
	}

}
