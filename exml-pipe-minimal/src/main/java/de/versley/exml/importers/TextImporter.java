package de.versley.exml.importers;

import de.versley.exml.pipe.ExmlDocBuilder;
import exml.MarkableLevel;
import exml.MissingObjectException;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTopicMarkable;

import java.io.*;
import java.nio.charset.Charset;

/** Imports plain text files
 */
public class TextImporter extends SimpleImporter {
    public TextImporter() {
        super(".txt");
    }

    private static String[] readStreamParagraphs(InputStream in, String filename) throws IOException {
        String contents = readStream(in, filename);
        return contents.split("\n\n+");
    }

    private static String readStream(InputStream in, String filename) throws IOException {
        InputStreamReader rd = new InputStreamReader(in, Charset.forName("UTF-8"));
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[4096];
        int n;
        while ((n=rd.read(buf))!=-1) {
            sb.append(buf, 0, n);
        }
        rd.close();
        return sb.toString();
    }


    @Override
    public TuebaDocument importStream(InputStream in, String fname) throws IOException {
        ExmlDocBuilder db = new ExmlDocBuilder(language);
        MarkableLevel<TuebaTopicMarkable> paragraph_level = db.getDocument().topics;
        int num_paras = 0;
        for (String para_text: readStreamParagraphs(in, fname)) {
            int start_offset = db.getDocument().size();
            db.addText(para_text);
            try {
                TuebaTopicMarkable m = paragraph_level.addMarkable(start_offset, db.getDocument().size());
                ++num_paras;
                m.setXMLId(String.format("p%d", num_paras));
            } catch (MissingObjectException e) {
                throw new RuntimeException("Cannot create markable", e);
            }
        }
        return db.getDocument();
    }
}
