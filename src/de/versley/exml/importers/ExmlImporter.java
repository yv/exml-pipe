package de.versley.exml.importers;

import exml.io.DocumentReader;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.InputStream;

/** Imports an ExportXML file
 */
public class ExmlImporter extends SimpleImporter {
    public ExmlImporter() {
        super(".exml.xml");
    }

    @Override
    public TuebaDocument importStream(InputStream in, String fname) throws FileNotFoundException {
        //TODO refactor TuebaDocument API and simplify here
        TuebaDocument doc=new TuebaDocument();
        try {
            DocumentReader.readDocument(doc, in);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Cannot load "+fname, ex);
        }
        doc.nodes.<TuebaNodeMarkable> addToChildList("parent", doc.node_children);
        doc.addToChildList("parent", doc.node_children);
        doc.nodes.sortChildList(doc.node_children);
        return doc;
    }
}
