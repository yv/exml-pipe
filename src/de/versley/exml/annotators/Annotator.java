package de.versley.exml.annotators;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import exml.tueba.TuebaDocument;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface Annotator {
	void annotate(TuebaDocument doc);
	void loadModels();
	void unloadModels();
}
