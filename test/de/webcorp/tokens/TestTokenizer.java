package de.webcorp.tokens;

import org.junit.Test;
import webcorp.tokens.Token;
import webcorp.tokens.TokenizerInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 */
public class TestTokenizer {
    private void compareSequences(List<Token> wanted, List<Token> result) {
        for (int i=0; i<wanted.size(); i++) {
            assertTrue("token missing in result:"+wanted.get(i), result.size() > i);
            assertEquals("different tokens", wanted.get(i).value, result.get(i).value);
        }
        assertTrue("additional token in result", result.size() <= wanted.size());
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

    void testGeneral(TokenizerInterface tokenizer) {
        assertTokenization(tokenizer, "These are words");
        assertTokenization(tokenizer, "Numbers 1|, 2|, 3|: 123");
        assertTokenization(tokenizer, "more info on www.example.com|, or write to hello@example.com");
    }
}
