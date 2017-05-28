package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import webcorp.tokens.TokenizerInterface;

import java.util.Arrays;
import java.util.Collection;

/** Tests for English tokenizers
 */
@RunWith(value=Parameterized.class)
public class TestEnglish extends TestTokenizer {
    public TestEnglish(TokenizerInterface tokenizer) {
        super(tokenizer);
    }

    @Test
    public void testEnglish() {
        assertTokenization(tok, "Peter ca|n't do this|.");
    }

    @Parameters
    public static Collection<TokenizerInterface> getTokenizers() {
        return Arrays.asList(new CoreNLPTokenizer());
    }
}
