package de.versley.exml.treetransform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import exml.tueba.util.SentenceTree;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY,
   property="@class")
public interface TreeTransformer {
	void transform(SentenceTree t);
}
