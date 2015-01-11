package de.versley.exml.treetransform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.versley.exml.pipe.SentenceTree;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY,
   property="@class")
public interface TreeTransformer {
	void transform(SentenceTree t);
}
