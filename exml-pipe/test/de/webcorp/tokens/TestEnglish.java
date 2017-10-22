package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import de.versley.exml.annotators.nlp4j.NLP4JTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import webcorp.tokens.JFlexTokenizer;

import static org.junit.Assume.assumeTrue;

import java.util.List;

/** Tests for English tokenizers
 */
@RunWith(value=Parameterized.class)
public class TestEnglish extends TestTokenizer {

    /*
       Driver was waiting for me on arrival.Checkin was easy but ...
       The wakeup call was forgotten.The bathroom facilities were great.

       recognize as [a-z]+\.[A-Z][a-z]+
     */

    @Test
    public void testEnglish() {
        // CoreNLP's PTBTokenizer normalizes " to `` and '', any test including it will fail.
        // CoreNLP also transforms ( and ) to -LRB- and -RRB-
        // NLP4J tokenizes "token-extraction" as "token" "-" "extraction" (WebTB/Ontonotes-like)
        assertTokenization(tok, "Peter ca|n't do this|.");
        assertTokenization(tok, "``|Oh|, no|,|'' she|'s saying|, "+
                "``|our $|400 blender ca|n't handle something this hard|!|''");
        assertTokenization(tok,"Here|'s another ``|contrived|'' example|.");
        assertTokenization(tok,"Dr. Jekyll and Mr. Hyde|, at Macy|'s|. "+
                "Meeting Mr. T. in the U.S. for his Ph.D. and his wife|.");
    }

    @Test
    public void testEnglishExtended() {
        assumeTrue(tok instanceof JFlexTokenizer);
        assertTokenization(tok, "They did something about their new guest|.|The staff "+
                        "ranged from indifferent to not helpful|.");
        assertTokenization(tok, "No special requests|.|TV hard to use and iPad sound dock not functioning|.");
        assertTokenization(tok, "She|'s an M.Sc. from MIT and ai|n't no fish|.");
        assertTokenization(tok, "Here|'s a (|good|, bad|, indifferent|, ...|) " +
                "example sentence for our ``|token-extraction|''|.");
    }

    @Test
    public void testBoundaries() {
        // failed by NLP4J
        //assertSentence(tok, "I'll take model A.| In variant 12.|");
        assertSentence(tok, "Alfred E. Neumann recommends this.| Yeah.|");
        assertSentence(tok, "Let's meet at the YMCA.| Why not?|");
        assertSentence(tok, "This is a sentence.| This is a sentence too.|");
    }


    @Parameters(name="{index}:{1}")
    public static List<Object[]> getTokenizers() {
        return withTokenizers(
                new CoreNLPTokenizer(),
                new NLP4JTokenizer(),
                new JFlexTokenizer("en"));
    }
}
