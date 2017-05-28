package de.webcorp.tokens;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import webcorp.tokens.JFlexTokenizer;
import webcorp.tokens.TokenizerInterface;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value=Parameterized.class)
public class TestGerman extends TestTokenizer {

    public TestGerman(TokenizerInterface tokenizer) {
        super(tokenizer);
    }

    @Test
    public void testGeneral() {
        super.testGeneral();
    }

    @Test
    public void testGerman() {
        assertTokenization(tok, "Abkürzungen|: Dr. Mabuse gefiel das sehr|.");
        assertTokenization(tok, "Komposita|: (Musik-)Geschichte|, Anti-Abtreibungs-Gesetz");
        assertTokenization(tok, "öffentlich-rechtlichen|, §218-Gesetz");
        assertTokenization(tok, "Peter|: Nach §218 und dem 12. 1000-Meter-Lauf|, "+
                "sagte die 16jährige|, wäre alles|- (|naja|, fast alles|) mit dem "+
                "\"Grenzenlos\"-Modell OK gewesen|, mit ca. 12 Metern|/|Sek.");
        //"Wissen|'s|, das ist Charly's Traum, und Jonas'...");
        assertTokenization(tok, "Lach- und Sachgeschichten");
        assertTokenization(tok, "Marokkos König Mohammed VI. will sein Land ...");
    }

    @Test
    public void testOrdinal() {
        assertTokenization(tok, "Zahlen wie 12 oder XVII|, auch 123.000 oder der 30.|6.|2017");
        assertTokenization(tok, "Vom 4. bis 31. Juli");
        assertTokenization(tok, "SWR Film 03.|07.");
        assertTokenization(tok, "Peter wohnt in der Susenstr. 4|.");
        assertTokenization(tok, "Ich nehme Modell A|. In Variante 12|.");
        assertTokenization(tok, "Bundestagssitzung am 7.|7. gemeldet|: 2016-07-08");
    }

    @Test
    public void testNonStandard() {
        assertTokenization(tok, "Siehe asset001.jpg von www.sector9.com oder www.anwalt24.de|.");
        assertTokenization(tok, "Christian <|kickern@bollwerk107.de|>");
        assertTokenization(tok, "Ich verkaufe mein Nokia lumia 830|. ungefähr 1 Jahr alt|.");
        assertTokenization(tok, "Er schaut 1080p-Video auf seinem 16:9-Fernseher um 18:30|.");
        // maybe: 19.30 81,- Erdogan'sche
        // maybe: Erdogan’sche (wrong quote) ko(s)mischen Nach†gedanke
        assertTokenization(tok, "Ein Spät-1968er im BMW auf der A7");
        assertTokenization(tok, "Commedia dell'|arte an der Côte d'|Azur");
        assertTokenization(tok, "@gqspain @tracy_clemens LOVE IT #GQSpain");
    }

    @Parameters
    public static Collection<TokenizerInterface> getTokenizers() {
        return Arrays.asList(
                new JFlexTokenizer("de"));
    }
}
