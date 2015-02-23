package de.versley.exml.annotators;

import de.versley.exml.async.Consumer;
import exml.tueba.TuebaDocument;

public abstract class SimpleAnnotator implements Annotator {

	@Override
	public void loadModels() {
	}

	@Override
	public void close() {
	}

	public void process(TuebaDocument input, Consumer<TuebaDocument> output) {
		annotate(input);
		output.consume(input);
	}

}
