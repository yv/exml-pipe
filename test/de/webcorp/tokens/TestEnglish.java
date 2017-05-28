package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import webcorp.tokens.TokenizerInterface;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Tests for English tokenizers
 */
@RunWith(value=Parameterized.class)
public class TestEnglish extends TestTokenizer {

    @Test
    public void testEnglish() {
        // CoreNLP's PTBTokenizer normalizes " to `` and '', any test including them will fail.
        assertTokenization(tok, "Peter ca|n't do this|.");
        assertTokenization(tok, "``|Oh|, no|,|'' she|'s saying|, "+
                "``|our $|400 blender ca|n't handle something this hard|!|''");
    }

    @Parameters(name="{index}:{1}")
    public static List<Object[]> getTokenizers() {
        return withTokenizers(new CoreNLPTokenizer());
    }
}
