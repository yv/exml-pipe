package de.versley.exml.importers;

import de.versley.exml.async.Consumer;
import exml.tueba.TuebaDocument;

import java.io.File;
import java.io.IOException;

/** Interface for an import file format
 */
public abstract class SimpleImporter implements Importer {
    public String extension;
    public String language;

    public SimpleImporter() {
        // leave extension to YAML serialization
    }

    public SimpleImporter(String ext) {
        extension = ext;
    }

    @Override
    public void setLanguage(String lang) {
        language = lang;
    }

    /** returns null (not applicable) or the basename of the file */
    public String matchFilename(String fname) {
        if (fname.endsWith(extension)) {
            File f_name = new File(fname.substring(0, fname.length()-extension.length()));
            return f_name.getName();
        }
        // not applicable -> return null
        return null;
    }

    @Override
    public void loadModels() {

    }

    @Override
    public void process(String input, Consumer<TuebaDocument> output) {
        try {
            TuebaDocument result = importFile(input);
            output.consume(result);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load file: "+input, ex);
        }
    }

    @Override
    public void close() {

    }
}
