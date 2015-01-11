package de.versley.exml.annotators;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY,
   property="@class")
public interface AnnotatorConfig {
	Annotator create(GlobalConfig conf);
}
