package de.versley.exml.annotators;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.versley.exml.async.Channel;
import exml.tueba.TuebaDocument;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface Annotator extends Channel<TuebaDocument, TuebaDocument>{
	void annotate(TuebaDocument doc);
	void loadModels();
	void close();
}
