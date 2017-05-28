package de.webcorp.tokens;

import org.junit.Test;
import webcorp.tokens.DFATokenizer;
import webcorp.tokens.JFlexTokenizer;
import webcorp.tokens.TokenizerInterface;

/**
 */
public class TestGerman extends TestTokenizer {
    public void testGerman(TokenizerInterface tok) {
        assertTokenization(tok, "Zahlen wie 12 oder XVII|, auch 123.000 oder der 30.|6.|2017");
        assertTokenization(tok, "Vom 4. bis 31. Juli");
        assertTokenization(tok, "Peter wohnt in der Susenstr. 4|.");
        assertTokenization(tok, "Abkürzungen|: Dr. Mabuse gefiel das sehr|.");
        assertTokenization(tok, "Komposita|: (Musik-)Geschichte|, Anti-Abtreibungs-Gesetz");
        assertTokenization(tok, "öffentlich-rechtlichen|, §218-Gesetz");
        assertTokenization(tok, "Peter|: Nach §218 und dem 12. 1000-Meter-Lauf|, "+
                "sagte die 16jährige|, wäre alles|- (|naja|, fast alles|) mit dem "+
                "\"Grenzenlos\"-Modell OK gewesen|, mit ca. 12 Metern|/|Sek.");
        assertTokenization(tok, "Ich nehme Modell A|. In Variante 12|.");
        //"Wissen|'s|, das ist Charly's Traum, und Jonas'...");
        assertTokenization(tok, "Lach- und Sachgeschichten");
        assertTokenization(tok, "Marokkos König Mohammed VI. will sein Land ...");
        assertTokenization(tok, "Bundestagssitzung am 7.|7. gemeldet|: 2016-07-08");
    }

    public void testNonStandard(TokenizerInterface tok) {
        assertTokenization(tok, "Siehe asset001.jpg von www.sector9.com oder www.anwalt24.de|.");
        assertTokenization(tok, "Christian <|kickern@bollwerk107.de|>");
        assertTokenization(tok, "Ich verkaufe mein Nokia lumia 830|. ungefähr 1 Jahr alt|.");
    }

    @Test
    public void testDFA() {
        TokenizerInterface tok = new DFATokenizer("de");
        assertTokenization(tok, "Vom 4. bis 31. Juli");
        assertTokenization(tok, "Peter wohnt in der Susenstr. 4|.");
        assertTokenization(tok, "Komposita|: (Musik-)Geschichte|, Anti-Abtreibungs-Gesetz");
        assertTokenization(tok, "öffentlich-rechtlichen|, §218-Gesetz");
        //testGeneral(tok);
        //testGerman(tok);
    }

    @Test
    public void testJFlex() {
        TokenizerInterface tok = new JFlexTokenizer("de");
        testGeneral(tok);
        testGerman(tok);
        testNonStandard(tok);
    }
}
