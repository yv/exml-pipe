package de.versley.exml.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FileReference {
	public String relPath;
	protected GlobalConfig conf;
	public FileReference(String rp, GlobalConfig cf) {
		relPath = rp;
		conf = cf;
	}
	
	public FileReference(String rp) {
		relPath = rp;
		conf = null;
	}
	
	public File toFile() {
		if (conf == null) {
			return new File(relPath);
		} else {
			return new File(conf.computeModelDir(), relPath);
		}
	}
	
	public String toPath() {
		return toFile().toString();
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
			}
			throw ctxt.mappingException(handledType());
		}
	}
}
