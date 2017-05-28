package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import webcorp.tokens.JFlexTokenizer;
import webcorp.tokens.Token;
import webcorp.tokens.TokenizerInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.*;

/**
 */
@RunWith(value=Parameterized.class)
public class TestTokenizer {
    protected TokenizerInterface tok;

    private void compareSequences(List<Token> wanted, List<Token> result) {
        for (int i=0; i<wanted.size(); i++) {
            assertTrue("token missing in result:"+wanted.get(i), result.size() > i);
            assertEquals("different tokens", wanted.get(i).value, result.get(i).value);
        }
        assertTrue("additional token in result", result.size() <= wanted.size());
    }

    public TestTokenizer(TokenizerInterface tokenizer) {
        tok = tokenizer;
    }

    private final Pattern wsp_token = Pattern.compile("\\S+");
    void assertTokenization(TokenizerInterface tokenizer, String testCase) {
        String[] parts = testCase.split("\\|");
        StringBuilder input = new StringBuilder();
        List<Token> wanted = new ArrayList<>();
        for (String part: parts) {
            int offset = input.length();
            input.append(part);
            Matcher m = wsp_token.matcher(part);
            while(m.find()) {
                Token tok = new Token();
                tok.value = m.group();
                tok.start = m.start() + offset;
                tok.end = m.end() + offset;
                wanted.add(tok);
            }
        }
        List<Token> result = tokenizer.tokenize(input.toString(), 0);
        compareSequences(wanted, result);
    }

    @Test
    public void testGeneral() {
        assertTokenization(tok, "These are words");
        assertTokenization(tok, "Numbers 1|, 2|, 3|: 123");
        assertTokenization(tok, "more info on www.example.com|, or write to hello@example.com");
    }

    @Parameters
    public static Collection<TokenizerInterface> getTokenizers() {
        return Arrays.asList(
                new JFlexTokenizer("de"),
                new CoreNLPTokenizer());
    }
}
