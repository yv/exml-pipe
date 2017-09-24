package de.versley.exml.annotators;

import com.google.re2j.Pattern;
import de.versley.exml.config.FileReference;
import de.versley.iwnlp.MappingLemmatizer;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class IWNLPAnnotator extends SimpleAnnotator {
    private static Pattern uc_tags = Pattern.compile("NN|NE");
    public FileReference lemma_file;
    public FileReference pos_file;

    private transient MappingLemmatizer lemmatizer;
    private transient Map<String, String> pos_map;

    /**
     * Maps a string to a plausible form for a noun.
     * BERLIN => Berlin, berlin => Berlin, CamelCase => CamelCase
     * @param s the original string
     * @return the string with adjusted capitalization
     */
    private static String ucfirst(String s) {
        String trailing = s.substring(1);
        if (s.equals(s.toUpperCase())) {
            trailing = trailing.toLowerCase();
        }
        return s.substring(0,1).toUpperCase()+trailing;
    }

    @Override
    public void loadModels() {
        if (lemmatizer == null) {
            try (InputStream dataIn = lemma_file.toStream()) {
                lemmatizer = MappingLemmatizer.load(dataIn);
                pos_map = new HashMap<String,String>();
                if (pos_file != null) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(pos_file.toStream()));
                    String line;
                    // read POS mapping
                    while ((line = rd.readLine()) != null) {
                        String[] fields = line.split("\\s+");
                        if (fields.length >= 2) {
                            if (!fields[1].matches("NOUN|VERB|ADJ|PRON|DET|PREP")) {
                                fields[1] = "X";
                            }
                            pos_map.put(fields[0], fields[1]);
                        }
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void annotate(TuebaDocument doc) {
        for (TuebaTerminal term: doc.getTerminals()) {
            String pos = term.getCat();
            if (pos_map.containsKey(pos)) {
                pos = pos_map.get(pos);
                //System.err.format("%s: %s->%s\n", term.getWord(), term.getCat(), pos);
            }
            String lemma = lemmatizer.lemmatizeSingle(term.getWord(), pos, false);
            if (lemma == null) {
                if (uc_tags.matches(term.getCat())) {
                    lemma = ucfirst(term.getWord());
                } else {
                    lemma = term.getWord().toLowerCase();
                }
            }
            term.setLemma(lemma);
        }
    }
}
