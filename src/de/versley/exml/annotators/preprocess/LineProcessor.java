package de.versley.exml.annotators.preprocess;

/**
 * interface for pre-treatments that provide word clusters or
 * normalization or other stuff.
 * The async-io-style design seems to be necessary because
 * Java's Process design makes it difficult to do it as a
 * synchronous interaction.
 * @author yannick
 *
 */
public interface LineProcessor {
	void loadModels();
	void preprocess_line(String input, LineConsumer and_then);
	/**
	 * processes all pending lines and calls the corresponding and_then
	 * methods.
	 */
	void close();
}
