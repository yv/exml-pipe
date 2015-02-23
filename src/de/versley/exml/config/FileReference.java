package de.versley.exml.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * container for a relative reference that would allow
 * to incorporate more complex file search behaviour
 * @author yannick
 *
 */
public class FileReference {
	public String relPath;
	protected GlobalConfig conf;
	public FileReference(String rp, GlobalConfig cf) {
		String s = rp;
		s = s.replace("${user.home}", System.getProperty("user.home"));
		s = s.replace("${lang}", cf.language);
		relPath = s;
		conf = cf;
	}
	
	public FileReference(String rp) {
		relPath = rp;
		conf = null;
	}
	
	public File toFile() {
		if (conf == null || relPath.startsWith("/")) {
			return new File(relPath);
		} else {
			return new File(conf.computeModelDir(), relPath);
		}
	}
	
	public String toPath() {
		return toFile().toString();
	}
	
	public InputStream toStream() throws FileNotFoundException {
		return new FileInputStream(toFile());
	}
	
	public static class Deserializer extends JsonDeserializer<FileReference> {
		private GlobalConfig _conf;
		
		public Deserializer(GlobalConfig cf) {
			_conf = cf;
		}

		@Override
		public FileReference deserialize(JsonParser jp,
				DeserializationContext ctxt) throws IOException
		{
			JsonToken t = jp.getCurrentToken();
			
			if (t == JsonToken.VALUE_STRING) {
				String s = jp.getText().trim();
				return new FileReference(s, _conf);
			} else if (t == JsonToken.VALUE_NULL) {
				return null;
			}
			throw ctxt.mappingException(handledType());
		}
	}
}
