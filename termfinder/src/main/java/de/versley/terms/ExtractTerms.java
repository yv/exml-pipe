package de.versley.terms;

import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;

public class ExtractTerms {
    static ObjectIntHashMap<String> lemmaRank = new ObjectIntHashMap<>();

    public static void main(String[] args) {
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/Users/yannick/data/pynlp/data/encow14ax.freq10.l.tsv"),
                    Charset.forName("UTF-8")));
            String line;
            while ((line = rd.readLine())!=null) {
                String[] fields = line.split("\\s+");
                try {
                    lemmaRank.put(fields[6], Integer.parseInt(fields[1]));
                } catch (NumberFormatException ex) {}
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (String arg: args) {
            try {
                TuebaDocument doc = TuebaDocument.loadDocument(arg);
                for (SentenceTree t: SentenceTree.getTrees(doc)) {
                    for (int i=0; i<t.getTerminals().size(); i++) {
                        TuebaTerminal term = t.getTerminals().get(i);
                        String cat = term.getCat();
                        if ("NN".equals(cat) | "NNS".equals(cat)) {
                            String lemma = term.getLemma();
                            int rank = lemmaRank.get(lemma);
                            if (rank == 0 || rank > 1000) {
                                System.out.format("%s\n", lemma);
                            }
                        }
                        if (i < t.getTerminals().size()-1) {
                            TuebaTerminal term2 = t.getTerminals().get(i+1);
                            String cat2 = term2.getCat();
                            if (("NN".equals(cat) || "JJ".equals(cat)) &&
                                    ("NN".equals(cat2) || "NNS".equals(cat2))) {
                                String lemma = term.getLemma();
                                String lemma2 = term2.getLemma();
                                System.out.format("%s_%s\n", lemma, lemma2);
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
