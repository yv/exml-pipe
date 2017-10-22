package de.webcorp.tokens;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import webcorp.tokens.JFlexTokenizer;

import java.util.List;

@RunWith(value=Parameterized.class)
public class TestGerman extends TestTokenizer {

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
        assertTokenization(tok, "Tore durch Alexis Alégué (|44.|) und Stefan Aigner (|38.|)");
    }

    @Test
    public void testNonStandard() {
        assertTokenization(tok, "Siehe asset001.jpg von www.sector9.com oder www.anwalt24.de|.");
        assertTokenization(tok, "Christian <|kickern@bollwerk107.de|>");
        assertTokenization(tok, "Ich verkaufe mein Nokia lumia 830|. ungefähr 1 Jahr alt|.");
        assertTokenization(tok, "Er schaut 1080p-Video auf seinem 16:9-Fernseher um 18:30|.");
        // maybe: 19.30 81,- Erdogan'sche Tambourg'sell
        // maybe: Erdogan’sche (wrong quote) ko(s)mischen Nach†gedanke
        // cW-Wert 3000|maH S&P|500
        assertTokenization(tok, "Ein Spät-1968er im BMW auf der A7");
        assertTokenization(tok, "Commedia dell'|arte an der Côte d'|Azur");
        assertTokenization(tok, "@gqspain @tracy_clemens LOVE IT #GQSpain");
        assertTokenization(tok,
                "Papierbackformen (|https://www.pack4food24.de/Baeckerei-Konditorei-Confiserie/"+
                        "Einwegbackformen-aus-Papier|)");
    }

    @Test
    public void testBoundaries() {
        assertSentence(tok, "Ich nehme Modell A.| In Variante 12.|");
        assertSentence(tok,
                "Dies ist ein Satz ohne Anführungszeichen.| "+
                "\"Hier kommen Anführungszeichen!| Sie werden passend zugeordnet.\"| "+
                "Der nächste Satz hat wieder keine.|");
        assertSentence(tok, "Wir wussten es:| Schokolade macht dick.|");
        assertSentence(tok, "Neuer Film (Regie: Alan Smithee), USA 2001");
        assertSentence(tok, "\"Sätze mit Anführungszeichen sind toll.\"|");
        assertSentence(tok, "Speck und Thymianzweig ( ohne Salz ! ) weich kochen");
        assertSentence(tok, "in regelmäßigen ( jeder zweite Aphorismus knallt ! ) , unbeherrschbaren");
        assertSentence(tok, "durch den Brünstawald ( angenehm kühl ! ) hoch");
        assertSentence(tok, "Windows 7 ( von DVD ! ) installiert");
        assertSentence(tok, "Ein Satz.| (Noch ein Satz!)| Ein dritter Satz.");
        assertSentence(tok, "Selbst aus der grandiosen Rollengeschichte "+
                "(Lotte Lehmann! Viorica Ursuleac! Maria Reining !) "+
                "fallen einem keine Sängerinnen ein, die");
    }

    @Parameters(name="{index}:{1}")
    public static List<Object[]> getTokenizers() {
        return withTokenizers(
                new JFlexTokenizer("de"));
    }
}
