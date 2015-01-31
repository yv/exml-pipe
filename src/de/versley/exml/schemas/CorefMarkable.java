package de.versley.exml.schemas;

import exml.GenericMarkable;
import exml.objects.ObjectSchema;

public class CorefMarkable extends GenericMarkable {
	public CorefMarkable(ObjectSchema<? extends GenericMarkable> schema) {
		super(schema);
	}

	public String chainId;
}
