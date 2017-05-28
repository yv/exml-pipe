package de.webcorp.tokens;

import de.versley.exml.annotators.CoreNLPTokenizer;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import webcorp.tokens.JFlexTokenizer;
import webcorp.tokens.Token;
import webcorp.tokens.TokenizerInterface;

import java.util.ArrayList;
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
    @Parameter(0)
    public TokenizerInterface tok;
    @Parameter(1)
    public String tokName;

    @Test
    public void testGeneral() {
        assertTokenization(tok, "These are words");
        assertTokenization(tok, "Numbers 1|, 2|, 3|: 123");
        assertTokenization(tok, "more info on www.example.com|, or write to hello@example.com");
    }

    @Test
    public void testBoundaries() {
        assertSentence(tok, "This is a sentence.| Dies ist ein Satz.|");
    }

    @Parameters(name="{index}:{1}")
    public static List<Object[]> getTokenizers() {
        return withTokenizers(
                new JFlexTokenizer("de"),
                new CoreNLPTokenizer());
    }

    public static List<Object[]> withTokenizers(TokenizerInterface ...tokenizers) {
        List<Object[]>params = new ArrayList<>();
        for (TokenizerInterface tok: tokenizers) {
            params.add(new Object[] {tok, tok.getClass().getSimpleName()});
        }
        return params;
    }

    private static void compareSequences(List<Token> wanted, List<Token> result) {
        for (int i=0; i<wanted.size(); i++) {
            assertTrue("token missing in result:"+wanted.get(i), result.size() > i);
            assertEquals("different tokens", wanted.get(i).value, result.get(i).value);
        }
        assertTrue("additional token in result", result.size() <= wanted.size());
    }


    private static final Pattern wsp_token = Pattern.compile("\\S+");

    static void assertTokenization(TokenizerInterface tokenizer, String testCase) {
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

    static void checkSentences(String input, IntArrayList wanted, IntArrayList result) {
        for (int i=0; i<wanted.size(); i++) {
            String s_wanted = input.substring(0, wanted.get(i)) + "|" + input.substring(wanted.get(i));
            assertTrue("boundary missing in result:\n"+s_wanted, result.size() > i);
            String s_result = input.substring(0, result.get(i)) + "|" + input.substring(result.get(i));
            assertEquals("different boundaries", s_wanted, s_result);
        }
        String s_result = result.size() <= wanted.size() ? "(ok)" :
                input.substring(0, result.get(wanted.size()))
                + "|" + input.substring(result.get(wanted.size()));
        assertTrue("additional boundary in result:"+s_result, result.size() <= wanted.size());
    }

    static IntArrayList getSentences(List<Token> tokens) {
        IntArrayList result = new IntArrayList();
        int lastEnd=0;
        for (Token tok: tokens) {
            if (tok.hasFlag(Token.FLAG_BOUNDARY)) {
                result.add(lastEnd);
            }
            lastEnd = tok.end;
        }
        if (result.size() == 0 || result.get(result.size()-1)!= lastEnd) {
            result.add(lastEnd);
        }
        return result;
    }

    static void assertSentence(TokenizerInterface tokenizer, String testCase) {
        String[] parts = testCase.split("\\|");
        StringBuilder input = new StringBuilder();
        IntArrayList wanted = new IntArrayList();
        for (String part: parts) {
            input.append(part);
            wanted.add(input.length());
        }
        List<Token> result = tokenizer.tokenize(input.toString(), 0);
        IntArrayList resultS = getSentences(result);
        checkSentences(input.toString(), wanted, resultS);
    }
}
