package de.versley.exml.importers;

import de.versley.exml.pipe.ExmlDocBuilder;
import exml.tueba.TuebaDocument;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

/** Reads the .seg.xml format for segmented raw text
 */
public class SegmentImporter extends SimpleImporter {
    public SegmentImporter() {
        super(".seg.xml");
    }

    @Override
    public TuebaDocument importStream(InputStream is, String filename) throws IOException {
        ExmlDocBuilder db = new ExmlDocBuilder(language);
        try {
            return SegmentReader.loadStream(is, db);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Cannot load "+filename, ex);
        }
    }
}
