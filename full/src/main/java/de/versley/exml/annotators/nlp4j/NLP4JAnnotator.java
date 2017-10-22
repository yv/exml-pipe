package de.versley.exml.annotators.nlp4j;

import de.versley.exml.annotators.SimpleAnnotator;
import de.versley.exml.util.StreamUtils;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.decode.DecodeConfig;
import edu.emory.mathcs.nlp.decode.NLPDecoder;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNEMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NLP4JAnnotator extends SimpleAnnotator {
    public List<String> annotators;
    transient private DecodeConfig config;
    transient private NLPDecoder decoder;

    @Override
    public void loadModels() {
        if (decoder == null) {
            try {
                config = new DecodeConfig(
                        StreamUtils.openResource(NLP4JAnnotator.class, "nlp4j_config.xml"));
                decoder = new NLPDecoder(config);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void annotate(TuebaDocument doc) {
        for (SentenceTree tree: SentenceTree.getTrees(doc)) {
            List<TuebaTerminal> terms = tree.getTerminals();
            List<NLPNode> nodes = new ArrayList<>(terms.size());
            NLPNode root = new NLPNode();
            root.toRoot();
            nodes.add(root);
            for (TuebaTerminal term : terms) {
                NLPNode node = new NLPNode();
                node.setID(nodes.size());
                node.setWordForm(term.getWord());
                nodes.add(node);
            }
            NLPNode[] output = decoder.decode(nodes.toArray(new NLPNode[terms.size()]));
            for (int i = 0; i < output.length; i++) {
                output[i].setID(i);
            }
            TuebaNEMarkable last_name = null;
            for (int i = 0; i < terms.size(); i++) {
                TuebaTerminal term = terms.get(i);
                NLPNode node = output[i + 1];
                term.setLemma(node.getLemma());
                term.setCat(node.getPartOfSpeechTag());
                term.setSyn_label(node.getDependencyLabel());
                NLPNode n_parent = node.getDependencyHead();
                if (n_parent != null && n_parent.getID() != 0) {
                    term.setSyn_parent(terms.get(n_parent.getID() - 1));
                }
                int posn = tree.getStart() + i;
                String s_tag = node.getNamedEntityTag();
                char char0 = s_tag.charAt(0);
                // BILOU and/or IOB decoding. TODO do we want a unified decoder for this?
                if (last_name != null && "BOU".indexOf(char0) >= 0) {
                    // the markable does not continue
                    doc.nes.addMarkable(last_name);
                    last_name = null;
                }
                if ("BU".indexOf(char0) >= 0 || char0 == 'I' && last_name == null) {
                    last_name = new TuebaNEMarkable();
                    last_name.setKind(s_tag.substring(2));
                    last_name.setStart(posn);
                    last_name.setEnd(posn + 1);
                } else if ("IL".indexOf(char0) >= 0) {
                    // the markable continues until this position
                    last_name.setEnd(posn + 1);
                }
            }
            if (last_name != null) {
                doc.nes.addMarkable(last_name);
            }
        }
    }
}
