package de.versley.exml.annotators.opennlp;

import de.versley.exml.annotators.SimpleAnnotator;
import de.versley.exml.config.FileReference;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class POSAnnotator extends SimpleAnnotator {
    transient private POSModel model;

    public FileReference modelName;

    @Override
    public void loadModels() {
        if (model == null) {
            try (InputStream dataIn = modelName.toStream()) {
                model = new POSModel(dataIn);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void annotate(TuebaDocument doc) {
        POSTaggerME tagger = new POSTaggerME(model);
        for (SentenceTree tree : SentenceTree.getTrees(doc)) {
            List<TuebaTerminal> terms = tree.getTerminals();
            String[] words = new String[terms.size()];
            for (int i = 0; i < terms.size(); i++) {
                words[i] = terms.get(i).getWord();
            }
            String[] tags = tagger.tag(words);
            for (int i = 0; i < terms.size(); i++) {
                terms.get(i).setCat(tags[i]);
            }
        }
    }
}
