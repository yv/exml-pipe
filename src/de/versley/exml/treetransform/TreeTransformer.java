package de.versley.exml.treetransform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import exml.tueba.util.SentenceTree;

import java.io.Serializable;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY,
   property="@class")
public interface TreeTransformer extends Serializable {
	void transform(SentenceTree t);
}
