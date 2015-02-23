package de.versley.exml.async;


public interface Channel<I,O> {
	void loadModels();
	void process(I input, Consumer<O> output);
	void close();
}
