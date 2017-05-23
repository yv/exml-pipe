package de.versley.exml.importers;

import exml.tueba.TuebaDocument;

import java.io.FileNotFoundException;

/** Imports an ExportXML file
 */
public class ExmlImporter extends SimpleImporter {
    public ExmlImporter() {
        super(".exml.xml");
    }

    @Override
    public TuebaDocument importFile(String fname) throws FileNotFoundException {
        return TuebaDocument.loadDocument(fname);
    }
}
