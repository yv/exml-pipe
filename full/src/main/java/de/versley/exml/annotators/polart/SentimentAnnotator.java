package de.versley.exml.annotators.polart;

import de.versley.exml.annotators.SimpleAnnotator;
import de.versley.exml.config.FileReference;
import exml.objects.Attribute;
import exml.objects.StringConverter;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;
import exml.tueba.TuebaTerminalSchema;
import exml.tueba.util.SentenceTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentimentAnnotator  extends SimpleAnnotator {
    public static Pattern re_line = Pattern.compile("([^ ]+) (\\w+)=([\\d\\.]+)");
    public final static Attribute<TuebaTerminal,String> att_sentiment;

    static {
        TuebaTerminalSchema.instance.addAttribute("sentiment", StringConverter.instance);
        att_sentiment = (Attribute<TuebaTerminal,String>)TuebaTerminalSchema.instance.getAttribute("sentiment");
    }

    transient private Map<String,SentimentItem> model;


    public FileReference lexicon;

    @Override
    public void loadModels() {
        if (model == null) {
            try (BufferedReader rd = new BufferedReader(
                    new InputStreamReader(lexicon.toStream(), Charset.forName("UTF-8")))) {
                model = new HashMap<String, SentimentItem>();
                String line;
                while ((line=rd.readLine())!=null) {
                    Matcher m = re_line.matcher(line);
                    if (m.matches()) {
                        SentimentItem item = new SentimentItem();
                        item.setForm(m.group(1));
                        item.setPolarity(Polarity.valueOf(m.group(2)));
                        item.setWeight(Double.parseDouble(m.group(3)));
                        model.put(item.getForm(), item);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void annotate(TuebaDocument doc) {
        boolean shifter_seen;
        for (SentenceTree tree : SentenceTree.getTrees(doc)) {
            List<TuebaTerminal> terms = tree.getTerminals();
            shifter_seen = false;
            for (TuebaTerminal term: terms) {
                String lemma = term.getLemma();
                if (model.containsKey(lemma)) {
                    SentimentItem item = model.get(lemma);
                    if (item.getPolarity() == Polarity.SHI) {
                        shifter_seen = true;
                    } else if (item.getPolarity() == Polarity.INT) {
                        // ignore intensifiers
                    } else {
                        String tag = String.format("%s_%s", item.getPolarity(),
                                shifter_seen?"NEG":"AFF");
                        att_sentiment.accessor.put(term, tag);
                    }
                }
            }
        }
    }
}

