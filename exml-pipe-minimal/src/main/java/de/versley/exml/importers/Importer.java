package de.versley.exml.importers;

import de.versley.exml.async.Channel;
import exml.tueba.TuebaDocument;

import java.io.FileNotFoundException;
import java.io.IOException;

/** An Importer is an object that can import a certain type of file
 * into an ExportXML document
 */
public interface Importer extends Channel<String,TuebaDocument> {
    /** returns null (not applicable) or the basename of the file */
    String matchFilename(String fname);

    /** imports a single file */
    TuebaDocument importFile(String fname) throws FileNotFoundException, IOException;

    void setLanguage(String lang);
}
