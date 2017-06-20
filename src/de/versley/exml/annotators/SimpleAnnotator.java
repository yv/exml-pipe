package de.versley.exml.annotators;

import de.versley.exml.async.Consumer;
import exml.tueba.TuebaDocument;

import java.io.Serializable;
import java.util.Locale;

public abstract class SimpleAnnotator implements Annotator, Serializable {

	@Override
	public void loadModels() {
	}

	@Override
	public void close() {
	}

	public void process(TuebaDocument input, Consumer<TuebaDocument> output) {
		Long time_before = System.nanoTime();
		annotate(input);
		Long time_after = System.nanoTime();
		System.err.format(Locale.ROOT,"  %s: %f ms (%.2f sec/MW)\n",
				this.getClass().getSimpleName(), (time_after - time_before) * 1e-6,
                (time_after - time_before) * 1e-3 / input.size());
		output.consume(input);
	}

}
