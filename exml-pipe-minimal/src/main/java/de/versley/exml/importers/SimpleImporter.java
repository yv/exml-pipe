package de.versley.exml.importers;

import de.versley.exml.async.Consumer;
import exml.tueba.TuebaDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

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
        // Gzip file => strip .gz
        if (fname.endsWith(".gz")) {
            File f_name = new File(fname.substring(0, fname.length()-3));
            return matchFilename(f_name.getName());
        }
        // not applicable -> return null
        return null;
    }

    @Override
    public void loadModels() {

    }

    public abstract TuebaDocument importStream(InputStream input, String filename)
            throws IOException;

    public TuebaDocument importFile(String input) throws IOException {
        InputStream is = new FileInputStream(input);
        if (input.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return importStream(is, input);
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
