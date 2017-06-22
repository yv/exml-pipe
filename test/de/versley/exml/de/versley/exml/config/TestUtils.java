package de.versley.exml.de.versley.exml.config;

import exml.io.DocumentReader;
import exml.tueba.TuebaDocument;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

/** Utility class for test helpers
 */
public class TestUtils {
    public static InputStream openResource(Class cls, String fname) throws IOException {
        String packagePrefix = String.format("/%s/", cls.getPackage().getName().replace('.', '/'));
        InputStream stream=cls.getResourceAsStream(packagePrefix+fname);
        if (stream == null) {
            throw new RuntimeException("Could not load resource "+packagePrefix+fname);
        }
        return stream;
    }
}
