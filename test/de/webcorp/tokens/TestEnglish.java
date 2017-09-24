package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import de.versley.exml.annotators.nlp4j.NLP4JTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/** Tests for English tokenizers
 */
@RunWith(value=Parameterized.class)
public class TestEnglish extends TestTokenizer {

    /* TODO glued-together tokens
       They did something about their new guest.The staff ranged from indifferent to not helpful.
       No special requests.TV hard to use and iPad sound dock not functioning.
       Driver was waiting for me on arrival.Checkin was easy but ...
       The wakeup call was forgotten.The bathroom facilities were great.

       recognize as [a-z]+\.[A-Z][a-z]+
     */

    @Test
    public void testEnglish() {
        // CoreNLP's PTBTokenizer normalizes " to `` and '', any test including it will fail.
        assertTokenization(tok, "Peter ca|n't do this|.");
        assertTokenization(tok, "``|Oh|, no|,|'' she|'s saying|, "+
                "``|our $|400 blender ca|n't handle something this hard|!|''");
    }

    @Test
    public void testBoundaries() {
        //assertSentence(tok, "I'll take model A.| In variant 12.|");
        assertSentence(tok, "Alfred E. Neumann recommends this.| Yeah.|");
        assertSentence(tok, "Let's meet at the YMCA.| Why not?|");
        assertSentence(tok, "This is a sentence.| This is a sentence too.|");
    }

    @Parameters(name="{index}:{1}")
    public static List<Object[]> getTokenizers() {
        return withTokenizers(
                new CoreNLPTokenizer(),
                new NLP4JTokenizer());
    }
}
