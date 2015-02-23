package de.versley.exml.annotators.preprocess;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.versley.exml.async.Channel;
import de.versley.exml.async.Consumer;

/**
 * interface for pre-treatments that provide word clusters or
 * normalization or other stuff.
 * The async-io-style design seems to be necessary because
 * Java's Process design makes it difficult to do it as a
 * synchronous interaction.
 * @author yannick
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface LineProcessor extends Channel<String,String>{
	void loadModels();
	void process(String input, Consumer<String> and_then);
	/**
	 * processes all pending lines and calls the corresponding and_then
	 * methods.
	 */
	void close();
}
