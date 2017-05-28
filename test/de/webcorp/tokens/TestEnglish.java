package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import org.junit.Test;
import webcorp.tokens.TokenizerInterface;

/** Tests for English tokenizers
 */
public class TestEnglish extends TestTokenizer {
    void testEnglish(TokenizerInterface tok) {
        assertTokenization(tok, "Peter ca|n't do this|.");
    }

    @Test
    public void testStanford() {
        TokenizerInterface tok = new CoreNLPTokenizer();
        testGeneral(tok);
        testEnglish(tok);
    }
}
